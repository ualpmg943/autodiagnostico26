package es.ual.dra.autodiagnostico.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import es.ual.dra.autodiagnostico.dto.MechanicClientDTO;
import es.ual.dra.autodiagnostico.dto.RepairVehicleMockDTO;
import es.ual.dra.autodiagnostico.dto.WorkshopDTO;
import es.ual.dra.autodiagnostico.dto.WorkshopSelectionRequestDTO;
import es.ual.dra.autodiagnostico.dto.WorkshopSelectionResponseDTO;
import es.ual.dra.autodiagnostico.model.entitity.core.Issue;
import es.ual.dra.autodiagnostico.model.entitity.core.IssueStatus;
import es.ual.dra.autodiagnostico.model.entitity.core.PersonalVehicle;
import es.ual.dra.autodiagnostico.model.entitity.core.Workshop;
import es.ual.dra.autodiagnostico.model.entitity.user.AppUser;
import es.ual.dra.autodiagnostico.model.entitity.user.UserRole;
import es.ual.dra.autodiagnostico.repository.IssueRepository;
import es.ual.dra.autodiagnostico.repository.PersonalVehicleRepository;
import es.ual.dra.autodiagnostico.repository.UserRepository;
import es.ual.dra.autodiagnostico.repository.WorkshopRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkshopService {

    private final WorkshopRepository workshopRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final PersonalVehicleRepository personalVehicleRepository;

    @Transactional(readOnly = true)
    public List<WorkshopDTO> listWorkshops(Long clientId) {
        return workshopRepository.findAll().stream()
                .map(workshop -> toDto(workshop, clientId))
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkshopDTO getWorkshop(Long workshopId, Long clientId) {
        Workshop workshop = getWorkshopOrThrow(workshopId);
        return toDto(workshop, clientId);
    }

    @Transactional
    public WorkshopSelectionResponseDTO selectWorkshop(Long workshopId, WorkshopSelectionRequestDTO request) {
        if (request == null || request.getClientId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId es obligatorio");
        }
        if (request.getPersonalVehicleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personalVehicleId es obligatorio");
        }

        Workshop workshop = getWorkshopOrThrow(workshopId);
        AppUser client = userRepository.findById(request.getClientId())
                .filter(user -> user.getRole() == UserRole.USER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));

        PersonalVehicle personalVehicle = personalVehicleRepository.findById(request.getPersonalVehicleId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehiculo no encontrado"));

        if (personalVehicle.getOwner() == null || !personalVehicle.getOwner().getId().equals(client.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El vehiculo no pertenece al cliente");
        }

        AppUser mechanic = getMechanic(workshop);

        Issue existing = issueRepository
                .findByWorkshopMechanicIdAndPersonalVehicleOwnerIdAndActiveTrue(mechanic.getId(), client.getId())
                .orElse(null);

        Issue issue;
        if (existing != null) {
            issue = existing;
        } else {
            long activeIssues = issueRepository.countByWorkshopMechanicIdAndActiveTrue(mechanic.getId());
            if (activeIssues >= workshop.getVehicleLimit()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El taller ha alcanzado su capacidad maxima");
            }

            for (Issue current : issueRepository.findByPersonalVehicleOwnerIdAndActiveTrue(client.getId())) {
                current.setActive(false);
            }

            issue = Issue.builder()
                    .personalVehicle(personalVehicle)
                    .workshop(workshop)
                    .description(request.getDescription() == null ? "Seleccion de taller por el cliente"
                            : request.getDescription())
                    .status(IssueStatus.WORKSHOP_ASSIGNED)
                    .progressColor("amarillo")
                    .latestUpdate("Taller seleccionado. Pendiente de primera revision del mecanico.")
                    .active(true)
                    .build();
            issue = issueRepository.save(issue);
        }

        return WorkshopSelectionResponseDTO.builder()
                .workshop(toDto(workshop, client.getId()))
                .tracking(toTrackingDto(issue, client))
                .build();
    }

    private Workshop getWorkshopOrThrow(Long workshopId) {
        return workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Taller no encontrado"));
    }

    private AppUser getMechanic(Workshop workshop) {
        return userRepository.findById(workshop.getMechanicId())
                .filter(user -> user.getRole() == UserRole.TALLER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mecanico del taller no encontrado"));
    }

    private WorkshopDTO toDto(Workshop workshop, Long clientId) {
        AppUser mechanic = getMechanic(workshop);
        Issue activeClientIssue = findClientIssueForWorkshop(workshop, clientId);
        long activeVehicles = issueRepository.countByWorkshopMechanicIdAndActiveTrue(mechanic.getId());

        return WorkshopDTO.builder()
                .id(workshop.getId())
                .name(workshop.getName())
                .address(workshop.getAddress())
                .phone(workshop.getPhone())
                .email(workshop.getEmail())
                .schedule(workshop.getSchedule())
                .photoUrl(workshop.getPhotoUrl())
                .vehicleLimit(workshop.getVehicleLimit())
                .activeVehicles(activeVehicles)
                .mechanicId(mechanic.getId())
                .mechanicName(mechanic.getFullName())
                .mechanicAvatar(mechanic.getAvatarUrl())
                .latitude(workshop.getLatitude())
                .longitude(workshop.getLongitude())
                .selectedByClient(activeClientIssue != null)
                .sessionUuid(activeClientIssue == null ? null : activeClientIssue.getSessionUuid())
                .vehiclesInRepair(buildVehicleMocks(activeClientIssue))
                .build();
    }

    private Issue findClientIssueForWorkshop(Workshop workshop, Long clientId) {
        if (clientId == null) {
            return null;
        }
        return issueRepository
                .findByWorkshopMechanicIdAndPersonalVehicleOwnerIdAndActiveTrue(workshop.getMechanicId(), clientId)
                .orElse(null);
    }

    private List<RepairVehicleMockDTO> buildVehicleMocks(Issue issue) {
        if (issue == null) {
            return List.of();
        }
        PersonalVehicle pv = issue.getPersonalVehicle();
        String displayName = pv != null && pv.getVehicleModel() != null && pv.getVehicleModel().getVehicle() != null
                ? (pv.getVehicleModel().getVehicle().getBrand() + " "
                        + pv.getVehicleModel().getVehicle().getName())
                : "Vehiculo";
        return List.of(
                RepairVehicleMockDTO.builder()
                        .id(issue.getId())
                        .name(displayName)
                        .plate(pv == null ? null : pv.getPlate())
                        .status(issue.getProgressColor())
                        .build());
    }

    private MechanicClientDTO toTrackingDto(Issue issue, AppUser client) {
        return MechanicClientDTO.builder()
                .clientId(client.getId())
                .clientName(client.getFullName())
                .clientEmail(client.getEmail())
                .clientAvatar(client.getAvatarUrl())
                .carInfo(null)
                .problemDescription(issue.getDescription())
                .status(issue.getProgressColor())
                .latestUpdate(issue.getLatestUpdate())
                .sessionUuid(issue.getSessionUuid())
                .issueId(issue.getId())
                .build();
    }
}

package es.ual.dra.autodiagnostico.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.ual.dra.autodiagnostico.model.entitity.user.AppUser;
import es.ual.dra.autodiagnostico.model.entitity.user.UserRole;
import es.ual.dra.autodiagnostico.repository.UserRepository;
import es.ual.dra.autodiagnostico.model.entitity.core.Workshop;
import es.ual.dra.autodiagnostico.repository.WorkshopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class MechanicsDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing mechanics and clients data...");
        initializeMechanicsAndClients();
    }

    private void initializeMechanicsAndClients() {
        // Ensure mechanics exist (idempotent by email).
        List<AppUser> mechanics = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            String email = "mecanico" + i + "@taller.local";
            AppUser mechanic = upsertUser(
                    email,
                    mechanicName(i),
                    UserRole.TALLER,
                    "password123",
                    "https://api.dicebear.com/9.x/initials/svg?seed=M" + i + "&backgroundColor=1a6bbd");
            mechanics.add(mechanic);
        }

        initializeWorkshops(mechanics);

        // Ensure 15 default clients exist (idempotent by email).
        for (int i = 1; i <= 15; i++) {
            String email = "cliente" + i + "@user.local";
            AppUser client = upsertUser(
                    email,
                    "Cliente " + i,
                    UserRole.USER,
                    "password123",
                    "https://api.dicebear.com/9.x/initials/svg?seed=C" + i + "&backgroundColor=1a6bbd");
        }

        log.info("Mechanics, workshops and clients initialization completed.");
    }

    private AppUser upsertUser(String email, String fullName, UserRole role, String rawPassword, String avatarUrl) {
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        AppUser user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElseGet(AppUser::new);
        boolean isNew = user.getId() == null;

        user.setEmail(normalizedEmail);
        user.setFullName(fullName);
        user.setRole(role);
        user.setAvatarUrl(avatarUrl);

        if (isNew) {
            user.setPasswordHash(passwordEncoder.encode(rawPassword));
            user.setCreatedAt(LocalDateTime.now());
            AppUser saved = userRepository.save(user);
            log.info("Created {}: {}", role, saved.getEmail());
            return saved;
        }

        // Keep existing password for existing users.
        AppUser saved = userRepository.save(user);
        log.info("Updated {}: {}", role, saved.getEmail());
        return saved;
    }

    private void initializeWorkshops(List<AppUser> mechanics) {
        upsertWorkshop("Taller Central Autodiagnostico", "Calle Principal 123, Almeria", "+34 950 100 201",
                "central@taller.local", "L-V 08:00-18:00", "/taller1.png", 6, mechanics.get(0), 36.8381, -2.4597);
        upsertWorkshop("Auto Diagnosis Express", "Avenida del Mediterraneo 45, Almeria", "+34 950 100 202",
                "express@taller.local", "L-S 09:00-20:00", "/taller2.jpg", 4, mechanics.get(1), 36.8348, -2.4492);
        upsertWorkshop("Reparaciones Rapidas Sur", "Plaza Mayor 8, Almeria", "+34 950 100 203",
                "rapidas@taller.local", "L-V 07:30-16:30", "/taller3.jpg", 5, mechanics.get(2), 36.8424, -2.4665);
        upsertWorkshop("MotorLab Costa", "Camino de Ronda 72, Almeria", "+34 950 100 204",
                "motorlab@taller.local", "L-V 10:00-19:00", "/taller4.jpg", 3, mechanics.get(3), 36.8296, -2.4428);
    }

    private void upsertWorkshop(
            String name,
            String address,
            String phone,
            String email,
            String schedule,
            String photoUrl,
            int vehicleLimit,
            AppUser mechanic,
            double latitude,
            double longitude) {
        Workshop workshop = workshopRepository.findByNameIgnoreCase(name).orElseGet(Workshop::new);
        workshop.setName(name);
        workshop.setAddress(address);
        workshop.setPhone(phone);
        workshop.setEmail(email);
        workshop.setSchedule(schedule);
        workshop.setPhotoUrl(photoUrl);
        workshop.setVehicleLimit(vehicleLimit);
        workshop.setMechanicId(mechanic.getId());
        workshop.setLatitude(latitude);
        workshop.setLongitude(longitude);
        workshopRepository.save(workshop);
        log.info("Upserted workshop {} for mechanic {}", name, mechanic.getEmail());
    }

    private String mechanicName(int index) {
        return switch (index) {
            case 1 -> "Carlos Medina";
            case 2 -> "Lucia Navarro";
            case 3 -> "Andres Molina";
            case 4 -> "Marta Salas";
            default -> "Mecanico " + index;
        };
    }
}

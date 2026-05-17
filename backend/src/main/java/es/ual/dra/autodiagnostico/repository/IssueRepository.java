package es.ual.dra.autodiagnostico.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import es.ual.dra.autodiagnostico.model.entitity.core.Issue;

@Repository
public interface IssueRepository extends JpaRepository<Issue, Long> {

    Optional<Issue> findBySessionUuid(String sessionUuid);

    List<Issue> findByWorkshopMechanicIdAndActiveTrue(Long mechanicId);

    List<Issue> findByPersonalVehicleOwnerIdAndActiveTrue(Long ownerId);

    Optional<Issue> findFirstByPersonalVehicleOwnerIdAndActiveTrue(Long ownerId);

    Optional<Issue> findByWorkshopMechanicIdAndPersonalVehicleOwnerIdAndActiveTrue(Long mechanicId, Long ownerId);

    long countByWorkshopMechanicIdAndActiveTrue(Long mechanicId);
}

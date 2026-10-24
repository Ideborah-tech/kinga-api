package com.kinga.api.repository;

import com.kinga.api.model.ProviderIncident;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderIncidentRepository extends JpaRepository<ProviderIncident, Long> {

    Optional<ProviderIncident> findFirstByProviderAndActiveTrue(String provider);

    List<ProviderIncident> findByActiveTrueOrderByStartedAtDesc();

    List<ProviderIncident> findTop50ByOrderByStartedAtDesc();

    List<ProviderIncident> findTop20ByProviderOrderByStartedAtDesc(String provider);
}

package com.kinga.api.repository;

import com.kinga.api.model.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByOwnerOrderByIdAsc(String owner);

    List<Monitor> findByProviderAndActiveTrue(String provider);

    List<Monitor> findByActiveTrue();
}

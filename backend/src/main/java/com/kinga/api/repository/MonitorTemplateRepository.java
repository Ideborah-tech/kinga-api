package com.kinga.api.repository;

import com.kinga.api.model.MonitorTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonitorTemplateRepository extends JpaRepository<MonitorTemplate, Long> {

    List<MonitorTemplate> findAllByOrderByUsesDesc();

    List<MonitorTemplate> findByProviderOrderByUsesDesc(String provider);
}

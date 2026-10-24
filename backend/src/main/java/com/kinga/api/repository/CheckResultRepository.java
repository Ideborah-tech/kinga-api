package com.kinga.api.repository;

import com.kinga.api.model.CheckResult;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    List<CheckResult> findByMonitorIdOrderByCheckedAtDesc(Long monitorId, Pageable pageable);

    List<CheckResult> findByMonitorIdAndCheckedAtAfterOrderByCheckedAtAsc(Long monitorId, Instant after);

    Optional<CheckResult> findFirstByMonitorIdAndSuccessFalseOrderByCheckedAtDesc(Long monitorId);

    Optional<CheckResult> findFirstByMonitorIdAndSuccessTrueOrderByCheckedAtDesc(Long monitorId);

    long countByCheckedAtAfter(Instant after);

    long countByCheckedAtAfterAndSuccessFalse(Instant after);

    @Transactional
    void deleteByMonitorId(Long monitorId);
}

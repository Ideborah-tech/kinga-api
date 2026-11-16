package com.kinga.api.service;

import com.kinga.api.catalog.ProviderCatalog;
import com.kinga.api.dto.FailureReport;
import com.kinga.api.dto.ProviderHealth;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.kinga.api.model.MonitorStatus;
import com.kinga.api.model.ProviderIncident;
import com.kinga.api.repository.CheckResultRepository;
import com.kinga.api.repository.MonitorRepository;
import com.kinga.api.repository.ProviderIncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportServiceTest {

    private static final String STATUS_PAGE = "https://status.example.com/mpesa";

    private final MonitorRepository monitors = mock(MonitorRepository.class);
    private final CheckResultRepository checks = mock(CheckResultRepository.class);
    private final CommunityService community = mock(CommunityService.class);
    private final ProviderCatalog catalog = mock(ProviderCatalog.class);
    private final ProviderIncidentRepository incidents = mock(ProviderIncidentRepository.class);

    private ReportService reports;
    private Monitor monitor;
    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        reports = new ReportService(monitors, checks, community, catalog, incidents);
        ReflectionTestUtils.setField(reports, "minMonitors", 3);
        ReflectionTestUtils.setField(reports, "windowMinutes", 5);

        monitor = new Monitor();
        monitor.id = 7L;
        monitor.name = "M-Pesa OAuth";
        monitor.owner = "amina";
        monitor.provider = "MPESA";
        monitor.url = "http://localhost:8085/mock/mpesa/oauth";
        monitor.lastStatus = MonitorStatus.DOWN;
        monitor.consecutiveFailures = 3;
        when(monitors.findById(7L)).thenReturn(Optional.of(monitor));

        var provider = new ProviderCatalog.Provider("MPESA", "mpesa", "M-Pesa", "Safaricom", List.of("KE"),
                "mobile-money", "#00a651", "https://developer.example.com", STATUS_PAGE, List.of());
        when(catalog.byCode("MPESA")).thenReturn(Optional.of(provider));
        when(catalog.nameOf("MPESA")).thenReturn("M-Pesa");
    }

    private CheckResult check(boolean success, FailureType type, int minutesAgo) {
        CheckResult c = new CheckResult();
        c.monitorId = 7L;
        c.success = success;
        c.failureType = type;
        c.statusCode = success ? 200 : 401;
        c.checkedAt = now.minusSeconds(minutesAgo * 60L);
        return c;
    }

    /** Recent checks, newest first: three failures after one success. */
    private void givenFailingAfterSuccess(FailureType type) {
        CheckResult ok = check(true, FailureType.NONE, 4);
        CheckResult f1 = check(false, type, 3);
        CheckResult f2 = check(false, type, 2);
        CheckResult f3 = check(false, type, 1);
        when(checks.findByMonitorIdOrderByCheckedAtDesc(eq(7L), any())).thenReturn(List.of(f3, f2, f1, ok));
        when(checks.findFirstByMonitorIdAndSuccessFalseOrderByCheckedAtDesc(7L)).thenReturn(Optional.of(f3));
        when(checks.findFirstByMonitorIdAndSuccessTrueOrderByCheckedAtDesc(7L)).thenReturn(Optional.of(ok));
    }

    private void givenHealth(int total, int failing, int developers, ProviderIncident incident) {
        when(community.health("MPESA")).thenReturn(new ProviderHealth("MPESA", "M-Pesa", "DEGRADED",
                total, failing, 0, developers, 1, 300, FailureType.AUTH, incident));
    }

    @Test
    void openProviderIncidentMeansItsThem() {
        givenFailingAfterSuccess(FailureType.SERVER_ERROR);
        ProviderIncident incident = new ProviderIncident();
        incident.summary = "4 of 5 M-Pesa monitors are failing.";
        givenHealth(5, 4, 4, incident);

        FailureReport r = reports.build(7L);

        assertThat(r.verdict()).isEqualTo("THEM");
        assertThat(r.verdictDetail()).contains(incident.summary).contains(STATUS_PAGE);
        assertThat(r.resolved()).isFalse();
    }

    @Test
    void everyoneElseHealthyMeansItsYouWithAuthSuggestions() {
        givenFailingAfterSuccess(FailureType.AUTH);
        givenHealth(5, 1, 4, null);

        FailureReport r = reports.build(7L);

        assertThat(r.verdict()).isEqualTo("YOU");
        assertThat(r.verdictDetail())
                .contains("4 other monitor(s) from 3 other developer(s)")
                .contains("credentials or your request");
        assertThat(r.title()).isEqualTo("Authentication failed");
        assertThat(r.suggestions()).hasSize(4)
                .anyMatch(s -> s.contains("sandbox"))
                .anyMatch(s -> s.contains("one hour"));
        // Failing since the oldest failure after the last success, not the latest failure.
        assertThat(r.failingSince()).isEqualTo(now.minusSeconds(180));
        assertThat(r.consecutiveFailures()).isEqualTo(3);
    }

    @Test
    void verdictIsUnclearWithTooFewMonitorsOrSomeOthersFailing() {
        givenFailingAfterSuccess(FailureType.TIMEOUT);

        givenHealth(2, 1, 2, null);
        FailureReport tooFew = reports.build(7L);

        givenHealth(5, 3, 4, null);
        FailureReport someFailing = reports.build(7L);

        assertThat(tooFew.verdict()).isEqualTo("UNCLEAR");
        assertThat(tooFew.verdictHeadline()).isEqualTo("Not enough shared data yet.");
        assertThat(someFailing.verdict()).isEqualTo("UNCLEAR");
        assertThat(someFailing.verdictDetail()).startsWith("2 of 4 other monitor(s)");
        // A timed-out payment may still go through: the report must warn about double charging.
        assertThat(someFailing.suggestions()).anyMatch(s -> s.contains("double-charge"));
    }

    @Test
    void monitorThatNeverFailedGetsAllGoodReport() {
        monitor.lastStatus = MonitorStatus.UP;
        when(checks.findByMonitorIdOrderByCheckedAtDesc(eq(7L), any())).thenReturn(List.of(check(true, FailureType.NONE, 1)));
        when(checks.findFirstByMonitorIdAndSuccessFalseOrderByCheckedAtDesc(7L)).thenReturn(Optional.empty());
        givenHealth(5, 0, 4, null);

        FailureReport r = reports.build(7L);

        assertThat(r.hasFailure()).isFalse();
        assertThat(r.resolved()).isTrue();
        assertThat(r.verdict()).isEqualTo("NONE");
        assertThat(r.failureType()).isEqualTo(FailureType.NONE);
        assertThat(r.actual()).isNull();
        assertThat(r.suggestions()).isEmpty();
    }

    @Test
    void recoveredFailureDuringAnOutageStillSaysItWasThem() {
        // M-Pesa outage is over: the provider is healthy now, but the last failure happened during it.
        monitor.lastStatus = MonitorStatus.UP;
        givenFailingAfterSuccess(FailureType.SERVER_ERROR);
        givenHealth(5, 0, 4, null);
        ProviderIncident past = new ProviderIncident();
        past.summary = "6 of 6 monitors from 4 developers are failing on M-Pesa (server errors).";
        past.startedAt = now.minusSeconds(150);
        past.resolvedAt = now.minusSeconds(30);
        past.active = false;
        when(incidents.findTop20ByProviderOrderByStartedAtDesc("MPESA")).thenReturn(List.of(past));

        FailureReport r = reports.build(7L);

        assertThat(r.resolved()).isTrue();
        assertThat(r.verdict()).isEqualTo("THEM");
        assertThat(r.verdictHeadline()).isEqualTo("It was them, not you.");
        assertThat(r.verdictDetail()).contains(past.summary);
    }

    @Test
    void recoveredFailureWithoutAnOutageIsNotBlamedOnYourSetup() {
        monitor.lastStatus = MonitorStatus.UP;
        givenFailingAfterSuccess(FailureType.TIMEOUT);
        givenHealth(5, 0, 4, null);
        ProviderIncident older = new ProviderIncident();
        older.startedAt = now.minusSeconds(3 * 3600);
        older.resolvedAt = now.minusSeconds(2 * 3600);
        when(incidents.findTop20ByProviderOrderByStartedAtDesc("MPESA")).thenReturn(List.of(older));

        FailureReport r = reports.build(7L);

        assertThat(r.verdict()).isEqualTo("NONE");
        assertThat(r.verdictHeadline()).doesNotContain("your setup");
    }

    @Test
    void unknownMonitorThrowsNotFound() {
        when(monitors.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reports.build(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("99");
    }
}

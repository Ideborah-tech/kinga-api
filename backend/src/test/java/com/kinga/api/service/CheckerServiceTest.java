package com.kinga.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs real checks against a local HTTP server so the classification order is tested end to end. */
class CheckerServiceTest {

    private HttpServer server;
    private CheckerService checker;

    /** What the local server answers with; each test sets these before calling check(). */
    private volatile int status = 200;
    private volatile String body = "{}";
    private volatile long delayMs = 0;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try {
                if (delayMs > 0) Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            } catch (IOException ignored) {
                // The client gave up (timeout test).
            }
        });
        server.start();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        checker = new CheckerService(client, new ObjectMapper());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private Monitor monitor(String expectedFields) {
        Monitor m = new Monitor();
        m.id = 1L;
        m.provider = "MPESA";
        m.owner = "amina";
        m.url = "http://127.0.0.1:" + server.getAddress().getPort() + "/oauth";
        m.expectedFields = expectedFields;
        m.timeoutMs = 2000;
        return m;
    }

    @Test
    void healthyResponseWithAllFieldsIsSuccess() {
        body = "{\"access_token\":\"abc\",\"data\":[{\"code\":\"OK\"}]}";

        CheckResult r = checker.check(monitor("access_token, data.0.code"));

        assertThat(r.success).isTrue();
        assertThat(r.formatValid).isTrue();
        assertThat(r.failureType).isEqualTo(FailureType.NONE);
        assertThat(r.statusCode).isEqualTo(200);
        assertThat(r.monitorId).isEqualTo(1L);
        assertThat(r.provider).isEqualTo("MPESA");
    }

    @ParameterizedTest(name = "HTTP {0} -> {1}")
    @CsvSource({
            "401, AUTH",
            "403, AUTH",
            "429, RATE_LIMIT",
            "500, SERVER_ERROR",
            "503, SERVER_ERROR",
            "404, UNEXPECTED_STATUS",
            "400, UNEXPECTED_STATUS"
    })
    void statusCodesAreClassified(int code, FailureType expected) {
        status = code;

        CheckResult r = checker.check(monitor(null));

        assertThat(r.success).isFalse();
        assertThat(r.statusCode).isEqualTo(code);
        assertThat(r.failureType).isEqualTo(expected);
        assertThat(r.errorMessage).contains(String.valueOf(code));
    }

    @Test
    void missingJsonFieldsAreBadFormatAndListed() {
        // Schema-change story: the provider renamed access_token.
        body = "{\"token\":\"abc\",\"expires_in\":3599}";

        CheckResult r = checker.check(monitor("access_token,expires_in,data.0.code"));

        assertThat(r.success).isFalse();
        assertThat(r.failureType).isEqualTo(FailureType.BAD_FORMAT);
        assertThat(r.missingFields).isEqualTo("access_token,data.0.code");
    }

    @Test
    void nonJsonBodyIsBadFormatOnlyWhenFieldsAreExpected() {
        body = "<html>Bad gateway</html>";

        CheckResult withFields = checker.check(monitor("access_token"));
        CheckResult withoutFields = checker.check(monitor(""));

        assertThat(withFields.failureType).isEqualTo(FailureType.BAD_FORMAT);
        assertThat(withFields.errorMessage).contains("not valid JSON");
        assertThat(withoutFields.success).isTrue();
        assertThat(withoutFields.failureType).isEqualTo(FailureType.NONE);
    }

    @Test
    void slowButCorrectResponseIsSuccessMarkedSlow() {
        delayMs = 300;
        Monitor m = monitor(null);
        m.slowThresholdMs = 100;

        CheckResult r = checker.check(m);

        assertThat(r.success).isTrue();
        assertThat(r.failureType).isEqualTo(FailureType.SLOW);
        assertThat(r.responseTimeMs).isGreaterThan(100);
    }

    @Test
    void noAnswerBeforeTimeoutIsTimeout() {
        delayMs = 1500;
        Monitor m = monitor(null);
        m.timeoutMs = 300;

        CheckResult r = checker.check(m);

        assertThat(r.success).isFalse();
        assertThat(r.failureType).isEqualTo(FailureType.TIMEOUT);
        assertThat(r.statusCode).isNull();
    }

    @Test
    void refusedConnectionIsConnection() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        Monitor m = monitor(null);
        m.url = "http://127.0.0.1:" + closedPort + "/oauth";

        CheckResult r = checker.check(m);

        assertThat(r.success).isFalse();
        assertThat(r.failureType).isEqualTo(FailureType.CONNECTION);
    }

    @Test
    void parseHeadersSkipsMalformedLines() {
        List<String[]> headers = CheckerService.parseHeaders(
                "Authorization: Bearer abc:def\r\nno colon here\n: empty name\nX-Env:  sandbox ");

        assertThat(headers).hasSize(2);
        assertThat(headers.get(0)).containsExactly("Authorization", "Bearer abc:def");
        assertThat(headers.get(1)).containsExactly("X-Env", "sandbox");
    }
}

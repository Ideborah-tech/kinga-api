package com.kinga.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kinga.api.model.CheckResult;
import com.kinga.api.model.FailureType;
import com.kinga.api.model.Monitor;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sends one request to a monitored endpoint and classifies what came back. */
@Service
public class CheckerService {

    private static final int SNIPPET_LIMIT = 1500;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CheckerService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public CheckResult check(Monitor monitor) {
        CheckResult result = new CheckResult();
        result.monitorId = monitor.id;
        result.provider = monitor.provider;
        result.owner = monitor.owner;
        result.requestUrl = monitor.url;
        result.requestMethod = monitor.method;

        long start = System.nanoTime();
        try {
            HttpResponse<String> response = httpClient.send(buildRequest(monitor), HttpResponse.BodyHandlers.ofString());
            result.responseTimeMs = elapsedMs(start);
            result.statusCode = response.statusCode();
            result.responseSnippet = truncate(response.body());
            classifyResponse(monitor, response.body(), result);
        } catch (HttpTimeoutException e) {
            result.responseTimeMs = elapsedMs(start);
            fail(result, FailureType.TIMEOUT, "No response within " + monitor.timeoutMs + " ms");
        } catch (ConnectException e) {
            result.responseTimeMs = elapsedMs(start);
            fail(result, FailureType.CONNECTION, "Could not connect: " + messageOf(e));
        } catch (IllegalArgumentException e) {
            result.responseTimeMs = elapsedMs(start);
            fail(result, FailureType.CONNECTION, "Invalid request: " + messageOf(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.responseTimeMs = elapsedMs(start);
            fail(result, FailureType.CONNECTION, "Check was interrupted");
        } catch (Exception e) {
            result.responseTimeMs = elapsedMs(start);
            fail(result, FailureType.CONNECTION, e.getClass().getSimpleName() + ": " + messageOf(e));
        }
        return result;
    }

    private HttpRequest buildRequest(Monitor monitor) {
        String method = monitor.method == null ? "GET" : monitor.method.toUpperCase();
        HttpRequest.BodyPublisher body = (monitor.body == null || monitor.body.isBlank())
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(monitor.body);

        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(monitor.url.trim()))
                .timeout(Duration.ofMillis(monitor.timeoutMs))
                .header("User-Agent", "KingaAPI-Monitor/1.0")
                .method(method, body);

        boolean hasContentType = false;
        for (String[] header : parseHeaders(monitor.headers)) {
            builder.header(header[0], header[1]);
            hasContentType |= header[0].equalsIgnoreCase("Content-Type");
        }
        if (!hasContentType && monitor.body != null && !monitor.body.isBlank()) {
            builder.header("Content-Type", "application/json");
        }
        return builder.build();
    }

    static List<String[]> parseHeaders(String headers) {
        List<String[]> parsed = new ArrayList<>();
        if (headers == null) return parsed;
        for (String line : headers.split("\\r?\\n")) {
            int colon = line.indexOf(':');
            if (colon <= 0) continue;
            String name = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (!name.isEmpty()) parsed.add(new String[]{name, value});
        }
        return parsed;
    }

    private void classifyResponse(Monitor monitor, String body, CheckResult result) {
        int status = result.statusCode;
        if (status == 401 || status == 403) {
            fail(result, FailureType.AUTH, "Provider rejected the credentials (HTTP " + status + ")");
            return;
        }
        if (status == 429) {
            fail(result, FailureType.RATE_LIMIT, "Rate limit reached (HTTP 429)");
            return;
        }
        if (status >= 500) {
            fail(result, FailureType.SERVER_ERROR, "Provider server error (HTTP " + status + ")");
            return;
        }
        if (status != monitor.expectedStatus) {
            fail(result, FailureType.UNEXPECTED_STATUS, "Expected HTTP " + monitor.expectedStatus + " but got " + status);
            return;
        }

        List<String> missing = missingFields(monitor.expectedFields, body);
        if (missing == null) {
            fail(result, FailureType.BAD_FORMAT, "Response is not valid JSON");
            return;
        }
        if (!missing.isEmpty()) {
            result.missingFields = String.join(",", missing);
            fail(result, FailureType.BAD_FORMAT, "Response is missing expected fields: " + result.missingFields);
            return;
        }

        result.success = true;
        result.formatValid = true;
        if (result.responseTimeMs > monitor.slowThresholdMs) {
            result.failureType = FailureType.SLOW;
            result.errorMessage = "Slow response: " + result.responseTimeMs + " ms (threshold " + monitor.slowThresholdMs + " ms)";
        }
    }

    /** Returns the expected fields that are absent, or null when the body is not JSON but fields were expected. */
    List<String> missingFields(String expectedFields, String body) {
        List<String> expected = splitFields(expectedFields);
        if (expected.isEmpty()) return List.of();
        JsonNode root;
        try {
            root = objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
        if (root == null) return null;
        List<String> missing = new ArrayList<>();
        for (String field : expected) {
            JsonNode node = root.at("/" + field.replace('.', '/'));
            if (node.isMissingNode()) missing.add(field);
        }
        return missing;
    }

    public static List<String> splitFields(String fields) {
        if (fields == null || fields.isBlank()) return List.of();
        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private static void fail(CheckResult result, FailureType type, String message) {
        result.success = false;
        result.failureType = type;
        result.errorMessage = message;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String truncate(String body) {
        if (body == null) return null;
        return body.length() <= SNIPPET_LIMIT ? body : body.substring(0, SNIPPET_LIMIT) + "…";
    }

    private static String messageOf(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}

package com.kinga.api.model;

/** Why a check failed. Drives the diagnosis in the Dependency Failure Report. */
public enum FailureType {
    NONE,
    /** Succeeded but slower than the monitor's threshold. */
    SLOW,
    /** 401 / 403 */
    AUTH,
    /** 429 */
    RATE_LIMIT,
    /** 5xx */
    SERVER_ERROR,
    /** No response within the timeout. */
    TIMEOUT,
    /** DNS, refused connection, TLS errors. */
    CONNECTION,
    /** Right status but the body is not the expected shape. */
    BAD_FORMAT,
    /** Any other status code that is not the expected one. */
    UNEXPECTED_STATUS;

    /** Failures that usually come from the provider rather than the developer's own setup. */
    public boolean isProviderSide() {
        return this == SERVER_ERROR || this == TIMEOUT || this == CONNECTION || this == BAD_FORMAT || this == SLOW;
    }

    public boolean isFailure() {
        return this != NONE && this != SLOW;
    }
}

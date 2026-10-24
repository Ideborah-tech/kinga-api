package com.kinga.api.mock;

/** How a mock provider behaves. Switched from the Demo Control page to stage outages on cue. */
public enum MockMode {
    HEALTHY,
    /** Answers correctly but takes 2.5–4 seconds. */
    SLOW,
    /** 50% of requests return 500. */
    FLAKY,
    /** 503 Service Unavailable. */
    SERVER_ERROR,
    /** Never answers within the monitor's timeout. */
    TIMEOUT,
    /** 401 for everyone. */
    AUTH_ERROR,
    /** 429 Too Many Requests. */
    RATE_LIMITED,
    /** 200 OK but the JSON shape changed. */
    BAD_FORMAT
}

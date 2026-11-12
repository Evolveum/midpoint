/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.util;

import static com.evolveum.midpoint.authentication.impl.util.MidpointRequestMatchers.pathMatcher;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Determines which actuator endpoint a request is for.
 *
 * It is used for security decisions.
 * A path that cannot be decoded is classified by its raw segments.
 */
public class ActuatorEndpointPath {

    /** Returned for the discovery page ({@code /actuator}), which lists the links to the endpoints. */
    public static final String DISCOVERY = "";

    private static final String ACTUATOR_PREFIX = "actuator";

    private static final String ENDPOINT_VARIABLE = "endpoint";

    private static final RequestMatcher ENDPOINT = pathMatcher("/actuator/{" + ENDPOINT_VARIABLE + "}/**");

    private static final RequestMatcher DISCOVERY_PAGE = new OrRequestMatcher(
            pathMatcher("/actuator"),
            pathMatcher("/actuator/"));

    /**
     * Returns the actuator endpoint of the request, {@link #DISCOVERY} for the discovery page,
     * or {@code null} if the request is not an actuator one.
     */
    public static @Nullable String getEndpoint(@NotNull HttpServletRequest request) {
        try {
            RequestMatcher.MatchResult result = ENDPOINT.matcher(request);
            if (result.isMatch()) {
                return result.getVariables().get(ENDPOINT_VARIABLE);
            }
            return DISCOVERY_PAGE.matches(request) ? DISCOVERY : null;
        } catch (IllegalArgumentException e) {
            // Invalid URL encoding, the path cannot be parsed. Classify it by the raw segments.
            return getEndpointFromRawPath(request);
        }
    }

    private static @Nullable String getEndpointFromRawPath(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String[] segments = StringUtils.removeStart(path, "/").split("/", -1);
        if (!ACTUATOR_PREFIX.equals(StringUtils.substringBefore(segments[0], ";"))) {
            return null;
        }
        return segments.length > 1 ? StringUtils.substringBefore(segments[1], ";") : DISCOVERY;
    }
}

/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.util;

import static com.evolveum.midpoint.authentication.impl.util.MidpointRequestMatchers.pathMatcher;

import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Classifies a request path as a REST API endpoint, a cluster REST endpoint, or neither.
 *
 * It is used for security decisions.
 * (via {@link MidpointRequestMatchers}).
 *
 * A path that cannot be decoded is classified by its raw first segment, and never as {@link Kind#CLUSTER}.
 */
public class RestEndpointPath {

    /** First path segments under which REST controllers are mapped; used only for paths that cannot be decoded. */
    private static final Set<String> REST_PREFIXES = Set.of("ws", "rest", "api");

    /** Cluster endpoints, under all three REST prefixes. */
    private static final RequestMatcher CLUSTER_ENDPOINTS = new OrRequestMatcher(
            pathMatcher("/ws/cluster/**"),
            pathMatcher("/rest/cluster/**"),    //todo exists but not used
            pathMatcher("/api/cluster/**"));    //todo exists but not used

    /** All REST endpoints (prefixes under which REST controllers are mapped). */
    private static final RequestMatcher REST_ENDPOINTS = new OrRequestMatcher(
            pathMatcher("/ws/**"),
            pathMatcher("/rest/**"),
            pathMatcher("/api/**"));

    public enum Kind {
        /** Not a REST endpoint; not affected by the REST startup options. */
        NOT_REST,
        /** REST API endpoint under {@code /ws}, {@code /rest} or {@code /api}. */
        API,
        /** Cluster REST endpoint ({@code /ws/cluster}, ...). */
        CLUSTER
    }

    public static @NotNull Kind classify(@NotNull HttpServletRequest request) {
        try {
            if (CLUSTER_ENDPOINTS.matches(request)) {
                return Kind.CLUSTER;
            }
            return REST_ENDPOINTS.matches(request) ? Kind.API : Kind.NOT_REST;
        } catch (IllegalArgumentException e) {
            // Invalid URL encoding (e.g. "%zz"), the path cannot be parsed. Classify it by the raw first segment.
            return hasRawRestPrefix(request) ? Kind.API : Kind.NOT_REST;
        }
    }

    /** Is the first raw (not decoded) path segment after the context path one of the REST prefixes? */
    private static boolean hasRawRestPrefix(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String firstSegment = StringUtils.substringBefore(StringUtils.removeStart(path, "/"), "/");
        return REST_PREFIXES.contains(StringUtils.substringBefore(firstSegment, ";"));
    }

    public static boolean isClusterEndpoint(@NotNull HttpServletRequest request) {
        return classify(request) == Kind.CLUSTER;
    }
}

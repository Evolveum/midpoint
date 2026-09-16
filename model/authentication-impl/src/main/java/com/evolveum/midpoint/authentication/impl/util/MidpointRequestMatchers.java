/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.util;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Factory for request matchers used by authentication security configuration.
 *
 * Keeps the concrete Spring Security matcher implementation in one place,
 * while callers only depend on the generic {@link RequestMatcher} contract.
 */
public final class MidpointRequestMatchers {

    private static final PathPatternRequestMatcher.Builder MATCHER_BUILDER = PathPatternRequestMatcher.withDefaults();

    private MidpointRequestMatchers() {
    }

    public static RequestMatcher pathMatcher(String pattern) {
        return MATCHER_BUILDER.matcher(pattern);
    }

    public static RequestMatcher pathMatcher(String pattern, String method) {
        return MATCHER_BUILDER.matcher(HttpMethod.valueOf(method), pattern);
    }
}

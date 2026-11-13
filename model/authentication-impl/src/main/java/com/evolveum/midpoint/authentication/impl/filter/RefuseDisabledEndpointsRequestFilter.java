/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import com.evolveum.midpoint.authentication.impl.ActuatorEndpointAccessEvaluator;
import com.evolveum.midpoint.authentication.impl.RestEndpointAccessEvaluator;
import com.evolveum.midpoint.authentication.impl.util.ActuatorEndpointPath;
import com.evolveum.midpoint.authentication.impl.util.RestEndpointPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Ends requests to disabled REST and actuator endpoints with HTTP 404,
 * see {@link RestEndpointAccessEvaluator} and {@link ActuatorEndpointAccessEvaluator}.
 * Other requests (GUI, ...) pass unchanged.
 * <p>
 * It is the first filter in the security filter chain, so a rejected request causes no authentication attempt,
 * no lockout counting and no audit record (TODO review/confirm).
 * <p>
 * {@code SecurityConfigurer} adds it to the chain.
 */
public class RefuseDisabledEndpointsRequestFilter extends OncePerRequestFilter {

    private static final Trace LOGGER = TraceManager.getTrace(RefuseDisabledEndpointsRequestFilter.class);

    @NotNull private final RestEndpointAccessEvaluator restAccessEvaluator;
    @NotNull private final ActuatorEndpointAccessEvaluator actuatorAccessEvaluator;

    public RefuseDisabledEndpointsRequestFilter(
            @NotNull RestEndpointAccessEvaluator restAccessEvaluator,
            @NotNull ActuatorEndpointAccessEvaluator actuatorAccessEvaluator) {
        this.restAccessEvaluator = restAccessEvaluator;
        this.actuatorAccessEvaluator = actuatorAccessEvaluator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain chain) throws IOException, ServletException {
        if (!isEndpointEnabled(request)) {
            LOGGER.debug("Request to {} rejected with 404, because the {} is disabled",
                    request.getRequestURI(), describeDisabledEndpoint(request));
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }

   private boolean isEndpointEnabled(HttpServletRequest request) {
        Boolean actuatorEnabled = isActuatorEndpointEnabled(request);
        if (actuatorEnabled != null) {
            return actuatorEnabled;
        }
        Boolean restEnabled = isRestEndpointEnabled(request);
        if (restEnabled != null) {
            return restEnabled;
        }
        // Neither an actuator nor a REST endpoint; such requests are not affected by this filter.
        return true;
    }

    /**
     * Returns whether the actuator endpoint of the request is enabled;
     * {@code null} if the request is not for an actuator endpoint.
     */
    private @Nullable Boolean isActuatorEndpointEnabled(HttpServletRequest request) {
        String actuatorEndpoint = ActuatorEndpointPath.getEndpoint(request);
        return actuatorEndpoint != null ? actuatorAccessEvaluator.isEnabled(actuatorEndpoint) : null;
    }

    /**
     * Returns whether the REST endpoint of the request is enabled;
     * {@code null} if the request is not for a REST endpoint.
     */
    private @Nullable Boolean isRestEndpointEnabled(HttpServletRequest request) {
        RestEndpointPath.Kind kind = RestEndpointPath.classify(request);
        return kind != RestEndpointPath.Kind.NOT_REST ? restAccessEvaluator.isEnabled(kind) : null;
    }

    /**
     * Describes, for logging, the disabled endpoint the request is for.
     */
    private @NotNull String describeDisabledEndpoint(HttpServletRequest request) {
        String actuatorEndpoint = ActuatorEndpointPath.getEndpoint(request);
        if (actuatorEndpoint != null) {
            return "actuator endpoint '" + actuatorEndpoint + "'";
        }
        return RestEndpointPath.classify(request) == RestEndpointPath.Kind.CLUSTER
                ? "intra-cluster REST endpoint" : "REST API";
    }
}

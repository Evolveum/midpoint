/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.util.Set;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.Test;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.filter.RefuseDisabledEndpointsRequestFilter;
import com.evolveum.midpoint.authentication.impl.util.AuthSequenceUtil;
import com.evolveum.midpoint.common.configuration.api.ActuatorConfigurationSection;
import com.evolveum.midpoint.test.AbstractHigherUnitTest;

/**
 * Unit tests of switching the REST API, the intra-cluster endpoints and the actuator endpoints by startup options (bug:12960).
 */
public class TestDisabledEndpoints extends AbstractHigherUnitTest {

    /**
     * The state of the intra-cluster endpoints, and node authentication ({@code Cluster} authorization header)
     * accepted only on the intra-cluster endpoints, not on the REST API endpoints.
     */
    @Test
    public void test010ClusterEndpoints() {
        // Startup option, if set, wins; otherwise enabled only on a clustered node.
        assertFalse(RestEndpointAccessEvaluator.resolveClusterEndpointsState(null, false));
        assertTrue(RestEndpointAccessEvaluator.resolveClusterEndpointsState(null, true));
        assertTrue(RestEndpointAccessEvaluator.resolveClusterEndpointsState(true, false));
        assertFalse(RestEndpointAccessEvaluator.resolveClusterEndpointsState(false, true));

        assertTrue(AuthSequenceUtil.isClusterSequence(requestWithClusterHeader("/midpoint/ws/cluster/scheduler")));
        assertTrue(AuthSequenceUtil.isClusterSequence(requestWithClusterHeader("/midpoint/api/cluster/scheduler")));
        assertFalse(AuthSequenceUtil.isClusterSequence(requestWithClusterHeader("/midpoint/ws/rest/users")));
        assertFalse(AuthSequenceUtil.isClusterSequence(requestWithClusterHeader("/midpoint/api/model/users")));
    }

    /**
     * REST API endpoints are refused when the REST API is disabled.
     * Only the enabled actuators pass, and the discovery page passes only if at least one actuator is enabled.
     * Requests other than REST and actuator ones pass even when everything is disabled.
     */
    @Test
    public void test020DisabledEndpointsAreRefused() throws Exception {
        RefuseDisabledEndpointsRequestFilter healthOnly = filter(false, false, Set.of("health"));
        assertPassed(healthOnly, "/midpoint/actuator/health");
        assertPassed(healthOnly, "/midpoint/actuator/health/liveness");
        assertRefused(healthOnly, "/midpoint/actuator/metrics");
        assertRefused(healthOnly, "/midpoint/actuator/heapdump");
        assertPassed(healthOnly, "/midpoint/actuator");
        assertPassed(healthOnly, "/midpoint/actuator/");

        RefuseDisabledEndpointsRequestFilter allDisabled = filter(false, false, Set.of());
        assertRefused(allDisabled, "/midpoint/ws/rest/users");
        assertRefused(allDisabled, "/midpoint/rest/model/users");
        assertRefused(allDisabled, "/midpoint/api/model/users");
        assertRefused(allDisabled, "/midpoint/actuator");
        assertRefused(allDisabled, "/midpoint/actuator/");
        assertPassed(allDisabled, "/midpoint/admin/users");
        assertPassed(allDisabled, "/midpoint/login");

        assertPassed(filter(true, false, Set.of()), "/midpoint/ws/rest/users");
    }

    private void assertRefused(RefuseDisabledEndpointsRequestFilter filter, String uri) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request(uri), response, chain);

        assertEquals("Wrong status for " + uri, HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        assertNull("Request " + uri + " must not continue down the chain", chain.getRequest());
    }

    private void assertPassed(RefuseDisabledEndpointsRequestFilter filter, String uri) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request(uri), response, chain);

        assertNotNull("Request " + uri + " should continue down the chain", chain.getRequest());
        assertEquals("Wrong status for " + uri, HttpServletResponse.SC_OK, response.getStatus());
    }

    private MockHttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setContextPath("/midpoint");
        return request;
    }

    private MockHttpServletRequest requestWithClusterHeader(String uri) {
        MockHttpServletRequest request = request(uri);
        request.addHeader("Authorization", AuthenticationModuleNameConstants.CLUSTER + " c2VjcmV0");
        return request;
    }

    private RefuseDisabledEndpointsRequestFilter filter(
            boolean apiEnabled, boolean clusterEnabled, Set<String> enabledActuators) {
        RestEndpointAccessEvaluator restEvaluator = new RestEndpointAccessEvaluator();
        ReflectionTestUtils.setField(restEvaluator, "restApiEnabledProperty", apiEnabled);
        ReflectionTestUtils.setField(restEvaluator, "restClusterEnabled", clusterEnabled);

        ActuatorEndpointAccessEvaluator actuatorEvaluator = new ActuatorEndpointAccessEvaluator();
        ReflectionTestUtils.setField(actuatorEvaluator, "actuatorConfigurationSection", new ActuatorConfigurationSection() {
            @Override
            public boolean isEndpointEnabled(String endpointId) {
                return enabledActuators.contains(endpointId);
            }

            @Override
            public boolean isAnyEndpointEnabled() {
                return !enabledActuators.isEmpty();
            }
        });
        return new RefuseDisabledEndpointsRequestFilter(restEvaluator, actuatorEvaluator);
    }
}

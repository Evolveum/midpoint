/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.authentication.impl.filter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.UrlUtils;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

public class MidpointFilterChainProxy extends FilterChainProxy {

    private static final Trace LOGGER = TraceManager.getTrace(MidpointFilterChainProxy.class);

    private static final String FILTER_APPLIED =
            MidpointFilterChainProxy.class.getName().concat(".APPLIED");

    private final List<SecurityFilterChain> filterChains;

    private FilterChainValidator filterChainValidator =
            new NullFilterChainValidator();

    private HttpFirewall firewall = new StrictHttpFirewall();

    public MidpointFilterChainProxy(List<SecurityFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    @Override
    public void afterPropertiesSet() {
        filterChainValidator.validate(this);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        boolean clearContext = request.getAttribute(FILTER_APPLIED) == null;
        if (clearContext) {
            try {
                request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
                doFilterInternal(request, response, chain);
            } finally {
                SecurityContextHolder.clearContext();
                request.removeAttribute(FILTER_APPLIED);
            }
        } else {
            doFilterInternal(request, response, chain);
        }
    }

    private void doFilterInternal(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        FirewalledRequest fwRequest = firewall
                .getFirewalledRequest((HttpServletRequest) request);
        HttpServletResponse fwResponse = firewall
                .getFirewalledResponse((HttpServletResponse) response);

        List<Filter> filters = getFilters(fwRequest);

        if (filters == null || filters.size() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(UrlUtils.buildRequestUrl(fwRequest)
                        + (filters == null ? " has no matching filters"
                        : " has an empty filter list"));
            }

            fwRequest.reset();

            chain.doFilter(fwRequest, fwResponse);

            return;
        }

        VirtualFilterChain vfc = new VirtualFilterChain(fwRequest, chain, filters);
        vfc.doFilter(fwRequest, fwResponse);
    }

    private List<Filter> getFilters(HttpServletRequest request) {
        for (SecurityFilterChain chain : filterChains) {
            if (chain.matches(request)) {
                return chain.getFilters();
            }
        }

        return null;
    }

    public List<Filter> getFilters(String url) {
        return getFilters(firewall.getFirewalledRequest((new FilterInvocation(url, "GET")
                .getRequest())));
    }

    public List<SecurityFilterChain> getFilterChains() {
        return Collections.unmodifiableList(filterChains);
    }

    public void setFilterChainValidator(FilterChainValidator filterChainValidator) {
        this.filterChainValidator = filterChainValidator;
    }

    public void setFirewall(HttpFirewall firewall) {
        this.firewall = firewall;
    }

    @Override
    public String toString() {
        return "FilterChainProxy["
                + "Filter Chains: "
                + filterChains
                + "]";
    }

    private static class VirtualFilterChain implements FilterChain {
        private final FilterChain originalChain;
        private final List<Filter> additionalFilters;
        private final FirewalledRequest firewalledRequest;
        private final int size;
        private int currentPosition = 0;

        private VirtualFilterChain(FirewalledRequest firewalledRequest,
                FilterChain chain, List<Filter> additionalFilters) {
            this.originalChain = chain;
            this.additionalFilters = additionalFilters;
            this.size = additionalFilters.size();
            this.firewalledRequest = firewalledRequest;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (currentPosition == size) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " reached end of additional filter chain");
                }
            } else {
                currentPosition++;

                Filter nextFilter = additionalFilters.get(currentPosition - 1);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " at position " + currentPosition + " of " + size
                            + " in additional filter chain; firing Filter: '"
                            + nextFilter.getClass().getSimpleName() + "'");
                }
                FilterChain chain = this;
                if (nextFilter instanceof MidpointAuthFilter) {
                    nextFilter.doFilter(request, response, originalChain);
                } else {
                    nextFilter.doFilter(request, response, chain);
                }
            }
        }
    }

    public interface FilterChainValidator {
        void validate(MidpointFilterChainProxy filterChainProxy);
    }

    private static class NullFilterChainValidator implements FilterChainValidator {
        @Override
        public void validate(MidpointFilterChainProxy filterChainProxy) {
        }
    }

}

package com.evolveum.midpoint.web.boot.testsaml;

import com.evolveum.midpoint.web.boot.WebSecurityConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.FirewalledRequest;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.UrlUtils;
import org.springframework.web.accept.ContentNegotiationStrategy;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MidpointFilterChainProxy extends GenericFilterBean {
    // ~ Static fields/initializers
    // =====================================================================================

    private static final Log logger = LogFactory.getLog(MidpointFilterChainProxy.class);

    // ~ Instance fields
    // ================================================================================================

    private final static String FILTER_APPLIED = MidpointFilterChainProxy.class.getName().concat(
            ".APPLIED");

    private List<SecurityFilterChain> filterChains;

    private MidpointFilterChainProxy.FilterChainValidator filterChainValidator = new MidpointFilterChainProxy.NullFilterChainValidator();

    private HttpFirewall firewall = new StrictHttpFirewall();

    @Autowired(required = false)
    private ObjectPostProcessor<Object> objectObjectPostProcessor;

    @Autowired
    ApplicationContext context;

    // ~ Methods
    // ========================================================================================================

    public MidpointFilterChainProxy() {
    }

//    public MidpointFilterChainProxy(SecurityFilterChain chain) {
//        this(Arrays.asList(chain));
//    }

    public MidpointFilterChainProxy(List<SecurityFilterChain> filterChains) {
        this.filterChains = filterChains;
    }

    @Override
    public void afterPropertiesSet() {
        filterChainValidator.validate(this);
    }

    boolean createFilter = false;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        if (!createFilter) {
            createFilter = true;
            List<SecurityFilterChain> filters = new ArrayList<SecurityFilterChain>();
            filters.addAll(filterChains);

            WebSecurityConfig webSecInternal = objectObjectPostProcessor.postProcess(new WebSecurityConfig());
            webSecInternal.setObjectPostProcessor(objectObjectPostProcessor);
            webSecInternal.setApplicationContext(context);
            try {
                webSecInternal.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            try {
                webSecInternal.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            try {
                webSecInternal.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            SecurityFilterChain internalFilter = null;
            try {
                HttpSecurity internalHttp = webSecInternal.getNewHttp();


            } catch (Exception e) {
                e.printStackTrace();
            }

            WebSecurityConfig webSec = objectObjectPostProcessor.postProcess(new WebSecurityConfig());
            webSec.setObjectPostProcessor(objectObjectPostProcessor);
            webSec.setApplicationContext(context);
            try {
                webSec.setTrustResolver(context.getBean(AuthenticationTrustResolver.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            try {
                webSec.setContentNegotationStrategy(context.getBean(ContentNegotiationStrategy.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            try {
                webSec.setAuthenticationConfiguration(context.getBean(AuthenticationConfiguration.class));
            } catch( NoSuchBeanDefinitionException e ) {
            }
            SecurityFilterChain newFilter = null;
            try {
                newFilter = webSec.getNewHttp().build();

            } catch (Exception e) {
                e.printStackTrace();
            }
            SecurityFilterChain filter2 = filters.get(15);
            filters.remove(15);
            filters.add(newFilter);
            filters.add(filter2);
            this.filterChains = filters;
        }

        boolean clearContext = request.getAttribute(FILTER_APPLIED) == null;
        if (clearContext) {
            try {
                request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
                doFilterInternal(request, response, chain);
            }
            finally {
                SecurityContextHolder.clearContext();
                request.removeAttribute(FILTER_APPLIED);
            }
        }
        else {
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
            if (logger.isDebugEnabled()) {
                logger.debug(UrlUtils.buildRequestUrl(fwRequest)
                        + (filters == null ? " has no matching filters"
                        : " has an empty filter list"));
            }

            fwRequest.reset();

            chain.doFilter(fwRequest, fwResponse);

            return;
        }



        MidpointFilterChainProxy.VirtualFilterChain vfc = new MidpointFilterChainProxy.VirtualFilterChain(fwRequest, chain, filters);
        vfc.doFilter(fwRequest, fwResponse);
    }

    /**
     * Returns the first filter chain matching the supplied URL.
     *
     * @param request the request to match
     * @return an ordered array of Filters defining the filter chain
     */
    private List<Filter> getFilters(HttpServletRequest request) {
        for (SecurityFilterChain chain : filterChains) {
            if (chain.matches(request)) {
                return chain.getFilters();
            }
        }

        return null;
    }

    /**
     * Convenience method, mainly for testing.
     *
     * @param url the URL
     * @return matching filter list
     */
    public List<Filter> getFilters(String url) {
        return getFilters(firewall.getFirewalledRequest((new FilterInvocation(url, "GET")
                .getRequest())));
    }

    /**
     * @return the list of {@code SecurityFilterChain}s which will be matched against and
     * applied to incoming requests.
     */
    public List<SecurityFilterChain> getFilterChains() {
        return Collections.unmodifiableList(filterChains);
    }

    /**
     * Used (internally) to specify a validation strategy for the filters in each
     * configured chain.
     *
     * @param filterChainValidator the validator instance which will be invoked on during
     * initialization to check the {@code FilterChainProxy} instance.
     */
    public void setFilterChainValidator(MidpointFilterChainProxy.FilterChainValidator filterChainValidator) {
        this.filterChainValidator = filterChainValidator;
    }

    /**
     * Sets the "firewall" implementation which will be used to validate and wrap (or
     * potentially reject) the incoming requests. The default implementation should be
     * satisfactory for most requirements.
     *
     * @param firewall
     */
    public void setFirewall(HttpFirewall firewall) {
        this.firewall = firewall;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FilterChainProxy[");
        sb.append("Filter Chains: ");
        sb.append(filterChains);
        sb.append("]");

        return sb.toString();
    }

    // ~ Inner Classes
    // ==================================================================================================

    /**
     * Internal {@code FilterChain} implementation that is used to pass a request through
     * the additional internal list of filters which match the request.
     */
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
                if (logger.isDebugEnabled()) {
                    logger.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " reached end of additional filter chain; proceeding with original chain");
                }

                // Deactivate path stripping as we exit the security filter chain
                this.firewalledRequest.reset();

                originalChain.doFilter(request, response);
            }
            else {
                currentPosition++;

                Filter nextFilter = additionalFilters.get(currentPosition - 1);

                if (logger.isDebugEnabled()) {
                    logger.debug(UrlUtils.buildRequestUrl(firewalledRequest)
                            + " at position " + currentPosition + " of " + size
                            + " in additional filter chain; firing Filter: '"
                            + nextFilter.getClass().getSimpleName() + "'");
                }

                nextFilter.doFilter(request, response, this);
            }
        }
    }

    public interface FilterChainValidator {
        void validate(MidpointFilterChainProxy filterChainProxy);
    }

    private static class NullFilterChainValidator implements MidpointFilterChainProxy.FilterChainValidator {
        @Override
        public void validate(MidpointFilterChainProxy filterChainProxy) {
        }
    }

}
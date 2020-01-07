/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.filter;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.model.api.context.SecurityQuestionsAuthenticationContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.RestAuthenticationMethod;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.security.BasicMidPointAuthenticationSuccessHandler;
import com.evolveum.midpoint.web.security.module.authentication.SecurityQuestionsAuthenticationToken;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.github.openjson.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.opensaml.xmlsec.signature.J;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.util.Assert;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;

/**
 * @author skublik
 */

public class HttpSecurityQuestionsAuthenticationFilter extends BasicAuthenticationFilter {

    private static final Trace LOGGER = TraceManager.getTrace(HttpSecurityQuestionsAuthenticationFilter.class);

    public static final String J_ANSWER = "answer";
    public static final String J_USER = "user";


    private AuthenticationDetailsSource<HttpServletRequest, ?> authenticationDetailsSource = new WebAuthenticationDetailsSource();
    private RememberMeServices rememberMeServices = new NullRememberMeServices();
    private String credentialsCharset = "UTF-8";
    private AuthenticationSuccessHandler successHandler = new BasicMidPointAuthenticationSuccessHandler();

    public HttpSecurityQuestionsAuthenticationFilter(AuthenticationManager authenticationManager,
                                                     AuthenticationEntryPoint authenticationEntryPoint) {
        super(authenticationManager, authenticationEntryPoint);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        try {
            String header = request.getHeader("Authorization");

            int delim = header.indexOf(" ");

            if (delim == -1) {
                throw new BadCredentialsException("Invalid authentication header, value of header don't contains delimiter ' '."
                        + " Please use form 'Authorization: <type> <credentials>' for successful authentication");
            }

            if (header == null || !header.toLowerCase().startsWith(NameOfModuleType.SECURITY_QUESTIONS.getName().toLowerCase() + " ")) {
                chain.doFilter(request, response);
                return;
            }

            JSONObject tokens = extractAndDecodeHeader(header, request);
            assert tokens.keySet().contains(J_USER);
            assert tokens.keySet().contains(J_ANSWER);

            String username = tokens.getString(J_USER);

            LOGGER.debug("Security Questions - Authentication Authorization header found for user '" + username + "'");

            if (authenticationIsRequired(username)) {
                Map<String, String> answers = SecurityUtils.obtainAnswers(tokens.get(J_ANSWER).toString(),
                        SecurityQuestionsAuthenticationFilter.J_QID, SecurityQuestionsAuthenticationFilter.J_QANS);
                SecurityQuestionsAuthenticationToken authRequest = new SecurityQuestionsAuthenticationToken(
                        username, answers);
                authRequest.setDetails(
                        this.authenticationDetailsSource.buildDetails(request));
                Authentication authResult = getAuthenticationManager()
                        .authenticate(authRequest);

                onSuccessfulAuthentication(request, response, authResult);

                LOGGER.debug("Authentication success: " + authResult);

                this.rememberMeServices.loginSuccess(request, response, authResult);

            }

        }
        catch (AuthenticationException failed) {
            LOGGER.debug("Authentication request for failed: " + failed);

            this.rememberMeServices.loginFail(request, response);

            this.getAuthenticationEntryPoint().commence(request, response, failed);

            return;
        }

        chain.doFilter(request, response);
    }


    private JSONObject extractAndDecodeHeader(String header, HttpServletRequest request)
            throws IOException {

        int startIndex = NameOfModuleType.SECURITY_QUESTIONS.getName().length() + 1;
        byte[] base64Token = header.substring(startIndex).getBytes("UTF-8");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Token);
        }
        catch (IllegalArgumentException e) {
            throw new BadCredentialsException(
                    "Failed to decode security question authentication token");
        }

        String token = new String(decoded, getCredentialsCharset(request));

        return new JSONObject(token);
    }

    private boolean authenticationIsRequired(String username) {
        Authentication existingAuth = SecurityContextHolder.getContext()
                .getAuthentication();

        if (existingAuth == null || !existingAuth.isAuthenticated()) {
            return true;
        }

        if ((existingAuth instanceof UsernamePasswordAuthenticationToken
        || existingAuth instanceof MidpointAuthentication)
                && !existingAuth.getName().equals(username)) {
            return true;
        }

        if (existingAuth instanceof AnonymousAuthenticationToken) {
            return true;
        }

        return false;
    }

    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        Assert.notNull(rememberMeServices, "rememberMeServices cannot be null");
        this.rememberMeServices = rememberMeServices;
    }

    public void setCredentialsCharset(String credentialsCharset) {
        Assert.hasText(credentialsCharset, "credentialsCharset cannot be null or empty");
        this.credentialsCharset = credentialsCharset;
    }

    protected String getCredentialsCharset(HttpServletRequest httpRequest) {
        return this.credentialsCharset;
    }

    @Override
    protected void onSuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, Authentication authResult) throws IOException {
        try {
            successHandler.onAuthenticationSuccess(request, response, authResult);
        } catch (ServletException e) {
            LOGGER.error("Couldn't execute post successful authentication method", e);
        }
    }
}

/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.security.util;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.DescriptorLoader;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.security.factory.channel.AbstractChannelFactory;
import com.evolveum.midpoint.web.security.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.web.security.factory.module.HttpClusterModuleFactory;
import com.evolveum.midpoint.web.security.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.web.security.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.web.security.factory.module.AuthModuleRegistryImpl;
import com.evolveum.midpoint.web.security.factory.module.AbstractModuleFactory;
import com.evolveum.midpoint.web.component.menu.MainMenuItem;
import com.evolveum.midpoint.web.component.menu.MenuItem;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.request.Request;
import org.apache.wicket.request.Response;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;
import static org.springframework.security.saml.util.StringUtils.stripStartingSlashes;

/**
 * @author lazyman
 * @author lskublik
 */
public class SecurityUtils {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityUtils.class);
    private static final String PROXY_USER_OID_HEADER = "Switch-To-Principal";
    public static final String DEFAULT_LOGOUT_PATH = "/logout";

    public static GuiProfiledPrincipal getPrincipalUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return getPrincipalUser(authentication);
    }

    private static final Map<String, String> LOCAL_PATH_AND_CHANNEL;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("ws/rest", SchemaConstants.CHANNEL_REST_URI);
        map.put("actuator", SchemaConstants.CHANNEL_ACTUATOR_URI);
        map.put("resetPassword", SchemaConstants.CHANNEL_GUI_RESET_PASSWORD_URI);
        map.put("registration", SchemaConstants.CHANNEL_GUI_SELF_REGISTRATION_URI);
        LOCAL_PATH_AND_CHANNEL = Collections.unmodifiableMap(map);
    }

    public static GuiProfiledPrincipal getPrincipalUser(Authentication authentication) {
        if (authentication == null) {
            LOGGER.trace("Authentication not available in security context.");
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }
        if (principal instanceof GuiProfiledPrincipal) {
            return (GuiProfiledPrincipal) principal;
        }
        if (AuthorizationConstants.ANONYMOUS_USER_PRINCIPAL.equals(principal)) {
            // silently ignore to avoid filling the logs
            return null;
        }
        LOGGER.debug("Principal user in security context holder is {} ({}) but not type of {}",
                principal, principal.getClass(), GuiProfiledPrincipal.class.getName());
        return null;
    }

    public static boolean isMenuAuthorized(MainMenuItem item) {
        Class clazz = item.getPageClass();
        return clazz == null || isPageAuthorized(clazz);
    }

    public static boolean isMenuAuthorized(MenuItem item) {
        Class clazz = item.getPageClass();
        return isPageAuthorized(clazz);
    }

    public static boolean isPageAuthorized(Class page) {
        if (page == null) {
            return false;
        }

        PageDescriptor descriptor = (PageDescriptor) page.getAnnotation(PageDescriptor.class);
        if (descriptor == null ){
            return false;
        }


        AuthorizationAction[] actions = descriptor.action();
        List<String> list = new ArrayList<>();
        if (actions != null) {
            for (AuthorizationAction action : actions) {
                list.add(action.actionUri());
            }
        }

        return WebComponentUtil.isAuthorized(list.toArray(new String[list.size()]));
    }

    public static WebMarkupContainer createHiddenInputForCsrf(String id) {
        WebMarkupContainer field = new WebMarkupContainer(id) {

            @Override
            public void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
                super.onComponentTagBody(markupStream, openTag);

                appendHiddenInputForCsrf(getResponse());
            }
        };
        field.setRenderBodyOnly(true);

        return field;
    }

    public static void appendHiddenInputForCsrf(Response resp) {
        CsrfToken csrfToken = getCsrfToken();
        if (csrfToken == null) {
            return;
        }

        String parameterName = csrfToken.getParameterName();
        String value = csrfToken.getToken();

        resp.write("<input type=\"hidden\" name=\"" + parameterName + "\" value=\"" + value + "\"/>");
    }

    public static CsrfToken getCsrfToken() {
        Request req = RequestCycle.get().getRequest();
        HttpServletRequest httpReq = (HttpServletRequest) req.getContainerRequest();

        return (CsrfToken) httpReq.getAttribute("_csrf");
    }

    public static AuthenticationSequenceType getSequenceByPath(HttpServletRequest httpRequest, AuthenticationsPolicyType authenticationPolicy){
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (authenticationPolicy == null || authenticationPolicy.getSequence() == null
                || authenticationPolicy.getSequence().isEmpty()) {
            return null;
        }
        String[] partsOfLocalPath = stripStartingSlashes(localePath).split("/");

        AuthenticationSequenceType specificSequence =  getSpecificSequence(httpRequest);
        if (specificSequence != null) {
            return specificSequence;
        }

        if (partsOfLocalPath.length >= 2 && partsOfLocalPath[0].equals(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE)) {
            AuthenticationSequenceType sequence = searchSequence(partsOfLocalPath[1], false, authenticationPolicy);
            if (sequence == null) {
                LOGGER.debug("Couldn't find sequence by preffix {}, so try default channel", partsOfLocalPath[1]);
                sequence = searchSequence(SecurityPolicyUtil.DEFAULT_CHANNEL, true, authenticationPolicy);
            }
            return sequence;
        }
        String usedChannel = searchChannelByPath(localePath);

        if (usedChannel == null) {
            usedChannel = SecurityPolicyUtil.DEFAULT_CHANNEL;
        }

        AuthenticationSequenceType sequence = searchSequence(usedChannel, true, authenticationPolicy);
        return sequence;

    }

    public static String searchChannelByPath(String localePath) {
        for (String prefix : LOCAL_PATH_AND_CHANNEL.keySet()) {
            if (stripStartingSlashes(localePath).startsWith(prefix)) {
                return LOCAL_PATH_AND_CHANNEL.get(prefix);
            }
        }
        return null;
    }

    public static String searchPathByChannel(String searchchannel) {
        for (Map.Entry<String, String> entry : LOCAL_PATH_AND_CHANNEL.entrySet()) {
            if (entry.getValue().equals(searchchannel)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static String findChannelByRequest(HttpServletRequest httpRequest) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        return searchChannelByPath(localePath);
    }

    private static AuthenticationSequenceType getSpecificSequence(HttpServletRequest httpRequest) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String channel = searchChannelByPath(localePath);
        if (LOCAL_PATH_AND_CHANNEL.get("ws/rest").equals(channel)) {
            String header = httpRequest.getHeader("Authorization");
            if (header != null) {
                String type = header.split(" ")[0];
                if (NameOfModuleType.CLUSTER.getName().toLowerCase().equals(type.toLowerCase())) {
                    AuthenticationSequenceType sequence = new AuthenticationSequenceType();
                    sequence.setName(NameOfModuleType.CLUSTER.getName());
                    AuthenticationSequenceChannelType seqChannel = new AuthenticationSequenceChannelType();
                    seqChannel.setUrlSuffix(NameOfModuleType.CLUSTER.getName().toLowerCase());
                    return sequence;
                }
            }
        }
        return null;
    }

    private static AuthenticationSequenceType searchSequence(String comparisonAttribute, boolean useOnlyChannel, AuthenticationsPolicyType authenticationPolicy) {
        Validate.notBlank(comparisonAttribute, "Comparison attribute for searching of sequence is blank");
        for (AuthenticationSequenceType sequence : authenticationPolicy.getSequence()) {
            if (sequence != null && sequence.getChannel() != null) {
                if (useOnlyChannel && comparisonAttribute.equals(sequence.getChannel().getChannelId())
                        && Boolean.TRUE.equals(sequence.getChannel().isDefault())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()){
                        return null;
                    }
                    return sequence;
                } else if (!useOnlyChannel && comparisonAttribute.equals(sequence.getChannel().getUrlSuffix())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()){
                        return null;
                    }
                    return sequence;
                }
            }
        }
        return null;
    }

    public static AuthenticationSequenceType getSequenceByName(String name, AuthenticationsPolicyType authenticationPolicy){
        if (authenticationPolicy == null || authenticationPolicy.getSequence() == null
                || authenticationPolicy.getSequence().isEmpty()) {
            return null;
        }

        Validate.notBlank(name,"Name for searching of sequence is blank");
        for (AuthenticationSequenceType sequence : authenticationPolicy.getSequence()) {
            if (sequence != null) {
                if (name.equals(sequence.getName())) {
                    if (sequence.getModule() == null || sequence.getModule().isEmpty()){
                        return null;
                    }
                    return sequence;
                }
            }
        }
        return null;
    }

    public static List<AuthModule> buildModuleFilters(AuthModuleRegistryImpl authRegistry, AuthenticationSequenceType sequence,
                                                      HttpServletRequest request, AuthenticationModulesType authenticationModulesType,
                                                      CredentialsPolicyType credentialPolicy, Map<Class<? extends Object>, Object> sharedObjects,
                                                      AuthenticationChannel authenticationChannel) {
        Validate.notNull(authRegistry, "Registry for module factories is null");
        Validate.notEmpty(sequence.getModule(), "Sequence " + sequence.getName() + " don't contains authentication modules");

        List<AuthModule> specificModules = getSpecificModuleFilter(sequence.getChannel().getUrlSuffix(), request,
                sharedObjects, authenticationModulesType, credentialPolicy);
        if (specificModules != null) {
            return specificModules;
        }

        List<AuthenticationSequenceModuleType> sequenceModules = SecurityPolicyUtil.getSortedModules(sequence);
        List<AuthModule> authModules = new ArrayList<AuthModule>();
        sequenceModules.forEach(sequenceModule -> {
            try {
                AbstractAuthenticationModuleType module = getModuleByName(sequenceModule.getName(), authenticationModulesType);
                AbstractModuleFactory moduleFactory = authRegistry.findModelFactory(module);
                AuthModule authModule = moduleFactory.createModuleFilter(module, sequence.getChannel().getUrlSuffix(), request,
                        sharedObjects, authenticationModulesType, credentialPolicy, authenticationChannel);
                authModules.add(authModule);
            } catch (Exception e) {
                LOGGER.error("Couldn't build filter for module moduleFactory", e);
            }
        });
        if (authModules.isEmpty()) {
            return null;
        }
        return authModules;
    }

    private static List<AuthModule> getSpecificModuleFilter(String urlSuffix, HttpServletRequest httpRequest, Map<Class<?>, Object> sharedObjects,
                                                            AuthenticationModulesType authenticationModulesType, CredentialsPolicyType credentialPolicy) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String channel = searchChannelByPath(localePath);
        if (LOCAL_PATH_AND_CHANNEL.get("ws/rest").equals(channel)) {
            String header = httpRequest.getHeader("Authorization");
            if (header != null) {
                String type = header.split(" ")[0];
                if (NameOfModuleType.CLUSTER.getName().toLowerCase().equals(type.toLowerCase())) {
                    List<AuthModule> authModules = new ArrayList<AuthModule>();
                    WebApplicationContext context = ContextLoader.getCurrentWebApplicationContext();
                    HttpClusterModuleFactory factory = context.getBean(HttpClusterModuleFactory.class);
                    AbstractAuthenticationModuleType module = new AbstractAuthenticationModuleType() {
                    };
                    module.setName(NameOfModuleType.CLUSTER.getName().toLowerCase() + "-module");
                    try {
                        authModules.add(factory.createModuleFilter(module, urlSuffix, httpRequest,
                                sharedObjects, authenticationModulesType, credentialPolicy, null));
                    } catch (Exception e) {
                        LOGGER.error("Couldn't create module for cluster authentication");
                        return null;
                    }
                    return authModules;
                }
            }
        }
        return null;
    }

    private static AbstractAuthenticationModuleType getModuleByName(String name, AuthenticationModulesType authenticationModulesType){
        List<AbstractAuthenticationModuleType> modules = new ArrayList<AbstractAuthenticationModuleType>();
        modules.addAll(authenticationModulesType.getLoginForm());
        modules.addAll(authenticationModulesType.getSaml2());
        modules.addAll(authenticationModulesType.getHttpBasic());
        modules.addAll(authenticationModulesType.getHttpHeader());
        modules.addAll(authenticationModulesType.getHttpSecQ());
        modules.addAll(authenticationModulesType.getMailNonce());
        modules.addAll(authenticationModulesType.getOidc());
        modules.addAll(authenticationModulesType.getSecurityQuestionsForm());
        modules.addAll(authenticationModulesType.getSmsNonce());
        modules.addAll(authenticationModulesType.getLdap());

        for (AbstractAuthenticationModuleType module: modules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    public static AbstractModuleFactory getFactoryByName(AuthModuleRegistryImpl authRegistry, String name, AuthenticationModulesType authenticationModulesType){
        AbstractAuthenticationModuleType module = getModuleByName(name, authenticationModulesType);
        if (module != null) {
            return authRegistry.findModelFactory(module);
        }
        return null;
    }

    public static boolean isPermitAll(HttpServletRequest request) {
        for (String url: DescriptorLoader.getPermitAllUrls()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return true;
            }
        }
        String servletPath = request.getServletPath();
        if ("".equals(servletPath) || "/".equals(servletPath)) {
            // Special case, this is in fact "magic" redirect to home page or login page. It handles autz in its own way.
            return true;
        }
        return false;
    }

    public static boolean isLoginPage(HttpServletRequest request) {
        for (String url: DescriptorLoader.getLoginPages()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }

    public static ModuleAuthentication getProcessingModule(boolean required) {
        Authentication actualAuthentication = SecurityContextHolder.getContext().getAuthentication();

        if (actualAuthentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) actualAuthentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (required && moduleAuthentication == null) {
                LOGGER.error("Couldn't find processing module authentication {}", mpAuthentication);
                throw new AuthenticationServiceException("web.security.flexAuth.module.null");
            }
            return moduleAuthentication;
        } else if (required) {
            LOGGER.error("Type of actual authentication in security context isn't MidpointAuthentication");
            throw new AuthenticationServiceException("web.security.flexAuth.auth.wrong.type");
        }
        return null;
    }

    public static void saveException(HttpServletRequest request,
                                     AuthenticationException exception) {
        HttpSession session = request.getSession(false);

        request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, exception);
    }

    public static AuthenticationChannel buildAuthChannel(AuthChannelRegistryImpl registry, AuthenticationSequenceType sequence) {
        Validate.notNull(sequence, "Couldn't build authentication channel object, because sequence is null");
        String channelId = null;
        AuthenticationSequenceChannelType channelSequence = sequence.getChannel();
        if (channelSequence != null) {
            channelId = channelSequence.getChannelId();
        }

        AbstractChannelFactory factory = registry.findModelFactory(channelId);
        if (factory == null) {
            LOGGER.error("Couldn't find factory for {}", channelId);
            return null;
        }
        AuthenticationChannel channel = null;
        try {
            channel = factory.createAuthChannel(channelSequence);
        } catch (Exception e) {
            LOGGER.error("Couldn't create channel for {}", channelId);
        }
        return channel;
    }

//    public static ModuleAuthentication createDefaultAuthenticationModule() {
//        LoginFormModuleAuthentication moduleAuthentication = new LoginFormModuleAuthentication();
//        moduleAuthentication.setPrefix(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH
//                + ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_FOR_DEFAULT_MODULE + SecurityPolicyUtil.DEFAULT_MODULE_NAME + "/");
//        moduleAuthentication.setNameOfModule(SecurityPolicyUtil.DEFAULT_MODULE_NAME);
//        return moduleAuthentication;
//    }

    public static Map<String, String> obtainAnswers(String answers, String idParameter, String answerParameter) {
        if (answers == null) {
            return null;
        }

        JSONArray answersList = new JSONArray(answers);
        Map<String, String> questionAnswers = new HashMap<String, String>();
        for (int i = 0; i < answersList.length(); i++) {
            JSONObject answer = answersList.getJSONObject(i);
            String questionId = answer.getString(idParameter);
            String questionAnswer = answer.getString(answerParameter);
            questionAnswers.put(questionId, questionAnswer);
        }
        return questionAnswers;
    }

    public static void resolveProxyUserOidHeader(HttpServletRequest request) {
        String proxyUserOid = request.getHeader(PROXY_USER_OID_HEADER);

        Authentication actualAuth = SecurityContextHolder.getContext().getAuthentication();

        if (proxyUserOid != null && actualAuth instanceof MidpointAuthentication) {
            ModuleAuthentication moduleAuth = ((MidpointAuthentication) actualAuth).getProcessingModuleAuthentication();
            if (moduleAuth != null && moduleAuth instanceof HttpModuleAuthentication) {
                ((HttpModuleAuthentication) moduleAuth).setProxyUserOid(proxyUserOid);
            }
        }
    }

    private static Task createAnonymousTask(String operation, TaskManager manager) {
        Task task = manager.createTaskInstance(operation);
        task.setChannel(SchemaConstants.CHANNEL_GUI_USER_URI);
        return task;
    }

    public static UserType searchUserPrivileged(String username, SecurityContextManager securityContextManager, TaskManager manager,
                                            ModelService modelService, PrismContext prismContext) {
        UserType userType = securityContextManager.runPrivileged(new Producer<UserType>() {
            ObjectQuery query = prismContext.queryFor(UserType.class).item(UserType.F_NAME)
                    .eqPoly(username).matchingNorm().build();

            @Override
            public UserType run() {

                Task task = createAnonymousTask("load user", manager);
                OperationResult result = new OperationResult("search user");

                SearchResultList<PrismObject<UserType>> users;
                try {
                    users = modelService.searchObjects(UserType.class, query, null, task, result);
                } catch (SchemaException | ObjectNotFoundException | SecurityViolationException
                        | CommunicationException | ConfigurationException | ExpressionEvaluationException e) {
                    LoggingUtils.logException(LOGGER, "failed to search user", e);
                    return null;
                }

                if ((users == null) || (users.isEmpty())) {
                    LOGGER.trace("Empty user list in ForgetPassword");
                    return null;
                }

                if (users.size() > 1) {
                    LOGGER.trace("Problem while seeking for user");
                    return null;
                }

                UserType user = users.iterator().next().asObjectable();
                LOGGER.trace("User found for ForgetPassword: {}", user);

                return user;
            }

        });
        return userType;
    }

    public static  SecurityPolicyType resolveSecurityPolicy(PrismObject<UserType> user, SecurityContextManager securityContextManager, TaskManager manager,
                                                       ModelInteractionService modelInteractionService) {
        SecurityPolicyType securityPolicy = securityContextManager.runPrivileged(new Producer<SecurityPolicyType>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SecurityPolicyType run() {

                Task task = createAnonymousTask("get security policy", manager);
                OperationResult result = new OperationResult("get security policy");

                try {
                    return modelInteractionService.getSecurityPolicy(user, task, result);
                } catch (CommonException e) {
                    LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                    return null;
                }

            }

        });

        return securityPolicy;
    }

    public static boolean isIgnoredLocalPath(AuthenticationsPolicyType authenticationsPolicy, HttpServletRequest httpRequest) {
        if (authenticationsPolicy != null && authenticationsPolicy.getIgnoredLocalPath() != null
                && !authenticationsPolicy.getIgnoredLocalPath().isEmpty()) {
            List<String> ignoredPaths = authenticationsPolicy.getIgnoredLocalPath();
            for (String ignoredPath : ignoredPaths) {
                AntPathRequestMatcher matcher = new AntPathRequestMatcher(ignoredPath);
                if (matcher.matches(httpRequest)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String getPathForLogoutWithContextPath(String contextPath, @NotNull ModuleAuthentication moduleAuthentication) {
        return "/" + stripSlashes(contextPath) + getPathForLogout(moduleAuthentication);
    }

    public static String getPathForLogout(@NotNull ModuleAuthentication moduleAuthentication) {
        return "/" + stripSlashes(moduleAuthentication.getPrefix()) + DEFAULT_LOGOUT_PATH;
    }

     public static ModuleAuthentication getAuthenticatedModule() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            for (ModuleAuthentication moduleAuthentication : mpAuthentication.getAuthentications()) {
                if (StateOfModule.SUCCESSFULLY.equals(moduleAuthentication.getState())) {
                    return moduleAuthentication;
                }
            }
        } else {
            String message = "Unsupported type " + (authentication == null ? null : authentication.getClass().getName())
                    + " of authentication for MidpointLogoutRedirectFilter, supported is only MidpointAuthentication";
            throw new IllegalArgumentException(message);
        }
        return null;
    }

    public static boolean isBasePathForSequence(HttpServletRequest httpRequest, AuthenticationSequenceType sequence) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (!localePath.startsWith(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH)) {
            return false;
        }
        String defaultPrefix = ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH;
        int startIndex = localePath.indexOf(defaultPrefix) + defaultPrefix.length();
        localePath = localePath.substring(startIndex);
        if (sequence == null || sequence.getChannel() == null || sequence.getChannel().getUrlSuffix() == null
            || !stripSlashes(localePath).equals(stripSlashes(sequence.getChannel().getUrlSuffix()))) {
            return false;
        }
        return true;
    }
}

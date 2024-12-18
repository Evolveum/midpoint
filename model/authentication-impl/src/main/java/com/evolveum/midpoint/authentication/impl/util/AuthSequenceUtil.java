/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.util;

import java.util.*;
import java.util.stream.Collectors;

import com.github.openjson.JSONArray;
import com.github.openjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.authorization.DescriptorLoaderImpl;
import com.evolveum.midpoint.authentication.impl.factory.channel.AbstractChannelFactory;
import com.evolveum.midpoint.authentication.impl.factory.channel.AuthChannelRegistryImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.HttpModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.configuration.ModuleWebSecurityConfigurationImpl;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lazyman
 * @author lskublik
 */
public class AuthSequenceUtil {

    private static final Trace LOGGER = TraceManager.getTrace(AuthSequenceUtil.class);
    private static final String PROXY_USER_OID_HEADER = "Switch-To-Principal";

    private static final Map<String, String> LOCAL_PATH_AND_CHANNEL;

    static {
        LOCAL_PATH_AND_CHANNEL = ImmutableMap.<String, String>builder()
                .put("ws", SchemaConstants.CHANNEL_REST_URI)
                .put("rest", SchemaConstants.CHANNEL_REST_URI)
                .put("api", SchemaConstants.CHANNEL_REST_URI)
                .put("actuator", SchemaConstants.CHANNEL_ACTUATOR_URI)
                .put("resetPassword", SchemaConstants.CHANNEL_RESET_PASSWORD_URI)
                .put("registration", SchemaConstants.CHANNEL_SELF_REGISTRATION_URI)
                .put("invitation", SchemaConstants.CHANNEL_INVITATION_URI)
                .put("identityRecovery", SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI)
                .build();
    }

    public static boolean isPathForChannel(String path, String channel) {
        return LOCAL_PATH_AND_CHANNEL.get(path).equals(channel);
    }

    public static AuthenticationSequenceType getSequenceByPath(HttpServletRequest httpRequest, AuthenticationsPolicyType authenticationPolicy,
            Collection<ObjectReferenceType> nodeGroups) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (authenticationPolicy == null || authenticationPolicy.getSequence() == null
                || authenticationPolicy.getSequence().isEmpty()) {
            return null;
        }
        String[] partsOfLocalPath = AuthUtil.stripStartingSlashes(localePath).split("/");

        if (isClusterSequence(httpRequest)) {
            return getSpecificSequence(httpRequest);
        }

        List<AuthenticationSequenceType> sequences = getSequencesForNodeGroups(nodeGroups, authenticationPolicy);
        if (sequences.isEmpty()) {
            LOGGER.error("Not found any sequence for node group " + nodeGroups + ". Please see your configuration and define "
                    + "authentication sequence for this node group by defining attribute 'nodeGroup'. When will be attribute "
                    + "'nodeGroup' empty, then sequence will be used for all nodes.");
            return null;
        }

        if (partsOfLocalPath.length >= 2 && partsOfLocalPath[0].equals(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE)) {
            AuthenticationSequenceType sequence = searchSequenceComparingUrlSuffix(partsOfLocalPath[1], sequences);
            if (sequence == null) {
                LOGGER.debug("Couldn't find sequence by prefix {}, so try default channel", partsOfLocalPath[1]);
                sequence = AuthUtil.searchSequenceComparingChannelId(SecurityPolicyUtil.DEFAULT_CHANNEL, sequences);
            }
            return sequence;
        }
        String usedChannel = searchChannelByPath(localePath);
        return AuthUtil.searchSequenceComparingChannelId(usedChannel, sequences);
    }

    public static List<AuthenticationSequenceType> getSequencesForNodeGroups(Collection<ObjectReferenceType> nodeGroups,
            AuthenticationsPolicyType authenticationPolicy) {
        Set<String> nodeGroupsOid = nodeGroups.stream()
                .map(ObjectReferenceType::getOid)
                .collect(Collectors.toSet());

        List<AuthenticationSequenceType> sequences = new ArrayList<>();
        authenticationPolicy.getSequence().forEach(sequence -> {
            if (sequence != null) {
                if (sequence.getNodeGroup().isEmpty()) {
                    addSequenceToPoll(sequences, sequence, false);
                } else {
                    for (ObjectReferenceType nodeGroup : sequence.getNodeGroup()) {
                        if (nodeGroup != null && nodeGroup.getOid() != null && !nodeGroup.getOid().isEmpty()
                                && nodeGroupsOid.contains(nodeGroup.getOid())) {
                            addSequenceToPoll(sequences, sequence, true);
                            return;
                        }
                    }
                }
            }
        });
        return sequences;
    }

    private static void addSequenceToPoll(List<AuthenticationSequenceType> sequences, AuthenticationSequenceType addingSequence, boolean replace) {
        if (sequences.isEmpty()) {
            sequences.add(addingSequence);
            return;
        } else if (addingSequence == null) {
            throw new IllegalArgumentException("Comparing sequence is null");
        }
        boolean isDefaultAddSeq = Boolean.TRUE.equals(addingSequence.getChannel().isDefault());
        String suffixOfAddSeq = addingSequence.getChannel().getUrlSuffix();
        String channelAddSeq = addingSequence.getChannel().getChannelId();
        for (AuthenticationSequenceType actualSequence : sequences) {
            boolean isDefaultActSeq = Boolean.TRUE.equals(actualSequence.getChannel().isDefault());
            String suffixOfActSeq = actualSequence.getChannel().getUrlSuffix();
            String channelActSeq = actualSequence.getChannel().getChannelId();
            if (channelAddSeq == null || !channelAddSeq.equals(channelActSeq)) {
                continue;
            }
            if (suffixOfAddSeq.equalsIgnoreCase(suffixOfActSeq)) {
                if (replace) {
                    sequences.remove(actualSequence);
                    sequences.add(addingSequence);
                }
                return;
            }
            if (isDefaultAddSeq && isDefaultActSeq) {
                if (replace) {
                    sequences.remove(actualSequence);
                    sequences.add(addingSequence);
                }
                return;
            }
        }
        sequences.add(addingSequence);
    }

    public static String searchChannelByPath(String localePath) {
        for (String prefix : LOCAL_PATH_AND_CHANNEL.keySet()) {
            if (AuthUtil.stripStartingSlashes(localePath).startsWith(prefix)) {
                return LOCAL_PATH_AND_CHANNEL.get(prefix);
            }
        }
        return SecurityPolicyUtil.DEFAULT_CHANNEL;
    }

    public static String searchPathByChannel(String searchChannel) {
        for (Map.Entry<String, String> entry : LOCAL_PATH_AND_CHANNEL.entrySet()) {
            if (entry.getValue().equals(searchChannel)) {
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
        if (SchemaConstants.CHANNEL_REST_URI.equals(channel)) {
            String header = httpRequest.getHeader("Authorization");
            if (header != null) {
                String type = header.split(" ")[0];
                if (AuthenticationModuleNameConstants.CLUSTER.equalsIgnoreCase(type)) {
                    AuthenticationSequenceType sequence = new AuthenticationSequenceType();
                    sequence.setIdentifier(AuthenticationModuleNameConstants.CLUSTER);
                    AuthenticationSequenceChannelType seqChannel = new AuthenticationSequenceChannelType();
                    seqChannel.setUrlSuffix(AuthenticationModuleNameConstants.CLUSTER.toLowerCase());
                    seqChannel.setChannelId(SchemaConstants.CHANNEL_REST_URI);
                    sequence.setChannel(seqChannel);
                    return sequence;
                }
            }
        }
        return null;
    }

    public static boolean isClusterSequence(HttpServletRequest httpRequest) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        String channel = searchChannelByPath(localePath);
        if (SchemaConstants.CHANNEL_REST_URI.equals(channel)) {
            String header = httpRequest.getHeader("Authorization");
            if (header != null) {
                String type = header.split(" ")[0];
                return AuthenticationModuleNameConstants.CLUSTER.equalsIgnoreCase(type);
            }
        }
        return false;
    }



    private static AuthenticationSequenceType searchSequenceComparingUrlSuffix(String urlSuffix, List<AuthenticationSequenceType> sequences) {
        Validate.notBlank(urlSuffix, "UrlSuffix for searching of sequence is blank");
        AuthenticationSequenceType foundSequence = null;
        for (AuthenticationSequenceType sequence : sequences) {
            if (sequence != null && sequence.getChannel() != null && urlSuffix.equals(sequence.getChannel().getUrlSuffix())) {
                if (sequence.getModule() == null || sequence.getModule().isEmpty()) {
                    LOGGER.error("Found sequence " + sequence.getName() + "not contains configuration for module");
                    return null;
                }
                foundSequence = sequence;
                break;
            }
        }
        if (foundSequence != null
                && foundSequence.getChannel().isDefault() == null
                && foundSequence.getChannel().getChannelId() != null) {
            AuthenticationSequenceType finalFoundSequence = foundSequence;
            long count = sequences.stream().filter(sequence -> sequence.getChannel() != null
                    && finalFoundSequence.getChannel().getChannelId().equals(sequence.getChannel().getChannelId())).count();
            if (count == 1) {
                AuthenticationSequenceType sequence = foundSequence.clone();
                sequence.getChannel().setDefault(true);
                return sequence;
            }
        }
        return foundSequence;
    }

    public static boolean isPermitAll(HttpServletRequest request) {
        for (String url : DescriptorLoaderImpl.getPermitAllUrls()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return true;
            }
        }
        String servletPath = request.getServletPath();
        // Special case, this is in fact "magic" redirect to home page or login page. It handles autz in its own way.
        return "".equals(servletPath) || "/".equals(servletPath);
    }

    public static boolean isLoginPage(HttpServletRequest request) {
        for (String url : DescriptorLoaderImpl.getLoginPages()) {
            AntPathRequestMatcher matcher = new AntPathRequestMatcher(url);
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }

    public static void saveException(HttpServletRequest request,
            AuthenticationException exception) {
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

    public static Map<String, String> obtainAnswers(String answers, String idParameter, String answerParameter) {
        if (StringUtils.isEmpty(answers)) {
            return null;
        }

        JSONArray answersList = new JSONArray(answers);
        Map<String, String> questionAnswers = new HashMap<>();
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
            if (moduleAuth instanceof HttpModuleAuthentication) {
                ((HttpModuleAuthentication) moduleAuth).setProxyUserOid(proxyUserOid);
            }
        }
    }

    private static Task createAnonymousTask(String operation, TaskManager manager) {
        Task task = manager.createTaskInstance(operation);
        task.setChannel(SchemaConstants.CHANNEL_USER_URI);
        return task;
    }

    public static UserType searchUserPrivileged(String username, SecurityContextManager securityContextManager, TaskManager manager,
            ModelService modelService, PrismContext prismContext) {
        return securityContextManager.runPrivileged((Producer<UserType>) () -> {
            Task task = createAnonymousTask("load user", manager);
            OperationResult result = new OperationResult("search user");

            SearchResultList<PrismObject<UserType>> users;
            try {
                final ObjectQuery query = prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).eqPoly(username).matchingNorm()
                        .build();

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
        });
    }

    public static NonceCredentialsPolicyType determineNoncePolicy(PrismObject<UserType> user,
            String nonceName, TaskManager manager,
            ModelInteractionService modelInteractionService) {

            Task task = createAnonymousTask("get security policy", manager);
            OperationResult result = new OperationResult("get security policy");

            try {
                return modelInteractionService.determineNonceCredentialsPolicy(user, nonceName, task, result);
            } catch (CommonException e) {
                LOGGER.error("Could not retrieve security policy: {}", e.getMessage(), e);
                return null;
            }
    }

    public static boolean isBasePathForSequence(HttpServletRequest httpRequest, AuthenticationSequenceType sequence) {
        String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if (!localePath.startsWith(ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH)) {
            return false;
        }
        String defaultPrefix = ModuleWebSecurityConfigurationImpl.DEFAULT_PREFIX_OF_MODULE_WITH_SLASH;
        int startIndex = localePath.indexOf(defaultPrefix) + defaultPrefix.length();
        localePath = localePath.substring(startIndex);
        return sequence != null && sequence.getChannel() != null && sequence.getChannel().getUrlSuffix() != null
                && AuthUtil.stripSlashes(localePath).equals(AuthUtil.stripSlashes(sequence.getChannel().getUrlSuffix()));
    }

    public static boolean isRecordSessionLessAccessChannel(HttpServletRequest httpRequest) {
        if (httpRequest != null) {
            if (isClusterSequence(httpRequest)) {
                return true;
            }
            String localePath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
            String channel = AuthSequenceUtil.searchChannelByPath(localePath);
            return SecurityUtil.isRecordSessionLessAccessChannel(channel);
        }
        return false;
    }

    public static boolean existLoginPageForActualAuthModule() {
        ModuleAuthentication authModule = AuthUtil.getProcessingModuleIfExist();
        if (authModule == null) {
            return false;
        }
        String moduleType = authModule.getModuleTypeName();
        return DescriptorLoaderImpl.existPageUrlByAuthName(moduleType);
    }

    public static boolean isLoginPageForActualAuthModule(String url) {
        ModuleAuthentication authModule = AuthUtil.getProcessingModule();
        String moduleType = authModule.getModuleTypeName();
        return DescriptorLoaderImpl.getPageUrlsByAuthName(moduleType).contains(url);
    }

    public static String getName(PrismObject<? extends FocusType> user) {
        if (user == null || user.asObjectable().getName() == null) {
            return null;
        }
        return user.asObjectable().getName().getOrig();
    }

    public static String getBasePath(HttpServletRequest request) {
        boolean includePort = true;
        if (443 == request.getServerPort() && "https".equals(request.getScheme())) {
            includePort = false;
        } else if (80 == request.getServerPort() && "http".equals(request.getScheme())) {
            includePort = false;
        }
        return request.getScheme() +
                "://" +
                request.getServerName() +
                (includePort ? (":" + request.getServerPort()) : "") +
                request.getContextPath();
    }

    public static boolean isAllowUpdatingAuthBehavior(boolean isUpdatingDuringUnsuccessfulLogin) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return true;
        }
        if (mpAuthentication.getSequence() != null) {
            FocusBehaviorUpdateType actualOption = mpAuthentication.getSequence().getFocusBehaviorUpdate();
            if (actualOption == null || FocusBehaviorUpdateType.ENABLED.equals(actualOption)) {
                return true;
            } else if (FocusBehaviorUpdateType.DISABLED.equals(actualOption)) {
                return false;
            } else if (FocusBehaviorUpdateType.FAILURE_ONLY.equals(actualOption)) {
                return isUpdatingDuringUnsuccessfulLogin;
            }
        }
        return true;
    }

    public static String getAuthSequenceIdentifier(@NotNull AuthenticationSequenceType seq) {
        return StringUtils.isNotEmpty(seq.getIdentifier()) ? seq.getIdentifier() : seq.getName();
    }

    public static boolean isUrlForAuthProcessing(HttpServletRequest httpRequest) {
        String localPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        return localPath.startsWith(SchemaConstants.AUTH_MODULE_PREFIX);
    }
}

/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.security;

import javax.xml.datatype.Duration;

import com.evolveum.midpoint.model.common.archetypes.ArchetypeManager;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import com.evolveum.midpoint.schema.util.ArchetypeTypeUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelAuditRecorder;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.repo.common.AuditHelper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.ConnectionEnvironment;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.List;

/**
 * @author semancik
 */
@Component
public class SecurityHelper implements ModelAuditRecorder {

    private static final Trace LOGGER = TraceManager.getTrace(SecurityHelper.class);

    @Autowired private TaskManager taskManager;
    @Autowired private AuditHelper auditHelper;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private PrismContext prismContext;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private ArchetypeManager archetypeManager;

    @Override
    public void auditLoginSuccess(@NotNull ObjectType object, @NotNull ConnectionEnvironment connEnv) {
        FocusType focus = null;
        if (object instanceof FocusType){
            focus = (FocusType) object;
        }
        auditLogin(object.getName().getOrig(), focus, connEnv, OperationResultStatus.SUCCESS, null);
    }

    @Override
    public void auditLoginFailure(@Nullable String username, @Nullable FocusType focus, @NotNull ConnectionEnvironment connEnv, String message) {
        auditLogin(username, focus, connEnv, OperationResultStatus.FATAL_ERROR, message);
    }

    private void auditLogin(@Nullable String username, @Nullable FocusType focus, @NotNull ConnectionEnvironment connEnv, @NotNull OperationResultStatus status,
            @Nullable String message) {
        String channel = connEnv.getChannel();
        if (!SecurityUtil.isAuditedLoginAndLogout(getSystemConfig(), channel)) {
            return;
        }
        Task task = taskManager.createTaskInstance();
        task.setChannel(channel);

        LOGGER.debug("Login {} username={}, channel={}: {}",
                status == OperationResultStatus.SUCCESS ? "success" : "failure", username,
                connEnv.getChannel(), message);

        AuditEventRecord record = new AuditEventRecord(AuditEventType.CREATE_SESSION, AuditEventStage.REQUEST);
        record.setParameter(username);
        if (focus != null) {
            record.setInitiator(focus.asPrismObject());
        }
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(status);
        record.setMessage(message);
        storeConnectionEnvironment(record, connEnv);

        try {
            auditHelper.audit(record, null, task, new OperationResult(SecurityHelper.class.getName() + ".auditLogin"));
        } catch (Exception e) {
            LOGGER.error("Couldn't audit audit event because of malformed username: " + username, e);
            String normalizedUsername = new PolyString(username).getNorm();
            LOGGER.info("Normalization of username and create audit record with normalized username. Normalized username: " + normalizedUsername);
            record.setParameter(normalizedUsername);
            auditHelper.audit(record, null, task, new OperationResult(SecurityHelper.class.getName() + ".auditLogin"));
        }
    }

    @Override
    public void auditLogout(ConnectionEnvironment connEnv, Task task, OperationResult result) {
        if (!SecurityUtil.isAuditedLoginAndLogout(getSystemConfig(), connEnv.getChannel())) {
            return;
        }
        AuditEventRecord record = new AuditEventRecord(AuditEventType.TERMINATE_SESSION, AuditEventStage.REQUEST);
        PrismObject<? extends FocusType> taskOwner = task.getOwner(result);
        record.setInitiatorAndLoginParameter(taskOwner);
        record.setTimestamp(System.currentTimeMillis());
        record.setOutcome(OperationResultStatus.SUCCESS);
        storeConnectionEnvironment(record, connEnv);
        try {
            auditHelper.audit(record, null, task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't audit audit event", e);
        }
    }

    private SystemConfigurationType getSystemConfig() {
        SystemConfigurationType system = null;
        try {
            system = systemObjectCache.getSystemConfiguration(new OperationResult("LOAD SYSTEM CONFIGURATION")).asObjectable();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get system configuration from cache", e);
        }
        return system;
    }

    private void storeConnectionEnvironment(AuditEventRecord record, ConnectionEnvironment connEnv) {
        record.setChannel(connEnv.getChannel());
        record.setSessionIdentifier(connEnv.getSessionId());
        HttpConnectionInformation connInfo = connEnv.getConnectionInformation();
        if (connInfo != null) {
            record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
            record.setHostIdentifier(connInfo.getLocalHostName());
        }
    }

    /**
     * Returns security policy applicable for the specified focus if specified. It looks for organization, archetype and global policies and
     * takes into account deprecated properties and password policy references. The resulting security policy has all the
     * (non-deprecated) properties set. If there is also referenced value policy, it is will be stored as "object" in the value
     * policy reference inside the returned security policy.
     *
     * If no focus is specified, returns the security policy referenced with the archetype and merged with the global security policy
     */
    public <F extends FocusType> SecurityPolicyType locateSecurityPolicy(PrismObject<F> focus, String archetypeOid,
            PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        SecurityPolicyType globalSecurityPolicy = locateGlobalSecurityPolicy(focus, systemConfiguration, task, result);

        SecurityPolicyType mergedSecurityPolicy = null;

        if (focus != null) {
            mergedSecurityPolicy = resolveSecurityPolicyForFocus(focus, globalSecurityPolicy, task, result);
        } else if (StringUtils.isNotEmpty(archetypeOid)) {
            mergedSecurityPolicy = resolveSecurityPolicyForArchetype(archetypeOid, globalSecurityPolicy, task, result);
        }

        if (mergedSecurityPolicy != null) {
            traceSecurityPolicy(mergedSecurityPolicy, focus);
            return mergedSecurityPolicy;
        }

        if (globalSecurityPolicy != null) {
            traceSecurityPolicy(globalSecurityPolicy, focus);
            return globalSecurityPolicy;
        }

        return null;
    }

    private <F extends FocusType> SecurityPolicyType resolveSecurityPolicyForFocus(PrismObject<F> focus,
            SecurityPolicyType globalSecurityPolicy, Task task, OperationResult result) throws SchemaException{
        SecurityPolicyType securityPolicyFromOrgs = locateFocusSecurityPolicyFromOrgs(focus, task, result);
        SecurityPolicyType mergedSecurityPolicy = mergeSecurityPolicies(securityPolicyFromOrgs, globalSecurityPolicy);  //sec policy from org overrides global sec policy
        SecurityPolicyType securityPolicyFromArchetypes = locateFocusSecurityPolicyFromArchetypes(focus, task, result);
        mergedSecurityPolicy = mergeSecurityPolicies(securityPolicyFromArchetypes, mergedSecurityPolicy);  //sec policy from archetypes overrides sec policy from org
        return mergedSecurityPolicy;
    }

    /**
     * Returns security policy referenced from the archetype and merged with the global security policy referenced from the
     * system configuration.
     */
    private SecurityPolicyType resolveSecurityPolicyForArchetype(String archetypeOid, SecurityPolicyType globalSecurityPolicy,
            Task task, OperationResult result) throws SchemaException {
        try {
            var archetype = archetypeManager.getArchetype(archetypeOid, result);
            var shortDescription = "load security policy from archetype";
            var archetypeSecurityPolicy = loadArchetypeSecurityPolicy(archetype, shortDescription, task, result);

            if (archetypeSecurityPolicy == null) {
                return null;
            }

            return mergeSecurityPolicies(archetypeSecurityPolicy, globalSecurityPolicy);
        } catch (ObjectNotFoundException e) {
            LOGGER.error("Cannot load archetype object, ", e);
        }
        return null;
    }

    private <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromOrgs(
            PrismObject<F> focus, Task task, OperationResult result) throws SchemaException {
        PrismObject<SecurityPolicyType> orgSecurityPolicy = objectResolver.searchOrgTreeWidthFirstReference(
                focus,
                org -> {
                    // For create-on-demand feature used in "safe" preview mode (the default), the org returned here may be null.
                    // (It is unlike real simulations that go a step further, and provide in-memory object here.)
                    // If the org is null, we have nothing to do; we simply return null policy reference.
                    return org != null ? org.asObjectable().getSecurityPolicyRef() : null;
                },
                "security policy",
                task,
                result);
        LOGGER.trace("Found organization security policy: {}", orgSecurityPolicy);
        if (orgSecurityPolicy != null) {
            SecurityPolicyType orgSecurityPolicyType = orgSecurityPolicy.asObjectable();
            postProcessSecurityPolicy(orgSecurityPolicyType, task, result);
            return orgSecurityPolicyType;
        } else {
            return null;
        }
    }

    private <F extends FocusType> SecurityPolicyType locateFocusSecurityPolicyFromArchetypes(PrismObject<F> focus, Task task,
            OperationResult result) throws SchemaException {
        SecurityPolicyType archetypeSecurityPolicy = searchSecurityPolicyFromArchetype(focus,
                "security policy", task, result);
        LOGGER.trace("Found archetype security policy: {}", archetypeSecurityPolicy);
        if (archetypeSecurityPolicy != null) {
//            SecurityPolicyType securityPolicyType = archetypeSecurityPolicy.asObjectable();
            postProcessSecurityPolicy(archetypeSecurityPolicy, task, result);
            return archetypeSecurityPolicy;
        } else {
            return null;
        }
    }

    private <O extends ObjectType> SecurityPolicyType searchSecurityPolicyFromArchetype(PrismObject<O> object,
            String shortDesc, Task task, OperationResult result) throws SchemaException {
        if (object == null) {
            LOGGER.trace("No object provided. Cannot find security policy specific for an object.");
            return null;
        }
        ArchetypeType structuralArchetype = ArchetypeTypeUtil.getStructuralArchetype(archetypeManager.determineArchetypes(object.asObjectable(), result));
        if (structuralArchetype == null) {
            return null;
        }
        return loadArchetypeSecurityPolicy(structuralArchetype, shortDesc, task, result);
    }

    private SecurityPolicyType loadArchetypeSecurityPolicy(ArchetypeType archetype,
            String shortDesc, Task task, OperationResult result) throws SchemaException {
        PrismObject<SecurityPolicyType> securityPolicy;
        ObjectReferenceType securityPolicyRef = archetype.getSecurityPolicyRef();
        if (securityPolicyRef == null) {
            return null;
        }
        try {
            securityPolicy = objectResolver.resolve(securityPolicyRef.asReferenceValue(), shortDesc, task, result);
        } catch (ObjectNotFoundException ex) {
            LOGGER.warn("Cannot find security policy referenced in archetype {}, oid {}", archetype.getName(),
                    archetype.getOid());
            return null;
        }
        return mergeSecurityPolicyWithSuperArchetype(archetype, securityPolicy.asObjectable(), task, result);
    }

    private SecurityPolicyType mergeSecurityPolicyWithSuperArchetype(ArchetypeType archetype, SecurityPolicyType securityPolicy,
            Task task, OperationResult result) {
        ArchetypeType superArchetype = null;
        try {
            superArchetype = archetype.getSuperArchetypeRef() != null ?
                    objectResolver.resolve(archetype.getSuperArchetypeRef(), ArchetypeType.class, null, "resolving super archetype ref", task, result)
                    : null;
        } catch (Exception ex) {
            LOGGER.warn("Cannot resolve super archetype reference for archetype {}, oid {}", archetype.getName(), archetype.getOid());
            return securityPolicy;
        }
        if (superArchetype == null) {
            return securityPolicy;
        }
        SecurityPolicyType superArchetypeSecurityPolicy = null;
        try {
            superArchetypeSecurityPolicy = superArchetype.getSecurityPolicyRef() != null ?
                    objectResolver.resolve(superArchetype.getSecurityPolicyRef(), SecurityPolicyType.class, null, "resolving security policy ref", task, result)
                    : null;
        } catch (Exception ex) {
            LOGGER.warn("Cannot resolve security policy reference for archetype {}, oid {}", superArchetype.getName(), superArchetype.getOid());
            return securityPolicy;
        }
        if (superArchetypeSecurityPolicy == null) {
            return securityPolicy;
        }
        SecurityPolicyType mergedSecurityPolicy = mergeSecurityPolicies(securityPolicy, superArchetypeSecurityPolicy);
        return mergeSecurityPolicyWithSuperArchetype(superArchetype, mergedSecurityPolicy, task, result);
    }

    private SecurityPolicyType mergeSecurityPolicies(SecurityPolicyType lowLevelSecurityPolicy,
            SecurityPolicyType topLevelSecurityPolicy) {
        if (lowLevelSecurityPolicy == null && topLevelSecurityPolicy == null) {
            return null;
        }
        if (topLevelSecurityPolicy == null) {
            return lowLevelSecurityPolicy.cloneWithoutId();
        }
        if (lowLevelSecurityPolicy == null) {
            return topLevelSecurityPolicy.cloneWithoutId();
        }

        SecurityPolicyType mergedSecurityPolicy = lowLevelSecurityPolicy.cloneWithoutId();
        AuthenticationsPolicyType mergedAuthentication = mergeSecurityPolicyAuthentication(lowLevelSecurityPolicy.getAuthentication(),
                topLevelSecurityPolicy.getAuthentication());
        mergedSecurityPolicy.setAuthentication(mergedAuthentication);
        mergedSecurityPolicy.setCredentials(mergeCredentialsPolicy(lowLevelSecurityPolicy.getCredentials(),
                topLevelSecurityPolicy.getCredentials()));
        mergedSecurityPolicy.setCredentialsReset(mergeCredentialsReset(lowLevelSecurityPolicy.getCredentialsReset(),
                topLevelSecurityPolicy.getCredentialsReset()));
        mergedSecurityPolicy.setIdentityRecovery(mergeIdentityRecovery(lowLevelSecurityPolicy.getIdentityRecovery(),
                topLevelSecurityPolicy.getIdentityRecovery()));
//        return mergedSecurityPolicy.asPrismObject();
//        PrismObject<SecurityPolicyType> mergedSecurityPolicy = mergeSecurityPolicies(lowLevelSecurityPolicy.asPrismObject(), topLevelSecurityPolicy.asPrismObject());
        return mergedSecurityPolicy;// != null ? mergedSecurityPolicy.asObjectable() : null;
    }

    /**
     *
     * @param lowLevelSecurityPolicy    means the security policy referenced from child archetype
     * @param topLevelSecurityPolicy    means the security policy referenced from super archetype
     * @return
     */
//    private PrismObject<SecurityPolicyType> mergeSecurityPoliciesss(PrismObject<SecurityPolicyType> lowLevelSecurityPolicy,
//            PrismObject<SecurityPolicyType> topLevelSecurityPolicy) {
//        if (lowLevelSecurityPolicy == null && topLevelSecurityPolicy == null) {
//            return null;
//        }
//        if (topLevelSecurityPolicy == null) {
//            return lowLevelSecurityPolicy.clone();
//        }
//        if (lowLevelSecurityPolicy == null) {
//            return topLevelSecurityPolicy.clone();
//        }
//
//    }

    private AuthenticationsPolicyType mergeSecurityPolicyAuthentication(AuthenticationsPolicyType lowLevelAuthentication, AuthenticationsPolicyType topLevelAuthentication) {
        if (lowLevelAuthentication == null && topLevelAuthentication == null) {
            return null;
        }
        if (lowLevelAuthentication == null) {
            return topLevelAuthentication.cloneWithoutId();
        }
        if (topLevelAuthentication == null) {
            return lowLevelAuthentication.cloneWithoutId();
        }
        AuthenticationsPolicyType mergedAuthentication = lowLevelAuthentication.cloneWithoutId();
        mergeAuthenticationModules(mergedAuthentication, topLevelAuthentication.getModules());
        mergeSequences(mergedAuthentication, topLevelAuthentication.getSequence());
        if (CollectionUtils.isEmpty(mergedAuthentication.getIgnoredLocalPath())) {
            mergedAuthentication.getIgnoredLocalPath().addAll(topLevelAuthentication.getIgnoredLocalPath());
        } else {
            mergedAuthentication.getIgnoredLocalPath().addAll(CollectionUtils.union(mergedAuthentication.getIgnoredLocalPath(), topLevelAuthentication.getIgnoredLocalPath()));
        }
        return mergedAuthentication;
    }

    private void mergeAuthenticationModules(AuthenticationsPolicyType mergedAuthentication, AuthenticationModulesType modules) {
        if (modules == null) {
            return;
        }
        if (mergedAuthentication.getModules() == null) {
            mergedAuthentication.setModules(modules);
            return;
        }
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getHttpBasic(), modules.getHttpBasic());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getHttpHeader(), modules.getHttpHeader());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getHttpSecQ(), modules.getHttpSecQ());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getLdap(), modules.getLdap());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getLoginForm(), modules.getLoginForm());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getMailNonce(), modules.getMailNonce());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getOidc(), modules.getOidc());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getOther(), modules.getOther());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getSaml2(), modules.getSaml2());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getSecurityQuestionsForm(), modules.getSecurityQuestionsForm());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getSmsNonce(), modules.getSmsNonce());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getAttributeVerification(), modules.getAttributeVerification());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getHint(), modules.getHint());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getFocusIdentification(), modules.getFocusIdentification());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getArchetypeSelection(), modules.getArchetypeSelection());
        mergeAuthenticationModuleList(mergedAuthentication.getModules().getCorrelation(), modules.getCorrelation());
    }

    private <AM extends AbstractAuthenticationModuleType> void mergeAuthenticationModuleList(List<AM> mergedList, List<AM> listToProcess) {
        if (CollectionUtils.isEmpty(listToProcess)) {
            return;
        }
        if (CollectionUtils.isEmpty(mergedList)) {
            listToProcess.forEach(i -> mergedList.add((AM) i.cloneWithoutId()));
            return;
        }
        listToProcess.forEach(itemToProcess -> {
            boolean exist = false;
            for (AM item : mergedList) {
                String itemIdentifier = StringUtils.isNotEmpty(item.getIdentifier()) ? item.getIdentifier() : item.getName();
                String itemToProcessIdentifier = StringUtils.isNotEmpty(itemToProcess.getIdentifier()) ?
                        itemToProcess.getIdentifier() : itemToProcess.getName();
                if (itemIdentifier != null && StringUtils.equals(itemIdentifier, itemToProcessIdentifier)) {
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                mergedList.add(itemToProcess.cloneWithoutId());
            }
        });
    }

    private void mergeSequences(AuthenticationsPolicyType mergedAuthentication, List<AuthenticationSequenceType> sequences) {
        if (CollectionUtils.isEmpty(sequences)) {
            return;
        }
        if (CollectionUtils.isEmpty(mergedAuthentication.getSequence())) {
            sequences.forEach(s -> mergedAuthentication.getSequence().add(s.cloneWithoutId()));
            return;
        }
        sequences.forEach(sequenceToProcess -> {
            boolean exist = false;
            for (AuthenticationSequenceType sequence : mergedAuthentication.getSequence()) {
                if (sequencesIdentifiersMatch(sequence, sequenceToProcess)) {
                    mergeSequence(sequence, sequenceToProcess);
                    exist = true;
                    break;
                }
            }
            if (!exist) {
                mergedAuthentication.getSequence().add(sequenceToProcess.cloneWithoutId());
            }
        });
    }

    private void mergeSequence(AuthenticationSequenceType sequence, AuthenticationSequenceType sequenceToProcess) {
        if (sequence == null || sequenceToProcess == null || !sequencesIdentifiersMatch(sequence, sequenceToProcess)) {
            return;
        }
        if (sequence.getChannel() == null) {
            sequence.setChannel(sequenceToProcess.getChannel());
        }
        if (StringUtils.isEmpty(sequence.getDescription())) {
            sequence.setDescription(sequenceToProcess.getDescription());
        }
        if (CollectionUtils.isNotEmpty(sequenceToProcess.getModule())) {
            if (CollectionUtils.isEmpty(sequence.getModule())) {
                sequenceToProcess.getModule().forEach(m -> sequence.getModule().add(m.cloneWithoutId()));
            } else {
                sequenceToProcess.getModule().forEach(sequenceModule -> {
                    if (findSequenceModuleByIdentifier(sequence.getModule(), sequenceModule) == null) {
                        sequence.getModule().add(sequenceModule.cloneWithoutId());
                    }
                });
            }
        }
        if (CollectionUtils.isNotEmpty(sequenceToProcess.getNodeGroup()) && CollectionUtils.isEmpty(sequence.getNodeGroup())) {
            sequence.getNodeGroup().addAll(sequenceToProcess.getNodeGroup());
        }
        if (CollectionUtils.isNotEmpty(sequenceToProcess.getRequireAssignmentTarget())
                && CollectionUtils.isEmpty(sequence.getRequireAssignmentTarget())) {
            sequence.getRequireAssignmentTarget().addAll(sequenceToProcess.getRequireAssignmentTarget());
        }
    }

    private boolean sequencesIdentifiersMatch(AuthenticationSequenceType sequence1, AuthenticationSequenceType sequence2) {
        String identifier1 = StringUtils.isNotEmpty(sequence1.getIdentifier()) ? sequence1.getIdentifier() : sequence1.getName();
        String identifier2 = StringUtils.isNotEmpty(sequence2.getIdentifier()) ? sequence2.getIdentifier() : sequence2.getName();
        return identifier1 != null && StringUtils.equals(identifier1, identifier2);
    }

    private AuthenticationSequenceModuleType findSequenceModuleByIdentifier(List<AuthenticationSequenceModuleType> sequenceModules,
            AuthenticationSequenceModuleType moduleToFind) {
        if (CollectionUtils.isEmpty(sequenceModules) || moduleToFind == null || StringUtils.isEmpty(moduleToFind.getIdentifier())) {
            return null;
        }
        for (AuthenticationSequenceModuleType module : sequenceModules) {
            if (moduleToFind.getIdentifier().equals(module.getIdentifier())) {
                return module;
            }
        }
        return null;
    }

    private CredentialsPolicyType mergeCredentialsPolicy(CredentialsPolicyType lowLevelCredentialsPolicy, CredentialsPolicyType topLevelCredentialsPolicy) {
        if (lowLevelCredentialsPolicy == null && topLevelCredentialsPolicy == null) {
            return null;
        }
        if (lowLevelCredentialsPolicy == null) {
            return topLevelCredentialsPolicy.cloneWithoutId();
        }
        CredentialsPolicyType mergedPolicy = new CredentialsPolicyType();
        if (lowLevelCredentialsPolicy.getPassword() != null) {
            mergedPolicy.setPassword(lowLevelCredentialsPolicy.getPassword().cloneWithoutId());
        } else if (topLevelCredentialsPolicy.getPassword() != null) {
            mergedPolicy.setPassword(topLevelCredentialsPolicy.getPassword().cloneWithoutId());
        }
        if (lowLevelCredentialsPolicy.getSecurityQuestions() != null) {
            mergedPolicy.setSecurityQuestions(lowLevelCredentialsPolicy.getSecurityQuestions().cloneWithoutId());
        } else if (topLevelCredentialsPolicy.getSecurityQuestions() != null) {
            mergedPolicy.setSecurityQuestions(topLevelCredentialsPolicy.getSecurityQuestions().cloneWithoutId());
        }
        if (CollectionUtils.isNotEmpty(lowLevelCredentialsPolicy.getNonce())) {
            lowLevelCredentialsPolicy.getNonce().forEach(n -> mergedPolicy.getNonce().add(n.cloneWithoutId()));
        } else if (topLevelCredentialsPolicy.getNonce() != null) {
            topLevelCredentialsPolicy.getNonce().forEach(n -> mergedPolicy.getNonce().add(n.cloneWithoutId()));
        }
        if (lowLevelCredentialsPolicy.getAttributeVerification() != null) {
            mergedPolicy.setAttributeVerification(lowLevelCredentialsPolicy.getAttributeVerification().cloneWithoutId());
        } else if (topLevelCredentialsPolicy.getAttributeVerification() != null) {
            mergedPolicy.setAttributeVerification(topLevelCredentialsPolicy.getAttributeVerification().cloneWithoutId());
        }
        if (lowLevelCredentialsPolicy.getDefault() != null) {
            mergedPolicy.setDefault(lowLevelCredentialsPolicy.getDefault().cloneWithoutId());
        } else if (topLevelCredentialsPolicy.getDefault() != null) {
            mergedPolicy.setDefault(topLevelCredentialsPolicy.getDefault().cloneWithoutId());
        }
        return mergedPolicy;
    }

    private CredentialsResetPolicyType mergeCredentialsReset(CredentialsResetPolicyType lowLevelCredentialsReset, CredentialsResetPolicyType topLevelCredentialsReset) {
        if (lowLevelCredentialsReset != null) {
            return lowLevelCredentialsReset.cloneWithoutId();
        }
        if (topLevelCredentialsReset != null) {
            return topLevelCredentialsReset.cloneWithoutId();
        }
        return null;
    }

    private IdentityRecoveryPolicyType mergeIdentityRecovery(IdentityRecoveryPolicyType lowLevelRecoveryPolicy,
            IdentityRecoveryPolicyType topLevelRecoveryPolicy) {
        if (lowLevelRecoveryPolicy != null) {
            return lowLevelRecoveryPolicy.cloneWithoutId();
        }
        if (topLevelRecoveryPolicy != null) {
            return topLevelRecoveryPolicy.cloneWithoutId();
        }
        return null;
    }

    public <F extends FocusType> SecurityPolicyType locateGlobalSecurityPolicy(PrismObject<F> focus,
            PrismObject<SystemConfigurationType> systemConfiguration, Task task, OperationResult result)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (systemConfiguration == null) {
            return null;
        }
        return resolveGlobalSecurityPolicy(focus, systemConfiguration.asObjectable(), task, result);
    }

    public SecurityPolicyType locateProjectionSecurityPolicy(ResourceObjectDefinition structuralObjectClassDefinition,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        LOGGER.trace("locateProjectionSecurityPolicy starting");
        ObjectReferenceType securityPolicyRef = structuralObjectClassDefinition.getSecurityPolicyRef();
        if (securityPolicyRef == null || securityPolicyRef.getOid() == null) {
            LOGGER.trace("Security policy not defined for the structural object class.");
            return null;
        }
        LOGGER.trace("Loading security policy {} from: {}", securityPolicyRef, structuralObjectClassDefinition);
        // TODO Use read-only option. (But deal with the fact that we modify the returned object...)
        SecurityPolicyType securityPolicy = objectResolver.resolve(securityPolicyRef, SecurityPolicyType.class,
                null, " projection security policy", task, result);
        if (securityPolicy == null) {
            LOGGER.debug("Security policy {} defined for the projection does not exist", securityPolicyRef);
            return null;
        }
        postProcessSecurityPolicy(securityPolicy, task, result);
        return securityPolicy;
    }

    private <F extends FocusType> SecurityPolicyType resolveGlobalSecurityPolicy(PrismObject<F> focus,
            SystemConfigurationType systemConfiguration, Task task, OperationResult result)
            throws CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (systemConfiguration == null) {
            return null;
        }
        ObjectReferenceType globalSecurityPolicyRef = systemConfiguration.getGlobalSecurityPolicyRef();
        if (globalSecurityPolicyRef != null) {
            try {
                SecurityPolicyType globalSecurityPolicyType = objectResolver.resolve(globalSecurityPolicyRef, SecurityPolicyType.class, null, "global security policy reference in system configuration", task, result);
                LOGGER.trace("Using global security policy: {}", globalSecurityPolicyType);
                postProcessSecurityPolicy(globalSecurityPolicyType, task, result);
                traceSecurityPolicy(globalSecurityPolicyType, focus);
                return globalSecurityPolicyType;
            } catch (ObjectNotFoundException | SchemaException e) {
                LOGGER.error(e.getMessage(), e);
                traceSecurityPolicy(null, focus);
                return null;
            }
        }

        return null;
    }

    private <F extends FocusType> void traceSecurityPolicy(SecurityPolicyType securityPolicyType, PrismObject<F> user) {
        if (LOGGER.isTraceEnabled()) {
            if (user != null) {
                if (securityPolicyType == null) {
                    LOGGER.trace("Located security policy for {}: null", user);
                } else {
                    LOGGER.trace("Located security policy for {}:\n{}", user, securityPolicyType.asPrismObject().debugDump(1));
                }
            } else {
                if (securityPolicyType == null) {
                    LOGGER.trace("Located global security policy null");
                } else {
                    LOGGER.trace("Located global security policy :\n{}", securityPolicyType.asPrismObject().debugDump(1));
                }
            }
        }

    }

    private void postProcessSecurityPolicy(SecurityPolicyType securityPolicyType, Task task, OperationResult result) {
        CredentialsPolicyType creds = securityPolicyType.getCredentials();
        if (creds != null) {
            PasswordCredentialsPolicyType passwd = creds.getPassword();
            if (passwd != null) {
                postProcessPasswordCredentialPolicy(securityPolicyType, passwd, task, result);
            }
            for (NonceCredentialsPolicyType nonce : creds.getNonce()) {
                postProcessCredentialPolicy(securityPolicyType, nonce, "nonce credential policy", task, result);
            }
            SecurityQuestionsCredentialsPolicyType securityQuestions = creds.getSecurityQuestions();
            if (securityQuestions != null) {
                postProcessCredentialPolicy(securityPolicyType, securityQuestions, "security questions credential policy", task, result);
            }
        }
    }

    private void postProcessPasswordCredentialPolicy(SecurityPolicyType securityPolicyType, PasswordCredentialsPolicyType passwd, Task task, OperationResult result) {
        postProcessCredentialPolicy(securityPolicyType, passwd, "password credential policy", task, result);
    }

    private ValuePolicyType postProcessCredentialPolicy(SecurityPolicyType securityPolicyType, CredentialPolicyType credPolicy, String credShortDesc, Task task, OperationResult result) {
        ObjectReferenceType valuePolicyRef = credPolicy.getValuePolicyRef();
        if (valuePolicyRef == null) {
            return null;
        }
        ValuePolicyType valuePolicyType;
        try {
            valuePolicyType = objectResolver.resolve(valuePolicyRef, ValuePolicyType.class, null, credShortDesc + " in " + securityPolicyType, task, result);
        } catch (ObjectNotFoundException | SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException e) {
            LOGGER.warn("{} {} referenced from {} was not found", credShortDesc, valuePolicyRef.getOid(), securityPolicyType);
            return null;
        }
        valuePolicyRef.asReferenceValue().setObject(valuePolicyType.asPrismObject());
        return valuePolicyType;
    }

    private SecurityPolicyType postProcessPasswordPolicy(ValuePolicyType passwordPolicyType) {
        SecurityPolicyType securityPolicyType = new SecurityPolicyType();
        CredentialsPolicyType creds = new CredentialsPolicyType();
        PasswordCredentialsPolicyType passwd = new PasswordCredentialsPolicyType();
        ObjectReferenceType passwordPolicyRef = new ObjectReferenceType();
        passwordPolicyRef.asReferenceValue().setObject(passwordPolicyType.asPrismObject());
        passwd.setValuePolicyRef(passwordPolicyRef);
        creds.setPassword(passwd);
        securityPolicyType.setCredentials(creds);
        return securityPolicyType;
    }

    private Duration daysToDuration(int days) {
        return XmlTypeConverter.createDuration((long) days * 1000 * 60 * 60 * 24);
    }

    public SecurityEnforcer getSecurityEnforcer() {
        return securityEnforcer;
    }
}

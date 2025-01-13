/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.securitypolicy;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Merges two {@link SecurityPolicyType} objects.
 *
 * Currently does *NOT* use the generic merger, it is custom (handwritten) for now.
 *
 * TODO consider rewriting to use the generic merger
 */
public class SecurityPolicyCustomMerger {

    public static SecurityPolicyType mergeSecurityPolicies(
            SecurityPolicyType mostSpecific, SecurityPolicyType lessSpecific, SecurityPolicyType generic) {
        return mergeSecurityPolicies(mostSpecific, mergeSecurityPolicies(lessSpecific, generic));
    }

    public static SecurityPolicyType mergeSecurityPolicies(SecurityPolicyType specific, SecurityPolicyType generic) {
        if (specific == null && generic == null) {
            return null;
        }
        if (generic == null) {
            return specific.cloneWithoutId();
        }
        if (specific == null) {
            return generic.cloneWithoutId();
        }
        SecurityPolicyType merged = specific.cloneWithoutId();
        merged.setAuthentication(mergeSecurityPolicyAuthentication(specific.getAuthentication(), generic.getAuthentication()));
        merged.setCredentials(mergeCredentialsPolicy(specific.getCredentials(), generic.getCredentials()));
        merged.setCredentialsReset(mergeCredentialsReset(specific.getCredentialsReset(), generic.getCredentialsReset()));
        merged.setIdentityRecovery(mergeIdentityRecovery(specific.getIdentityRecovery(), generic.getIdentityRecovery()));
        merged.setFlow(mergeFlow(specific.getFlow(), generic.getFlow()));
        return merged;
    }

    private static AuthenticationsPolicyType mergeSecurityPolicyAuthentication(
            AuthenticationsPolicyType specific, AuthenticationsPolicyType generic) {
        if (specific == null && generic == null) {
            return null;
        }
        if (specific == null) {
            return generic.cloneWithoutId();
        }
        if (generic == null) {
            return specific.cloneWithoutId();
        }
        AuthenticationsPolicyType merged = specific.cloneWithoutId();
        mergeAuthenticationModules(merged, generic.getModules());
        mergeSequences(merged, generic.getSequence());
        if (CollectionUtils.isEmpty(merged.getIgnoredLocalPath())) {
            merged.getIgnoredLocalPath().addAll(generic.getIgnoredLocalPath());
        } else {
            merged.getIgnoredLocalPath().addAll(
                    CollectionUtils.union(merged.getIgnoredLocalPath(), generic.getIgnoredLocalPath()));
        }
        return merged;
    }

    private static void mergeAuthenticationModules(AuthenticationsPolicyType mergedAuthentication, AuthenticationModulesType modules) {
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

    private static <AM extends AbstractAuthenticationModuleType> void mergeAuthenticationModuleList(List<AM> mergedList, List<AM> listToProcess) {
        if (CollectionUtils.isEmpty(listToProcess)) {
            return;
        }
        if (CollectionUtils.isEmpty(mergedList)) {
            listToProcess.forEach(i -> mergedList.add(i.cloneWithoutId()));
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

    private static void mergeSequences(AuthenticationsPolicyType mergedAuthentication, List<AuthenticationSequenceType> sequences) {
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

    private static void mergeSequence(AuthenticationSequenceType sequence, AuthenticationSequenceType sequenceToProcess) {
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

    private static boolean sequencesIdentifiersMatch(AuthenticationSequenceType sequence1, AuthenticationSequenceType sequence2) {
        String identifier1 = StringUtils.isNotEmpty(sequence1.getIdentifier()) ? sequence1.getIdentifier() : sequence1.getName();
        String identifier2 = StringUtils.isNotEmpty(sequence2.getIdentifier()) ? sequence2.getIdentifier() : sequence2.getName();
        return identifier1 != null && StringUtils.equals(identifier1, identifier2);
    }

    private static AuthenticationSequenceModuleType findSequenceModuleByIdentifier(List<AuthenticationSequenceModuleType> sequenceModules,
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

    private static CredentialsPolicyType mergeCredentialsPolicy(CredentialsPolicyType specific, CredentialsPolicyType generic) {
        if (specific == null && generic == null) {
            return null;
        }
        if (specific == null) {
            return generic.cloneWithoutId();
        }
        CredentialsPolicyType mergedPolicy = new CredentialsPolicyType();
        if (specific.getPassword() != null) {
            mergedPolicy.setPassword(specific.getPassword().cloneWithoutId());
        } else if (generic.getPassword() != null) {
            mergedPolicy.setPassword(generic.getPassword().cloneWithoutId());
        }
        if (specific.getSecurityQuestions() != null) {
            mergedPolicy.setSecurityQuestions(specific.getSecurityQuestions().cloneWithoutId());
        } else if (generic.getSecurityQuestions() != null) {
            mergedPolicy.setSecurityQuestions(generic.getSecurityQuestions().cloneWithoutId());
        }
        if (CollectionUtils.isNotEmpty(specific.getNonce())) {
            specific.getNonce().forEach(n -> mergedPolicy.getNonce().add(n.cloneWithoutId()));
        } else if (generic.getNonce() != null) {
            generic.getNonce().forEach(n -> mergedPolicy.getNonce().add(n.cloneWithoutId()));
        }
        if (specific.getAttributeVerification() != null) {
            mergedPolicy.setAttributeVerification(specific.getAttributeVerification().cloneWithoutId());
        } else if (generic.getAttributeVerification() != null) {
            mergedPolicy.setAttributeVerification(generic.getAttributeVerification().cloneWithoutId());
        }
        if (specific.getDefault() != null) {
            mergedPolicy.setDefault(specific.getDefault().cloneWithoutId());
        } else if (generic.getDefault() != null) {
            mergedPolicy.setDefault(generic.getDefault().cloneWithoutId());
        }
        return mergedPolicy;
    }

    private static CredentialsResetPolicyType mergeCredentialsReset(
            CredentialsResetPolicyType specific, CredentialsResetPolicyType generic) {
        if (specific != null) {
            return specific.cloneWithoutId();
        }
        if (generic != null) {
            return generic.cloneWithoutId();
        }
        return null;
    }

    private static IdentityRecoveryPolicyType mergeIdentityRecovery(
            IdentityRecoveryPolicyType specific, IdentityRecoveryPolicyType generic) {
        if (specific != null) {
            return specific.cloneWithoutId();
        }
        if (generic != null) {
            return generic.cloneWithoutId();
        }
        return null;
    }

    private static RegistrationsPolicyType mergeFlow(RegistrationsPolicyType specific, RegistrationsPolicyType generic) {
        if (specific != null) {
            return specific.cloneWithoutId();
        }
        if (generic != null) {
            return generic.cloneWithoutId();
        }
        return null;
    }
}

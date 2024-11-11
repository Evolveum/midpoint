/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.stringpolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.model.api.validator.StringLimitationResult.extractMessages;

import static com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil.getMetadata;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Evaluator that validates the value of any object property. The validation means a checks whether
 * the value is a valid for that property. It usually applies to credentials such as passwords.
 * But it can be used also for other properties.
 *
 * (We may need to move this class to the model-impl.
 * User template will be probably needed for this.)
 *
 * This class is directly responsible for "meta" checking like minOccurs, minAge, history.
 * Specific "string syntax" checks like length, number of special characters and so on are delegated
 * to ValuePolicyProcessor.
 *
 * Methods for validation provided here have a bit unusual treating of OperationResult:
 * they add their subresult to the parent result (as usual) but also return it to the caller.
 *
 * @author semancik
 *
 */
public class ObjectValuePolicyEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectValuePolicyEvaluator.class);

    private static final String OPERATION_VALIDATE_VALUE = ObjectValuePolicyEvaluator.class.getName() + ".validateValue";

    private final Protector protector;

    private final ValuePolicyProcessor valuePolicyProcessor;

    private final SecurityPolicyType securityPolicy;

    private final XMLGregorianCalendar now;

    private final ObjectBasedValuePolicyOriginResolver<?> originResolver;

    // We need to get old credential as a configuration. We cannot determine it
    // from the "object". E.g. in case of addition the object is the new object that
    // is just being added. The password will conflict with itself.
    private final AbstractCredentialType oldCredential;

    private final String shortDesc;

    private final Task task;

    private final ItemName credentialName;

    private final CredentialPolicyType credentialPolicy;

    private final ValuePolicyType valuePolicy;

    private ObjectValuePolicyEvaluator(Builder builder) throws SchemaException {
        protector = builder.protector;
        valuePolicyProcessor = builder.valuePolicyProcessor;
        securityPolicy = builder.securityPolicy;
        now = builder.now;
        originResolver = builder.originResolver;
        oldCredential = builder.oldCredential;
        shortDesc = builder.shortDesc;
        task = builder.task;

        credentialName = determineCredentialName(builder.valueItemPath);
        credentialPolicy = determineCredentialPolicy(credentialName, securityPolicy);
        valuePolicy = determineValuePolicy(builder.valuePolicy, credentialPolicy);
    }

    private static ItemName determineCredentialName(ItemPath valueItemPath) {
        if (valueItemPath != null && valueItemPath.startsWithName(FocusType.F_CREDENTIALS)) {
            Object secondPathSegment = valueItemPath.getSegment(1);
            if (ItemPath.isName(secondPathSegment)) {
                return ItemPath.toName(secondPathSegment);
            }
        }
        return null;
    }

    private static CredentialPolicyType determineCredentialPolicy(ItemName credentialName, SecurityPolicyType securityPolicy)
            throws SchemaException {
        if (QNameUtil.match(CredentialsType.F_PASSWORD, credentialName)) {
            return SecurityUtil.getEffectivePasswordCredentialsPolicy(securityPolicy);
        } else if (QNameUtil.match(CredentialsType.F_NONCE, credentialName)) {
            return SecurityUtil.getEffectiveNonceCredentialsPolicy(securityPolicy);
        } else {
            return null;
        }
    }

    private static ValuePolicyType determineValuePolicy(ValuePolicyType providedValuePolicy, CredentialPolicyType credentialPolicy) {
        if (providedValuePolicy != null) {
            return providedValuePolicy;
        }

        if (credentialPolicy != null) {
            ObjectReferenceType valuePolicyRef = credentialPolicy.getValuePolicyRef();
            if (valuePolicyRef != null) {
                PrismObject<ValuePolicyType> valuePolicyObj = valuePolicyRef.asReferenceValue().getObject();
                if (valuePolicyObj != null) {
                    return valuePolicyObj.asObjectable();
                }
            }
        }

        // TODO: check value policy from the schema (definition)

        return null;
    }

    public Protector getProtector() {
        return protector;
    }

    public ValuePolicyProcessor getValuePolicyProcessor() {
        return valuePolicyProcessor;
    }

    public SecurityPolicyType getSecurityPolicy() {
        return securityPolicy;
    }

    public XMLGregorianCalendar getNow() {
        return now;
    }

    public String getShortDesc() {
        return shortDesc;
    }

    public Task getTask() {
        return task;
    }

    // Beware: minOccurs is not checked here; it has to be done globally over all values. See validateMinOccurs method.
    public OperationResult validateProtectedStringValue(ProtectedStringType value, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return validateStringValue(getClearValue(value), parentResult);
    }

    // Beware: minOccurs is not checked here; it has to be done globally over all values. See validateMinOccurs method.
    public OperationResult validateStringValue(String clearValue, OperationResult parentResult) throws SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_VALIDATE_VALUE);
        try {
            List<LocalizableMessage> messages = new ArrayList<>();

            validateMinAge(messages, result);
            validateHistory(clearValue, messages, result);
            validateStringPolicy(clearValue, messages, result);

            generateResultMessage(messages, result);
            return result;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void generateResultMessage(List<LocalizableMessage> messages, OperationResult result) {
        result.computeStatus();
        if (!result.isSuccess() && !messages.isEmpty()) {
            result.setUserFriendlyMessage(
                    new LocalizableMessageListBuilder()
                            .messages(messages)
                            .separator(LocalizableMessageList.SPACE)
                            .buildOptimized());
        }
    }

    public OperationResult validateMinOccurs(int valuesCount, OperationResult parentResult) {
        OperationResult result = parentResult.createMinorSubresult(OPERATION_VALIDATE_VALUE);
        try {
            List<LocalizableMessage> messages = new ArrayList<>();

            validateMinOccurs(valuesCount, messages, result);

            generateResultMessage(messages, result);
            return result;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void validateMinAge(List<LocalizableMessage> messages, OperationResult result) {
        if (oldCredential == null) {
            return;
        }
        var minAge = getMinAge();
        var lastChangeTimestamp = getLastChangeTimestamp();
        if (minAge != null && lastChangeTimestamp != null) {
            XMLGregorianCalendar changeAllowedTimestamp = XmlTypeConverter.addDuration(lastChangeTimestamp, minAge);
            if (changeAllowedTimestamp.compare(now) == DatatypeConstants.GREATER) {
                LOGGER.trace("Password minAge violated. lastChange={}, minAge={}, now={}", lastChangeTimestamp, minAge, now);
                LocalizableMessage msg = LocalizableMessageBuilder.buildKey("ValuePolicy.minAgeNotReached");
                result.addSubresult(new OperationResult("Password minimal age", OperationResultStatus.FATAL_ERROR, msg));
                messages.add(msg);
            }
        }
    }

    private @Nullable XMLGregorianCalendar getLastChangeTimestamp() {
        return ValueMetadataTypeUtil.getLastChangeTimestamp(getMetadata(oldCredential));
    }

    private boolean isMaxAgeViolated() {
        if (oldCredential == null) {
            return false;
        }
        var maxAge = getMaxAge();
        var lastChangeTimestamp = getLastChangeTimestamp();
        if (maxAge != null && lastChangeTimestamp != null) {
            XMLGregorianCalendar changeAllowedTimestamp = XmlTypeConverter.addDuration(lastChangeTimestamp, maxAge);
            if (changeAllowedTimestamp.compare(now) == DatatypeConstants.LESSER) {
                LOGGER.trace("Password maxAge violated. lastChange={}, maxAge={}, now={}", lastChangeTimestamp, maxAge, now);
                return true;
            }
        }
        return false;
    }

    private void validateStringPolicy(String clearValue, List<LocalizableMessage> messages, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (clearValue == null) {
            return; // should be checked elsewhere
        }

        if (valuePolicy != null) {
            var results = valuePolicyProcessor.validateValue(
                    clearValue, valuePolicy, originResolver,
                    "focus " + shortDesc + " value policy validation", task, result);
            messages.addAll(
                    extractMessages(results));
        } else {
            LOGGER.trace("Skipping validating {} value. Value policy not specified.", shortDesc);
        }
    }

    private void validateMinOccurs(int values, List<LocalizableMessage> messages, OperationResult result) {
        int minOccurs = getMinOccurs();
        if (values < minOccurs) {       // implies minOccurs > 0
            LocalizableMessage msg;
            if (minOccurs == 1) {
                msg = LocalizableMessageBuilder.buildKey("ValuePolicy.valueMustBePresent");
            } else {
                msg = new LocalizableMessageBuilder()
                        .key("ValuePolicy.valuesMustBePresent")
                        .args(minOccurs, values)
                        .build();
            }
            result.addSubresult(new OperationResult("minOccurs", OperationResultStatus.FATAL_ERROR, msg));
            messages.add(msg);
        }
    }

    private void validateHistory(String clearValue, List<LocalizableMessage> messages, OperationResult result) throws SchemaException {

        if (!CredentialsType.F_PASSWORD.matches(credentialName)) {
            LOGGER.trace("Skipping validating {} history, only password history is supported", shortDesc);
            return;
        }

        int historyLength = SecurityUtil.getCredentialHistoryLength(credentialPolicy);
        if (historyLength == 0) {
            LOGGER.trace("Skipping validating {} history, because history length is set to zero", shortDesc);
            return;
        }

        PasswordType existingPassword = (PasswordType) oldCredential;
        if (existingPassword == null) {
            LOGGER.trace("Skipping validating {} history, because it is empty", shortDesc);
            return;
        }

        ProtectedStringType newPasswordPs = new ProtectedStringType();
        newPasswordPs.setClearValue(clearValue);

        if (passwordEquals(newPasswordPs, existingPassword.getValue())) {
            LOGGER.trace("{} matched current value", shortDesc);

            if (!SecurityUtil.isHistoryAllowExistingPasswordReuse(credentialPolicy) || isMaxAgeViolated()) {
                // existing password can be reused even when stored in focus, it has to be valid according to maxAge setting
                appendHistoryViolationMessage(messages, result);
                return;
            }
        }

        //noinspection unchecked
        PrismContainer<PasswordHistoryEntryType> historyContainer = existingPassword.asPrismContainerValue()
                .findContainer(PasswordType.F_HISTORY_ENTRY);
        List<PasswordHistoryEntryType> sortedHistoryList = getSortedHistoryList(historyContainer, false);
        int i = 1;
        for (PasswordHistoryEntryType historyEntry: sortedHistoryList) {
            if (i >= historyLength) {
                // success (history has more entries than needed)
                return;
            }
            if (passwordEquals(newPasswordPs, historyEntry.getValue())) {
                LOGGER.trace("Password history entry #{} matched (changed {})", i, historyEntry.getChangeTimestamp());
                appendHistoryViolationMessage(messages, result);
                return;
            }
            i++;
        }
    }

    private Duration getMinAge() {
        if (credentialPolicy != null) {
            return credentialPolicy.getMinAge();
        } else {
            return null;
        }
    }

    private Duration getMaxAge() {
        if (credentialPolicy != null) {
            return credentialPolicy.getMaxAge();
        } else {
            return null;
        }
    }

    private int getMinOccurs() {
        if (credentialPolicy != null) {
            String minOccursPhrase = credentialPolicy.getMinOccurs();
            Integer minOccurs = XsdTypeMapper.multiplicityToInteger(minOccursPhrase);
            return defaultIfNull(minOccurs, 0);
        } else {
            return 0;
        }
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private List<PasswordHistoryEntryType> getSortedHistoryList(
            PrismContainer<PasswordHistoryEntryType> historyEntries, boolean ascending) {
        if (historyEntries == null || historyEntries.isEmpty()) {
            return Collections.emptyList();
        }
        List<PasswordHistoryEntryType> historyEntryValues = (List<PasswordHistoryEntryType>) historyEntries.getRealValues();

        historyEntryValues.sort((o1, o2) -> {
            XMLGregorianCalendar changeTimestampFirst = o1.getChangeTimestamp();
            XMLGregorianCalendar changeTimestampSecond = o2.getChangeTimestamp();

            if (ascending) {
                return changeTimestampFirst.compare(changeTimestampSecond);
            } else {
                return changeTimestampSecond.compare(changeTimestampFirst);
            }
        });
        return historyEntryValues;
    }

    private void appendHistoryViolationMessage(List<LocalizableMessage> messages, OperationResult result) {
        LocalizableMessage msg = LocalizableMessageBuilder.buildKey("ValuePolicy.valueRecentlyUsed");
        result.addSubresult(new OperationResult("history", OperationResultStatus.FATAL_ERROR, msg));
        messages.add(msg);
    }

    private String getClearValue(ProtectedStringType protectedString) {
        if (protectedString != null) {
            String clearValue = protectedString.getClearValue();
            if (clearValue != null) {
                return clearValue;
            } else if (protectedString.isEncrypted() || protectedString.isExternal()) {
                try {
                    return protector.decryptString(protectedString);
                } catch (EncryptionException e) {
                    throw new SystemException("Failed to decrypt " + shortDesc + ": " + e.getMessage(), e);
                }
            }
        }
        return null;
    }

    private boolean passwordEquals(ProtectedStringType newPasswordPs, ProtectedStringType currentPassword) throws SchemaException {
        if (currentPassword == null) {
            return newPasswordPs == null;
        }
        try {
            return protector.compareCleartext(newPasswordPs, currentPassword);
        } catch (EncryptionException e) {
            throw new SystemException("Failed to compare " + shortDesc + ": " + e.getMessage(), e);
        }
    }

    public static final class Builder {
        private Protector protector;
        private ValuePolicyProcessor valuePolicyProcessor;
        private SecurityPolicyType securityPolicy;
        private XMLGregorianCalendar now;
        private ItemPath valueItemPath;
        private ObjectBasedValuePolicyOriginResolver<?> originResolver;
        private AbstractCredentialType oldCredential;
        private String shortDesc;
        private Task task;
        private ValuePolicyType valuePolicy;

        public Builder protector(Protector val) {
            protector = val;
            return this;
        }

        public Builder valuePolicyProcessor(ValuePolicyProcessor val) {
            valuePolicyProcessor = val;
            return this;
        }

        public Builder securityPolicy(SecurityPolicyType val) {
            securityPolicy = val;
            return this;
        }

        public Builder now(XMLGregorianCalendar val) {
            now = val;
            return this;
        }

        public Builder valueItemPath(ItemPath val) {
            valueItemPath = val;
            return this;
        }

        public Builder originResolver(ObjectBasedValuePolicyOriginResolver<?> val) {
            originResolver = val;
            return this;
        }

        public Builder oldCredential(AbstractCredentialType val) {
            oldCredential = val;
            return this;
        }

        public Builder shortDesc(String val) {
            shortDesc = val;
            return this;
        }

        public Builder task(Task val) {
            task = val;
            return this;
        }

        public Builder valuePolicy(ValuePolicyType val) {
            valuePolicy = val;
            return this;
        }

        public ObjectValuePolicyEvaluator build() throws SchemaException {
            return new ObjectValuePolicyEvaluator(this);
        }
    }
}

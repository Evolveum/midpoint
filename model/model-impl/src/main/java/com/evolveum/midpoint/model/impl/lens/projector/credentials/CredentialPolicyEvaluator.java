/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.stringpolicy.*;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordHistoryEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Processor for evaluating credential policies on the focus object.
 *
 * This class is processing the credential-related settings of security policy: credential lifetime, history and so on.
 *
 * Specific responsibilities:
 *
 * 1) Validates the operation (add, modify) with regards to security policies.
 * 2) Emits secondary deltas related to changes being executed (e.g. password history, metadata, etc).
 *
 * This class is supposed to be quite generic. It should be able to operate on all credential types.
 *
 * This class does NOT directly deal with details of value policies, validation and generation. That task is
 * delegated to {@link ValuePolicyProcessor}.
 *
 * @author mamut
 * @author katkav
 * @author semancik
 */
public abstract class CredentialPolicyEvaluator<R extends AbstractCredentialType, P extends CredentialPolicyType,
        F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(CredentialPolicyEvaluator.class);

    private static final ItemPath CREDENTIAL_RELATIVE_VALUE_PATH = PasswordType.F_VALUE;

    // Configuration

    private final PrismContext prismContext;
    private final Protector protector;
    private final LocalizationService localizationService;
    private final OperationalDataManager metadataManager;
    private final ValuePolicyProcessor valuePolicyProcessor;
    private final ModelObjectResolver resolver;
    private final LensContext<F> context;
    private final XMLGregorianCalendar now;
    private final Task task;
    private final OperationResult result;

    // State

    private ObjectValuePolicyEvaluator objectValuePolicyEvaluator;
    private P credentialPolicy;

    CredentialPolicyEvaluator(Builder<F> builder) {
        this.prismContext = builder.prismContext;
        this.protector = builder.protector;
        this.localizationService = builder.localizationService;
        this.metadataManager = builder.metadataManager;
        this.valuePolicyProcessor = builder.valuePolicyProcessor;
        this.resolver = builder.resolver;
        this.context = builder.context;
        this.now = builder.now;
        this.task = builder.task;
        this.result = builder.result;
    }

    /**
     * E.g. "credentials/password"
     */
    protected abstract ItemPath getCredentialsContainerPath();

    /**
     * E.g. "value"
     */
    private ItemPath getCredentialRelativeValuePath() {
        return CREDENTIAL_RELATIVE_VALUE_PATH;
    }

    /**
     * E.g. "credentials/password/value"
     */
    private ItemPath getCredentialValuePath() {
        return getCredentialsContainerPath().append(getCredentialRelativeValuePath());
    }

    protected abstract String getCredentialHumanReadableName();

    protected abstract String getCredentialHumanReadableKey();

    protected boolean supportsHistory() {
        return false;
    }

    private P getCredentialPolicy() throws SchemaException {
        if (credentialPolicy == null) {
            credentialPolicy = determineEffectiveCredentialPolicy();
        }
        return credentialPolicy;
    }

    protected abstract P determineEffectiveCredentialPolicy() throws SchemaException;

    protected SecurityPolicyType getSecurityPolicy() {
        return context.getFocusContext().getSecurityPolicy();
    }

    private ObjectValuePolicyEvaluator getObjectValuePolicyEvaluator() throws SchemaException {
        if (objectValuePolicyEvaluator == null) {
            PrismObject<F> object = context.getFocusContext().getObjectAny();
            objectValuePolicyEvaluator = new ObjectValuePolicyEvaluator.Builder()
                    .now(now)
                    .originResolver(new FocusValuePolicyOriginResolver<>(object, resolver))
                    .protector(protector)
                    .securityPolicy(getSecurityPolicy())
                    .shortDesc(getCredentialHumanReadableName() + " for " + object)
                    .task(task)
                    .valueItemPath(getCredentialValuePath())
                    .valuePolicyProcessor(valuePolicyProcessor)
                    .oldCredential(getOldCredential())
                    .build();
        }
        return objectValuePolicyEvaluator;
    }

    /**
     * Main entry point.
     */
    public void process() throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException,
            PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext.isAdd()) {
            processAdd(focusContext);
        } else if (focusContext.isModify()) {
            processModify(focusContext);
        } else if (focusContext.isDelete()) {
            LOGGER.trace("Skipping processing {} policies. Focus will be deleted.", getCredentialHumanReadableName());
        }
    }

    private void processAdd(LensFocusContext<F> focusContext)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, PolicyViolationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        if (focusContext.wasAddExecuted()) {
            LOGGER.trace("Skipping processing {} policies. Focus addition was already executed.", getCredentialHumanReadableName());
        } else {
            PrismObject<F> focus = focusContext.getObjectNew();
            PrismContainer<R> credentialsContainer = focus.findContainer(getCredentialsContainerPath());
            if (credentialsContainer != null) {
                for (PrismContainerValue<R> cVal : credentialsContainer.getValues()) {
                    processCredentialContainerValue(cVal);
                }
            }
            validateMinOccurs(credentialsContainer);
        }
    }

    private void processModify(LensFocusContext<F> focusContext)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException,
            PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        boolean credentialValueChanged = false;
        boolean checkMinOccurs = false;
        ObjectDelta<F> focusDelta = focusContext.getCurrentDelta();
        ContainerDelta<R> containerDelta = focusDelta != null ? focusDelta.findContainerDelta(getCredentialsContainerPath()) : null; // e.g. credentials/password
        if (containerDelta != null) {
            if (containerDelta.isAdd()) {
                for (PrismContainerValue<R> cVal : containerDelta.getValuesToAdd()) {
                    credentialValueChanged = true;
                    processCredentialContainerValue(cVal);
                }
            }
            if (containerDelta.isReplace()) {
                for (PrismContainerValue<R> cVal : containerDelta.getValuesToReplace()) {
                    credentialValueChanged = true;
                    processCredentialContainerValue(cVal);
                }
                checkMinOccurs = true;
            }
            if (containerDelta.isDelete()) {
                checkMinOccurs = true;
            }
        } else {
            if (hasValueDelta(focusDelta, getCredentialsContainerPath())) {
                credentialValueChanged = isCredentialValueChanged(focusDelta);
                checkMinOccurs = true; // might not be precise (e.g. might check minOccurs even if a value is being added)
                processValueDelta(focusDelta);

                // Do not add metadata to the password and do not append password history, if the new value is same as old
                // (and password reuse in history is ON). This is because modifyTimestamp would change and could not be
                // relied upon with maxAge.
                if (!credentialValueChanged && SecurityUtil.isHistoryAllowExistingPasswordReuse(getCredentialPolicy())) {
                    LOGGER.trace("Skipping processing metadata delta");
                } else {
                    addMetadataDelta();
                }
            }
        }

        if (checkMinOccurs) {
            PrismObject<F> objectNew = getObjectNew(focusContext);
            PrismContainer<R> credentialsContainer = objectNew.findContainer(getCredentialsContainerPath());
            validateMinOccurs(credentialsContainer);
        }
        if (credentialValueChanged) {
            addHistoryDeltas();
        }
    }

    private boolean isCredentialValueChanged(ObjectDelta<F> focusDelta) {
        ProtectedStringType oldPassword;
        ProtectedStringType newPassword = null;

        PropertyDelta<ProtectedStringType> valueDelta = focusDelta.findPropertyDelta(getCredentialValuePath());
        if (valueDelta != null && valueDelta.getValuesToReplace() != null) {
            for (var val : valueDelta.getValuesToReplace()) {
                newPassword = val.getValue();
                break; // password should have only one value
            }
        }

        // in case that password is added and not replaced:
        if (newPassword == null && valueDelta != null && valueDelta.getValuesToAdd() != null) {
            for (var val : valueDelta.getValuesToAdd()) {
                newPassword = val.getValue();
                break; // password should have only one value
            }
        }

        if (getOldCredential() == null) {
            return newPassword != null;
        }

        if (!(getOldCredential() instanceof PasswordType)) {
            // e.g. SecurityQuestionsCredentialsType
            return true;
        }

        oldPassword = ((PasswordType) getOldCredential()).getValue();

        if (newPassword == null) {
            return oldPassword != null;
        }
        try {
            return !protector.compareCleartext(oldPassword, newPassword);
        } catch (EncryptionException | SchemaException e) {
            throw new SystemException("Failed to compare passwords: " + e.getMessage(), e);
        }
    }

    @NotNull
    private PrismObject<F> getObjectNew(LensFocusContext<F> focusContext) throws SchemaException {
        PrismObject<F> objectNew = focusContext.getObjectNew();
        if (objectNew != null) {
            return objectNew;
        }
        PrismObject<F> objectNewAfter = focusContext.getObjectNew();
        if (objectNewAfter != null) {
            return objectNewAfter;
        }
        throw new IllegalStateException("Unexpected null objectNew in " + focusContext);
    }

    /**
     * Process values from credential deltas that add/replace the whole container or even object.
     * E.g. $user/credentials/password, $user/credentials/securityQuestions
     */
    private void processCredentialContainerValue(PrismContainerValue<R> cVal) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, PolicyViolationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        addMissingMetadata(cVal);
        validateCredentialContainerValues(cVal);
    }

    private void validateMinOccurs(PrismContainer<R> credentialsContainer) throws SchemaException, PolicyViolationException {
        int valuesCount = getValuesCount(credentialsContainer);
        OperationResult validationResult = getObjectValuePolicyEvaluator().validateMinOccurs(valuesCount, result);
        processValidationResult(validationResult);
    }

    // e.g. for checking minOccurs; override for non-standard cases
    private int getValuesCount(PrismContainer<R> credentialsContainer) {
        Collection<PrismValue> allValues = Item.getAllValues(credentialsContainer, getCredentialRelativeValuePath());
        // todo check for duplicates?
        return MiscUtil.nonNullValues(allValues).size();
    }

    /**
     * Process values from credential deltas that add/replace values below value container
     * E.g. $user/credentials/password/value,
     *      $user/credentials/securityQuestions/questionAnswer
     *      $user/credentials/securityQuestions/questionAnswer/questionAnswer
     *
     *  This implementation is OK for the password, nonce and similar simple cases. It needs to be
     *  overridden for more complex cases.
     */
    private void processValueDelta(ObjectDelta<F> focusDelta) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        PropertyDelta<ProtectedStringType> valueDelta = focusDelta.findPropertyDelta(getCredentialValuePath());
        if (valueDelta != null) {
            processPropertyValueCollection(valueDelta.getValuesToAdd());
            processPropertyValueCollection(valueDelta.getValuesToReplace());
        } else {
            LOGGER.trace("Skipping processing {} policies. Focus delta does not contain value change.", getCredentialHumanReadableName());
        }
    }

    private void processPropertyValueCollection(Collection<PrismPropertyValue<ProtectedStringType>> collection) throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (collection == null) {
            return;
        }
        for (PrismPropertyValue<ProtectedStringType> val: collection) {
            validateProtectedStringValue(val.getValue());
        }
    }

    protected void validateCredentialContainerValues(PrismContainerValue<R> cVal) throws PolicyViolationException,
            SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        PrismProperty<ProtectedStringType> credentialValueProperty = cVal.findProperty(getCredentialRelativeValuePath());
        if (credentialValueProperty != null) {
            for (PrismPropertyValue<ProtectedStringType> credentialValuePropertyValue : credentialValueProperty.getValues()) {
                validateProtectedStringValue(credentialValuePropertyValue.getValue());
            }
        }
    }

    void validateProtectedStringValue(ProtectedStringType value) throws PolicyViolationException, SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        OperationResult validationResult = getObjectValuePolicyEvaluator().validateProtectedStringValue(value, result);
        processValidationResult(validationResult);
    }

    private void processValidationResult(OperationResult validationResult) throws PolicyViolationException {
        if (!validationResult.isAcceptable()) {
            SingleLocalizableMessage message = new LocalizableMessageBuilder()
                    .key("PolicyViolationException.message.credentials." + getCredentialHumanReadableKey())
                    .arg(validationResult.getUserFriendlyMessage())
                    .build();
            throw localizationService.translate(new PolicyViolationException(message));
        }
    }

    private void addMetadataDelta() throws SchemaException {
        context.getFocusContextRequired().swallowToSecondaryDelta(
                metadataManager.createCredentialsModificationRelatedStorageMetadataDeltas(
                        context,
                        getCredentialsContainerPath(),
                        getCurrentCredential(),
                        context.getFocusClass(), now, task));
    }

    private void addMissingMetadata(PrismContainerValue<R> cVal) throws SchemaException {
        if (hasValueChange(cVal)) {
            if (!cVal.hasValueMetadata()) {
                context.getFocusContext().swallowToSecondaryDelta(
                        prismContext.deltaFactory().container().createModificationAdd(
                                getCredentialsContainerPath().append(InfraItemName.METADATA),
                                FocusType.class,
                                metadataManager.createCreateMetadata(context, now, task)));
            }
        }
    }

    private boolean hasValueDelta(ObjectDelta<F> focusDelta, ItemPath credentialsPath) {
        if (focusDelta == null) {
            return false;
        }
        for (PartiallyResolvedDelta<?,?> partialDelta : focusDelta.findPartial(credentialsPath)) {
            LOGGER.trace("Residual delta:\n{}", partialDelta.debugDumpLazily());
            ItemPath residualPath = partialDelta.getResidualPath();
            if (ItemPath.isEmpty(residualPath)) {
                continue;
            }
            LOGGER.trace("PATH: {}", residualPath);
            ItemName name = residualPath.firstName();
            LOGGER.trace("NAME: {}", name);
            if (isValueElement(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasValueChange(PrismContainerValue<R> cVal) {
        for (Item<?, ?> item : cVal.getItems()) {
            if (isValueElement(item.getElementName())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValueElement(ItemName itemName) {
        return !AbstractCredentialType.F_FAILED_LOGINS.matches(itemName)
                && !AbstractCredentialType.F_LAST_FAILED_LOGIN.matches(itemName)
                && !AbstractCredentialType.F_LAST_SUCCESSFUL_LOGIN.matches(itemName)
                && !AbstractCredentialType.F_METADATA.matches(itemName)
                && !AbstractCredentialType.F_PREVIOUS_SUCCESSFUL_LOGIN.matches(itemName);
    }

    private AbstractCredentialType getOldCredential() {
        PrismContainer<R> oldCredentialContainer = getOldCredentialContainer();
        return oldCredentialContainer != null ? oldCredentialContainer.getRealValue() : null;
    }

    private AbstractCredentialType getCurrentCredential() {
        PrismContainer<R> currentCredentialContainer = getCurrentCredentialContainer();
        return currentCredentialContainer != null ? currentCredentialContainer.getRealValue() : null;
    }

    private PrismContainer<R> getOldCredentialContainer() {
        PrismObject<F> objectOld = context.getFocusContext().getObjectOld();
        return objectOld != null ? objectOld.findContainer(getCredentialsContainerPath()) : null;
    }

    private PrismContainer<R> getCurrentCredentialContainer() {
        PrismObject<F> objectCurrent = context.getFocusContext().getObjectCurrent();
        return objectCurrent != null ? objectCurrent.findContainer(getCredentialsContainerPath()) : null;
    }

    private void addHistoryDeltas() throws SchemaException {
        if (supportsHistory()) {
            PrismContainer<R> oldCredentialContainer = getOldCredentialContainer();
            if (oldCredentialContainer != null) {
                // Note: historyLength=1 means that we need just compare with current password
                // The real number of values stored in the history is historyLength-1
                int historyLength = SecurityUtil.getCredentialHistoryLength(getCredentialPolicy());
                int addedValues = historyLength > 1 ? createAddHistoryDelta(oldCredentialContainer) : 0;
                createDeleteHistoryDeltasIfNeeded(historyLength, addedValues, oldCredentialContainer);
            }
        }
    }

    // TODO: generalize for other credentials
    private int createAddHistoryDelta(PrismContainer<R> oldCredentialContainer) throws SchemaException {
        PrismProperty<ProtectedStringType> oldValueProperty = oldCredentialContainer.findProperty(getCredentialRelativeValuePath());
        if (oldValueProperty == null) {
            return 0;
        }
        ProtectedStringType oldValue = oldValueProperty.getRealValue();
        if (oldValue == null) {
            return 0;
        }
        ProtectedStringType passwordPsForStorage = oldValue.clone();

        CredentialsStorageTypeType storageType = defaultIfNull(
                SecurityUtil.getCredentialStorageTypeType(getCredentialPolicy().getHistoryStorageMethod()),
                CredentialsStorageTypeType.HASHING);
        prepareProtectedStringForStorage(passwordPsForStorage, storageType);

        PrismContainerDefinition<PasswordHistoryEntryType> historyEntryDefinition = oldCredentialContainer.getDefinition().findContainerDefinition(PasswordType.F_HISTORY_ENTRY);
        PrismContainer<PasswordHistoryEntryType> entryContainer = historyEntryDefinition.instantiate();
        PrismContainerValue<PasswordHistoryEntryType> entryPcv = entryContainer.createNewValue();
        PasswordHistoryEntryType entry = entryPcv.asContainerable();
        entry.setValue(passwordPsForStorage);
        entry.asPrismContainerValue().setValueMetadata(
                oldCredentialContainer.getValue().getValueMetadata().clone());
        entry.setChangeTimestamp(now);

        ContainerDelta<PasswordHistoryEntryType> addHistoryDelta = prismContext.deltaFactory().container()
                .createModificationAdd(SchemaConstants.PATH_CREDENTIALS_PASSWORD_HISTORY_ENTRY, FocusType.class, entry.clone());
        context.getFocusContext().swallowToSecondaryDelta(addHistoryDelta);

        return 1;
    }

    // TODO: generalize for other credentials
    private void createDeleteHistoryDeltasIfNeeded(int historyLength, int addedValues, PrismContainer<R> currentCredentialContainer) throws SchemaException {

        PrismContainer<PasswordHistoryEntryType> historyEntriesContainer = currentCredentialContainer.findOrCreateContainer(PasswordType.F_HISTORY_ENTRY);
        List<PrismContainerValue<PasswordHistoryEntryType>> historyEntryValues = historyEntriesContainer.getValues();

        if (!historyEntryValues.isEmpty()) {
            // We need to delete one more entry than intuitively expected - because we are computing from the history entries
            // in the old object. In the new object there will be one new history entry for the changed password.
            int numberOfHistoryEntriesToDelete = historyEntriesContainer.size() - historyLength + addedValues + 1;

            Iterator<PrismContainerValue<PasswordHistoryEntryType>> historyEntryIterator = historyEntryValues.iterator();

            int i = 0;
            while (historyEntryIterator.hasNext() && i < numberOfHistoryEntriesToDelete) {
                ContainerDelta<PasswordHistoryEntryType> deleteHistoryDelta = prismContext.deltaFactory().container()
                        .createModificationDelete(SchemaConstants.PATH_CREDENTIALS_PASSWORD_HISTORY_ENTRY,
                                FocusType.class,
                                historyEntryIterator.next().clone());
                context.getFocusContext().swallowToSecondaryDelta(deleteHistoryDelta);
                i++;
            }
        }
    }

    private void prepareProtectedStringForStorage(ProtectedStringType ps, CredentialsStorageTypeType storageType) throws SchemaException {
        try {
            switch (storageType) {
                case ENCRYPTION:
                    if (ps.isEncrypted() || ps.isExternal()) {
                        break;
                    }
                    if (ps.isHashed()) {
                        throw new SchemaException("Cannot store hashed value in an encrypted form");
                    }
                    protector.encrypt(ps);
                    break;

                case HASHING:
                    if (ps.isHashed()) {
                        break;
                    }
                    protector.hash(ps);
                    break;

                case NONE:
                    throw new SchemaException("Cannot store value on NONE storage form");

                default:
                    throw new SchemaException("Unknown storage type: "+storageType);
            }
        } catch (EncryptionException e) {
            throw new SystemException(e.getMessage(), e);
        }
    }

    public abstract static class Builder<F extends FocusType> {
        private PrismContext prismContext;
        private Protector protector;
        private LocalizationService localizationService;
        private OperationalDataManager metadataManager;
        private ValuePolicyProcessor valuePolicyProcessor;
        private ModelObjectResolver resolver;
        private LensContext<F> context;
        private XMLGregorianCalendar now;
        private Task task;
        private OperationResult result;

        public Builder<F> prismContext(PrismContext val) {
            prismContext = val;
            return this;
        }

        public Builder<F> protector(Protector val) {
            protector = val;
            return this;
        }

        public Builder<F> localizationService(LocalizationService val) {
            localizationService = val;
            return this;
        }

        public Builder<F> metadataManager(OperationalDataManager val) {
            metadataManager = val;
            return this;
        }

        public Builder<F> valuePolicyProcessor(ValuePolicyProcessor val) {
            valuePolicyProcessor = val;
            return this;
        }

        public Builder<F> resolver(ModelObjectResolver val) {
            resolver = val;
            return this;
        }

        public Builder<F> context(LensContext<F> val) {
            context = val;
            return this;
        }

        public Builder<F> now(XMLGregorianCalendar val) {
            now = val;
            return this;
        }

        public Builder<F> task(Task val) {
            task = val;
            return this;
        }

        public Builder<F> result(OperationResult val) {
            result = val;
            return this;
        }

        public abstract CredentialPolicyEvaluator<?, ?, F> build();
    }
}

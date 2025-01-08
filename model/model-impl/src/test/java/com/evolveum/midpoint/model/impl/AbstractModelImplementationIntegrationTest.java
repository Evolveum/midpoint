/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.tools.testng.UnusedTestElement;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * An enhancement of AbstractModelIntegrationTest (i.e. model-api-level test) that contains various useful
 * methods that depend on model-impl functionality - but without importing various objects at initialization time.
 *
 * @author semancik
 */
public class AbstractModelImplementationIntegrationTest extends AbstractModelIntegrationTest {

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }

    protected <O extends ObjectType> LensContext<O> createLensContext(Class<O> focusType) {
        return new LensContext<>(focusType, TaskExecutionMode.PRODUCTION);
    }

    protected LensContext<UserType> createUserLensContext() {
        return new LensContext<>(UserType.class, TaskExecutionMode.PRODUCTION);
    }

    protected <O extends ObjectType> LensFocusContext<O> fillContextWithFocus(
            LensContext<O> context, PrismObject<O> focus) {
        LensFocusContext<O> focusContext = context.getOrCreateFocusContext();
        focusContext.setInitialObject(focus);
        return focusContext;
    }

    protected <O extends ObjectType> LensFocusContext<O> fillContextWithFocus(LensContext<O> context, Class<O> type,
            String userOid, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        PrismObject<O> focus = repositoryService.getObject(type, userOid, null, result);
        return fillContextWithFocus(context, focus);
    }

    protected LensFocusContext<UserType> fillContextWithUser(LensContext<UserType> context, String userOid, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        return fillContextWithFocus(context, UserType.class, userOid, result);
    }

    @UnusedTestElement
    protected <O extends ObjectType> void fillContextWithFocus(LensContext<O> context, File file)
            throws SchemaException, IOException {
        PrismObject<O> user = PrismTestUtil.parseObject(file);
        fillContextWithFocus(context, user);
    }

    protected void fillContextWithEmptyAddUserDelta(LensContext<UserType> context) throws SchemaException {
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createEmptyAddDelta(UserType.class, null);
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        focusContext.setPrimaryDelta(userDelta);
    }

    protected void fillContextWithAddUserDelta(LensContext<UserType> context, PrismObject<UserType> user) throws EncryptionException {
        CryptoUtil.encryptValues(protector, user);
        ObjectDelta<UserType> userDelta = DeltaFactory.Object.createAddDelta(user);
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        focusContext.setPrimaryDelta(userDelta);
    }

    protected <F extends FocusType> void fillContextWithAddDelta(LensContext<F> context, PrismObject<F> object) throws EncryptionException {
        CryptoUtil.encryptValues(protector, object);
        ObjectDelta<F> addDelta = DeltaFactory.Object.createAddDelta(object);
        LensFocusContext<F> focusContext = context.getOrCreateFocusContext();
        focusContext.setPrimaryDelta(addDelta);
    }

    protected LensProjectionContext fillContextWithAccount(
            LensContext<UserType> context, String accountOid, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        // This is better than using repository directly
        var account = provisioningService.getObject(ShadowType.class, accountOid, createNoFetchCollection(), task, result);
        return fillContextWithAccount(context, account, task, result);
    }

    @SuppressWarnings("UnusedReturnValue")
    protected LensProjectionContext fillContextWithAccountFromFile(
            LensContext<UserType> context, File file, Task task, OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, IOException, SchemaException {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(file);
        provisioningService.applyDefinition(account, task, result);
        provisioningService.determineShadowState(account, task, result);
        provisioningService.updateShadowMarksAndPolicies(account, true, task, result);
        return fillContextWithAccount(context, account, task, result);
    }

    protected LensProjectionContext fillContextWithAccount(
            LensContext<UserType> context,
            PrismObject<ShadowType> account,
            Task task,
            OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ShadowType accountType = account.asObjectable();
        String resourceOid = accountType.getResourceRef().getOid();
        ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();
        applyResourceSchema(accountType, resourceType);
        ProjectionContextKey key = // We can use account/default as the default because we are in tests here
                ProjectionContextKey.classified(
                        resourceOid,
                        Objects.requireNonNullElse(accountType.getKind(), ShadowKindType.ACCOUNT),
                        Objects.requireNonNullElse(accountType.getIntent(), SchemaConstants.INTENT_DEFAULT),
                        null);
        LensProjectionContext accountSyncContext = getOrCreateProjectionContext(context, key);
        accountSyncContext.setOid(account.getOid());
        accountSyncContext.setInitialObject(account);
        accountSyncContext.setResource(resourceType);
        accountSyncContext.setExists(true);
        context.rememberResource(resourceType);
        return accountSyncContext;
    }

    private LensProjectionContext getOrCreateProjectionContext(LensContext<UserType> context, ProjectionContextKey key) {
        LensProjectionContext existing = context.findProjectionContextByKeyExact(key);
        if (existing != null) {
            return existing;
        } else {
            return context.createProjectionContext(key);
        }
    }

    protected <O extends ObjectType> ObjectDelta<O> addFocusModificationToContext(
            LensContext<O> context, File file)
            throws SchemaException, IOException {
        ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(
                file, ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<O> focusDelta = DeltaConvertor.createObjectDelta(modElement, context.getFocusClass());
        return addFocusDeltaToContext(context, focusDelta);
    }

    protected <O extends ObjectType> ObjectDelta<O> addFocusDeltaToContext(
            LensContext<O> context, ObjectDelta<O> focusDelta) throws SchemaException {
        LensFocusContext<O> focusContext = context.getOrCreateFocusContext();
        focusContext.addToPrimaryDelta(focusDelta);
        return focusDelta;
    }

    protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(
            LensContext<UserType> context, ItemPath propertyPath, Object... propertyValues)
            throws SchemaException {
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        ObjectDelta<UserType> userDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(UserType.class, focusContext
                        .getObjectOld().getOid(), propertyPath, propertyValues);
        focusContext.addToPrimaryDelta(userDelta);
        return userDelta;
    }

    protected ObjectDelta<UserType> addModificationToContextAddAccountFromFile(
            LensContext<UserType> context, File file)
            throws SchemaException, IOException {
        return addModificationToContextAddProjection(context, UserType.class, file);
    }

    protected <F extends FocusType> ObjectDelta<F> addModificationToContextAddProjection(
            LensContext<F> context, Class<F> focusType, File file)
            throws SchemaException, IOException {
        PrismObject<ShadowType> account = PrismTestUtil.parseObject(file);
        account.trim();
        account.checkConsistence();
        LensFocusContext<F> focusContext = context.getOrCreateFocusContext();
        ObjectDelta<F> userDelta = prismContext.deltaFactory().object().createModificationAddReference(focusType, focusContext
                .getObjectOld().getOid(), FocusType.F_LINK_REF, account);
        focusContext.addToPrimaryDelta(userDelta);
        return userDelta;
    }

    protected ObjectDelta<ShadowType> addModificationToContextDeleteAccount(
            LensContext<UserType> context, String accountOid) throws SchemaException {
        LensProjectionContext accountCtx = context.findProjectionContextByOid(accountOid);
        ObjectDelta<ShadowType> deleteAccountDelta = prismContext.deltaFactory().object().createDeleteDelta(ShadowType.class,
                accountOid);
        accountCtx.addToPrimaryDelta(deleteAccountDelta);
        return deleteAccountDelta;
    }

    @SafeVarargs
    protected final <T> ObjectDelta<ShadowType> addModificationToContextReplaceAccountAttribute(
            LensContext<UserType> context, String accountOid, String attributeLocalName, T... propertyValues)
            throws SchemaException, ConfigurationException {
        LensProjectionContext accCtx = context.findProjectionContextByOid(accountOid);
        ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName,
                propertyValues);
        accCtx.addToPrimaryDelta(accountDelta);
        return accountDelta;
    }

    @SafeVarargs
    protected final <T> ObjectDelta<ShadowType> addSyncModificationToContextReplaceAccountAttribute(
            LensContext<UserType> context, String accountOid, String attributeLocalName, T... propertyValues)
            throws SchemaException, ConfigurationException {
        LensProjectionContext accCtx = context.findProjectionContextByOid(accountOid);
        ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName, propertyValues);
        accCtx.addAccountSyncDelta(accountDelta);
        return accountDelta;
    }

    protected ObjectDelta<UserType> addModificationToContextAssignRole(
            LensContext<UserType> context, String userOid, String roleOid)
            throws SchemaException {
        return addModificationToContextAssignRole(context, userOid, roleOid, null);
    }

    protected ObjectDelta<UserType> addModificationToContextAssignRole(
            LensContext<UserType> context, String userOid, String roleOid, Consumer<AssignmentType> modificationBlock)
            throws SchemaException {
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid,
                roleOid, RoleType.COMPLEX_TYPE, null, modificationBlock, true);
        focusContext.addToPrimaryDelta(userDelta);
        return userDelta;
    }

    protected ObjectDelta<UserType> addModificationToContextUnassignRole(
            LensContext<UserType> context, String userOid, String roleOid)
            throws SchemaException {
        LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
        ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid,
                roleOid, RoleType.COMPLEX_TYPE, null, null, null, false);
        focusContext.addToPrimaryDelta(userDelta);
        return userDelta;
    }

    private <T> ObjectDelta<ShadowType> createAccountDelta(
            LensProjectionContext accCtx, String accountOid, String attributeLocalName, T... propertyValues)
            throws SchemaException, ConfigurationException {
        ResourceType resourceType = accCtx.getResource();
        QName attrQName = new QName(MidPointConstants.NS_RI, attributeLocalName);
        ItemPath attrPath = ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName);
        ResourceObjectDefinition refinedAccountDefinition = accCtx.getCompositeObjectDefinitionRequired();
        ShadowSimpleAttributeDefinition<T> attrDef = refinedAccountDefinition.findSimpleAttributeDefinition(attrQName);
        assertNotNull("No definition of attribute " + attrQName + " in account def " + refinedAccountDefinition, attrDef);
        ObjectDelta<ShadowType> accountDelta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountOid);
        PropertyDelta<T> attrDelta = prismContext.deltaFactory().property().create(attrPath, attrDef);
        attrDelta.setValuesToReplace(PrismValueCollectionsUtil.createCollection(prismContext, propertyValues));
        accountDelta.addModification(attrDelta);
        return accountDelta;
    }

    /**
     * Breaks user assignment delta in the context by inserting some empty value. This may interfere with comparing the values to
     * existing user values.
     */
    protected <F extends FocusType> void breakAssignmentDelta(LensContext<F> context) throws SchemaException {
        LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> userPrimaryDelta = focusContext.getPrimaryDelta();
        breakAssignmentDelta(userPrimaryDelta);
    }

    protected void makeImportSyncDelta(LensProjectionContext accContext) {
        PrismObject<ShadowType> syncAccountToAdd = accContext.getObjectOld().clone();
        ObjectDelta<ShadowType> syncDelta = DeltaFactory.Object.createAddDelta(syncAccountToAdd);
        accContext.setSyncDelta(syncDelta);
    }

    protected void assertNoUserPrimaryDelta(LensContext<UserType> context) {
        LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
        if (userPrimaryDelta == null) {
            return;
        }
        assertTrue("User primary delta is not empty", userPrimaryDelta.isEmpty());
    }

    protected void assertUserSecondaryDelta(LensContext<UserType> context) {
        LensFocusContext<UserType> focusContext = context.getFocusContext();
        ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
        assertNotNull("User secondary delta is null", userSecondaryDelta);
        assertFalse("User secondary delta is empty", userSecondaryDelta.isEmpty());
    }

    @NotNull
    protected LensContext<UserType> createContextForRoleAssignment(String userOid, String roleOid,
            QName relation, Consumer<AssignmentType> modificationBlock, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return createContextForAssignment(UserType.class, userOid, RoleType.class, roleOid, relation, modificationBlock, result);
    }

    @NotNull
    protected <F extends FocusType> LensContext<F> createContextForRoleAssignment(Class<F> focusClass,
            String userOid, String roleOid,
            QName relation, Consumer<AssignmentType> modificationBlock,
            Consumer<ObjectDelta<F>> deltaModifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return createContextForAssignment(focusClass, userOid, RoleType.class, roleOid, relation, modificationBlock,
                deltaModifier, result);
    }

    @NotNull
    protected <F extends FocusType> LensContext<F> createContextForAssignment(Class<F> focusClass,
            String focusOid, Class<? extends FocusType> targetClass, String targetOid,
            QName relation, Consumer<AssignmentType> modificationBlock, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        return createContextForAssignment(focusClass, focusOid, targetClass, targetOid, relation, modificationBlock, null, result);
    }

    @NotNull
    protected <F extends FocusType> LensContext<F> createContextForAssignment(Class<F> focusClass,
            String focusOid, Class<? extends FocusType> targetClass, String targetOid,
            QName relation, Consumer<AssignmentType> modificationBlock,
            Consumer<ObjectDelta<F>> deltaModifier, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        LensContext<F> context = createLensContext(focusClass);
        fillContextWithFocus(context, focusClass, focusOid, result);
        QName targetType = prismContext.getSchemaRegistry().determineTypeForClass(targetClass);
        assertNotNull("Unknown target class " + targetClass, targetType);
        ObjectDelta<F> primaryDelta = createAssignmentFocusDelta(focusClass, focusOid, targetOid, targetType, relation, modificationBlock, true);
        if (deltaModifier != null) {
            deltaModifier.accept(primaryDelta);
        }
        addFocusDeltaToContext(context, primaryDelta);
        displayDumpable("Input context", context);
        assertFocusModificationSanity(context);
        return context;
    }

    protected List<CaseWorkItemType> getWorkItemsForCase(String caseOid,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result) throws SchemaException {
        // TODO use simple getObject(CaseType)
        return repositoryService.searchContainers(CaseWorkItemType.class,
                prismContext.queryFor(CaseWorkItemType.class).ownerId(caseOid).build(),
                options, result);

    }
}

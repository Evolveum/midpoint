/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.impl;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.Consumer;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.*;

/**
 * An enhancement of AbstractModelIntegrationTest (i.e. model-api-level test) that contains various useful
 * methods that depend on model-impl functionality - but without importing various objects at initialization time.
 *
 * @author semancik
 * @author mederly
 */
public class AbstractModelImplementationIntegrationTest extends AbstractModelIntegrationTest {

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	protected <O extends ObjectType> LensContext<O> createLensContext(Class<O> focusType) {
		return new LensContext<O>(focusType, prismContext, provisioningService);
	}

	protected LensContext<UserType> createUserLensContext() {
		return new LensContext<UserType>(UserType.class, prismContext, provisioningService);
	}

	protected <O extends ObjectType> LensFocusContext<O> fillContextWithFocus(LensContext<O> context, PrismObject<O> focus)
			throws SchemaException, ObjectNotFoundException {
		LensFocusContext<O> focusContext = context.getOrCreateFocusContext();
		focusContext.setLoadedObject(focus);
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


	protected <O extends ObjectType> void fillContextWithFocus(LensContext<O> context, File file) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, IOException {
		PrismObject<O> user = PrismTestUtil.parseObject(file);
		fillContextWithFocus(context, user);
	}

	protected void fillContextWithEmtptyAddUserDelta(LensContext<UserType> context, OperationResult result) throws SchemaException {
		ObjectDelta<UserType> userDelta = ObjectDelta.createEmptyAddDelta(UserType.class, null, prismContext);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}

	protected void fillContextWithAddUserDelta(LensContext<UserType> context, PrismObject<UserType> user) throws SchemaException, EncryptionException {
		CryptoUtil.encryptValues(protector, user);
		ObjectDelta<UserType> userDelta = ObjectDelta.createAddDelta(user);
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		focusContext.setPrimaryDelta(userDelta);
	}

	protected LensProjectionContext fillContextWithAccount(LensContext<UserType> context, String accountOid, Task task, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        PrismObject<ShadowType> account = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        provisioningService.applyDefinition(account, task, result);
        return fillContextWithAccount(context, account, task, result);
	}

	protected LensProjectionContext fillContextWithAccountFromFile(LensContext<UserType> context, File file, Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, IOException, ExpressionEvaluationException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(file);
		provisioningService.applyDefinition(account, task, result);
		return fillContextWithAccount(context, account, task, result);
	}

    protected LensProjectionContext fillContextWithAccount(LensContext<UserType> context, PrismObject<ShadowType> account, Task task, OperationResult result) throws SchemaException,
		ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	ShadowType accountType = account.asObjectable();
        String resourceOid = accountType.getResourceRef().getOid();
        ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, task, result).asObjectable();
        applyResourceSchema(accountType, resourceType);
        ResourceShadowDiscriminator rat = new ResourceShadowDiscriminator(resourceOid,
        		ShadowKindType.ACCOUNT, ShadowUtil.getIntent(accountType));
        LensProjectionContext accountSyncContext = context.findOrCreateProjectionContext(rat);
        accountSyncContext.setOid(account.getOid());
		accountSyncContext.setLoadedObject(account);
		accountSyncContext.setResource(resourceType);
		accountSyncContext.setExists(true);
		context.rememberResource(resourceType);
		return accountSyncContext;
    }

	protected <O extends ObjectType> ObjectDelta<O> addFocusModificationToContext(
			LensContext<O> context, File file)
            throws JAXBException, SchemaException, IOException {
		ObjectModificationType modElement = PrismTestUtil.parseAtomicValue(
                file, ObjectModificationType.COMPLEX_TYPE);
		ObjectDelta<O> focusDelta = DeltaConvertor.createObjectDelta(
				modElement, context.getFocusClass(), prismContext);
		return addFocusDeltaToContext(context, focusDelta);
	}

	protected <O extends ObjectType> ObjectDelta<O> addFocusDeltaToContext(
			LensContext<O> context, ObjectDelta<O> focusDelta) throws SchemaException {
		LensFocusContext<O> focusContext = context.getOrCreateFocusContext();
		focusContext.addPrimaryDelta(focusDelta);
		return focusDelta;
	}

	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(
			LensContext<UserType> context, QName propertyName, Object... propertyValues)
			throws SchemaException {
		return addModificationToContextReplaceUserProperty(context, new ItemPath(propertyName), propertyValues);
	}

	protected ObjectDelta<UserType> addModificationToContextReplaceUserProperty(
			LensContext<UserType> context, ItemPath propertyPath, Object... propertyValues)
			throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, focusContext
				.getObjectOld().getOid(), propertyPath, prismContext, propertyValues);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<UserType> addModificationToContextAddAccountFromFile(
			LensContext<UserType> context, String filename) throws JAXBException, SchemaException,
            IOException {
		return addModificationToContextAddProjection(context, UserType.class, new File(filename));
	}

	protected ObjectDelta<UserType> addModificationToContextAddAccountFromFile(
			LensContext<UserType> context, File file) throws JAXBException, SchemaException,
            IOException {
		return addModificationToContextAddProjection(context, UserType.class, file);
	}

	protected <F extends FocusType> ObjectDelta<F> addModificationToContextAddProjection(
			LensContext<F> context, Class<F> focusType, File file) throws JAXBException, SchemaException,
            IOException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(file);
		account.trim();
		account.checkConsistence();
		LensFocusContext<F> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<F> userDelta = ObjectDelta.createModificationAddReference(focusType, focusContext
				.getObjectOld().getOid(), FocusType.F_LINK_REF, prismContext, account);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<ShadowType> addModificationToContextDeleteAccount(
			LensContext<UserType> context, String accountOid) throws SchemaException,
			FileNotFoundException {
		LensProjectionContext accountCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> deleteAccountDelta = ObjectDelta.createDeleteDelta(ShadowType.class,
				accountOid, prismContext);
		accountCtx.addPrimaryDelta(deleteAccountDelta);
		return deleteAccountDelta;
	}

	protected <T> ObjectDelta<ShadowType> addModificationToContextReplaceAccountAttribute(
			LensContext<UserType> context, String accountOid, String attributeLocalName,
			T... propertyValues) throws SchemaException {
		LensProjectionContext accCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName,
				propertyValues);
		accCtx.addPrimaryDelta(accountDelta);
		return accountDelta;
	}

	protected <T> ObjectDelta<ShadowType> addSyncModificationToContextReplaceAccountAttribute(
			LensContext<UserType> context, String accountOid, String attributeLocalName,
			T... propertyValues) throws SchemaException {
		LensProjectionContext accCtx = context.findProjectionContextByOid(accountOid);
		ObjectDelta<ShadowType> accountDelta = createAccountDelta(accCtx, accountOid, attributeLocalName,
				propertyValues);
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
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected ObjectDelta<UserType> addModificationToContextUnassignRole(
			LensContext<UserType> context, String userOid, String roleOid)
			throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getOrCreateFocusContext();
		ObjectDelta<UserType> userDelta = createAssignmentUserDelta(userOid,
				roleOid, RoleType.COMPLEX_TYPE, null, null, null, false);
		focusContext.addPrimaryDelta(userDelta);
		return userDelta;
	}

	protected <T> ObjectDelta<ShadowType> createAccountDelta(LensProjectionContext accCtx, String accountOid,
			String attributeLocalName, T... propertyValues) throws SchemaException {
		ResourceType resourceType = accCtx.getResource();
		QName attrQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeLocalName);
		ItemPath attrPath = new ItemPath(ShadowType.F_ATTRIBUTES, attrQName);
		RefinedObjectClassDefinition refinedAccountDefinition = accCtx.getCompositeObjectClassDefinition();
		RefinedAttributeDefinition attrDef = refinedAccountDefinition.findAttributeDefinition(attrQName);
		assertNotNull("No definition of attribute "+attrQName+" in account def "+refinedAccountDefinition, attrDef);
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
		PropertyDelta<T> attrDelta = new PropertyDelta<T>(attrPath, attrDef, prismContext);
		attrDelta.setValuesToReplace(PrismPropertyValue.createCollection(propertyValues));
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
    	ObjectDelta<ShadowType> syncDelta = ObjectDelta.createAddDelta(syncAccountToAdd);
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

	protected void assertUserPrimaryDelta(LensContext<UserType> context) {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userPrimaryDelta = focusContext.getPrimaryDelta();
		assertNotNull("User primary delta is null", userPrimaryDelta);
		assertFalse("User primary delta is empty", userPrimaryDelta.isEmpty());
	}

	protected void assertNoUserSecondaryDelta(LensContext<UserType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		if (userSecondaryDelta == null) {
			return;
		}
		assertTrue("User secondary delta is not empty", userSecondaryDelta.isEmpty());
	}

	protected void assertUserSecondaryDelta(LensContext<UserType> context) throws SchemaException {
		LensFocusContext<UserType> focusContext = context.getFocusContext();
		ObjectDelta<UserType> userSecondaryDelta = focusContext.getSecondaryDelta();
		assertNotNull("User secondary delta is null", userSecondaryDelta);
		assertFalse("User secondary delta is empty", userSecondaryDelta.isEmpty());
	}

	@NotNull
	protected LensContext<UserType> createContextForRoleAssignment(String userOid, String roleOid, QName relation,
			Consumer<AssignmentType> modificationBlock, OperationResult result)
			throws SchemaException, ObjectNotFoundException, JAXBException {
		return createContextForAssignment(UserType.class, userOid, RoleType.class, roleOid, relation, modificationBlock, result);
	}

	@NotNull
	protected <F extends FocusType> LensContext<F> createContextForAssignment(Class<F> focusClass, String focusOid,
			Class<? extends FocusType> targetClass, String targetOid, QName relation,
			Consumer<AssignmentType> modificationBlock, OperationResult result)
			throws SchemaException, ObjectNotFoundException, JAXBException {
		LensContext<F> context = createLensContext(focusClass);
		fillContextWithFocus(context, focusClass, focusOid, result);
		QName targetType = prismContext.getSchemaRegistry().determineTypeForClass(targetClass);
		assertNotNull("Unknown target class "+targetClass, targetType);
		addFocusDeltaToContext(context, createAssignmentFocusDelta(focusClass, focusOid, targetOid, targetType, relation,
				modificationBlock, true));
		context.recompute();
		display("Input context", context);
		assertFocusModificationSanity(context);
		return context;
	}
}

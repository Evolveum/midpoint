/*
 * Copyright (c) 2010-2014 Evolveum
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

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.AssertJUnit;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.ResourceShadowDiscriminator;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.util.mock.MockClockworkHook;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class AbstractInternalModelIntegrationTest extends AbstractModelIntegrationTest {
	
	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR_PATH + "/connector-dummy.xml";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_PATH + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_NAME = "administrator";
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	
	protected static final String USER_JACK_FILENAME = COMMON_DIR_PATH + "/user-jack.xml";
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	protected static final String USER_JACK_USERNAME = "jack";
	
	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_PATH + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	
	protected static final String USER_GUYBRUSH_FILENAME = COMMON_DIR_PATH + "/user-guybrush.xml";
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	
	static final String USER_ELAINE_FILENAME = COMMON_DIR_PATH + "/user-elaine.xml";
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";
	
	// Largo does not have a full name set, employeeType=PIRATE
	protected static final String USER_LARGO_FILENAME = COMMON_DIR_PATH + "/user-largo.xml";
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	
	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
	
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-hbarbossa-dummy.xml";
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	protected static final String ACCOUNT_HBARBOSSA_DUMMY_USERNAME = "hbarbossa";
	
	public static final File ACCOUNT_SHADOW_JACK_DUMMY_FILE = new File(COMMON_DIR, "account-shadow-jack-dummy.xml");
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	
	public static final String ACCOUNT_HERMAN_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-herman-dummy.xml";
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";
	
//	public static final String ACCOUNT_HERMAN_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-herman-opendj.xml";
//	public static final String ACCOUNT_HERMAN_OPENDJ_OID = "22220000-2200-0000-0000-333300003333";
	
	public static final String ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-shadow-guybrush-dummy.xml";
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-guybrush-dummy.xml";
	
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-elaine-dummy.xml";
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	
	public static final File ENTITLEMENT_SHADOW_PIRATE_DUMMY_FILE = new File(COMMON_DIR, "entitlement-shadow-pirate-dummy.xml");
	public static final String ENTITLEMENT_PIRATE_DUMMY_NAME = "pirate";

	public static final String ACCOUNT_SHADOW_CALYPSO_DUMMY_FILENAME = COMMON_DIR_PATH + "/account-shadow-calypso-dummy.xml";
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";
	
	public static final String RESOURCE_DUMMY_FILENAME = COMMON_DIR_PATH + "/resource-dummy.xml";
	public static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	public static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	
	public static final String USER_TEMPLATE_FILENAME = COMMON_DIR_PATH + "/user-template.xml";
	public static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";
	
	protected static final String PASSWORD_POLICY_GLOBAL_FILENAME = COMMON_DIR_PATH + "/password-policy-global.xml";
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";
	
	protected static final String MOCK_CLOCKWORK_HOOK_URL = MidPointConstants.NS_MIDPOINT_TEST_PREFIX + "/mockClockworkHook";
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);

	protected PrismObject<UserType> userAdministrator;
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected UserType userTypeGuybrush;
	protected UserType userTypeElaine;
		
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	protected static DummyResource dummyResource;
	protected static DummyResourceContoller dummyResourceCtl;
	
	protected MockClockworkHook mockClockworkHook;
			
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		super.initSystem(initTask, initResult);
		
		mockClockworkHook = new MockClockworkHook();
		hookRegistry.registerChangeHook(MOCK_CLOCKWORK_HOOK_URL, mockClockworkHook);
		
		modelService.postInit(initResult);
		
		// System Configuration
		try {
			repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
				
		// Administrator
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult);
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
		login(userAdministrator);
		
		// User Templates
		repoAddObjectFromFile(USER_TEMPLATE_FILENAME, ObjectTemplateType.class, initResult);

		// Resources
		
		dummyResourceCtl = DummyResourceContoller.create(null, resourceDummy);
		dummyResourceCtl.extendSchemaPirate();
		dummyResource = dummyResourceCtl.getDummyResource();
		resourceDummy = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_FILENAME, RESOURCE_DUMMY_OID, initTask, initResult);
		resourceDummyType = resourceDummy.asObjectable();
		dummyResourceCtl.setResource(resourceDummy);
		
		// We need to create Barbossa's account in exactly the shape that is given by his existing assignments
		// otherwise any substantial change will trigger reconciliation and the recon changes will interfere with
		// the tests
		DummyAccount account = new DummyAccount(ACCOUNT_HBARBOSSA_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, "Hector Barbossa");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, "Caribbean");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, "Pirate Brethren, Inc.");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_WEAPON_NAME, "Undead Monkey");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, "rum");
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_QUOTE_NAME, "Arr!");
		dummyResource.addAccount(account);
		
		dummyResourceCtl.addAccount(ACCOUNT_HERMAN_DUMMY_USERNAME, "Herman Toothrot", "Monkey Island");
		dummyResourceCtl.addAccount(ACCOUNT_GUYBRUSH_DUMMY_USERNAME, "Guybrush Threepwood", "Melee Island");
				
		// Accounts
		repoAddObjectFromFile(ACCOUNT_HBARBOSSA_DUMMY_FILENAME, ShadowType.class, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILENAME, ShadowType.class, initResult);
		repoAddObjectFromFile(ACCOUNT_SHADOW_ELAINE_DUMMY_FILENAME, ShadowType.class, initResult);
		
		// Users
		userTypeJack = repoAddObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = repoAddObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		userTypeGuybrush = repoAddObjectFromFile(USER_GUYBRUSH_FILENAME, UserType.class, initResult).asObjectable();
		userTypeElaine = repoAddObjectFromFile(USER_ELAINE_FILENAME, UserType.class, initResult).asObjectable();
				
	}
	
	protected <O extends ObjectType> LensContext<O> createLensContext(Class<O> focusType) {
		return new LensContext<O>(focusType, prismContext, provisioningService);
	}
	
	protected LensContext<UserType> createUserAccountContext() {
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

	protected LensProjectionContext fillContextWithAccount(LensContext<UserType> context, String accountOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismObject<ShadowType> account = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        provisioningService.applyDefinition(account, result);
        return fillContextWithAccount(context, account, result);
	}

	protected LensProjectionContext fillContextWithAccountFromFile(LensContext<UserType> context, String filename, OperationResult result) throws SchemaException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, IOException {
		PrismObject<ShadowType> account = PrismTestUtil.parseObject(new File(filename));
		provisioningService.applyDefinition(account, result);
		return fillContextWithAccount(context, account, result);
	}

    protected LensProjectionContext fillContextWithAccount(LensContext<UserType> context, PrismObject<ShadowType> account, OperationResult result) throws SchemaException,
		ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
    	ShadowType accountType = account.asObjectable();
        String resourceOid = accountType.getResourceRef().getOid();
        ResourceType resourceType = provisioningService.getObject(ResourceType.class, resourceOid, null, null, result).asObjectable();
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
	
	
	protected <T> ObjectDelta<ShadowType> createAccountDelta(LensProjectionContext accCtx, String accountOid, 
			String attributeLocalName, T... propertyValues) throws SchemaException {
		ResourceType resourceType = accCtx.getResource();
		QName attrQName = new QName(ResourceTypeUtil.getResourceNamespace(resourceType), attributeLocalName);
		ItemPath attrPath = new ItemPath(ShadowType.F_ATTRIBUTES, attrQName);
		RefinedObjectClassDefinition refinedAccountDefinition = accCtx.getRefinedAccountDefinition();
		RefinedAttributeDefinition attrDef = refinedAccountDefinition.findAttributeDefinition(attrQName);
		assertNotNull("No definition of attribute "+attrQName+" in account def "+refinedAccountDefinition, attrDef);
		ObjectDelta<ShadowType> accountDelta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
		PropertyDelta<T> attrDelta = new PropertyDelta<T>(attrPath, attrDef, prismContext);
		attrDelta.setValuesToReplace(PrismPropertyValue.createCollection(propertyValues));
		accountDelta.addModification(attrDelta);
		return accountDelta;
	}	
	
	protected <F extends FocusType> void assertFocusModificationSanity(LensContext<F> context) throws JAXBException {
		LensFocusContext<F> focusContext = context.getFocusContext();
	    PrismObject<F> focusOld = focusContext.getObjectOld();
	    if (focusOld == null) {
	    	return;
	    }
	    ObjectDelta<F> focusPrimaryDelta = focusContext.getPrimaryDelta();
	    if (focusPrimaryDelta != null) {
		    assertEquals("No OID in old focus object", focusOld.getOid(), focusPrimaryDelta.getOid());
		    assertEquals(ChangeType.MODIFY, focusPrimaryDelta.getChangeType());
		    assertNull(focusPrimaryDelta.getObjectToAdd());
		    for (ItemDelta itemMod : focusPrimaryDelta.getModifications()) {
		        if (itemMod.getValuesToDelete() != null) {
		            Item property = focusOld.findItem(itemMod.getPath());
		            assertNotNull("Deleted item " + itemMod.getParentPath() + "/" + itemMod.getElementName() + " not found in focus", property);
		            for (Object valueToDelete : itemMod.getValuesToDelete()) {
		                if (!property.containsRealValue((PrismValue) valueToDelete)) {
		                    display("Deleted value " + valueToDelete + " is not in focus item " + itemMod.getParentPath() + "/" + itemMod.getElementName());
		                    display("Deleted value", valueToDelete);
		                    display("HASHCODE: " + valueToDelete.hashCode());
		                    for (Object value : property.getValues()) {
		                        display("Existing value", value);
		                        display("EQUALS: " + valueToDelete.equals(value));
		                        display("HASHCODE: " + value.hashCode());
		                    }
		                    AssertJUnit.fail("Deleted value " + valueToDelete + " is not in focus item " + itemMod.getParentPath() + "/" + itemMod.getElementName());
		                }
		            }
		        }
		
		    }
	    }
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
	     	
}

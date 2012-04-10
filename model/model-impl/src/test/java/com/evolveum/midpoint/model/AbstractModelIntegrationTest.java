/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ResourceAccountType;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
public class AbstractModelIntegrationTest extends AbstractIntegrationTest {
	
	protected static final String COMMON_DIR_NAME = "src/test/resources/common";
	
	public static final String SYSTEM_CONFIGURATION_FILENAME = COMMON_DIR_NAME + "/system-configuration.xml";
	public static final String SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";
	
	protected static final String USER_TEMPLATE_FILENAME = COMMON_DIR_NAME + "/user-template.xml";
	protected static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	protected static final String CONNECTOR_LDAP_FILENAME = COMMON_DIR_NAME + "/connector-ldap.xml";
	protected static final String CONNECTOR_DBTABLE_FILENAME = COMMON_DIR_NAME + "/connector-dbtable.xml";
	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR_NAME + "/connector-dummy.xml";
	
	protected static final String RESOURCE_OPENDJ_FILENAME = COMMON_DIR_NAME + "/resource-opendj.xml";
	protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
	
	protected static final String RESOURCE_DUMMY_FILENAME = COMMON_DIR_NAME + "/resource-dummy.xml";
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	
	protected static final String ROLE_ALPHA_FILENAME = COMMON_DIR_NAME + "/role-alpha.xml";
	protected static final String ROLE_ALPHA_OID = "12345678-d34d-b33f-f00d-55555555aaaa";

	protected static final String ROLE_BETA_FILENAME = COMMON_DIR_NAME + "/role-beta.xml";
	protected static final String ROLE_BETA_OID = "12345678-d34d-b33f-f00d-55555555bbbb";

	protected static final String USER_JACK_FILENAME = COMMON_DIR_NAME + "/user-jack.xml";
	protected static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

	protected static final String USER_BARBOSSA_FILENAME = COMMON_DIR_NAME + "/user-barbossa.xml";
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";

	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_FILENAME = COMMON_DIR_NAME + "/account-hbarbossa-opendj.xml";
	protected static final String ACCOUNT_HBARBOSSA_OPENDJ_OID = "c0c010c0-d34d-b33f-f00d-222211111112";
	
	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR_NAME + "/account-shadow-jack-dummy.xml";

	@Autowired(required = true)
	protected ModelService modelService;
	
	protected static final Trace LOGGER = TraceManager.getTrace(AbstractModelIntegrationTest.class);
	
	protected UserType userTypeJack;
	protected UserType userTypeBarbossa;
	protected ResourceType resourceOpenDjType;
	protected PrismObject<ResourceType> resourceOpenDj;
	protected ResourceType resourceDummyType;
	protected PrismObject<ResourceType> resourceDummy;
	
	public AbstractModelIntegrationTest() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		
		addObjectFromFile(SYSTEM_CONFIGURATION_FILENAME, initResult);
		addObjectFromFile(USER_TEMPLATE_FILENAME, initResult);

		// Connectors
		addObjectFromFile(CONNECTOR_LDAP_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DBTABLE_FILENAME, ConnectorType.class, initResult);
		addObjectFromFile(CONNECTOR_DUMMY_FILENAME, ConnectorType.class, initResult);
		
		// Resources
		resourceOpenDj = addObjectFromFile(RESOURCE_OPENDJ_FILENAME, ResourceType.class, initResult);
		resourceOpenDjType = resourceOpenDj.asObjectable();
		resourceDummy = addObjectFromFile(RESOURCE_DUMMY_FILENAME, ResourceType.class, initResult);
		resourceDummyType = resourceDummy.asObjectable();

		// Users
		userTypeJack = addObjectFromFile(USER_JACK_FILENAME, UserType.class, initResult).asObjectable();
		userTypeBarbossa = addObjectFromFile(USER_BARBOSSA_FILENAME, UserType.class, initResult).asObjectable();
		
		// Accounts
		addObjectFromFile(ACCOUNT_HBARBOSSA_OPENDJ_FILENAME, initResult);
		

	}
	
	private void importObjectFromFile(String filename, OperationResult result) throws FileNotFoundException {
		LOGGER.trace("importObjectFromFile: {}", filename);
		Task task = taskManager.createTaskInstance();
		FileInputStream stream = new FileInputStream(filename);
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);
	}
	
	protected void fillContextWithUser(SyncContext context, String userOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException {
        PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
        context.setUserOld(user);
    }

	protected void fillContextWithAccount(SyncContext context, String accountOid, OperationResult result) throws SchemaException,
			ObjectNotFoundException {
        PrismObject<AccountShadowType> account = repositoryService.getObject(AccountShadowType.class, accountOid, null, result);
        AccountShadowType accountType = account.asObjectable();
        ResourceAccountType rat = new ResourceAccountType(accountType.getResourceRef().getOid(), accountType.getAccountType());
        AccountSyncContext accountSyncContext = context.createAccountSyncContext(rat);
        accountSyncContext.setOid(accountOid);
		accountSyncContext.setAccountOld(account);
    }

	protected ObjectDelta<UserType> addModificationToContext(SyncContext context, String filename) throws JAXBException,
			SchemaException, FileNotFoundException {
	    ObjectModificationType modElement = PrismTestUtil.unmarshalObject(new File(filename), ObjectModificationType.class);
	    ObjectDelta<UserType> userDelta = DeltaConvertor.createObjectDelta(modElement, UserType.class, prismContext);
	    context.addPrimaryUserDelta(userDelta);
	    return userDelta;
	}

	protected void assertUserModificationSanity(SyncContext context) throws JAXBException {
	    PrismObject<UserType> userOld = context.getUserOld();
	    ObjectDelta<UserType> userPrimaryDelta = context.getUserPrimaryDelta();
	    assertEquals(userOld.getOid(), userPrimaryDelta.getOid());
	    assertEquals(ChangeType.MODIFY, userPrimaryDelta.getChangeType());
	    assertNull(userPrimaryDelta.getObjectToAdd());
	    for (ItemDelta itemMod : userPrimaryDelta.getModifications()) {
	        if (itemMod.getValuesToDelete() != null) {
	            Item property = userOld.findItem(itemMod.getPath());
	            assertNotNull("Deleted item " + itemMod.getParentPath() + "/" + itemMod.getName() + " not found in user", property);
	            for (Object valueToDelete : itemMod.getValuesToDelete()) {
	                if (!property.getValues().contains(valueToDelete)) {
	                    display("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
	                    display("Deleted value", valueToDelete);
	                    display("HASHCODE: " + valueToDelete.hashCode());
	                    for (Object value : property.getValues()) {
	                        display("Existing value", value);
	                        display("EQUALS: " + valueToDelete.equals(value));
	                        display("HASHCODE: " + value.hashCode());
	                    }
	                    AssertJUnit.fail("Deleted value " + valueToDelete + " is not in user item " + itemMod.getParentPath() + "/" + itemMod.getName());
	                }
	            }
	        }
	
	    }
	}
	
	protected void assertDummyRefinedSchemaSanity(RefinedResourceSchema refinedSchema) {
		
		RefinedAccountDefinition accountDef = refinedSchema.getDefaultAccountDefinition();
		assertNotNull("Account definition is missing", accountDef);
		assertNotNull("Null identifiers in account", accountDef.getIdentifiers());
		assertFalse("Empty identifiers in account", accountDef.getIdentifiers().isEmpty());
		assertNotNull("Null secondary identifiers in account", accountDef.getSecondaryIdentifiers());
		assertFalse("Empty secondary identifiers in account", accountDef.getSecondaryIdentifiers().isEmpty());
		assertNotNull("No naming attribute in account", accountDef.getNamingAttribute());
		assertFalse("No nativeObjectClass in account", StringUtils.isEmpty(accountDef.getNativeObjectClass()));

		RefinedAttributeDefinition uidDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_UID);
		assertEquals(1, uidDef.getMaxOccurs());
		assertEquals(1, uidDef.getMinOccurs());
		assertFalse("No UID display name", StringUtils.isBlank(uidDef.getDisplayName()));
		assertFalse("UID has create", uidDef.canCreate());
		assertFalse("UID has update",uidDef.canUpdate());
		assertTrue("No UID read",uidDef.canRead());
		assertTrue("UID definition not in identifiers", accountDef.getIdentifiers().contains(uidDef));

		RefinedAttributeDefinition nameDef = accountDef.findAttributeDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		assertEquals(1, nameDef.getMaxOccurs());
		assertEquals(1, nameDef.getMinOccurs());
		assertFalse("No NAME displayName", StringUtils.isBlank(nameDef.getDisplayName()));
		assertTrue("No NAME create", nameDef.canCreate());
		assertFalse("NAME has update",nameDef.canUpdate());
		assertTrue("No NAME read",nameDef.canRead());
		assertTrue("NAME definition not in identifiers", accountDef.getSecondaryIdentifiers().contains(nameDef));

		RefinedAttributeDefinition fullnameDef = accountDef.findAttributeDefinition("fullname");
		assertNotNull("No definition for fullname", fullnameDef);
		assertEquals(1, fullnameDef.getMaxOccurs());
		assertEquals(1, fullnameDef.getMinOccurs());
		assertTrue("No fullname create", fullnameDef.canCreate());
		assertTrue("No fullname update", fullnameDef.canUpdate());
		assertTrue("No fullname read", fullnameDef.canRead());
		
		assertNull("The _PASSSWORD_ attribute sneaked into schema", accountDef.findAttributeDefinition(new QName(ConnectorFactoryIcfImpl.NS_ICF_SCHEMA,"password")));
		
	}

}

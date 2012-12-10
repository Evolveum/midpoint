/**
 * 
 */
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertProvisioningAccountShadow;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.common.refinery.RefinedAccountDefinition;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.DiffUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.api.ResultHandler;
import com.evolveum.midpoint.provisioning.impl.ConnectorTypeManager;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceObjectShadowUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ObjectChecker;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CachingMetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilitiesType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ActivationCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.ScriptCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_2.TestConnectionCapabilityType;

/**
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-provisioning.xml",
		"classpath:ctx-provisioning-test.xml", "classpath:ctx-task.xml",
		"classpath:ctx-audit.xml", "classpath:ctx-repository.xml",
		"classpath:ctx-repo-cache.xml", "classpath:ctx-configuration-test.xml" })
@DirtiesContext
public class TestDummyNegative extends AbstractDummyTest {

	private static final Trace LOGGER = TraceManager.getTrace(TestDummyNegative.class);
	
	private static final String ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILENAME = TEST_DIR + "account-elaine-resource-not-found.xml";
	
	@Test
	public void test110GetResourceBrokenSchemaNetwork() throws Exception {
		testGetResourceBrokenSchema(BreakMode.NETWORK, "test110GetResourceBrokenSchemaNetwork");
	}
	
	@Test
	public void test111GetResourceBrokenSchemaGeneric() throws Exception {
		testGetResourceBrokenSchema(BreakMode.GENERIC, "test111GetResourceBrokenSchemaGeneric");
	}
	
	@Test
	public void test112GetResourceBrokenSchemaIo() throws Exception {
		testGetResourceBrokenSchema(BreakMode.IO, "test112GetResourceBrokenSchemaIO");
	}
	
	@Test
	public void test113GetResourceBrokenSchemaRuntime() throws Exception {
		testGetResourceBrokenSchema(BreakMode.RUNTIME, "test113GetResourceBrokenSchemaRuntime");
	}
	
	public void testGetResourceBrokenSchema(BreakMode breakMode, String testName) throws Exception {
		displayTestTile(testName);
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ "."+testName);
		
		// precondition
		PrismObject<ResourceType> repoResource = repositoryService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, result);
		display("Repo resource (before)", repoResource);
		PrismContainer<Containerable> schema = repoResource.findContainer(ResourceType.F_SCHEMA);
		assertTrue("Schema found in resource before the test (precondition)", schema == null || schema.isEmpty());

		dummyResource.setSchemaBreakMode(breakMode);
		try {
			
			// WHEN
			PrismObject<ResourceType> resource = provisioningService.getObject(ResourceType.class, RESOURCE_DUMMY_OID, null, result);
			
			// THEN
			display("Resource with broken schema", resource);
			OperationResultType fetchResult = resource.asObjectable().getFetchResult();
			
			result.computeStatus();
			display("getObject result", result);
			assertEquals("Unexpected result of getObject operation", OperationResultStatus.PARTIAL_ERROR, result.getStatus());
			
			assertNotNull("No fetch result", fetchResult);
			display("fetchResult", fetchResult);
			assertEquals("Unexpected result of fetchResult", OperationResultStatusType.PARTIAL_ERROR, fetchResult.getStatus());
			
		} finally {
			dummyResource.setSchemaBreakMode(BreakMode.NONE);
		}
	}
	
	
	@Test
	public void test200AddAccountNullAttributes() throws Exception {
		displayTestTile("test200AddAccountNullAttributes");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test200AddAccountNullAttributes");

		AccountShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		account.removeContainer(AccountShadowType.F_ATTRIBUTES);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

	}
	
	@Test
	public void test201AddAccountEmptyAttributes() throws Exception {
		displayTestTile("test201AddAccountEmptyAttributes");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test201AddAccountEmptyAttributes");

		AccountShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		account.findContainer(AccountShadowType.F_ATTRIBUTES).getValue().clear();

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

	}
	
	@Test
	public void test210AddAccountNoObjectclass() throws Exception {
		displayTestTile("test210AddAccountNoObjectclass");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test210AddAccountNoObjectclass");

		AccountShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		accountType.setObjectClass(null);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

	}
	
	@Test
	public void test220AddAccountNoResourceRef() throws Exception {
		displayTestTile("test220AddAccountNoResourceRef");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test220AddAccountNoResourceRef");

		AccountShadowType accountType = parseObjectTypeFromFile(ACCOUNT_WILL_FILENAME, AccountShadowType.class);
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
		accountType.setResourceRef(null);

		display("Adding shadow", account);

		try {
			// WHEN
			provisioningService.addObject(account, null, result);
			
			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

	}
	
	@Test
	public void test221DeleteAccountResourceNotFound() throws Exception {
		displayTestTile("test220AddAccountNoResourceRef");
		// GIVEN
		OperationResult result = new OperationResult(TestDummyNegative.class.getName()
				+ ".test220AddAccountNoResourceRef");

		AccountShadowType accountType = parseObjectTypeFromFile(ACCOUNT_ELAINE_RESOURCE_NOT_FOUND_FILENAME, AccountShadowType.class);
		PrismObject<AccountShadowType> account = accountType.asPrismObject();
		account.checkConsistence();
		
//		accountType.setResourceRef(null);

		display("Adding shadow", account);

		try {
			// WHEN
			String oid = repositoryService.addObject(account, result);
			
			provisioningService.deleteObject(AccountShadowType.class, oid, ObjectOperationOption.FORCE, null, result);
//			AssertJUnit.fail("The addObject operation was successful. But expecting an exception.");
		} catch (SchemaException e) {
			// This is expected
			display("Expected exception", e);
		}

	}


}

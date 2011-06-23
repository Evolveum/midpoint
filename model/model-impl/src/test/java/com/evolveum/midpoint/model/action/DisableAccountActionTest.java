package com.evolveum.midpoint.model.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opends.server.controls.AccountUsableResponseControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-provisioning.xml",
		"classpath:application-context-model-test.xml" })
public class DisableAccountActionTest {

	@Autowired(required = true)
	private ResourceObjectChangeListenerPortType resourceObjectChangeService;
	@Autowired(required = true)
	private RepositoryPortType repositoryService;
	@Autowired(required = true)
	private ResourceAccessInterface rai;

	public DisableAccountActionTest() {
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	private ObjectType addObjectToRepo(ObjectType object) throws Exception {
		ObjectContainerType objectContainer = new ObjectContainerType();
		objectContainer.setObject(object);
		repositoryService.addObject(objectContainer);
		return object;
	}

	@SuppressWarnings("unchecked")
	private ObjectType addObjectToRepo(String fileString) throws Exception {
		ObjectContainerType objectContainer = new ObjectContainerType();
		ObjectType object = ((JAXBElement<ObjectType>) JAXBUtil.unmarshal(new File(fileString))).getValue();
		objectContainer.setObject(object);
		repositoryService.addObject(objectContainer);
		return object;
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescriptionType createChangeDescription(String file)
			throws JAXBException {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(file))).getValue();
		return change;
	}

	private ResourceObject createSampleResourceObject(ResourceSchema schema, ResourceObjectShadowType shadow)
			throws ParserConfigurationException {
		ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
		return valueWriter.buildResourceObject(shadow, schema);
	}

	@Test
	@Ignore
	public void testDeleteAccountAction() throws Exception {
		final String resourceOid = "66666666-d34d-b33f-f00d-333222111111";
		final String accountOid = "66666666-d34d-b44f-f11d-333222111111";

		try {
			// create additional change
			ResourceObjectShadowChangeDescriptionType change = createChangeDescription("src/test/resources/account-change-disable-account.xml");
			// adding objects to repo
			final ResourceType resourceType = (ResourceType) addObjectToRepo(change.getResource());
			final AccountShadowType accountType = (AccountShadowType) addObjectToRepo(change.getShadow());

			assertNotNull(resourceType);
			// setup provisioning mock
			BaseResourceIntegration bri = new BaseResourceIntegration(resourceType);
			ResourceObject ro = createSampleResourceObject(bri.getSchema(), accountType);

			when(rai.get(any(OperationalResultType.class), any(ResourceObject.class))).thenReturn(ro);

			when(rai.getConnector()).thenReturn(bri);

			resourceObjectChangeService.notifyChange(change);

			// TODO:make some asserts to verify the result

		} finally {
			// cleanup repo
			try {
				repositoryService.deleteObject(accountOid);
			} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
			}
			try {
				repositoryService.deleteObject(resourceOid);
			} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
			}

		}
	}

}

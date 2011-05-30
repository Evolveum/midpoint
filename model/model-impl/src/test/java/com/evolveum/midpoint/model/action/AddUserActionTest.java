package com.evolveum.midpoint.model.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.DOMUtil;
import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
//import com.evolveum.midpoint.provisioning.schema.util.AccountType;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.SchemaHandlingType.AccountType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository.xml",
		"classpath:application-context-repository-test.xml",
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-model-test.xml" })
public class AddUserActionTest {

	@Autowired(required = true)
	private ResourceObjectChangeListenerPortType resourceObjectChangeService;
	@Autowired(required = true)
	private RepositoryPortType repositoryService;
	@Autowired(required = true)
	private ResourceAccessInterface rai;

	// @Autowired(required = true)
	// private ProvisioningPortType provisioningService;

	public AddUserActionTest() {
	}

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
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
		ObjectType object = ((JAXBElement<ObjectType>) JAXBUtil
				.unmarshal(new File(fileString))).getValue();
		objectContainer.setObject(object);
		repositoryService.addObject(objectContainer);
		return object;
	}

	@SuppressWarnings("unchecked")
	private ResourceObjectShadowChangeDescriptionType createChangeDescription(
			String file) throws JAXBException {
		ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
				.unmarshal(new File(file))).getValue();
		return change;
	}

	private ResourceObject createSampleResourceObject(ResourceSchema schema,
			ResourceObjectShadowType shadow)
			throws ParserConfigurationException {
		ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
		return valueWriter.buildResourceObject(shadow, schema);
	}

	@Test
	public void testAddUserAction() throws Exception {

		final String resourceOid = "c0c010c0-d34d-b33f-f00d-333222111111";
		final String userTemplateOid = "c0c010c0-d34d-b55f-f22d-777666111111";
		final String accountOid = "c0c010c0-d34d-b44f-f11d-333222111111";

		UserType addedUser = null;

		try {
			// create additional change
			ResourceObjectShadowChangeDescriptionType change = createChangeDescription("src/test/resources/account-change-add-user.xml");
			// adding objects to repo
			addObjectToRepo("src/test/resources/user-template-create-account.xml");
			ResourceType resourceType = (ResourceType) addObjectToRepo(change.getResource());
			AccountShadowType accountType = (AccountShadowType) addObjectToRepo(change.getShadow());

			//setting resource for ResourceObjectShadowType
			((ResourceObjectShadowType) ((ObjectChangeAdditionType) change.getObjectChange()).getObject()).setResource(resourceType);
			((ResourceObjectShadowType) ((ObjectChangeAdditionType) change.getObjectChange()).getObject()).setResourceRef(null);

			assertNotNull(resourceType);
			// setup provisioning mock
			BaseResourceIntegration bri = new BaseResourceIntegration(
					resourceType);
			ResourceObject ro = createSampleResourceObject(bri.getSchema(),
					accountType);
			when(
					rai.get(any(OperationalResultType.class),
							any(ResourceObject.class))).thenReturn(ro);
			when(rai.getConnector()).thenReturn(bri);

			resourceObjectChangeService.notifyChange(change);

			//creating filter to search user according to the user name
			XPathSegment xpathSegment = new XPathSegment(SchemaConstants.C_NAME);
			Document doc = DOMUtil.getDocument();
			List<XPathSegment> xpathSegments = new ArrayList<XPathSegment>();
			xpathSegments.add(xpathSegment);

			XPathType xpath = new XPathType(xpathSegments);

			List<Element> values = new ArrayList<Element>();
			Element element = doc.createElementNS(SchemaConstants.NS_C, "name");
			element.setTextContent("will");
			values.add(element);

			Element filter = QueryUtil.createAndFilter(
					doc,
					QueryUtil.createTypeFilter(doc, QNameUtil.qNameToUri(SchemaConstants.I_USER_TYPE)),
					QueryUtil.createEqualFilter(doc, xpath, values));

			QueryType query = new QueryType();
			query.setFilter(filter);

			ObjectListType list = repositoryService.searchObjects(query,
					new PagingType());

			for (ObjectType objectType : list.getObject()) {
				addedUser = (UserType) objectType;
			}
			List<ObjectReferenceType> accountRefs = addedUser.getAccountRef();

			assertNotNull(addedUser);
			assertEquals(accountOid, accountRefs.get(0).getOid());

			ObjectContainerType container = repositoryService.getObject(
					accountOid, new PropertyReferenceListType());
			AccountShadowType addedAccount = (AccountShadowType) container
					.getObject();

			assertNotNull(addedAccount);
			assertEquals(addedUser.getName(), addedAccount.getName());

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
			try {
				repositoryService.deleteObject(userTemplateOid);
			} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
			}
			try {
				repositoryService.deleteObject(addedUser.getOid());
			} catch (com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage e) {
			}
		}

	}

}

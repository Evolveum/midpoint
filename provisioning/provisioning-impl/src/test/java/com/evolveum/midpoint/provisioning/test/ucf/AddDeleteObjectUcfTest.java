/*
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

package com.evolveum.midpoint.provisioning.test.ucf;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response.Status.Family;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.internal.resources.mapping.ChangeDescription;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.result.OperationResult;

import com.evolveum.midpoint.provisioning.ucf.api.AttributeModificationOperation;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.CommunicationException;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.ucf.api.ObjectNotFoundException;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.Token;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

public class AddDeleteObjectUcfTest extends OpenDJUnitTestAdapter {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String FILENAME_RESOURCE_OPENDJ_BAD = "src/test/resources/ucf/opendj-resource-bad.xml";
	private static final String FILENAME_CONNECTOR_LDAP = "src/test/resources/ucf/ldap-connector.xml";

	private static final String RESOURCE_NS = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	protected static OpenDJUtil djUtil = new OpenDJUtil();
	private JAXBContext jaxbctx;
	ResourceType resource;
	ResourceType badResource;
	private ConnectorFactory manager;
	private ConnectorInstance cc;
	ConnectorType connectorType;
	Schema schema;

	public AddDeleteObjectUcfTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage()
				.getName());
	}

	@BeforeClass
	public static void startLdap() throws Exception {
		startACleanDJ();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		stopDJ();
	}

	@Before
	public void initUcf() throws Exception {

		File file = new File(FILENAME_RESOURCE_OPENDJ);
		FileInputStream fis = new FileInputStream(file);

		// Resource
        Unmarshaller u = jaxbctx.createUnmarshaller();
        Object object = u.unmarshal(fis);		
		resource = (ResourceType) ((JAXBElement) object).getValue();
		
		// Resource: Second copy for negative test cases
		file = new File(FILENAME_RESOURCE_OPENDJ_BAD);
        fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		badResource = (ResourceType) ((JAXBElement) object).getValue();

		// Connector
		file = new File(FILENAME_CONNECTOR_LDAP);
        fis = new FileInputStream(file);
		object = u.unmarshal(fis);
		connectorType = (ConnectorType) ((JAXBElement) object).getValue();

		ConnectorFactoryIcfImpl managerImpl = new ConnectorFactoryIcfImpl();
		managerImpl.initialize();
		manager = managerImpl;

		cc = manager.createConnectorInstance(connectorType,resource.getNamespace());
		assertNotNull(cc);
		cc.configure(resource.getConfiguration(), new OperationResult("initUcf"));
		// TODO: assert something

		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".initUcf");
		schema = cc.fetchResourceSchema(result);

		assertNotNull(schema);

	}

	@After
	public void shutdownUcf() throws Exception {
	
	}

	private Set<ResourceObjectAttribute> addSampleResourceObject(String name,
			String givenName, String familyName) throws CommunicationException,
			GenericFrameworkException, SchemaException, ObjectAlreadyExistsException {
		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".testAdd");

		
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema
				.findContainerDefinitionByType(new QName(resource
						.getNamespace(), "AccountObjectClass"));
		System.out.println(accountDefinition.getTypeName().getNamespaceURI());
		ResourceObject resourceObject = accountDefinition.instantiate();
		resourceObject.getProperties();

		PropertyDefinition propertyDefinition = accountDefinition
				.findPropertyDefinition(ConnectorFactoryIcfImpl.ICFS_NAME);
		Property property = propertyDefinition.instantiate();
		property.setValue("uid=" + name + ",ou=people,dc=example,dc=com");
		resourceObject.getProperties().add(property);

		propertyDefinition = accountDefinition
				.findPropertyDefinition(new QName(RESOURCE_NS, "sn"));
		property = propertyDefinition.instantiate();
		property.setValue(familyName);
		resourceObject.getProperties().add(property);

		propertyDefinition = accountDefinition
				.findPropertyDefinition(new QName(RESOURCE_NS, "cn"));
		property = propertyDefinition.instantiate();
		property.setValue(givenName + " " + familyName);
		resourceObject.getProperties().add(property);

		propertyDefinition = accountDefinition
				.findPropertyDefinition(new QName(RESOURCE_NS, "givenName"));
		property = propertyDefinition.instantiate();
		property.setValue(givenName);
		resourceObject.getProperties().add(property);
		
		

		Set<Operation> operation = new HashSet<Operation>();
		Set<ResourceObjectAttribute> resourceAttributes = cc.addObject(
				resourceObject, operation, result);
		return resourceAttributes;
	}

	@Test
	public void testAddObject() throws Exception {

		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".testAdd");

		Set<ResourceObjectAttribute> resourceAttributes = addSampleResourceObject(
				"jack", "Jack", "Sparow");
	
		for (ResourceObjectAttribute resourceAttribute : resourceAttributes) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(resourceAttribute.getName())) {
				String uid = resourceAttribute.getValue(String.class);
				System.out.println("uuuuid:" + uid);
				assertNotNull(uid);

			}
		}

	}

	@Test
	public void testDeleteObject() throws Exception {

		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".testDelete");

		Set<ResourceObjectAttribute> identifiers = addSampleResourceObject(
				"john", "John", "Smith");

		String uid = null;
		for (ResourceObjectAttribute resourceAttribute : identifiers) {
			if (ConnectorFactoryIcfImpl.ICFS_UID.equals(resourceAttribute.getName())) {
				uid = resourceAttribute.getValue(String.class);
				System.out.println("uuuuid:" + uid);
				assertNotNull(uid);
			}
		}

		QName objectClass = new QName(resource.getNamespace(),
				"AccountObjectClass");

		cc.deleteObject(objectClass, null, identifiers, result);

		ResourceObject resObj = null;
		try {
			 resObj = cc.fetchObject(objectClass, identifiers, result);
			fail();
		} catch (ObjectNotFoundException ex) {
			assertNull(resObj);
		}

	}
	
	@Test
	public void testChangeModifyObject() throws Exception{
		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".testModify");

		Set<ResourceObjectAttribute> identifiers = addSampleResourceObject(
				"john", "John", "Smith");
		
		Set<Operation> changes = new HashSet<Operation>();
		
		changes.add(createAddChange("employeeNumber", "123123123"));
		changes.add(createReplaceChange("sn", "Smith007"));
		changes.add(createAddChange("street", "Wall Street"));
		changes.add(createDeleteChange("givenName", "John"));
				
		QName objectClass = new QName(resource.getNamespace(), "AccountObjectClass"); 
		cc.modifyObject(objectClass, identifiers, changes, result);
		
		ResourceObject resObj = cc.fetchObject(objectClass, identifiers, result);
		
		assertNull(resObj.findAttribute(new QName(RESOURCE_NS, "givenName")));
		
		String addedEmployeeNumber =resObj.findAttribute(new QName(RESOURCE_NS, "employeeNumber")).getValue(String.class);
		String changedSn = resObj.findAttribute(new QName(RESOURCE_NS, "sn")).getValue(String.class);
		String addedStreet = resObj.findAttribute(new QName(RESOURCE_NS, "street")).getValue(String.class);

		
		System.out.println("changed employee number: " + addedEmployeeNumber);
		System.out.println("changed sn: " + changedSn);
		System.out.println("added street: " + addedStreet);


		assertEquals("123123123", addedEmployeeNumber);
		assertEquals("Smith007", changedSn);
		assertEquals("Wall Street", addedStreet);
		
		
	}
	
	@Test
	public void testFetchChanges() throws Exception{
		OperationResult result = new OperationResult(this.getClass().getName()
				+ ".testFetchChanges");
		QName objectClass = new QName(resource.getNamespace(), "AccountObjectClass"); 
		Property lastToken = cc.fetchCurrentToken(objectClass, result);
		
		System.out.println("Property:");
		System.out.println(DebugUtil.prettyPrint(lastToken));
		
		System.out.println("token "+ lastToken.toString());
		List<Change> changes = cc.fetchChanges(objectClass, lastToken, result);
		assertEquals(0, changes.size());
	}
	
	
	
	private Property createProperty(String propertyName, String propertyValue){
		ResourceObjectDefinition accountDefinition = (ResourceObjectDefinition) schema
		.findContainerDefinitionByType(new QName(resource
				.getNamespace(), "AccountObjectClass"));
		PropertyDefinition propertyDef = accountDefinition.findPropertyDefinition(new QName(resource.getNamespace(), propertyName));
		Property property = propertyDef.instantiate();
		property.setValue(propertyValue);
		return property;
	}
	
	
	private AttributeModificationOperation createReplaceChange(String propertyName, String propertyValue){
		AttributeModificationOperation attributeModification = new AttributeModificationOperation();
		attributeModification.setChangeType(PropertyModificationTypeType.replace);	
		Property property = createProperty(propertyName, propertyValue);	
		attributeModification.setNewAttribute(property);
		System.out.println("-------replace attribute modification-----");
		System.out.println("property name: " +property.getName().getLocalPart());
		System.out.println("property namespace: "+property.getName().getNamespaceURI());
		System.out.println("property value: "+property.getValue(String.class));
		for (Object obj : property.getValues()){
			System.out.println("asdasdasd: "+ obj.toString());
		}
		System.out.println("-------replace attribute modification end-------");
		return attributeModification;
	}
	
	private AttributeModificationOperation createAddChange(String propertyName, String propertyValue){
		AttributeModificationOperation attributeModification = new AttributeModificationOperation();
		attributeModification.setChangeType(PropertyModificationTypeType.add);	
		
		Property property = createProperty(propertyName, propertyValue);	
		
		attributeModification.setNewAttribute(property);
		System.out.println("-------add attribute modification-----");
		System.out.println("property name: " +property.getName().getLocalPart());
		System.out.println("property namespace: "+property.getName().getNamespaceURI());
		System.out.println("property value: "+property.getValue(String.class));
		System.out.println("-------add attribute modification end-------");
		
		return attributeModification;
	}
	

	private AttributeModificationOperation createDeleteChange(String propertyName, String propertyValue){
		AttributeModificationOperation attributeModification = new AttributeModificationOperation();
		attributeModification.setChangeType(PropertyModificationTypeType.delete);	
		
		Property property = createProperty(propertyName, propertyValue);	
		
		attributeModification.setNewAttribute(property);
		System.out.println("-------delete attribute modification-----");
		System.out.println("property name: " +property.getName().getLocalPart());
		System.out.println("property namespace: "+property.getName().getNamespaceURI());
		System.out.println("property value: "+property.getValue(String.class));
		System.out.println("-------delete attribute modification end-------");
		
		return attributeModification;
	}
}

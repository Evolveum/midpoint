/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_2.ModificationTypeType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 */
public class TestDeltaConverter {
	
	private static final File TEST_DIR = new File("src/test/resources/deltaconverter");
	private static final File COMMON_TEST_DIR = new File("src/test/resources/common");
	
	private static final ItemPath CREDENTIALS_PASSWORD_VALUE_PATH = 
		new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
    }

    @Test
    public void testRefWithObject() throws SchemaException, FileNotFoundException, JAXBException {
    	System.out.println("===[ testRefWithObject ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(new File(TEST_DIR, "user-modify-add-account.xml"),
    			ObjectModificationType.class);
    	
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	ReferenceDelta accoutRefDelta = objectDelta.findReferenceModification(UserType.F_ACCOUNT_REF);
    	assertNotNull("No accountRef delta", accoutRefDelta);
    	Collection<PrismReferenceValue> valuesToAdd = accoutRefDelta.getValuesToAdd();
    	assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
    	PrismReferenceValue accountRefVal = valuesToAdd.iterator().next();
    	assertNotNull("No object in accountRef value", accountRefVal.getObject());
    	
    	objectDelta.assertDefinitions();
    }
    
    @Test
    public void testPasswordChange() throws SchemaException, FileNotFoundException, JAXBException {
    	System.out.println("===[ testPasswordChange ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(new File(TEST_DIR, "user-modify-password.xml"),
    			ObjectModificationType.class);
    	
    	// WHEN
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	// THEN
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	PropertyDelta<ProtectedStringType> protectedStringDelta = objectDelta.findPropertyDelta(CREDENTIALS_PASSWORD_VALUE_PATH);
    	assertNotNull("No protectedString delta", protectedStringDelta);
    	Collection<PrismPropertyValue<ProtectedStringType>> valuesToReplace = protectedStringDelta.getValuesToReplace();
    	assertEquals("Wrong number of values to add", 1, valuesToReplace.size());
    	PrismPropertyValue<ProtectedStringType> protectedStringVal = valuesToReplace.iterator().next();
    	assertNotNull("Null value in protectedStringDelta", protectedStringVal);
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(new File(COMMON_TEST_DIR, "user-jack.xml"));
    	// apply to user
    	objectDelta.applyTo(user);
    	
    	PrismProperty<ProtectedStringType> protectedStringProperty = user.findProperty(CREDENTIALS_PASSWORD_VALUE_PATH);
    	PrismPropertyValue<ProtectedStringType> protectedStringPropertyValue = protectedStringProperty.getValue();
    	assertTrue("protectedString not equivalent", protectedStringPropertyValue.equalsRealValue(protectedStringVal));
    	
    	objectDelta.assertDefinitions();
    }
    
    @Test
    public void testAddAssignment() throws SchemaException, FileNotFoundException, JAXBException {
    	System.out.println("===[ testAddAssignment ]====");
    	
    	ObjectModificationType objectChange = PrismTestUtil.unmarshalObject(new File(TEST_DIR, "user-modify-add-role-pirate.xml"),
    			ObjectModificationType.class);
    	
    	// WHEN
    	ObjectDelta<UserType> objectDelta = DeltaConvertor.createObjectDelta(objectChange, UserType.class, 
    			PrismTestUtil.getPrismContext());
    	
    	System.out.println("Delta:");
    	System.out.println(objectDelta.dump());
    	
    	// THEN
    	assertNotNull("No object delta", objectDelta);
    	objectDelta.checkConsistence();
    	assertEquals("Wrong OID", "c0c010c0-d34d-b33f-f00d-111111111111", objectDelta.getOid());
    	ContainerDelta<AssignmentType> assignmentDelta = objectDelta.findContainerDelta(UserType.F_ASSIGNMENT);
    	assertNotNull("No assignment delta", assignmentDelta);
    	Collection<PrismContainerValue<AssignmentType>> valuesToAdd = assignmentDelta.getValuesToAdd();
    	assertEquals("Wrong number of values to add", 1, valuesToAdd.size());
    	PrismContainerValue<AssignmentType> assignmentVal = valuesToAdd.iterator().next();
    	assertNotNull("Null value in protectedStringDelta", assignmentVal);
    	
    	PrismReference targetRef = assignmentVal.findReference(AssignmentType.F_TARGET_REF);
    	assertNotNull("No targetRef in assignment", targetRef);
    	PrismReferenceValue targetRefVal = targetRef.getValue();
    	assertNotNull("No targetRef value in assignment", targetRefVal);
    	assertEquals("Wrong OID in targetRef value", "12345678-d34d-b33f-f00d-987987987988", targetRefVal.getOid());
    	assertEquals("Wrong type in targetRef value", RoleType.COMPLEX_TYPE, targetRefVal.getTargetType());
    	
    	PrismObject<UserType> user = PrismTestUtil.parseObject(new File(COMMON_TEST_DIR, "user-jack.xml"));
    	
    	objectDelta.assertDefinitions("delta before test");
    	user.assertDefinitions("user before test");
    	
    	// apply to user
    	objectDelta.applyTo(user);
    	
    	objectDelta.assertDefinitions("delta after test");
    	user.assertDefinitions("user after test");

    	// TODO
    }
    
    @Test
    public void testAccountRefDelta() throws Exception {
    	System.out.println("===[ testAccountRefDelta ]====");
    	
    	// GIVEN
    	ObjectModificationType objectChange = new ObjectModificationType();
        objectChange.setOid("12345");
        ItemDeltaType modificationDeleteAccountRef = new ItemDeltaType();
        modificationDeleteAccountRef.setModificationType(ModificationTypeType.DELETE);
        ItemDeltaType.Value modificationValue = new ItemDeltaType.Value();
        ObjectReferenceType accountRefToDelete = new ObjectReferenceType();
        accountRefToDelete.setOid("54321");
        JAXBElement<ObjectReferenceType> accountRefToDeleteElement = new JAXBElement<ObjectReferenceType>(SchemaConstants.I_ACCOUNT_REF, ObjectReferenceType.class, accountRefToDelete);
        modificationValue.getAny().add(accountRefToDeleteElement);
        modificationDeleteAccountRef.setValue(modificationValue);
        objectChange.getModification().add(modificationDeleteAccountRef);
        
        PrismObjectDefinition<UserType> objDef = PrismTestUtil.getObjectDefinition(UserType.class);
        
		// WHEN
        Collection<? extends ItemDelta> modifications = DeltaConvertor.toModifications(objectChange, objDef);
        
        // THEN
        assertNotNull("Null modifications", modifications);
        assertFalse("Empty modifications", modifications.isEmpty());
        // TODO: more asserts
    }
    
    @Test
    public void testProtectedStringObjectDelta() throws Exception {
    	System.out.println("===[ testProtectedStringObjectDelta ]====");

    	// GIVEN
    	ItemPath path = CREDENTIALS_PASSWORD_VALUE_PATH;
    	ProtectedStringType protectedString = new ProtectedStringType();
    	protectedString.setClearValue("abrakadabra");
    	ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, "12345",
    			path, PrismTestUtil.getPrismContext(), protectedString);

    	System.out.println("ObjectDelta");
    	System.out.println(objectDelta.dump());

    	// WHEN
    	ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

    	// THEN
    	System.out.println("ObjectDeltaType (XML)");
    	System.out.println(PrismTestUtil.marshalWrap(objectDeltaType));
    	
    	assertEquals("Wrong changetype", ChangeTypeType.MODIFY, objectDeltaType.getChangeType());
    	assertEquals("Wrong OID", "12345", objectDeltaType.getOid());
    	List<ItemDeltaType> modifications = objectDeltaType.getModification();
    	assertNotNull("null modifications", modifications);
    	assertEquals("Wrong number of modifications", 1, modifications.size());
    	ItemDeltaType mod1 = modifications.iterator().next();
    	assertEquals("Wrong mod type", ModificationTypeType.REPLACE, mod1.getModificationType());
    	XPathHolder xpath = new XPathHolder(mod1.getPath());
    	assertEquals("Wrong path", path.allExceptLast(), xpath.toPropertyPath());
    	List<Object> valueElements = mod1.getValue().getAny();
    	assertEquals("Wrong number of value elements", 1, valueElements.size());
    	JAXBElement<ProtectedStringType> valueElement = (JAXBElement<ProtectedStringType>)valueElements.iterator().next();
    	assertEquals("Wrong element name", PasswordType.F_VALUE, valueElement.getName());
    	assertEquals("Wrong element value", protectedString, valueElement.getValue());
    }
    
    @Test
    public void testObjectDeltaRoundtrip() throws Exception {
    	System.out.println("===[ testObjectDeltaRoundtrip ]====");

    	// GIVEN
    	final String OID = "13235545";
    	final String VALUE = "Very Costly Center";
    	ObjectDelta<UserType> objectDelta = ObjectDelta.createModificationReplaceProperty(UserType.class, OID,
    			UserType.F_COST_CENTER, PrismTestUtil.getPrismContext(), VALUE);

    	System.out.println("ObjectDelta");
    	System.out.println(objectDelta.dump());

    	// WHEN
    	ObjectDeltaType objectDeltaType = DeltaConvertor.toObjectDeltaType(objectDelta);

    	// THEN
    	System.out.println("ObjectDeltaType (XML)");
    	System.out.println(PrismTestUtil.marshalWrap(objectDeltaType));
    	
    	assertEquals("Wrong changetype", ChangeTypeType.MODIFY, objectDeltaType.getChangeType());
    	assertEquals("Wrong OID", OID, objectDeltaType.getOid());
    	List<ItemDeltaType> modifications = objectDeltaType.getModification();
    	assertNotNull("null modifications", modifications);
    	assertEquals("Wrong number of modifications", 1, modifications.size());
    	ItemDeltaType mod1 = modifications.iterator().next();
    	assertEquals("Wrong mod type", ModificationTypeType.REPLACE, mod1.getModificationType());
    	XPathHolder xpath = new XPathHolder(mod1.getPath());
    	assertTrue("Wrong path: "+xpath, xpath.toPropertyPath().isEmpty());
    	List<Object> valueElements = mod1.getValue().getAny();
    	assertEquals("Wrong number of value elements", 1, valueElements.size());
    	Element valueElement = (Element)valueElements.iterator().next();
    	assertEquals("Wrong element name", UserType.F_COST_CENTER, DOMUtil.getQName(valueElement));
    	assertEquals("Wrong element value", VALUE, valueElement.getTextContent());
    	
    	// WHEN
    	ObjectDelta<Objectable> objectDeltaRoundtrip = DeltaConvertor.createObjectDelta(objectDeltaType, PrismTestUtil.getPrismContext());
    	
    	// THEN
    	System.out.println("ObjectDelta (roundtrip)");
    	System.out.println(objectDelta.dump());
    	
    	assertTrue("Roundtrip not equals", objectDelta.equals(objectDeltaRoundtrip));
    	
    	// TODO: more checks
    }
}

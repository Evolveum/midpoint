/*
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.schema;

import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.SerializationUtil;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.List;


/**
 * @author semancik
 *
 */
public class TestSerialization {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void testSerializeMessage() throws Exception {
		System.out.println("===[ testSerializeMessage ]===");

		PrismContext prismContext = PrismTestUtil.getPrismContext();
		SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
		LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
		QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");
		String xml = prismContext.xmlSerializer().serializeAnyData(localizableMessageBean, fakeQName);
		System.out.println(xml);
	}

	@Test
	public void testSerializeExecuteCredentialResetResponseType() throws Exception {
		System.out.println("===[ testSerializeExecuteCredentialResetResponseType ]===");

		PrismContext prismContext = PrismTestUtil.getPrismContext();

		SingleLocalizableMessage localizableMessage = new SingleLocalizableMessage("execute.reset.credential.bad.request", null, "Failed to execute reset password. Bad request.");
		LocalizableMessageType localizableMessageBean = LocalizationUtil.createLocalizableMessageType(localizableMessage);
		ExecuteCredentialResetResponseType response = new ExecuteCredentialResetResponseType();
		response.setMessage(localizableMessageBean);
		QName fakeQName = new QName(PrismConstants.NS_TYPES, "object");

		prismContext.adopt(response);

		String xml = prismContext.xmlSerializer().serializeAnyData(response, fakeQName);
		System.out.println(xml);
	}

	@Test
	public void testSerializeResource() throws Exception {
		System.out.println("===[ testSerializeResource ]===");

		serializationRoundTrip(TestConstants.RESOURCE_FILE);
	}

	@Test
	public void testSerializeUser() throws Exception {
		System.out.println("===[ testSerializeUser ]===");

		serializationRoundTrip(TestConstants.USER_FILE);
	}

	@Test
	public void testSerializeRole() throws Exception {
		System.out.println("===[ testSerializeRole ]===");

		PrismContext prismContext = PrismTestUtil.getPrismContext();

		PrismObject<RoleType> parsedObject = prismContext.parseObject(TestConstants.ROLE_FILE);

		System.out.println("Parsed object:");
		System.out.println(parsedObject.debugDump());

		RoleType parsedRoleType = parsedObject.asObjectable();
		PolicyConstraintsType policyConstraints = parsedRoleType.getPolicyConstraints();
		List<MultiplicityPolicyConstraintType> minAssignees = policyConstraints.getMinAssignees();
		minAssignees.iterator().next();

		// WHEN
		serializationRoundTripPrismObject(parsedObject);
		serializationRoundTripObjectType(parsedRoleType);

		// WHEN
		String serializedMinAssignees = SerializationUtil.toString(minAssignees);
		List<MultiplicityPolicyConstraintType> deserializedMinAssignees = SerializationUtil.fromString(serializedMinAssignees);
		assertTrue("minAssignees mismatch: expected "+minAssignees+", was "+deserializedMinAssignees, MiscUtil.listEquals(minAssignees, deserializedMinAssignees));
	}

	private <O extends ObjectType> void serializationRoundTrip(File file) throws Exception {
		PrismContext prismContext = PrismTestUtil.getPrismContext();

		PrismObject<O> parsedObject = prismContext.parseObject(file);

		System.out.println("\nParsed object:");
		System.out.println(parsedObject.debugDump());

		serializationRoundTripPrismObject(parsedObject);
		serializationRoundTripObjectType(parsedObject.asObjectable());
	}

	private <O extends ObjectType> void serializationRoundTripPrismObject(PrismObject<O> parsedObject) throws Exception {

		// WHEN
		String serializedObject = SerializationUtil.toString(parsedObject);

		// THEN
		System.out.println("\nSerialized object:");
		System.out.println(serializedObject);
		PrismObject<O> deserializedObject = SerializationUtil.fromString(serializedObject);

		System.out.println("\nDeserialized object (PrismObject):");
		System.out.println(deserializedObject.debugDump());

		ObjectDelta<O> diff = parsedObject.diff(deserializedObject);
		assertTrue("Something changed in serialization of "+parsedObject+" (PrismObject): "+diff, diff.isEmpty());
	}

	private <O extends ObjectType> void serializationRoundTripObjectType(O parsedObject) throws Exception {

		// WHEN
		String serializedObject = SerializationUtil.toString(parsedObject);

		// THEN
		O deserializedObject = SerializationUtil.fromString(serializedObject);

		System.out.println("Deserialized object (ObjectType):");
		System.out.println(deserializedObject.asPrismObject().debugDump());

		ObjectDelta<O> diff = parsedObject.asPrismObject().diff((PrismObject) deserializedObject.asPrismObject());
		assertTrue("Something changed in serializetion of "+parsedObject+" (ObjectType): "+diff, diff.isEmpty());
	}

}

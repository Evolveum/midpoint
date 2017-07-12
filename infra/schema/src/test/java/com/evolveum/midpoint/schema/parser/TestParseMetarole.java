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
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

import static com.evolveum.midpoint.schema.TestConstants.METAROLE_FILE_BASENAME;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
public class TestParseMetarole extends AbstractObjectParserTest<RoleType> {

	@Override
	protected File getFile() {
		return getFile(METAROLE_FILE_BASENAME);
	}

	@Test
	public void testParseFileAsPCV() throws Exception {
		displayTestTitle("testParseFileAsPCV");
		processParsingsPCV(null, null);
	}

	@Test
	public void testParseFileAsPO() throws Exception {
		displayTestTitle("testParseFileAsPO");
		processParsingsPO(null, null, true);
	}

	@Test
	public void testParseRoundTripAsPCV() throws Exception{
		displayTestTitle("testParseRoundTripAsPCV");

		processParsingsPCV(v -> getPrismContext().serializerFor(language).serialize(v), "s0");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2");		// misleading item name
		processParsingsPCV(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asContainerable()), "s3");
		processParsingsPCV(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	@Test
	public void testParseRoundTripAsPO() throws Exception{
		displayTestTitle("testParseRoundTripAsPO");

		processParsingsPO(v -> getPrismContext().serializerFor(language).serialize(v), "s0", true);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_SYSTEM_CONFIGURATION).serialize(v), "s2", false);		// misleading item name
		processParsingsPO(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asObjectable()), "s3", false);
		processParsingsPO(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asObjectable()), "s4", false);
	}

	private void processParsingsPCV(SerializingFunction<PrismContainerValue<RoleType>> serializer, String serId) throws Exception {
		processParsings(RoleType.class, null, RoleType.COMPLEX_TYPE, null, serializer, serId);
	}

	private void processParsingsPO(SerializingFunction<PrismObject<RoleType>> serializer, String serId, boolean checkItemName) throws Exception {
		processObjectParsings(RoleType.class, RoleType.COMPLEX_TYPE, serializer, serId, checkItemName);
	}

	@Override
	protected void assertPrismContainerValueLocal(PrismContainerValue<RoleType> value) throws SchemaException {
		PrismObject user = value.asContainerable().asPrismObject();
		user.checkConsistence();
		assertMetarolePrism(user, false);
		assertMetaroleJaxb(value.asContainerable(), false);
	}

	@Override
	protected void assertPrismObjectLocal(PrismObject<RoleType> metarole) throws SchemaException {
		assertMetarolePrism(metarole, true);
		assertMetaroleJaxb(metarole.asObjectable(), true);
		metarole.checkConsistence(true, true);
	}


	private void assertMetarolePrism(PrismObject<RoleType> metarole, boolean isObject) {
		if (isObject) {
			assertEquals("Wrong oid", "12345678-d34d-b33f-f00d-55555555a020", metarole.getOid());
		}
		PrismObjectDefinition<RoleType> usedDefinition = metarole.getDefinition();
		assertNotNull("No metarole definition", usedDefinition);
		PrismAsserts.assertObjectDefinition(usedDefinition, new QName(SchemaConstantsGenerated.NS_COMMON, "role"),
				RoleType.COMPLEX_TYPE, RoleType.class);
		assertEquals("Wrong class in role", RoleType.class, metarole.getCompileTimeClass());
		RoleType roleType = metarole.asObjectable();
		assertNotNull("asObjectable resulted in null", roleType);
		
		assertPropertyValue(metarole, "name", PrismTestUtil.createPolyString("Generic Metarole"));
		assertPropertyDefinition(metarole, "name", PolyStringType.COMPLEX_TYPE, 0, 1);

		PrismContainer<?> assignmentConstraints = metarole.findContainer(
				new ItemPath(RoleType.F_INDUCEMENT, 2L, AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS, PolicyConstraintsType.F_ASSIGNMENT));
		assertNotNull("No assignment constraints", assignmentConstraints);
		assertEquals("Wrong # of assignment constraints", 1, assignmentConstraints.size());
		assertTrue("Wrong (not empty) assignment constraints", assignmentConstraints.getValues().get(0).isEmpty());
	}
	
	private void assertMetaroleJaxb(RoleType roleType, boolean isObject) throws SchemaException {
		assertEquals("Wrong name", PrismTestUtil.createPolyStringType("Generic Metarole"), roleType.getName());
		boolean found = false;
		for (AssignmentType inducement : roleType.getInducement()) {
			if (inducement.getId() == 2L) {
				found = true;
				PolicyRuleType rule = inducement.getPolicyRule();
				assertNotNull("No constraints", rule.getPolicyConstraints());
				assertEquals("Wrong # of assignment constraints", 1, rule.getPolicyConstraints().getAssignment().size());
				assertTrue("Wrong (not empty) assignment constraints", rule.getPolicyConstraints().getAssignment().get(0).asPrismContainerValue().isEmpty());
				break;
			}
		}
		assertTrue("Assignment number 2 was not found", found);
	}



}

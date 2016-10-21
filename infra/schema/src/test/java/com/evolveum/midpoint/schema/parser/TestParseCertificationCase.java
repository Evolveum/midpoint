/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.util.Collection;

import static com.evolveum.midpoint.schema.TestConstants.CERTIFICATION_CASE_FILE_BASENAME;
import static org.testng.AssertJUnit.*;

/**
 * @author semancik
 *
 */
@SuppressWarnings("Convert2MethodRef")
public class TestParseCertificationCase extends AbstractParserTest {

	@FunctionalInterface
	interface ParsingFunction {
		PrismContainerValue<AccessCertificationCaseType> apply(PrismParser prismParser) throws Exception;
	}

	@FunctionalInterface
	interface SerializingFunction {
		String apply(PrismContainerValue<AccessCertificationCaseType> value) throws Exception;
	}

	@Test
	public void testParseFile() throws Exception {
		displayTestTitle("testParseFile");
		processParsings(null, null);
	}

	private void process(String desc, ParsingFunction parser, SerializingFunction serializer, String serId) throws Exception {
		PrismContext prismContext = getPrismContext();

		System.out.println("================== Starting test for '" + desc + "' (serializer: " + serId + ") ==================");

		PrismContainerValue<AccessCertificationCaseType> pcv =
				parser.apply(prismContext.parserFor(getFile(CERTIFICATION_CASE_FILE_BASENAME)));

		System.out.println("Parsed certification case: " + desc);
		System.out.println(pcv.debugDump());

		assertCase(pcv);

		if (serializer != null) {

			String serialized = serializer.apply(pcv);
			System.out.println("Serialized:\n" + serialized);

			PrismContainerValue<AccessCertificationCaseType> reparsed =
					parser.apply(prismContext.parserFor(serialized));

			System.out.println("Reparsed: " + desc);
			System.out.println(reparsed.debugDump());

			assertCase(reparsed);
			assertTrue("Values not equal", pcv.equals(reparsed));

			Collection<? extends ItemDelta> deltas = pcv.diff(reparsed);
			assertTrue("Deltas not empty", deltas.isEmpty());
		}
	}

	@Test
	public void testParseRoundTrip() throws Exception{
		displayTestTitle("testParseRoundTrip");

		//processParsings(v -> getPrismContext().serializerFor(language).serialize(v));													// no item name nor definition => cannot serialize
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
		processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");		// misleading item name
		processParsings(v -> getPrismContext().serializerFor(language).serializeRealValue(v.asContainerable()), "s3");
		processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
	}

	private void processParsings(SerializingFunction serializer, String serId) throws Exception {
		process("parseItemValue - no hint", p -> p.parseItemValue(), serializer, serId);

		process("parseItemValue - AccessCertificationCaseType.class",
				p -> p.type(AccessCertificationCaseType.class).parseItemValue(),
				serializer, serId);

		process("parseItemValue - AccessCertificationAssignmentCaseType.class",
				p -> p.type(AccessCertificationAssignmentCaseType.class).parseItemValue(),
				serializer, serId);

		process("parseItemValue - AccessCertificationCaseType (QName)",
				p -> p.type(AccessCertificationCaseType.COMPLEX_TYPE).parseItemValue(),
				serializer, serId);

		process("parseItemValue - AccessCertificationAssignmentCaseType (QName)",
				p -> p.type(AccessCertificationAssignmentCaseType.COMPLEX_TYPE).parseItemValue(),
				serializer, serId);

		process("parseRealValue - no hint",
				p -> ((AccessCertificationCaseType) p.parseRealValue()).asPrismContainerValue(),
				serializer, serId);

		process("parseRealValue (AccessCertificationCaseType.class)",
				p -> p.parseRealValue(AccessCertificationCaseType.class).asPrismContainerValue(),
				serializer, serId);

		process("parseRealValue (AccessCertificationAssignmentCaseType.class)",
				p -> p.parseRealValue(AccessCertificationAssignmentCaseType.class).asPrismContainerValue(),
				serializer, serId);

		process("parseRealValue - AccessCertificationCaseType (QName)",
				p -> ((AccessCertificationCaseType)
						p.type(AccessCertificationCaseType.COMPLEX_TYPE).parseRealValue())
						.asPrismContainerValue(),
				serializer, serId);

		process("parseRealValue - AccessCertificationAssignmentCaseType (QName)",
				p -> ((AccessCertificationCaseType)
						p.type(AccessCertificationAssignmentCaseType.COMPLEX_TYPE).parseRealValue())
						.asPrismContainerValue(),
				serializer, serId);

		process("parseAnyData",
				p -> ((PrismContainer<AccessCertificationCaseType>) p.parseItemOrRealValue()).getValue(0),
				serializer, serId);
	}

	private PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}

	void assertCase(PrismContainerValue<AccessCertificationCaseType> pcv) throws SchemaException {

		//pcv.checkConsistence();
		assertPrismValue(pcv);
		assertJaxb(pcv.asContainerable());
		
		//pcv.checkConsistence(true, true);
	}

	private void assertPrismValue(PrismContainerValue<AccessCertificationCaseType> pcv) {
		assertEquals("Wrong id", (Long) 4L, pcv.getId());
		ComplexTypeDefinition ctd = pcv.getComplexTypeDefinition();
		assertNotNull("No CTD", ctd);
		//noinspection ConstantConditions
		assertEquals("Wrong CTD typeName", AccessCertificationAssignmentCaseType.COMPLEX_TYPE, ctd.getTypeName());
		assertEquals("Wrong real class in PCV", AccessCertificationAssignmentCaseType.class, pcv.getRealClass());
	}
	
	private void assertJaxb(AccessCertificationCaseType aCase) throws SchemaException {
		PrismAsserts.assertRefEquivalent("Wrong objectRef",
				new PrismReferenceValue("ee53eba7-5c16-4c16-ad15-dd6a2360ab1a", UserType.COMPLEX_TYPE),
				aCase.getObjectRef().asReferenceValue());
		PrismAsserts.assertRefEquivalent("Wrong targetRef",
				new PrismReferenceValue("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", ResourceType.COMPLEX_TYPE),
				aCase.getTargetRef().asReferenceValue());

		assertTrue(aCase instanceof AccessCertificationAssignmentCaseType);
		AccessCertificationAssignmentCaseType assignmentCase = (AccessCertificationAssignmentCaseType) aCase;

		assertNotNull("no assignment", assignmentCase.getAssignment());
		assertEquals((Long) 1L, assignmentCase.getAssignment().getId());
		PrismAsserts.assertRefEquivalent("Wrong resourceRef in assignment",
				new PrismReferenceValue("ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2", ResourceType.COMPLEX_TYPE),
				assignmentCase.getAssignment().getConstruction().getResourceRef().asReferenceValue());
		assertEquals("wrong isInducement", Boolean.FALSE, assignmentCase.isIsInducement());
	}

}

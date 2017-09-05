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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationAssignmentCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;

import java.io.File;

import static com.evolveum.midpoint.schema.TestConstants.CERTIFICATION_CASE_FILE_BASENAME;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@SuppressWarnings("Convert2MethodRef")
public class TestParseCertificationCase extends AbstractContainerValueParserTest<AccessCertificationCaseType> {

	@Override
	protected File getFile() {
		return getFile(CERTIFICATION_CASE_FILE_BASENAME);
	}

	@Test
	public void testParseFile() throws Exception {
		displayTestTitle("testParseFile");
		processParsings(null, null);
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

	private void processParsings(SerializingFunction<PrismContainerValue<AccessCertificationCaseType>> serializer, String serId) throws Exception {
		processParsings(AccessCertificationCaseType.class, AccessCertificationAssignmentCaseType.class,
				AccessCertificationCaseType.COMPLEX_TYPE, AccessCertificationAssignmentCaseType.COMPLEX_TYPE, serializer, serId);
	}

	public void assertPrismContainerValueLocal(PrismContainerValue<AccessCertificationCaseType> value) throws SchemaException {
		assertPrismValue(value);
		assertJaxb(value.asContainerable());
	}

	protected void assertPrismValue(PrismContainerValue<AccessCertificationCaseType> pcv) {
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

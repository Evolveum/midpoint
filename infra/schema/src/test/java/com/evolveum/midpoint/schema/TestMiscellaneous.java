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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static org.testng.AssertJUnit.fail;

/**
 * @author mederly
 *
 */
public class TestMiscellaneous {

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@Test
	public void singleValuedItems() throws Exception {
		System.out.println("===[ singleValuedItems ]===");

		UserType userBean = getPrismContext().createObjectable(UserType.class)
				.beginAssignment()
						.id(1L)
						.targetRef(new ObjectReferenceType().oid("123456").type(RoleType.COMPLEX_TYPE))
				.end();

		//noinspection unchecked
		PrismContainerValue<AssignmentType> assignmentPcv = userBean.getAssignment().get(0).asPrismContainerValue();
		PrismContainer<Containerable> limitContentPc = assignmentPcv
				.findOrCreateContainer(AssignmentType.F_LIMIT_TARGET_CONTENT);
		PrismContainerValue<Containerable> val1 = limitContentPc.createNewValue();
		val1.setId(1L);
		PrismContainerValue<Containerable> val2 = val1.clone();
		val2.setId(2L);
		try {
			limitContentPc.add(val2);
			fail("unexpected success");
		} catch (SchemaException e) {
			System.out.println("Got expected exception: " + e);
		}
	}

}

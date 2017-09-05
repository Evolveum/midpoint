/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 *
 */
public class TestPerformance {

	private static final int ITERATIONS = 10000;


	@BeforeSuite
	public void setupDebug() {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}

    /**
     * Construct object with schema. Starts by instantiating a definition and working downwards.
     * All the items in the object should have proper definition.
     */
	@Test
	public void testPerfContainerNewValue() throws Exception {
		final String TEST_NAME = "testPerfContainerNewValue";
		PrismInternalTestUtil.displayTestTitle(TEST_NAME);

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		PrismObject<UserType> user = userDefinition.instantiate();
		PrismContainer<AssignmentType> assignmentContainer = user.findOrCreateContainer(UserType.F_ASSIGNMENT);
		PerfRecorder recorderCreateNewValue = new PerfRecorder("createNewValue");
		PerfRecorder recorderFindOrCreateProperty = new PerfRecorder("findOrCreateProperty");
		PerfRecorder recorderSetRealValue = new PerfRecorder("setRealValue");

		// WHEN
		for (int i=0; i < ITERATIONS; i++) {
			long tsStart = System.nanoTime();

			PrismContainerValue<AssignmentType> newValue = assignmentContainer.createNewValue();

			long ts1 = System.nanoTime();

			PrismProperty<String> descriptionProperty = newValue.findOrCreateProperty(AssignmentType.F_DESCRIPTION);

			long ts2 = System.nanoTime();

			descriptionProperty.setRealValue("ass "+i);

			long tsEnd = System.nanoTime();

			recorderCreateNewValue.record(i, ((double)(ts1 - tsStart))/1000000);
			recorderFindOrCreateProperty.record(i, ((double)(ts2 - ts1))/1000000);
			recorderSetRealValue.record(i, ((double)(tsEnd - ts2))/1000000);

			System.out.println("Run "+i+": total "+(((double)(tsEnd - tsStart))/1000000)+"ms");
		}

		// THEN
		System.out.println(recorderCreateNewValue.dump());
		System.out.println(recorderFindOrCreateProperty.dump());
		System.out.println(recorderCreateNewValue.dump());

		// Do not assert maximum here. The maximum values may jump around
		// quite wildly (e.g. because of garbage collector runs?)
		recorderCreateNewValue.assertAverageBelow(0.05D);

		recorderFindOrCreateProperty.assertAverageBelow(0.1D);

		recorderCreateNewValue.assertAverageBelow(0.05D);

		System.out.println("User:");
		System.out.println(user.debugDump());
	}

}

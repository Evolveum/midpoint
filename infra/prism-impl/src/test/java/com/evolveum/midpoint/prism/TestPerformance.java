/*
 * Copyright (c) 2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 */
public class TestPerformance {

    private static final int ITERATIONS = 10_000;

    private static final double NANOS_TO_MILLIS_DOUBLE = 1_000_000d;

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
        PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO, "user"));
        PrismObject<UserType> user = userDefinition.instantiate();
        PrismContainer<AssignmentType> assignmentContainer = user.findOrCreateContainer(UserType.F_ASSIGNMENT);
        PerfRecorder recorderCreateNewValue = new PerfRecorder("createNewValue");
        PerfRecorder recorderFindOrCreateProperty = new PerfRecorder("findOrCreateProperty");
        PerfRecorder recorderSetRealValue = new PerfRecorder("setRealValue");

        // WHEN
        for (int i = 0; i < ITERATIONS; i++) {
            long tsStart = System.nanoTime();

            PrismContainerValue<AssignmentType> newValue = assignmentContainer.createNewValue();

            long ts1 = System.nanoTime();

            PrismProperty<String> descriptionProperty = newValue.findOrCreateProperty(AssignmentType.F_DESCRIPTION);

            long ts2 = System.nanoTime();

            descriptionProperty.setRealValue("ass " + i);

            long tsEnd = System.nanoTime();

            recorderCreateNewValue.record(i, (ts1 - tsStart) / NANOS_TO_MILLIS_DOUBLE);
            recorderFindOrCreateProperty.record(i, (ts2 - ts1) / NANOS_TO_MILLIS_DOUBLE);
            recorderSetRealValue.record(i, (tsEnd - ts2) / NANOS_TO_MILLIS_DOUBLE);

            System.out.println("Run " + i + ": total " + ((tsEnd - tsStart) / NANOS_TO_MILLIS_DOUBLE) + "ms");
        }

        // THEN
        System.out.println(recorderCreateNewValue.dump());
        System.out.println(recorderFindOrCreateProperty.dump());
        System.out.println(recorderCreateNewValue.dump());

        // Do not assert maximum here. The maximum values may jump around
        // quite wildly (e.g. because of garbage collector runs?)
        recorderCreateNewValue.assertAverageBelow(0.05d);
        recorderFindOrCreateProperty.assertAverageBelow(0.1d);
        recorderCreateNewValue.assertAverageBelow(0.05d);

        assertThat(assignmentContainer.size()).isEqualTo(ITERATIONS);
        // we skip the 20k-line dump, it's heavy on some (*cough*Windows) consoles and crashes JVM
    }
}

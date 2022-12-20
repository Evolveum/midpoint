/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.prism.ObjectDeltaAsserter;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;

/**
 * Asserts prepositions about {@link ProcessedObject} instances.
 *
 * (This class is in `model.test` package. Would the {@link ProcessedObject} class move e.g. to `schema` module, this asserter
 * can be moved to a lower layer as well.)
 */
public class ProcessedObjectAsserter<O extends ObjectType, RA> extends AbstractAsserter<RA> {

    private final ProcessedObject<O> processedObject;

    ProcessedObjectAsserter(@NotNull ProcessedObject<O> processedObject, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.processedObject =
                MiscUtil.requireNonNull(
                        processedObject, () -> new AssertionError("no processed object"));
    }

    public ProcessedObjectAsserter<O, RA> assertState(ObjectProcessingStateType expected) {
        assertThat(processedObject.getState()).as("processing state").isEqualTo(expected);
        return this;
    }

    public ProcessedObjectAsserter<O, RA> assertName(String expectedOrig) {
        assertThat(getOrig(processedObject.getName())).as("object name (orig)").isEqualTo(expectedOrig);
        return this;
    }

    public ProcessedObjectAsserter<O, RA> assertType(Class<?> expected) {
        assertThat(processedObject.getType()).as("object type").isEqualTo(expected);
        return this;
    }

    @SafeVarargs
    public final ProcessedObjectAsserter<O, RA> assertEventTags(TestResource<TagType>... expectedTags) {
        Set<String> expectedTagsOids = Arrays.stream(expectedTags)
                .map(r -> r.oid)
                .collect(Collectors.toSet());
        assertThat(processedObject.getEventTags())
                .as("event tags")
                .containsExactlyInAnyOrderElementsOf(expectedTagsOids);
        return this;
    }

    public ObjectDeltaAsserter<O, ProcessedObjectAsserter<O, RA>> delta() {
        ObjectDelta<O> delta = processedObject.getDelta();
        assertThat(delta).as("delta").isNotNull();
        ObjectDeltaAsserter<O, ProcessedObjectAsserter<O, RA>> asserter =
                new ObjectDeltaAsserter<>(delta, this, "delta in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O, ProcessedObjectAsserter<O, RA>> objectBefore() {
        PrismObject<O> object = asPrismObject(processedObject.getBefore());
        assertThat(object).as("object before").isNotNull();
        PrismObjectAsserter<O, ProcessedObjectAsserter<O, RA>> asserter =
                new PrismObjectAsserter<>(object, this, "object before in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public PrismObjectAsserter<O, ProcessedObjectAsserter<O, RA>> objectAfter() {
        PrismObject<O> object = asPrismObject(processedObject.getAfter());
        assertThat(object).as("object after").isNotNull();
        PrismObjectAsserter<O, ProcessedObjectAsserter<O, RA>> asserter =
                new PrismObjectAsserter<>(object, this, "object after in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    protected String desc() {
        return descWithDetails(processedObject);
    }

    public ProcessedObjectAsserter<O, RA> display() {
        display(desc());
        return this;
    }

    public ProcessedObjectAsserter<O, RA> display(String message) {
        PrismTestUtil.display(message, processedObject);
        return this;
    }
}

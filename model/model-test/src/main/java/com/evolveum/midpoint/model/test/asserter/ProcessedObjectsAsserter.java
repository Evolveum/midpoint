/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Asserts on the collections of {@link ProcessedObject} instances.
 */
@SuppressWarnings("WeakerAccess")
public class ProcessedObjectsAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final Collection<? extends ProcessedObject<?>> processedObjects;

    ProcessedObjectsAsserter(Collection<? extends ProcessedObject<?>> processedObjects, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.processedObjects = emptyIfNull(processedObjects);
    }

    public static ProcessedObjectsAsserter<Void> forObjects(
            Collection<? extends ProcessedObject<?>> processedObjects, String details) {
        return new ProcessedObjectsAsserter<>(processedObjects, null, details);
    }

    public @NotNull Collection<? extends ProcessedObject<?>> getProcessedObjects() {
        return processedObjects;
    }

    public ProcessedObjectsAsserter<RA> assertSize(int expected) {
        assertThat(processedObjects).as("processed objects").hasSize(expected);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public ProcessedObjectsAsserter<RA> assertSizeBetween(int min, int max) {
        assertThat(processedObjects).as("processed objects")
                .hasSizeGreaterThanOrEqualTo(min)
                .hasSizeLessThanOrEqualTo(max);
        return this;
    }

    public <O extends ObjectType> ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>> single() {
        assertSize(1);
        //noinspection unchecked
        ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>> asserter =
                new ProcessedObjectAsserter<>(
                        (ProcessedObject<O>) processedObjects.iterator().next(),
                        this,
                        "single object in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public ProcessedObjectsAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(processedObjects));
        return this;
    }

    public ProcessedObjectFinder<RA> by() {
        return new ProcessedObjectFinder<>(this);
    }

    public <O extends ObjectType> ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>> spawn(ProcessedObject<O> delta, String message) {
        var asserter = new ProcessedObjectAsserter<>(delta, this, message + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }
}

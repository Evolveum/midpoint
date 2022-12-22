/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test.asserter.prism;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Asserts on the collections of deltas
 */
@SuppressWarnings("WeakerAccess")
public class DeltaCollectionAsserter<RA> extends AbstractAsserter<RA> {

    @NotNull private final Collection<? extends ObjectDelta<?>> deltaCollection;

    DeltaCollectionAsserter(Collection<? extends ObjectDelta<?>> deltaCollection, RA returnAsserter, String details) {
        super(returnAsserter, details);
        this.deltaCollection = emptyIfNull(deltaCollection);
    }

    public static DeltaCollectionAsserter<Void> forDeltas(Collection<? extends ObjectDelta<?>> deltas, String details) {
        return new DeltaCollectionAsserter<>(deltas, null, details);
    }

    public @NotNull Collection<? extends ObjectDelta<?>> getDeltaCollection() {
        return deltaCollection;
    }

    public DeltaCollectionAsserter<RA> assertSize(int expected) {
        assertThat(deltaCollection).as("delta collection").hasSize(expected);
        return this;
    }

    public <O extends ObjectType> ObjectDeltaAsserter<O, DeltaCollectionAsserter<RA>> single() {
        assertSize(1);
        // We know that the delta may not be of ObjectType - but usually it will be
        //noinspection unchecked
        ObjectDeltaAsserter<O, DeltaCollectionAsserter<RA>> asserter =
                new ObjectDeltaAsserter<>(
                        (ObjectDelta<O>) deltaCollection.iterator().next(), this, "single delta in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    @Override
    protected String desc() {
        return getDetails();
    }

    public DeltaCollectionAsserter<RA> display() {
        IntegrationTestTools.display(desc(), DebugUtil.debugDump(deltaCollection));
        return this;
    }

    public ObjectDeltaFinder<RA> by() {
        return new ObjectDeltaFinder<>(this);
    }

    public <O extends ObjectType> ObjectDeltaAsserter<O, DeltaCollectionAsserter<RA>> spawn(ObjectDelta<O> delta, String message) {
        var asserter = new ObjectDeltaAsserter<>(delta, this, message + " in " + desc());
        copySetupTo(asserter);
        return asserter;
    }
}

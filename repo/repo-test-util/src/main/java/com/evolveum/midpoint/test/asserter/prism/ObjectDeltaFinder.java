/*
 * Copyright (c) 2018-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.test.asserter.prism;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import static org.assertj.core.api.Assertions.assertThat;

public class ObjectDeltaFinder<RA> {

    private final DeltaCollectionAsserter<RA> deltaCollectionAsserter;
    private Class<? extends ObjectType> objectType;
    private String objectOid;
    private ChangeType changeType;
    private Integer index;

    ObjectDeltaFinder(DeltaCollectionAsserter<RA> deltaCollectionAsserter) {
        this.deltaCollectionAsserter = deltaCollectionAsserter;
    }

    public ObjectDeltaFinder<RA> objectType(Class<? extends ObjectType> objectType) {
        this.objectType = objectType;
        return this;
    }

    public ObjectDeltaFinder<RA> objectOid(String objectOid) {
        this.objectOid = objectOid;
        return this;
    }

    public ObjectDeltaFinder<RA> changeType(ChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    public ObjectDeltaFinder<RA> index(Integer index) {
        this.index = index;
        return this;
    }

    /** Returns to the parent asserter. */
    public DeltaCollectionAsserter<RA> assertCount(int expected) {
        assertThat(select()).as("matching deltas").hasSize(expected);
        return deltaCollectionAsserter;
    }

    private List<ObjectDelta<?>> select() {
        List<ObjectDelta<?>> selected = new ArrayList<>();
        int currentIndex = -1;
        for (ObjectDelta<?> delta : deltaCollectionAsserter.getDeltaCollection()) {
            if (objectType != null && !Objects.equals(delta.getObjectTypeClass(), objectType)) {
                continue;
            }
            if (objectOid != null && !Objects.equals(delta.getOid(), objectOid)) {
                continue;
            }
            if (changeType != null && delta.getChangeType() != changeType) {
                continue;
            }
            if (index != null) {
                // Searching by values + by index
                currentIndex++;
                if (currentIndex == index) {
                    return List.of(delta);
                }
            } else {
                selected.add(delta);
            }
        }
        return selected;
    }

    public <O extends ObjectType> ObjectDeltaAsserter<O, DeltaCollectionAsserter<RA>> find() {
        List<ObjectDelta<?>> found = select();
        if (found.isEmpty()) {
            fail("Found no delta that matches search criteria: " + this);
        } else if (found.size() > 1) {
            fail("Found more than one delta that matches search criteria: " + this + ": " + found);
        }
        //noinspection unchecked
        return deltaCollectionAsserter.spawn((ObjectDelta<O>) found.get(0), "selected delta");
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    @Override
    public String toString() {
        return "ObjectDeltaFinder{" +
                "objectType=" + objectType +
                ", objectOid=" + objectOid +
                ", changeType=" + changeType +
                ", index=" + index +
                '}';
    }
}

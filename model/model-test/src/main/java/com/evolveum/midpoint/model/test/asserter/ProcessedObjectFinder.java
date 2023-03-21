/*
 * Copyright (c) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.test.asserter;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.testng.AssertJUnit;

import com.evolveum.midpoint.model.api.simulation.ProcessedObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectProcessingStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ProcessedObjectFinder<RA> {

    private final ProcessedObjectsAsserter<RA> processedObjectsAsserter;
    private Class<? extends ObjectType> objectType;
    private String objectOid;
    private ObjectProcessingStateType state;
    private ChangeType changeType;
    private String eventMarkOid;
    private String withoutEventMarkOid;
    private boolean noEventMarks;
    private String resourceOid;
    private Integer index;

    ProcessedObjectFinder(ProcessedObjectsAsserter<RA> processedObjectsAsserter) {
        this.processedObjectsAsserter = processedObjectsAsserter;
    }

    public ProcessedObjectFinder<RA> objectType(Class<? extends ObjectType> objectType) {
        this.objectType = objectType;
        return this;
    }

    public ProcessedObjectFinder<RA> objectOid(String objectOid) {
        this.objectOid = objectOid;
        return this;
    }

    public ProcessedObjectFinder<RA> state(ObjectProcessingStateType state) {
        this.state = state;
        return this;
    }

    public ProcessedObjectFinder<RA> changeType(ChangeType changeType) {
        this.changeType = changeType;
        return this;
    }

    public ProcessedObjectFinder<RA> eventMarkOid(String eventMarkOid) {
        this.eventMarkOid = eventMarkOid;
        return this;
    }

    public ProcessedObjectFinder<RA> withoutEventMarkOid(String eventMarkOid) {
        this.withoutEventMarkOid = eventMarkOid;
        return this;
    }

    public ProcessedObjectFinder<RA> noEventMarks() {
        this.noEventMarks = true;
        return this;
    }

    public ProcessedObjectFinder<RA> resourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
        return this;
    }

    public ProcessedObjectFinder<RA> index(Integer index) {
        this.index = index;
        return this;
    }

    public ProcessedObjectFinder<RA> assertCount(int expected) {
        assertThat(select()).as("matching objects").hasSize(expected);
        return this;
    }

    public ProcessedObjectFinder<RA> assertNames(String... expectedOrig) {
        Set<String> names = select().stream()
                .map(po -> getOrig(po.getName()))
                .collect(Collectors.toSet());
        assertThat(names).as("matching objects names").containsExactlyInAnyOrder(expectedOrig);
        return this;
    }

    public ProcessedObjectsAsserter<RA> end() {
        return processedObjectsAsserter;
    }

    private List<ProcessedObject<?>> select() {
        List<ProcessedObject<?>> selected = new ArrayList<>();
        int currentIndex = -1;
        for (ProcessedObject<?> processedObject : processedObjectsAsserter.getProcessedObjects()) {
            if (objectType != null && !Objects.equals(processedObject.getType(), objectType)) {
                continue;
            }
            if (objectOid != null && !Objects.equals(processedObject.getOid(), objectOid)) {
                continue;
            }
            if (state != null && processedObject.getState() != state) {
                continue;
            }
            ObjectDelta<?> delta = processedObject.getDelta();
            if (changeType != null && (delta == null || delta.getChangeType() != changeType)) {
                continue;
            }
            if (eventMarkOid != null && !processedObject.hasEventMark(eventMarkOid)) {
                continue;
            }
            if (withoutEventMarkOid != null && processedObject.hasEventMark(withoutEventMarkOid)) {
                continue;
            }
            if (noEventMarks && !processedObject.hasNoEventMarks()) {
                continue;
            }
            if (resourceOid != null && !resourceOid.equals(processedObject.getResourceOid())) {
                continue;
            }
            if (index != null) {
                // Searching by values + by index
                currentIndex++;
                if (currentIndex == index) {
                    return List.of(processedObject);
                }
            } else {
                selected.add(processedObject);
            }
        }
        return selected;
    }

    public <O extends ObjectType> ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>> find() {
        List<ProcessedObject<?>> found = select();
        if (found.isEmpty()) {
            fail("Found no object that matches search criteria: " + this);
        } else if (found.size() > 1) {
            fail("Found more than one object that matches search criteria: " + this + ": " + found);
        }
        //noinspection unchecked
        return processedObjectsAsserter.spawn((ProcessedObject<O>) found.get(0), "selected delta");
    }

    /** Experimental variant of {@link #find()} not requiring turning auto-formatter off.  */
    @Experimental
    public <O extends ObjectType> ProcessedObjectsAsserter<RA> find(
            Function<ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>>, ProcessedObjectAsserter<O, ProcessedObjectsAsserter<RA>>> function) {
        return function
                .apply(find())
                .end();
    }

    protected void fail(String message) {
        AssertJUnit.fail(message);
    }

    @Override
    public String toString() {
        List<String> components = new ArrayList<>();
        if (objectType != null) {
            components.add("objectType=" + objectType);
        }
        if (objectOid != null) {
            components.add("objectOid='" + objectOid + "'");
        }
        if (state != null) {
            components.add("state=" + state);
        }
        if (changeType != null) {
            components.add("changeType=" + changeType);
        }
        if (eventMarkOid != null) {
            components.add("eventMarkOid=" + eventMarkOid);
        }
        if (noEventMarks) {
            components.add("noEventMarks");
        }
        if (index != null) {
            components.add("index=" + index);
        }
        return "ProcessedObjectFinder{" + String.join(", ", components) + "}";
    }
}

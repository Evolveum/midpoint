/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter.prism;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.test.asserter.AbstractAsserter;
import com.evolveum.midpoint.test.asserter.ContainerDeltaAsserter;
import com.evolveum.midpoint.test.asserter.PropertyDeltaAsserter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.collect.Sets;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author semancik
 */
public class ObjectDeltaAsserter<O extends ObjectType,RA> extends AbstractAsserter<RA> {

    private final ObjectDelta<O> delta;

    public ObjectDeltaAsserter(ObjectDelta<O> delta) {
        super();
        this.delta = delta;
    }

    public ObjectDeltaAsserter(ObjectDelta<O> delta, String detail) {
        super(detail);
        this.delta = delta;
    }

    public ObjectDeltaAsserter(ObjectDelta<O> delta, RA returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.delta = delta;
    }

    public static <O extends ObjectType> ObjectDeltaAsserter<O,Void> forDelta(ObjectDelta<O> delta, String detail) {
        return new ObjectDeltaAsserter<>(delta, detail);
    }

    public ObjectDeltaAsserter<O,RA> assertAdd() {
        assertChangeType(ChangeType.ADD);
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertModify() {
        assertChangeType(ChangeType.MODIFY);
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertDelete() {
        assertChangeType(ChangeType.DELETE);
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertChangeType(ChangeType expected) {
        assertEquals("Wrong change type in "+desc(), expected, delta.getChangeType());
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertObjectTypeClass(Class<? extends ObjectType> expected) {
        assertEquals("Wrong object type class in "+desc(), expected, delta.getObjectTypeClass());
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertOid(String expected) {
        assertEquals("Wrong OID in "+desc(), expected, delta.getOid());
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertOid() {
        assertNotNull("No OID in "+desc(), delta.getOid());
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertModifications(int expected) {
        assertEquals("Wrong number of modifications in "+desc(), expected, delta.getModifications().size());
        return this;
    }

    /** Asserts that the specified paths are modified, and nothing else. Supports prefixes. */
    public ObjectDeltaAsserter<O,RA> assertModifiedExclusive(ItemPath... expectedPaths) {
        assertModified(expectedPaths);
        assertNotModifiedExcept(expectedPaths);
        return this;
    }

    /** Asserts that (something) in every specified path is modified. Supports prefixes. */
    @SuppressWarnings({ "UnusedReturnValue", "WeakerAccess" })
    public ObjectDeltaAsserter<O,RA> assertModified(ItemPath... pathsThatMustBeModified) {
        PathSet modifiedPaths = delta.getModifications().stream()
                .map(ItemDelta::getPath)
                .collect(Collectors.toCollection(() -> new PathSet()));
        for (ItemPath pathThatMustBeModified : pathsThatMustBeModified) {
            if (!ItemPathCollectionsUtil.containsSuperpathOrEquivalent(modifiedPaths, pathThatMustBeModified)) {
                fail(String.format("Assumed that path %s should be modified but it is not. Modified paths: %s",
                        pathThatMustBeModified, modifiedPaths));
            }
        }
        return this;
    }

    /** Asserts that _nothing_ in any specified path is modified. Supports prefixes. */
    @SuppressWarnings("WeakerAccess")
    public ObjectDeltaAsserter<O,RA> assertNotModified(ItemPath... pathsThatMustNotBeModified) {
        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            ItemPath modifiedPath = modification.getPath();
            if (ItemPathCollectionsUtil.containsSubpathOrEquivalent(List.of(pathsThatMustNotBeModified), modifiedPath)) {
                fail(String.format("Assumed that paths %s should not be modified but there is: %s",
                        Arrays.toString(pathsThatMustNotBeModified), modification));
            }
        }
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertNoRealResourceObjectModifications() {
        return assertNotModified(
                ShadowType.F_ATTRIBUTES, ShadowType.F_CREDENTIALS, ShadowType.F_AUXILIARY_OBJECT_CLASS);
    }

    /** Asserts that nothing except for specified paths is modified. Supports prefixes. */
    public ObjectDeltaAsserter<O,RA> assertNotModifiedExcept(ItemPath... pathsThatCanBeModified) {
        for (ItemDelta<?, ?> modification : delta.getModifications()) {
            ItemPath modifiedPath = modification.getPath();
            if (!ItemPathCollectionsUtil.containsSubpathOrEquivalent(List.of(pathsThatCanBeModified), modifiedPath)) {
                fail(String.format("Assumed that nothing except %s should be modified but there is: %s",
                        Arrays.toString(pathsThatCanBeModified), modification));
            }
        }
        return this;
    }

    public ObjectDeltaAsserter<O,RA> assertPolyStringModification(
            ItemPath path, String expectedOldOrig, String expectedAddOrReplaceOrig) {
        PropertyDelta<PolyString> propertyDelta = delta.findPropertyDelta(path);
        assertThat(propertyDelta).as("delta for '" + path + "'").isNotNull();
        Set<String> realAddOrReplaceOrig =
                Sets.union(
                        getOrig(propertyDelta.getRealValuesToAdd()),
                        getOrig(propertyDelta.getRealValuesToReplace()));
        assertThat(realAddOrReplaceOrig)
                .as("values added or replaced")
                .containsExactlyInAnyOrder(expectedAddOrReplaceOrig);
        Set<String> realOldOrig = getOrig(
                PrismValueCollectionsUtil.getRealValuesOfCollection(
                        propertyDelta.getEstimatedOldValues()));
        assertThat(realOldOrig)
                .as("old values")
                .containsExactlyInAnyOrder(expectedOldOrig);
        return this;
    }

    private Set<String> getOrig(Collection<?> polyStrings) {
        return emptyIfNull(polyStrings).stream()
                .filter(val -> val instanceof PolyString)
                .map(ps -> ((PolyString) ps).getOrig())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /** Asserts that the specified paths are modified (among other ones - optionally). Checks the equivalence of paths. */
    public ObjectDeltaAsserter<O,RA> assertModificationPathsNonExclusive(ItemPath... expectedPaths) {
        return assertModificationPaths(false, expectedPaths);
    }

    /** Asserts that the set of modified paths is exactly the same as expected. Checks the equivalence of paths. */
    public ObjectDeltaAsserter<O,RA> assertModifiedPathsExclusive(ItemPath... expectedPaths) {
        return assertModificationPaths(true, expectedPaths);
    }

    private ObjectDeltaAsserter<O,RA> assertModificationPaths(boolean strict, ItemPath... expectedPaths) {
        assertModify();
        PathSet actualPathSet = delta.getModifications().stream()
                .map(modification -> modification.getPath())
                .collect(Collectors.toCollection(() -> new PathSet()));
        PathSet expectedPathSet = new PathSet(List.of(expectedPaths));
        for (ItemPath expected : expectedPathSet) {
            if (!actualPathSet.contains(expected)) {
                fail("Expected path '" + expected + "' is not among actually modified paths: " + actualPathSet);
            }
        }
        if (strict) {
            for (ItemPath actualPath : actualPathSet) {
                if (!expectedPathSet.contains(actualPath)) {
                    fail("Actual path '" + actualPath + "' is not among expected modified paths: " + expectedPathSet);
                }
            }
        }
        return this;
    }

    public <T> PropertyDeltaAsserter<T,ObjectDeltaAsserter<O,RA>> property(ItemPath path) {
        PropertyDelta<T> propertyDelta = delta.findPropertyDelta(path);
        assertNotNull("No property delta for path "+path+" in "+desc(), propertyDelta);
        PropertyDeltaAsserter<T,ObjectDeltaAsserter<O,RA>> propertyDeltaAsserter = new PropertyDeltaAsserter<>(propertyDelta, this, "property delta for "+path+" in "+desc());
        copySetupTo(propertyDeltaAsserter);
        return propertyDeltaAsserter;
    }

    public <C extends Containerable> ContainerDeltaAsserter<C,ObjectDeltaAsserter<O,RA>> container(ItemPath path) {
        ContainerDelta<C> containerDelta = delta.findContainerDelta(path);
        assertNotNull("No container delta for path "+path+" in "+desc(), containerDelta);
        ContainerDeltaAsserter<C,ObjectDeltaAsserter<O,RA>> containerDeltaAsserter = new ContainerDeltaAsserter<>(containerDelta, this, "container delta for "+path+" in "+desc());
        copySetupTo(containerDeltaAsserter);
        return containerDeltaAsserter;
    }

    public PrismObjectAsserter<O, ObjectDeltaAsserter<O, RA>> objectToAdd() {
        PrismObject<O> objectToAdd = delta.getObjectToAdd();
        assertThat(objectToAdd).as("object to add").isNotNull();
        PrismObjectAsserter<O, ObjectDeltaAsserter<O, RA>> asserter =
                new PrismObjectAsserter<>(objectToAdd, this, "object to add in " + desc());
        copySetupTo(asserter);
        return asserter;
    }

    public String getOid() {
        return delta.getOid();
    }

    protected String desc() {
        return descWithDetails(delta);
    }

    public ObjectDeltaAsserter<O,RA> display() {
        display(desc());
        return this;
    }

    public ObjectDeltaAsserter<O,RA> display(String message) {
        PrismTestUtil.display(message, delta);
        return this;
    }
}

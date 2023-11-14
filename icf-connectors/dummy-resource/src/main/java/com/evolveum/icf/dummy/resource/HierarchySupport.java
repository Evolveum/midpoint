/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Supports LDAP-like object hierarchies for dummy resources.
 *
 * If enabled, each object has a name of `segment1:segment2:...:segmentN`.
 * The name is read from bottom to top, similar to LDAP DN (like `cn=segment1,ou=segment2,ou=segment3,...,ou=segmentN`).
 *
 * This means that there must exist an org named `segment2:...:segmentN` for any object whose name consists of more
 * than one segment. Single-segment names correspond to top-level objects that do not reside in any particular org.
 *
 * When an org is renamed, all contained objects are renamed as well. An org cannot be deleted if it contains any object.
 *
 * Normally, this option should not be used with {@link DummyResource#UID_MODE_NAME}.
 * {@link DummyResource#enforceUniqueName} must be set to `true` for this option to work.
 *
 * See MID-8929.
 */
class HierarchySupport {

    @NotNull private final DummyResource dummyResource;

    private boolean enabled;

    HierarchySupport(@NotNull DummyResource dummyResource) {
        this.dummyResource = dummyResource;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks that the containing (parent) org exists for a given object name.
     *
     * Assumes being synchronized on {@link DummyResource} instance.
     */
    void checkHasContainingOrg(String name) throws ObjectDoesNotExistException {
        if (!isCorrectlyEnabled()) {
            return;
        }
        String normName = normalize(name);
        HierarchicalName parentNormHName = HierarchicalName.of(normName).getParent();
        if (parentNormHName == null) {
            return; // object is top level
        }
        String parentNormName = parentNormHName.asString();
        if (!dummyResource.containsOrg(parentNormName)) {
            throw new ObjectDoesNotExistException(
                    "Cannot add object with name '%s' because its parent org '%s' does not exist".formatted(
                            normName, parentNormName));
        }
    }

    /** Returns true if hierarchy support is enabled. Checks the related settings as well, to provide basic sanity. */
    private boolean isCorrectlyEnabled() {
        if (!enabled) {
            return false;
        }
        if (!dummyResource.isEnforceUniqueName()) {
            // Checked also in resource configuration validity check, here we do that just for safety.
            throw new IllegalStateException("Cannot use hierarchy support if 'enforceUniqueName' is not set");
        }
        return true;
    }

    /**
     * Checks that the object does not contain any other objects.
     *
     * Assumes being synchronized on {@link DummyResource} instance.
     */
    void checkNoContainedObjects(DummyObject object) throws SchemaViolationException {
        if (!isCorrectlyEnabled() || !(object instanceof DummyOrg)) {
            return;
        }
        String orgNormName = normalize(object.getName());
        HierarchicalName orgNormHName = HierarchicalName.of(orgNormName);
        Optional<DummyObject> sampleContainedObject = dummyResource.getAllObjectsStream()
                .filter(o -> o.containedByOrg(orgNormHName))
                .findAny();
        if (sampleContainedObject.isPresent()) {
            // Not quite ideal, but we have nothing better now.
            throw new SchemaViolationException("Cannot delete org '%s' because it contains objects, e.g. '%s'".formatted(
                    orgNormName, sampleContainedObject.get()));
        }
    }

    void updateNormalizedHierarchicalName(@NotNull DummyObject object) {
        if (isCorrectlyEnabled()) {
            String name = object.getName();
            object.setNormalizedHierarchicalName(
                    name != null ?
                            HierarchicalName.of(normalize(name)) : null);
        }
    }

    private String normalize(String name) {
        return dummyResource.normalize(name);
    }

    private HierarchicalName getNormalizedHName(String name) {
        return HierarchicalName.of(normalize(name));
    }

    /**
     * Renames all objects contained in the given object (presumably org).
     *
     * Assumes being synchronized on {@link DummyResource} instance.
     */
    void renameContainedObjects(DummyObject object, String oldName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException {
        if (!isCorrectlyEnabled() || !(object instanceof DummyOrg)) {
            return;
        }
        HierarchicalName oldOrgNormHName = HierarchicalName.of(normalize(oldName));
        HierarchicalName newOrgNormHName = getNormalizedHName(object.getName());
        List<DummyObject> containedObjects = dummyResource.getAllObjectsStream()
                .filter(o -> o.containedByOrg(oldOrgNormHName))
                .toList();
        for (DummyObject containedObject : containedObjects) {
            renameContainedObject(containedObject, oldOrgNormHName, newOrgNormHName);
        }
    }

    private <T extends DummyObject> void renameContainedObject(
            T object, HierarchicalName oldOrgNormHName, HierarchicalName newOrgNormHName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException {
        String oldObjectNormName = normalize(object.getName());
        HierarchicalName oldObjectNormHName = getNormalizedHName(object.getName());
        HierarchicalName newObjectNormHName = oldObjectNormHName.move(oldOrgNormHName, newOrgNormHName);
        String newObjectNormName = newObjectNormHName.asString();
        object.setName(newObjectNormName);
        Map<String, T> map = dummyResource.getSpecificObjectMap(object);
        //noinspection unchecked
        DummyResource.updateSpecificObjectMap(map, (Class<T>) object.getClass(), oldObjectNormName, newObjectNormName);
    }
}

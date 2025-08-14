/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.icf.dummy.resource;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stores objects of given object class.
 */
public class ObjectStore<O extends DummyObject> implements DebugDumpable {

    @NotNull private final Class<O> objectJavaType;

    @NotNull private final String objectClassName;

    @NotNull private final DummyObjectClass objectClass;

    private final String description;

    /** Objects, indexed by the normalized name (if enforcing unique names), or by {@link DummyObject#id} (otherwise). */
    @NotNull private final Map<String, O> objectMap = Collections.synchronizedMap(new LinkedHashMap<>());

    ObjectStore(@NotNull Class<O> objectJavaType, @NotNull String objectClassName, @NotNull DummyObjectClass objectClass) {
        this(objectJavaType, objectClassName, objectClass, null);
    }

    ObjectStore(@NotNull Class<O> objectJavaType, @NotNull String objectClassName, @NotNull DummyObjectClass objectClass,
            String description) {
        this.objectJavaType = objectJavaType;
        this.objectClassName = objectClassName;
        this.objectClass = objectClass;
        this.description = description;
    }

    public @NotNull String getObjectClassName() {
        return objectClassName;
    }

    public @NotNull Map<String, O> getObjectMap() {
        return objectMap;
    }

    public @NotNull Collection<O> getObjects() {
        return Collections.unmodifiableCollection(objectMap.values());
    }

    public String getDescription() {
        return description;
    }

    public void clear() {
        objectMap.clear();
    }

    public void reset() {
        clear();
        objectClass.clear();
    }

    O getObject(String normalizedNameOrId) {
        return objectMap.get(normalizedNameOrId);
    }

    boolean containsObject(String normalizedNameOrId) {
        return objectMap.containsKey(normalizedNameOrId);
    }

    void putObject(String normalizedNameOrId, O object) {
        objectMap.put(normalizedNameOrId, object);
    }

    void removeObject(String normalizedNameOrId) {
        objectMap.remove(normalizedNameOrId);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilder("Object store for " + objectClassName, indent);
        sb.append("\n");
        DebugUtil.debugDumpMapMultiLine(sb, objectMap, indent + 1);
        return sb.toString();
    }

    public O renameObject(String oldNormName, String newNormName)
            throws ObjectAlreadyExistsException, ObjectDoesNotExistException {
        O existingObject = getObject(oldNormName);
        if (existingObject == null) {
            throw new ObjectDoesNotExistException(
                    "Cannot rename, " + getSimpleTypeName() + " with name '" + oldNormName + "' does not exist");
        }
        if (containsObject(newNormName)) {
            throw new ObjectAlreadyExistsException(
                    "Cannot rename, " + getSimpleTypeName() + " with name '" + newNormName + "' already exists");
        }
        putObject(newNormName, existingObject);
        removeObject(oldNormName);
        return existingObject;
    }

    private String getSimpleTypeName() {
        return objectJavaType.getSimpleName();
    }

    public int size() {
        return objectMap.size();
    }

    public @NotNull DummyObjectClass getObjectClass() {
        return objectClass;
    }
}

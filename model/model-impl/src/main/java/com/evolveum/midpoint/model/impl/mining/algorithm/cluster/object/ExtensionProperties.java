/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.mining.algorithm.cluster.object;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * Represents extension properties associated with a data point.
 */
public class ExtensionProperties implements Serializable {
    ListMultimap<AttributeMatch, String> object;

    /**
     * Constructs extension properties with an empty ListMultimap.
     */
    public ExtensionProperties() {
        this.object = ArrayListMultimap.create();
    }

    /**
     * Constructs extension properties with the given ListMultimap.
     *
     * @param object The ListMultimap representing extension properties.
     */
    public ExtensionProperties(ListMultimap<AttributeMatch, String> object) {
        this.object = object;
    }

    /**
     * Adds a property to the extension properties.
     *
     * @param key The key of the property.
     * @param value The value of the property.
     */
    public void addProperty(AttributeMatch key, String value) {
        object.put(key, value);
    }

    /**
     * Gets all values associated with a specific key.
     *
     * @param key The key for which values are retrieved.
     * @return The list of values associated with the key.
     */
    public List<String> getValuesForKey(AttributeMatch key) {
        return object.get(key);
    }

    public Set<String> getSetValuesForKeys(AttributeMatch key) {
        List<String> list = getValuesForKey(key);
        return new HashSet<>(list);
    }

    /**
     * Removes a specific property.
     *
     * @param key The key of the property to be removed.
     * @param value The value of the property to be removed.
     * @return true if the property was removed, false otherwise.
     */
    public boolean removeProperty(AttributeMatch key, String value) {
        return object.remove(key, value);
    }

    /**
     * Checks if the extension properties contain a specific key.
     *
     * @param key The key to check for existence.
     * @return true if the key is present, false otherwise.
     */
    public boolean containsKey(AttributeMatch key) {
        return object.containsKey(key);
    }

    /**
     * Clears all extension properties.
     */
    public void clearProperties() {
        object.clear();
    }

    /**
     * Returns the underlying ListMultimap.
     *
     * @return The ListMultimap representing extension properties.
     */
    public ListMultimap<AttributeMatch, String> getObject() {
        return object;
    }

    public Set<AttributeMatch> getAttributesMatch() {
        if (getObject() != null) {
            return getObject().keySet();
        } else {
            return null;
        }
    }

    public String getSingleValueForKey(AttributeMatch key) {
        List<String> values = object.get(key);
        return values.isEmpty() ? null : values.get(0);
    }
}


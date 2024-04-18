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
    ListMultimap<RoleAnalysisAttributeDefConvert, String> object;

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
    public ExtensionProperties(ListMultimap<RoleAnalysisAttributeDefConvert, String> object) {
        this.object = object;
    }

    /**
     * Adds a property to the extension properties.
     *
     * @param key The key of the property.
     * @param value The value of the property.
     */
    public void addProperty(RoleAnalysisAttributeDefConvert key, String value) {
        object.put(key, value);
    }

    /**
     * Gets all values associated with a specific key.
     *
     * @param key The key for which values are retrieved.
     * @return The list of values associated with the key.
     */
    public List<String> getValuesForKey(RoleAnalysisAttributeDefConvert key) {
        return object.get(key);
    }

    public Set<String> getSetValuesForKeys(RoleAnalysisAttributeDefConvert key) {
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
    public boolean removeProperty(RoleAnalysisAttributeDefConvert key, String value) {
        return object.remove(key, value);
    }

    /**
     * Checks if the extension properties contain a specific key.
     *
     * @param key The key to check for existence.
     * @return true if the key is present, false otherwise.
     */
    public boolean containsKey(RoleAnalysisAttributeDefConvert key) {
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
    public ListMultimap<RoleAnalysisAttributeDefConvert, String> getObject() {
        return object;
    }

    public Set<RoleAnalysisAttributeDefConvert> getAttributesMatch() {
        if (getObject() != null) {
            return getObject().keySet();
        } else {
            return null;
        }
    }

    public String getSingleValueForKey(RoleAnalysisAttributeDefConvert key) {
        List<String> values = object.get(key);
        return values.isEmpty() ? null : values.get(0);
    }
}


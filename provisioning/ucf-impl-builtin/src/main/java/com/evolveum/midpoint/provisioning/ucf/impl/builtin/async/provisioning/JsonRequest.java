/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.Map;

/**
 * A translation of OperationRequested that is directly serializable to JSON form by {@link JsonRequestFormatter}.
 */
public class JsonRequest {

    /**
     * Operation name (add, modify, delete).
     */
    private String operation;

    /**
     * Object class name (qualified or unqualified).
     */
    private String objectClass;

    /**
     * Attributes. Keys are qualified or unqualified attribute names. Values are collections of real values.
     * Usually empty for non-ADD operations.
     */
    private Map<String, Collection<?>> attributes;

    /**
     * Primary identifiers. Keys are qualified or unqualified attribute names. Values are collections of real values.
     * Usually empty for ADD operations.
     */
    private Map<String, Collection<?>> primaryIdentifiers;

    /**
     * Secondary identifiers. Keys are qualified or unqualified attribute names. Values are collections of real values.
     * Usually empty for ADD operations.
     */
    private Map<String, Collection<?>> secondaryIdentifiers;

    /**
     * Modifications. Present only for MODIFY operations. Can be replaced by attributes (in the case of REPLACE modifications)
     * if requested.
     */
    private Map<String, DeltaValues> changes;

    /**
     * An extension point for any custom information.
     */
    private Object additionalInformation;

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public Map<String, Collection<?>> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Collection<?>> attributes) {
        this.attributes = attributes;
    }

    public Map<String, Collection<?>> getPrimaryIdentifiers() {
        return primaryIdentifiers;
    }

    public void setPrimaryIdentifiers(Map<String, Collection<?>> primaryIdentifiers) {
        this.primaryIdentifiers = primaryIdentifiers;
    }

    public Map<String, Collection<?>> getSecondaryIdentifiers() {
        return secondaryIdentifiers;
    }

    public void setSecondaryIdentifiers(Map<String, Collection<?>> secondaryIdentifiers) {
        this.secondaryIdentifiers = secondaryIdentifiers;
    }

    public Map<String, DeltaValues> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, DeltaValues> changes) {
        this.changes = changes;
    }

    public Object getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Object additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    /**
     * Simple representation of an ItemDelta: contains collections of real values being
     * added/deleted/replaced.
     */
    public static class DeltaValues {
        private Collection<?> add;
        private Collection<?> delete;
        private Collection<?> replace;

        public DeltaValues() {
        }

        public DeltaValues(Collection<?> add, Collection<?> delete, Collection<?> replace) {
            this.add = add;
            this.delete = delete;
            this.replace = replace;
        }

        public Collection<?> getAdd() {
            return add;
        }

        public void setAdd(Collection<?> add) {
            this.add = add;
        }

        public Collection<?> getDelete() {
            return delete;
        }

        public void setDelete(Collection<?> delete) {
            this.delete = delete;
        }

        public Collection<?> getReplace() {
            return replace;
        }

        public void setReplace(Collection<?> replace) {
            this.replace = replace;
        }
    }

    public static JsonRequest from(String req) throws JsonProcessingException {
        return new ObjectMapper()
                .readerFor(JsonRequest.class)
                .readValue(req);
    }
}

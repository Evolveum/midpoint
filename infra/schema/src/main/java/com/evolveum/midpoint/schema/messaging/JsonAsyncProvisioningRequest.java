/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.messaging;

import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AsyncProvisioningOperationRequestedType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 *     A simplified representation of a requested asynchronous provisioning operation.
 *     MidPoint offers two such built-in representations:
 * </p>
 * <ol>
 *     <li>{@link AsyncProvisioningOperationRequestedType} </li>
 *     <li>this class</li>
 * </ol>
 * <p>
 *     The first one is a direct translation of the operation being requested. It is a Prism
 *     structure that can be serialized to any Prism language (XML, JSON, YAML, Axiom). However,
 *     it requires Prism implementation at the receiving side in order to be easily and completely
 *     parsed.
 * </p>
 * <p>
 *     On the other hand, this class provides a simplified representation (or, better, a class of
 *     representations) of the asynchronous provisioning operation. It can be easily parsed by any
 *     JSON parser without the need of Prism functionality.
 * </p>
 * <p>
 *     This class offers a basic structure for the request. It is up to the user how he/she decides
 *     to use it. For example, individual item and type names can be qualified or unqualified.
 *     "Replace-only" changes can be represented as attributes. And so on.
 * </p>
 * <p>
 *     BEWARE: This class is very EXPERIMENTAL. It can change or disappear at any time.
 * </p>
 */
@Experimental
public class JsonAsyncProvisioningRequest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ADD = "add";
    private static final String MODIFY = "modify";
    private static final String DELETE = "delete";

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
        this.attributes = new HashMap<>(attributes);
    }

    public void addAttributes(Map<String, Collection<?>> added) {
        if (attributes != null) {
            attributes.putAll(added);
        } else {
            setAttributes(added);
        }
    }

    /**
     * Returns simplified version of the attributes map, suitable e.g. for presentation via Velocity.
     * Each attribute with no values or one value is replaced by scalar, i.e. null or the single item.
     * Attributes with multiple values are represented as lists.
     */
    public Map<String, Object> getAttributesSimplified() {
        Map<String, Object> rv = new HashMap<>();
        for (Map.Entry<String, Collection<?>> entry : attributes.entrySet()) {
            String name = entry.getKey();
            Collection<?> values = entry.getValue();
            if (values.isEmpty()) {
                rv.put(name, null);
            } else if (values.size() == 1) {
                rv.put(name, values.iterator().next());
            } else {
                rv.put(name, new ArrayList<>(values));
            }
        }
        return rv;
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

    @JsonIgnore
    public boolean isAdd() {
        return ADD.equals(operation);
    }

    @JsonIgnore
    public boolean isModify() {
        return MODIFY.equals(operation);
    }

    @JsonIgnore
    public boolean isDelete() {
        return DELETE.equals(operation);
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

    public static JsonAsyncProvisioningRequest from(String req) throws JsonProcessingException {
        return MAPPER
                .readerFor(JsonAsyncProvisioningRequest.class)
                .readValue(req);
    }
}

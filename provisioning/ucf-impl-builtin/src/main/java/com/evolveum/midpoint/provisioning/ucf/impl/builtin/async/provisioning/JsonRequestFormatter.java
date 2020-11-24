/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.util.QNameUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Formats JSON request. This is to allow easy creation of JSON requests for asynchronous operations.
 *
 * A user can either use {@link JsonRequest} as is; or request creation of JsonGenerator
 * and write respective fields on his own.
 */
public class JsonRequestFormatter {

    /**
     * Abstract representation of the operation requested.
     */
    @NotNull private final OperationRequested operationRequested;

    /**
     * Concrete request created by this formatter.
     */
    @NotNull private final JsonRequest request = new JsonRequest();

    /**
     * Should we use qualified names for attributes and object class?
     */
    private boolean qualified;

    /**
     * Should we output changes as a plain attributes map? Requires that all changes are of REPLACE type.
     * Useful in connection with 'attributeContentRequirement = all' update capability option.
     */
    private boolean changeMapAsAttributes;

    public JsonRequestFormatter(@NotNull OperationRequested operationRequested) {
        this.operationRequested = operationRequested;
    }

    public boolean isQualified() {
        return qualified;
    }

    public JsonRequestFormatter qualified() {
        qualified = true;
        return this;
    }

    public void setQualified(boolean qualified) {
        this.qualified = qualified;
    }

    public boolean isChangeMapAsAttributes() {
        return changeMapAsAttributes;
    }

    public JsonRequestFormatter changeMapAsAttributes() {
        changeMapAsAttributes = true;
        return this;
    }

    public void setChangeMapAsAttributes(boolean changeMapAsAttributes) {
        this.changeMapAsAttributes = changeMapAsAttributes;
    }

    /**
     * Does default formatting: creates the request and returns its JSON form.
     */
    public String format() throws JsonProcessingException {
        createRequest();
        return toJson();
    }

    /**
     * Returns JSON form of the (pre-prepared) request.
     */
    public String toJson() throws JsonProcessingException {
        return new ObjectMapper()
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(request);
    }

    /**
     * Fills-in the request in the default way.
     * (If you need a custom way of doing this, you can use individual setXXX methods as needed.)
     */
    public void createRequest() {
        setOperationName();
        setObjectClass();
        if (operationRequested instanceof OperationRequested.Add) {
            setAttributes();
        } else if (operationRequested instanceof OperationRequested.Modify) {
            setIdentifiers();
            if (changeMapAsAttributes) {
                setAttributesFromChanges();
            } else {
                setChanges();
            }
        } else if (operationRequested instanceof OperationRequested.Delete) {
            setIdentifiers();
        } else {
            throw new IllegalStateException("Unsupported operation requested: " + operationRequested);
        }
    }

    /** Sets operation name in the request. */
    public void setOperationName() {
        request.setOperation(operationRequested.getSimpleName());
    }

    /** Sets object class name in the request. */
    public void setObjectClass() {
        request.setObjectClass(transformQName(operationRequested.getObjectClassName()));
    }

    /**
     * Sets the attributes in the request. Uses attributes in the shadow as the source.
     * This is obviously fully appropriate for ADD operation. But for MODIFY/DELETE operation
     * the attributes usually contain values _before_ the operation. Nevertheless, it might make
     * some sense to call this method even in these cases.
     */
    public void setAttributes() {
        request.setAttributes(transformAttributesMap(operationRequested.getAttributeValueMap()));
    }

    /**
     * Sets primary and secondary identifiers. Useful for MODIFY and DELETE operations. Can
     * be used for ADD operations as well, if needed.
     */
    public void setIdentifiers() {
        request.setPrimaryIdentifiers(transformAttributesMap(operationRequested.getPrimaryIdentifiersValueMap()));
        request.setSecondaryIdentifiers(transformAttributesMap(operationRequested.getSecondaryIdentifiersValueMap()));
    }

    private Map<String, Collection<?>> transformAttributesMap(Map<ItemName, Collection<?>> attributes) {
        Map<String, Collection<?>> formatted = new HashMap<>();
        for (Map.Entry<ItemName, Collection<?>> entry : attributes.entrySet()) {
            String name = transformQName(entry.getKey());
            if (formatted.containsKey(name)) {
                throw new IllegalStateException("Multiple attributes with name '" + name + "', consider using qualified names");
            }
            formatted.put(name, entry.getValue());
        }
        return formatted;
    }

    /**
     * Sets attributes in the request, using REPLACE changes as the source.
     * Fails if there is a non-REPLACE change. Useful in connection with
     * 'attributeContentRequirement = all' update capability option.
     */
    public void setAttributesFromChanges() {
        Map<ItemName, ItemDelta<?, ?>> changeMap = ((OperationRequested.Modify) operationRequested).getAttributeChangeMap();
        request.setAttributes(transformChangeMapToAttributes(changeMap));
    }

    private Map<String, Collection<?>> transformChangeMapToAttributes(Map<ItemName, ItemDelta<?, ?>> changeMap) {
        Map<String, Collection<?>> formatted = new HashMap<>();
        for (Map.Entry<ItemName, ItemDelta<?, ?>> entry : changeMap.entrySet()) {
            String name = transformQName(entry.getKey());
            if (formatted.containsKey(name)) {
                throw new IllegalStateException("Multiple attribute changes with name '" + name + "', consider using qualified names");
            }
            ItemDelta<?, ?> itemDelta = entry.getValue();
            if (!itemDelta.isReplace()) {
                throw new IllegalStateException("Non-REPLACE item delta cannot be represented as an attribute: " + itemDelta);
            }
            formatted.put(name, itemDelta.getRealValuesToReplace());
        }
        return formatted;
    }

    /**
     * Transforms changes from operation to request.
     */
    public void setChanges() {
        Map<ItemName, ItemDelta<?, ?>> changeMap = ((OperationRequested.Modify) operationRequested).getAttributeChangeMap();
        request.setChanges(transformChangeMap(changeMap));
    }

    private Map<String, JsonRequest.DeltaValues> transformChangeMap(Map<ItemName, ItemDelta<?, ?>> changeMap) {
        Map<String, JsonRequest.DeltaValues> formatted = new HashMap<>();
        for (Map.Entry<ItemName, ItemDelta<?, ?>> entry : changeMap.entrySet()) {
            String name = transformQName(entry.getKey());
            if (formatted.containsKey(name)) {
                throw new IllegalStateException("Multiple attribute changes with name '" + name + "', consider using qualified names");
            }
            formatted.put(name, transformItemDelta(entry.getValue()));
        }
        return formatted;
    }

    private JsonRequest.DeltaValues transformItemDelta(ItemDelta<?, ?> itemDelta) {
        return new JsonRequest.DeltaValues(
                itemDelta.getRealValuesToAdd(),
                itemDelta.getRealValuesToDelete(),
                itemDelta.getRealValuesToReplace());
    }

    @NotNull
    private String transformQName(QName name) {
        if (qualified) {
            return QNameUtil.qNameToUri(name);
        } else {
            return name.getLocalPart();
        }
    }

    public @NotNull OperationRequested getOperationRequested() {
        return operationRequested;
    }

    public @NotNull JsonRequest getRequest() {
        return request;
    }
}

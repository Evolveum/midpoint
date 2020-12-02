/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import java.io.IOException;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncProvisioningRequest;
import com.evolveum.midpoint.provisioning.ucf.api.async.StringAsyncProvisioningRequest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedOperationRequestTransformationType;

/**
 * Provides methods that are useful in transformation scripts.
 * Published as "transformerHelper" object there.
 */
public class TransformerHelper {

    @NotNull private final AsyncProvisioningConnectorInstance connectorInstance;

    TransformerHelper(@NotNull AsyncProvisioningConnectorInstance connectorInstance) {
        this.connectorInstance = connectorInstance;
    }

    /**
     * Converts any containerable to JSON form. Tries to guess element name; or uses c:value instead.
     */
    public StringAsyncProvisioningRequest toJson(Containerable value) throws SchemaException {
        return serialize(value, getPrismContext().jsonSerializer());
    }

    /**
     * Converts any containerable to XML form. Tries to guess element name; or uses c:value instead.
     */
    public StringAsyncProvisioningRequest toXml(Containerable value) throws SchemaException {
        return serialize(value, getPrismContext().xmlSerializer());
    }

    /**
     * Converts any containerable to YAML form. Tries to guess element name; or uses c:value instead.
     */
    public StringAsyncProvisioningRequest toYaml(Containerable value) throws SchemaException {
        return serialize(value, getPrismContext().yamlSerializer());
    }

    public JsonRequestFormatter jsonRequestFormatter(OperationRequested operationRequested) {
        return new JsonRequestFormatter(operationRequested);
    }

    @NotNull
    private StringAsyncProvisioningRequest serialize(Containerable value, @NotNull PrismSerializer<String> serializer)
            throws SchemaException {
        if (value != null) {
            return StringAsyncProvisioningRequest.of(
                    serializer
                            .options(SerializationOptions.createSerializeCompositeObjects())
                            .serializeRealValue(value, getElementName(value)));
        } else {
            return StringAsyncProvisioningRequest.of("");
        }
    }

    private QName getElementName(Containerable value) {
        PrismContainerable parent = value.asPrismContainerValue().getParent();
        if (parent != null) {
            return parent.getElementName();
        }
        PrismContainerDefinition<? extends Containerable> pcd = getPrismContext().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(value.getClass());
        if (pcd != null) {
            return pcd.getItemName();
        }
        return SchemaConstants.C_VALUE;
    }

    // Beware, this is not available at object creation time. Prism context is injected to the connector
    // instance afterwards.
    private PrismContext getPrismContext() {
        return connectorInstance.getPrismContext();
    }

    public AsyncProvisioningRequest applyPredefinedTransformation(@NotNull OperationRequested operationRequested,
            @NotNull PredefinedOperationRequestTransformationType predefinedTransformation) throws SchemaException, IOException {
        switch (predefinedTransformation) {
            case FULL_JSON:
                return toJson(operationRequested.asBean());
            case FULL_JSON_WITHOUT_SHADOW:
                return toJson(operationRequested.asBeanWithoutShadow());
            case SIMPLIFIED_JSON:
                return StringAsyncProvisioningRequest.of(
                        jsonRequestFormatter(operationRequested)
                                .format());
            case SIMPLIFIED_QUALIFIED_JSON:
                return StringAsyncProvisioningRequest.of(
                        jsonRequestFormatter(operationRequested)
                                .qualified()
                                .format());
            default:
                throw new AssertionError(predefinedTransformation);
        }
    }
}

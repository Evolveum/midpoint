/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api.connectors;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

/**
 * @author semancik
 *
 */
@Experimental
public abstract class AbstractManagedConnectorInstance implements ConnectorInstance {

    private ConnectorType connectorObject;
    private PrismSchema connectorConfigurationSchema;
    private String resourceSchemaNamespace;
    private PrismContext prismContext;

    private PrismContainerValue<?> connectorConfiguration;
    private ResourceSchema resourceSchema = null;
    private CapabilityCollectionType capabilities = null;
    private boolean configured = false;

    private String instanceName; // resource name
    private String resourceOid; // FIXME temporary -- remove when no longer needed (MID-5931)

    public ConnectorType getConnectorObject() {
        return connectorObject;
    }

    public void setConnectorObject(ConnectorType connectorObject) {
        this.connectorObject = connectorObject;
    }

    public void setConnectorConfigurationSchema(PrismSchema connectorConfigurationSchema) {
        this.connectorConfigurationSchema = connectorConfigurationSchema;
    }

    public PrismContainerValue<?> getConnectorConfiguration() {
        return connectorConfiguration;
    }

    public void setConnectorConfiguration(PrismContainerValue<?> connectorConfiguration) {
        this.connectorConfiguration = connectorConfiguration;
    }

    public String getResourceSchemaNamespace() {
        return resourceSchemaNamespace;
    }

    public void setResourceSchemaNamespace(String resourceSchemaNamespace) {
        this.resourceSchemaNamespace = resourceSchemaNamespace;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public void setPrismContext(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    public ResourceSchema getResourceSchema() {
        return resourceSchema;
    }

    protected void setResourceSchema(ResourceSchema resourceSchema) {
        this.resourceSchema = resourceSchema;
    }

    protected CapabilityCollectionType getCapabilities() {
        return capabilities;
    }

    protected void setCapabilities(CapabilityCollectionType capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void initialize(
            ResourceSchema resourceSchema,
            CapabilityCollectionType capabilities,
            boolean caseIgnoreAttributeNames,
            OperationResult parentResult) {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_INITIALIZE);
        result.addContext("connector", getConnectorObject().toString());
        result.addContext(OperationResult.CONTEXT_IMPLEMENTATION_CLASS, this.getClass());

        updateSchema(resourceSchema);
        setCapabilities(capabilities);

        result.recordSuccessIfUnknown();
    }

    @Override
    public void updateSchema(ResourceSchema resourceSchema) {
        setResourceSchema(resourceSchema);
    }

    @Override
    public void configure(
            @NotNull PrismContainerValue<?> configuration,
            @Nullable ConnectorConfigurationOptions options,
            @NotNull OperationResult parentResult)
            throws SchemaException, ConfigurationException {

        OperationResult result = parentResult.createSubresult(ConnectorInstance.OPERATION_CONFIGURE);

        PrismContainerValue<?> mutableConfiguration;
        if (configuration.isImmutable()) {
            mutableConfiguration = configuration.clone();
        } else {
            mutableConfiguration = configuration;
        }

        mutableConfiguration.applyDefinition(getConfigurationContainerDefinition());
        setConnectorConfiguration(mutableConfiguration);
        applyConfigurationToConfigurationClass(mutableConfiguration);

        // TODO: transform configuration in a subclass

        if (configured) {
            disconnect(result);
        }

        connect(result);

        configured = true;

        result.recordSuccessIfUnknown();
    }

    protected abstract void connect(OperationResult result);

    protected abstract void disconnect(OperationResult result);

    protected PrismContainerDefinition<?> getConfigurationContainerDefinition() throws SchemaException {
        QName configContainerQName = new QName(getConnectorObject().getNamespace(),
                ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart());
        PrismContainerDefinition<?> configContainerDef =
                connectorConfigurationSchema.findContainerDefinitionByElementName(configContainerQName);
        if (configContainerDef == null) {
            throw new SchemaException("No definition of container " + configContainerQName
                    + " in configuration schema for connector " + this);
        }
        return configContainerDef;
    }

    private void applyConfigurationToConfigurationClass(PrismContainerValue<?> configurationContainer) throws ConfigurationException {
        BeanWrapper connectorBean = new BeanWrapperImpl(this);
        PropertyDescriptor connectorConfigurationProp = UcfUtil.findAnnotatedProperty(connectorBean, ManagedConnectorConfiguration.class);
        if (connectorConfigurationProp == null) {
            return;
        }
        Class<?> configurationClass = connectorConfigurationProp.getPropertyType();
        Object configurationObject;
        try {
            configurationObject = configurationClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new ConfigurationException("Cannot instantiate configuration "+configurationClass);
        }
        BeanWrapper configurationClassBean = new BeanWrapperImpl(configurationObject);
        for (Item<?, ?> configurationItem: configurationContainer.getItems()) {
            ItemDefinition<?> configurationItemDefinition = configurationItem.getDefinition();
            String itemLocalName = configurationItem.getElementName().getLocalPart();
            if (configurationItemDefinition != null && configurationItemDefinition.isMultiValue()) {
                Object[] realValuesArray = configurationItem.getRealValuesArray(Object.class);
                configurationClassBean.setPropertyValue(itemLocalName, realValuesArray);
            } else {
                Object realValue = configurationItem.getRealValue();
                configurationClassBean.setPropertyValue(itemLocalName, realValue);
            }
        }
        connectorBean.setPropertyValue(connectorConfigurationProp.getName(), configurationObject);
    }

    @Override
    public void dispose() {
        OperationResult result = new OperationResult(ConnectorInstance.OPERATION_DISPOSE);
        disconnect(result);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    private String getConnectorObjectName() {
        return connectorObject != null ? getOrig(connectorObject.getName()) : null;
    }

    @Override
    public String getHumanReadableDescription() {
        return getClass().getSimpleName() + " (" + getConnectorObjectName() + ", " + getInstanceName() + ")";
    }
}

/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.api.connectors;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.provisioning.ucf.api.*;
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
    private Collection<Object> capabilities = null;
    private boolean configured = false;

    private String instanceName; // resource name
    private String resourceOid; // FIXME temporary -- remove when no longer needed (MID-5931)

    public ConnectorType getConnectorObject() {
        return connectorObject;
    }

    public void setConnectorObject(ConnectorType connectorObject) {
        this.connectorObject = connectorObject;
    }

    public PrismSchema getConnectorConfigurationSchema() {
        return connectorConfigurationSchema;
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

    protected Collection<Object> getCapabilities() {
        return capabilities;
    }

    protected void setCapabilities(Collection<Object> capabilities) {
        this.capabilities = capabilities;
    }

    @Override
    public void initialize(ResourceSchema resourceSchema, Collection<Object> capabilities, boolean caseIgnoreAttributeNames,
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
    public void configure(PrismContainerValue<?> configuration, List<QName> generateObjectClasses, OperationResult parentResult)
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
        PrismContainerDefinition<?> configContainerDef = getConnectorConfigurationSchema()
                .findContainerDefinitionByElementName(configContainerQName);
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
            configurationObject = configurationClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConfigurationException("Cannot instantiate configuration "+configurationClass);
        }
        BeanWrapper configurationClassBean = new BeanWrapperImpl(configurationObject);
        for (Item<?, ?> configurationItem: configurationContainer.getItems()) {
            if (! (configurationItem instanceof PrismProperty<?>)) {
                throw new ConfigurationException("Only properties are supported for now");
            }
            PrismProperty<?> configurationProperty = (PrismProperty<?>)configurationItem;
            PrismPropertyDefinition<?> configurationPropertyDefinition = configurationProperty.getDefinition();
            if (configurationPropertyDefinition != null && configurationPropertyDefinition.isMultiValue()) {
                Object[] realValuesArray = configurationProperty.getRealValuesArray(Object.class);
                configurationClassBean.setPropertyValue(configurationProperty.getElementName().getLocalPart(), realValuesArray);
            } else {
                Object realValue = configurationProperty.getRealValue();
                configurationClassBean.setPropertyValue(configurationProperty.getElementName().getLocalPart(), realValue);
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
}

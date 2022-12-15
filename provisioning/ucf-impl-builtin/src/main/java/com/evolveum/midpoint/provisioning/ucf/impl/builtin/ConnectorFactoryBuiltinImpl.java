/*
 * Copyright (c) 2017-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.provisioning.ucf.impl.builtin;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcherAware;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.provisioning.ucf.api.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.security.api.SecurityContextManagerAware;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskManagerAware;
import com.evolveum.midpoint.task.api.Tracer;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.provisioning.ucf.api.connectors.AbstractManagedConnectorInstance;
import com.evolveum.midpoint.repo.api.RepositoryAware;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorHostType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Connector factory for the connectors built-in to midPoint, such as
 * the "manual connector".
 *
 * @author Radovan Semancik
 *
 */
@Component
public class ConnectorFactoryBuiltinImpl implements ConnectorFactory {

    private static final String SCAN_PACKAGE = "com.evolveum.midpoint";

    private static final String CONFIGURATION_NAMESPACE_PREFIX = SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN + "/bundle/";

    private static final Trace LOGGER = TraceManager.getTrace(ConnectorFactoryBuiltinImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private CaseEventDispatcher caseManager;
    @Autowired private TaskManager taskManager;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private UcfExpressionEvaluator ucfExpressionEvaluator;
    @Autowired private Tracer tracer;

    private final Object connectorDiscovery = new Object();

    private Map<String,ConnectorStruct> connectorMap;

    @Override
    public Set<ConnectorType> listConnectors(ConnectorHostType host, OperationResult parentResult) {
        if (host != null) {
            return null;
        }
        discoverConnectorsIfNeeded();
        return connectorMap.values().stream().map(connectorStruct -> connectorStruct.connectorObject).collect(Collectors.toSet());
    }

    private void discoverConnectorsIfNeeded() {
        synchronized (connectorDiscovery) {
            if (connectorMap == null) {
                connectorMap = new HashMap<>();
                ClassPathScanningCandidateComponentProvider scanner =
                        new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(ManagedConnector.class));
                LOGGER.trace("Scanning package {}", SCAN_PACKAGE);
                for (BeanDefinition bd : scanner.findCandidateComponents(SCAN_PACKAGE)) {
                    LOGGER.debug("Found connector class {}", bd);
                    String beanClassName = bd.getBeanClassName();
                    try {
                        Class<?> connectorClass = Class.forName(beanClassName);
                        ManagedConnector annotation = connectorClass.getAnnotation(ManagedConnector.class);
                        String type = annotation.type();
                        LOGGER.debug("Found connector {} class {}", type, connectorClass);
                        ConnectorStruct struct = createConnectorStruct(connectorClass, annotation);
                        connectorMap.put(type, struct);
                    } catch (ClassNotFoundException e) {
                        LOGGER.error("Error loading connector class {}: {}", beanClassName, e.getMessage(), e);
                    } catch (SchemaException e) {
                        LOGGER.error("Error discovering the connector {}: {}", beanClassName, e.getMessage(), e);
                    }
                }
                LOGGER.trace("Scan done");
            }
        }
    }

    private ConnectorStruct createConnectorStruct(Class<?> connectorClass, ManagedConnector annotation) throws SchemaException {
        ConnectorStruct struct = new ConnectorStruct();
        if (!ConnectorInstance.class.isAssignableFrom(connectorClass)) {
            throw new IllegalStateException("Connector class " + connectorClass + " is not of ConnectorInstance type");
        }
        //noinspection unchecked
        struct.connectorClass = (Class<? extends ConnectorInstance>) connectorClass;

        ConnectorType connectorType = new ConnectorType();
        String bundleName = connectorClass.getPackage().getName();
        String type = annotation.type();
        if (type.isEmpty()) {
            type = connectorClass.getSimpleName();
        }
        String version = annotation.version();
        UcfUtil.addConnectorNames(connectorType, "Built-in", bundleName, type, version, null);
        connectorType.setConnectorBundle(bundleName);
        connectorType.setConnectorType(type);
        connectorType.setConnectorVersion(version);
        connectorType.setFramework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN);
        String namespace = CONFIGURATION_NAMESPACE_PREFIX + bundleName + "/" + type;
        connectorType.setNamespace(namespace);

        struct.connectorObject = connectorType;

        PrismSchema connectorSchema = generateConnectorConfigurationSchema(struct);
        //noinspection ConstantConditions (probably can be null in the future)
        if (connectorSchema != null) {
            LOGGER.trace("Generated connector schema for {}: {} definitions",
                    connectorType, connectorSchema.getDefinitions().size());
            UcfUtil.setConnectorSchema(connectorType, connectorSchema);
            struct.connectorConfigurationSchema = connectorSchema;
        } else {
            LOGGER.warn("No connector schema generated for {}", connectorType);
        }

        return struct;
    }

    private ConnectorStruct getConnectorStruct(ConnectorType connectorType) throws ObjectNotFoundException {
        discoverConnectorsIfNeeded();
        String type = connectorType.getConnectorType();
        ConnectorStruct struct = connectorMap.get(type);
        if (struct == null) {
            LOGGER.error("No built-in connector type {}; known types: {}", type, connectorMap);
            throw new ObjectNotFoundException("No built-in connector type " + type, ConnectorType.class, null);
        }
        return struct;
    }

    @Override
    public PrismSchema generateConnectorConfigurationSchema(ConnectorType connectorType)
            throws ObjectNotFoundException {
        ConnectorStruct struct = getConnectorStruct(connectorType);
        return generateConnectorConfigurationSchema(struct);
    }

    private PrismSchema generateConnectorConfigurationSchema(ConnectorStruct struct) {

        Class<? extends ConnectorInstance> connectorClass = struct.connectorClass;

        PropertyDescriptor connectorConfigurationProp = UcfUtil.findAnnotatedProperty(connectorClass, ManagedConnectorConfiguration.class);

        MutablePrismSchema connectorSchema = prismContext.schemaFactory().createPrismSchema(struct.connectorObject.getNamespace());
        // Create configuration type - the type used by the "configuration" element
        MutablePrismContainerDefinition<?> configurationContainerDef = connectorSchema.createContainerDefinition(
                ResourceType.F_CONNECTOR_CONFIGURATION.getLocalPart(),
                SchemaConstants.CONNECTOR_SCHEMA_CONFIGURATION_TYPE_LOCAL_NAME);

        Class<?> configurationClass = connectorConfigurationProp.getPropertyType();
        BeanWrapper configurationClassBean = new BeanWrapperImpl(configurationClass);
        for (PropertyDescriptor prop: configurationClassBean.getPropertyDescriptors()) {
            if (UcfUtil.hasAnnotation(prop, ConfigurationItem.class)) {
                ItemDefinition<?> itemDef = createConfigurationItemDefinition(configurationContainerDef, prop);
                LOGGER.trace("Configuration item definition for {}: {}", prop.getName(), itemDef);
            }
        }

        return connectorSchema;
    }

    private ItemDefinition<?> createConfigurationItemDefinition(MutablePrismContainerDefinition<?> configurationContainerDef,
            PropertyDescriptor prop) {
        String itemLocalName = prop.getName();
        Class<?> itemType = prop.getPropertyType();
        Class<?> baseType;
        int minOccurs = 1;
        int maxOccurs;
        if (!itemType.isArray()) {
            maxOccurs = 1;
            baseType = itemType;
        } else {
            maxOccurs = -1;
            baseType = itemType.getComponentType();
        }
        // TODO: minOccurs: define which properties are optional/mandatory
        // TODO: display names, ordering, help texts
        QName itemTypeName;
        QName standardTypeName = XsdTypeMapper.getJavaToXsdMapping(baseType);
        ComplexTypeDefinition complexTypeDefinition;
        if (standardTypeName != null) {
            complexTypeDefinition = null;
            itemTypeName = standardTypeName;
        } else {
            ItemDefinition<?> itemDef = prismContext.getSchemaRegistry()
                    .findItemDefinitionByCompileTimeClass(baseType, ItemDefinition.class);
            if (itemDef != null) {
                itemTypeName = itemDef.getTypeName();
                if (itemDef instanceof PrismContainerDefinition) {
                    complexTypeDefinition = ((PrismContainerDefinition<?>) itemDef).getComplexTypeDefinition();
                    if (complexTypeDefinition == null) {
                        throw new IllegalStateException("Configuration item " + itemLocalName + " of " + baseType
                                + " is a container without complex type definition");
                    }
                } else {
                    complexTypeDefinition = null;
                }
            } else {
                throw new IllegalStateException("Configuration item " + itemLocalName + " of " + baseType
                        + " cannot be resolved to a XSD type or a prism item type");
            }
        }
        String namespaceURI = configurationContainerDef.getItemName().getNamespaceURI();
        QName itemName = new QName(namespaceURI, itemLocalName);
        if (complexTypeDefinition != null) {
            return configurationContainerDef.createContainerDefinition(itemName, complexTypeDefinition, minOccurs, maxOccurs);
        } else {
            return configurationContainerDef.createPropertyDefinition(itemName, itemTypeName, minOccurs, maxOccurs);
        }
    }

    @Override
    public ConnectorInstance createConnectorInstance(ConnectorType connectorType, String instanceName,
            String desc) throws ObjectNotFoundException {
        ConnectorStruct struct = getConnectorStruct(connectorType);
        Class<? extends ConnectorInstance> connectorClass = struct.connectorClass;
        ConnectorInstance connectorInstance;
        try {
            connectorInstance = connectorClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            // TODO is this really "object not found" exception?
            throw new ObjectNotFoundException(
                    "Cannot create instance of connector " + connectorClass + ": " + e.getMessage(),
                    e,
                    ConnectorInstance.class,
                    instanceName,
                    false);
        }
        if (connectorInstance instanceof AbstractManagedConnectorInstance) {
            setupAbstractConnectorInstance((AbstractManagedConnectorInstance)connectorInstance, instanceName, connectorType,
                    MidPointConstants.NS_RI, struct);
        }
        if (connectorInstance instanceof RepositoryAware) {
            ((RepositoryAware)connectorInstance).setRepositoryService(repositoryService);
        }
        if (connectorInstance instanceof CaseEventDispatcherAware) {
            ((CaseEventDispatcherAware)connectorInstance).setDispatcher(caseManager);
        }
        if (connectorInstance instanceof TaskManagerAware) {
            ((TaskManagerAware)connectorInstance).setTaskManager(taskManager);
        }
        if (connectorInstance instanceof SecurityContextManagerAware) {
            ((SecurityContextManagerAware) connectorInstance).setSecurityContextManager(securityContextManager);
        }
        if (connectorInstance instanceof UcfExpressionEvaluatorAware) {
            ((UcfExpressionEvaluatorAware) connectorInstance).setUcfExpressionEvaluator(ucfExpressionEvaluator);
        }
        if (connectorInstance instanceof TracerAware) {
            ((TracerAware) connectorInstance).setTracer(tracer);
        }
        if (connectorInstance instanceof RepositoryAware) {
            ((RepositoryAware) connectorInstance).setRepositoryService(repositoryService);
        }
        return connectorInstance;
    }

    private void setupAbstractConnectorInstance(AbstractManagedConnectorInstance connectorInstance, String instanceName,
            ConnectorType connectorObject, String namespace,
            ConnectorStruct struct) {
        connectorInstance.setInstanceName(instanceName);
        connectorInstance.setConnectorObject(connectorObject);
        connectorInstance.setResourceSchemaNamespace(namespace);
        connectorInstance.setPrismContext(prismContext);
        connectorInstance.setConnectorConfigurationSchema(struct.connectorConfigurationSchema);
    }

    @Override
    public void selfTest(OperationResult parentTestResult) {
        // Nothing to do
    }

    @Override
    public boolean supportsFramework(String frameworkIdentifier) {
        return SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN.equals(frameworkIdentifier);
    }

    @Override
    public String getFrameworkVersion() {
        return "1.0.0";
    }

    @Override
    public void shutdown() {
        // Nothing to do
    }

    private static class ConnectorStruct {
        private Class<? extends ConnectorInstance> connectorClass;
        private ConnectorType connectorObject;
        private PrismSchema connectorConfigurationSchema;
    }

    @Override
    public void registerDiscoveryListener(ConnectorDiscoveryListener listener) {
        // NOOP
    }

}

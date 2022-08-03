/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectTypeDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.api.DiscoveredConfiguration;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    private static final String DOT_CLASS = ResourceDetailsModel.class.getName() + ".";
    private static final String OPERATION_CREATE_CONFIGURATION_WRAPPERS = DOT_CLASS + "loadConnectorWrapper";
    private static final String OPERATION_FETCH_SCHEMA = DOT_CLASS + "fetchSchema";

    private final LoadableModel<PrismContainerWrapper<ConnectorConfigurationType>> configurationModel;

    private final LoadableModel<List<ObjectClassWrapper>> objectClassesModel;
    private DiscoveredConfiguration connectorConfigurationSuggestions;

    public ResourceDetailsModel(LoadableDetachableModel<PrismObject<ResourceType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.configurationModel = new LoadableModel<>(true) {
            @Override
            protected PrismContainerWrapper<ConnectorConfigurationType> load() {
                OperationResult result = new OperationResult(OPERATION_CREATE_CONFIGURATION_WRAPPERS);
                try {
                    return createConfigContainerWrappers(result);
                } catch (Exception e) {
                    result.recordPartialError("Cannot load conector configuration, " + e.getMessage());
                    getPageBase().showResult(result);
                    return null;
                }
            }
        };

        this.objectClassesModel = new LoadableModel<>() {
            @Override
            protected List<ObjectClassWrapper> load() {
                List<ObjectClassWrapper> list = new ArrayList<>();

                ResourceSchema schema = null;
                OperationResult result = new OperationResult(OPERATION_FETCH_SCHEMA);
                try {
                    schema = getPageBase().getModelService().fetchSchema(getObjectWrapper().getObjectApplyDelta(), result);
                } catch (SchemaException | RuntimeException e) {
                    result.recordFatalError("Cannot load schema object classes, " + e.getMessage());
                    result.computeStatus();
                    getPageBase().showResult(result);
                }


                if (schema == null) {
                    return list;
                }

                for(ResourceObjectClassDefinition definition: schema.getObjectClassDefinitions()){
                    list.add(new ObjectClassWrapper(definition));
                }

                Collections.sort(list);

                return list;
            }
        };
    }

    private PrismContainerWrapper<ConnectorConfigurationType> createConfigContainerWrappers(OperationResult result) throws SchemaException {

        Task task = getModelServiceLocator().createSimpleTask(OPERATION_CREATE_CONFIGURATION_WRAPPERS);
        PrismObjectWrapper<ResourceType> resourceWrapper = getObjectWrapper();

        PrismContainerWrapper<ConnectorConfigurationType> configuration = resourceWrapper.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<ConnectorConfigurationType> connectorConfigurationType = null;

        if (configuration == null || configuration.isEmpty()) {
            PrismReferenceWrapper<Referencable> connectorRef = resourceWrapper.findReference(ResourceType.F_CONNECTOR_REF);
            if (connectorRef == null || connectorRef.getValue() == null || connectorRef.getValue().getRealValue() == null) {
                return null;
            }

            PrismObject<ConnectorType> connector = WebModelServiceUtils.resolveReferenceNoFetch(connectorRef.getValue().getRealValue(), getPageBase(), task, result);
            if (connector == null) {
                return null;
            }
            ConnectorType connectorType = connector.asObjectable();
            PrismSchema schema;
            try {
                schema = ConnectorTypeUtil.parseConnectorSchema(connectorType);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't parse connector schema: " + e.getMessage(), e);
            }
            PrismContainerDefinition<ConnectorConfigurationType> definition =
                    ConnectorTypeUtil.findConfigurationContainerDefinitionRequired(connectorType, schema);
            // Fixing (errorneously) set maxOccurs = unbounded. See MID-2317 and related issues.
            PrismContainerDefinition<ConnectorConfigurationType> definitionFixed = definition.clone();
            definitionFixed.toMutable().setMaxOccurs(1);
            connectorConfigurationType = definitionFixed.instantiate();

            WrapperContext ctx = new WrapperContext(task, result);
            ctx.setShowEmpty(true);
            ctx.setDetailsPageTypeConfiguration(getResourceDetailsPageConfiguration());
            configuration = getModelServiceLocator().createItemWrapper(connectorConfigurationType, ItemStatus.ADDED, ctx);
        }

        return configuration;
    }

    private List<ContainerPanelConfigurationType> getResourceDetailsPageConfiguration() {
        GuiObjectDetailsPageType resourceDetailsPage = getObjectDetailsPageConfiguration().getObject(); //TODO shouldn't be resource details page type?
        if (resourceDetailsPage == null) {
            return Collections.emptyList();
        }
        return resourceDetailsPage.getPanel();
    }

    public LoadableModel<PrismContainerWrapper<ConnectorConfigurationType>> getConfigurationModel() {
        return configurationModel;
    }

    public PrismContainerWrapper<ConnectorConfigurationType> getConfigurationModelObject() {
        return configurationModel.getObject();
    }

    //    @Override
//    protected GuiObjectDetailsPageType loadDetailsPageConfiguration(PrismObject<ResourceType> resource) {
//        GuiObjectDetailsPageType defaultPageConfig = super.loadDetailsPageConfiguration(resource);
//
//        Optional<ContainerPanelConfigurationType> schemaHandlingConfig = defaultPageConfig.getPanel().stream().filter(p -> "schemaHandling".equals(p.getPanelType())).findFirst();
//        if (!schemaHandlingConfig.isPresent()) {
//            return defaultPageConfig;
//        }
//
//
//    }

    public ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(getObjectWrapperModel().getObject().getObjectOld().asObjectable());
    }

    @Override
    public void reset() {
        super.reset();
        configurationModel.reset();
        objectClassesModel.reset();
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        PrismObject<ResourceType> resource = getPrismObject();
        PrismReference connector = resource.findReference(ResourceType.F_CONNECTOR_REF);
        String connectorOid = connector != null ? connector.getOid() : null;
        return getModelServiceLocator().getCompiledGuiProfile().findResourceDetailsConfiguration(connectorOid);
    }

    public void setConnectorConfigurationSuggestions(DiscoveredConfiguration connectorConfigurationSuggestions) {
        this.connectorConfigurationSuggestions = connectorConfigurationSuggestions;
    }

    public IModel<List<ObjectClassWrapper>> getObjectClassesModel() {
        return objectClassesModel;
    }

    @Override
    public Collection<ObjectDelta<? extends ObjectType>> collectDeltas(OperationResult result) throws SchemaException {
        return super.collectDeltas(result);
    }
}

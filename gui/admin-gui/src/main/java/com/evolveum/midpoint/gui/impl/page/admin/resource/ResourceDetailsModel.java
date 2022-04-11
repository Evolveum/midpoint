/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ConnectorTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.LoadableDetachableModel;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    private static final String DOT_CLASS = ResourceDetailsModel.class.getName() + ".";
    private static final String OPERATION_CREATE_CONFIGURATION_WRAPPERS = DOT_CLASS + "loadConnectorWrapper";

    private final LoadableModel<PrismContainerWrapper<ConnectorConfigurationType>> configurationModel;

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
    }

    private PrismContainerWrapper<ConnectorConfigurationType> createConfigContainerWrappers(OperationResult result) throws SchemaException {

        Task task = getModelServiceLocator().createSimpleTask(OPERATION_CREATE_CONFIGURATION_WRAPPERS);
        PrismObjectWrapper<ResourceType> resourceWrapper = getObjectWrapper();

        PrismContainerWrapper<ConnectorConfigurationType> configuration = resourceWrapper.findContainer(ResourceType.F_CONNECTOR_CONFIGURATION);
        PrismContainer<ConnectorConfigurationType> connectorConfigurationType = null;

        ItemStatus configurationStatus = ItemStatus.NOT_CHANGED;
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
                schema = ConnectorTypeUtil.parseConnectorSchema(connectorType, getPrismContext());
            } catch (SchemaException e) {
                throw new SystemException("Couldn't parse connector schema: " + e.getMessage(), e);
            }
            PrismContainerDefinition<ConnectorConfigurationType> definition = ConnectorTypeUtil.findConfigurationContainerDefinition(connectorType, schema);
            // Fixing (errorneously) set maxOccurs = unbounded. See MID-2317 and related issues.
            PrismContainerDefinition<ConnectorConfigurationType> definitionFixed = definition.clone();
            definitionFixed.toMutable().setMaxOccurs(1);
            connectorConfigurationType = definitionFixed.instantiate();
            configurationStatus = ItemStatus.ADDED;

            WrapperContext ctx = new WrapperContext(task, result);
            ctx.setShowEmpty(ItemStatus.ADDED == configurationStatus);
            configuration = getModelServiceLocator().createItemWrapper(connectorConfigurationType, configurationStatus, ctx);
        }

        return configuration;
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

    public ResourceSchema getRefinedSchema() throws SchemaException {
        return ResourceSchemaFactory.getCompleteSchema(getObjectWrapperModel().getObject().getObjectOld().asObjectable());
    }


}

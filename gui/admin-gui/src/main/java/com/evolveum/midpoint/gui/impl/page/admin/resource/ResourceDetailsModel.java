/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ResourceDetailsModel extends AssignmentHolderDetailsModel<ResourceType> {

    private static final String DOT_CLASS = ResourceDetailsModel.class.getName() + ".";
    private static final String OPERATION_FETCH_SCHEMA = DOT_CLASS + "fetchSchema";

    private final LoadableModel<List<ObjectClassWrapper>> objectClassesModel;

    public ResourceDetailsModel(LoadableDetachableModel<PrismObject<ResourceType>> prismObjectModel, ModelServiceLocator serviceLocator) {
        super(prismObjectModel, serviceLocator);

        this.objectClassesModel = new LoadableModel<>() {
            @Override
            protected List<ObjectClassWrapper> load() {
                List<ObjectClassWrapper> list = new ArrayList<>();

                ResourceSchema schema = getResourceSchema(getObjectWrapper(), getPageBase());

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

    public static ResourceSchema getResourceSchema(PrismObjectWrapper<ResourceType> objectWrapper, PageAdminLTE pageBase) {
        ResourceSchema schema = null;
        OperationResult result = new OperationResult(OPERATION_FETCH_SCHEMA);
        try {
            schema = pageBase.getModelService().fetchSchema(objectWrapper.getObjectApplyDelta(), result);
        } catch (SchemaException | RuntimeException e) {
            result.recordFatalError("Cannot load schema object classes, " + e.getMessage());
            result.computeStatus();
            pageBase.showResult(result);
        }
        return schema;
    }

    public ResourceSchema getRefinedSchema() throws SchemaException, ConfigurationException {
        return ResourceSchemaFactory.getCompleteSchema(getObjectWrapperModel().getObject().getObjectOld().asObjectable());
    }

    @Override
    public void reset() {
        super.reset();
        objectClassesModel.reset();
    }

    @Override
    protected GuiObjectDetailsPageType loadDetailsPageConfiguration() {
        PrismObject<ResourceType> resource = getPrismObject();
        PrismReference connector = resource.findReference(ResourceType.F_CONNECTOR_REF);
        String connectorOid = connector != null ? connector.getOid() : null;
        return getModelServiceLocator().getCompiledGuiProfile().findResourceDetailsConfiguration(connectorOid);
    }

    public IModel<List<ObjectClassWrapper>> getObjectClassesModel() {
        return objectClassesModel;
    }

    public PageResource getPageResource() {
        return (PageResource) super.getPageBase();
    }

//    @Override
//    protected WrapperContext createWrapperContext(Task task, OperationResult result) {
//        WrapperContext ctx = new WrapperContext(task, result) {
//            @Override
//            protected void collectVirtualContainers(@NotNull Collection<? extends ContainerPanelConfigurationType> panelConfigs, Collection<VirtualContainersSpecificationType> virtualContainers) {
//                if (!(getModelServiceLocator() instanceof PageResource)) {
//                    super.collectVirtualContainers(panelConfigs, virtualContainers);
//                    return;
//                }
//                PageResource pageResource = (PageResource) getModelServiceLocator();
//                for (ContainerPanelConfigurationType panelConfig : panelConfigs) {
//                    if (getObjectStatus() == null || panelConfig.getApplicableForOperation() == null
//                            || (pageResource.isEditObject()
//                            && OperationTypeType.MODIFY.equals(panelConfig.getApplicableForOperation()))
//                            || (!pageResource.isEditObject()
//                            && OperationTypeType.ADD.equals(panelConfig.getApplicableForOperation()))
//                            || (DelineationResourceObjectTypeStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || BasicSettingResourceObjectTypeStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || FocusResourceObjectTypeStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || ReactionMainSettingStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || ReactionOptionalSettingStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || AttributeInboundStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || AttributeOutboundStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || MainConfigurationStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || PasswordStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || AssociationStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || AdministrativeStatusStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || ExistenceStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || ValidFromStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || ValidToStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || LockoutStatusStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier())
//                            || LimitationsStepPanel.PANEL_TYPE.equals(panelConfig.getIdentifier()))) { // UGLY HACK we need define visibility of panel in menu
//                        virtualContainers.addAll(panelConfig.getContainer());
//                        collectVirtualContainers(panelConfig.getPanel(), virtualContainers);
//                    }
//                }
//            }
//        };
//        ctx.setCreateIfEmpty(true);
//        ctx.setDetailsPageTypeConfiguration(getPanelConfigurations());
//        ctx.setConfigureMappingType(true);
//        if (getModelServiceLocator() instanceof PageResource) {
//            ctx.setShowEmpty(!((PageResource)getModelServiceLocator()).isEditObject());
//        }
//        return ctx;
//    }
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeSynchronizationPolicy;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.task.PageTask;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.work.ResourceObjectSetUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.box.BasicInfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class ResourceDetailsTabPanel extends BasePanel<PrismObject<ResourceType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceDetailsTabPanel.class);

    private static final String DOT_CLASS = ResourceDetailsTabPanel.class.getName() + ".";
    private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "searchTasks";

    public static final String ID_LAST_AVAILABILITY_STATUS = "lastStatus";
    private static final String ID_SOURCE_TARGET = "sourceTarget";
    private static final String ID_SCHEMA_STATUS = "schemaStatus";

    private static final String PANEL_CAPABILITIES = "capabilities";

    private static final long serialVersionUID = 1L;

    LoadableModel<CapabilitiesDto> capabilitiesModel;

    public ResourceDetailsTabPanel(String id, final LoadableModel<PrismObject<ResourceType>> model) {
        super(id, model);

        capabilitiesModel = new LoadableModel<CapabilitiesDto>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected CapabilitiesDto load() {
                PrismObject<ResourceType> resource = model.getObject();
                return new CapabilitiesDto(resource.asObjectable());
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        PrismObject<ResourceType> resourceObject = getModelObject();
        ResourceType resource = resourceObject.asObjectable();

        add(createLastAvailabilityStatusInfo(resource));

        add(createSourceTargetInfo(resource));

        add(createSchemaStatusInfo(resource));

        CapabilitiesPanel capabilities = new CapabilitiesPanel(PANEL_CAPABILITIES, capabilitiesModel);
        add(capabilities);

        List<ResourceConfigurationDto> resourceConfigList = createResourceConfigList(resource);

        ListDataProvider<ResourceConfigurationDto> resourceConfigProvider = new ListDataProvider<>(
                ResourceDetailsTabPanel.this, new ListModel<>(resourceConfigList));

        List<IColumn<SelectableBeanImpl<ResourceType>, String>> tableColumns = new ArrayList<>();
        tableColumns.add(ColumnUtils.createPropertyColumn(
                new ColumnTypeDto<>(
                        "ShadowType.kind", "objectTypeDefinition.kind", ShadowType.F_KIND.getLocalPart())));
        tableColumns.add(new PropertyColumn<>(createStringResource("ShadowType.objectClass"),
                "objectTypeDefinition.objectClass") {

            @Override
            public IModel<?> getDataModel(IModel<SelectableBeanImpl<ResourceType>> rowModel) {
                IModel<QName> model = (IModel<QName>) super.getDataModel(rowModel);
                if (model.getObject() != null) {
                    return () -> model.getObject().getLocalPart();
                }
                return model;
            }
        });

        List<ColumnTypeDto<String>> columns = Arrays.asList(
                new ColumnTypeDto<>("ShadowType.intent", "objectTypeDefinition.intent",
                        ShadowType.F_INTENT.getLocalPart()),
                new ColumnTypeDto<>("ResourceType.isSync", "sync", null));

        tableColumns.addAll(ColumnUtils.createColumns(columns));

        PropertyColumn tasksColumn = new PropertyColumn(
                PageBase.createStringResourceStatic("ResourceType.tasks"), "definedTasks") {

            @Override
            public void populateItem(Item item, String componentId, final IModel rowModel) {
                ResourceConfigurationDto conf = (ResourceConfigurationDto) rowModel.getObject();
                RepeatingView repeater = new RepeatingView(componentId);
                for (final TaskType task : conf.getDefinedTasks()) {
                    repeater.add(new AjaxLinkPanel(repeater.newChildId(),
                            new Model<>(task.getName().getOrig())) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            ResourceDetailsTabPanel.this.taskDetailsPerformed(target, task.getOid());
                        }

                    });
                }

                item.add(repeater);
            }

        };

        tableColumns.add(tasksColumn);

        BoxedTablePanel<ResourceConfigurationDto> resourceConfig = new BoxedTablePanel("resourceConfig",
                resourceConfigProvider, tableColumns);
        resourceConfig.setAdditionalBoxCssClasses("box-success");
        add(resourceConfig);

    }

    private List<ResourceConfigurationDto> createResourceConfigList(ResourceType resource) {
        OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);

        List<PrismObject<TaskType>> tasks = WebModelServiceUtils.searchObjects(TaskType.class,
                getPageBase().getPrismContext().queryFor(TaskType.class)
                        .item(TaskType.F_OBJECT_REF).ref(resource.getOid())
                        .and()
                        .item(TaskType.F_PARENT).isNull()
                        .build(),
                result, getPageBase());

        List<ResourceConfigurationDto> configs = new ArrayList<>();

        if (resource.getSchemaHandling() == null) {
            return configs;
        }

        List<ResourceObjectTypeDefinitionType> objectTypes = resource.getSchemaHandling().getObjectType();

        if (objectTypes == null) {
            return configs;
        }

        try {
            for (ResourceObjectTypeDefinitionType objectTypeDefBean : objectTypes) {
                ResourceObjectTypeSynchronizationPolicy syncPolicy =
                        ResourceObjectTypeSynchronizationPolicy.forDefinitionBean(objectTypeDefBean, resource);
                List<TaskType> syncTasks;
                if (syncPolicy != null) {
                    syncTasks = getTasksFor(tasks, syncPolicy, resource.asPrismObject());
                } else {
                    syncTasks = new ArrayList<>();
                }

                ResourceConfigurationDto resourceConfig =
                        new ResourceConfigurationDto(objectTypeDefBean, syncPolicy != null, syncTasks);
                configs.add(resourceConfig);
            }
        } catch (SchemaException | ConfigurationException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not determine resource configuration", ex);
        }

        return configs;
    }

    private void taskDetailsPerformed(AjaxRequestTarget target, String taskOid) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
        ((PageBase) getPage()).navigateToNext(PageTask.class, parameters);
    }

    private BasicInfoBoxPanel createSourceTargetInfo(ResourceType resource) {

        String backgroundColor = "bg-aqua";
        SourceTarget sourceTarget = determineIfSourceOrTarget(resource);

        String numberKey;
        switch (sourceTarget) {
            case SOURCE:
                numberKey = "PageResource.resource.source";
                break;
            case TARGET:
                numberKey = "PageResource.resource.target";
                break;
            case SOURCE_TARGET:
                numberKey = "PageResource.resource.sourceAndTarget";
                break;

            default:
                backgroundColor = "bg-gray";
                numberKey = "PageResource.resource.noMappings";
                break;
        }

        InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, sourceTarget.getCssClass(),
                getPageBase().getString("PageResource.resource.mappings"));
        infoBoxType.setNumber(getPageBase().getString(numberKey));

        if (ResourceTypeUtil.isSynchronizationDefined(resource)) {
            infoBoxType.setDescription(getPageBase().getString("PageResource.resource.sync"));
        }

        Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

        return new BasicInfoBoxPanel(ID_SOURCE_TARGET, boxModel);

    }

    private InfoBoxPanel createLastAvailabilityStatusInfo(ResourceType resource) {

        String messageKey = "PageResource.resource.availabilityUnknown";
        String backgroundColor = "bg-gray";
        String icon = "fa fa-question";

        OperationalStateType operationalState = resource.getOperationalState();
        AdministrativeOperationalStateType administrativeOperationalState = resource.getAdministrativeOperationalState();
        boolean inMaintenance = false;

        if (administrativeOperationalState != null) {
            AdministrativeAvailabilityStatusType administrativeAvailabilityStatus = administrativeOperationalState.getAdministrativeAvailabilityStatus();
            if (administrativeAvailabilityStatus == AdministrativeAvailabilityStatusType.MAINTENANCE) {
                messageKey = "PageResource.resource.maintenance";
                backgroundColor = "bg-gray";
                icon = "fa fa-wrench";
                inMaintenance = true;
            }
        }
        if (operationalState != null && !inMaintenance) {
            AvailabilityStatusType lastAvailabilityStatus = operationalState.getLastAvailabilityStatus();
            if (lastAvailabilityStatus != null) {
                if (lastAvailabilityStatus == AvailabilityStatusType.UP) {
                    messageKey = "PageResource.resource.up";
                    backgroundColor = "bg-green";
                    icon = "fa fa-power-off";
                } else if (lastAvailabilityStatus == AvailabilityStatusType.DOWN) {
                    backgroundColor = "bg-red";
                    messageKey = "PageResource.resource.down";
                    icon = "fa fa-ban";
                } else if (lastAvailabilityStatus == AvailabilityStatusType.BROKEN) {
                    backgroundColor = "bg-yellow";
                    messageKey = "PageResource.resource.broken";
                    icon = "fa fa-warning";
                }
            }
        }

        InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, icon, getPageBase().getString(messageKey));

        ConnectorType connectorType = getConnectorType(resource);
        if (connectorType == null) {
            // Connector not found. Probably bad connectorRef reference.
            infoBoxType.setNumber("--");
            infoBoxType.setDescription("--");
        } else {
            String connectorName = StringUtils.substringAfterLast(
                    WebComponentUtil.getEffectiveName(connectorType, ConnectorType.F_CONNECTOR_TYPE), ".");
            String connectorVersion = connectorType.getConnectorVersion();
            infoBoxType.setNumber(connectorName);
            infoBoxType.setDescription(connectorVersion);
        }

        Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

        InfoBoxPanel lastAvailabilityStatus = new BasicInfoBoxPanel(ID_LAST_AVAILABILITY_STATUS, boxModel);
        lastAvailabilityStatus.setOutputMarkupId(true);

        return lastAvailabilityStatus;

    }

    private ConnectorType getConnectorType(ResourceType resource) {
        if (resource == null) {
            return null;
        }

        ObjectReferenceType connectorRef = resource.getConnectorRef();
        if (connectorRef == null) {
            return null;
        }

        PrismObject<ConnectorType> object = connectorRef.asReferenceValue().getObject();
        if (object == null) {
            return null;
        }

        return object.asObjectable();
    }

    private InfoBoxPanel createSchemaStatusInfo(ResourceType resource) {

        String backgroundColor = "bg-gray";
        String icon = "fa fa-times";
        String numberMessage;
        String description = null;

        Integer progress = null;
        ResourceSchema refinedSchema;
        try {
            refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
            if (refinedSchema != null) {
                backgroundColor = "bg-purple";
                icon = "fa fa-cubes";
                // TODO is this correct?
                int numObjectTypes = refinedSchema.getObjectTypeDefinitions().size();
                int numAllDefinitions = refinedSchema.getObjectClassDefinitions().size() + numObjectTypes;
                numberMessage = numObjectTypes + " " + getPageBase().getString("PageResource.resource.objectTypes");
                if (numAllDefinitions != 0) {
                    progress = numObjectTypes * 100 / numAllDefinitions;
                    if (progress > 100) {
                        progress = 100;
                    }
                }
                description = numAllDefinitions + " " + getPageBase().getString("PageResource.resource.schemaDefinitions");
            } else {
                numberMessage = getPageBase().getString("PageResource.resource.noSchema");
            }
        } catch (SchemaException e) {
            backgroundColor = "bg-danger";
            icon = "fa fa-warning";
            numberMessage = getPageBase().getString("PageResource.resource.schemaError");
        }

        InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, icon,
                getPageBase().getString("PageResource.resource.schema"));
        infoBoxType.setNumber(numberMessage);
        infoBoxType.setProgress(progress);
        infoBoxType.setDescription(description);

        Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

        return new BasicInfoBoxPanel(ID_SCHEMA_STATUS, boxModel);

    }

    private List<TaskType> getTasksFor(List<PrismObject<TaskType>> tasks,
            ResourceObjectTypeSynchronizationPolicy synchronizationPolicy, PrismObject<ResourceType> resource)
            throws SchemaException {
        List<TaskType> syncTasks = new ArrayList<>();
        for (PrismObject<TaskType> task : tasks) {
            ShadowKindType taskKindValue = null;
            String taskIntentValue = null;
            QName taskObjectClassValue = null;

            @Nullable ResourceObjectSetType resourceSet = ResourceObjectSetUtil.fromTask(task.asObjectable());
            if (!Objects.isNull(resourceSet)) {
                taskKindValue = resourceSet.getKind();
                taskIntentValue = resourceSet.getIntent();
                taskObjectClassValue = resourceSet.getObjectclass();
            }

            // TODO: unify with determineObjectClass in Utils (model-impl, which
            //  is not accessible in admin-gui)
            if (taskObjectClassValue == null) {
                ResourceObjectDefinition taskObjectClassDef = null;
                ResourceSchema schema = ResourceSchemaFactory.getCompleteSchema(resource);
                if (schema == null) {
                    throw new SchemaException(
                            "No schema defined in resource. Possible configuration problem?");
                }
                if (taskKindValue == null && taskIntentValue == null) {
                    taskObjectClassDef = schema.findObjectDefinition(ShadowKindType.ACCOUNT, null); // TODO ok?
                }

                if (taskKindValue != null) {
                    if (StringUtils.isEmpty(taskIntentValue)) {
                        taskObjectClassDef = schema.findObjectDefinition(taskKindValue, null); // TODO ok?
                    } else {
                        taskObjectClassDef = schema.findObjectDefinition(taskKindValue, taskIntentValue);
                    }

                }
                if (taskObjectClassDef != null) {
                    taskObjectClassValue = taskObjectClassDef.getTypeName();
                }
            }

            if (synchronizationPolicy.isApplicableTo(taskObjectClassValue, taskKindValue, taskIntentValue, true)) {
                syncTasks.add(task.asObjectable());
            }
        }

        return syncTasks;
    }

    // TODO: ####### start of move to ResourceTypeUtil ###########

    private SourceTarget determineIfSourceOrTarget(ResourceType resource) {

        if (resource.getSchemaHandling() != null
                && CollectionUtils.isNotEmpty(resource.getSchemaHandling().getObjectType())) {

            boolean hasOutbound = false;
            boolean hasInbound = false;

            for (ResourceObjectTypeDefinitionType resourceObjectTypeDefinition : resource.getSchemaHandling()
                    .getObjectType()) {
                if (CollectionUtils.isEmpty(resourceObjectTypeDefinition.getAttribute())) {
                    continue;
                }

                if (hasInbound && hasOutbound) {
                    return SourceTarget.SOURCE_TARGET;
                }

                for (ResourceAttributeDefinitionType attr : resourceObjectTypeDefinition.getAttribute()) {

                    if (hasInbound && hasOutbound) {
                        return SourceTarget.SOURCE_TARGET;
                    }

                    if (!hasOutbound) {
                        hasOutbound = ResourceTypeUtil.isOutboundDefined(attr);
                    }

                    if (!hasInbound) {
                        hasInbound = ResourceTypeUtil.isInboundDefined(attr);
                    }
                }

                // TODO: what about situation that we have only
            }

            if (hasOutbound) {
                return SourceTarget.TARGET;
            }

            if (hasInbound) {
                return SourceTarget.SOURCE;
            }

        }

        return SourceTarget.NOT_DEFINED;
    }

    // TODO: ####### end of move to ResourceTypeUtil ###########

    private enum SourceTarget {

        NOT_DEFINED("fa fa-square-o"), SOURCE("fa fa-sign-in"), TARGET("fa fa-sign-out"), SOURCE_TARGET("fa fa-exchange");

        private final String cssClass;

        SourceTarget(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

}

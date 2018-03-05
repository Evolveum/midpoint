/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.SynchronizationUtils;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceDetailsTabPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(ResourceDetailsTabPanel.class);

	private static final String DOT_CLASS = ResourceDetailsTabPanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";

	public static final String ID_LAST_AVAILABILITY_STATUS = "lastStatus";
	private static final String ID_SOURCE_TARGET = "sourceTarget";
	private static final String ID_SCHEMA_STATUS = "schemaStatus";

	private static final String PANEL_CAPABILITIES = "capabilities";

	private static final long serialVersionUID = 1L;

	LoadableModel<CapabilitiesDto> capabilitiesModel;

	private PageBase parentPage;

	public ResourceDetailsTabPanel(String id, final IModel<?> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;

		capabilitiesModel = new LoadableModel<CapabilitiesDto>() {
			private static final long serialVersionUID = 1L;

			@Override
			protected CapabilitiesDto load() {
				PrismObject<ResourceType> resource = (PrismObject<ResourceType>) model.getObject();
				return new CapabilitiesDto(resource.asObjectable());
			}
		};

		initLayout(model, parentPage);
	}

	protected void initLayout(IModel model, PageBase parentPage) {

		PrismObject<ResourceType> resourceObject = (PrismObject<ResourceType>) model.getObject();
		ResourceType resource = resourceObject.asObjectable();

		add(createLastAvailabilityStatusInfo(resource));

		add(createSourceTargetInfo(resource));

		add(createSchemaStatusInfo(resource));

		CapabilitiesPanel capabilities = new CapabilitiesPanel(PANEL_CAPABILITIES, capabilitiesModel);
		add(capabilities);

		List<ResourceConfigurationDto> resourceConfigList = createResourceConfigList(resource);

		ListDataProvider<ResourceConfigurationDto> resourceConfigProvider = new ListDataProvider<>(
            ResourceDetailsTabPanel.this, new ListModel<>(resourceConfigList));

		List<ColumnTypeDto<String>> columns = Arrays.asList(
				new ColumnTypeDto<String>("ShadowType.kind", "objectTypeDefinition.kind",
						ShadowType.F_KIND.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.objectClass",
						"objectTypeDefinition.objectClass.localPart",
						ShadowType.F_OBJECT_CLASS.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", "objectTypeDefinition.intent",
						ShadowType.F_INTENT.getLocalPart()),
				new ColumnTypeDto<String>("ResourceType.isSync", "sync", null));

		List<IColumn<SelectableBean<ResourceType>, String>> tableColumns = ColumnUtils.createColumns(columns);

		PropertyColumn tasksColumn = new PropertyColumn(
				PageBase.createStringResourceStatic(this, "ResourceType.tasks"), "definedTasks") {

			@Override
			public void populateItem(Item item, String componentId, final IModel rowModel) {
				ResourceConfigurationDto conf = (ResourceConfigurationDto) rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);
				for (final TaskType task : conf.getDefinedTasks()) {
					repeater.add(new LinkPanel(repeater.newChildId(),
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
				QueryBuilder.queryFor(TaskType.class, parentPage.getPrismContext())
						.item(TaskType.F_OBJECT_REF).ref(resource.getOid())
						.build(),
				result, parentPage);

		List<ResourceConfigurationDto> configs = new ArrayList<>();

		if (resource.getSchemaHandling() == null) {
			return configs;
		}

		List<ResourceObjectTypeDefinitionType> objectTypes = resource.getSchemaHandling().getObjectType();

		if (objectTypes == null) {
			return configs;
		}

		try {
			for (ResourceObjectTypeDefinitionType objectType : objectTypes) {
				ObjectSynchronizationType obejctSynchronization = null;
				if (resource.getSynchronization() != null
						&& resource.getSynchronization().getObjectSynchronization() != null) {

					obejctSynchronization = getSynchronizationFor(objectType,
							resource.getSynchronization().getObjectSynchronization(),
							resource.asPrismObject());

				}
				List<TaskType> syncTask = new ArrayList<>();
				if (obejctSynchronization != null) {
					syncTask = getTaskFor(tasks, obejctSynchronization, resource.asPrismObject());
				}

				ResourceConfigurationDto resourceConfig = new ResourceConfigurationDto(objectType,
						obejctSynchronization != null, syncTask);
				configs.add(resourceConfig);
			}
		} catch (SchemaException ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Could not determine resource configuration", ex);
		}

		return configs;
	}

	private void taskDetailsPerformed(AjaxRequestTarget target, String taskOid) {
		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
		((PageBase) getPage()).navigateToNext(PageTaskEdit.class, parameters);
	}

	private InfoBoxPanel createSourceTargetInfo(ResourceType resource) {

		String backgroundColor = "bg-aqua";
		SourceTarget sourceTarget = determineIfSourceOrTarget(resource);

		String numberKey = null;
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
				parentPage.getString("PageResource.resource.mappings"));
		infoBoxType.setNumber(parentPage.getString(numberKey));

		if (isSynchronizationDefined(resource)) {
			infoBoxType.setDescription(parentPage.getString("PageResource.resource.sync"));
		}

		Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

		return new InfoBoxPanel(ID_SOURCE_TARGET, boxModel);

	}

	private InfoBoxPanel createLastAvailabilityStatusInfo(ResourceType resource) {

		String messageKey = "PageResource.resource.availabilityUnknown";
		String backgroundColor = "bg-gray";
		String icon = "fa-question";

		OperationalStateType operationalState = resource.getOperationalState();
		if (operationalState != null) {
			AvailabilityStatusType lastAvailabilityStatus = operationalState.getLastAvailabilityStatus();
			if (lastAvailabilityStatus != null) {
				if (lastAvailabilityStatus == AvailabilityStatusType.UP) {
					messageKey = "PageResource.resource.up";
					backgroundColor = "bg-green";
					icon = "fa-power-off";
				} else if (lastAvailabilityStatus == AvailabilityStatusType.DOWN) {
					backgroundColor = "bg-red";
					messageKey = "PageResource.resource.down";
					icon = "fa-ban";
				} else if (lastAvailabilityStatus == AvailabilityStatusType.BROKEN) {
					backgroundColor = "bg-yellow";
					messageKey = "PageResource.resource.broken";
					icon = "fa-warning";
				}
			}
		}

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, icon, parentPage.getString(messageKey));

		ConnectorType connectorType = resource.getConnector();
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

		InfoBoxPanel lastAvailabilityStatus = new InfoBoxPanel(ID_LAST_AVAILABILITY_STATUS, boxModel);
		lastAvailabilityStatus.setOutputMarkupId(true);

		return lastAvailabilityStatus;

	}

	private InfoBoxPanel createSchemaStatusInfo(ResourceType resource) {

		String backgroundColor = "bg-gray";
		String icon = "fa-times";
		String numberMessage = null;
		String description = null;

		Integer progress = null;
		RefinedResourceSchema refinedSchema = null;
		try {
			refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
			if (refinedSchema != null) {
				backgroundColor = "bg-purple";
				icon = "fa-cubes";
				int numObjectTypes = 0;
				List<? extends RefinedObjectClassDefinition> refinedDefinitions = refinedSchema
						.getRefinedDefinitions();
				for (RefinedObjectClassDefinition refinedDefinition : refinedDefinitions) {
					if (refinedDefinition.getKind() != null) {
						numObjectTypes++;
					}
				}
				int numAllDefinitions = refinedDefinitions.size();
				numberMessage = numObjectTypes + " " + parentPage.getString("PageResource.resource.objectTypes");
				if (numAllDefinitions != 0) {
					progress = numObjectTypes * 100 / numAllDefinitions;
					if (progress > 100) {
						progress = 100;
					}
				}
				description = numAllDefinitions + " " + parentPage.getString("PageResource.resource.schemaDefinitions");
			} else {
				numberMessage = parentPage.getString("PageResource.resource.noSchema");
			}
		} catch (SchemaException e) {
			backgroundColor = "bg-danger";
			icon = "fa-warning";
			numberMessage = parentPage.getString("PageResource.resource.schemaError");
		}

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, icon,
				parentPage.getString("PageResource.resource.schema"));
		infoBoxType.setNumber(numberMessage);
		infoBoxType.setProgress(progress);
		infoBoxType.setDescription(description);

		Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

		return new InfoBoxPanel(ID_SCHEMA_STATUS, boxModel);

	}

	private ObjectSynchronizationType getSynchronizationFor(
			ResourceObjectTypeDefinitionType obejctTypesDefinition,
			List<ObjectSynchronizationType> synchronizationPolicies, PrismObject<ResourceType> resource)
					throws SchemaException {

		for (ObjectSynchronizationType synchronizationPolicy : synchronizationPolicies) {
			if (SynchronizationUtils.isPolicyApplicable(obejctTypesDefinition.getObjectClass(),
					obejctTypesDefinition.getKind(), obejctTypesDefinition.getIntent(), synchronizationPolicy,
					resource)) {
				if (synchronizationPolicy.getObjectClass().isEmpty()) {
					synchronizationPolicy.getObjectClass().add(obejctTypesDefinition.getObjectClass());
				}
				return synchronizationPolicy;
			}
		}

		return null;
	}

	private List<TaskType> getTaskFor(List<PrismObject<TaskType>> tasks,
			ObjectSynchronizationType synchronizationPolicy, PrismObject<ResourceType> resource)
					throws SchemaException {
		List<TaskType> syncTasks = new ArrayList<>();
		for (PrismObject<TaskType> task : tasks) {
			PrismProperty<ShadowKindType> taskKind = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			ShadowKindType taskKindValue = null;
			if (taskKind != null) {
				taskKindValue = taskKind.getRealValue();
			}

			PrismProperty<String> taskIntent = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			String taskIntentValue = null;
			if (taskIntent != null) {
				taskIntentValue = taskIntent.getRealValue();
			}

			PrismProperty<QName> taskObjectClass = task.findProperty(
					new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME));
			QName taskObjectClassValue = null;
			if (taskObjectClass != null) {
				taskObjectClassValue = taskObjectClass.getRealValue();
			}

			// TODO: unify with determineObjectClass in Utils (model-impl, which
			// is not accessible in admin-gui)
			if (taskObjectClassValue == null) {
				ObjectClassComplexTypeDefinition taskObjectClassDef = null;
				RefinedResourceSchema schema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
				if (schema == null) {
					throw new SchemaException(
							"No schema defined in resource. Possible configuration problem?");
				}
				if (taskKindValue == null && taskIntentValue == null) {
					taskObjectClassDef = schema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
				}

				if (taskKindValue != null) {
					if (StringUtils.isEmpty(taskIntentValue)) {
						taskObjectClassDef = schema.findDefaultObjectClassDefinition(taskKindValue);
					} else {
						taskObjectClassDef = schema.findObjectClassDefinition(taskKindValue, taskIntentValue);
					}

				}
				if (taskObjectClassDef != null) {
					taskObjectClassValue = taskObjectClassDef.getTypeName();
				}
			}

			if (SynchronizationUtils.isPolicyApplicable(taskObjectClassValue, taskKindValue, taskIntentValue,
					synchronizationPolicy, resource)) {
				syncTasks.add(task.asObjectable());
			}
		}

		return syncTasks;
	}

	// TODO: ####### start of move to ResourceTypeUtil ###########

	private boolean isOutboundDefined(ResourceAttributeDefinitionType attr) {
		return attr.getOutbound() != null
				&& (attr.getOutbound().getSource() != null || attr.getOutbound().getExpression() != null);
	}

	private boolean isInboundDefined(ResourceAttributeDefinitionType attr) {
		return attr.getInbound() != null && CollectionUtils.isNotEmpty(attr.getInbound())
				&& (attr.getInbound().get(0).getTarget() != null
						|| attr.getInbound().get(0).getExpression() != null);
	}

	private boolean isSynchronizationDefined(ResourceType resource) {
		if (resource.getSynchronization() == null) {
			return false;
		}

		if (resource.getSynchronization().getObjectSynchronization().isEmpty()) {
			return false;
		}

		for (ObjectSynchronizationType syncType : resource.getSynchronization().getObjectSynchronization()) {
			if (syncType.isEnabled() != null && !syncType.isEnabled()) {
				continue;
			}

			if (CollectionUtils.isEmpty(syncType.getReaction())) {
				continue;
			}

			return true;

		}

		return false;

	}

	private SourceTarget determineCredentialsMappings(ResourceType resource) {
		if (resource.getSchemaHandling() != null
				&& CollectionUtils.isNotEmpty(resource.getSchemaHandling().getObjectType())) {

			boolean hasOutbound = false;
			boolean hasInbound = false;

			for (ResourceObjectTypeDefinitionType resourceObjectTypeDefinition : resource.getSchemaHandling()
					.getObjectType()) {

				if (hasInbound && hasOutbound) {
					return SourceTarget.SOURCE_TARGET;
				}

				if (resourceObjectTypeDefinition.getCredentials() == null) {
					continue;
				}

				if (resourceObjectTypeDefinition.getCredentials().getPassword() == null) {
					continue;
				}

				ResourcePasswordDefinitionType passwordDef = resourceObjectTypeDefinition.getCredentials()
						.getPassword();
				if (!hasOutbound) {
					hasOutbound = passwordDef.getOutbound() != null;
				}

				if (!hasInbound) {
					hasInbound = CollectionUtils.isNotEmpty(passwordDef.getInbound());
				}
			}

			if (hasInbound) {
				return SourceTarget.SOURCE;
			}

			if (hasOutbound) {
				return SourceTarget.TARGET;
			}

		}

		return SourceTarget.NOT_DEFINED;
	}

	private SourceTarget determineActivationMappings(ResourceType resource) {
		if (resource.getSchemaHandling() != null
				&& CollectionUtils.isNotEmpty(resource.getSchemaHandling().getObjectType())) {

			boolean hasOutbound = false;
			boolean hasInbound = false;

			for (ResourceObjectTypeDefinitionType resourceObjectTypeDefinition : resource.getSchemaHandling()
					.getObjectType()) {

				if (hasInbound && hasOutbound) {
					return SourceTarget.SOURCE_TARGET;
				}

				if (resourceObjectTypeDefinition.getActivation() == null) {
					continue;
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getAdministrativeStatus() != null && CollectionUtils
							.isNotEmpty(activationDef.getAdministrativeStatus().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getValidFrom() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidFrom().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getValidTo() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidTo().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getExistence() != null
							&& CollectionUtils.isNotEmpty(activationDef.getExistence().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getAdministrativeStatus() != null && CollectionUtils
							.isNotEmpty(activationDef.getAdministrativeStatus().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getValidFrom() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidFrom().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getValidTo() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidTo().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition
							.getActivation();
					if (activationDef.getExistence() != null
							&& CollectionUtils.isNotEmpty(activationDef.getExistence().getInbound())) {
						hasInbound = true;
					}
				}
			}

			if (hasInbound) {
				return SourceTarget.SOURCE;
			}

			if (hasOutbound) {
				return SourceTarget.TARGET;
			}

		}

		return SourceTarget.NOT_DEFINED;
	}

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
						hasOutbound = isOutboundDefined(attr);
					}

					if (!hasInbound) {
						hasInbound = isInboundDefined(attr);
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

		NOT_DEFINED("fa-square-o"), SOURCE("fa-sign-in"), TARGET("fa-sign-out"), SOURCE_TARGET("fa-exchange");

		private String cssClass;

		SourceTarget(String cssClass) {
			this.cssClass = cssClass;
		}

		public String getCssClass() {
			return cssClass;
		}
	}

}

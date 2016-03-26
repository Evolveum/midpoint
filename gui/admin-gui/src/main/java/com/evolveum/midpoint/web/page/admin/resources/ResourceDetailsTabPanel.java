/*
 * Copyright (c) 2010-2016 Evolveum
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Page;
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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceDetailsTabPanel extends Panel{
	
	private static final String DOT_CLASS = ResourceDetailsTabPanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";
	
	private static final String FIELD_SOURCE_TARGET = "sourceTarget";
	private static final String FIELD_CREDENTIALS_MAPPING = "credentialsMapping";
	private static final String FIELD_ACTIVATION_MAPPING = "activationMapping";
	public static final String FIELD_LAST_AVAILABILITY_STATUS = "lastStatus";
	
	
	
	private static final String PANEL_CAPABILITIES = "capabilities";
	
	private static final long serialVersionUID = 1L;
	
	LoadableModel<CapabilitiesDto> capabilitiesModel;
	
	private PageBase parentPage;
	
	public ResourceDetailsTabPanel(String id, final IModel<?> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;
		
		capabilitiesModel = new LoadableModel<CapabilitiesDto>() {
			@Override
			protected CapabilitiesDto load() {
				PrismObject<ResourceType> resource = (PrismObject<ResourceType>) model.getObject();
				return new CapabilitiesDto(resource.asObjectable());
			}
		};
		
		initLayout(model, parentPage);
	}
	
	protected void initLayout(IModel model, PageBase parentPage){
		
		
		PrismObject<ResourceType> resourceObject = (PrismObject<ResourceType>) model.getObject();
		ResourceType resource = resourceObject.asObjectable();

		add(addLastAvailabilityStatusInfo(resource));

		add(addSourceTargetInfo(resource));

		add(addCapabilityMappingInfo(FIELD_CREDENTIALS_MAPPING, determineCredentialsMappings(resource),
				"PageResource.resource.mapping.credentials"));
		add(addCapabilityMappingInfo(FIELD_ACTIVATION_MAPPING, determineActivationMappings(resource),
				"PageResource.resource.mapping.activation"));

		CapabilitiesPanel capabilities = new CapabilitiesPanel(PANEL_CAPABILITIES, capabilitiesModel);
		add(capabilities);

		List<ResourceConfigurationDto> resourceConfigList = createResourceConfigList(resource);

		ListDataProvider<ResourceConfigurationDto> resourceConfigProvider = new ListDataProvider<ResourceConfigurationDto>(
				ResourceDetailsTabPanel.this, new ListModel<ResourceConfigurationDto>(resourceConfigList));

		List<ColumnTypeDto> columns = Arrays.asList(
				new ColumnTypeDto("ShadowType.kind", "objectTypeDefinition.kind", ShadowType.F_KIND.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.objectClass", "objectTypeDefinition.objectClass.localPart",
						ShadowType.F_OBJECT_CLASS.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", "objectTypeDefinition.intent",
						ShadowType.F_INTENT.getLocalPart()),
				new ColumnTypeDto<Boolean>("ResourceType.isSync", "sync", null));

		List<IColumn> tableColumns = ColumnUtils.createColumns(columns);

		PropertyColumn tasksColumn = new PropertyColumn(
				PageBase.createStringResourceStatic(this, "ResourceType.tasks"), "definedTasks") {

			@Override
			public void populateItem(Item item, String componentId, final IModel rowModel) {
				ResourceConfigurationDto conf = (ResourceConfigurationDto) rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);
				for (final TaskType task : conf.getDefinedTasks()){
					repeater.add(new LinkPanel(repeater.newChildId(), new Model<String>(task.getName().getOrig())) {

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

		BoxedTablePanel<ResourceConfigurationDto> resourceConfig = new BoxedTablePanel(
				"resourceConfig", resourceConfigProvider, tableColumns);
		add(resourceConfig);
		
		

	}
	
	private List<ResourceConfigurationDto> createResourceConfigList(ResourceType resource) {
		OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);
		Task task = parentPage.createSimpleTask(OPERATION_SEARCH_TASKS_FOR_RESOURCE);
		List<PrismObject<TaskType>> tasks = WebModelServiceUtils.searchObjects(TaskType.class,
				ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class,
						parentPage.getPrismContext(), resource.getOid())),
				result, parentPage);

		List<ResourceConfigurationDto> configs = new ArrayList<>();
		
		if (resource.getSchemaHandling() == null){
			return configs;
		}
		
		List<ResourceObjectTypeDefinitionType> objectTypes = resource.getSchemaHandling().getObjectType();
		
		if (objectTypes == null){
			return configs;
		}
		
		for (ResourceObjectTypeDefinitionType objectType : objectTypes) {
			boolean sync = false;
			if (resource.getSynchronization() != null && resource.getSynchronization().getObjectSynchronization() != null){
			 sync =  isSynchronizationFor(objectType, resource.getSynchronization().getObjectSynchronization());

			}
			List<TaskType> syncTask = getTaskFor(tasks, objectType);

			ResourceConfigurationDto resourceConfig = new ResourceConfigurationDto(objectType, sync, syncTask);
			configs.add(resourceConfig);
		}

		return configs;
	}
	
	private void taskDetailsPerformed(AjaxRequestTarget target, String taskOid){
		PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
        setResponsePage(new PageTaskEdit(parameters, new PageResource()));
		
	}
	
	private InfoBoxPanel addCapabilityMappingInfo(String fieldId, SourceTarget sourceTarget, String messageKey) {
		String backgroundColor = "bg-green";

		List<String> description = new ArrayList<>();
		description.add(getString(messageKey));

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, sourceTarget.getCssClass(), description);
		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		return new InfoBoxPanel(fieldId, boxModel);
	}

	private InfoBoxPanel addSourceTargetInfo(ResourceType resource) {

		String backgroundColor = "bg-green";
		SourceTarget sourceTarget = determineIfSourceOrTarget(resource);
		List<String> description = new ArrayList<>();

		switch (sourceTarget) {
		case SOURCE:
			description.add(getString("PageResource.resource.source"));
			break;
		case TARGET:
			description.add(getString("PageResource.resource.target"));
			break;
		case SOURCE_TARGET:
			description.add(getString("PageResource.resource.source"));
			description.add(getString("PageResource.resource.target"));
			break;

		default:
			description.add("No");
			description.add("mappings");
			description.add("defined");
			break;
		}

		// TODO: credentials and activation mappings

		if (isSynchronizationDefined(resource)) {
			description.add(getString("PageResource.resource.sync"));
		}

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, sourceTarget.getCssClass(), description);
		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		return new InfoBoxPanel(FIELD_SOURCE_TARGET, boxModel);
		
	}

	private InfoBoxPanel addLastAvailabilityStatusInfo(ResourceType resource) {

		String backgroundColor = "bg-green";

		if (ResourceTypeUtil.isDown(resource)) {
			backgroundColor = "bg-red";
		}

		List<String> description = new ArrayList<>();
		
		ConnectorType connectorType = resource.getConnector();
		description.add(StringUtils
				.substringAfterLast(WebComponentUtil.getEffectiveName(connectorType, ConnectorType.F_CONNECTOR_TYPE), "."));
		
		description.add(connectorType.getConnectorVersion());
		description.add(connectorType.getConnectorBundle());

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, "fa-power-off", description);
		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		InfoBoxPanel lastAvailabilityStatus = new InfoBoxPanel(FIELD_LAST_AVAILABILITY_STATUS, boxModel);
		lastAvailabilityStatus.setOutputMarkupId(true);
		
		return lastAvailabilityStatus;
		
	}
	
	private boolean isSynchronizationFor(ResourceObjectTypeDefinitionType obejctTypesDefinition,
			List<ObjectSynchronizationType> synchronizationPolicies) {

		for (ObjectSynchronizationType synchronizationPolicy : synchronizationPolicies) {
			List<QName> policyObjectClasses = synchronizationPolicy.getObjectClass();
			if (policyObjectClasses == null || policyObjectClasses.isEmpty()) {
				return isSynchronizationFor(obejctTypesDefinition, null, synchronizationPolicy.getKind(),
						synchronizationPolicy.getIntent());
			}
			for (QName policyObjetClass : policyObjectClasses) {
				return isSynchronizationFor(obejctTypesDefinition, policyObjetClass, synchronizationPolicy.getKind(),
						synchronizationPolicy.getIntent());
			}
		}

		return false;
	}

	private boolean isSynchronizationFor(ResourceObjectTypeDefinitionType obejctTypesDefinition, QName objectClass,
			ShadowKindType kind, String intent) {
		if (obejctTypesDefinition.getObjectClass() != null) {
			if (objectClass != null) {
				if (!objectClass.equals(obejctTypesDefinition.getObjectClass())) {
					return false;
				}
			}
		}

		// kind
		ShadowKindType shadowKind = obejctTypesDefinition.getKind();
		if (kind != null && shadowKind != null && !kind.equals(shadowKind)) {
			return false;
		}

		// intent
		// TODO is the intent always present in shadow at this time? [med]
		String shadowIntent = obejctTypesDefinition.getIntent();
		if (intent != null && shadowIntent != null && !MiscSchemaUtil.equalsIntent(shadowIntent, intent)) {
			return false;
		}

		return true;
	}

	private List<TaskType> getTaskFor(List<PrismObject<TaskType>> tasks,
			ResourceObjectTypeDefinitionType objectTypeDefinition) {
		List<TaskType> syncTasks = new ArrayList<TaskType>();
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

			PrismProperty<QName> taskObjectClass = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.OBJECTCLASS_PROPERTY_NAME));
			QName taskObjectClassValue = null;
			if (taskObjectClass != null) {
				taskObjectClassValue = taskObjectClass.getRealValue();
			}

			if (isSynchronizationFor(objectTypeDefinition, taskObjectClassValue, taskKindValue, taskIntentValue)) {
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
				&& (attr.getInbound().get(0).getTarget() != null || attr.getInbound().get(0).getExpression() != null);
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
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getAdministrativeStatus() != null
							&& CollectionUtils.isNotEmpty(activationDef.getAdministrativeStatus().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getValidFrom() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidFrom().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getValidTo() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidTo().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasOutbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getExistence() != null
							&& CollectionUtils.isNotEmpty(activationDef.getExistence().getOutbound())) {
						hasOutbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getAdministrativeStatus() != null
							&& CollectionUtils.isNotEmpty(activationDef.getAdministrativeStatus().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getValidFrom() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidFrom().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
					if (activationDef.getValidTo() != null
							&& CollectionUtils.isNotEmpty(activationDef.getValidTo().getInbound())) {
						hasInbound = true;
					}
				}

				if (!hasInbound) {
					ResourceActivationDefinitionType activationDef = resourceObjectTypeDefinition.getActivation();
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

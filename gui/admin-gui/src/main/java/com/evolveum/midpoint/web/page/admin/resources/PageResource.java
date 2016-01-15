package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourcePasswordDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(url = "/admin/resource", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL, label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL, description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL, label = "PageResource.auth.resource.label", description = "PageResource.auth.resource.description") })
public class PageResource extends PageAdminResources {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

	private static final String DOT_CLASS = PageResource.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";

	private static final String FIELD_LAST_AVAILABILITY_STATUS = "lastStatus";
	private static final String FIELD_SOURCE_TARGET = "sourceTarget";
	private static final String FIELD_CREDENTIALS_MAPPING = "credentialsMapping";
	private static final String FIELD_ACTIVATION_MAPPING = "activationMapping";

	private static final String PANEL_CAPABILITIES = "capabilities";

	LoadableModel<PrismObject<ResourceType>> resourceModel;

	private LoadableModel<CapabilitiesDto> capabilitiesModel;

	public PageResource() {

	}

	public PageResource(PageParameters parameters) {
		getPageParameters().overwriteWith(parameters);
		initialize();
	}

	public PageResource(PageParameters parameters, PageTemplate previousPage) {
		getPageParameters().overwriteWith(parameters);
		setPreviousPage(previousPage);
		initialize();
	}

	private void initialize() {

		resourceModel = new LoadableModel<PrismObject<ResourceType>>() {

			@Override
			protected PrismObject<ResourceType> load() {
				return loadResource();
			}
		};

		capabilitiesModel = new LoadableModel<CapabilitiesDto>() {
			@Override
			protected CapabilitiesDto load() {
				return new CapabilitiesDto(getResourceType());
			}
		};

		initLayout();
	}

	protected String getResourceOid() {
		StringValue resourceOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		return resourceOid != null ? resourceOid.toString() : null;
	}

	private PrismObject<ResourceType> loadResource() {
		String resourceOid = getResourceOid();
		LOGGER.trace("Loading resource with oid: {}", resourceOid);

		Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);

		PrismObject<ResourceType> resource = WebModelUtils.loadObject(ResourceType.class, resourceOid, this, task,
				result);

		result.recomputeStatus();
		showResult(result, "pageAdminResources.message.cantLoadResource");

		return resource;
	}

	private void initLayout() {
		if (resourceModel == null || resourceModel.getObject() == null) {
			return;
		}

		ResourceType resource = getResourceType();

		addLastAvailabilityStatusInfo(resource);

		addSourceTargetInfo(resource);

		addCapabilityMappingInfo(FIELD_CREDENTIALS_MAPPING, determineCredentialsMappings(resource),
				"PageResource.resource.mapping.credentials");
		addCapabilityMappingInfo(FIELD_ACTIVATION_MAPPING, determineActivationMappings(resource),
				"PageResource.resource.mapping.activation");

		CapabilitiesPanel capabilities = new CapabilitiesPanel(PANEL_CAPABILITIES, capabilitiesModel);
		add(capabilities);

		List<ResourceConfigurationDto> resourceConfigList = createResourceConfigList(resource);

		ListDataProvider<ResourceConfigurationDto> resourceConfigProvider = new ListDataProvider<ResourceConfigurationDto>(
				PageResource.this, new ListModel<ResourceConfigurationDto>(resourceConfigList));

		List<String> columns = Arrays.asList("objectTypeDefinition.kind", "objectTypeDefinition.intent",
				"sync");
		List<IColumn<ResourceConfigurationDto, String>> tableColumns = createColumns(columns);

		BoxedTablePanel<ResourceConfigurationDto> resourceConfig = new BoxedTablePanel<ResourceConfigurationDto>(
				"resourceConfig", resourceConfigProvider, tableColumns);
		add(resourceConfig);

	}

	private List<IColumn<ResourceConfigurationDto, String>> createColumns(List<String> columns) {
		List<IColumn<ResourceConfigurationDto, String>> tableColumns = new ArrayList<IColumn<ResourceConfigurationDto, String>>();
		for (String column : columns) {
			PropertyColumn tableColumn = new PropertyColumn(createStringResource("UserType.givenName"), column);
			tableColumns.add(tableColumn);
		}
		return tableColumns;
	}

	private List<ResourceConfigurationDto> createResourceConfigList(ResourceType resource) {
		OperationResult result=  new OperationResult(OPERATION_LOAD_RESOURCE);
		Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
		List<PrismObject<TaskType>> tasks = WebModelUtils.searchObjects(TaskType.class, ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class, getPrismContext(), resource.getOid())), result, this);
		
		List<ResourceConfigurationDto> configs = new ArrayList<>();
		List<ResourceObjectTypeDefinitionType> objectTypes = resource.getSchemaHandling().getObjectType();
		for (ResourceObjectTypeDefinitionType objectType : objectTypes ){
			ShadowKindType objectTypeKind = objectType.getKind();
			String intent = objectType.getIntent();
			
			getTaskFor(tasks, objectTypeKind, intent);
			
			ResourceConfigurationDto resourceConfig = new ResourceConfigurationDto(objectType, true, new ArrayList<TaskType>());
			configs.add(resourceConfig);
		}
		
		return configs;
	}
	
	private TaskType getTaskFor(List<PrismObject<TaskType>> tasks, ShadowKindType kind, String intent){
		for (PrismObject<TaskType> task : tasks){
			PrismProperty<ShadowKindType> taskKind = task.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			ShadowKindType taskKindValue = null;
			if (taskKind != null){
				taskKindValue = taskKind.getRealValue();
			}
			
			PrismProperty<ShadowKindType> taskIntent = task.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			ShadowKindType taskIntentValue = null;
			if (taskIntent != null){
				taskIntentValue = taskIntent.getRealValue();
			}
		}
		
		return null;
	}

	private void addCapabilityMappingInfo(String fieldId, SourceTarget sourceTarget, String messageKey) {
		String backgroundColor = "bg-yellow";

		List<String> description = new ArrayList<>();
		description.add(getString(messageKey));

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, sourceTarget.getCssClass(), description);
		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		InfoBoxPanel activationMappingInfo = new InfoBoxPanel(fieldId, boxModel);
		add(activationMappingInfo);
	}

	private void addSourceTargetInfo(ResourceType resource) {

		String backgroundColor = "bg-yellow";
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

		InfoBoxPanel sourceTargetInfo = new InfoBoxPanel(FIELD_SOURCE_TARGET, boxModel);
		add(sourceTargetInfo);
	}

	private void addLastAvailabilityStatusInfo(ResourceType resource) {

		String backgroundColor = "bg-green";

		if (ResourceTypeUtil.isDown(resource)) {
			backgroundColor = "bg-red";
		}

		List<String> description = new ArrayList<>();
		Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		PrismObject<ConnectorType> connector = WebModelUtils.loadObject(ConnectorType.class,
				resource.getConnectorRef().getOid(), this, task, result);
		description.add(StringUtils
				.substringAfterLast(WebMiscUtil.getEffectiveName(connector, ConnectorType.F_CONNECTOR_TYPE), "."));
		ConnectorType connectorType = connector.asObjectable();
		description.add(connectorType.getConnectorVersion());
		description.add(connectorType.getConnectorBundle());

		InfoBoxType infoBoxType = new InfoBoxType(backgroundColor, "fa-power-off", description);
		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		InfoBoxPanel lastStatusInfo = new InfoBoxPanel(FIELD_LAST_AVAILABILITY_STATUS, boxModel);
		add(lastStatusInfo);
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

	private ResourceType getResourceType() {
		PrismObject<ResourceType> resource = resourceModel.getObject();
		return resource.asObjectable();
	}

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

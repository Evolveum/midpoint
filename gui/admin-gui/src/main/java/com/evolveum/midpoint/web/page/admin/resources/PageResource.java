package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.LoadableModel;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceConfigurationDto;
import com.evolveum.midpoint.web.page.admin.resources.dto.TestConnectionResultDto;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@PageDescriptor(url = "/admin/resource", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminResources.AUTH_RESOURCE_ALL, label = PageAdminResources.AUTH_RESOURCE_ALL_LABEL, description = PageAdminResources.AUTH_RESOURCE_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_RESOURCE_URL, label = "PageResource.auth.resource.label", description = "PageResource.auth.resource.description") })
public class PageResource extends PageAdminResources {

	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageResource.class);

	private static final String DOT_CLASS = PageResource.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCE = DOT_CLASS + "loadResource";
	private static final String OPERATION_TEST_CONNECTION = DOT_CLASS + "testConnection";

	private static final String FIELD_LAST_AVAILABILITY_STATUS = "lastStatus";
	private static final String FIELD_SOURCE_TARGET = "sourceTarget";
	private static final String FIELD_CREDENTIALS_MAPPING = "credentialsMapping";
	private static final String FIELD_ACTIVATION_MAPPING = "activationMapping";
	
	private static final String BUTTON_TEST_CONNECTION_ID= "testConnection";

	private static final String PANEL_CAPABILITIES = "capabilities";
	
	private static final String TABLE_TEST_CONNECTION_RESULT_ID = "testConnectionResults";
	
	private static final String FORM_DETAILS_OD = "details";

	LoadableModel<PrismObject<ResourceType>> resourceModel;

	private LoadableModel<CapabilitiesDto> capabilitiesModel;
	
	private ListModel testConnectionModel = new ListModel();

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
		showResult(result, "pageAdminResources.message.cantLoadResource", false);

		return resource;
	}

	private void initLayout() {
		if (resourceModel == null || resourceModel.getObject() == null) {
			return;
		}
		
		Form form = new Form(FORM_DETAILS_OD);
		add(form);

		form.add(createTestConnectionResult());
		
		ResourceType resource = getResourceType();

		form.add(addLastAvailabilityStatusInfo(resource));

		form.add(addSourceTargetInfo(resource));

		form.add(addCapabilityMappingInfo(FIELD_CREDENTIALS_MAPPING, determineCredentialsMappings(resource),
				"PageResource.resource.mapping.credentials"));
		form.add(addCapabilityMappingInfo(FIELD_ACTIVATION_MAPPING, determineActivationMappings(resource),
				"PageResource.resource.mapping.activation"));

		CapabilitiesPanel capabilities = new CapabilitiesPanel(PANEL_CAPABILITIES, capabilitiesModel);
		form.add(capabilities);

		List<ResourceConfigurationDto> resourceConfigList = createResourceConfigList(resource);

		ListDataProvider<ResourceConfigurationDto> resourceConfigProvider = new ListDataProvider<ResourceConfigurationDto>(
				PageResource.this, new ListModel<ResourceConfigurationDto>(resourceConfigList));

		List<ColumnTypeDto> columns = Arrays.asList(
				new ColumnTypeDto("ShadowType.kind", "objectTypeDefinition.kind", ShadowType.F_KIND.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.objectClass", "objectTypeDefinition.objectClass.localPart",
						ShadowType.F_OBJECT_CLASS.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", "objectTypeDefinition.intent",
						ShadowType.F_INTENT.getLocalPart()),
				new ColumnTypeDto<Boolean>("ResourceType.isSync", "sync", null));

		List<IColumn> tableColumns = ColumnUtils.createColumns(columns);

		// new ColumnTypeDto<>("ResourceType.tasks", "definedTasks", null, true)
		PropertyColumn tasksColumn = new PropertyColumn(
				createStringResource("ResourceType.tasks"), "definedTasks") {

			@Override
			public void populateItem(Item item, String componentId, final IModel rowModel) {
				ResourceConfigurationDto conf = (ResourceConfigurationDto) rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);
				for (final TaskType task : conf.getDefinedTasks()){
					repeater.add(new LinkPanel(repeater.newChildId(), new Model<String>(task.getName().getOrig())) {

			            @Override
			            public void onClick(AjaxRequestTarget target) {
			                PageResource.this.taskDetailsPerformed(target, task.getOid());
			            }

			           
			        });
				}
				
				
				item.add(repeater);
			}
			
		};

		BoxedTablePanel<ResourceConfigurationDto> resourceConfig = new BoxedTablePanel(
				"resourceConfig", resourceConfigProvider, tableColumns);
		form.add(resourceConfig);
		
		AjaxButton test = new AjaxButton(BUTTON_TEST_CONNECTION_ID, createStringResource("pageResource.button.test")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        form.add(test);

	}
	
	private void testConnectionPerformed(AjaxRequestTarget target) {
        PrismObject<ResourceType> dto = resourceModel.getObject();
        if (dto == null || StringUtils.isEmpty(dto.getOid())) {
            error(getString("pageResource.message.oidNotDefined"));
            target.add(getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_TEST_CONNECTION);
        PrismObject<ResourceType> resource = null;
        List<TestConnectionResultDto> resultsDto = new ArrayList<TestConnectionResultDto>();
        try {
        	
        	 Task task = createSimpleTask(OPERATION_TEST_CONNECTION);
             
            
            result = getModelService().testResource(dto.getOid(), task);
           
            for (ConnectorTestOperation connectorOperation : ConnectorTestOperation.values()){
            for (OperationResult testResult : result.getSubresults()){
            	if (connectorOperation.getOperation().equals(testResult.getOperation())){
            		TestConnectionResultDto resultDto = new TestConnectionResultDto(connectorOperation.getOperation(), testResult.getStatus(), testResult.getMessage());
            		resultsDto.add(resultDto);
            	}
            }
            }
            
           resource  = getModelService().getObject(ResourceType.class, dto.getOid(), null, task, result);
            
            
        } catch (ObjectNotFoundException ex) {
            result.recordFatalError("Failed to test resource connection", ex);
        } catch (ConfigurationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (SchemaException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (CommunicationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        } catch (SecurityViolationException e) {
            result.recordFatalError("Failed to test resource connection", e);
        }

//        // a bit of hack: result of TestConnection contains a result of getObject as a subresult
//        // so in case of TestConnection succeeding we recompute the result to show any (potential) getObject problems
        if (result.isSuccess()) {
            result.recomputeStatus();
        }
        
        BoxedTablePanel connResult = (BoxedTablePanel) get(createComponentPath(FORM_DETAILS_OD, TABLE_TEST_CONNECTION_RESULT_ID));
        ((ListDataProvider) connResult.getDataTable().getDataProvider()).getAvailableData().clear();
        ((ListDataProvider) connResult.getDataTable().getDataProvider()).getAvailableData().addAll(resultsDto);
        target.add(connResult);
        
        // this provides some additional tests, namely a test for schema handling section
       
        target.add(get(createComponentPath(FORM_DETAILS_OD, FIELD_LAST_AVAILABILITY_STATUS)));
        
        showResult(result, "Test connection failed", false);
//        target.add(getFeedbackPanel());
        
    }
	
	private BoxedTablePanel createTestConnectionResult(){
		
		final ListDataProvider<TestConnectionResultDto> listprovider = new ListDataProvider<TestConnectionResultDto>(getPage(), testConnectionModel);		
		List<ColumnTypeDto> columns = Arrays.asList(
				new ColumnTypeDto<String>("Operation Name", "operationName", null),
				new ColumnTypeDto("Status", "status", null),
				new ColumnTypeDto<String>("Error Message", "errorMessage", null));
		
		BoxedTablePanel testConnectionResults = new BoxedTablePanel(TABLE_TEST_CONNECTION_RESULT_ID, listprovider, ColumnUtils.createColumns(columns));
//		DataTable table = new DataTable(TABLE_TEST_CONNECTION_RESULT_ID, ColumnUtils.createColumns(columns), listprovider, 10);
////		TablePanel table = new TablePanel(TABLE_TEST_CONNECTION_RESULT_ID, listprovider, );
//		testConnectionResults.setOutputMarkupId(true);

		testConnectionResults.add(new VisibleEnableBehaviour(){
			
			@Override
			public boolean isVisible() {
				return CollectionUtils.isNotEmpty(listprovider.getAvailableData());
			}
		});
		
		return testConnectionResults;
	}

	private void taskDetailsPerformed(AjaxRequestTarget target, String taskOid){
		PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, taskOid);
        setResponsePage(new PageTaskEdit(parameters, this));
		
	}
	
//	private List<IColumn<ResourceConfigurationDto, String>> createColumns(List<ColumnTypeDto> columns) {
//		List<IColumn<ResourceConfigurationDto, String>> tableColumns = new ArrayList<IColumn<ResourceConfigurationDto, String>>();
//		for (ColumnTypeDto column : columns) {
//			// if (column.isMultivalue()) {
//			// PropertyColumn tableColumn = new PropertyColumn(displayModel,
//			// propertyExpression)
//			// } else {
//			PropertyColumn tableColumn = new PropertyColumn(createStringResource(column.getColumnName()),
//					column.getSortableColumn(), column.getColumnValue());
//			tableColumns.add(tableColumn);
//			// }
//		}
//		return tableColumns;
//	}

	private List<ResourceConfigurationDto> createResourceConfigList(ResourceType resource) {
		OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE);
		Task task = createSimpleTask(OPERATION_LOAD_RESOURCE);
		List<PrismObject<TaskType>> tasks = WebModelUtils.searchObjects(TaskType.class,
				ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class,
						getPrismContext(), resource.getOid())),
				result, this);

		List<ResourceConfigurationDto> configs = new ArrayList<>();
		
		if (resource.getSchemaHandling() == null){
			return configs;
		}
		
		List<ResourceObjectTypeDefinitionType> objectTypes = resource.getSchemaHandling().getObjectType();
		
		if (objectTypes == null){
			return configs;
		}
		
		for (ResourceObjectTypeDefinitionType objectType : objectTypes) {

			boolean sync = isSynchronizationFor(objectType, resource.getSynchronization().getObjectSynchronization());

			List<TaskType> syncTask = getTaskFor(tasks, objectType);

			ResourceConfigurationDto resourceConfig = new ResourceConfigurationDto(objectType, sync, syncTask);
			configs.add(resourceConfig);
		}

		return configs;
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

		InfoBoxPanel lastAvailabilityStatus = new InfoBoxPanel(FIELD_LAST_AVAILABILITY_STATUS, boxModel);
//		lastAvailabilityStatus.setOutputMarkupId(true);
		
		return lastAvailabilityStatus;
		
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

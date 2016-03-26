package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEventSink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.data.ListDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.ObjectListPanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.util.TaskOperationUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceContentResourcePanel extends ResourceContentPanel{

	private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";

	private static final String ID_IMPORT = "import";
	private static final String ID_RECONCILIATION = "reconciliation";
	private static final String ID_LIVE_SYNC = "liveSync";

	private static final String ID_INFO = "info";
	private static final String ID_LABEL = "label";
	private static final String ID_ICON = "icon";
	
	
	public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			ShadowKindType kind, String intent, PageBase pageBase) {
		super(id, resourceModel, kind, intent, pageBase);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ObjectQuery createQuery(IModel<PrismObject<ResourceType>> resourceModel)
			throws SchemaException {
		ObjectQuery baseQuery = null;
		
		RefinedObjectClassDefinition rOcDef = getDefinitionByKind(resourceModel);
		if (rOcDef != null) {
			if (rOcDef.getKind() != null) {
				baseQuery = ObjectQueryUtil.createResourceAndKindIntent(
						resourceModel.getObject().getOid(), rOcDef.getKind(), rOcDef.getIntent(),
						getPageBase().getPrismContext());
			} else {
				baseQuery = ObjectQueryUtil.createResourceAndObjectClassQuery(
						resourceModel.getObject().getOid(), rOcDef.getTypeName(),
						getPageBase().getPrismContext());
			}
		}
		return baseQuery;
	}
	
	private RefinedObjectClassDefinition getDefinitionByKind(IModel<PrismObject<ResourceType>> resourceModel) throws SchemaException{
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
					.getRefinedSchema(resourceModel.getObject(), getPageBase().getPrismContext());
			return refinedSchema.getRefinedDefinition(getKind(), getIntent());
	
	}

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean isUseObjectCounting(IModel<PrismObject<ResourceType>> resourceModel) {
		return false;
	}

	@Override
	protected void initCustomLayout(IModel<PrismObject<ResourceType>> resource) {
		OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);
	
		List<PrismObject<TaskType>> tasks = WebModelServiceUtils.searchObjects(TaskType.class,
				ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class,
						getPageBase().getPrismContext(), resource.getObject().getOid())),
				result, getPageBase());
		
		
		List<TaskType> tasksForKind = getTasksForKind(tasks);
		
		List<TaskType> importTasks = new ArrayList<>();
		List<TaskType> syncTasks = new ArrayList<>();
		List<TaskType> reconTasks = new ArrayList<>();
		for (TaskType task : tasksForKind){
			if (TaskCategory.RECONCILIATION.equals(task.getCategory())){
				reconTasks.add(task);
			} else if (TaskCategory.LIVE_SYNCHRONIZATION.equals(task.getCategory())){
			syncTasks.add(task);
			} else if (TaskCategory.IMPORTING_ACCOUNTS.equals(task.getCategory())){
				importTasks.add(task);
			}
		}
		
		
		initButton(ID_IMPORT, "Import", " fa-download", TaskCategory.IMPORTING_ACCOUNTS, importTasks);
		initButton(ID_RECONCILIATION, "Reconciliation", " fa-link", TaskCategory.RECONCILIATION, reconTasks);
		initButton(ID_LIVE_SYNC, "Live Sync", " fa-refresh", TaskCategory.LIVE_SYNCHRONIZATION, syncTasks);
//		ApplicationButtonPanel importButton = new ApplicationButtonPanel(ID_IMPORT, new Model<ApplicationButtonModel>(new ApplicationButtonModel(String.valueOf(importTasks.size()), " fa-download", "Import"))){
//			@Override
//			protected void onClick(AjaxRequestTarget target) {
//				// TODO Auto-generated method stub
//				super.onClick(target);
//			}
//		};
//		add(importButton);
//		
//		ApplicationButtonPanel reconButton = new ApplicationButtonPanel(ID_RECONCILIATION, new Model<ApplicationButtonModel>(new ApplicationButtonModel(String.valueOf(reconTasks.size()), " fa-download", "Import"))){
//			@Override
//			protected void onClick(AjaxRequestTarget target) {
//				// TODO Auto-generated method stub
//				super.onClick(target);
//			}
//		};
//		add(reconButton);
//		
//		ApplicationButtonPanel liveSyncButton = new ApplicationButtonPanel(ID_LIVE_SYNC, new Model<ApplicationButtonModel>(new ApplicationButtonModel(String.valueOf(syncTasks.size()), " fa-download", "Import"))){
//			@Override
//			protected void onClick(AjaxRequestTarget target) {
//				// TODO Auto-generated method stub
//				super.onClick(target);
//			}
//		};
//		add(liveSyncButton);
		
	}
	
	private void initButton(String id, String label, String icon, final String category, final List<TaskType> tasks){
		
		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentResourcePanel.showExisting"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						runTask(tasks, target);
					}
				});
		items.add(item);

		item = new InlineMenuItem(getPageBase().createStringResource("ResourceContentResourcePanel.newTask"),
				new InlineMenuItemAction() {

					@Override
					public void onClick(AjaxRequestTarget target) {
						newTaskPerformed(category, target);
					}
				});
		items.add(item);
		
		DropdownButtonPanel button = new DropdownButtonPanel(id, new DropdownButtonDto(String.valueOf(tasks.size()), icon, label, items));
		add(button);
//		AjaxButton button = new AjaxButton(id) {
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				runTask(tasks, target);
//			}
//		};
//		add(button);
//		
//		Label infoL = new Label(ID_INFO, String.valueOf(tasks.size()));
//		button.add(infoL);
//		
//		Label labelL = new Label(ID_LABEL, label);
//		button.add(labelL);
//		
//		WebMarkupContainer iconL = new WebMarkupContainer(ID_ICON);
//		iconL.add(AttributeModifier.append("class", icon));
//		button.add(iconL);
	}
	
	private void newTaskPerformed(String category, AjaxRequestTarget target) {
		TaskType taskType = new TaskType();
		PrismProperty<ShadowKindType> pKind;
		try {
			pKind = taskType.asPrismObject().findOrCreateProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			pKind.setRealValue(getKind());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}
		
		
		PrismProperty<String> pIntent;
		try {
			pIntent = taskType.asPrismObject().findOrCreateProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			pIntent.setRealValue(getIntent());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}
		
		
		PrismObject<ResourceType> resource = getResourceModel().getObject();
		taskType.setObjectRef(ObjectTypeUtil.createObjectRef(resource));
	
		taskType.setCategory(category);
		setResponsePage(new PageTaskAdd(taskType));;
	}
	
	private void runTask(List<TaskType> tasks, AjaxRequestTarget target){
		
		ResourceTasksPanel tasksPanel = new ResourceTasksPanel(getPageBase().getMainPopupBodyId(), new ListModel<>(tasks), getPageBase());
		getPageBase().showMainPopup(tasksPanel, new Model<String>("Defined tasks"), target, 900, 500);
		
//		final ObjectListPanel<TaskType> tasksPanel = new ObjectListPanel<TaskType>(ID_TASKS_TABLE, TaskType.class, getPageBase());
//		tasksPanel.setProvider(new ListDataProvider2(getPageBase(), new ListModel(tasks)));
//		tasksPanel.setEditable(false);
//		tasksPanel.setMultiSelect(true);
//		popupContent.add(tasksPanel);
//		
//		AjaxButton runNow = new AjaxButton(ID_RUN_NOW, getPageBase().createStringResource("pageTaskEdit.button.runNow")) {
//		
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
//				
//				OperationResult result = TaskOperationUtils.runNowPerformed(getPageBase().getTaskService(), oids);
//				getPageBase().showResult(result);
//				target.add(getPageBase().getFeedbackPanel());
//				
//			}
//		};
//		
//		AjaxButton resume = new AjaxButton(ID_RESUME, getPageBase().createStringResource("pageTaskEdit.button.resume")) {
//			
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
//				
//				OperationResult result = TaskOperationUtils.resumePerformed(getPageBase().getTaskService(), oids);
//				getPageBase().showResult(result);
//				target.add(getPageBase().getFeedbackPanel());
//				
//			}
//		};
//		
//		AjaxButton suspend = new AjaxButton(ID_SUSPEND, getPageBase().createStringResource("pageTaskEdit.button.suspend")) {
//			
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				List<String> oids = createOidList(getTaskListPanel().getSelectedObjects());
//				
//				OperationResult result = TaskOperationUtils.suspendPerformed(getPageBase().getTaskService(), oids);
//				getPageBase().showResult(result);
//				target.add(getPageBase().getFeedbackPanel());
//				
//			}
//		};
		
	}
	
	
	
	private List<TaskType> getTasksForKind(List<PrismObject<TaskType>> tasks){
		List<TaskType> tasksForKind = new ArrayList<>();
		for (PrismObject<TaskType> task : tasks) {
			PrismProperty<ShadowKindType> taskKind = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			ShadowKindType taskKindValue = null;
			if (taskKind != null) {
				taskKindValue = taskKind.getRealValue();
				
				PrismProperty<String> taskIntent = task
						.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
				String taskIntentValue = null;
				if (taskIntent != null) {
					taskIntentValue = taskIntent.getRealValue();
				}
				if (StringUtils.isNotEmpty(getIntent())){
					if (getKind() == taskKindValue && getIntent().equals(taskIntentValue)){
						tasksForKind.add(task.asObjectable());
					}
				} else if (getKind() == taskKindValue){
					tasksForKind.add(task.asObjectable());
				}
			}
		}
		return tasksForKind;
	}

}

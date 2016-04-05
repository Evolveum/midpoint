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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.page.admin.server.PageTaskAdd;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class ResourceContentResourcePanel extends ResourceContentPanel {

	private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";
	private static final String OPERATION_SEARCH_TASKS_FOR_RESOURCE = DOT_CLASS + "seachTasks";

	private static final String ID_IMPORT = "import";
	private static final String ID_RECONCILIATION = "reconciliation";
	private static final String ID_LIVE_SYNC = "liveSync";

	private static final String ID_OBJECT_CLASS = "objectClass";
	private static final String ID_LABEL = "label";
	private static final String ID_ICON = "icon";

	public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			QName objectClass, ShadowKindType kind, String intent, PageBase pageBase) {
		super(id, resourceModel, objectClass, kind, intent, pageBase);
	}

	@Override
	protected SelectorOptions<GetOperationOptions> addAdditionalOptions() {
		return null;
	}

	@Override
	protected boolean isUseObjectCounting() {
		return false;
	}

	@Override
	protected void initCustomLayout() {
		OperationResult result = new OperationResult(OPERATION_SEARCH_TASKS_FOR_RESOURCE);

		List<PrismObject<TaskType>> tasks = WebModelServiceUtils
				.searchObjects(TaskType.class,
						ObjectQuery.createObjectQuery(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF,
								TaskType.class, getPageBase().getPrismContext(),
								getResourceModel().getObject().getOid())),
						result, getPageBase());

		List<TaskType> tasksForKind = getTasksForKind(tasks);

		List<TaskType> importTasks = new ArrayList<>();
		List<TaskType> syncTasks = new ArrayList<>();
		List<TaskType> reconTasks = new ArrayList<>();
		for (TaskType task : tasksForKind) {
			if (TaskCategory.RECONCILIATION.equals(task.getCategory())) {
				reconTasks.add(task);
			} else if (TaskCategory.LIVE_SYNCHRONIZATION.equals(task.getCategory())) {
				syncTasks.add(task);
			} else if (TaskCategory.IMPORTING_ACCOUNTS.equals(task.getCategory())) {
				importTasks.add(task);
			}
		}

		initButton(ID_IMPORT, "Import", " fa-download", TaskCategory.IMPORTING_ACCOUNTS, importTasks);
		initButton(ID_RECONCILIATION, "Reconciliation", " fa-link", TaskCategory.RECONCILIATION, reconTasks);
		initButton(ID_LIVE_SYNC, "Live Sync", " fa-refresh", TaskCategory.LIVE_SYNCHRONIZATION, syncTasks);

		Label objectClass = new Label(ID_OBJECT_CLASS, new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				RefinedObjectClassDefinition ocDef;
				try {
					ocDef = getDefinitionByKind();
					if (ocDef != null) {
						return ocDef.getObjectClassDefinition().getTypeName().getLocalPart();
					}
				} catch (SchemaException e) {
				}

				return "NOT FOUND";
			}
		});
		add(objectClass);

	}

	private void initButton(String id, String label, String icon, final String category,
			final List<TaskType> tasks) {

		List<InlineMenuItem> items = new ArrayList<>();

		InlineMenuItem item = new InlineMenuItem(
				getPageBase().createStringResource("ResourceContentResourcePanel.showExisting"),
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

		DropdownButtonPanel button = new DropdownButtonPanel(id,
				new DropdownButtonDto(String.valueOf(tasks.size()), icon, label, items));
		add(button);

	}

	private void newTaskPerformed(String category, AjaxRequestTarget target) {
		TaskType taskType = new TaskType();
		PrismProperty<ShadowKindType> pKind;
		try {
			pKind = taskType.asPrismObject().findOrCreateProperty(
					new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			pKind.setRealValue(getKind());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}

		PrismProperty<String> pIntent;
		try {
			pIntent = taskType.asPrismObject().findOrCreateProperty(
					new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
			pIntent.setRealValue(getIntent());
		} catch (SchemaException e) {
			getSession().warn("Could not set kind for new task " + e.getMessage());
		}

		PrismObject<ResourceType> resource = getResourceModel().getObject();
		taskType.setObjectRef(ObjectTypeUtil.createObjectRef(resource));

		taskType.setCategory(category);
		setResponsePage(new PageTaskAdd(taskType));
		;
	}

	private void runTask(List<TaskType> tasks, AjaxRequestTarget target) {

		ResourceTasksPanel tasksPanel = new ResourceTasksPanel(getPageBase().getMainPopupBodyId(), false, 
				new ListModel<>(tasks), getPageBase());
		getPageBase().showMainPopup(tasksPanel, new Model<String>("Defined tasks"), target, 900, 500);

	}

	private List<TaskType> getTasksForKind(List<PrismObject<TaskType>> tasks) {
		List<TaskType> tasksForKind = new ArrayList<>();
		for (PrismObject<TaskType> task : tasks) {
			PrismProperty<ShadowKindType> taskKind = task
					.findProperty(new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
			ShadowKindType taskKindValue = null;
			if (taskKind != null) {
				taskKindValue = taskKind.getRealValue();

				PrismProperty<String> taskIntent = task.findProperty(
						new ItemPath(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
				String taskIntentValue = null;
				if (taskIntent != null) {
					taskIntentValue = taskIntent.getRealValue();
				}
				if (StringUtils.isNotEmpty(getIntent())) {
					if (getKind() == taskKindValue && getIntent().equals(taskIntentValue)) {
						tasksForKind.add(task.asObjectable());
					}
				} else if (getKind() == taskKindValue) {
					tasksForKind.add(task.asObjectable());
				}
			}
		}
		return tasksForKind;
	}

	@Override
	protected Search createSearch() {
		Map<ItemPath, ItemDefinition> availableDefs = new HashMap<>();
		availableDefs.putAll(createAttributeDefinitionList());

		Search search = new Search(ShadowType.class, availableDefs);
//		search.setShowAdvanced(true);

//		SchemaRegistry registry = ctx.getSchemaRegistry();
//		PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(Sh.class);
//		PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);
//
//		search.addItem(def);

		return search;
	}

//	private <T extends ObjectType> Search createSearch() {
//		Map<ItemPath, ItemDefinition> availableDefs = new HashMap<>();
//		availableDefs.putAll(createAttributeDefinitionList());
//
//		Search search = new Search(ShadowType.class, availableDefs);
//		search.setShowAdvanced(true);
//
////		SchemaRegistry registry = ctx.getSchemaRegistry();
////		PrismObjectDefinition objDef = registry.findObjectDefinitionByCompileTimeClass(Sh.class);
////		PrismPropertyDefinition def = objDef.findPropertyDefinition(ObjectType.F_NAME);
////
////		search.addItem(def);
//
//		return search;
//	}

	private <T extends ObjectType> Map<ItemPath, ItemDefinition> createAttributeDefinitionList() {

		Map<ItemPath, ItemDefinition> map = new HashMap<>();

		RefinedObjectClassDefinition ocDef = null;
		try {

			if (getKind() != null) {

				ocDef = getDefinitionByKind();

			} else if (getObjectClass() != null) {
				ocDef = getDefinitionByObjectClass();

			}
		} catch (SchemaException e) {
			warn("Could not get determine object class definition");
			return map;
		}

		
		ItemPath attributePath = new ItemPath(ShadowType.F_ATTRIBUTES);

//		PrismContainerDefinition attrs = objDef.findContainerDefinition(ShadowType.F_ATTRIBUTES);
		for (ItemDefinition def : (List<ItemDefinition>) ocDef.getDefinitions()) {
			if (!(def instanceof PrismPropertyDefinition) && !(def instanceof PrismReferenceDefinition)) {
				continue;
			}

			map.put(new ItemPath(attributePath, def.getName()), def);
		}

		return map;
	}

	@Override
	protected ModelExecuteOptions createModelOptions() {
		return null;
	}

}

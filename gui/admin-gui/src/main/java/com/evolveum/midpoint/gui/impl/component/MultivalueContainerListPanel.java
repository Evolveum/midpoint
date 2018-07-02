/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentPanel;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.ContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.AssignmentsTabStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanel<C extends Containerable> extends BasePanel<ContainerWrapper<C>> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ITEMS = "items";
	private static final String ID_NEW_ITEM_BUTTON = "newItemButton";
	private static final String ID_ITEMS_TABLE = "itemsTable";
	public static final String ID_ITEMS_DETAILS = "itemsDetails";
	public static final String ID_ITEM_DETAILS = "itemtDetails";

	public static final String ID_DETAILS = "details";

	private final static String ID_DONE_BUTTON = "doneButton";
	private final static String ID_CANCEL_BUTTON = "cancelButton";

	private static final Trace LOGGER = TraceManager.getTrace(AssignmentPanel.class);

	protected boolean itemDetailsVisible;
	private List<ContainerValueWrapper<C>> detailsPanelItemsList = new ArrayList<>();
	
	public MultivalueContainerListPanel(String id, IModel<ContainerWrapper<C>> model) {
		super(id, model);
	}
	
	protected abstract void initPaging();

	@Override
	protected void onInitialize() {
		super.onInitialize();
		initPaging();
		initLayout();
	}
	
	private void initLayout() {

		initListPanel();

		initDetailsPanel();

		setOutputMarkupId(true);

	}

	private void initListPanel() {
		WebMarkupContainer assignmentsContainer = new WebMarkupContainer(ID_ITEMS);
		assignmentsContainer.setOutputMarkupId(true);
		add(assignmentsContainer);

		BoxedTablePanel<ContainerValueWrapper<C>> assignmentTable = initAssignmentTable();
		assignmentsContainer.add(assignmentTable);

		AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_ITEM_BUTTON, new Model<>("fa fa-plus"),
				createStringResource("MainObjectListPanel.newObject")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				newAssignmentClickPerformed(target);
			}
		};

		newObjectIcon.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return enableActionNewObject();
			}
		});
		assignmentsContainer.add(newObjectIcon);

		createCustomLayout(assignmentsContainer);

		assignmentsContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return !itemDetailsVisible;
			}
		});

	}
	
	protected abstract boolean enableActionNewObject();

	private BoxedTablePanel<ContainerValueWrapper<C>> initAssignmentTable() {

		ContainerListDataProvider containersProvider = new ContainerListDataProvider(this, new PropertyModel<>(getModel(), "values")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				getAssignmentsStorage().setPaging(paging);
			}

			@Override
			public ObjectQuery getQuery() {
				return createQuery();
			}
			
			@Override
			protected List<ContainerValueWrapper<C>> searchThroughList() {
				List<ContainerValueWrapper<C>> resultList = super.searchThroughList();
				return postSearch(resultList);
			}

		};

		List<IColumn<ContainerValueWrapper<C>, String>> columns = createColumns();

		BoxedTablePanel<ContainerValueWrapper<C>> itemTable = new BoxedTablePanel<ContainerValueWrapper<C>>(ID_ITEMS_TABLE,
				containersProvider, columns, getTableId(), getItemsPerPage()) {
			private static final long serialVersionUID = 1L;

			@Override
			public int getItemsPerPage() {
				return getPageBase().getSessionStorage().getUserProfile().getTables()
						.get(UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE);
			}

			@Override
			protected Item<ContainerValueWrapper<C>> customizeNewRowItem(Item<ContainerValueWrapper<C>> item,
																					  IModel<ContainerValueWrapper<C>> model) {
				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
							@Override
							public String getObject() {
								return GuiImplUtil.getObjectStatus(((ContainerValueWrapper<Containerable>)model.getObject()));
							}
						}));
				return item;
			}

		};
		itemTable.setOutputMarkupId(true);
		itemTable.setCurrentPage(getAssignmentsStorage().getPaging());
		return itemTable;

	}
	
	protected List<ContainerValueWrapper<C>> postSearch(List<ContainerValueWrapper<C>> assignments) {
		return assignments;
	}

	protected AssignmentsTabStorage getAssignmentsStorage() {
		return getPageBase().getSessionStorage().getAssignmentsTabStorage();
	}

	protected abstract ObjectQuery createQuery();

	protected abstract List<IColumn<ContainerValueWrapper<C>, String>> createColumns();
	
	protected abstract void newAssignmentClickPerformed(AjaxRequestTarget target);

	protected abstract void createCustomLayout(WebMarkupContainer assignmentsContainer);
	
	protected abstract void createDetailsPanel(WebMarkupContainer assignmentsContainer);

	private void initDetailsPanel() {
		WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
		details.setOutputMarkupId(true);
		details.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return itemDetailsVisible;
			}
		});

		add(details);

		createDetailsPanel(details);

		AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON,
				createStringResource("AssignmentPanel.doneButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				itemDetailsVisible = false;
				refreshTable(target);
				target.add(MultivalueContainerListPanel.this);
			}
		};
		details.add(doneButton);

		AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
				createStringResource("AssignmentPanel.cancelButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				itemDetailsVisible = false;
				ajaxRequestTarget.add(MultivalueContainerListPanel.this);
			}
		};
		details.add(cancelButton);
	}

	protected ContainerListDataProvider getAssignmentListProvider() {
		return (ContainerListDataProvider) getAssignmentTable().getDataTable().getDataProvider();
	}

	protected BoxedTablePanel<ContainerValueWrapper<C>> getAssignmentTable() {
		return (BoxedTablePanel<ContainerValueWrapper<C>>) get(createComponentPath(ID_ITEMS, ID_ITEMS_TABLE));
	}

	protected abstract String getAuthirizationForRemoveAction();
	
	protected abstract String getAuthirizationForAddAction();
	
	protected abstract TableId getTableId();

	protected abstract int getItemsPerPage();

	protected void refreshTable(AjaxRequestTarget target) {
		target.add(getItemContainer().addOrReplace(initAssignmentTable()));
	}

	protected ContainerValueWrapper<C> createNewAssignmentContainerValueWrapper(PrismContainerValue<C> newAssignment) {
		ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
		Task task = getPageBase().createSimpleTask("Creating new assignment");
		ContainerValueWrapper<C> valueWrapper = factory.createContainerValueWrapper(getModelObject(), newAssignment,
                getModelObject().getObjectStatus(), ValueStatus.ADDED, getModelObject().getPath(), task);
		valueWrapper.setShowEmpty(true, false);
		getModelObject().getValues().add(valueWrapper);
		return valueWrapper;
	}

	protected WebMarkupContainer getItemContainer() {
		return (WebMarkupContainer) get(ID_ITEMS);
	}

}

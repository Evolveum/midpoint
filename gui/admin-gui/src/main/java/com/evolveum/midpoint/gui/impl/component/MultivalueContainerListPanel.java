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

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.model.PropertyWrapperFromContainerValueWrapperModel;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanel<C extends Containerable> extends BasePanel<ContainerWrapper<C>> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ITEMS = "items";
	private static final String ID_ITEMS_TABLE = "itemsTable";
	public static final String ID_SEARCH_ITEM_PANEL = "search";

	public static final String ID_DETAILS = "details";

	private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListPanel.class);

	private TableId tableId;
	private int itemPerPage;
	private PageStorage pageStorage;
	
	public MultivalueContainerListPanel(String id, IModel<ContainerWrapper<C>> model, TableId tableId, int itemPerPage, PageStorage pageStorage) {
		super(id, model);
		this.tableId = tableId;
		this.itemPerPage = itemPerPage;
		this.pageStorage = pageStorage;
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initPaging();
		initLayout();
	}
	
	private void initLayout() {

		initListPanel();

		initCustomLayout();
		
		setOutputMarkupId(true);

	}
	
	protected abstract void initPaging();
	
	protected abstract void initCustomLayout();
	
	private void initListPanel() {
		WebMarkupContainer itemsContainer = new WebMarkupContainer(ID_ITEMS);
		itemsContainer.setOutputMarkupId(true);
		add(itemsContainer);

		BoxedTablePanel<ContainerValueWrapper<C>> itemTable = initItemTable();
		itemsContainer.add(itemTable);

//		AjaxIconButton newObjectIcon = new AjaxIconButton(ID_NEW_ITEM_BUTTON, new Model<>("fa fa-plus"),
//				createStringResource("MainObjectListPanel.newObject")) {
//
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public void onClick(AjaxRequestTarget target) {
//				newItemPerformed(target);
//			}
//		};
//
//		newObjectIcon.add(new VisibleEnableBehaviour() {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			public boolean isVisible() {
//				return enableActionNewObject();
//			}
//		});
//		itemsContainer.add(newObjectIcon);
		
		WebMarkupContainer searchContainer = getSearchPanel(ID_SEARCH_ITEM_PANEL);
		itemsContainer.add(searchContainer);
		itemsContainer.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return isListPanelVisible();
			}
		});

	}
	
	protected boolean isListPanelVisible() {
		return true;
	}
	
	protected WebMarkupContainer getSearchPanel(String contentAreaId) {
		return new WebMarkupContainer(contentAreaId);
	}
	
	protected abstract boolean enableActionNewObject();

	private BoxedTablePanel<ContainerValueWrapper<C>> initItemTable() {

		MultivalueContainerListDataProvider<C> containersProvider = new MultivalueContainerListDataProvider<C>(this, new PropertyModel<>(getModel(), "values")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				pageStorage.setPaging(paging);
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
				containersProvider, columns, tableId, itemPerPage) {
			private static final long serialVersionUID = 1L;

			@Override
			public int getItemsPerPage() {
				return getPageBase().getSessionStorage().getUserProfile().getTables()
						.get(getTableId());
			}

			@Override
			protected Item<ContainerValueWrapper<C>> customizeNewRowItem(Item<ContainerValueWrapper<C>> item,
																					  IModel<ContainerValueWrapper<C>> model) {
				item.add(AttributeModifier.append("class", new AbstractReadOnlyModel<String>() {
					
							private static final long serialVersionUID = 1L;

							@Override
							public String getObject() {
								return GuiImplUtil.getObjectStatus(((ContainerValueWrapper<Containerable>)model.getObject()));
							}
						}));
				return item;
			}
			
			@Override
			protected WebMarkupContainer createButtonToolbar(String id) {
				AjaxIconButton newObjectIcon = new AjaxIconButton(id, new Model<>("fa fa-plus"),
						createStringResource("MainObjectListPanel.newObject")) {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						newItemPerformed(target);
					}
				};

				newObjectIcon.add(new VisibleEnableBehaviour() {
					private static final long serialVersionUID = 1L;

					@Override
					public boolean isVisible() {
						return enableActionNewObject();
					}
				});
				newObjectIcon.add(AttributeModifier.append("class", createStyleClassModelForNewObjectIcon()));
				return newObjectIcon;
			}

		};
		itemTable.setOutputMarkupId(true);
		itemTable.setCurrentPage(pageStorage.getPaging());
		return itemTable;

	}
	
	private IModel<String> createStyleClassModelForNewObjectIcon() {
        return new AbstractReadOnlyModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	return "btn btn-success btn-sm";
            }
        };
    }
	
	protected abstract List<ContainerValueWrapper<C>> postSearch(List<ContainerValueWrapper<C>> items);

	protected abstract ObjectQuery createQuery();

	protected abstract List<IColumn<ContainerValueWrapper<C>, String>> createColumns();
	
	protected abstract void newItemPerformed(AjaxRequestTarget target);
	
	public BoxedTablePanel<ContainerValueWrapper<C>> getItemTable() {
		return (BoxedTablePanel<ContainerValueWrapper<C>>) get(createComponentPath(ID_ITEMS, ID_ITEMS_TABLE));
	}

	public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
		IModel objectModel = new LoadableModel<ContainerValueWrapper<GlobalPolicyRuleType>>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected ContainerValueWrapper<GlobalPolicyRuleType> load() {
				return ((ContainerValueWrapper<GlobalPolicyRuleType>)getModelObject().getValues().get(0));
			}
		};
		ajaxRequestTarget.add(getItemContainer().addOrReplace(initItemTable()));
	}

	public WebMarkupContainer getItemContainer() {
		return (WebMarkupContainer) get(ID_ITEMS);
	}
	
	public PrismObject getFocusObject(){
		FocusMainPanel mainPanel = findParent(FocusMainPanel.class);
		if (mainPanel != null) {
			return mainPanel.getObjectWrapper().getObject();
		}
		return null;
	}
	
	public List<ContainerValueWrapper<C>> getSelectedItems() {
		BoxedTablePanel<ContainerValueWrapper<C>> itemsTable = getItemTable();
		MultivalueContainerListDataProvider<C> itemsProvider = (MultivalueContainerListDataProvider<C>) itemsTable.getDataTable()
				.getDataProvider();
		return itemsProvider.getAvailableData().stream().filter(a -> a.isSelected()).collect(Collectors.toList());
	}
	
	public void reloadSavePreviewButtons(AjaxRequestTarget target){
		FocusMainPanel mainPanel = findParent(FocusMainPanel.class);
		if (mainPanel != null) {
			mainPanel.reloadSavePreviewButtons(target);
		}
	}
	
	public ContainerValueWrapper<C> createNewItemContainerValueWrapper(
			PrismContainerValue<C> newItem,
			IModel<ContainerWrapper<C>> model) {
    	ContainerWrapperFactory factory = new ContainerWrapperFactory(getPageBase());
		Task task = getPageBase().createSimpleTask("Creating new object policy");
		ContainerValueWrapper<C> valueWrapper = factory.createContainerValueWrapper(model.getObject(), newItem,
				model.getObject().getObjectStatus(), ValueStatus.ADDED, model.getObject().getPath(), task);
		valueWrapper.setShowEmpty(true, true);
		model.getObject().getValues().add(valueWrapper);
		return valueWrapper;
	}
	
	public ColumnMenuAction<ContainerValueWrapper<C>> createDeleteColumnAction() {
		return new ColumnMenuAction<ContainerValueWrapper<C>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				if (getRowModel() == null) {
					deleteItemPerformed(target, getSelectedItems());
				} else {
					List<ContainerValueWrapper<C>> toDelete = new ArrayList<>();
					toDelete.add(getRowModel().getObject());
					deleteItemPerformed(target, toDelete);
				}
			}
		};
	}

	public ColumnMenuAction<ContainerValueWrapper<C>> createEditColumnAction() {
		return new ColumnMenuAction<ContainerValueWrapper<C>>() {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				itemPerformedForDefaultAction(target, getRowModel(), getSelectedItems());
			}
		};
	}
	
	protected abstract void itemPerformedForDefaultAction(AjaxRequestTarget target, IModel<ContainerValueWrapper<C>> rowModel, List<ContainerValueWrapper<C>> listItems);
	
	private void deleteItemPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<C>> toDelete) {
		if (toDelete == null){
			return;
		}
		toDelete.forEach(value -> {
			if (value.getStatus() == ValueStatus.ADDED) {
				ContainerWrapper<C> wrapper = getModelObject();
				wrapper.getValues().remove(value);
			} else {
				value.setStatus(ValueStatus.DELETED);
			}
			value.setSelected(false);
		});
		refreshTable(target);
		reloadSavePreviewButtons(target);
	}
	
	public List<InlineMenuItem> getDefaultMenuActions() {
		List<InlineMenuItem> menuItems = new ArrayList<>();
		PrismObject obj = getFocusObject();
		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.unassign"), new Model<>(true),
			new Model<>(true), false, createDeleteColumnAction(), 0, GuiStyleConstants.CLASS_DELETE_MENU_ITEM,
			DoubleButtonColumn.BUTTON_COLOR_CLASS.DANGER.toString()));

		menuItems.add(new InlineMenuItem(createStringResource("PageBase.button.edit"), new Model<>(true),
            new Model<>(true), false, createEditColumnAction(), 1, GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
			DoubleButtonColumn.BUTTON_COLOR_CLASS.DEFAULT.toString()));
		return menuItems;
	}
}

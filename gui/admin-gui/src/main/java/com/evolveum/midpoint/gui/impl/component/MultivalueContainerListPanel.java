/*
 * Copyright (c) 2018 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.MultifunctionalButton;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanel<C extends Containerable, S extends Serializable> extends BasePanel<ContainerWrapperImpl<C>> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ITEMS = "items";
	private static final String ID_ITEMS_TABLE = "itemsTable";
	public static final String ID_SEARCH_ITEM_PANEL = "search";

	public static final String ID_DETAILS = "details";

	private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListPanel.class);

	private TableId tableId;
	private PageStorage pageStorage;
	
	private LoadableModel<Search> searchModel = null;
	
	public MultivalueContainerListPanel(String id, IModel<ContainerWrapperImpl<C>> model, TableId tableId, PageStorage pageStorage) {
		super(id, model);
		this.tableId = tableId;
		this.pageStorage = pageStorage;
		
		searchModel = new LoadableModel<Search>(false) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected Search load() {
				PrismContainerDefinition<C> containerDef = model.getObject().getItemDefinition();
		    	List<SearchItemDefinition> availableDefs = initSearchableItems(containerDef);
		    	
		    	Search search = new Search(model.getObject().getItem().getCompileTimeClass(), availableDefs);
				return search;
			}

			
		};
	}
	
	protected abstract List<SearchItemDefinition> initSearchableItems(PrismContainerDefinition<C> containerDef);
	
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
				return MultivalueContainerListPanel.this.createProviderQuery();
			}
			
			@Override
			protected List<ContainerValueWrapper<C>> searchThroughList() {
				List<ContainerValueWrapper<C>> resultList = super.searchThroughList();
				return postSearch(resultList);
			}

		};

		List<IColumn<ContainerValueWrapper<C>, String>> columns = createColumns();

		int itemPerPage = (int) getPageBase().getItemsPerPage(tableId);
		BoxedTablePanel<ContainerValueWrapper<C>> itemTable = new BoxedTablePanel<ContainerValueWrapper<C>>(ID_ITEMS_TABLE,
				containersProvider, columns, tableId, itemPerPage) {
			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return MultivalueContainerListPanel.this.initSearch(headerId);
			}

			@Override
			public int getItemsPerPage() {
				return getPageBase().getSessionStorage().getUserProfile().getTables()
						.get(getTableId());
			}

			@Override
			protected Item<ContainerValueWrapper<C>> customizeNewRowItem(Item<ContainerValueWrapper<C>> item,
																					  IModel<ContainerValueWrapper<C>> model) {
				item.add(AttributeModifier.append("class", new IModel<String>() {
					
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
				return initButtonToolbar(id);
			}

		};
		itemTable.setOutputMarkupId(true);
		itemTable.setCurrentPage(pageStorage.getPaging());
		return itemTable;

	}
	
	protected WebMarkupContainer initButtonToolbar(String id) {
		return getNewItemButton(id);
	}
	
	public MultifunctionalButton<S> getNewItemButton(String id) {
		MultifunctionalButton<S> newObjectIcon =
				new MultifunctionalButton<S>(id) {
					private static final long serialVersionUID = 1L;

					@Override
					protected List<S> getAdditionalButtonsObjects() {
						return getNewObjectInfluencesList();
					}

					@Override
					protected void buttonClickPerformed(AjaxRequestTarget target, S influencingObject) {
						List<S> additionalButtonObjects = getNewObjectInfluencesList();
						if (influencingObject == null && (additionalButtonObjects == null || additionalButtonObjects.size() == 0)) {
							newItemPerformed(target);
						} else {
							newItemPerformed(target, influencingObject);
						}
					}

					@Override
					protected DisplayType getMainButtonDisplayType() {
						return getNewObjectButtonDisplayType();
					}

					@Override
					protected DisplayType getAdditionalButtonDisplayType(S buttonObject) {
						return getNewObjectAdditionalButtonDisplayType(buttonObject);
					}

					@Override
					protected DisplayType getDefaultObjectButtonDisplayType() {
						return getNewObjectButtonDisplayType();
					}
				};
		newObjectIcon.add(AttributeModifier.append("class", "btn-group btn-margin-right"));
		newObjectIcon.add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return enableActionNewObject();
			}

			@Override
			public boolean isEnabled() {
				return isNewObjectButtonEnabled();
			}
		});
//		newObjectIcon.add(AttributeModifier.append("class", createStyleClassModelForNewObjectIcon()));
		return newObjectIcon;
	}

	protected boolean isNewObjectButtonEnabled(){
		return true;
	}

	protected List<S> getNewObjectInfluencesList(){
		return new ArrayList<>();
	}

	protected DisplayType getNewObjectButtonDisplayType(){
		return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", createStringResource("MainObjectListPanel.newObject").getString());
	}

	protected DisplayType getNewObjectAdditionalButtonDisplayType(S buttonObject){
		return null;
	}

	protected WebMarkupContainer initSearch(String headerId) {
		SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
				MultivalueContainerListPanel.this.searchPerformed(query, target);
			}
		};
		return searchPanel;
	}
	
	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

//		MultivalueContainerListDataProvider<C> provider = getDataProvider();

//		ObjectQuery finalQuery = null;
//
//		ObjectQuery searchQuery = getQuery();
//
//		ObjectQuery customQuery = createQuery();
//
//		if (query != null && query.getFilter() != null) {
//			if (customQuery != null && customQuery.getFilter() != null) {
//				finalQuery = ObjectQuery.createObjectQuery(AndFilter.createAnd(customQuery.getFilter(), query.getFilter()));
//			}
//			finalQuery = query;
//
//		} else {
//			finalQuery = customQuery;
//		}
//
//		provider.setQuery(finalQuery);
//		String storageKey = getStorageKey();
//		if (StringUtils.isNotEmpty(storageKey)) {
//			PageStorage storage = getPageStorage(storageKey);
//			if (storage != null) {
//				storage.setSearch(searchModel.getObject());
//				storage.setPaging(null);
//			}
//		}
//		
		Table table = getItemTable();
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(getPageBase().getFeedbackPanel());

	}
	
	private ObjectQuery getQuery() {
		Search search = searchModel.getObject();
		ObjectQuery query = search.createObjectQuery(getPageBase().getPrismContext());
//		query = addFilterToContentQuery(query);
		return query;
	}
	
	private MultivalueContainerListDataProvider<C> getDataProvider() {
		return (MultivalueContainerListDataProvider<C>) getItemTable().getDataTable().getDataProvider();
	}

	
	protected IModel<String> createStyleClassModelForNewObjectIcon() {
        return new IModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	return "btn btn-success btn-sm";
            }
        };
    }
	
	protected abstract List<ContainerValueWrapper<C>> postSearch(List<ContainerValueWrapper<C>> items);

	private ObjectQuery createProviderQuery() {
		ObjectQuery searchQuery = getQuery();

		ObjectQuery customQuery = createQuery();

		if (searchQuery != null && searchQuery.getFilter() != null) {
			if (customQuery != null && customQuery.getFilter() != null) {
				return getPrismContext().queryFactory().createQuery(getPrismContext().queryFactory().createAnd(customQuery.getFilter(), searchQuery.getFilter()));
			}
			return searchQuery;

		}
		return customQuery;
	}

	protected abstract ObjectQuery createQuery();

	protected abstract List<IColumn<ContainerValueWrapper<C>, String>> createColumns();
	
	protected void newItemPerformed(AjaxRequestTarget target){}
	
	protected void newItemPerformed(AjaxRequestTarget target, S influencingObject){}

	public BoxedTablePanel<ContainerValueWrapper<C>> getItemTable() {
		return (BoxedTablePanel<ContainerValueWrapper<C>>) get(createComponentPath(ID_ITEMS, ID_ITEMS_TABLE));
	}

	public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
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
			IModel<ContainerWrapperImpl<C>> model) {
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
	
	protected void deleteItemPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<C>> toDelete) {
		if (toDelete == null || toDelete.isEmpty()){
			warn(createStringResource("MultivalueContainerListPanel.message.noAssignmentSelected").getString());
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		toDelete.forEach(value -> {
			if (value.getStatus() == ValueStatus.ADDED) {
				ContainerWrapperImpl<C> wrapper = getModelObject();
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
		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.unassign")) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
			}

			@Override
			public InlineMenuItemAction initAction() {
				return createDeleteColumnAction();
			}
		});

		menuItems.add(new ButtonInlineMenuItem(createStringResource("PageBase.button.edit")) {
			private static final long serialVersionUID = 1L;

			@Override
			public String getButtonIconCssClass() {
				return GuiStyleConstants.CLASS_EDIT_MENU_ITEM;
			}

			@Override
			public InlineMenuItemAction initAction() {
				return createEditColumnAction();
			}
		});
		return menuItems;
	}
}

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
package com.evolveum.midpoint.gui.api.component;

import java.util.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider2;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<O extends ObjectType> extends BasePanel<O> {
	private static final long serialVersionUID = 1L;

	private static final String ID_MAIN_FORM = "mainForm";

	private static final String ID_TABLE = "table";

	private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);

	private Class<? extends O> type;
	private PageBase parentPage;

	private LoadableModel<Search> searchModel;

	private Collection<SelectorOptions<GetOperationOptions>> options;

	private boolean multiselect;
	
	private TableId tableId;

	protected List<O> selectedObjects = null;

	private String addutionalBoxCssClasses;

	public Class<? extends O> getType() {
		return type;
	}

	/**
	 * @param defaultType specifies type of the object that will be selected by default. It can be changed.
	 */
	public ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options,
			PageBase parentPage) {
		super(id);
		this.type = defaultType;
		this.parentPage = parentPage;
		this.options = options;
		this.tableId = tableId;
		initLayout();
	}

	/**
	 * @param defaultType specifies type of the object that will be selected by default. It can be changed.
	 */
	ObjectListPanel(String id, Class<? extends O> defaultType, boolean multiselect, PageBase parentPage) {
		super(id);
		this.type = defaultType;
		this.parentPage = parentPage;
		this.multiselect = multiselect;
		initLayout();
	}

	public ObjectListPanel(String id, Class<? extends O> defaultType, Collection<SelectorOptions<GetOperationOptions>> options,
						   boolean multiselect, PageBase parentPage, List<O> selectedObjectsList) {
		super(id);
		this.type = defaultType;
		this.parentPage = parentPage;
		this.options = options;
		this.multiselect = multiselect;
		this.selectedObjects = selectedObjectsList;
		initLayout();
	}

	public boolean isMultiselect() {
		return multiselect;
	}

	public int getSelectedObjectsCount(){
		List<O> selectedList = getSelectedObjects();
		return selectedList == null ? 0 : selectedList.size();
	}

	@SuppressWarnings("unchecked")
	public List<O> getSelectedObjects() {
		BaseSortableDataProvider<SelectableBean<O>> dataProvider = getDataProvider();
		if (dataProvider instanceof SelectableBeanObjectDataProvider) {
			return ((SelectableBeanObjectDataProvider<O>) dataProvider).getSelectedData();
		} else if (dataProvider instanceof ListDataProvider2) {
			return ((ListDataProvider2) dataProvider).getSelectedObjects();
		}
		return new ArrayList<>();
	}

	private void initLayout() {
		Form<O> mainForm = new Form<O>(ID_MAIN_FORM);
		add(mainForm);

		searchModel = initSearchModel();

		BoxedTablePanel<SelectableBean<O>> table = createTable();
		mainForm.add(table);

	}
	
	private LoadableModel<Search> initSearchModel(){
		return new LoadableModel<Search>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			public Search load() {
				String storageKey = getStorageKey();
				Search search = null;
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getPageStorage(storageKey);
					if (storage != null) {
						search = storage.getSearch();
					}
				}
				if (search == null) {
					search = createSearch();
				}
				return search;
			}
		};
	}
	
	protected Search createSearch() {
		return SearchFactory.createSearch(type, parentPage);
	}
	
	private BoxedTablePanel<SelectableBean<O>> createTable() {

		List<IColumn<SelectableBean<O>, String>> columns;
		if (isCustomColumnsListConfigured()){
			columns = initCustomColumns();
		} else {
			columns = initColumns();
		}

		BaseSortableDataProvider<SelectableBean<O>> provider = initProvider();
		
		
		BoxedTablePanel<SelectableBean<O>> table = new BoxedTablePanel<SelectableBean<O>>(ID_TABLE, provider,
				columns, tableId, tableId == null ? 10 : parentPage.getSessionStorage().getUserProfile().getPagingSize(tableId)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return initSearch(headerId);
			}

			@Override
			public String getAdditionalBoxCssClasses() {
				return ObjectListPanel.this.getAdditionalBoxCssClasses();
			}

			@Override
			protected WebMarkupContainer createButtonToolbar(String id) {
				WebMarkupContainer bar = ObjectListPanel.this.createTableButtonToolbar(id);

				return bar != null ? bar : super.createButtonToolbar(id);
			}

		};
		table.setOutputMarkupId(true);
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey); 
			if (storage != null) {
				table.setCurrentPage(storage.getPaging());
			}
		}

		return table;
	}
	
	protected List<IColumn<SelectableBean<O>, String>> initCustomColumns() {
		LOGGER.trace("Start to init custom columns for table of type {}", type);
		List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<IColumn<SelectableBean<O>, String>>();
		List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
		if (customColumns == null){
			return columns;
		}

		CheckBoxHeaderColumn<SelectableBean<O>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<O>>) createCheckboxColumn();
		if (checkboxColumn != null) {
			columns.add(checkboxColumn);
		}

		IColumn<SelectableBean<O>, String> iconColumn = ColumnUtils.createIconColumn(type);
		columns.add(iconColumn);

		columns.addAll(getCustomColumnsTransformed(customColumns));
		IColumn<SelectableBean<O>, String> actionsColumn = createActionsColumn();
		if (actionsColumn != null){
			columns.add(actionsColumn);
		}
		LOGGER.trace("Finished to init custom columns, created columns {}", columns);
		return columns;
	}

	protected List<IColumn<SelectableBean<O>, String>> getCustomColumnsTransformed(List<GuiObjectColumnType> customColumns){
		List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<IColumn<SelectableBean<O>, String>>();
		if (customColumns == null || customColumns.size() == 0){
			return columns;
		}
		IColumn<SelectableBean<O>, String> column;
		for (GuiObjectColumnType customColumn : customColumns){
			if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
				if (customColumns.indexOf(customColumn) == 0) {
					column = createNameColumn(customColumn.getDisplay() != null && customColumn.getDisplay().getLabel() != null ?
									Model.of(customColumn.getDisplay().getLabel()) : createStringResource(getItemDisplayName(customColumn)),
							customColumn.getPath().toString());
				} else {
					column = new PropertyColumn(customColumn.getDisplay() != null && customColumn.getDisplay().getLabel() != null ?
							Model.of(customColumn.getDisplay().getLabel()) : createStringResource(getItemDisplayName(customColumn)), null,
							SelectableBean.F_VALUE + "." + customColumn.getPath());
				}
				columns.add(column);
			}
		}
		return columns;
	}

	protected List<IColumn<SelectableBean<O>, String>> initColumns() {
		LOGGER.trace("Start to init columns for table of type {}", type);
		List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<IColumn<SelectableBean<O>, String>>();

		CheckBoxHeaderColumn<SelectableBean<O>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<O>>) createCheckboxColumn();
		if (checkboxColumn != null) {
			columns.add(checkboxColumn);
		}

		IColumn<SelectableBean<O>, String> iconColumn = ColumnUtils.createIconColumn(type);
		columns.add(iconColumn);

		IColumn<SelectableBean<O>, String> nameColumn = createNameColumn(null, null);
		columns.add(nameColumn);

		List<IColumn<SelectableBean<O>, String>> others = createColumns();
		columns.addAll(others);
		IColumn<SelectableBean<O>, String> actionsColumn = createActionsColumn();
		if (actionsColumn != null) {
			columns.add(createActionsColumn());
		}
		LOGGER.trace("Finished to init columns, created columns {}", columns);
		return columns;
	}

	protected BaseSortableDataProvider<SelectableBean<O>> initProvider() {
		Set<O> selectedObjectsSet = selectedObjects == null ? null : new HashSet<O>(selectedObjects);
		SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
				parentPage, type, selectedObjectsSet) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				String storageKey = getStorageKey();
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getPageStorage(storageKey);
					if (storage != null) {
						storage.setPaging(paging);
					}
				}
			}

			@Override
			public SelectableBean<O> createDataObjectWrapper(O obj) {
				SelectableBean<O> bean = super.createDataObjectWrapper(obj);
				List<InlineMenuItem> inlineMenu = createInlineMenu();
				if (inlineMenu != null) {
					bean.getMenuItems().addAll(inlineMenu);
				}
				return bean;
			}
			
			@Override
					protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
				List<ObjectOrdering> customOrdering =  createCustomOrdering(sortParam);		
				if (customOrdering != null) {
							return customOrdering;
						}
						return super.createObjectOrderings(sortParam);
					}
		};
		if (options == null){
			if (ResourceType.class.equals(type)) {
				options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
			}
		} else {
			if (ResourceType.class.equals(type)) {
				GetOperationOptions root = SelectorOptions.findRootOptions(options);
				root.setNoFetch(Boolean.TRUE);
			}
			provider.setOptions(options);
		}
		provider.setQuery(getQuery());
		
		return provider;
	}
	
	protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
		return null;
	}
	
	private SearchFormPanel initSearch(String headerId) {
		SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
				ObjectListPanel.this.searchPerformed(query, target);
			}

		};

		return searchPanel;
	}

	public String getAdditionalBoxCssClasses() {
		return addutionalBoxCssClasses;
	}

	public void setAdditionalBoxCssClasses(String boxCssClasses) {
		this.addutionalBoxCssClasses = boxCssClasses;
	}

	/**
	 * there's no way to do it properly...
	 */
	@Deprecated
	protected WebMarkupContainer createTableButtonToolbar(String id) {
		return null;
	}
	
	private String getStorageKey() {
		String storageKey =  WebComponentUtil.getStorageKeyForPage(parentPage.getClass());
		if (storageKey == null) {
			storageKey = WebComponentUtil.getStorageKeyForTableId(tableId);
		}
		
		return storageKey;
		
	}

	private PageStorage getPageStorage(String storageKey){
		PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
		if (storage == null) {
			storage = getSession().getSessionStorage().initPageStorage(storageKey);
		}
		return storage;
	}

	@SuppressWarnings("unchecked")
	protected BaseSortableDataProvider<SelectableBean<O>> getDataProvider() {
		BoxedTablePanel<SelectableBean<O>> table = getTable();
		BaseSortableDataProvider<SelectableBean<O>> provider = (BaseSortableDataProvider<SelectableBean<O>>) table
				.getDataTable().getDataProvider();
		return provider;

	}

	@SuppressWarnings("unchecked")
	protected BoxedTablePanel<SelectableBean<O>> getTable() {
		return (BoxedTablePanel<SelectableBean<O>>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}
	
		
	@SuppressWarnings("deprecation")
	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

		BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();

		// note: we ignore 'query' parameter, as the 'customQuery' already contains its content (MID-3271)
		ObjectQuery customQuery = getQuery();

//		if (customQuery == null){
//			customQuery = query;
//		} else {
//			if (query != null){
//				customQuery.addFilter(query.getFilter());
//			}
//		}

		provider.setQuery(customQuery);
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey);
			if (storage != null) {
				storage.setSearch(searchModel.getObject());
				storage.setPaging(null);
			}
		}

		Table table = getTable();
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	public void refreshTable(Class<O> newType, AjaxRequestTarget target) {
		BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();
		provider.setQuery(getQuery());
		if (newType != null && provider instanceof SelectableBeanObjectDataProvider) {
			((SelectableBeanObjectDataProvider<O>) provider).setType(newType);
		}

		BoxedTablePanel<SelectableBean<O>> table = getTable();

		((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
		if (newType != null && !this.type.equals(newType)) {
			this.type = newType;
			resetSearchModel();
			table.setCurrentPage(null);
		} else {
			saveSearchModel(getCurrentTablePaging());
		}

		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	public void resetSearchModel(){
		searchModel.reset();
	}

	private void saveSearchModel(ObjectPaging paging) {
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey);
			if (storage != null) {
				storage.setSearch(searchModel.getObject());
				storage.setPaging(paging);
			}
		}

	}

	public void clearCache() {
		BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();
		provider.clearCache();
		if (provider instanceof SelectableBeanObjectDataProvider) {
			((SelectableBeanObjectDataProvider<O>) provider).clearSelectedObjects();
		}
	}

	public ObjectQuery getQuery() {
		ObjectQuery customQuery = createContentQuery();

		return customQuery;
	}

	protected ObjectQuery createContentQuery() {
		Search search = searchModel.getObject();
		ObjectQuery query = search.createObjectQuery(parentPage.getPrismContext());
		query = addFilterToContentQuery(query);
		return query;
	}

	protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
		return query;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	protected abstract IColumn<SelectableBean<O>, String> createCheckboxColumn();

	protected abstract IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath);

	protected abstract List<IColumn<SelectableBean<O>, String>> createColumns();

	protected IColumn<SelectableBean<O>, String> createActionsColumn(){
		return null;
	}

	protected abstract List<InlineMenuItem> createInlineMenu();


	public void addPerformed(AjaxRequestTarget target, List<O> selected) {
		parentPage.hideMainPopup(target);
	}

	private List<GuiObjectColumnType> getGuiObjectColumnTypeList(){
		AdminGuiConfigurationType adminGuiConfig = parentPage.getPrincipal().getAdminGuiConfiguration();
		if (adminGuiConfig != null && adminGuiConfig.getObjectLists() != null &&
				adminGuiConfig.getObjectLists().getObjectList() != null){
			for (GuiObjectListType object : adminGuiConfig.getObjectLists().getObjectList()){
				if (object.getType() != null &&
						!type.getSimpleName().equals(object.getType().getLocalPart())){
					continue;
				}
				return object.getColumn();
			}
		}
		return null;
	}

	private boolean isCustomColumnsListConfigured(){
		return getGuiObjectColumnTypeList() != null;
	}

	private String getItemDisplayName(GuiObjectColumnType column){
		return parentPage.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(type).findItemDefinition(column.getPath().getItemPath()).getDisplayName();
	}

	public ObjectPaging getCurrentTablePaging(){
		String storageKey = getStorageKey();
		if (StringUtils.isEmpty(storageKey)){
			return null;
		}
		PageStorage storage = getPageStorage(storageKey);
		if (storage == null) {
			return null;
		}
		return storage.getPaging();
	}
}

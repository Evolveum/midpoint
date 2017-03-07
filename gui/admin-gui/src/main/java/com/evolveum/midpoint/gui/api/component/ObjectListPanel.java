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
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
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

	public boolean isMultiselect() {
		return multiselect;
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
		return SearchFactory.createSearch(type, parentPage.getPrismContext(),
				parentPage.getModelInteractionService());
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

		LOGGER.trace("Finished to init custom columns, created columns {}", columns);
		return columns;
	}

	protected List<IColumn<SelectableBean<O>, String>> getCustomColumnsProcessed(List<GuiObjectColumnType> customColumns){
		List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<IColumn<SelectableBean<O>, String>>();
		if (customColumns == null || customColumns.size() == 0){
			return columns;
		}
		GuiObjectColumnType firstColumn = null;
		List<GuiObjectColumnType> unorderedColumns = new ArrayList<>();
		for (GuiObjectColumnType customColumn : customColumns){
			if (firstColumn == null && StringUtils.isEmpty(customColumn.getPreviousColumn())){
				firstColumn = customColumn;
				customColumns.remove(customColumn);
			} else if (StringUtils.isEmpty(customColumn.getPreviousColumn())){
				unorderedColumns.add(customColumn);
				customColumns.remove(customColumn);
			}
		}
		if (firstColumn == null){
			if (unorderedColumns.size() > 0){
				firstColumn = unorderedColumns.get(0);
				unorderedColumns.remove(0);
			} else {
				firstColumn = customColumns.get(0);
				customColumns.remove(0);
			}
		}
		IColumn<SelectableBean<O>, String> column = new PropertyColumn(Model.of(firstColumn.getDisplay().getLabel()),
				null, SelectableBean.F_VALUE + "." + firstColumn.getPath());
		columns.add(column);

		GuiObjectColumnType previousColumn = firstColumn;
		while (customColumns.size() > 0){
			GuiObjectColumnType currentCustomColumn = null;
			for (GuiObjectColumnType customColumn : customColumns){
				if (customColumn.getPreviousColumn() != null &&
						customColumn.getPreviousColumn().equals(previousColumn.getName())){
					currentCustomColumn = customColumn;
					customColumns.remove(customColumn);
					break;
				}
			}
			if (currentCustomColumn != null) {
				column = new PropertyColumn(Model.of(currentCustomColumn.getDisplay().getLabel()), null,
						SelectableBean.F_VALUE + "." + currentCustomColumn.getPath());
				columns.add(column);
			}
		}
		for (GuiObjectColumnType customColumn : unorderedColumns){
			column = new PropertyColumn(Model.of(customColumn.getDisplay().getLabel()), null,
					SelectableBean.F_VALUE + "." + customColumn.getPath());
			columns.add(column);
			unorderedColumns.remove(customColumn);
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

		IColumn<SelectableBean<O>, String> nameColumn = createNameColumn();
		columns.add(nameColumn);

		List<IColumn<SelectableBean<O>, String>> others = createColumns();
		columns.addAll(others);
		LOGGER.trace("Finished to init columns, created columns {}", columns);
		return columns;
	}

	protected BaseSortableDataProvider<SelectableBean<O>> initProvider() {
		
		SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
				parentPage, type) {
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
	private BaseSortableDataProvider<SelectableBean<O>> getDataProvider() {
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

		if (newType != null && !this.type.equals(newType)) {
			this.type = newType;
			searchModel.reset();
		} else {
			saveSearchModel();
		}

		BoxedTablePanel<SelectableBean<O>> table = getTable();

		((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	

	private void saveSearchModel() {
		String storageKey = getStorageKey();
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getPageStorage(storageKey);
			if (storage != null) {
				storage.setSearch(searchModel.getObject());
				storage.setPaging(null);
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

	protected abstract IColumn<SelectableBean<O>, String> createNameColumn();

	protected abstract List<IColumn<SelectableBean<O>, String>> createColumns();

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
}

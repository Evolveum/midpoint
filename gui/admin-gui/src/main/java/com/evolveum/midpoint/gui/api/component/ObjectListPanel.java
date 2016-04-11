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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
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
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class ObjectListPanel<T extends ObjectType> extends BasePanel<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private static final String ID_SEARCH_FORM = "searchForm";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_BUTTON_CANCEL = "cancelButton";

	private static final String ID_TABLE = "table";

	private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);

	private Class<T> type;
	private PageBase parentPage;

	private LoadableModel<Search> searchModel;

	private BaseSortableDataProvider<SelectableBean<T>> provider;

	private Collection<SelectorOptions<GetOperationOptions>> options;

	private int pageSize = 10;

	private TableId tableId = TableId.TABLE_USERS;

	public Class<T> getType() {
		return type;
	}

	private static Map<Class, String> storageMap;

	static {
		storageMap = new HashMap<Class, String>();
		storageMap.put(UserType.class, SessionStorage.KEY_USERS);
		storageMap.put(ResourceType.class, SessionStorage.KEY_RESOURCES);
		storageMap.put(ReportType.class, SessionStorage.KEY_REPORTS);
		storageMap.put(RoleType.class, SessionStorage.KEY_ROLES);
		// storageMap.put(ObjectType.class, SessionStorage.KEY_CONFIGURATION);
		// storageMap.put(FocusType.class, SessionStorage.KEY_ROLE_MEMBERS);

	}

	public ObjectListPanel(String id, Class<T> type, Collection<SelectorOptions<GetOperationOptions>> options,
			PageBase parentPage) {
		super(id);
		this.type = type;
		this.parentPage = parentPage;
		this.options = options;
		initLayout();
	}

	public void setProvider(BaseSortableDataProvider<SelectableBean<T>> provider) {
		this.provider = provider;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setTableId(TableId tableId) {
		this.tableId = tableId;
	}


	public List<T> getSelectedObjects() {
		BaseSortableDataProvider<SelectableBean<T>> dataProvider = getDataProvider();
		if (dataProvider instanceof ObjectDataProvider2) {
			return ((ObjectDataProvider2) dataProvider).getSelectedData();
		} else if (dataProvider instanceof ListDataProvider2) {
			return ((ListDataProvider2) dataProvider).getSelectedObjects();
		}
		return new ArrayList<>();
	}

	private void initLayout() {
		Form<T> mainForm = new Form<T>(ID_MAIN_FORM);
		add(mainForm);

		searchModel = new LoadableModel<Search>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			public Search load() {
				String storageKey = storageMap.get(type);
				Search search = null;
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getSession().getSessionStorage().getPageStorageMap()
							.get(storageKey);
					if (storage != null) {
						search = storage.getSearch();
					}
				}
				if (search == null) {
					search = SearchFactory.createSearch(type, parentPage.getPrismContext(), true);
				}
				return search;
			}
		};

		BoxedTablePanel<SelectableBean<T>> table = createTable();
		mainForm.add(table);

		// saveSearch();
	}

	protected BaseSortableDataProvider<SelectableBean<T>> getProvider() {
		if (provider != null) {
			return provider;
		}
		ObjectDataProvider2<SelectableBean<T>, T> objProvider = new ObjectDataProvider2<SelectableBean<T>, T>(
				parentPage, type) {
			@Override
			protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
				String storageKey = storageMap.get(type);
				if (StringUtils.isNotEmpty(storageKey)) {
					PageStorage storage = getSession().getSessionStorage().getPageStorageMap()
							.get(storageKey);
					if (storage != null) {
						storage.setPaging(paging);
					}
				}
			}

			@Override
			public SelectableBean<T> createDataObjectWrapper(T obj) {
				SelectableBean<T> bean = super.createDataObjectWrapper(obj);
				List<InlineMenuItem> inlineMenu = createInlineMenu();
				if (inlineMenu != null){
					bean.getMenuItems().addAll(inlineMenu);
				}
				return bean;
			}
		};
		if (options != null) {
			objProvider.setOptions(options);
		}
		provider = objProvider;
		return provider;
	}

	private BoxedTablePanel<SelectableBean<T>> createTable() {
		List<IColumn<SelectableBean<T>, String>> columns = initColumns();
		provider = getProvider();
		provider.setQuery(getQuery());

		BoxedTablePanel<SelectableBean<T>> table = new BoxedTablePanel<SelectableBean<T>>(ID_TABLE, provider,
				columns, tableId, pageSize) {

			private static final long serialVersionUID = 1L;

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFormPanel(headerId, searchModel) {

					private static final long serialVersionUID = 1L;

					@Override
					protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
						ObjectListPanel.this.searchPerformed(query, target);
					}

				};
			}

		};
		table.setOutputMarkupId(true);
		String storageKey = storageMap.get(type);
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
			if (storage != null) {
				table.setCurrentPage(storage.getPaging());
			}
		}

		return table;
	}

	private BaseSortableDataProvider<SelectableBean<T>> getDataProvider() {
		BoxedTablePanel<SelectableBean<T>> table = getTable();
		BaseSortableDataProvider<SelectableBean<T>> provider = (BaseSortableDataProvider<SelectableBean<T>>) table
				.getDataTable().getDataProvider();
		return provider;

	}

	protected BoxedTablePanel<SelectableBean<T>> getTable() {
		return (BoxedTablePanel<SelectableBean<T>>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
		BaseSortableDataProvider<SelectableBean<T>> provider = getDataProvider();
		provider.setQuery(query);
		String storageKey = storageMap.get(type);
		if (StringUtils.isNotEmpty(storageKey)) {
			PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
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

	public void clearCache() {
		getDataProvider().clearCache();
	}

	public ObjectQuery getQuery() {
		ObjectQuery customQuery = createContentQuery();

		return customQuery;
	}

	protected ObjectQuery createContentQuery() {
		Search search = searchModel.getObject();
		ObjectQuery query = search.createObjectQuery(parentPage.getPrismContext());
		return query;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	protected void onCheckboxUpdate(AjaxRequestTarget target) {

	}

	protected abstract IColumn<SelectableBean<T>, String> createCheckboxColumn();

	protected abstract IColumn<SelectableBean<T>, String> createNameColumn();

	protected abstract List<IColumn<SelectableBean<T>, String>> createColumns();
	protected abstract List<InlineMenuItem> createInlineMenu();

	// protected abstract void saveSearch(ObjectPaging paging);

	protected List<IColumn<SelectableBean<T>, String>> initColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		CheckBoxHeaderColumn<SelectableBean<T>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<T>>) createCheckboxColumn();
		if (checkboxColumn != null) {
			columns.add(checkboxColumn);
		}

		IColumn<SelectableBean<T>, String> iconColumn = ColumnUtils.createIconColumn(type);
		columns.add(iconColumn);

		IColumn<SelectableBean<T>, String> nameColumn = createNameColumn();
		columns.add(nameColumn);

		List<IColumn<SelectableBean<T>, String>> others = createColumns();
		columns.addAll(others);

		return columns;
	}

	private void clearSearchPerformed(AjaxRequestTarget target) {
		BaseSortableDataProvider<SelectableBean<T>> provider = getDataProvider();
		provider.setQuery(null);

		target.add(getTable());
	}

	public void addPerformed(AjaxRequestTarget target, List<T> selected) {
		parentPage.hideMainPopup(target);
	}



}

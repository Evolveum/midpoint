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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchFormPanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectListPanel<T extends ObjectType> extends BasePanel<T> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// private static final String ID_SEARCH_FORM = "searchForm";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_BUTTON_CANCEL = "cancelButton";
	private static final String ID_BUTTON_ADD = "addButton";
	private static final String ID_TABLE = "table";
	

	private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);

	private Class type;
	private PageBase parentPage;

	private LoadableModel<Search> searchModel;

	private boolean multiSelect = true;
	private boolean editable = true;

	private List<ColumnTypeDto> columnDefinitions;

	
	
	private int pageSize = 10;

	private TableId tableId = TableId.TABLE_USERS;

	public ObjectListPanel(String id, Class type, PageBase parentPage) {
		super(id);
		this.type = type;
		this.parentPage = parentPage;
		
		initLayout();
	}

	public void setMultiSelect(boolean multiSelect) {
		this.multiSelect = multiSelect;
	}

	public boolean isMultiSelect() {
		return multiSelect;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setColumnDefinitions(List<ColumnTypeDto> columnDefinitions) {
		this.columnDefinitions = columnDefinitions;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setTableId(TableId tableId) {
		this.tableId = tableId;
	}

	
	

	private void initLayout() {
		Form mainForm = new Form(ID_MAIN_FORM);
		add(mainForm);
		
			
		searchModel = new LoadableModel<Search>(false) {

			@Override
			public Search load() {
				 
					Search search = SearchFactory.createSearch(type, parentPage.getPrismContext(), true);
			
				return search;
			}
		};

		BoxedTablePanel table = createTable();
		mainForm.add(table);

		AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
				createStringResource("userBrowserDialog.button.cancelButton")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				cancelPerformed(target);
			}
		};
		mainForm.add(cancelButton);

		AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
				createStringResource("userBrowserDialog.button.addButton")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				List<T> selected = ((ObjectDataProvider2) getDataProvider()).getSelectedData();
				addPerformed(target, selected);
			}
		};
		addButton.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return ObjectListPanel.this.isMultiSelect();
			}
		});
		mainForm.add(addButton);
	}

	private BoxedTablePanel createTable() {
		List<IColumn<SelectableBean<T>, String>> columns = initColumns();
		ObjectDataProvider2<SelectableBean<T>, T> provider = new ObjectDataProvider2<SelectableBean<T>, T>(
				parentPage, type);
		provider.setQuery(getQuery());
		BoxedTablePanel<SelectableBean<T>> table = new BoxedTablePanel<SelectableBean<T>>(ID_TABLE, provider,
				columns, tableId, pageSize) {

			@Override
			protected WebMarkupContainer createHeader(String headerId) {
				return new SearchFormPanel(headerId, searchModel) {

					@Override
					protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
						ObjectListPanel.this.searchPerformed(query, target);
					}
				};
			}

		};
		table.setOutputMarkupId(true);
		return table;
	}

	private ObjectDataProvider2<SelectableBean<T>, T> getDataProvider() {
		BoxedTablePanel<SelectableBean<T>> table = getTable();
		ObjectDataProvider2<SelectableBean<T>, T> provider = (ObjectDataProvider2<SelectableBean<T>, T>) table
				.getDataTable().getDataProvider();
		return provider;

	}

	private BoxedTablePanel<SelectableBean<T>> getTable() {
		return (BoxedTablePanel<SelectableBean<T>>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
		ObjectDataProvider2 provider = getDataProvider();
		provider.setQuery(query);

		// RolesStorage storage = getSessionStorage().getRoles();
		// storage.setRolesSearch(searchModel.getObject());
		// storage.setRolesPaging(null);

		Table table = getTable();
		table.setCurrentPage(null);
		target.add((Component) table);
		target.add(parentPage.getFeedbackPanel());

	}

	public ObjectQuery getQuery() {
		ObjectQuery customQuery = createContentQuery();

		return customQuery;
	}

	protected ObjectQuery createContentQuery() {
		return null;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
	}

	private List<IColumn<SelectableBean<T>, String>> initColumns() {
		List<IColumn<SelectableBean<T>, String>> columns = new ArrayList<IColumn<SelectableBean<T>, String>>();

		CheckBoxHeaderColumn checkboxColumn = new CheckBoxHeaderColumn();
		checkboxColumn.setCheckboxVisible(isMultiSelect());
		columns.add(checkboxColumn);

		String nameColumnName = SelectableBean.F_VALUE + ".name";
		if (isEditable()) {
			columns.add(new LinkColumn<SelectableBean<T>>(createStringResource("ObjectType.name"),
					nameColumnName, nameColumnName) {

				@Override
				public void onClick(AjaxRequestTarget target, IModel<SelectableBean<T>> rowModel) {
					T user = rowModel.getObject().getValue();
					objectDetailsPerformed(target, user);
				}

			});
		} else {
			columns.add(new PropertyColumn(createStringResource("userBrowserDialog.name"), nameColumnName,
					nameColumnName));
		}

		if (columnDefinitions != null && !columnDefinitions.isEmpty()) {
			((Collection) columns).addAll(ColumnUtils.createColumns(columnDefinitions));
		} else {
			columns.addAll(ColumnUtils.getDefaultColumns(type));
		}

		return columns;
	}

	private void clearSearchPerformed(AjaxRequestTarget target) {
		ObjectDataProvider2 provider = getDataProvider();
		provider.setQuery(null);

		target.add(getTable());
	}

	private void cancelPerformed(AjaxRequestTarget target) {
		parentPage.hideMainPopup(target);
	}

	public void addPerformed(AjaxRequestTarget target, List<T> selected) {
		parentPage.hideMainPopup(target);
	}

	public void objectDetailsPerformed(AjaxRequestTarget target, T user) {
		parentPage.hideMainPopup(target);
	}

}

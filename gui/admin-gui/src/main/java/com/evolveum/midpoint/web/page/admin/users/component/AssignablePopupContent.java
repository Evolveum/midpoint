/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgUnitSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class AssignablePopupContent extends BasePanel {

    private static final String ID_TABLE = "table";
    private static final String ID_ADD = "add";
    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_SEARCH_FORM = "searchForm";

    private static final Trace LOGGER = TraceManager.getTrace(AssignablePopupContent.class);

    private QName searchParameter = RoleType.F_NAME;
    private Class<? extends ObjectType> type = RoleType.class;
    private IModel<AssignmentSearchDto> searchModel;

    public AssignablePopupContent(String id) {
        super(id, null);

        searchModel = new LoadableModel<AssignmentSearchDto>() {

            @Override
            protected AssignmentSearchDto load() {
                return new AssignmentSearchDto();
            }
        };
    }

    @Override
    protected void initLayout() {

        Form searchForm = new Form(ID_SEARCH_FORM);
        searchForm.setOutputMarkupId(true);
        add(searchForm);

        TablePanel table = createTable();
        add(table);

        BasicSearchPanel<AssignmentSearchDto> basicSearch = new BasicSearchPanel<AssignmentSearchDto>(ID_BASIC_SEARCH) {

            @Override
            protected IModel<String> createSearchTextModel() {
                return new PropertyModel<>(searchModel, AssignmentSearchDto.F_SEARCH_TEXT);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                assignmentSearchPerformed(target);
            }

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                assignmentClearSearchPerformed(target);
            }
        };
        searchForm.add(basicSearch);

        AjaxButton addButton = new AjaxButton(ID_ADD,
                createStringResource("assignablePopupContent.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedObjects());
            }
        };
        add(addButton);
    }

    private TablePanel createTable() {
        List<IColumn> columns = createMultiSelectColumns();
        ObjectDataProvider provider = new ObjectDataProvider(getPageBase(), type);
        provider.setQuery(getProviderQuery());
        TablePanel table = new TablePanel(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);

        return table;
    }

    public ObjectQuery getProviderQuery(){
        return null;
    }

    private List<IColumn> createMultiSelectColumns() {
        List<IColumn> columns = new ArrayList<IColumn>();

        IColumn column = new CheckBoxHeaderColumn();
        columns.add(column);

        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.name"), "value.name"));
        if (OrgType.class.isAssignableFrom(type)) {
            columns.add(new PropertyColumn(createStringResource("assignablePopupContent.displayName"), "value.displayName.orig"));
        }
        columns.add(new PropertyColumn(createStringResource("assignablePopupContent.description"), "value.description"));

        return columns;
    }

    private TablePanel getTablePanel(){
        return (TablePanel) get(ID_TABLE);
    }

    private <T extends ObjectType> List<ObjectType> getSelectedObjects() {
        List<ObjectType> selected = new ArrayList<ObjectType>();

        TablePanel table = (TablePanel) get(ID_TABLE);
        ObjectDataProvider<SelectableBean<T>, T> provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
        for (SelectableBean<T> bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            selected.add(bean.getValue());
        }

        return selected;
    }

    public void setType(Class<? extends ObjectType> type) {
        Validate.notNull(type, "Class must not be null.");

        this.type = type;

        TablePanel table = (TablePanel) get(ID_TABLE);
        if (table != null) {
            ObjectDataProvider provider = (ObjectDataProvider) table.getDataTable().getDataProvider();
            provider.setType(type);

            //replace table with table with proper columns
            replace(createTable());
        }
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public QName getSearchParameter() {
        return searchParameter;
    }

    public void setSearchParameter(QName searchParameter) {
        Validate.notNull(searchParameter, "Search Parameter must not be null.");
        this.searchParameter = searchParameter;
    }

    protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {

    }

    private void assignmentSearchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createSearchQuery();
        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(query);

        target.add(get(ID_TABLE));
        target.add(panel);
    }

    private ObjectQuery createSearchQuery(){
        AssignmentSearchDto dto = searchModel.getObject();
        ObjectQuery query = null;

        if(StringUtils.isEmpty(dto.getText())){
            if(getProviderQuery() != null){
                return getProviderQuery();
            } else {
                return null;
            }
        }

        try{
            PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
            String normalized = normalizer.normalize(dto.getText());

            SubstringFilter substring = SubstringFilter.createSubstring(searchParameter, type, getPageBase().getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalized);

            if(getProviderQuery() != null){
                AndFilter and = AndFilter.createAnd(getProviderQuery().getFilter(), substring);
                query = ObjectQuery.createObjectQuery(and);
            } else {
                query = ObjectQuery.createObjectQuery(substring);
            }

        } catch (Exception e){
            error(getString("OrgUnitBrowser.message.queryError") + " " + e.getMessage());
            LoggingUtils.logException(LOGGER, "Couldn't create query filter.", e);
        }

        return query;
    }

    protected void assignmentClearSearchPerformed(AjaxRequestTarget target){
        searchModel.setObject(new AssignmentSearchDto());

        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider)table.getDataProvider();

        if(getProviderQuery() != null){
            provider.setQuery(getProviderQuery());
        } else {
            provider.setQuery(null);
        }

        target.add(panel);
    }
}

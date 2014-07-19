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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.resources.dto.ResourceDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

public class ResourcesPopup extends BasePanel {

    private static final String ID_BASIC_SEARCH = "basicSearch";
    private static final String ID_SEARCH_FORM = "searchForm";
    private static final String ID_TABLE = "table";
    private static final String ID_ADD = "add";

    private static final Trace LOGGER = TraceManager.getTrace(ResourcesPopup.class);

    private IModel<AssignmentSearchDto> searchModel;

    public ResourcesPopup(String id) {
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

        TablePanel resources = new TablePanel<>(ID_TABLE,
                getProvider(), initResourceColumns());
        resources.setOutputMarkupId(true);
        add(resources);

        AjaxButton addButton = new AjaxButton(ID_ADD,
                new StringResourceModel("resourcePopup.button.add", this, null)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedResources());
            }
        };
        add(addButton);
    }

    public SimpleUserResourceProvider getProvider(){
        return null;
    }

    private List<IColumn<ResourceDto, String>> initResourceColumns() {
        List<IColumn<ResourceDto, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<ResourceDto>();
        columns.add(column);

        columns.add(new PropertyColumn(new StringResourceModel("resourcePopup.name", this, null), "value.name"));

        return columns;
    }

    private List<ResourceType> getSelectedResources() {
        List<ResourceType> list = new ArrayList<>();

        TablePanel table = getTablePanel();
        SimpleUserResourceProvider provider = (SimpleUserResourceProvider) table.getDataTable().getDataProvider();
        for (SelectableBean<ResourceType> bean : provider.getAvailableData()) {
            if (!bean.isSelected()) {
                continue;
            }

            list.add(bean.getValue());
        }

        return list;
    }

    private TablePanel getTablePanel(){
        return (TablePanel) get(ID_TABLE);
    }

    protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {

    }

    private void assignmentSearchPerformed(AjaxRequestTarget target){
        ObjectQuery query = createSearchQuery();
        TablePanel panel = getTablePanel();
        DataTable table = panel.getDataTable();
        SimpleUserResourceProvider provider = (SimpleUserResourceProvider) table.getDataProvider();
        provider.setResourceProviderQuery(query);

        target.add(panel);
    }

    private ObjectQuery createSearchQuery(){
        AssignmentSearchDto dto = searchModel.getObject();
        ObjectQuery query = null;

        if(StringUtils.isEmpty(dto.getText())){
            return null;
        }

        try{
            PolyStringNormalizer normalizer = getPageBase().getPrismContext().getDefaultPolyStringNormalizer();
            String normalized = normalizer.normalize(dto.getText());

            SubstringFilter substring = SubstringFilter.createSubstring(ResourceType.F_NAME, ResourceType.class, getPageBase().getPrismContext(),
                    PolyStringNormMatchingRule.NAME, normalized);

            query = new ObjectQuery();
            query.setFilter(substring);

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
        SimpleUserResourceProvider provider = (SimpleUserResourceProvider)table.getDataProvider();
        provider.setResourceProviderQuery(null);

        target.add(panel);
    }


}

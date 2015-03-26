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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.org.OrgTabbedPannel;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTableDto;
import com.evolveum.midpoint.web.util.WebMiscUtil;
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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;

import java.util.ArrayList;
import java.util.List;

public abstract class AssignablePopupContent extends BasePanel {

//    private static final String ID_TABLE = "table";
    private static final String ID_ADD = "add";
//    private static final String ID_BASIC_SEARCH = "basicSearch";
//    private static final String ID_SEARCH_FORM = "searchForm";

    private static final Trace LOGGER = TraceManager.getTrace(AssignablePopupContent.class);
//
//    private QName searchParameter = RoleType.F_NAME;
    protected Class<? extends ObjectType> type = RoleType.class;
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

        Panel panel = createPopupContent();
        add(panel);
        
        AjaxButton addButton = new AjaxButton(ID_ADD,
                createStringResource("assignablePopupContent.button.add")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, getSelectedObjects());
            }
        };
        add(addButton);
    }


    /**
     *  Override to provide handle operation for partial error during provider iterator operation.
     * */
   
    protected List<IColumn> createMultiSelectColumns() {
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

    public Class<? extends ObjectType> getType() {
        return type;
    }
    
    public abstract void setType(Class<? extends ObjectType> type);
    
    protected abstract Panel createPopupContent();
   
    protected void handlePartialError(OperationResult result){}

    protected abstract Panel getTablePanel();

    protected abstract <T extends ObjectType> List<ObjectType> getSelectedObjects();
    
    public ObjectQuery getProviderQuery(){
        return null;
    }


    protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {

    }
   

}

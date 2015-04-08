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


import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

public abstract class AssignablePopupContent extends BasePanel {

    private static final String ID_ADD = "add";
    protected Class<? extends ObjectType> type = RoleType.class;
    protected IModel<AssignmentSearchDto> searchModel;

    public AssignablePopupContent(String id) {
        super(id, null);

        searchModel = new LoadableModel<AssignmentSearchDto>(false) {

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

    protected List<IColumn> createMultiSelectColumns() {
        List<IColumn> columns = new ArrayList<>();

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

    /**
     *  Override to set the type of the of assignable popup window
     * */
    public abstract void setType(Class<? extends ObjectType> type);

    /**
     *  Override to provide the content of such window - this should differ
     *  for each assignable type
     * */
    protected abstract Panel createPopupContent();

    /**
     *  Override to provide special handling for partial errors during
     *  object loading
     * */
    protected void handlePartialError(OperationResult result){}

    protected abstract Panel getTablePanel();

    protected abstract <T extends ObjectType> List<ObjectType> getSelectedObjects();
    
    public ObjectQuery getProviderQuery(){
        return null;
    }

    protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {}
}

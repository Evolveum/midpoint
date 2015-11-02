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


import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentSearchDto;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.PageReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAssignableSelectionPanel<T extends ObjectType> extends BasePanel {

    private static final String ID_ADD = "add";
    protected Class<T> type = (Class<T>) RoleType.class;
    protected IModel<AssignmentSearchDto> searchModel;

    protected Context context;

    // see ObjectSelectionPanel.Context description
    public static abstract class Context implements Serializable {

        protected PageReference callingPageReference;
        protected Component callingComponent;
        protected PageReference modalWindowPageReference;
        protected Class<? extends ObjectType> defaultType;

        abstract public Component getRealParent();

        public Context(Component callingComponent) {
            this.callingComponent = callingComponent;
        }

        public PageReference getCallingPageReference() {
            return callingPageReference;
        }

        public PageBase getCallingPage() {
            return (PageBase) callingComponent.getPage();
        }

        abstract protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected);

        public ObjectQuery getProviderQuery() {
            return null;
        }

        abstract protected void handlePartialError(OperationResult result);

        public PrismObject<UserType> getUserDefinition() {
            return null;
        }

        public PageReference getModalWindowPageReference() {
            return modalWindowPageReference;
        }

        public void setModalWindowPageReference(PageReference modalWindowPageReference) {
            this.modalWindowPageReference = modalWindowPageReference;
        }

        public void setType(Class<? extends ObjectType> type) {
            defaultType = type;
            if (modalWindowPageReference != null) {
                Page page = modalWindowPageReference.getPage();
                if (page != null) {
                    AbstractAssignableSelectionPanel panel = (AbstractAssignableSelectionPanel) page.get(AssignableSelectionPage.ID_ASSIGNABLE_SELECTION_PANEL);
                    if (panel != null) {
                        panel.setType(type);
                    }
                }
            }
        }

        public Class<? extends ObjectType> getDefaultType() {
            return defaultType;
        }

        public String getSubmitKey() {
            return "assignablePopupContent.button.assign";
        }
    }

    public AbstractAssignableSelectionPanel(String id, Context context) {
        super(id, null);
        this.context = context;

        searchModel = new LoadableModel<AssignmentSearchDto>(false) {

            @Override
            protected AssignmentSearchDto load() {
                return new AssignmentSearchDto();
            }
        };
    }
    
    @Override
    protected void initLayout() {

    	createPopupContent();
      
        AjaxButton addButton = new AjaxButton(ID_ADD, createStringResource(context.getSubmitKey())) {

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

    public Class<T> getType() {
        return type;
    }

    /**
     *  Override to set the type of the of assignable popup window
     * */
    public abstract void setType(Class<T> type);

    /**
     *  Override to provide the content of such window - this should differ
     *  for each assignable type
     * */
    protected abstract Panel createPopupContent();

    /**
     *  Override to provide special handling for partial errors during
     *  object loading
     * */
    protected void handlePartialError(OperationResult result) {
        context.handlePartialError(result);
    }

    protected abstract Panel getTablePanel();

    protected abstract <T extends ObjectType> List<T> getSelectedObjects();
    
    public ObjectQuery getProviderQuery(){
        return context.getProviderQuery();
    }

    protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
        context.addPerformed(target, selected);
    }
}

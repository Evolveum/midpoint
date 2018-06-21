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
package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class OrgUnitAddDeletePopup extends ModalWindow{

    public enum ActionState{
        DELETE,
        ADD
    }

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTON_ACTION = "actionButton";

    private static final String DEFAULT_SORTABLE_PROPERTY = null;

    private boolean initialized;

    private ActionState state = ActionState.ADD;

    public OrgUnitAddDeletePopup(String id){
        super(id);

        setTitle(createStringResource("orgUnitAddDeletePopup.title"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(OrgUnitAddDeletePopup.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(500);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    @Override
    protected void onBeforeRender(){
        super.onBeforeRender();

        if(initialized){
            return;
        }

        initLayout((WebMarkupContainer) get(getContentId()));
        initialized = true;
    }

    public void initLayout(WebMarkupContainer content){
        List<IColumn<SelectableBean<ObjectType>, String>> columns = initColumns();

        ObjectDataProvider provider = new ObjectDataProvider(getPageBase(), OrgType.class);
        provider.setQuery(getDataProviderQuery());

        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        content.add(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("orgUnitAddDeletePopup.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        content.add(cancelButton);

        AjaxButton actionButton = new AjaxButton(ID_BUTTON_ACTION,
                createActionButtonStringResource()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                actionPerformed(target);
            }
        };
        actionButton.add(new AttributeAppender("class", getActionButtonClass()));
        content.add(actionButton);

    }

    private List<IColumn<SelectableBean<ObjectType>, String>> initColumns(){
        List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

        IColumn column = new CheckBoxHeaderColumn<OrgType>();
        columns.add(column);

        column = new LinkColumn<SelectableBean<OrgType>>(createStringResource("orgUnitAddDeletePopup.column.name"), getSortableProperty(), "value.name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<OrgType>> rowModel){
                OrgType org = rowModel.getObject().getValue();
                chooseOperationPerformed(target, org);
            }

        };
        columns.add(column);

        return columns;
    }

    private StringResourceModel createActionButtonStringResource(){
        if(state == ActionState.ADD){
            return createStringResource("orgUnitAddDeletePopup.button.add");
        } else {
            return createStringResource("orgUnitAddDeletePopup.button.remove");
        }
    }

    private String getActionButtonClass(){
        if(state == ActionState.ADD){
            return "btn btn-primary btn-sm";
        } else {
            return "btn btn-danger btn-sm";
        }
    }

    public ActionState getState() {
        return state;
    }

    public void setState(ActionState state, AjaxRequestTarget target) {
        this.state = state;
        AjaxButton actionButton = (AjaxButton) getContent().get(ID_BUTTON_ACTION);
        actionButton.setModel(createActionButtonStringResource());
        actionButton.add(new AttributeAppender("class", getActionButtonClass()));
        target.add(actionButton);

        TablePanel panel = getTable();
        DataTable table = panel.getDataTable();
        ObjectDataProvider provider = (ObjectDataProvider) table.getDataProvider();
        provider.setQuery(getDataProviderQuery());

        target.add(panel);
    }

    private TablePanel getTable(){
        return (TablePanel) getContent().get(ID_TABLE);
    }

    private ObjectQuery getDataProviderQuery(){
        if(state == ActionState.ADD){
            return getAddProviderQuery();
        } else {
            return getRemoveProviderQuery();
        }
    }

    public ObjectQuery getAddProviderQuery(){
        return null;
    }

    public ObjectQuery getRemoveProviderQuery(){
        return null;
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
//        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    public String getSortableProperty(){
        return DEFAULT_SORTABLE_PROPERTY;
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }

    private void actionPerformed(AjaxRequestTarget target){
        if(state == ActionState.ADD){
            addPerformed(target, null);
        } else {
            removePerformed(target, null);
        }
    }

    private void chooseOperationPerformed(AjaxRequestTarget target, OrgType org){
        if(state == ActionState.ADD){
            addPerformed(target, org);
        } else {
            removePerformed(target, org);
        }
    }

    public void addPerformed(AjaxRequestTarget target, OrgType selected){}

    public void removePerformed(AjaxRequestTarget target, OrgType selected){}
}

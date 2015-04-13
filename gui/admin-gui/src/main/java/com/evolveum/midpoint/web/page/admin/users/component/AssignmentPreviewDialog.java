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

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class AssignmentPreviewDialog extends ModalWindow{

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private boolean initialized;
    private IModel<List<? extends ObjectType>> data;

    public AssignmentPreviewDialog(String id, final List<? extends ObjectType> data){
        super(id);

        this.data = new LoadableModel<List<? extends ObjectType>>(false) {

            @Override
            protected List<? extends ObjectType> load() {
                return data == null ? new ArrayList<ObjectType>() : data;
            }
        };

        setTitle(createStringResource("AssignmentPreviewDialog.label"));
        showUnloadConfirmation(false);
        setCssClassName(ModalWindow.CSS_CLASS_GRAY);
        setCookieName(AssignmentPreviewDialog.class.getSimpleName() + ((int) (Math.random() * 100)));
        setInitialWidth(500);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    public void updateData(AjaxRequestTarget target, List<? extends ObjectType> newData){
        data.setObject(newData);
        target.add(get(getContentId() + ":" + ID_TABLE));
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
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
        List<IColumn<RoleType, String>> columns = initColumns();
        ListDataProvider provider = new ListDataProvider(getPageBase(), data);

        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        content.add(table);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("AssignmentPreviewDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                cancelPerformed(ajaxRequestTarget);
            }
        };
        content.add(cancelButton);
    }

    private List<IColumn<RoleType, String>> initColumns(){
        List<IColumn<RoleType, String>> columns = new ArrayList<>();

        IColumn column = new LinkColumn<RoleType>(createStringResource("AssignmentPreviewDialog.column.name"), "name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<RoleType> rowModel){
                RoleType role = rowModel.getObject();
                chooseOperationPerformed(target, role);
            }

        };
        columns.add(column);

        column = new PropertyColumn<RoleType, String>(createStringResource("AssignmentPreviewDialog.column.description"), "description");
        columns.add(column);

        return columns;
    }

    private void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){
//        TODO
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }
}

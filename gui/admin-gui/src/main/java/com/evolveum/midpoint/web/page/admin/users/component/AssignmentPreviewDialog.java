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
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class AssignmentPreviewDialog extends ModalWindow{

    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    private boolean initialized;
    private List<String> directAssignments;
    private IModel<List<? extends ObjectType>> data;

    public AssignmentPreviewDialog(String id, final List<? extends ObjectType> data, List<String> directAssignments){
        super(id);

        this.directAssignments = directAssignments;
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
        setInitialWidth(650);
        setInitialHeight(500);
        setWidthUnit("px");

        WebMarkupContainer content = new WebMarkupContainer(getContentId());
        setContent(content);
    }

    public void updateData(AjaxRequestTarget target, List<? extends ObjectType> newData, List<String> directAssignments){
        this.directAssignments = directAssignments;
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
        List<IColumn<ObjectType, String>> columns = initColumns();
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

    private List<IColumn<ObjectType, String>> initColumns(){
        List<IColumn<ObjectType, String>> columns = new ArrayList<>();

        columns.add(new LinkColumn<ObjectType>(createStringResource("AssignmentPreviewDialog.column.name"), "name"){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<ObjectType> rowModel){
                ObjectType role = rowModel.getObject();
                chooseOperationPerformed(target, role);
            }
        });

        columns.add(new IconColumn<ObjectType>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<ObjectType> rowModel) {
                ObjectType object = rowModel.getObject();
                ObjectTypeGuiDescriptor guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(object.getClass());

                String icon = guiDescriptor != null ? guiDescriptor.getIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;

                return new Model<>(icon);
            }
        });

        columns.add(new AbstractColumn<ObjectType, String>(createStringResource("Type")) {

            @Override
            public void populateItem(Item<ICellPopulator<ObjectType>> cellItem, String componentId, final IModel<ObjectType> rowModel) {
                cellItem.add(new Label(componentId, new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return directAssignments.contains(rowModel.getObject().getOid()) ?
                                createStringResource("AssignmentPreviewDialog.type.direct").getString() :
                                createStringResource("AssignmentPreviewDialog.type.indirect").getObject();
                    }
                }));
            }
        });

        columns.add(new PropertyColumn<ObjectType, String>(createStringResource("AssignmentPreviewDialog.column.description"), "description"));

        return columns;
    }

    private void chooseOperationPerformed(AjaxRequestTarget target, ObjectType object){
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, object.getOid());

        if(object.getClass().equals(RoleType.class)){
            setResponsePage(PageRole.class, parameters);
        } else if(object.getClass().equals(ResourceType.class)){
            setResponsePage(PageResourceWizard.class, parameters);
        } else if(object.getClass().equals(OrgType.class)){
            setResponsePage(PageOrgUnit.class, parameters);
        }
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        close(target);
    }
}

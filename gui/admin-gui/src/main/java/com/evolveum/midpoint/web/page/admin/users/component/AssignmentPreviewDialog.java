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

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.ObjectBrowserPanel;
import com.evolveum.midpoint.gui.api.component.PopupObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class AssignmentPreviewDialog extends BasePanel implements Popupable {

    private static final String ID_CONTENT = "panel";
    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTON_ADD = "addButton";

    private boolean initialized;
    private List<String> directAssignments;
    private IModel<List<AssignmentsPreviewDto>> data;
    private PageBase pageBase;
    private boolean multiselect;

    public AssignmentPreviewDialog(String id, final List<AssignmentsPreviewDto> data, List<String> directAssignments,
                                   PageBase pageBase) {
        this(id, data, directAssignments, pageBase, false);
    }
    public AssignmentPreviewDialog(String id, final List<AssignmentsPreviewDto> data, List<String> directAssignments,
                                   PageBase pageBase, boolean multiselect){
        super(id);

        this.directAssignments = directAssignments;
        this.pageBase = pageBase;
        this.multiselect = multiselect;
        this.data = new LoadableModel<List<AssignmentsPreviewDto>>(false) {

            @Override
            protected List<AssignmentsPreviewDto> load() {
                return data == null ? new ArrayList<AssignmentsPreviewDto>() : data;
            }
        };
        initLayout();
    }
    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    public void initLayout(){
        List<IColumn<AssignmentsPreviewDto, String>> columns = initColumns();
        ListDataProvider provider = new ListDataProvider(pageBase, data);

        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        content.setOutputMarkupId(true);
        add(content);

        TablePanel table = new TablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        content.add(table);

        AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
                createStringResource("userBrowserDialog.button.selectButton")) {

            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                List<AssignmentsPreviewDto> previewDtos = data.getObject();
                List<AssignmentsPreviewDto> selectedDtos = new ArrayList<>();
                for (AssignmentsPreviewDto dto : previewDtos){
                    if (dto.isSelected()){
                        selectedDtos.add(dto);
                    }
                }
                    AssignmentPreviewDialog.this.addButtonClicked(target, selectedDtos);
            }
        };

        addButton.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return multiselect;
            }
        });

        content.add(addButton);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("AssignmentPreviewDialog.button.cancel")) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                ((PageBase)getPage()).hideMainPopup(ajaxRequestTarget);
            }
        };
        content.add(cancelButton);
    }

    private List<IColumn<AssignmentsPreviewDto, String>> initColumns() {
        List<IColumn<AssignmentsPreviewDto, String>> columns = new ArrayList<>();
        if (multiselect) {
            columns.add(new CheckBoxHeaderColumn<AssignmentsPreviewDto>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdateRow(AjaxRequestTarget target, DataTable table, IModel<AssignmentsPreviewDto> rowModel) {
                    super.onUpdateRow(target, table, rowModel);
                };

                @Override
                protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                    super.onUpdateHeader(target, selected, table);
                }
            });

        }
        columns.add(new LinkColumn<AssignmentsPreviewDto>(createStringResource("AssignmentPreviewDialog.column.name"), AssignmentsPreviewDto.F_TARGET_NAME){

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AssignmentsPreviewDto> rowModel){
                AssignmentsPreviewDto dto = rowModel.getObject();
                chooseOperationPerformed(dto.getTargetOid(), dto.getTargetClass());
            }

            @Override
            public boolean isEnabled(IModel<AssignmentsPreviewDto> rowModel) {
                if (multiselect){
                    return false;
                }
                Class targetClass = rowModel.getObject().getTargetClass();
                String authorizationAction = "";
                if (targetClass.getSimpleName().equals("OrgType")){
                    authorizationAction = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL;
                } else if (targetClass.getSimpleName().equals("RoleType")){
                    authorizationAction = AuthorizationConstants.AUTZ_UI_ROLE_URL;
                } else if (targetClass.getSimpleName().equals("ServiceType")){
                    authorizationAction = AuthorizationConstants.AUTZ_UI_SERVICE_URL;
                } else if (targetClass.getSimpleName().equals("ResourceType")){
                    authorizationAction = AuthorizationConstants.AUTZ_UI_RESOURCE_URL;
                }
                if (WebComponentUtil.isAuthorized(authorizationAction)) {
                    return true;
                }

                return false;
            }
        });

        columns.add(new IconColumn<AssignmentsPreviewDto>(createStringResource("")) {

            @Override
            protected IModel<String> createIconModel(IModel<AssignmentsPreviewDto> rowModel) {
                ObjectTypeGuiDescriptor guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(rowModel.getObject().getTargetClass());
                String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                return new Model<>(icon);
            }
        });

        if (!isDelegationPreview()) {
            columns.add(new AbstractColumn<AssignmentsPreviewDto, String>(createStringResource("Type")) {

                @Override
                public void populateItem(Item<ICellPopulator<AssignmentsPreviewDto>> cellItem, String componentId, final IModel<AssignmentsPreviewDto> rowModel) {
                    cellItem.add(new Label(componentId, new AbstractReadOnlyModel<String>() {

                        @Override
                        public String getObject() {
                            return rowModel.getObject().isDirect() ?
                                    createStringResource("AssignmentPreviewDialog.type.direct").getString() :
                                    createStringResource("AssignmentPreviewDialog.type.indirect").getString();
                        }
                    }));
                }
            });
        }

        columns.add(new PropertyColumn<AssignmentsPreviewDto, String>(
                createStringResource("AssignmentPreviewDialog.column.description"), AssignmentsPreviewDto.F_TARGET_DESCRIPTION));

        columns.add(new PropertyColumn<AssignmentsPreviewDto, String>(
                createStringResource("AssignmentPreviewDialog.column.tenant"), AssignmentsPreviewDto.F_TENANT_NAME));

        columns.add(new PropertyColumn<AssignmentsPreviewDto, String>(
                createStringResource("AssignmentPreviewDialog.column.orgRef"), AssignmentsPreviewDto.F_ORG_REF_NAME));

        if (!isDelegationPreview()) {
            columns.add(new PropertyColumn<AssignmentsPreviewDto, String>(
                    createStringResource("AssignmentPreviewDialog.column.kind"), AssignmentsPreviewDto.F_KIND));

            columns.add(new PropertyColumn<AssignmentsPreviewDto, String>(
                    createStringResource("AssignmentPreviewDialog.column.intent"), AssignmentsPreviewDto.F_INTENT));
        }
        return columns;
    }

    private void chooseOperationPerformed(String oid, Class clazz){
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

        PageBase page = getPageBase();

        if(clazz.equals(RoleType.class)){
            page.navigateToNext(PageRole.class, parameters);
        } else if(clazz.equals(ResourceType.class)){
            page.navigateToNext(PageResourceWizard.class, parameters);
        } else if(clazz.equals(OrgType.class)){
            page.navigateToNext(PageOrgUnit.class, parameters);
        }
    }

    protected boolean isDelegationPreview(){
        return false;
    }

    @Override
    public int getWidth() {
        return 1100;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("AssignmentPreviewDialog.label");
    }

    @Override
    public Component getComponent() {
        return this;
    }

    protected void addButtonClicked(AjaxRequestTarget target, List<AssignmentsPreviewDto> dtoList){
    }
}

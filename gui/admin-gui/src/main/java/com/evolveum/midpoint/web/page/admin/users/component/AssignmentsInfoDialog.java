/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.org.PageOrg;
import com.evolveum.midpoint.gui.impl.page.admin.resource.PageResource;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.GenericColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Abstract superclass for dialogs that display a list of assignments.
 * Configurable by overriding 'configuration methods'.
 *
 * @author shood
 */
public abstract class AssignmentsInfoDialog extends BasePanel<List<AssignmentInfoDto>> implements Popupable {

    private static final String ID_CONTENT = "panel";
    private static final String ID_TABLE = "table";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTON_ADD = "addButton";

    AssignmentsInfoDialog(String id, final List<AssignmentInfoDto> data, PageBase pageBase) {
        super(id, () -> MiscUtil.emptyIfNull(data));

        initLayout(pageBase);
    }

    public void initLayout(PageBase pageBase) {
        List<IColumn<AssignmentInfoDto, String>> columns = initColumns();
        ListDataProvider<AssignmentInfoDto> provider = new ListDataProvider<>(pageBase, getModel());

        WebMarkupContainer content = new WebMarkupContainer(ID_CONTENT);
        content.setOutputMarkupId(true);
        add(content);

        BoxedTablePanel<AssignmentInfoDto> table = new BoxedTablePanel<>(ID_TABLE, provider, columns);
        table.setOutputMarkupId(true);
        content.add(table);

        AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
                createStringResource("userBrowserDialog.button.selectButton")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                List<AssignmentInfoDto> allAssignmentInfoDtos = AssignmentsInfoDialog.this.getModelObject();
                List<AssignmentInfoDto> selectedDtos = allAssignmentInfoDtos.stream().filter(AssignmentInfoDto::isSelected).collect(Collectors.toList());
                AssignmentsInfoDialog.this.addButtonClicked(target, selectedDtos);
            }
        };
        addButton.setVisible(enableMultiSelect());
        content.add(addButton);

        AjaxButton cancelButton = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("AssignmentPreviewDialog.button.cancel")) {
            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                getPageBase().hideMainPopup(ajaxRequestTarget);
            }
        };
        cancelButton.add(new VisibleBehaviour(() -> showCancelButton()));
        content.add(cancelButton);
    }

    private List<IColumn<AssignmentInfoDto, String>> initColumns() {
        List<IColumn<AssignmentInfoDto, String>> columns = new ArrayList<>();
        if (enableMultiSelect()) {
            columns.add(new CheckBoxHeaderColumn<>());
        }
        columns.add(new AjaxLinkColumn<>(createStringResource("AssignmentPreviewDialog.column.name"), AssignmentInfoDto.F_TARGET_NAME) {

            @Override
            public void onClick(AjaxRequestTarget target, IModel<AssignmentInfoDto> rowModel) {
                AssignmentInfoDto dto = rowModel.getObject();
                chooseOperationPerformed(dto.getTargetOid(), dto.getTargetClass());
            }

            @Override
            public boolean isEnabled(IModel<AssignmentInfoDto> rowModel) {
                if (enableMultiSelect()) {
                    return false;
                }
                Class<?> targetClass = rowModel.getObject().getTargetClass();
                String authorizationAction = WebComponentUtil.getAuthorizationActionForTargetClass(targetClass);
                return authorizationAction != null && WebComponentUtil.isAuthorized(authorizationAction);
            }
        });

        columns.add(new IconColumn<>(createStringResource("")) {
            @Override
            protected DisplayType getIconDisplayType(IModel<AssignmentInfoDto> rowModel) {
                ObjectTypeGuiDescriptor guiDescriptor = ObjectTypeGuiDescriptor.getDescriptor(rowModel.getObject().getTargetClass());
                String icon = guiDescriptor != null ? guiDescriptor.getBlackIcon() : ObjectTypeGuiDescriptor.ERROR_ICON;
                return GuiDisplayTypeUtil.createDisplayType(icon);
            }
        });

        if (showDirectIndirectColumn()) {
            columns.add(new AbstractColumn<>(createStringResource("Type")) {
                @Override
                public void populateItem(Item<ICellPopulator<AssignmentInfoDto>> cellItem, String componentId, final IModel<AssignmentInfoDto> rowModel) {
                    cellItem.add(new Label(componentId,
                            (IModel<String>) () -> rowModel.getObject().isDirect()
                                    ? createStringResource("AssignmentPreviewDialog.type.direct").getString()
                                    : createStringResource("AssignmentPreviewDialog.type.indirect").getString()));
                    ObjectType assignmentParent = rowModel.getObject().getAssignmentParent();
                    if (assignmentParent != null) {
                        cellItem.add(AttributeModifier.replace("title", createStringResource("AssignmentPreviewDialog.tooltip.indirect.parent").getString() + ": " + assignmentParent.getName()));
                    }
                }
            });
        }

        columns.add(new PropertyColumn<>(createStringResource("AssignmentPreviewDialog.column.description"), AssignmentInfoDto.F_TARGET_DESCRIPTION));
        columns.add(new PropertyColumn<>(createStringResource("AssignmentPreviewDialog.column.tenant"), AssignmentInfoDto.F_TENANT_NAME));
        columns.add(new PropertyColumn<>(createStringResource("AssignmentPreviewDialog.column.orgRef"), AssignmentInfoDto.F_ORG_REF_NAME));

        if (showKindAndIntentColumns()) {
            columns.add(new PropertyColumn<>(createStringResource("AssignmentPreviewDialog.column.kind"), AssignmentInfoDto.F_KIND));
            columns.add(new PropertyColumn<>(createStringResource("AssignmentPreviewDialog.column.intent"), AssignmentInfoDto.F_INTENT));
        }
        if (showRelationColumn()) {
            columns.add(new GenericColumn<>(createStringResource("AssignmentPreviewDialog.column.relation"),
                    infoModel -> infoModel.getObject().getRelationDisplayNameModel(this)));
        }
        return columns;
    }

    private void chooseOperationPerformed(String oid, Class<?> clazz) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

        PageBase page = getPageBase();

        if (clazz.equals(RoleType.class)) {
            page.navigateToNext(PageRole.class, parameters);
        } else if (clazz.equals(ResourceType.class)) {
            page.navigateToNext(PageResource.class, parameters);
        } else if (clazz.equals(OrgType.class)) {
            page.navigateToNext(PageOrg.class, parameters);
        }
    }

    protected abstract boolean enableMultiSelect();
    protected abstract boolean showDirectIndirectColumn();
    protected abstract boolean showKindAndIntentColumns();
    protected abstract boolean showRelationColumn();
    protected abstract boolean showCancelButton();

    @Override
    public int getWidth() {
        return 1100;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("AssignmentPreviewDialog.label");
    }

    protected void addButtonClicked(AjaxRequestTarget target, List<AssignmentInfoDto> dtoList) {
    }
}

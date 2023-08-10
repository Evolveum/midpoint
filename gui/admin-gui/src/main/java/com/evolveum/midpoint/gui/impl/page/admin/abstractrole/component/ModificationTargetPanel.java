/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.AbstractRoleDetailsModel;
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.prism.delta.ObjectDelta;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;

import com.evolveum.midpoint.web.component.model.delta.DeltaDto;
import com.evolveum.midpoint.web.component.prism.show.VisualizationDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.BusinessRoleApplicationDto;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by tchrapovic.
 */
//@PanelType(name = "modificationTarget")
//@PanelInstance(identifier = "modificationTarget",
//        applicableForType = AbstractRoleType.class,
//        display = @PanelDisplay(label = "pageAdminFocus.modificationTarget", icon = GuiStyleConstants.CLASS_GROUP_ICON, order = 60))
public class ModificationTargetPanel<AR extends AbstractRoleType> extends AbstractObjectMainPanel<AR, AbstractRoleDetailsModel<AR>> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MODIFICATION_TARGET_CONTAINER = "modificationTargetContainer";
    private static final String ID_MODIFICATION_TARGET_PANEL = "modificationTargetPanel";

    public ModificationTargetPanel(String id, AbstractRoleDetailsModel<AR> focusWrapperModel, ContainerPanelConfigurationType config) {
        super(id, focusWrapperModel, config);
    }

    protected void initLayout() {
        WebMarkupContainer delegations = new WebMarkupContainer(ID_MODIFICATION_TARGET_CONTAINER);
        delegations.setOutputMarkupId(true);
        add(delegations);
        List<AssignmentType> inducement;
        try {
            inducement = getObjectDetailsModels().getObjectWrapper().getObjectApplyDelta().asObjectable().getInducement();
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        List<BusinessRoleApplicationDto> patternDeltas = getObjectDetailsModels().getPatternDeltas();

        for (BusinessRoleApplicationDto value : patternDeltas) {
            value.updateValue(inducement, (PageBase) getPage());
        }

        RoleMiningProvider<BusinessRoleApplicationDto> provider = new RoleMiningProvider<>(
                this, new ListModel<>(patternDeltas) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<BusinessRoleApplicationDto> object) {
                super.setObject(patternDeltas);
            }
        }, false);

        BoxedTablePanel<BusinessRoleApplicationDto> table = generateTable(provider);

        delegations.add(table);
    }

    private BoxedTablePanel<BusinessRoleApplicationDto> getTable() {
        return (BoxedTablePanel<BusinessRoleApplicationDto>) get(((PageBase) getPage()).createComponentPath(
                ID_MODIFICATION_TARGET_CONTAINER, ID_MODIFICATION_TARGET_PANEL));
    }

    private BoxedTablePanel<BusinessRoleApplicationDto> generateTable(RoleMiningProvider<BusinessRoleApplicationDto> provider) {

        BoxedTablePanel<BusinessRoleApplicationDto> table = new BoxedTablePanel<>(
                ID_MODIFICATION_TARGET_PANEL, provider, initColumns(provider));
        table.setOutputMarkupId(true);

        return table;
    }

    private List<IColumn<BusinessRoleApplicationDto, String>> initColumns(RoleMiningProvider<BusinessRoleApplicationDto> provider) {

        List<IColumn<BusinessRoleApplicationDto, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> cellItem, String componentId, IModel<BusinessRoleApplicationDto> rowModel) {
                changeBackgroundColorCell(cellItem, rowModel);
                super.populateItem(cellItem, componentId, rowModel);
            }

            @Override
            protected DisplayType getIconDisplayType(IModel<BusinessRoleApplicationDto> rowModel) {
                return GuiDisplayTypeUtil
                        .createDisplayType(WebComponentUtil.createDefaultBlackIcon(UserType.COMPLEX_TYPE));
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("ObjectType.name")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {

                AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(rowModel.getObject().getPrismObjectUser().getName())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, rowModel.getObject().getPrismObjectUser().getOid());

                        ((PageBase) getPage()).navigateToNext(PageRole.class, parameters);
                    }
                };
                ajaxLinkPanel.setOutputMarkupId(true);
                item.add(ajaxLinkPanel);
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Status")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {

                String include = "EXCLUDE";
                if (rowModel.getObject().isInclude()) {
                    include = "INCLUDE";
                }
                item.add(new Label(componentId, include));
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Added Assignment")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getAssignedCount()));
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Replaced Assignment")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getUnassignedCount()));
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Current Assignment count")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {
                item.add(new Label(componentId, rowModel.getObject().getPrismObjectUser().asObjectable().getAssignment().size()));
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(createStringResource("Final Assignment count")) {

            @Override
            public String getSortProperty() {
                return UserType.F_ACTIVATION.getLocalPart();
            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> item, String componentId,
                    IModel<BusinessRoleApplicationDto> rowModel) {
                int assignmentCount = rowModel.getObject().getPrismObjectUser().asObjectable().getAssignment().size();
                int reduction = rowModel.getObject().getUnassignedCount();

                item.add(new Label(componentId, (assignmentCount - reduction + 1)));
                changeBackgroundColorCell(item, rowModel);
            }

        });

        columns.add(new AbstractColumn<>(
                createStringResource("")) {

            @Override
            public void populateItem(Item<ICellPopulator<BusinessRoleApplicationDto>> cellItem,
                    String componentId, IModel<BusinessRoleApplicationDto> model) {

                InlineButtonPanel inlineButtonPanel = new InlineButtonPanel(componentId, Model.of("Operations")) {
                    @Override
                    protected void addPanelButton(RepeatingView repeatingView) {

                        LoadableModel<String> loadableIncludeModel = new LoadableModel<>() {
                            @Override
                            protected String load() {
                                String icon = "fa fa-minus";
                                if (!model.getObject().isInclude()) {
                                    icon = " fa fa-share";
                                }
                                return icon;
                            }
                        };

                        AjaxIconButton ajaxIncludeButton = new AjaxIconButton(repeatingView.newChildId(), loadableIncludeModel,
                                createStringResource("Include")) {
                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                model.getObject().setInclude(!model.getObject().isInclude());
                                ajaxRequestTarget.add(this);
                                getTable().replaceWith(generateTable(provider));
                                ajaxRequestTarget.add(getTable().setOutputMarkupId(true));
                            }
                        };

                        ajaxIncludeButton.setOutputMarkupId(true);
                        ajaxIncludeButton.setOutputMarkupPlaceholderTag(true);
                        repeatingView.add(ajaxIncludeButton);

                        AjaxIconButton ajaxEditButton = new AjaxIconButton(repeatingView.newChildId(),
                                Model.of("fa fa-eye"),
                                createStringResource("Check")) {
                            @Override
                            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                List<ObjectDelta<? extends ObjectType>> objectDeltas = model.getObject().getObjectDeltas();
                                List<DeltaDto> deltaDtos = model.getObject().getDeltaDtos();
                                Task task = ((PageBase) getPage()).createSimpleTask("visualizeDelta");
                                OperationResult operationResult = new OperationResult("visualizeDelta");
                                List<Visualization> visualizations;
                                try {
                                    visualizations = ((PageBase) getPage()).getModelInteractionService().visualizeDeltas(
                                            objectDeltas, task, operationResult);
                                } catch (SchemaException | ExpressionEvaluationException e) {
                                    throw new RuntimeException(e);
                                }

                                List<VisualizationDto> visualizationDtos = new ArrayList<>();
                                for (Visualization visualization : visualizations) {
                                    visualizationDtos.add(new VisualizationDto(visualization));
                                }

                                IModel<List<VisualizationDto>> model = () -> visualizationDtos;

                                ModificationTargetPreviewPanel detailsPanel = new ModificationTargetPreviewPanel(
                                        ((PageBase) getPage())
                                                .getMainPopupBodyId(), Model.of("User Assignment Editor"), deltaDtos, model) {
                                    @Override
                                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                        super.onClose(ajaxRequestTarget);
                                    }
                                };
                                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
                            }
                        };

                        ajaxEditButton.setOutputMarkupId(true);
                        repeatingView.add(ajaxEditButton);
                    }
                };

                inlineButtonPanel.setOutputMarkupId(true);
                cellItem.add(inlineButtonPanel);
                changeBackgroundColorCell(cellItem, model);

            }

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public String getSortProperty() {
                return UserType.F_NAME.toString();
            }
        });

        return columns;
    }

    private void changeBackgroundColorCell(Item<ICellPopulator<BusinessRoleApplicationDto>> item,
            IModel<BusinessRoleApplicationDto> rowModel) {
        if (!rowModel.getObject().isInclude()) {
            item.add(new AttributeModifier("class", "table-danger"));
        }
    }

}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisAccessTabPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.common.mining.objects.statistic.UserAccessDistribution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@PanelType(name = "outlierAccessDistribution")
@PanelInstance(
        identifier = "outlierAccessDistribution",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlier.access.distribution",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 60
        )
)
public class OutlierAccessDistributionPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public OutlierAccessDistributionPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisWidgetsPanel components = loadDetailsPanel(ID_PANEL);
        container.add(components);
    }

    private RoleAnalysisWidgetsPanel loadDetailsPanel(@NotNull String id) {
        RoleAnalysisOutlierType outlier = getObjectWrapperObject().asObjectable();
        ObjectReferenceType targetObjectRef = outlier.getObjectRef();
        String userOid = targetObjectRef.getOid();

        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task simpleTask = pageBase.createSimpleTask("loadOutlierDetails");
        OperationResult result = simpleTask.getResult();
        PrismObject<UserType> prismUser = roleAnalysisService
                .getUserTypeObject(userOid, simpleTask, result);

        UserAccessDistribution userAccessDistribution = new UserAccessDistribution();

        if (prismUser != null) {
            userAccessDistribution = roleAnalysisService.resolveUserAccessDistribution(
                    prismUser, simpleTask, result);
        }

        UserAccessDistribution finalUserAccessDistribution = userAccessDistribution;
        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel(finalUserAccessDistribution)) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                RoleAnalysisAccessTabPanel roleAnalysisAccessTabPanel = new RoleAnalysisAccessTabPanel(id1,
                        new LoadableModel<>() {
                            @Override
                            protected UserAccessDistribution load() {
                                return finalUserAccessDistribution;
                            }
                        });
                roleAnalysisAccessTabPanel.setOutputMarkupId(true);
                return roleAnalysisAccessTabPanel;
            }
        };
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel(UserAccessDistribution userAccessDistribution) {

        int directAssignment = userAccessDistribution.getDirectAssignmentsCount();
        int indirectAssignment = userAccessDistribution.getIndirectAssignmentsCount();
        int duplicatedRoleAssignmentCount = userAccessDistribution.getDuplicatesCount();
        int allAssignmentCount = userAccessDistribution.getAllAccessCount();

        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, allAssignmentCount);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("RoleAnalysisOutlierType.all.access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.all.access.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {

                        Label label = new Label(id, directAssignment);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("Direct access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, indirectAssignment);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Indirect access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("Chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Label label = new Label(id, duplicatedRoleAssignmentCount);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, Model.of("Duplicated access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierType.anomalyAverageConfidence.help");
                            }
                        };
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}

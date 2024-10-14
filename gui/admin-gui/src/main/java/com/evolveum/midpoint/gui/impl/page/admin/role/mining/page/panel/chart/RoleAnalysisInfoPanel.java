/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.AnalysisInfoWidgetDto;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.PageOutliers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.component.RoleAnalysisIdentifyWidgetPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.widgets.model.IdentifyWidgetItem;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDistributionProgressPanel;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class RoleAnalysisInfoPanel extends BasePanel<AnalysisInfoWidgetDto> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisInfoPanel.class);

    private static final String ID_PATTERN_PANEL = "patternPanel";
    private static final String ID_OUTLIER_PANEL = "outlierPanel";
    private static final String ID_DISTRIBUTION_PANEL = "distributionPanel";

    public RoleAnalysisInfoPanel(String id, IModel<AnalysisInfoWidgetDto> analysisInfoWidgetDto) {
        super(id, analysisInfoWidgetDto);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initPanels();
    }

    private void initPanels() {
        initInfoPatternPanel();
        initInfoOutlierPanel();
        initDistributionPanel();
    }

    private void initDistributionPanel() {

        RoleAnalysisIdentifyWidgetPanel distributionPanel = new RoleAnalysisIdentifyWidgetPanel(ID_DISTRIBUTION_PANEL,
                createStringResource("Distribution.access.title"), getModelDistribution()) {

            /*@Contract(pure = true)
            @Override
            protected @NotNull String getBodyHeaderPanelStyle() {
                return "height:90px;";
            }*/

            @Override
            protected @NotNull Component getBodyHeaderPanel(String id) {

                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task task = pageBase.createSimpleTask("Count objects");
                OperationResult result = task.getResult();

                Integer rolesInSystem = roleAnalysisService.countObjects(RoleType.class, null, null, task, result);
                if (rolesInSystem == null) {
                    rolesInSystem = 0;
                }

                Integer usersInSystem = roleAnalysisService.countObjects(UserType.class, null, null, task, result);
                if (usersInSystem == null) {
                    usersInSystem = 0;
                }

                int allObjects = rolesInSystem + usersInSystem;

                List<ProgressBar> progressBars = new ArrayList<>();
                progressBars.add(new ProgressBar(rolesInSystem * 100 / (double) allObjects, ProgressBar.State.SUCCESS));
                progressBars.add(new ProgressBar(usersInSystem * 100 / (double) allObjects, ProgressBar.State.DANGER));

                Integer finalUsersInSystem = usersInSystem;
                Integer finalRolesInSystem = rolesInSystem;
                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {
                    @Contract("_ -> new")
                    @Override
                    protected @NotNull Component getPanelComponent(String id) {
                        return new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                    }

                    @Override
                    protected Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);
                        MetricValuePanel resolved = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("Roles")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-success fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalRolesInSystem);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeAppender.append("style", "font-size:18px"));
                                return label;
                            }
                        };
                        resolved.setOutputMarkupId(true);
                        view.add(resolved);

                        MetricValuePanel inProgress = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("Users")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-danger fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, finalUsersInSystem);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeAppender.append("style", "font-size:18px"));
                                return label;
                            }
                        };
                        inProgress.setOutputMarkupId(true);
                        view.add(inProgress);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeAppender.append("class", "col-12 p-0"));
                return panel;
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> getFooterButtonLabelModel() {
                return Model.of("Explore distribution details");
            }

            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                RoleAnalysisChartPanel roleAnalysisChartPanel = new RoleAnalysisChartPanel(getPageBase().getMainPopupBodyId());
                roleAnalysisChartPanel.setOutputMarkupId(true);
                getPageBase().showMainPopup(roleAnalysisChartPanel, target);
            }

            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
            }
        };
        add(distributionPanel);
    }

    private void initInfoPatternPanel() {

        if (getModel() == null || getModelObject() == null || getModelObject().getPatternModelData() == null) {
            WebMarkupContainer roleAnalysisInfoOutlierPanel = new WebMarkupContainer(ID_PATTERN_PANEL);
            roleAnalysisInfoOutlierPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoOutlierPanel);
            return;
        }

        if (getModel() == null || getModelObject() == null) {
            WebMarkupContainer roleAnalysisInfoOutlierPanel = new WebMarkupContainer(ID_OUTLIER_PANEL);
            roleAnalysisInfoOutlierPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoOutlierPanel);
            return;
        }

        RoleAnalysisIdentifyWidgetPanel patternPanel = new RoleAnalysisIdentifyWidgetPanel(ID_PATTERN_PANEL,
                createStringResource("Pattern.suggestions.title"), Model.ofList(getModelObject().getPatternModelData())) {

            @Override
            protected @NotNull Component getBodyHeaderPanel(String id) {
                List<ProgressBar> progressBars = new ArrayList<>();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                Task task = getPageBase().createSimpleTask("Prepare data");
                OperationResult result = task.getResult();

                int[] resolvedAndCandidateRoles = roleAnalysisService.computeResolvedAndCandidateRoles(task, result);

                int resolved = resolvedAndCandidateRoles[0];
                int inProgress = resolvedAndCandidateRoles[1];
                int allObjects = resolved + inProgress;

                progressBars.add(new ProgressBar(resolved * 100 / (double) allObjects, ProgressBar.State.SUCCESS));
                progressBars.add(new ProgressBar(inProgress * 100 / (double) allObjects, ProgressBar.State.WARNINIG));

                RoleAnalysisDistributionProgressPanel<?> panel = new RoleAnalysisDistributionProgressPanel<>(id) {
                    @Contract("_ -> new")
                    @Override
                    protected @NotNull Component getPanelComponent(String id) {
                        return new ProgressBarPanel(id, new LoadableModel<>() {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            protected List<ProgressBar> load() {
                                return progressBars;
                            }
                        });
                    }

                    @Override
                    protected Component getLegendComponent(String id) {
                        RepeatingView view = new RepeatingView(id);
                        MetricValuePanel resolvedPanel = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("Resolved")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-success fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, resolved);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeAppender.append("style", "font-size:18px"));
                                return label;
                            }
                        };
                        resolvedPanel.setOutputMarkupId(true);
                        view.add(resolvedPanel);

                        MetricValuePanel inProgressPanel = new MetricValuePanel(view.newChildId()) {
                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getTitleComponent(String id) {
                                return new IconWithLabel(id, Model.of("In progress")) {
                                    @Override
                                    protected String getIconCssClass() {
                                        return "fa fa-circle text-warning fa-2xs align-middle";
                                    }

                                    @Override
                                    protected String getIconComponentCssStyle() {
                                        return "font-size:8px;margin-bottom:2px;";
                                    }

                                    @Override
                                    protected String getLabelComponentCssClass() {
                                        return "txt-toned";
                                    }

                                    @Override
                                    protected String getComponentCssClass() {
                                        return super.getComponentCssClass() + "mb-1 gap-2";
                                    }
                                };
                            }

                            @Contract("_ -> new")
                            @Override
                            protected @NotNull Component getValueComponent(String id) {
                                Label label = new Label(id, inProgress);
                                label.add(AttributeAppender.append("class", "d-flex pl-3 m-0 lh-1 text-bold txt-toned"));
                                label.add(AttributeAppender.append("style", "font-size:18px"));
                                return label;
                            }
                        };
                        inProgressPanel.setOutputMarkupId(true);
                        view.add(inProgressPanel);

                        return view;

                    }
                };

                panel.setOutputMarkupId(true);
                panel.add(AttributeAppender.append("class", "col-12 p-0"));
                return panel;
            }

            @Override
            protected @NotNull String getIconCssClass() {
                return GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
            }
        };
        add(patternPanel);
    }

    private void initInfoOutlierPanel() {

        if (getModel() == null || getModelObject() == null || getModelObject().getOutlierModelData() == null) {
            WebMarkupContainer roleAnalysisInfoOutlierPanel = new WebMarkupContainer(ID_OUTLIER_PANEL);
            roleAnalysisInfoOutlierPanel.setOutputMarkupId(true);
            add(roleAnalysisInfoOutlierPanel);
            return;
        }
        RoleAnalysisIdentifyWidgetPanel outlierPanel = new RoleAnalysisIdentifyWidgetPanel(ID_OUTLIER_PANEL,
                createStringResource("Outlier.suggestions.title"), Model.ofList(getModelObject().getOutlierModelData())) {
            @Override
            protected void onClickFooter(AjaxRequestTarget target) {
                getPageBase().navigateToNext(PageOutliers.class);
            }
        };
        outlierPanel.setOutputMarkupId(true);
        add(outlierPanel);
    }

    protected @Nullable IModel<List<IdentifyWidgetItem>> getModelDistribution() {
        PageBase pageBase = (PageBase) getPage();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("Count objects");
        OperationResult result = task.getResult();

        Integer usersInSystem = roleAnalysisService.countObjects(UserType.class, null, null, task, result);
        if (usersInSystem == null) {
            usersInSystem = 0;
        }

        int numberOfRoleToUserAssignment = roleAnalysisService.countUserOwnedRoleAssignment(result);

        int finalUsersInSystem = usersInSystem;

        double averagePerUser = finalUsersInSystem > 0
                ? (double) numberOfRoleToUserAssignment / finalUsersInSystem
                : 0.0;

        BigDecimal averagePerUserRounded = BigDecimal.valueOf(averagePerUser)
                .setScale(2, RoundingMode.HALF_UP);
        averagePerUser = averagePerUserRounded.doubleValue();

        double finalAveragePerUser = averagePerUser;

        int usedRoles = (int) countAppliedDirectlyRoles();

        List<IdentifyWidgetItem> detailsModel = new ArrayList<>();

        IdentifyWidgetItem identifyWidgetItem = new IdentifyWidgetItem(
                IdentifyWidgetItem.ComponentType.STATISTIC,
                Model.of("fe fe-assignment"),
                Model.of(),
                Model.of("Number of role assignment to user"),
                Model.of(String.valueOf(numberOfRoleToUserAssignment)),
                Model.of("name")) {

            public Component createValueTitleComponent(String id) {
                Label label = new Label(id);
                label.setOutputMarkupId(true);
                label.add(new VisibleBehaviour(() -> getDescription() != null));
                return label;
            }

            @Override
            public Component createDescriptionComponent(String id) {
                return super.createDescriptionComponent(id);
            }

            @Override
            public Component createScoreComponent(String id) {
                Component valueComponent = super.createScoreComponent(id);
                valueComponent.add(AttributeAppender.replace("class", "txt-default text-lg m-0 pr-2 text-right"));
                valueComponent.add(AttributeAppender.replace("style", "flex-basis:100%;"));
                return valueComponent;
            }

            @Override
            public Component createTitleComponent(String id) {
                Label linkPanel = new Label(id, Model.of("Role to user assignment"));
                linkPanel.setOutputMarkupId(true);
                linkPanel.add(AttributeAppender.append("class", "txt-default"));
                return linkPanel;
            }

            @Override
            public Component createActionComponent(String id) {
                return new WebMarkupContainer(id);
            }
        };
        detailsModel.add(identifyWidgetItem);

        identifyWidgetItem = new IdentifyWidgetItem(
                IdentifyWidgetItem.ComponentType.STATISTIC,
                Model.of("fa fa-bar-chart"),
                Model.of(),
                Model.of("Average role assignment per user"),
                Model.of(String.valueOf(finalAveragePerUser)),
                Model.of("name")) {

            public Component createValueTitleComponent(String id) {
                Label label = new Label(id);
                label.setOutputMarkupId(true);
                label.add(new VisibleBehaviour(() -> getDescription() != null));
                return label;
            }

            @Override
            public Component createDescriptionComponent(String id) {
                return super.createDescriptionComponent(id);
            }

            @Override
            public Component createScoreComponent(String id) {
                Component valueComponent = super.createScoreComponent(id);
                valueComponent.add(AttributeAppender.replace("class", "txt-default text-lg m-0 pr-2 text-right"));
                valueComponent.add(AttributeAppender.replace("style", "flex-basis:100%;"));
                return valueComponent;
            }

            @Override
            public Component createTitleComponent(String id) {
                Label linkPanel = new Label(id, Model.of("Average assignment"));
                linkPanel.setOutputMarkupId(true);
                linkPanel.add(AttributeAppender.append("class", "txt-default"));
                return linkPanel;
            }

            @Override
            public Component createActionComponent(String id) {
                return new WebMarkupContainer(id);
            }
        };
        detailsModel.add(identifyWidgetItem);

        identifyWidgetItem = new IdentifyWidgetItem(
                IdentifyWidgetItem.ComponentType.STATISTIC,
                Model.of("fa fa-recycle"),
                Model.of(),
                Model.of("Existing roles that is applied directly"),
                Model.of(String.valueOf(usedRoles)),
                Model.of("name")) {

            public Component createValueTitleComponent(String id) {
                Label label = new Label(id);
                label.setOutputMarkupId(true);
                label.add(new VisibleBehaviour(() -> getDescription() != null));
                return label;
            }

            @Override
            public Component createDescriptionComponent(String id) {
                return super.createDescriptionComponent(id);
            }

            @Override
            public Component createScoreComponent(String id) {
                Component valueComponent = super.createScoreComponent(id);
                valueComponent.add(AttributeAppender.replace("class", "txt-default text-lg m-0 pr-2 text-right"));
                valueComponent.add(AttributeAppender.replace("style", "flex-basis:100%;"));
                return valueComponent;
            }

            @Override
            public Component createTitleComponent(String id) {
                Label linkPanel = new Label(id, Model.of("Applied direct roles"));
                linkPanel.setOutputMarkupId(true);
                linkPanel.add(AttributeAppender.append("class", "txt-default"));
                return linkPanel;
            }

            @Override
            public Component createActionComponent(String id) {
                return new WebMarkupContainer(id);
            }
        };

        detailsModel.add(identifyWidgetItem);

        return Model.ofList(detailsModel);
    }

    private double countAppliedDirectlyRoles() {
        RepositoryService repositoryService = getPageBase().getRepositoryService();
        OperationResult result = new OperationResult("OP_LOAD_STATISTICS");

        SearchResultList<PrismContainerValue<?>> aggregateResult = new SearchResultList<>();

        var spec = AggregateQuery.forType(AssignmentType.class);
        try {
            spec.retrieve(F_NAME, ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME))
                    .retrieve(AssignmentType.F_TARGET_REF)
                    .filter(PrismContext.get().queryFor(AssignmentType.class).ownedBy(UserType.class, UserType.F_ASSIGNMENT)
                            .and().ref(AssignmentType.F_TARGET_REF).type(RoleType.class).buildFilter())
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH);

            AggregateQuery.ResultItem resultItem = spec.getResultItem(F_ASSIGNMENT);
            spec.orderBy(resultItem, OrderDirection.DESCENDING);
            aggregateResult = repositoryService.searchAggregate(spec, result);

        } catch (SchemaException e) {
            LOGGER.error("Cloud aggregate execute search", e);
        }

        return aggregateResult.size();

    }

}

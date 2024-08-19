/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.pattern;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.confidenceBasedTwoColor;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarNew;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class RoleAnalysisPatternTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisPatternTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_REDUCTION = "reduction";
    private static final String ID_ATTRIBUTE_CONFIDENCE = "attribute-confidence";
    private static final String ID_PROGRESS_BAR = "progress-bar";
    private static final String ID_USERS_COUNT = "users-count";
    private static final String ID_ROLES_COUNT = "roles-count";
    private static final String ID_SESSION = "session";
    private static final String ID_CLUSTER = "cluster";
    private static final String ID_SEPARATOR = "separator";

    public RoleAnalysisPatternTilePanel(String id, IModel<RoleAnalysisPatternTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initNamePanel();

        initToolBarPanel();

        initStatisticsPanel();

        initProgressBar();

        initLocationButtons();

        initFirstCountPanel();

        Label separator = new Label(ID_SEPARATOR, "/");
        separator.setOutputMarkupId(true);
        add(separator);

        initSecondCountPanel();
    }

    private void initProgressBar() {
        DetectedPattern pattern = getModelObject().getPattern();
        double itemsConfidence = pattern.getItemsConfidence();
        BigDecimal bd = new BigDecimal(itemsConfidence);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double finalItemsConfidence = bd.doubleValue();

        String colorClass = confidenceBasedTwoColor(finalItemsConfidence);

        ProgressBarNew progressBar = new ProgressBarNew(ID_PROGRESS_BAR) {

            @Override
            public double getActualValue() {
                return finalItemsConfidence;
            }

            @Override
            protected boolean isTitleContainerVisible() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssStyle() {
                return "border-radius: 3px; height:13px;";
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.replace("title", () -> "Attribute confidence: " + finalItemsConfidence + "%"));
        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    private void initStatisticsPanel() {
        MetricValuePanel reduction = new MetricValuePanel(ID_REDUCTION) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysisPatternTilePanel.reduction"));
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "text-muted"));
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {

                RepeatingView rv = new RepeatingView(id);
                rv.setOutputMarkupId(true);

                RoleAnalysisPatternTileModel<T> modelObject = RoleAnalysisPatternTilePanel.this.getModelObject();
                Double metric = modelObject.getPattern().getMetric();
                if (metric == null) {
                    metric = 0.0;
                }

                double systemReductionPercentage = modelObject.getSystemReductionPercentage();
                Double finalMetric = metric;
                String title = systemReductionPercentage + "%";
                IconWithLabel value = new IconWithLabel(rv.newChildId(), Model.of(title)) {
                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return "fa fa-arrow-down text-success";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getComponentCssClass() {
                        return "d-flex align-items-center h3";
                    }
                };

                value.setOutputMarkupId(true);
                rv.add(value);

                String mutedTitle = " (" + String.format("%.2f", finalMetric) + ")";
                Label label = new Label(rv.newChildId(), Model.of(mutedTitle));
                label.add(AttributeAppender.append("class", "text-muted"));
                label.setOutputMarkupId(true);
                rv.add(label);
                rv.add(AttributeAppender.append("class", "d-flex align-items-center"));
                return rv;
            }
        };

        reduction.setOutputMarkupId(true);
        add(reduction);

        MetricValuePanel attributeConfidence = new MetricValuePanel(ID_ATTRIBUTE_CONFIDENCE) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysisPatternTilePanel.attributeConfidence"));
                label.setOutputMarkupId(true);
                label.add(AttributeAppender.append("class", "text-muted"));
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                RoleAnalysisPatternTileModel<T> modelObject = RoleAnalysisPatternTilePanel.this.getModelObject();
                DetectedPattern pattern = modelObject.getPattern();

                double finalItemsConfidence = pattern.getItemsConfidence();
                IconWithLabel value = new IconWithLabel(id, () -> String.format("%.2f", finalItemsConfidence) + "%") {
                    @Contract(pure = true)
                    @Override
                    public @NotNull String getIconCssClass() {
                        return "fa fa-thumbs-up";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getComponentCssClass() {
                        return "d-flex align-items-center h3";
                    }
                };

                value.setOutputMarkupId(true);
                return value;
            }
        };

        attributeConfidence.setOutputMarkupId(true);
        add(attributeConfidence);
    }

    private void initSecondCountPanel() {
        Label processedObjectCount = new Label(ID_ROLES_COUNT, () -> getModelObject().getRoleCount() + " Roles") {
        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeAppender.replace(
                "title", () -> "Processed objects: " + getModelObject().getRoleCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        Label clusterCount = new Label(ID_USERS_COUNT, () -> getModelObject().getUserCount() + " Users") {
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "User count: " + getModelObject().getUserCount()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initLocationButtons() {
        DetectedPattern pattern = getModelObject().getPattern();
        ObjectReferenceType clusterRef = pattern.getClusterRef();
        ObjectReferenceType sessionRef = pattern.getSessionRef();
        AjaxLinkPanel sessionLink = new AjaxLinkPanel(ID_SESSION, Model.of(sessionRef.getTargetName().getOrig())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, sessionRef.getOid());
                getPageBase().navigateToNext(PageRoleAnalysisSession.class, parameters);
            }
        };

        sessionLink.setOutputMarkupId(true);
        add(sessionLink);

        AjaxLinkPanel clusterLink = new AjaxLinkPanel(ID_CLUSTER, Model.of(clusterRef.getTargetName().getOrig())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());
                getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
            }
        };
        clusterLink.setOutputMarkupId(true);
        add(clusterLink);
    }

    private void initNamePanel() {
        RoleAnalysisPatternTileModel<T> modelObject = getModelObject();
        AjaxLinkPanel objectTitle = new AjaxLinkPanel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetectedPattern pattern = modelObject.getPattern();
                explorePatternPerform(pattern);
            }
        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeAppender.replace("style", "font-size:20px"));
        objectTitle.add(AttributeAppender.replace("title", () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initToolBarPanel() {
        DropdownButtonPanel barMenu = new DropdownButtonPanel(ID_BUTTON_BAR, new DropdownButtonDto(
                null, "fa fa-ellipsis-v", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " p-0 ";
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace("title",
                createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class",
                "bg-white d-flex flex-column align-items-center elevation-1 rounded w-100 h-100 p-0"));

        add(AttributeAppender.append("style", "width:25%"));
    }

    private void explorePatternPerform(@NotNull DetectedPattern pattern) {
        PageParameters parameters = new PageParameters();
        String clusterOid = pattern.getClusterRef().getOid();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");
        parameters.add(PARAM_DETECTED_PATER_ID, pattern.getId());
        StringValue fullTableSetting = getPageBase().getPageParameters().get(PARAM_TABLE_SETTING);
        if (fullTableSetting != null && fullTableSetting.toString() != null) {
            parameters.add(PARAM_TABLE_SETTING, fullTableSetting.toString());
        }

        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

//    public void initStatusBar() {
//        RoleAnalysisPatternTileModel<T> modelObject = getModelObject();
//        String confidence;
//        DetectedPattern pattern;
//        if (modelObject != null && modelObject.getPattern() != null) {
//            pattern = modelObject.getPattern();
//            confidence = String.format("%.2f", pattern.getItemsConfidence()) + "%";
//        } else {
//            EmptyPanel emptyPanel = new EmptyPanel(ID_STATUS_BAR);
//            emptyPanel.setOutputMarkupId(true);
//            add(emptyPanel);
//            return;
//        }
//
//        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
//                .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);
//
//        DetectedPattern finalPattern = pattern;
//        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(ID_STATUS_BAR, iconBuilder.build(),
//                Model.of(confidence)) {
//
//            @Override
//            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
//                RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
//                        ((PageBase) getPage()).getMainPopupBodyId(),
//                        Model.of(finalPattern));
//                ((PageBase) getPage()).showMainPopup(component, ajaxRequestTarget);
//            }
//        };
//
//        objectButton.titleAsLabel(true);
//        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm "));
//        objectButton.add(AttributeAppender.append("style", "width:100px"));
//        objectButton.setOutputMarkupId(true);
//        add(objectButton);
//    }
//
//    private void buildExploreButton() {
//        DetectedPattern pattern = getModelObject().getPattern();
//        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
//                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
//        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
//                RoleAnalysisPatternTilePanel.ID_EXPLORE_PATTERN_BUTTON,
//                iconBuilder.build(),
//                createStringResource("RoleAnalysis.explore.button.title")) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target) {
//                explorePatternPerform(pattern);
//            }
//
//            @Override
//            protected void onError(@NotNull AjaxRequestTarget target) {
//                target.add(((PageBase) getPage()).getFeedbackPanel());
//            }
//        };
//        migrationButton.titleAsLabel(true);
//        migrationButton.setOutputMarkupId(true);
//        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
//        migrationButton.setOutputMarkupId(true);
//        add(migrationButton);
//    }
//
//    private void buildCandidateButton() {
//        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
//                GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
//        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
//                RoleAnalysisPatternTilePanel.ID_CREATE_CANDIDATE_BUTTON,
//                iconBuilder.build(),
//                createStringResource("RoleMining.button.title.candidate")) {
//            @Serial private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target) {
//                createCandidatePerform(target);
//            }
//
//            @Override
//            protected void onError(@NotNull AjaxRequestTarget target) {
//                target.add(((PageBase) getPage()).getFeedbackPanel());
//            }
//        };
//        migrationButton.titleAsLabel(true);
//        migrationButton.setOutputMarkupId(true);
//        migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
//        migrationButton.setOutputMarkupId(true);
//        migrationButton.add(new VisibleBehaviour(() -> false));
//        add(migrationButton);
//    }

    private void createCandidatePerform(@NotNull AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask("Process detected pattern");
        OperationResult result = task.getResult();
        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        DetectedPattern pattern = getModelObject().getPattern();
        ObjectReferenceType clusterRef = pattern.getClusterRef();
        @NotNull String status = roleAnalysisService
                .recomputeAndResolveClusterOpStatus(clusterRef.getOid(), result, task, true, null);

        if (status.equals("processing")) {
            warn("Couldn't start detection. Some process is already in progress.");
            LOGGER.error("Couldn't start detection. Some process is already in progress.");
            target.add(getFeedbackPanel());
            return;
        }

        Set<String> roles = pattern.getRoles();
        Set<String> users = pattern.getUsers();
        Long patternId = pattern.getId();

        Set<PrismObject<RoleType>> candidateInducements = new HashSet<>();

        for (String roleOid : roles) {
            PrismObject<RoleType> roleObject = roleAnalysisService
                    .getRoleTypeObject(roleOid, task, result);
            if (roleObject != null) {
                candidateInducements.add(roleObject);
            }
        }

        PrismObject<RoleType> businessRole = new RoleType().asPrismObject();

        List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

        for (String userOid : users) {
            PrismObject<UserType> userObject = WebModelServiceUtils.loadObject(UserType.class, userOid, getPageBase(), task, result);
//                    roleAnalysisService
//                    .getUserTypeObject(userOid, task, result);
            if (userObject != null) {
                roleApplicationDtos.add(new BusinessRoleDto(userObject,
                        businessRole, candidateInducements, getPageBase()));
            }
        }

        PrismObject<RoleAnalysisClusterType> prismObjectCluster = roleAnalysisService
                .getClusterTypeObject(clusterRef.getOid(), task, result);

        if (prismObjectCluster == null) {
            return;
        }

        BusinessRoleApplicationDto operationData = new BusinessRoleApplicationDto(
                prismObjectCluster, businessRole, roleApplicationDtos, candidateInducements);
        operationData.setPatternId(patternId);

        PageRole pageRole = new PageRole(operationData.getBusinessRole(), operationData);
        setResponsePage(pageRole);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Explore suggested role")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        DetectedPattern pattern = getModelObject().getPattern();
                        explorePatternPerform(pattern);
                    }
                };
            }

        });

        items.add(new InlineMenuItem(createStringResource("Create candidate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        createCandidatePerform(target);
                    }
                };
            }

        });

        items.add(new InlineMenuItem(createStringResource("Details view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        DetectedPattern pattern = getModelObject().getPattern();

                        RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                                ((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of(pattern));
                        ((PageBase) getPage()).showMainPopup(component, target);
                    }
                };
            }

        });

        return items;
    }
}

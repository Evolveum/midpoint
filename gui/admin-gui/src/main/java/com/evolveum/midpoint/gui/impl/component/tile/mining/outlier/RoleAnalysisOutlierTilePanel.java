/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.analysis.AttributeAnalysisStructure;
import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.chart.RoleAnalysisAttributeChartPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierHeaderResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierItemResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierObjectModel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.OutlierResultPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisOutlierTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisOutlierTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_MEMBERS_ANALYSIS_PANEL = "members-analysis-panel";
    private static final String ID_CLUSTER_ANALYSIS_PANEL = "cluster-analysis-panel";
    private static final String ID_CLUSTER = "cluster-info";
    private static final String ID_SESSION = "session-info";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";

    public RoleAnalysisOutlierTilePanel(String id, IModel<RoleAnalysisOutlierTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        buildClusterAnalysisButton();

        buildMembersAnalysisButton();

        initFirstCountPanel();

        initSecondCountPanel();
    }

    private void initSecondCountPanel() {
        RoleAnalysisOutlierType outlierParent = getModelObject().getOutlierParent();
        ObjectReferenceType targetClusterRef = outlierParent.getTargetClusterRef();

        IconWithLabel clusterCount = new IconWithLabel(ID_CLUSTER, () -> targetClusterRef.getTargetName().toString()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, targetClusterRef.getOid());
                parameters.add("panelId", "clusterDetails");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "Cluster: " + targetClusterRef.getTargetName().toString()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initFirstCountPanel() {
        RoleAnalysisOutlierType outlierParent = getModelObject().getOutlierParent();
        ObjectReferenceType targetSessionRef = outlierParent.getTargetSessionRef();

        IconWithLabel clusterCount = new IconWithLabel(ID_SESSION, () -> targetSessionRef.getTargetName().toString()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, targetSessionRef.getOid());
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "Session: " + targetSessionRef.getTargetName().toString()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initNamePanel() {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisOutlierTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                navigateToRoleDetails();
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
                "catalog-tile-panel d-flex flex-column align-items-center border w-100 h-100 p-3"));

        add(AttributeAppender.append("style", "width:25%"));
    }

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    public void initStatusBar() {

        OutlierObjectModel outlierObjectModel = getModelObject().getOutlierObjectModel();
        if (outlierObjectModel == null) {
            add(new EmptyPanel(ID_STATUS_BAR));
            return;
        }

        String outlierName = outlierObjectModel.getOutlierName();
        double outlierConfidence = outlierObjectModel.getOutlierConfidence();
        String description = outlierObjectModel.getOutlierDescription();
        String timestamp = outlierObjectModel.getTimeCreated();

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

        String formattedConfidence = String.format("%.2f%%", outlierConfidence);
        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(ID_STATUS_BAR, iconBuilder.build(),
                Model.of(formattedConfidence)) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                OutlierResultPanel detailsPanel = new OutlierResultPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel")) {

                    @Override
                    public StringResourceModel getTitle() {
                        return createStringResource("Outlier assignment description");
                    }

                    @Override
                    public @NotNull Component getCardHeaderBody(String componentId) {
                        OutlierHeaderResultPanel components = new OutlierHeaderResultPanel(componentId, outlierName,
                                description, String.valueOf(outlierConfidence), timestamp);
                        components.setOutputMarkupId(true);
                        return components;
                    }

                    @Override
                    public Component getCardBodyComponent(String componentId) {
                        //TODO just for testing
                        RepeatingView cardBodyComponent = (RepeatingView) super.getCardBodyComponent(componentId);
                        outlierObjectModel.getOutlierItemModels().forEach(outlierItemModel -> {
                            cardBodyComponent.add(new OutlierItemResultPanel(cardBodyComponent.newChildId(), outlierItemModel));
                        });
                        return cardBodyComponent;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }

                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
            }
        };

        objectButton.titleAsLabel(true);
        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm rounded-pill"));
        objectButton.add(AttributeAppender.append("style", "width:100px"));
        objectButton.setOutputMarkupId(true);
        add(objectButton);
    }

    private void buildClusterAnalysisButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisOutlierTilePanel.ID_CLUSTER_ANALYSIS_PANEL,
                iconBuilder.build(),
                createStringResource("Cluster analysis")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {

                Task task = getPageBase().createSimpleTask("Load object");
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                RoleAnalysisOutlierDescriptionType result = getModelObject().getDescriptionType();
                RoleAnalysisOutlierType outlierParent = getModelObject().getOutlierParent();
                ObjectReferenceType outlierParentObjectRef = outlierParent.getTargetObjectRef();
                ObjectReferenceType clusterRef = result.getCluster();
                ObjectReferenceType sessionRef = result.getSession();

                PrismObject<RoleAnalysisClusterType> clusterPrism = roleAnalysisService.getClusterTypeObject(
                        clusterRef.getOid(), task, task.getResult());

                PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService.getSessionTypeObject(
                        sessionRef.getOid(), task, task.getResult());
                if (clusterPrism == null || sessionPrism == null) {
                    return;
                }

                RoleAnalysisClusterType cluster = clusterPrism.asObjectable();
                RoleAnalysisSessionType session = sessionPrism.asObjectable();

                ObjectReferenceType propertyObjectRef = result.getObject();
                QName type = propertyObjectRef.getType();

                PrismObject<UserType> userTypeObject;
                PrismObject<RoleType> roleTypeObject;
                if (type.equals(RoleType.COMPLEX_TYPE)) {
                    userTypeObject = roleAnalysisService.getUserTypeObject(outlierParentObjectRef.getOid(), task, task.getResult());
                    roleTypeObject = roleAnalysisService.getRoleTypeObject(propertyObjectRef.getOid(), task, task.getResult());
                } else {
                    userTypeObject = roleAnalysisService.getUserTypeObject(propertyObjectRef.getOid(), task, task.getResult());
                    roleTypeObject = roleAnalysisService.getRoleTypeObject(outlierParentObjectRef.getOid(), task, task.getResult());
                }

                if (userTypeObject == null || roleTypeObject == null) {
                    return;
                }
                List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(session, UserType.COMPLEX_TYPE);
                if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
                    return;
                }
                Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(userTypeObject, attributesForUserAnalysis);

                RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

                AnalysisClusterStatisticType clusterStatistics = cluster.getClusterStatistics();
                RoleAnalysisAttributeAnalysisResult clusterAttributes = null;
                if (clusterStatistics == null || clusterStatistics.getUserAttributeAnalysisResult() == null) {
                    AnalysisClusterStatisticType outlierParentClusterStatistics = outlierParent.getClusterStatistics();
                    if (outlierParentClusterStatistics != null && outlierParentClusterStatistics.getUserAttributeAnalysisResult() != null) {
                        clusterAttributes = outlierParentClusterStatistics.getUserAttributeAnalysisResult();
                    }
                } else {
                    clusterAttributes = clusterStatistics.getUserAttributeAnalysisResult();
                }

                RoleAnalysisAttributeAnalysisResult compareAttributeResult = null;
                if (clusterAttributes != null) {
                    compareAttributeResult = roleAnalysisService.resolveSimilarAspect(userAttributes, clusterAttributes);
                }
                if (compareAttributeResult == null) {
                    return;
                }

                //TODO Support role mode
                RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Role analysis attribute panel"),
                        null, clusterAttributes,
                        null, compareAttributeResult) {
                    @Override
                    protected @NotNull String getChartContainerStyle() {
                        return "height:30vh;";
                    }

                    @Override
                    public Set<String> getPathToMark() {
                        return userPathToMark;
                    }
                };

                roleAnalysisAttributePanel.setOutputMarkupId(true);
                ((PageBase) getPage()).showMainPopup(roleAnalysisAttributePanel, target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);
    }

    private void buildMembersAnalysisButton() {

        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        RoleAnalysisOutlierDescriptionType descriptionType = modelObject.getDescriptionType();
        ObjectReferenceType objectRef = descriptionType.getObject();
        ObjectReferenceType parentRef = modelObject.getOutlierParent().getTargetObjectRef();
        QName type = objectRef.getType();
        if (type.equals(RoleType.COMPLEX_TYPE)) {
            roleAnalysisPanel(objectRef, parentRef);
        } else {
            userAnalysisPanel(objectRef, parentRef);
        }
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Details view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateToRoleDetails();
                    }
                };
            }

        });

        return items;
    }

    private void navigateToRoleDetails() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        RoleAnalysisOutlierDescriptionType descriptionType = modelObject.getDescriptionType();
        ObjectReferenceType objectRef = descriptionType.getObject();
        QName type = objectRef.getType();

        if (type.equals(RoleType.COMPLEX_TYPE)) {
            String roleOid = objectRef.getOid();
            PageParameters parameters = new PageParameters();
            parameters.add(OnePageParameterEncoder.PARAMETER, roleOid);

            Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                    .getObjectDetailsPage(RoleType.class);
            getPageBase().navigateToNext(detailsPageClass, parameters);
        }
    }

    private void roleAnalysisPanel(
            @NotNull ObjectReferenceType roleRef, ObjectReferenceType parentRef) {
        String title = "Members analysis";

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisOutlierTilePanel.ID_MEMBERS_ANALYSIS_PANEL,
                iconBuilder.build(),
                createStringResource(title)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
                ObjectReferenceType sessionRef = modelObject.getDescriptionType().getSession();

                PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService.getSessionTypeObject(
                        sessionRef.getOid(), task, task.getResult());

                if (sessionPrism == null) {
                    return;
                }

                List<RoleAnalysisAttributeDef> attributesForUserAnalysis = roleAnalysisService.resolveAnalysisAttributes(sessionPrism.asObjectable(), UserType.COMPLEX_TYPE);
                if (attributesForUserAnalysis == null || attributesForUserAnalysis.isEmpty()) {
                    return;
                }

                PrismObject<RoleType> prismRole = roleAnalysisService
                        .getRoleTypeObject(roleRef.getOid(), task, operationResult);
                if (prismRole == null) {
                    return;
                }

                PrismObject<UserType> userTypeObject = roleAnalysisService
                        .getUserTypeObject(parentRef.getOid(), task, task.getResult());

                if (userTypeObject == null) {
                    return;
                }

                Set<String> userPathToMark = roleAnalysisService.resolveUserValueToMark(userTypeObject, attributesForUserAnalysis);

                RoleAnalysisAttributeAnalysisResult roleAnalysisAttributeAnalysisResult = roleAnalysisService
                        .resolveRoleMembersAttribute(prismRole.getOid(), task, operationResult, attributesForUserAnalysis);

                RoleAnalysisAttributeAnalysisResult userAttributes = roleAnalysisService.resolveUserAttributes(userTypeObject, attributesForUserAnalysis);

                RoleAnalysisAttributeAnalysisResult compareAttributeResult = roleAnalysisService
                        .resolveSimilarAspect(userAttributes, roleAnalysisAttributeAnalysisResult);

                if (compareAttributeResult == null) {
                    return;
                }

                //TODO Support role mode
                RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Role analysis attribute panel"),
                        null, roleAnalysisAttributeAnalysisResult,
                        null, compareAttributeResult) {
                    @Override
                    protected @NotNull String getChartContainerStyle() {
                        return "height:30vh;";
                    }

                    @Override
                    public Set<String> getPathToMark() {
                        return userPathToMark;
                    }
                };

                roleAnalysisAttributePanel.setOutputMarkupId(true);
                ((PageBase) getPage()).showMainPopup(roleAnalysisAttributePanel, target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);

    }

    private void userAnalysisPanel(
            @NotNull ObjectReferenceType userRef,
            @NotNull ObjectReferenceType parentRef) {

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisOutlierTilePanel.ID_MEMBERS_ANALYSIS_PANEL,
                iconBuilder.build(),
                createStringResource("Members analysis")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                Task task = getPageBase().createSimpleTask("Load object");
                OperationResult operationResult = task.getResult();
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                PrismObject<RoleType> roleTypeObject = roleAnalysisService.getRoleTypeObject(parentRef.getOid(), task, task.getResult());

                if (roleTypeObject == null) {
                    return;
                }

                ObjectReferenceType sessionRef = getModelObject().getDescriptionType().getSession();

                PrismObject<RoleAnalysisSessionType> sessionPrism = roleAnalysisService.getSessionTypeObject(
                        sessionRef.getOid(), task, task.getResult());

                if (sessionPrism == null) {
                    return;
                }

                List<RoleAnalysisAttributeDef> attributesForRoleAnalysis = roleAnalysisService
                        .resolveAnalysisAttributes(sessionPrism.asObjectable(), RoleType.COMPLEX_TYPE);

                if (attributesForRoleAnalysis != null && attributesForRoleAnalysis.isEmpty()) {
                    return;
                }

                List<AttributeAnalysisStructure> attributeAnalysisStructures = roleAnalysisService
                        .userRolesAttributeAnalysis(attributesForRoleAnalysis, userRef.getOid(), task, operationResult);

                Set<String> rolePathToMark = roleAnalysisService.resolveRoleValueToMark(roleTypeObject, attributesForRoleAnalysis);

                RoleAnalysisAttributeChartPopupPanel detailsPanel = new RoleAnalysisAttributeChartPopupPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Analyzed members details panel"),
                        attributeAnalysisStructures, RoleAnalysisProcessModeType.ROLE) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }

                    @Override
                    protected Set<String> getRolePathToMark() {
                        return rolePathToMark;
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);

    }

}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.pattern;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_DETECTED_PATER_ID;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterOperationPanel.PARAM_TABLE_SETTING;
import static com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions.LOGGER;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.PageRole;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleApplicationDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.BusinessRoleDto;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetailsPopup;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class RoleAnalysisPatternTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisPatternTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_CREATE_CANDIDATE_BUTTON = "createCandidateButton";
    private static final String ID_EXPLORE_PATTERN_BUTTON = "explorePatternButton";
    private static final String ID_PROCESS_MODE = "mode";
    private static final String ID_USER_COUNT = "users-count";
    private static final String ID_ROLE_COUNT = "roles-count";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_DETAILS = "details";

    public RoleAnalysisPatternTilePanel(String id, IModel<RoleAnalysisPatternTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        initDetails();

        buildExploreButton();

        buildCandidateButton();

        initProcessModePanel();

        initFirstCountPanel();

        initSecondCountPanel();
    }

    private void initDetails() {
        RoleAnalysisPatternTileModel<T> modelObject = getModelObject();
        Double metric = modelObject.getPattern().getMetric();
        int relationCount = metric != null ? metric.intValue() : 0;
        String description = "Reduced relations: " + relationCount;

        Label details = new Label(ID_DETAILS, description);
        details.setOutputMarkupId(true);
        add(details);
    }

    private void initSecondCountPanel() {
        IconWithLabel processedObjectCount = new IconWithLabel(ID_ROLE_COUNT, () -> getModelObject().getRoleCount()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED;
            }
        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeAppender.replace(
                "title", () -> "Processed objects: " + getModelObject().getRoleCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        IconWithLabel clusterCount = new IconWithLabel(ID_USER_COUNT, () -> getModelObject().getUserCount()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED;
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "User count: " + getModelObject().getUserCount()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initProcessModePanel() {
        String processModeTitle = getModelObject().getProcessMode();
        IconWithLabel mode = new IconWithLabel(ID_PROCESS_MODE, () -> processModeTitle) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return "fa fa-cogs";
            }
        };
        mode.add(AttributeAppender.replace("title", () -> "Process mode: " + processModeTitle));
        mode.add(new TooltipBehavior());
        mode.setOutputMarkupId(true);
        add(mode);
    }

    private void initNamePanel() {
        RoleAnalysisPatternTileModel<T> modelObject = getModelObject();
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisPatternTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
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
                "catalog-tile-panel d-flex flex-column align-items-center border w-100 h-100 p-3"));

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

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    public void initStatusBar() {
        RoleAnalysisPatternTileModel<T> modelObject = getModelObject();
        String confidence;
        DetectedPattern pattern;
        if (modelObject != null && modelObject.getPattern() != null) {
            pattern = modelObject.getPattern();
            confidence = String.format("%.2f", pattern.getItemsConfidence()) + "%";
        } else {
            EmptyPanel emptyPanel = new EmptyPanel(ID_STATUS_BAR);
            emptyPanel.setOutputMarkupId(true);
            add(emptyPanel);
            return;
        }

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

        DetectedPattern finalPattern = pattern;
        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(ID_STATUS_BAR, iconBuilder.build(),
                Model.of(confidence)) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of(finalPattern));
                ((PageBase) getPage()).showMainPopup(component, ajaxRequestTarget);
            }
        };

        objectButton.titleAsLabel(true);
        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm rounded-pill"));
        objectButton.add(AttributeAppender.append("style", "width:100px"));
        objectButton.setOutputMarkupId(true);
        add(objectButton);
    }

    private void buildExploreButton() {
        DetectedPattern pattern = getModelObject().getPattern();
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisPatternTilePanel.ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.explore.button.title")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                explorePatternPerform(pattern);
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

    private void buildCandidateButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_PLUS_CIRCLE, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisPatternTilePanel.ID_CREATE_CANDIDATE_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleMining.button.title.candidate")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                createCandidatePerform(target);
            }

            @Override
            protected void onError(@NotNull AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeAppender.append("class", "btn btn-success btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);
    }

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

        Set<RoleType> candidateInducements = new HashSet<>();

        for (String roleOid : roles) {
            PrismObject<RoleType> roleObject = roleAnalysisService
                    .getRoleTypeObject(roleOid, task, result);
            if (roleObject != null) {
                candidateInducements.add(roleObject.asObjectable());
            }
        }

        PrismObject<RoleType> businessRole = roleAnalysisService
                .generateBusinessRole(new HashSet<>(), PolyStringType.fromOrig(""));

        List<BusinessRoleDto> roleApplicationDtos = new ArrayList<>();

        for (String userOid : users) {
            PrismObject<UserType> userObject = roleAnalysisService
                    .getUserTypeObject(userOid, task, result);
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

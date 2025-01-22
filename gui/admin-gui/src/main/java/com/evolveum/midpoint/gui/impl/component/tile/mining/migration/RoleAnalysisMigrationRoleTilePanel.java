/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile.mining.migration;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.confidenceBasedTwoColor;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.ProgressBarSecondStyleDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.MetricValuePanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class RoleAnalysisMigrationRoleTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisMigratedRoleTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "objectTitle";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_PROGRESS_BAR = "progress-bar";
    private static final String ID_USER_COUNT = "users-count";
    private static final String ID_ROLE_COUNT = "roles-count";
    private static final String ID_LOCATION = "location";
    private static final String ID_MIGRATION_BUTTON = "migration-button";

    public RoleAnalysisMigrationRoleTilePanel(String id, IModel<RoleAnalysisMigratedRoleTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {

        initDefaultStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        initProgressBar();

        initLocationButtons();

        buildExploreButton();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initLocationButtons() {
        ObjectReferenceType clusterRef = getModelObject().getClusterRef();
        ObjectReferenceType sessionRef = getModelObject().getSessionRef();

        MetricValuePanel panel = new MetricValuePanel(ID_LOCATION) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.title.panel.location"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                return label;
            }

            @Override
            protected Component getValueComponent(String id) {
                RepeatingView view = new RepeatingView(id);
                view.setOutputMarkupId(true);
                AjaxLinkPanel sessionLink = new AjaxLinkPanel(view.newChildId(), Model.of(sessionRef.getTargetName().getOrig())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, sessionRef.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class, parameters);
                    }
                };

                sessionLink.setOutputMarkupId(true);
                sessionLink.add(AttributeModifier.append(STYLE_CSS, "max-width:100px"));
                sessionLink.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                view.add(sessionLink);

                Label separator = new Label(view.newChildId(), "/");
                separator.setOutputMarkupId(true);
                view.add(separator);

                AjaxLinkPanel clusterLink = new AjaxLinkPanel(view.newChildId(), Model.of(clusterRef.getTargetName().getOrig())) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, clusterRef.getOid());
                        getPageBase().navigateToNext(PageRoleAnalysisCluster.class, parameters);
                    }
                };
                clusterLink.setOutputMarkupId(true);
                clusterLink.add(AttributeModifier.append(STYLE_CSS, "max-width:100px"));
                clusterLink.add(AttributeModifier.append(CLASS_CSS, TEXT_TRUNCATE));
                view.add(clusterLink);
                return view;
            }
        };

        panel.setOutputMarkupId(true);
        add(panel);

    }

    private void initProgressBar() {
        IModel<ProgressBarSecondStyleDto> model = () -> {
            double finalProgress = 100;
            String colorClass = confidenceBasedTwoColor(finalProgress);
            ProgressBarSecondStyleDto dto = new ProgressBarSecondStyleDto(finalProgress, colorClass);
            dto.setBarTitle("Migration status");
            return dto;
        };

        ProgressBarSecondStyle progressBar = new ProgressBarSecondStyle(ID_PROGRESS_BAR, model) {

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssStyle() {
                return "border-radius: 3px; height:13px;";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getProgressBarContainerCssClass() {
                return "col-12 pl-0 pr-0";
            }


        };
        progressBar.setOutputMarkupId(true);
        //TODOprogressBar.add(AttributeModifier.replace(TITLE_CSS, () -> "Attribute confidence: " + finalProgress + "%"));
        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    private void initSecondCountPanel() {
        MetricValuePanel panel = new MetricValuePanel(ID_ROLE_COUNT) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.induced.roles"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, "text-muted"));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size: 16px"));
                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                String inducementsCount = RoleAnalysisMigrationRoleTilePanel.this.getModelObject().getInducementsCount();
                Label inducementPanel = new Label(id, () -> inducementsCount) {
                };

                inducementPanel.setOutputMarkupId(true);
                inducementPanel.add(AttributeModifier.replace(TITLE_CSS, () -> "Induced roles count: " + inducementsCount));
                inducementPanel.add(new TooltipBehavior());
                return inducementPanel;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initFirstCountPanel() {
        MetricValuePanel panel = new MetricValuePanel(ID_USER_COUNT) {
            @Override
            protected @NotNull Component getTitleComponent(String id) {
                Label label = new Label(id, createStringResource("RoleAnalysis.tile.panel.user.members"));
                label.setOutputMarkupId(true);
                label.add(AttributeModifier.append(CLASS_CSS, TEXT_MUTED));
                label.add(AttributeModifier.append(STYLE_CSS, "font-size: 16px"));

                return label;
            }

            @Override
            protected @NotNull Component getValueComponent(String id) {
                String membersCount = RoleAnalysisMigrationRoleTilePanel.this.getModelObject().getMembersCount();
                Label memberPanel = new Label(id, () -> membersCount) {
                };

                memberPanel.setOutputMarkupId(true);
                memberPanel.add(AttributeModifier.replace(TITLE_CSS, () -> "User members count: " + membersCount));
                memberPanel.add(new TooltipBehavior());
                return memberPanel;
            }
        };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    private void initNamePanel() {
        AjaxLinkPanel objectTitle = new AjaxLinkPanel(ID_TITLE, () -> getModelObject().getName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisMigratedRoleTileModel<T> modelObject = RoleAnalysisMigrationRoleTilePanel.this.getModelObject();
                String oid = modelObject.getRole().getOid();
                dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
            }
        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeModifier.replace(STYLE_CSS, "font-size:18px"));
        objectTitle.add(AttributeModifier.replace(TITLE_CSS, () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initDefaultStyle() {
        setOutputMarkupId(true);
        add(AttributeModifier.append(CLASS_CSS,
                "bg-white d-flex flex-column align-items-center elevation-1 rounded w-100 h-100 p-0"));
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
        barMenu.add(AttributeModifier.replace(TITLE_CSS,
                createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void exploreRolePerform() {
        String clusterOid = getModelObject().getClusterRef().getOid();
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");

        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisClusterType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public void initStatusBar() {
        RoleAnalysisMigratedRoleTileModel<T> modelObject = getModelObject();
        String status = modelObject.getStatus();

        Label statusBar = new Label(ID_STATUS_BAR, Model.of(status));
        statusBar.add(AttributeModifier.append(CLASS_CSS, "badge badge-pill badge-info"));
        statusBar.add(AttributeModifier.append(STYLE_CSS, "width: 80px"));
        statusBar.setOutputMarkupId(true);
        add(statusBar);
    }

    private void buildExploreButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, IconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisMigrationRoleTilePanel.ID_MIGRATION_BUTTON,
                iconBuilder.build(),
                createStringResource("RoleAnalysis.title.panel.explore.in.cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                exploreRolePerform();
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        migrationButton.titleAsLabel(true);
        migrationButton.setOutputMarkupId(true);
        migrationButton.add(AttributeModifier.append(CLASS_CSS, "btn btn-default btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();

        RoleAnalysisMigratedRoleTileModel<T> modelObject = RoleAnalysisMigrationRoleTilePanel.this.getModelObject();

        items.add(new InlineMenuItem(createStringResource("Explore in the cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, modelObject.getClusterRef().getOid());
                        parameters.add("panelId", "clusterDetails");

                        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                .getObjectDetailsPage(RoleAnalysisClusterType.class);
                        getPageBase().navigateToNext(detailsPageClass, parameters);
                    }
                };
            }

        });

        items.add(new InlineMenuItem(createStringResource("RoleAnalysis.tile.panel.details.view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        String oid = getModelObject().getRole().getOid();
                        dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
                    }
                };
            }

        });

        return items;
    }
}

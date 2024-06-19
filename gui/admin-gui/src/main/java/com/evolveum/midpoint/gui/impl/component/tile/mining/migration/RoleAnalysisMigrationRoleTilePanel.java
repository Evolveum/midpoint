/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile.mining.migration;

import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class RoleAnalysisMigrationRoleTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisMigratedRoleTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXPLORE_PATTERN_BUTTON = "explorePatternButton";
    private static final String ID_PROCESS_MODE = "mode";
    private static final String ID_USER_COUNT = "users-count";
    private static final String ID_ROLE_COUNT = "roles-count";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";

    public RoleAnalysisMigrationRoleTilePanel(String id, IModel<RoleAnalysisMigratedRoleTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        initDescriptionPanel();

        buildExploreButton();

        initProcessModePanel();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initSecondCountPanel() {
        IconWithLabel processedObjectCount = new IconWithLabel(ID_ROLE_COUNT, () -> getModelObject().getInducementsCount()) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return "fe fe-assignment";
            }
        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeAppender.replace("title", () -> "Processed objects: " +
                getModelObject().getInducementsCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        IconWithLabel clusterCount = new IconWithLabel(ID_USER_COUNT, () -> getModelObject().getMembersCount()) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return "fa fa-users";
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "User count: " +
                getModelObject().getMembersCount()));
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

    private void initDescriptionPanel() {
        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getCreateDate());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getCreateDate()));
        description.add(new TooltipBehavior());
        add(description);
    }

    private void initNamePanel() {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisMigrationRoleTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                RoleAnalysisMigratedRoleTileModel<T> modelObject = RoleAnalysisMigrationRoleTilePanel.this.getModelObject();
                String oid = modelObject.getRole().getOid();
                dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
            }

        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeAppender.replace("style", "font-size:20px"));
        objectTitle.add(AttributeAppender.replace("title", () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class",
                "catalog-tile-panel d-flex flex-column align-items-center border w-100 h-100 p-3"));

        add(AttributeAppender.append("style", "width:25%"));
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

    private void exploreRolePerform() {
        String clusterOid = getModelObject().getClusterOid();
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, clusterOid);
        parameters.add("panelId", "clusterDetails");

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
        RoleAnalysisMigratedRoleTileModel<T> modelObject = getModelObject();
        String status = modelObject.getStatus();

        Label statusBar = new Label(ID_STATUS_BAR, Model.of(status));
        statusBar.add(AttributeAppender.append("class", "badge badge-pill badge-info"));
        statusBar.add(AttributeAppender.append("style", "width: 80px"));
        statusBar.setOutputMarkupId(true);
        add(statusBar);
    }

    private void buildExploreButton() {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton migrationButton = new AjaxCompositedIconSubmitButton(
                RoleAnalysisMigrationRoleTilePanel.ID_EXPLORE_PATTERN_BUTTON,
                iconBuilder.build(),
                createStringResource("Explore in the cluster")) {
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
        migrationButton.add(AttributeAppender.append("class", "btn btn-primary btn-sm"));
        migrationButton.setOutputMarkupId(true);
        add(migrationButton);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Explore in the cluster")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        PageParameters parameters = new PageParameters();
                        parameters.add(OnePageParameterEncoder.PARAMETER, createModel().getObject().getClusterOid());
                        parameters.add("panelId", "clusterDetails");

                        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                                .getObjectDetailsPage(RoleAnalysisClusterType.class);
                        getPageBase().navigateToNext(detailsPageClass, parameters);
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
                        String oid = getModelObject().getRole().getOid();
                        dispatchToObjectDetailsPage(RoleType.class, oid, getPageBase(), true);
                    }
                };
            }

        });

        return items;
    }
}

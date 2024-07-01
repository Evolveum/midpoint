/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile.mining.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisSessionTile<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DENSITY = "density";

    private static final String ID_PROCESS_MODE = "mode";
    private static final String ID_CLUSTER_COUNT = "clusterCount";
    private static final String ID_PROCESSED_OBJECTS_COUNT = "processedObjectCount";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_STATUS_BAR = "status";

    private static final String DOT_CLASS = RoleAnalysisTilePanel.class.getName() + ".";
    private static final String OP_DELETE_SESSION = DOT_CLASS + "deleteSession";

    public RoleAnalysisTilePanel(String id, IModel<RoleAnalysisSessionTile<T>> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        initDescriptionPanel();

        initDensityProgressPanel(getModelObject().getDensity());

        initProcessModePanel();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initSecondCountPanel() {
        IconWithLabel processedObjectCount = new IconWithLabel(ID_PROCESSED_OBJECTS_COUNT, () -> getModelObject().getProcessedObjectCount()) {
            @Override
            public String getIconCssClass() {
                RoleAnalysisProcessModeType processMode = RoleAnalysisTilePanel.this.getModelObject().getProcessMode();
                if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                    return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
                }
                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
            }
        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeAppender.replace("title", () -> "Processed objects: " + getModelObject().getProcessedObjectCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        IconWithLabel clusterCount = new IconWithLabel(ID_CLUSTER_COUNT, () -> getModelObject().getClusterCount()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeAppender.replace("title", () -> "Cluster count: " + getModelObject().getClusterCount()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initProcessModePanel() {
        String processModeTitle = getModelObject().getProcessMode().value() + "/"
                + (getModelObject().getCategory() == null ? "N/A" : getModelObject().getCategory().value());
        IconWithLabel mode = new IconWithLabel(ID_PROCESS_MODE, () -> processModeTitle) {
            @Override
            public String getIconCssClass() {
                return "fa fa-cogs";
            }
        };
        mode.add(AttributeAppender.replace("title", () -> "Process mode: " + processModeTitle));
        mode.add(new TooltipBehavior());
        mode.setOutputMarkupId(true);
        add(mode);
    }

    private void initDescriptionPanel() {
        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getDescription()));
        description.add(new TooltipBehavior());
        add(description);
    }

    private void initNamePanel() {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                RoleAnalysisTilePanel.this.onDetails();
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

            @Override
            protected String getSpecialButtonClass() {
                return " p-0 ";
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace("title", createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class", "catalog-tile-panel "
                + "d-flex flex-column align-items-center"
                + " bordered w-100 h-100 p-3 elevation-1"));

        add(AttributeAppender.append("style", "width:25%"));
    }

    protected void onDetails() {
        dispatchToObjectDetailsPage(RoleAnalysisSessionType.class, getModelObject().getOid(), getPageBase(), true);
    }

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    private void initDensityProgressPanel(Double meanDensity) {
        if (meanDensity == null) {
            WebMarkupContainer progressBar = new WebMarkupContainer(RoleAnalysisTilePanel.ID_DENSITY);
            progressBar.setVisible(false);
            add(progressBar);
            return;
        }

        String colorClass = densityBasedColor(meanDensity);

        ProgressBar progressBar = new ProgressBar(RoleAnalysisTilePanel.ID_DENSITY) {

            @Override
            public double getActualValue() {
                return meanDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Override
            public String getBarTitle() {
                return "Density";
            }
        };
        progressBar.setOutputMarkupId(true);
        progressBar.add(AttributeModifier.replace("title", () -> "Density: " + meanDensity));
        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    public void initStatusBar() {

        ObjectReferenceType finalTaskRef = getModelObject().getTaskRef();
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_STATUS_BAR, Model.of(getModelObject().getStateString())) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                if (finalTaskRef != null && finalTaskRef.getOid() != null) {
                    DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, finalTaskRef.getOid(),
                            this, true);
                }
            }
        };
        ajaxLinkPanel.add(AttributeModifier.replace("title", () -> getModelObject().getStateString()));
        ajaxLinkPanel.add(new TooltipBehavior());
        String buttonClass = resolveButtonClass(getModelObject().getStatus());

        ajaxLinkPanel.add(AttributeModifier.replace("class", "rounded-pill align-items-center"
                + " d-flex flex-column justify-content-center btn btn-sm " + buttonClass));
        ajaxLinkPanel.add(AttributeModifier.replace("style", "height: 25px;"));
        ajaxLinkPanel.setEnabled(getModelObject().getTaskRef() != null);
        ajaxLinkPanel.setOutputMarkupId(true);
        add(ajaxLinkPanel);
    }

    @NotNull
    private static String resolveButtonClass(@NotNull RoleAnalysisOperationStatus operationStatus) {
        OperationResultStatusType status = operationStatus.getStatus();
        String message = operationStatus.getMessage();
        if (status == null) {
            return "btn-outline-secondary";
        }
        String buttonClass = "btn-outline-secondary ";
        if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
            buttonClass = "btn-outline-warning ";
        } else if (status.equals(OperationResultStatusType.FATAL_ERROR)
                || status.equals(OperationResultStatusType.PARTIAL_ERROR)) {
            buttonClass = "btn-outline-danger ";
        } else if (status.equals(OperationResultStatusType.SUCCESS) && message.contains("7/7")) {
            buttonClass = "btn-outline-success";
        } else if (status.equals(OperationResultStatusType.SUCCESS)) {
            buttonClass = "btn-outline-primary";
        }
        return buttonClass;
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Task task = getPageBase().createSimpleTask(OP_DELETE_SESSION);
                        OperationResult result = task.getResult();

                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                        String parentOid = getModelObject().getOid();
                        roleAnalysisService
                                .deleteSession(parentOid,
                                        task, result);
                        target.add(getPageBase().getFeedbackPanel());
                    }
                };
            }

        });

        return items;
    }
}

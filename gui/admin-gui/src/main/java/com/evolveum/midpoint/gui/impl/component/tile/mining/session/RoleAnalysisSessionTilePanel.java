/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.tile.mining.session;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.*;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.Badge;

import com.evolveum.midpoint.gui.api.component.BadgePanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.ProgressBarDto;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisSessionTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisSessionTileModel<T>> {

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
    private static final String ID_ANALYSIS_MODE = "analysisMode";

    private static final String DOT_CLASS = RoleAnalysisSessionTilePanel.class.getName() + ".";
    private static final String OP_DELETE_SESSION = DOT_CLASS + "deleteSession";

    public RoleAnalysisSessionTilePanel(String id, IModel<RoleAnalysisSessionTileModel<T>> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initAnalysisModePanel();
        initToolBarPanel();

        initNamePanel();

        initDescriptionPanel();

        initDensityProgressPanel();

        initProcessModePanel();

        initFirstCountPanel();

        initSecondCountPanel();

    }

    private void initAnalysisModePanel() {
        RoleAnalysisProcedureType procedureType = getModelObject().getProcedureType();
        String badgeText = "";
        String badgeCss = "";
        if (procedureType == null) {
            badgeText = "N/A";
            badgeCss = "badge badge-danger";
        } else if (procedureType.equals(RoleAnalysisProcedureType.ROLE_MINING)) {
            badgeText = "role mining";
            // temporary disable to many colors
//            badgeCss = "badge badge-success";
            badgeCss = "badge badge-primary";
        } else if (procedureType.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
            badgeText = "outlier detection";
            badgeCss = "badge badge-primary";
        }

        Badge badge = new Badge(badgeCss, badgeText);
        BadgePanel status = new BadgePanel(ID_ANALYSIS_MODE, Model.of(badge));
        status.setOutputMarkupId(true);
        add(status);
    }

    private void initSecondCountPanel() {
        IconWithLabel processedObjectCount = new IconWithLabel(ID_PROCESSED_OBJECTS_COUNT, () -> getModelObject().getProcessedObjectCount()) {
            @Override
            public String getIconCssClass() {
                RoleAnalysisProcessModeType processMode = RoleAnalysisSessionTilePanel.this.getModelObject().getProcessMode();
                if (processMode.equals(RoleAnalysisProcessModeType.USER)) {
                    return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
                }
                return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pl-1 text-sm " + TEXT_TRUNCATE;
            }

        };
        processedObjectCount.setOutputMarkupId(true);
        processedObjectCount.add(AttributeModifier.replace(TITLE_CSS, () -> "Processed objects: " + getModelObject().getProcessedObjectCount()));
        processedObjectCount.add(new TooltipBehavior());
        add(processedObjectCount);
    }

    private void initFirstCountPanel() {
        IconWithLabel clusterCount = new IconWithLabel(ID_CLUSTER_COUNT, () -> getModelObject().getClusterCount()) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pl-1 text-sm " + TEXT_TRUNCATE;
            }

        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(AttributeModifier.replace(TITLE_CSS, () -> "Cluster count: " + getModelObject().getClusterCount()));
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initProcessModePanel() {
        String processModeTitle = getModelObject().getProcessMode().value() + "/"
                + (getModelObject().getCategory() == null ? "N/A" : getModelObject().getCategory().value());
        IconWithLabel mode = new IconWithLabel(ID_PROCESS_MODE, () -> processModeTitle) {
            @Contract(pure = true)
            @Override
            public @NotNull String getIconCssClass() {
                return "fa fa-cogs";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pl-1 text-sm text-truncate";
            }

        };
        mode.add(AttributeModifier.replace(TITLE_CSS, () -> "Process mode: " + processModeTitle));
        mode.add(new TooltipBehavior());
        mode.setOutputMarkupId(true);
        add(mode);
    }

    private void initDescriptionPanel() {
        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(AttributeModifier.replace(TITLE_CSS, () -> getModelObject().getDescription()));
        description.add(new TooltipBehavior());
        add(description);
    }

    private void initNamePanel() {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisSessionTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pl-2 text-truncate";
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                RoleAnalysisSessionTilePanel.this.onDetails();
            }

        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeModifier.replace(STYLE_CSS, "font-size:18px"));
        objectTitle.add(AttributeModifier.append(CLASS_CSS, ""));
        objectTitle.add(AttributeModifier.replace(TITLE_CSS, () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initToolBarPanel() {
        DropdownButtonPanel barMenu = new DropdownButtonPanel(ID_BUTTON_BAR, new DropdownButtonDto(
                null, "fa-ellipsis-v ml-1", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " px-1 py-0 "; /* p-0 */
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace(TITLE_CSS, createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeModifier.append(CLASS_CSS, "bg-white "
                + "d-flex flex-column align-items-center"
                + " rounded w-100 h-100 p-3 card-shadow"));

        add(AttributeModifier.append(STYLE_CSS, "")); /* width:25%; */
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

    private void initDensityProgressPanel() {

        IModel<ProgressBarDto> model = () -> {
            double value = getModelObject().getProgressBarValue();
            String colorClass = getModelObject().getProgressBarColor();
            String title = getModelObject().getProgressBarTitle();
            ProgressBarDto progressBarDto = new ProgressBarDto(value, colorClass, title);
            progressBarDto.setBarToolTip(title + " of " + value);
            return progressBarDto;
        };

        ProgressBar progressBar = new ProgressBar(RoleAnalysisSessionTilePanel.ID_DENSITY, model);

        progressBar.setOutputMarkupId(true);

        progressBar.add(new TooltipBehavior());
        add(progressBar);
    }

    public void initStatusBar() {

        String state;
        RoleAnalysisOperationStatus status;
        ObjectReferenceType taskRef;
        if (getModelObject() != null) {
            state = getModelObject().getStateString();
            status = getModelObject().getStatus();
            taskRef = getModelObject().getTaskRef();
        } else {
            state = "unknown";
            status = null;
            taskRef = null;
        }
        ObjectReferenceType finalTaskRef = getModelObject().getTaskRef();
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(ID_STATUS_BAR, Model.of(state)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                super.onClick(target);
                if (finalTaskRef != null && finalTaskRef.getOid() != null) {
                    DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, finalTaskRef.getOid(),
                            this, true);
                }
            }
        };
        ajaxLinkPanel.add(AttributeModifier.replace(TITLE_CSS, () -> state));
        ajaxLinkPanel.add(new TooltipBehavior());
        String buttonClass = resolveButtonClass(status);

        ajaxLinkPanel.add(AttributeModifier.replace(CLASS_CSS, "rounded-pill align-items-center"
                + " d-flex flex-column justify-content-center btn btn-sm " + buttonClass));
        ajaxLinkPanel.add(AttributeModifier.replace(STYLE_CSS, "height: 25px;"));
        ajaxLinkPanel.setEnabled(taskRef != null);
        ajaxLinkPanel.setOutputMarkupId(true);
        add(ajaxLinkPanel);
    }

    @NotNull
    private static String resolveButtonClass(@Nullable RoleAnalysisOperationStatus operationStatus) {
        if (operationStatus == null) {
            return "btn-outline-secondary";
        }

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
            buttonClass = "btn-outline-primary";
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

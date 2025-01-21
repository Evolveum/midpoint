/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.component.InlineOperationalButtonsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.RoleAnalysisReconfigureSessionPopupPanel;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Collection;

public class RoleAnalysisSessionOperationButtonPanel extends InlineOperationalButtonsPanel<RoleAnalysisSessionType> {

    private static final Trace LOGGER = TraceManager.getTrace(RoleAnalysisSessionOperationButtonPanel.class);

    private static final String DOT_CLASS = RoleAnalysisSessionOperationButtonPanel.class.getName() + ".";
    private static final String OP_PERFORM_CLUSTERING = DOT_CLASS + "performClustering";

    private final AssignmentHolderDetailsModel<RoleAnalysisSessionType> objectDetailsModels;

    public RoleAnalysisSessionOperationButtonPanel(
            String id,
            LoadableModel<PrismObjectWrapper<RoleAnalysisSessionType>> wrapperModel,
            AssignmentHolderDetailsModel<RoleAnalysisSessionType> objectDetailsModels) {
        super(id, wrapperModel);
        this.objectDetailsModels = objectDetailsModels;
    }

    @Override
    protected IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<RoleAnalysisSessionType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisSessionOperationButtonPanel.delete");
    }

    @Override
    protected IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<RoleAnalysisSessionType> modelObject) {
        return () -> LocalizationUtil.translate("RoleAnalysisSessionOperationButtonPanel.save");
    }

    @Override
    protected IModel<String> getTitle() {
        return createStringResource("RoleAnalysis.page.session.title");
    }

    @Override
    protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
        initEditConfigurationButton(rightButtonsView);

        initRebuildButton(rightButtonsView);
    }

    private void initEditConfigurationButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_EDIT_MENU_ITEM,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton editConfigurationButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("PageRoleAnalysisSession.button.configure")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                RoleAnalysisReconfigureSessionPopupPanel detailsPanel = new RoleAnalysisReconfigureSessionPopupPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        objectDetailsModels);

                getPageBase().showMainPopup(detailsPanel, target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        editConfigurationButton.titleAsLabel(true);
        editConfigurationButton.setOutputMarkupId(true);
        editConfigurationButton.add(AttributeAppender.append("class", "btn btn-default"));
        repeatingView.add(editConfigurationButton);
    }

    private void initRebuildButton(@NotNull RepeatingView repeatingView) {
        CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(GuiStyleConstants.CLASS_REFRESH,
                LayeredIconCssStyle.IN_ROW_STYLE);
        AjaxCompositedIconSubmitButton rebuildButton = new AjaxCompositedIconSubmitButton(repeatingView.newChildId(),
                iconBuilder.build(),
                ((PageBase) getPage()).createStringResource("PageRoleAnalysisSession.button.rebuild")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                clusteringPerform(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(((PageBase) getPage()).getFeedbackPanel());
            }
        };
        rebuildButton.titleAsLabel(true);
        rebuildButton.setOutputMarkupId(true);
        rebuildButton.add(AttributeAppender.append("class", "btn btn-primary"));
        repeatingView.add(rebuildButton);

        Form<?> form = rebuildButton.findParent(Form.class);
        if (form != null) {
            form.setDefaultButton(rebuildButton);
        }
    }

    private void clusteringPerform(@NotNull AjaxRequestTarget target) {

        Task task = getPageBase().createSimpleTask(OP_PERFORM_CLUSTERING);
        OperationResult result = task.getResult();

        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();

        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

        roleAnalysisService.deleteSessionTask(session.getOid(), task, result);

        try {
            ModelService modelService = getPageBase().getModelService();

            Collection<ObjectDelta<? extends ObjectType>> objectDeltas = objectDetailsModels.collectDeltas(result);
            if (objectDeltas != null && !objectDeltas.isEmpty()) {
                modelService.executeChanges(objectDeltas, null, task, result);
            }
        } catch (CommonException e) {
            LOGGER.error("Couldn't execute changes on RoleAnalysisSessionType object: {}", session.getOid(), e);
        }


        TaskType performTask = new TaskType(); //TODO rerun existing or create new?
        roleAnalysisService.executeClusteringTask(getPageBase().getModelInteractionService(), session.asPrismObject(), performTask,
                task, result);

        if (result.isWarning()) {
            warn(result.getMessage());
            target.add(getPageBase().getFeedbackPanel());
        } else {
            result.recordSuccessIfUnknown();
            setResponsePage(PageRoleAnalysis.class);
            ((PageBase) getPage()).showResult(result);
            target.add(getFeedbackPanel());
        }

    }
}

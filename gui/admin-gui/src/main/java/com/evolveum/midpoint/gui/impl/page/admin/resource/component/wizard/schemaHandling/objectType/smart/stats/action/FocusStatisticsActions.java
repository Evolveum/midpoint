/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.SmartStatisticsPanel;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.FocusObjectStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.dialog.steper.PopupStepperModel;
import com.evolveum.midpoint.web.component.dialog.steper.PopupStepperPanel;
import com.evolveum.midpoint.web.component.dialog.steper.step.SmartTaskProgressStepPanel;
import com.evolveum.midpoint.web.component.dialog.steper.step.ThreadSetupPopupStepPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.loadTask;

/**
 * Utility class encapsulating the UI workflow for loading and regenerating
 * Smart Integration focus object statistics (e.g. UserType).
 */
public final class FocusStatisticsActions {

    private static final String OPERATION_GET_FOCUS_STATISTICS = "getFocusObjectStatistics";
    private static final String OPERATION_REGENERATE_FOCUS_STATISTICS = "regenerateFocusObjectStatistics";

    private FocusStatisticsActions() {
    }

    public static void handleClick(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable ItemPathType preSelectedAttribute,
            boolean forceRegeneration) {

        Task task = pageBase.createSimpleTask(OPERATION_GET_FOCUS_STATISTICS);

        try {
            if (!forceRegeneration) {
                GenericObjectType latestStatistics =
                        smartIntegrationService.getLatestFocusObjectStatistics(
                                focusObjectTypeName,
                                resourceOid,
                                kind,
                                intent,
                                task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(
                            target,
                            pageBase,
                            latestStatistics,
                            preSelectedAttribute,
                            focusObjectTypeName,
                            resourceOid,
                            kind,
                            intent);
                    return;
                }
            }

            showProgressExecutorPopup(
                    target,
                    pageBase,
                    smartIntegrationService,
                    focusObjectTypeName,
                    resourceOid,
                    kind,
                    intent,
                    preSelectedAttribute);

        } catch (CommonException e) {
            pageBase.error("Couldn't get focus statistics for " + focusObjectTypeName + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    private static void showStatisticsPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull GenericObjectType statisticsObject,
            @Nullable ItemPathType preSelectedAttribute,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                FocusObjectStatisticsTypeUtil.getFocusObjectStatisticsRequired(statisticsObject);

        SmartStatisticsPanel panel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(),
                () -> statistics,
                resourceOid,
                ResourceObjectTypeIdentification.of(kind, intent),
                focusObjectTypeName) {

            @Override
            protected ItemPathType getDefaultSelectedAttributePath() {
                return preSelectedAttribute;
            }
        };

        pageBase.replaceMainPopup(panel, target);
    }

    private static void showProgressExecutorPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable ItemPathType preSelectedAttribute) {

        IModel<Integer> threadsModel = Model.of(4);
        IModel<String> taskOidModel = Model.of();

        String displayName = focusObjectTypeName.getLocalPart();
        ThreadSetupPopupStepPanel threadStep = new ThreadSetupPopupStepPanel(threadsModel) {

            @Override
            public boolean onNextPerformed(AjaxRequestTarget target) {
                IModel<TaskType> taskModel = regenerateFocusObjectStatistics(
                        target,
                        pageBase,
                        smartIntegrationService,
                        threadsModel.getObject(),
                        focusObjectTypeName,
                        resourceOid,
                        kind,
                        intent);

                if (taskModel == null || taskModel.getObject() == null) {
                    return false;
                }

                taskOidModel.setObject(taskModel.getObject().getOid());
                return true;
            }
        };

        IModel<TaskType> taskModel = new LoadableDetachableModel<>() {
            @Override
            protected TaskType load() {
                String oid = taskOidModel.getObject();
                return oid != null ? loadTask(pageBase, oid) : null;
            }
        };

        SmartTaskProgressStepPanel progressStep =
                new SmartTaskProgressStepPanel(
                        pageBase.createStringResource("FocusStatisticsButton.regeneratingStatistics"),
                        pageBase.createStringResource("FocusStatisticsButton.regeneratingStatistics.subText", displayName),
                        taskModel) {

                    @Override
                    protected boolean showResultAfterCompletion() {
                        return true;
                    }

                    @Override
                    protected void onShowResults(AjaxRequestTarget target) {
                        onProgressFinished(
                                target,
                                pageBase,
                                smartIntegrationService,
                                focusObjectTypeName,
                                resourceOid,
                                kind,
                                intent,
                                preSelectedAttribute);
                    }
                };

        PopupStepperModel model = new PopupStepperModel(List.of(threadStep, progressStep));

        PopupStepperPanel popup = new PopupStepperPanel(
                pageBase.getMainPopupBodyId(),
                Model.of(model)) {
            @Override
            public IModel<String> getTitle() {
                return pageBase.createStringResource("FocusStatisticsButton.generatingStatistics.title", displayName);
            }

            @Override
            public @NotNull IModel<String> getTitleIconClass() {
                return Model.of("fa fa-chart-bar");
            }
        };

        pageBase.replaceMainPopup(popup, target);
    }

    private static @Nullable IModel<TaskType> regenerateFocusObjectStatistics(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            Integer threads,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_FOCUS_STATISTICS);

        try {
            String taskOid = smartIntegrationService.regenerateFocusObjectStatistics(
                    focusObjectTypeName,
                    resourceOid,
                    kind,
                    intent,
                    threads,
                    task,
                    task.getResult());

            return new LoadableDetachableModel<>() {

                @Override
                protected TaskType load() {
                    return loadTask(pageBase, taskOid);
                }
            };

        } catch (CommonException e) {
            pageBase.error("Couldn't regenerate focus statistics for " + focusObjectTypeName + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
            return null;

        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    private static void onProgressFinished(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @Nullable ItemPathType preSelectedAttribute) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_FOCUS_STATISTICS);

        try {
            GenericObjectType latestStatistics =
                    smartIntegrationService.getLatestFocusObjectStatistics(
                            focusObjectTypeName,
                            resourceOid,
                            kind,
                            intent,
                            task.getResult());

            if (latestStatistics == null) {
                pageBase.warn("Statistics computation finished, but no statistics object was found.");
                target.add(pageBase.getFeedbackPanel());
                return;
            }

            showStatisticsPopup(
                    target,
                    pageBase,
                    latestStatistics,
                    preSelectedAttribute,
                    focusObjectTypeName,
                    resourceOid,
                    kind,
                    intent);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated focus statistics for "
                    + focusObjectTypeName + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }
}

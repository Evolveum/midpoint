/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.SmartStatisticsPanel;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeUtil;
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

import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.loadTask;

public final class ObjectTypeStatisticsActions {

    private static final String OPERATION_GET_STATISTICS = "getObjectTypeStatistics";
    private static final String OPERATION_REGENERATE_STATISTICS = "regenerateObjectTypeStatistics";

    private ObjectTypeStatisticsActions() {
    }

    public static void handleClick(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @Nullable ItemPathType preSelectedRefAttribute,
            boolean forceRegeneration) {

        Task task = pageBase.createSimpleTask(OPERATION_GET_STATISTICS);

        try {
            if (!forceRegeneration) {
                GenericObjectType latestStatistics =
                        smartIntegrationService.getLatestObjectTypeStatistics(
                                resourceOid,
                                objectTypeIdentification.getKind().value(),
                                objectTypeIdentification.getIntent(),
                                task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(
                            target,
                            pageBase,
                            latestStatistics,
                            resourceOid,
                            objectTypeIdentification,
                            preSelectedRefAttribute);
                    return;
                }
            }

            showProgressExecutorPopup(
                    target,
                    pageBase,
                    smartIntegrationService,
                    resourceOid,
                    objectTypeIdentification,
                    preSelectedRefAttribute);

        } catch (CommonException e) {
            pageBase.error("Couldn't get object type statistics for "
                    + objectTypeIdentification + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    private static void showStatisticsPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull GenericObjectType statisticsObject,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @Nullable ItemPathType preSelectedRefAttribute) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                ShadowObjectTypeUtil.getObjectTypeStatisticsRequired(statisticsObject);

        SmartStatisticsPanel statisticsPanel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(),
                () -> statistics,
                resourceOid,
                objectTypeIdentification) {

            @Override
            protected ItemPathType getDefaultSelectedAttributePath() {
                return preSelectedRefAttribute;
            }
        };

        pageBase.replaceMainPopup(statisticsPanel, target);
    }

    private static void showProgressExecutorPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @Nullable ItemPathType preSelectedRefAttribute) {

        ShadowKindType kind = objectTypeIdentification.getKind();
        String intent = objectTypeIdentification.getIntent();
        String displayName = "kind=" + kind.value() + ", intent=" + intent;

        IModel<Integer> threadsModel = Model.of(4);
        IModel<String> taskOidModel = Model.of();

        ThreadSetupPopupStepPanel threadStep =
                new ThreadSetupPopupStepPanel(threadsModel) {

                    @Override
                    public boolean onNextPerformed(AjaxRequestTarget target) {

                        IModel<TaskType> taskModel = regenerateObjectTypeStatistics(
                                target,
                                pageBase,
                                smartIntegrationService,
                                resourceOid,
                                objectTypeIdentification,
                                threadsModel.getObject());

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
                        pageBase.createStringResource("ObjectTypeStatisticsButton.regeneratingStatistics"),
                        pageBase.createStringResource(
                                "ObjectTypeStatisticsButton.regeneratingStatistics.subText",
                                displayName),
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
                                resourceOid,
                                objectTypeIdentification,
                                preSelectedRefAttribute);
                    }
                };

        PopupStepperModel model = new PopupStepperModel(
                List.of(threadStep, progressStep));

        PopupStepperPanel popup = new PopupStepperPanel(
                pageBase.getMainPopupBodyId(),
                Model.of(model)) {
            @Override
            public IModel<String> getTitle() {
                return pageBase.createStringResource(
                        "ObjectTypeStatisticsButton.generatingStatistics.title", displayName);
            }

            @Override
            public @NotNull IModel<String> getTitleIconClass() {
                return Model.of("fa fa-chart-bar");
            }
        };

        pageBase.replaceMainPopup(popup, target);
    }

    private static @Nullable IModel<TaskType> regenerateObjectTypeStatistics(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull Integer threads) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_STATISTICS);

        try {
            String taskOid = smartIntegrationService.regenerateObjectTypeStatistics(
                    resourceOid,
                    objectTypeIdentification,
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
            pageBase.error("Couldn't regenerate object type statistics for "
                    + objectTypeIdentification + ": " + e.getMessage());
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
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @Nullable ItemPathType preSelectedRefAttribute) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_STATISTICS);

        try {
            ShadowKindType kind = objectTypeIdentification.getKind();
            String intent = objectTypeIdentification.getIntent();

            GenericObjectType latestStatistics =
                    smartIntegrationService.getLatestObjectTypeStatistics(
                            resourceOid,
                            kind.value(),
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
                    resourceOid,
                    objectTypeIdentification,
                    preSelectedRefAttribute);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated statistics for "
                    + objectTypeIdentification + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }
}

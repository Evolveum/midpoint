/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowObjectTypeStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class encapsulating the UI workflow for loading and regenerating
 * Smart Integration object type statistics.
 *
 * <p>
 * This class coordinates interaction between the UI (popups, progress panels),
 * {@code SmartIntegrationService}, and background tasks.
 * It is designed to be reusable from multiple UI components.
 * </p>
 */
public final class ObjectTypeStatisticsActions {

    private static final String OPERATION_GET_STATISTICS = "getObjectClassStatistics";
    private static final String OPERATION_REGENERATE_STATISTICS = "regenerateObjectClassStatistics";
    private static final String OP_LOAD_TASK = "loadTask";

    private ObjectTypeStatisticsActions() {
    }

    public static void handleClick(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            ItemPathType preSelectedRefAttribute,
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
                    showStatisticsPopup(target, pageBase, latestStatistics, resourceOid,
                            objectTypeIdentification, preSelectedRefAttribute);
                    return;
                }
            }

            String taskOid = smartIntegrationService.regenerateObjectTypeStatistics(
                    resourceOid, objectTypeIdentification, task, task.getResult());

            showProgressPopup(target, pageBase, smartIntegrationService, resourceOid, objectTypeIdentification,
                    taskOid, preSelectedRefAttribute);

        } catch (CommonException e) {
            pageBase.error("Couldn't get object class statistics for " + objectTypeIdentification + ": " + e.getMessage());
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
            ItemPathType preSelectedRefAttribute) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                ShadowObjectTypeStatisticsTypeUtil.getObjectTypeStatisticsRequired(statisticsObject);

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

    private static void showProgressPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull String taskOid, ItemPathType preSelectedRefAttribute) {

        ShadowKindType kind = objectTypeIdentification.getKind();
        String intent = objectTypeIdentification.getIntent();
        String displayName = "kind=" + kind.value() + ", intent=" + intent;

        SmartTaskProgressPanel panel = new SmartTaskProgressPanel(
                pageBase.getMainPopupBodyId(),
                pageBase.createStringResource("ObjectTypeStatisticsButton.regeneratingStatistics"),
                pageBase.createStringResource("ObjectTypeStatisticsButton.regeneratingStatistics.subText", displayName),
                () -> loadTask(pageBase, taskOid)) {

            @Override
            protected boolean showResultAfterCompletion() {
                return true;
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                onProgressFinished(target, pageBase, smartIntegrationService, resourceOid,
                        objectTypeIdentification, preSelectedRefAttribute);
            }

            @Override
            protected IModel<String> getStopButtonLabel() {
                return pageBase.createStringResource("ObjectTypeStatisticsButton.stopRegeneration");
            }
        };

        pageBase.replaceMainPopup(panel, target);
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
                    smartIntegrationService.getLatestObjectTypeStatistics(resourceOid, kind.value(), intent, task.getResult());

            if (latestStatistics == null) {
                pageBase.warn("Statistics computation finished, but no statistics object was found.");
                target.add(pageBase.getFeedbackPanel());
                return;
            }

            showStatisticsPopup(target, pageBase, latestStatistics, resourceOid, objectTypeIdentification, preSelectedRefAttribute);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated statistics for " + objectTypeIdentification + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    private static @Nullable TaskType loadTask(@NotNull PageBase pageBase, @NotNull String taskOid) {
        Task task = pageBase.createSimpleTask(OP_LOAD_TASK);

        PrismObject<TaskType> taskObject = WebModelServiceUtils.loadObject(
                TaskType.class, taskOid, null, true, pageBase, task, task.getResult());

        return taskObject != null ? taskObject.asObjectable() : null;
    }
}

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
import com.evolveum.midpoint.schema.util.FocusObjectStatisticsTypeUtil;
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

import javax.xml.namespace.QName;

/**
 * Utility class encapsulating the UI workflow for loading and regenerating
 * Smart Integration focus object statistics (e.g. UserType).
 */
public final class FocusStatisticsActions {

    private static final String OPERATION_GET_FOCUS_STATISTICS = "getFocusObjectStatistics";
    private static final String OPERATION_REGENERATE_FOCUS_STATISTICS = "regenerateFocusObjectStatistics";
    private static final String OP_LOAD_TASK = "loadTask";

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
                                focusObjectTypeName, resourceOid, kind, intent, task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(target, pageBase, latestStatistics, preSelectedAttribute,
                            focusObjectTypeName, resourceOid, kind, intent);
                    return;
                }
            }

            String taskOid = smartIntegrationService.regenerateFocusObjectStatistics(
                    focusObjectTypeName, resourceOid, kind, intent, task, task.getResult());

            showProgressPopup(target, pageBase, smartIntegrationService,
                    focusObjectTypeName, resourceOid, kind, intent, taskOid, preSelectedAttribute);

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

    private static void showProgressPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName focusObjectTypeName,
            @NotNull String resourceOid,
            @NotNull ShadowKindType kind,
            @NotNull String intent,
            @NotNull String taskOid,
            @Nullable ItemPathType preSelectedAttribute) {

        String displayName = focusObjectTypeName.getLocalPart();

        SmartTaskProgressPanel panel = new SmartTaskProgressPanel(
                pageBase.getMainPopupBodyId(),
                pageBase.createStringResource("FocusStatisticsButton.regeneratingStatistics"),
                pageBase.createStringResource("FocusStatisticsButton.regeneratingStatistics.subText", displayName),
                () -> loadTask(pageBase, taskOid)) {

            @Override
            protected boolean showResultAfterCompletion() {
                return true;
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                onProgressFinished(target, pageBase, smartIntegrationService,
                        focusObjectTypeName, resourceOid, kind, intent, preSelectedAttribute);
            }

            @Override
            protected IModel<String> getStopButtonLabel() {
                return pageBase.createStringResource("FocusStatisticsButton.stopRegeneration");
            }
        };

        pageBase.replaceMainPopup(panel, target);
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
                            focusObjectTypeName, resourceOid, kind, intent, task.getResult());

            if (latestStatistics == null) {
                pageBase.warn("Statistics computation finished, but no statistics object was found.");
                target.add(pageBase.getFeedbackPanel());
                return;
            }

            showStatisticsPopup(target, pageBase, latestStatistics, preSelectedAttribute,
                    focusObjectTypeName, resourceOid, kind, intent);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated focus statistics for " + focusObjectTypeName + ": " + e.getMessage());
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

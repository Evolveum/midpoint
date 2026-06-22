/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.action;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.stats.SmartStatisticsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressPanel;
import com.evolveum.midpoint.schema.util.ShadowObjectClassUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.SmartIntegrationUtils.loadTask;

public final class ObjectClassStatisticsActions {

    private static final String OPERATION_GET_STATISTICS = "getObjectClassStatistics";
    private static final String OPERATION_REGENERATE_STATISTICS = "regenerateObjectClassStatistics";

    private ObjectClassStatisticsActions() {
    }

    public static void handleClick(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            boolean forceRegeneration) {

        Task task = pageBase.createSimpleTask(OPERATION_GET_STATISTICS);

        try {
            if (!forceRegeneration) {
                GenericObjectType latestStatistics =
                        smartIntegrationService.getLatestObjectClassStatistics(
                                resourceOid,
                                objectClassName,
                                task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(target, pageBase, latestStatistics, resourceOid, objectClassName);
                    return;
                }
            }

            showProgressPopup(target, pageBase, smartIntegrationService, resourceOid, objectClassName);

        } catch (CommonException e) {
            pageBase.error("Couldn't get object class statistics for " + objectClassName + ": " + e.getMessage());
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
            @NotNull QName objectClassName) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                ShadowObjectClassUtil.getStatisticsRequired(statisticsObject);

        SmartStatisticsPanel statisticsPanel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(),
                () -> statistics,
                resourceOid,
                objectClassName);

        pageBase.replaceMainPopup(statisticsPanel, target);
    }

    private static void showProgressPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull QName objectClassName) {

        SmartTaskProgressPanel panel = new SmartTaskProgressPanel(
                pageBase.getMainPopupBodyId(),
                pageBase.createStringResource("ObjectClassStatisticsButton.regeneratingStatistics"),
                pageBase.createStringResource(
                        "ObjectClassStatisticsButton.regeneratingStatistics.subText",
                        objectClassName.getLocalPart()),
                (ajaxTarget, threads) -> regenerateObjectClassStatistics(
                        ajaxTarget,
                        pageBase,
                        smartIntegrationService,
                        resourceOid,
                        objectClassName,
                        threads)) {

            @Override
            protected boolean showResultAfterCompletion() {
                return true;
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                onProgressFinished(target, pageBase, smartIntegrationService, resourceOid, objectClassName);
            }

            @Override
            protected IModel<String> getStopButtonLabel() {
                return pageBase.createStringResource("ObjectClassStatisticsButton.stopRegeneration");
            }
        };

        pageBase.replaceMainPopup(panel, target);
    }

    private static @Nullable IModel<TaskType> regenerateObjectClassStatistics(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull String resourceOid,
            @NotNull QName objectClassName,
            @NotNull Integer threads) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_STATISTICS);

        try {
            String taskOid = smartIntegrationService.regenerateObjectClassStatistics(
                    resourceOid,
                    objectClassName,
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
            pageBase.error("Couldn't regenerate object class statistics for "
                    + objectClassName + ": " + e.getMessage());
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
            @NotNull QName objectClassName) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_STATISTICS);

        try {
            GenericObjectType latestStatistics =
                    smartIntegrationService.getLatestObjectClassStatistics(
                            resourceOid,
                            objectClassName,
                            task.getResult());

            if (latestStatistics == null) {
                pageBase.warn("Statistics computation finished, but no statistics object was found.");
                target.add(pageBase.getFeedbackPanel());
                return;
            }

            showStatisticsPopup(target, pageBase, latestStatistics, resourceOid, objectClassName);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated statistics for " + objectClassName + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }
}

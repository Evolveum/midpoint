/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import java.io.Serial;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartStatisticsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * UI button panel for displaying or regenerating object class statistics.
 *
 * <p>When clicked, the button tries to load the latest statistics for the given
 * resource object class. If statistics exist, they are displayed in a popup.
 * If they do not exist (or regeneration is forced), a background task is started
 * to compute new statistics and a progress popup is shown.</p>
 *
 * <p>After the computation finishes, the newly generated statistics are
 * automatically displayed.</p>
 */
public class ObjectClassStatisticsButton extends BasePanel<QName> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROCESS_STATISTICS_BUTTON = "processStatisticsButton";

    private static final String OPERATION_GET_STATISTICS = "getObjectClassStatistics";
    private static final String OPERATION_REGENERATE_STATISTICS = "regenerateObjectClassStatistics";
    private static final String OP_LOAD_TASK = "loadTask";

    private final String resourceOid;

    public ObjectClassStatisticsButton(String id, IModel<QName> objectClassName, String resourceOid) {
        super(id, objectClassName);
        this.resourceOid = resourceOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxIconButton processStatisticsButton = new AjaxIconButton(
                ID_PROCESS_STATISTICS_BUTTON,
                Model.of("fa-solid fa-chart-bar"),
                getMainButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                handleClick(target);
            }
        };
        processStatisticsButton.showTitleAsLabel(true);
        add(processStatisticsButton);
    }

    protected IModel<String> getMainButtonLabel() {
        return createStringResource("ObjectClassStatisticsButton.processStatistics");
    }

    private void handleClick(@NotNull AjaxRequestTarget target) {
        PageBase page = getPageBase();
        QName objectClassName = getModelObject();
        SmartIntegrationService smartIntegrationService = page.getSmartIntegrationService();

        Task task = page.createSimpleTask(OPERATION_GET_STATISTICS);

        try {
            if (!forceRegeneration()) {
                GenericObjectType latestStatistics =
                        smartIntegrationService.getLatestStatistics(resourceOid, objectClassName, task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(target, page, latestStatistics, objectClassName);
                    return;
                }
            }

            // No stats exist -> trigger regeneration and show progress popup
            String taskOid = smartIntegrationService.regenerateObjectClassStatistics(
                    resourceOid, objectClassName, task, task.getResult());

            showProgressPopup(target, getPageBase(), smartIntegrationService, objectClassName, taskOid);

        } catch (CommonException e) {
            page.error("Couldn't get object class statistics for " + objectClassName + ": " + e.getMessage());
            target.add(page.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    protected boolean forceRegeneration() {
        return false;
    }

    private void showStatisticsPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase, @NotNull GenericObjectType statisticsObject,
            @NotNull QName objectClassName) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                ShadowObjectClassStatisticsTypeUtil.getStatisticsRequired(statisticsObject);

        SmartStatisticsPanel statisticsPanel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(),
                () -> statistics,
                resourceOid,
                objectClassName);

        pageBase.replaceMainPopup(statisticsPanel, target);
    }

    private void showProgressPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName objectClassName,
            @NotNull String taskOid) {

        SmartTaskProgressPanel panel = new SmartTaskProgressPanel(
                pageBase.getMainPopupBodyId(),
                createStringResource("ObjectClassStatisticsButton.regeneratingStatistics"),
                createStringResource("ObjectClassStatisticsButton.regeneratingStatistics.subText", objectClassName.getLocalPart()),
                () -> loadTask(pageBase, taskOid)) {

            @Override
            protected boolean showResultAfterCompletion() {
                return true;
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                onProgressFinished(target, pageBase, smartIntegrationService, objectClassName);
            }

            @Override
            protected IModel<String> getStopButtonLabel() {
                return createStringResource("ObjectClassStatisticsButton.stopRegeneration");
            }
        };

        pageBase.replaceMainPopup(panel, target);
    }

    private void onProgressFinished(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            @NotNull QName objectClassName) {

        Task task = pageBase.createSimpleTask(OPERATION_REGENERATE_STATISTICS);

        try {
            GenericObjectType latestStatistics =
                    smartIntegrationService.getLatestStatistics(resourceOid, objectClassName, task.getResult());

            if (latestStatistics == null) {
                pageBase.warn("Statistics computation finished, but no statistics object was found.");
                target.add(pageBase.getFeedbackPanel());
                return;
            }

            showStatisticsPopup(target, pageBase, latestStatistics, objectClassName);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated statistics for " + objectClassName + ": " + e.getMessage());
            target.add(pageBase.getFeedbackPanel());
        } finally {
            task.getResult().computeStatusIfUnknown();
        }
    }

    private @Nullable TaskType loadTask(@NotNull PageBase pageBase, @NotNull String taskOid) {
        Task task = pageBase.createSimpleTask(OP_LOAD_TASK);

        PrismObject<TaskType> taskObject = WebModelServiceUtils.loadObject(
                TaskType.class, taskOid, null, true, pageBase, task, task.getResult());

        return taskObject != null ? taskObject.asObjectable() : null;
    }
}

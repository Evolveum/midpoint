/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component.SmartStatisticsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.component.SmartTaskProgressPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.ShadowObjectClassStatisticsTypeUtil;
import com.evolveum.midpoint.smart.api.SmartIntegrationService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * UI button panel for displaying or regenerating object type statistics.
 *
 * <p>When clicked, the button tries to load the latest statistics for the given
 * resource object class. If statistics exist, they are displayed in a popup.
 * If they do not exist (or regeneration is forced), a background task is started
 * to compute new statistics and a progress popup is shown.</p>
 *
 * <p>After the computation finishes, the newly generated statistics are
 * automatically displayed.</p>
 */
public class ObjectTypeStatisticsButton extends BasePanel<ResourceObjectTypeIdentification> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROCESS_STATISTICS_BUTTON = "processStatisticsButton";

    private static final String OPERATION_GET_STATISTICS = "getObjectClassStatistics";
    private static final String OPERATION_REGENERATE_STATISTICS = "regenerateObjectClassStatistics";
    private static final String OP_LOAD_TASK = "loadTask";

    private final String resourceOid;

    public ObjectTypeStatisticsButton(String id, IModel<ResourceObjectTypeIdentification> resourceObjectTypeIdentifier, String resourceOid) {
        super(id, resourceObjectTypeIdentifier);
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
        ResourceObjectTypeIdentification resourceObjectTypeIdentification = getModelObject();
        SmartIntegrationService smartIntegrationService = page.getSmartIntegrationService();

        Task task = page.createSimpleTask(OPERATION_GET_STATISTICS);

        try {
            if (!forceRegeneration()) {
                GenericObjectType latestStatistics =
                        smartIntegrationService.getLatestObjectTypeStatistics(resourceOid,
                                resourceObjectTypeIdentification.getKind().value(),
                                resourceObjectTypeIdentification.getIntent(), task.getResult());

                if (latestStatistics != null) {
                    showStatisticsPopup(target, page, latestStatistics, resourceObjectTypeIdentification);
                    return;
                }
            }

            // No stats exist -> trigger regeneration and show progress popup
            String taskOid = smartIntegrationService.regenerateObjectTypeStatistics(
                    resourceOid, resourceObjectTypeIdentification, task, task.getResult());

            showProgressPopup(target, getPageBase(), smartIntegrationService, resourceObjectTypeIdentification, taskOid);

        } catch (CommonException e) {
            page.error("Couldn't get object class statistics for " + resourceObjectTypeIdentification + ": " + e.getMessage());
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
            ResourceObjectTypeIdentification objectTypeIdentification) throws SchemaException {

        ShadowObjectClassStatisticsType statistics =
                ShadowObjectClassStatisticsTypeUtil.getObjectTypeStatisticsRequired(statisticsObject);

        SmartStatisticsPanel statisticsPanel = new SmartStatisticsPanel(
                pageBase.getMainPopupBodyId(),
                () -> statistics,
                resourceOid,
                objectTypeIdentification);

        pageBase.replaceMainPopup(statisticsPanel, target);
    }

    private void showProgressPopup(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            ResourceObjectTypeIdentification objectTypeIdentification,
            @NotNull String taskOid) {
        ShadowKindType kind = objectTypeIdentification.getKind();
        String intent = objectTypeIdentification.getIntent();
        String displayName = "kind=" + kind.value() + ", intent=" + intent;

        SmartTaskProgressPanel panel = new SmartTaskProgressPanel(
                pageBase.getMainPopupBodyId(),
                createStringResource("ObjectTypeStatisticsButton.regeneratingStatistics"),
                createStringResource("ObjectTypeStatisticsButton.regeneratingStatistics.subText", displayName),
                () -> loadTask(pageBase, taskOid)) {

            @Override
            protected boolean showResultAfterCompletion() {
                return true;
            }

            @Override
            protected void onShowResults(AjaxRequestTarget target) {
                onProgressFinished(target, pageBase, smartIntegrationService, objectTypeIdentification);
            }

            @Override
            protected IModel<String> getStopButtonLabel() {
                return createStringResource("ObjectTypeStatisticsButton.stopRegeneration");
            }
        };

        pageBase.replaceMainPopup(panel, target);
    }

    private void onProgressFinished(
            @NotNull AjaxRequestTarget target,
            @NotNull PageBase pageBase,
            @NotNull SmartIntegrationService smartIntegrationService,
            ResourceObjectTypeIdentification objectTypeIdentification) {

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

            showStatisticsPopup(target, pageBase, latestStatistics, objectTypeIdentification);

        } catch (CommonException e) {
            pageBase.error("Couldn't load regenerated statistics for " + objectTypeIdentification + ": " + e.getMessage());
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

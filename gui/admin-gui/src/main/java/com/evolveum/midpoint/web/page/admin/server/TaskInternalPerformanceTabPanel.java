/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementPerformanceInformationType;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class TaskInternalPerformanceTabPanel extends BasePanel<PrismContainerWrapper<OperationStatsType>> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskInternalPerformanceTabPanel.class);

    private static final String ID_INFORMATION = "information";


    //private static final Trace LOGGER = TraceManager.getTrace(TaskInternalPerformanceTabPanel.class);

    public TaskInternalPerformanceTabPanel(String id, IModel<PrismContainerWrapper<OperationStatsType>> statsModel) {
        super(id, statsModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initLayout() {
        AceEditor informationText = new AceEditor(ID_INFORMATION, createStringModel()) {
            @Override
            public int getHeight() {
                return 300;
            }

            @Override
            public boolean isResizeToMaxHeight() {
                return true;
            }
        };
        informationText.setReadonly(true);
//        informationText.setHeight(300);
//        informationText.setResizeToMaxHeight(true);
        informationText.setMode(null);
        add(informationText);

    }

    private IModel<String> createStringModel() {
        return new IModel<String>() {
            @Override
            public String getObject() {
                return getStatistics();
            }

            @Override
            public void setObject(String object) {
                // nothing to do here
            }
        };
    }

    @SuppressWarnings("Duplicates")
    private String getStatistics() {
        PrismContainerWrapper<OperationStatsType> statsContainer = getModelObject();
        if (statsContainer == null) {
            return "No operation statistics available";
        }

        OperationStatsType statistics;
        try {
            PrismContainerValueWrapper<OperationStatsType> statsValue = statsContainer.getValue();
            statistics = statsValue.getRealValue();
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot get statistics from task", e);
            return "No operation statistics available";
        }

        if (statistics == null) {
            return "No operation statistics available";
        }
        StringBuilder sb = new StringBuilder();
        if (statistics.getRepositoryPerformanceInformation() != null) {
            sb.append("Repository performance information:\n")
                    .append(RepositoryPerformanceInformationUtil.format(statistics.getRepositoryPerformanceInformation()))
                    .append("\n");
        }
        WorkBucketManagementPerformanceInformationType buckets = statistics.getWorkBucketManagementPerformanceInformation();
        if (buckets != null && !buckets.getOperation().isEmpty()) {
            sb.append("Work buckets management performance information:\n")
                    .append(TaskWorkBucketManagementPerformanceInformationUtil.format(buckets))
                    .append("\n");
        }
        if (statistics.getCachesPerformanceInformation() != null) {
            sb.append("Cache performance information:\n")
                    .append(CachePerformanceInformationUtil.format(statistics.getCachesPerformanceInformation()))
                    .append("\n");
        }
        if (statistics.getOperationsPerformanceInformation() != null) {
            sb.append("Methods performance information:\n")
                    .append(OperationsPerformanceInformationUtil.format(statistics.getOperationsPerformanceInformation()))
                    .append("\n");
        }
        sb.append("\n-------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
        sb.append("Other performance-related information that is shown elsewhere (provided here just for completeness):\n\n");
        if (statistics.getIterativeTaskInformation() != null) {
            sb.append("Iterative task information:\n")
                    .append(IterativeTaskInformation.format(statistics.getIterativeTaskInformation()))
                    .append("\n");
        }
        if (statistics.getActionsExecutedInformation() != null) {
            sb.append("Actions executed:\n")
                    .append(ActionsExecutedInformation.format(statistics.getActionsExecutedInformation()))
                    .append("\n");
        }
//        if (statistics.getSynchronizationInformation() != null) {
//            sb.append("Synchronization information:\n")
//                    .append(SynchronizationInformation.format(statistics.getSynchronizationInformation()))
//                    .append("\n");
//        }
        if (statistics.getEnvironmentalPerformanceInformation() != null) {
            sb.append("Environmental performance information:\n")
                    .append(EnvironmentalPerformanceInformation.format(statistics.getEnvironmentalPerformanceInformation()))
                    .append("\n");
        }
        if (statistics.getCachingConfiguration() != null) {
            sb.append("\n-------------------------------------------------------------------------------------------------------------------------------------------------------------------\n");
            sb.append("Caching configuration:\n\n");
            sb.append(statistics.getCachingConfiguration());
        }
        return sb.toString();
    }


    @Override
    public Collection<Component> getComponentsToUpdate() {
        return Collections.singleton(this);
    }

    @Override
    protected void detachModel() {
        super.detachModel();
    }
}

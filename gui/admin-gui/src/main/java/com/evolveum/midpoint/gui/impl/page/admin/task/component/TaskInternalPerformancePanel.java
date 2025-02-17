/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.task.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.task.TaskDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.statistics.*;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.Format;
import com.evolveum.midpoint.schema.statistics.AbstractStatisticsPrinter.SortBy;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.web.page.admin.server.TaskDisplayUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationStatsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import com.inamik.text.tables.Cell;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.SimpleTable;
import com.inamik.text.tables.grid.Border;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.*;

@PanelType(name = "internalPerformance")
@PanelInstance(identifier = "internalPerformance", applicableForType = TaskType.class, childOf = TaskPerformancePanel.class,
        display = @PanelDisplay(label = "pageTask.internalPerformance.title", order = 70))
public class TaskInternalPerformancePanel extends AbstractObjectMainPanel<TaskType, TaskDetailsModel> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_FORMAT = "format";
    private static final String ID_SORT_BY = "sortBy";
    private static final String ID_INFORMATION = "information";

    private static final String LINE = "\n-------------------------------------------------------------------------------------------------------------------------------------------------------------------\n";

    public TaskInternalPerformancePanel(String id, TaskDetailsModel taskModel, ContainerPanelConfigurationType config) {
        super(id, taskModel, config);
    }

    private final IModel<Format> formatModel = Model.of(Format.TEXT);
    private final IModel<SortBy> sortByModel = Model.of(SortBy.NAME);

    protected void initLayout() {
        DropDownChoicePanel<Format> formatPanel =
                WebComponentUtil.createEnumPanel(ID_FORMAT, createFormatListModel(), formatModel, this, false);
        formatPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(TaskInternalPerformancePanel.this);
            }
        });
        add(formatPanel);

        DropDownChoicePanel<SortBy> sortByPanel =
                WebComponentUtil.createEnumPanel(SortBy.class, ID_SORT_BY, sortByModel, this, false);
        sortByPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(TaskInternalPerformancePanel.this);
            }
        });
        add(sortByPanel);

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
        informationText.setMode(null);
        add(informationText);

    }

    private IModel<List<Format>> createFormatListModel() {
        List<Format> formatList = new ArrayList<>();
        Collections.addAll(formatList, Format.values());
        formatList.remove(Format.RAW);
        return Model.ofList(formatList);
    }

    private IModel<String> createStringModel() {
        return new IModel<>() {
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
        PrismObjectWrapper<TaskType> taskWrapper = getObjectWrapper();
        if (taskWrapper == null) {
            return "No task available";
        }
        var taskObj = taskWrapper.getObject();
        WebPrismUtil.cleanupEmptyContainers(taskObj);
        TaskType task = PrismObject.asObjectable(taskWrapper.getObject());
        if (task == null) {
            return "No task available";
        }

        OperationStatsType statistics = task.getOperationStats();
        if (statistics == null) {
            return "No operation statistics available";
        }

        Integer iterations = ActivityStatisticsUtil.getAllItemsProcessed(task.getActivityState());
        Integer seconds = getSeconds(task);

        AbstractStatisticsPrinter.Options options = new AbstractStatisticsPrinter.Options(formatModel.getObject(),
                sortByModel.getObject());

        StringBuilder sb = new StringBuilder();

        formatBasicInfo(sb, seconds, iterations);

        if (statistics.getRepositoryPerformanceInformation() != null) {
            sb.append("Repository performance information\n\n")
                    .append(RepositoryPerformanceInformationUtil.format(statistics.getRepositoryPerformanceInformation(),
                            options, iterations, seconds))
                    .append("\n");
        }
        if (statistics.getCachesPerformanceInformation() != null) {
            sb.append("Cache performance information\n\n")
                    .append(CachePerformanceInformationUtil.format(statistics.getCachesPerformanceInformation(), options))
                    .append("\n");
        }
        if (statistics.getOperationsPerformanceInformation() != null) {
            sb.append("Methods performance information\n\n")
                    .append(OperationsPerformanceInformationUtil.format(statistics.getOperationsPerformanceInformation(),
                            options, iterations, seconds))
                    .append("\n");
        }
        sb.append(LINE);
        sb.append("Other performance-related information that is shown elsewhere (provided here just for completeness):\n\n");
        if (statistics.getEnvironmentalPerformanceInformation() != null) {
            sb.append("Environmental performance information:\n\n")
                    .append(EnvironmentalPerformanceInformation.format(statistics.getEnvironmentalPerformanceInformation()))
                    .append("\n");
        }
        sb.append(ActivityProgressAndStatisticsPrinter.print(task));
        if (statistics.getCachingConfiguration() != null) {
            sb.append(LINE);
            sb.append("Caching configuration:\n\n");
            sb.append(statistics.getCachingConfiguration());
        }
        return sb.toString();
    }

    private void formatBasicInfo(StringBuilder sb, Integer seconds, Integer iterations) {
        if (seconds == null && iterations == null) {
            return;
        }

        SimpleTable table = new SimpleTable();
        table.nextRow();
        table.nextCell();
        if (iterations != null) {
            table.addLine(" Iterations ");
        }
        if (seconds != null) {
            table.addLine(" Execution time (wall clock) ");
        }
        table.nextCell();
        if (iterations != null) {
            table.addLine(String.format(Locale.US, " %,d ", iterations));
        }
        if (seconds != null) {
            table.addLine(String.format(Locale.US, " %,d seconds ", seconds));
        }
        GridTable gridTable = table.toGrid();
        gridTable = Border.of(Border.Chars.of('+', '-', '|')).apply(gridTable);
        gridTable.apply(Cell.Functions.TOP_ALIGN);
        gridTable.apply(Cell.Functions.LEFT_ALIGN);
        for (String line: gridTable.toCell()) {
            sb.append(line).append("\n");
        }
        sb.append("\n");
    }

    private Integer getSeconds(TaskType task) {
        Long executionTime = TaskDisplayUtil.getExecutionTime(task);
        return executionTime != null ? (int) (executionTime / 1000) : null;
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

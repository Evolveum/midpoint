/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismReferenceImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.AjaxDownloadBehaviorFromStream;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.DateLabelComponent;
import com.evolveum.midpoint.web.page.admin.reports.ReportDownloadHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RelatedTasksPanel extends BasePanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(RelatedTasksPanel.class);

    private static final String DOT_CLASS = RelatedTasksPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_TASKS = DOT_CLASS + "loadTasks";
    private static final String OPERATION_LOAD_REPORT = DOT_CLASS + "loadReport";

    private static final String ID_RELATED_TASKS = "relatedTasks";
    private CertificationDetailsModel model;

    private static final int MAX_RELATED_TASKS = 5;
    private int realTasksCount = 0;

    public RelatedTasksPanel(String id, CertificationDetailsModel model) {
        super(id);
        this.model = model;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(initRelatedTasksPanel(ID_RELATED_TASKS, true));
    }

    private StatisticListBoxPanel<TaskType> initRelatedTasksPanel(String id, boolean allowViewAll) {
        IModel<List<StatisticBoxDto<TaskType>>> tasksModel = getRelatedTasksModel(allowViewAll);
        DisplayType relatedTasksDisplay = new DisplayType()
                .label("RelatedTasksPanel.title");
        return new StatisticListBoxPanel<>(id, Model.of(relatedTasksDisplay),
                tasksModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isViewAllAllowed() {
                return tasksCountExceedsLimit() && allowViewAll;
            }

            @Override
            protected void viewAllActionPerformed(AjaxRequestTarget target) {
                showAllRelatedTasksPerformed(target);
            }


            @Override
            protected Component createRightSideBoxComponent(String id, StatisticBoxDto<TaskType> statisticObject) {
                Component component = createRightSideTaskComponent(id, statisticObject.getStatisticObject());
                component.add(AttributeAppender.append("class", "col-md-3"));
                return component;
            }

            @Override
            protected boolean isLabelClickable() {
                return true;
            }
        };
    }

    private void showAllRelatedTasksPerformed(AjaxRequestTarget target) {
        getPageBase().showMainPopup(initRelatedTasksPanel(getPageBase().getMainPopupBodyId(), false), target);
    }


    private boolean tasksCountExceedsLimit() {
        return realTasksCount > MAX_RELATED_TASKS;
    }

    private Component createRightSideTaskComponent(String id, TaskType task) {
        ReportDataType report = loadReport(task);
        if (report != null) {
            AjaxDownloadBehaviorFromStream ajaxDownloadBehavior =
                    ReportDownloadHelper.createAjaxDownloadBehaviorFromStream(report, getPageBase());
            AjaxIconButton downloadButton = new AjaxIconButton(id, Model.of("fa fa-download"),
                    createStringResource("PageTask.download.report")) {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    ajaxDownloadBehavior.initiate(target);
                }
            };
            downloadButton.add(ajaxDownloadBehavior);
            return downloadButton;
        } else {
            Date completionTime = null;
            if (task.getExecutionState() == TaskExecutionStateType.CLOSED) {

                Long time = WebComponentUtil.xgc2long(task.getCompletionTimestamp());
                if (time != null) {
                    completionTime = new Date(time);
                }
            }
            DateLabelComponent dateLabel = new DateLabelComponent(id, Model.of(completionTime),
                    WebComponentUtil.getShortDateTimeFormat(getPageBase()));
            dateLabel.customizeDateString((dateAsString, date) -> {
                String prefix;
                if (task.getExecutionState() == TaskExecutionStateType.CLOSED) {
                    prefix = getString("pageTasks.task.closedAt") + " ";
                } else {
                    prefix = WebComponentUtil.formatDurationWordsForLocal(
                            date.getTime(), true, true);
                }
                return prefix + dateAsString;
            });
            return dateLabel;
        }
    }

    private ReportDataType loadReport(TaskType task) {
        if (task == null) {
            return null;
        }
        ExtensionType taskExtension = task.getExtension();
        if (taskExtension == null) {
            return null;
        }

        Item<?, ?> reportRef = taskExtension.asPrismContainerValue().findItem(new ItemName("reportDataParam"));
        if (!(reportRef instanceof PrismReferenceImpl)) {
            return null;
        }
        Referencable value = ((PrismReferenceImpl) reportRef).getRealValue();
        String createdReportOid = value != null ? value.getOid()  : null;
        if (createdReportOid == null) {
            return null;
        }
        ObjectQuery query = getPrismContext().queryFor(ReportDataType.class)
                .id(createdReportOid)
                .build();
        OperationResult result = new OperationResult(OPERATION_LOAD_REPORT);
        List<PrismObject<ReportDataType>> reports = WebModelServiceUtils.searchObjects(ReportDataType.class, query, null,
                result, getPageBase());
        if (reports.isEmpty()) {
            return null;
        }
        return reports.get(0).asObjectable();
    }

    private IModel<List<StatisticBoxDto<TaskType>>> getRelatedTasksModel(boolean restricted) {
        return () -> {
            List<StatisticBoxDto<TaskType>> list = new ArrayList<>();
            List<TaskType> tasks = loadTasks();
            if (tasks == null) {
                return list;
            }
            if (restricted) {
                realTasksCount = tasks.size();
                tasks.stream().limit(MAX_RELATED_TASKS).forEach(t -> list.add(createTaskStatisticBoxDto(t)));
            } else {
                tasks.forEach(t -> list.add(createTaskStatisticBoxDto(t)));
            }
            return list;
        };
    }

    private List<TaskType> loadTasks() {
        String campaignOid = model.getObjectType().getOid();
        ObjectQuery query = PrismContext.get().queryFor(TaskType.class)
                .item(ItemPath.create(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_OBJECTS, BasicObjectSetType.F_OBJECT_REF))
                .ref(campaignOid)
                .asc(TaskType.F_COMPLETION_TIMESTAMP)
                .build();
        OperationResult result = new OperationResult(OPERATION_LOAD_TASKS);
        List<PrismObject<TaskType>> tasks = WebModelServiceUtils.searchObjects(TaskType.class, query, null,
                result, getPageBase());

        //todo hack
        String certCasesReport = "00000000-0000-0000-0000-000000000150";
        String certWorkItemsReport = "00000000-0000-0000-0000-000000000160";
        ObjectQuery certRelatedTasksQuery = getPrismContext().queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF)
                .ref(certCasesReport, certWorkItemsReport)
                .build();
        List<PrismObject<TaskType>> certRelatedTasks = WebModelServiceUtils.searchObjects(TaskType.class, certRelatedTasksQuery,
                null, result, getPageBase());
        tasks.addAll(certRelatedTasks.stream()
                .filter(t -> isRelatedToCampaign(t, campaignOid))
                .toList());
        return tasks.stream()
                .map(t -> t.asObjectable())
                .toList();
    }

    //todo hack
    private boolean isRelatedToCampaign(PrismObject<TaskType> task, String campaignOid) {
        if (task == null) {
            return false;
        }
        var activity = task.asObjectable().getActivity();
        var work = activity != null ? activity.getWork() : null;
        var reportExport = work != null ? work.getReportExport() : null;
        var params = reportExport != null ? reportExport.getReportParam() : null;
        if (params == null) {
            return false;
        }
        Item<?, ?> item = params.asPrismContainerValue().findItem(new ItemName("campaignRef"));
        if (!(item instanceof PrismProperty<?>)) {
            return false;
        }
        PrismPropertyValue<?> value = ((PrismProperty<?>) item).getValue();
        if (value == null || !(value.getRealValue() instanceof RawType)) {
            return false;
        }
        ObjectReferenceType campaignRef = null;
        try {
            campaignRef = ((RawType) value.getRealValue()).getParsedRealValue(ObjectReferenceType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't parse campaignRef from task params", e);
        }
        if (campaignRef == null) {
            return false;
        }
        String oidParam = campaignRef.getOid();

        return campaignOid.equals(oidParam);
    }

    private StatisticBoxDto<TaskType> createTaskStatisticBoxDto(TaskType task) {
        DisplayType archetypeDisplay = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(task, getPageBase());
        String categoryInfo = GuiDisplayTypeUtil.getTranslatedLabel(archetypeDisplay);
        String iconFromArchetype = GuiDisplayTypeUtil.getIconCssClass(archetypeDisplay);
        String taskIcon = StringUtils.isNotEmpty(iconFromArchetype) ? iconFromArchetype : "fa fa-tasks";
        DisplayType displayType = new DisplayType()
                .label(task.getName())
                .help(categoryInfo)
                .icon(new IconType().cssClass(taskIcon));
        return new StatisticBoxDto<>(Model.of(displayType), null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public TaskType getStatisticObject() {
                return task;
            }
        };
    }

}

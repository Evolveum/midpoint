/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterativeTaskInformationType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * TODO MID-6850 (whole class)
 */
public class TaskIterativeInformationPanel extends BasePanel<IterativeTaskInformationType> {

    private static final Trace LOGGER = TraceManager.getTrace(TaskIterativeInformationPanel.class);

    private static final String ID_OBJECTS_PROCESSED_SUCCESS = "objectsProcessedSuccess";
    private static final String ID_OBJECTS_PROCESSED_SUCCESS_TIME = "objectsProcessedSuccessTime";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS = "lastObjectProcessedSuccess";
    private static final String ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME = "lastObjectProcessedSuccessTime";
    private static final String ID_OBJECTS_PROCESSED_FAILURE = "objectsProcessedFailure";
    private static final String ID_OBJECTS_PROCESSED_FAILURE_TIME = "objectsProcessedFailureTime";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE = "lastObjectProcessedFailure";
    private static final String ID_LAST_OBJECT_PROCESSED_FAILURE_TIME = "lastObjectProcessedFailureTime";
    private static final String ID_LAST_ERROR = "lastError";
    private static final String ID_CURRENT_OBJECT_PROCESSED = "currentObjectProcessed";
    private static final String ID_CURRENT_OBJECT_PROCESSED_TIME = "currentObjectProcessedTime";
    private static final String ID_OBJECTS_TOTAL = "objectsTotal";

    public TaskIterativeInformationPanel(String id, IModel<IterativeTaskInformationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        // TODO MID-6850
        Label processedSuccess = new Label(ID_OBJECTS_PROCESSED_SUCCESS, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_TOTAL_SUCCESS_COUNT.getLocalPart()));
        add(processedSuccess);

        Label processedSuccessTime = new Label(ID_OBJECTS_PROCESSED_SUCCESS_TIME, () -> {
                IterativeTaskInformationType info = getModelObject();
                if (info == null) {
                    return null;
                }
                if (info.getTotalSuccessCount() == 0) {
                    return null;
                } else {
                    return getString("TaskStatePanel.message.objectsProcessedTime",
                            info.getTotalSuccessDuration()/1000,
                            info.getTotalSuccessDuration()/info.getTotalSuccessCount());
                }
        });
        add(processedSuccessTime);

        // TODO MID-6850
        Label lastProcessedSuccess = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_LAST_SUCCESS_OBJECT_DISPLAY_NAME.getLocalPart()));
        add(lastProcessedSuccess);

        Label lastProcessedSuccessTime = new Label(ID_LAST_OBJECT_PROCESSED_SUCCESS_TIME, (IModel<String>) () -> {
            IterativeTaskInformationType info = getModelObject();
            if (info == null) {
                return null;
            }
            if (info.getLastSuccessEndTimestamp() == null) {
                return null;
            } else {
                if (showAgo(getModelObject())) {
                    return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                            formatDate(info.getLastSuccessEndTimestamp(), getPageBase()),
                            WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                    XmlTypeConverter.toMillis(info.getLastSuccessEndTimestamp()), true, true, getPageBase()),
                            info.getLastSuccessDuration());
                } else {
                    return getString("TaskStatePanel.message.timeInfoWithDuration",
                            formatDate(info.getLastSuccessEndTimestamp(), getPageBase()),
                            info.getLastSuccessDuration());
                }
            }
        });
        add(lastProcessedSuccessTime);

        // TODO MID-6850
        Label processedFailure = new Label(ID_OBJECTS_PROCESSED_FAILURE, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_TOTAL_FAILURE_COUNT.getLocalPart()));
        add(processedFailure);

        Label processedFailureTime = new Label(ID_OBJECTS_PROCESSED_FAILURE_TIME, (IModel<String>) () -> {
            IterativeTaskInformationType info = getModelObject();
            if (info == null) {
                return null;
            }
            if (info.getTotalFailureCount() == 0) {
                return null;
            } else {
                return getString("TaskStatePanel.message.objectsProcessedTime",
                        info.getTotalFailureDuration()/1000,
                        info.getTotalFailureDuration()/info.getTotalFailureCount());
            }
        });
        add(processedFailureTime);

        // TODO MID-6850
        Label lastProcessedFailure = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_LAST_FAILURE_OBJECT_DISPLAY_NAME.getLocalPart()));
        add(lastProcessedFailure);

        Label lastProcessedFailureTime = new Label(ID_LAST_OBJECT_PROCESSED_FAILURE_TIME, (IModel<String>) () -> {
            IterativeTaskInformationType info = getModelObject();
            if (info == null) {
                return null;
            }
            if (info.getLastFailureEndTimestamp() == null) {
                return null;
            } else {
                if (showAgo(info)) {
                    return getString("TaskStatePanel.message.timeInfoWithDurationAndAgo",
                            formatDate(info.getLastFailureEndTimestamp(), getPageBase()),
                            WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                    XmlTypeConverter.toMillis(info.getLastFailureEndTimestamp()), true, true, getPageBase()),
                            info.getLastFailureDuration());
                } else {
                    return getString("TaskStatePanel.message.timeInfoWithDuration",
                            formatDate(info.getLastFailureEndTimestamp(), getPageBase()),
                            info.getLastFailureDuration());
                }
            }
        });
        add(lastProcessedFailureTime);

        // TODO MID-6850
        Label lastError = new Label(ID_LAST_ERROR, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_LAST_FAILURE_EXCEPTION_MESSAGE.getLocalPart()));
        add(lastError);

        // TODO MID-6850
        Label currentObjectProcessed = new Label(ID_CURRENT_OBJECT_PROCESSED, new PropertyModel<>(getModel(), IterativeTaskInformationType.F_CURRENT_OBJECT_DISPLAY_NAME.getLocalPart()));
        add(currentObjectProcessed);

        Label currentObjectProcessedTime = new Label(ID_CURRENT_OBJECT_PROCESSED_TIME, (IModel<String>) () -> {
            IterativeTaskInformationType info = getModelObject();
            if (info == null) {
                return null;
            }
            if (info.getCurrentObjectStartTimestamp() == null) {
                return null;
            } else {
                return getString("TaskStatePanel.message.timeInfoWithAgo",
                        formatDate(info.getCurrentObjectStartTimestamp(), getPageBase()),
                        WebComponentUtil.formatDurationWordsForLocal(System.currentTimeMillis() -
                                XmlTypeConverter.toMillis(info.getCurrentObjectStartTimestamp()), true, true, getPageBase()));
            }
        });
        add(currentObjectProcessedTime);

        Label objectsTotal = new Label(ID_OBJECTS_TOTAL, (IModel<String>) () -> {
            IterativeTaskInformationType info = getModelObject();
            if (info == null) {
                return null;
            }
            int objectsTotal1 = info.getTotalSuccessCount() + info.getTotalFailureCount();
                Long avg = getWallClockAverage(objectsTotal1);
                if (avg != null) {
                    long throughput = avg != 0 ? 60000 / avg : 0;       // TODO what if avg == 0?
                    return getString("TaskStatePanel.message.objectsTotal",
                            objectsTotal1, avg, throughput);
                }
            return String.valueOf(objectsTotal1);
        });
        add(objectsTotal);
    }

    private String formatDate(XMLGregorianCalendar date, PageBase pageBase) {
        return formatDate(XmlTypeConverter.toDate(date), pageBase);
    }

    private String formatDate(Date date, PageBase pageBase) {
        if (date == null) {
            return null;
        }
        return WebComponentUtil.getLongDateTimeFormattedValue(date, pageBase);
    }

    protected boolean showAgo(IterativeTaskInformationType taskInfo) {
        boolean showAgo = false;
        if (taskInfo.getCurrentObjectDisplayName() != null) {
            showAgo = true; // for all running tasks
        }

        return showAgo;
    }

    protected Long getWallClockAverage(int objectsTotal) {
        return null;
    }
}

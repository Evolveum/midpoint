/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class InternalsThreadsPanel extends BasePanel<Void> {

    private static final Trace LOGGER = TraceManager.getTrace(InternalsThreadsPanel.class);

    private static final long serialVersionUID = 1L;

    private static final String ID_RESULT = "result";

    private static final String ID_SHOW_ALL_THREADS = "showAllThreads";
    private static final String ID_SHOW_TASKS_THREADS = "showTasksThreads";
    private static final String ID_RECORD_TASKS_THREADS = "recordTasksThreads";

    private IModel<String> resultModel = Model.of((String) null);

    public InternalsThreadsPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        AceEditor result = new AceEditor(ID_RESULT, resultModel);
        result.setReadonly(true);
        result.setResizeToMaxHeight(true);
        result.setMode(null);
        result.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return resultModel.getObject() != null;
            }
        });
        add(result);

        add(new AjaxButton(ID_SHOW_ALL_THREADS, createStringResource("InternalsThreadsPanel.button.showAllThreads")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                executeShowAllThreads(target);
            }
        });
        add(new AjaxButton(ID_SHOW_TASKS_THREADS, createStringResource("InternalsThreadsPanel.button.showTasksThreads")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                executeShowTasksThreads(target);
            }
        });
        add(new AjaxButton(ID_RECORD_TASKS_THREADS, createStringResource("InternalsThreadsPanel.button.recordTasksThreads")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void onClick(AjaxRequestTarget target) {
                executeRecordTasksThreads(target);
            }
        });

    }

    private void executeShowAllThreads(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(InternalsThreadsPanel.class.getName() + ".executeShowAllThreads");
        OperationResult result = task.getResult();

        try {
            String dump = getPageBase().getTaskService().getThreadsDump(task, result);
            LOGGER.debug("Threads:\n{}", dump);
            resultModel.setObject(dump);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError(getString("InternalsThreadsPanel.message.executeShowAllThreads.fatalError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get threads", e);
        } finally {
            result.computeStatus();
        }
        getPageBase().showResult(result);
        target.add(this, getPageBase().getFeedbackPanel());
    }

    private void executeShowTasksThreads(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(InternalsThreadsPanel.class.getName() + ".executeShowTasksThreads");
        OperationResult result = task.getResult();

        try {
            String dump = getPageBase().getTaskService().getRunningTasksThreadsDump(task, result);
            LOGGER.debug("Running tasks' threads:\n{}", dump);
            resultModel.setObject(dump);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError(getString("InternalsThreadsPanel.message.executeShowTasksThreads.fatalError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get tasks' threads", e);
        } finally {
            result.computeStatus();
        }
        getPageBase().showResult(result);
        target.add(this, getPageBase().getFeedbackPanel());
    }

    private void executeRecordTasksThreads(AjaxRequestTarget target) {
        Task task = getPageBase().createSimpleTask(InternalsThreadsPanel.class.getName() + ".executeRecordTasksThreads");
        OperationResult result = task.getResult();

        try {
            String info = getPageBase().getTaskService().recordRunningTasksThreadsDump(SchemaConstants.USER_REQUEST_URI, task, result);
            resultModel.setObject(info);
        } catch (CommonException | RuntimeException e) {
            result.recordFatalError(getString("InternalsThreadsPanel.message.executeRecordTasksThreads.fatalError"), e);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record tasks' threads", e);
        } finally {
            result.computeStatus();
        }
        getPageBase().showResult(result);
        target.add(this, getPageBase().getFeedbackPanel());
    }

}

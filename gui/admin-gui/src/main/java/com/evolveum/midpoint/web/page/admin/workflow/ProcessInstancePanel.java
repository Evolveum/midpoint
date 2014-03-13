/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wf.processes.EmptyProcessDetailsPanel;
import com.evolveum.midpoint.web.component.wf.processes.itemApproval.ItemApprovalPanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ItemApprovalProcessState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessInstanceState;
import com.evolveum.midpoint.xml.ns.model.workflow.process_instance_state_2.ProcessSpecificState;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class ProcessInstancePanel extends SimplePanel<ProcessInstanceDto> {

    private static final Trace LOGGER = TraceManager.getTrace(ProcessInstancePanel.class);

    private static final String ID_DETAILS = "details";
    private static final String ID_TASK = "task";
    private static final String ID_TASK_COMMENT = "taskComment";

    private static Map<Class<? extends ProcessSpecificState>,Class<? extends Panel>> panelsForProcesses = null;

    public static void registerProcessInstancePanel(Class<? extends ProcessSpecificState> dataClass, Class<? extends Panel> panelClass) {
        if (panelsForProcesses == null) {
            panelsForProcesses = new HashMap<>();
        }
        panelsForProcesses.put(dataClass, panelClass);
    }

    // TODO it would be nicer if individual panels could register themselves
    static {
        registerProcessInstancePanel(ItemApprovalProcessState.class, ItemApprovalPanel.class);
        registerProcessInstancePanel(ProcessSpecificState.class, EmptyProcessDetailsPanel.class);
    }

    public ProcessInstancePanel(String id, IModel<ProcessInstanceDto> model) {
        super(id, model);
        initLayoutLocal();
    }

    private void initLayoutLocal() {
        final IModel<ProcessInstanceDto> model = getModel();

        Label name = new Label("name", new PropertyModel(model, "name"));
        add(name);

        Label pid = new Label("pid", new PropertyModel(model, "instanceId"));
        add(pid);

        Label started = new Label("started", new PropertyModel(model, "startedTime"));
        add(started);

        Label finished = new Label("finished", new PropertyModel(model, "finishedTime"));
        add(finished);

        // todo what if task does not exist?
        LinkPanel task = new LinkPanel(ID_TASK, new PropertyModel(model, ProcessInstanceDto.F_WATCHING_TASK_OID)) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                String oid = model.getObject().getWatchingTaskOid();
                if (oid != null) {
                    PageParameters parameters = new PageParameters();
                    parameters.add(OnePageParameterEncoder.PARAMETER, oid);
                    setResponsePage(new PageTaskEdit(parameters, (PageBase) this.getPage()));
                }
            }
        };
        add(task);

        Label taskComment = new Label(ID_TASK_COMMENT, createStringResource("processInstancePanel.taskMightBeRemoved"));
        taskComment.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return model.getObject().isFinished();
            }
        });
        add(taskComment);

        try {
            Class<? extends Panel> panelClass = getDetailsPanelClassName();
            Panel detailsPanel = panelClass.getConstructor(String.class, IModel.class).newInstance(ID_DETAILS, model);
            add(detailsPanel);
        } catch (InvocationTargetException|InstantiationException|NoSuchMethodException|IllegalAccessException|RuntimeException e) {
            LoggingUtils.logException(LOGGER, "Details panel couldn't be shown", e);
            Label problemLabel = new Label(ID_DETAILS, "Details cannot be shown because of the following exception: " + e.getMessage() + ". Please see the log for more details");
            add(problemLabel);
        }
    }

    private Class<? extends Panel> getDetailsPanelClassName() {
        ProcessInstanceDto processInstanceDto = getModel().getObject();
        ProcessSpecificState processSpecificState = ((ProcessInstanceState) processInstanceDto.getProcessInstance().getState()).getProcessSpecificState();
        if (processSpecificState != null) {
            Class<? extends ProcessSpecificState> dataClass = processSpecificState.getClass();
            while (dataClass != null) {
                Class<? extends Panel> panelClass = panelsForProcesses.get(dataClass);
                if (panelClass != null) {
                    return panelClass;
                } else {
                    dataClass = (Class<? extends ProcessSpecificState>) dataClass.getSuperclass();
                }
            }
            throw new IllegalStateException("A panel for displaying workflow process state of type " + processInstanceDto.getProcessInstance().getState().getClass() + " couldn't be found");
        } else {
            return EmptyProcessDetailsPanel.class;
        }
    }


}

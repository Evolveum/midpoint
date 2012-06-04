/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.SqlRepositoryFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.ActivitiEngine;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import org.activiti.engine.FormService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.TaskService;
import org.activiti.engine.form.FormProperty;
import org.activiti.engine.form.FormType;
import org.activiti.engine.form.TaskFormData;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.GroupQuery;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Created with IntelliJ IDEA.
 * User: mederly
 * Date: 14.5.2012
 * Time: 13:20
 * To change this template use File | Settings | File Templates.
 */
@Component
public class WorkflowManager implements BeanFactoryAware {

    private static final transient Trace LOGGER = TraceManager.getTrace(WorkflowManager.class);

    @Autowired(required = true)
    ActivitiEngine activitiEngine;

    @Autowired(required = true)
    private HookRegistry hookRegistry;

    @Autowired(required = true)
    WfHook wfHook;

    @Autowired(required = true)
    PrismContext prismContext;

    @Autowired(required = true)
    MidpointConfiguration midpointConfiguration;

    private WfConfiguration wfConfiguration;

    private BeanFactory beanFactory;

    @PostConstruct
    public void initialize() throws Exception {     // todo exception handling

        wfConfiguration = new WfConfiguration();

        SqlRepositoryConfiguration sqlConfig;

        // activiti properties related to database connection will be taken from SQL repository
        // todo do this only when workflows are enabled
        try {
            SqlRepositoryFactory sqlRepositoryFactory = (SqlRepositoryFactory) beanFactory.getBean("sqlRepositoryFactory");
            sqlConfig = sqlRepositoryFactory.getSqlConfiguration();
        } catch(NoSuchBeanDefinitionException e) {
            LOGGER.debug("SqlRepositoryFactory is not available, Activiti database configuration (if any) will be taken from 'workflow' configuration section only.");
            LOGGER.trace("Reason is", e);
            sqlConfig = null;
        }

        wfConfiguration.initialize(midpointConfiguration, sqlConfig);

        if (!wfConfiguration.isEnabled()) {
            LOGGER.info("Workflow management is not enabled.");
        } else {

            activitiEngine.initialize(wfConfiguration);

            // todo move to wfhook
            LOGGER.trace("Registering workflow hook");
            hookRegistry.registerChangeHook(WfHook.WORKFLOW_HOOK_URI, wfHook);
        }
    }

    private static final QName WORK_ITEM_NAME = new QName(SchemaConstants.NS_C, "WorkItem");
    private static final QName WORK_ITEM_TASK_ID = new QName(SchemaConstants.NS_C, "taskId");
    private static final QName WORK_ITEM_TASK_NAME = new QName(SchemaConstants.NS_C, "taskName");
    private static final QName WORK_ITEM_ASSIGNEE = new QName(SchemaConstants.NS_C, "assignee");
    private static final QName WORK_ITEM_CANDIDATES = new QName(SchemaConstants.NS_C, "candidates");

    public int countWorkItemsAssignedToUser(String user, OperationResult parentResult) {
        TaskService taskService = activitiEngine.getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskAssignee(user);
        return (int) tq.count();
    }

    public List<WorkItem> listWorkItemsAssignedToUser(String user, int first, int count, OperationResult parentResult) {
        TaskService taskService = activitiEngine.getTaskService();
        TaskQuery tq = taskService.createTaskQuery();
        tq.taskAssignee(user);
        List<Task> tasks = tq.listPage(first, count);
        return tasksToWorkItems(tasks);
    }

    private List<WorkItem> tasksToWorkItems(List<Task> tasks) {
        List<WorkItem> retval = new ArrayList<WorkItem>();
        for (Task task : tasks)
            retval.add(taskToWorkItem(task));
        return retval;
    }

    private WorkItem taskToWorkItem(Task task) {
        WorkItem wi = new WorkItem();

        wi.setTaskId(task.getId());
        wi.setAssignee(task.getAssignee());
        wi.setName(task.getName());
        wi.setProcessId(task.getProcessInstanceId());
        wi.setCandidates(getCandidatesAsString(task));
        return wi;
    }

    public int countWorkItemsAssignableToUser(String user, OperationResult parentResult) {

        List<String> groups = groupsForUser(user);
        if (groups.isEmpty()) {
            return 0;
        } else {
            TaskService taskService = activitiEngine.getTaskService();
            TaskQuery tq = taskService.createTaskQuery();
            tq.taskCandidateGroupIn(groups);
            return (int) tq.count();
        }
    }

    public List<WorkItem> listWorkItemsAssignableToUser(String user, int first, int count, OperationResult parentResult) {

        List<String> groups = groupsForUser(user);
        List<Task> tasks;
        if (groups.isEmpty()) {
            tasks = new ArrayList<Task>();
        } else {
            TaskService taskService = activitiEngine.getTaskService();
            TaskQuery tq = taskService.createTaskQuery();
            tq.taskCandidateGroupIn(groups);
            tasks = tq.listPage(first, count);
        }

        LOGGER.trace("Activiti tasks assignable to " + user + ": " + tasks);
        return tasksToWorkItems(tasks);
    }

    private List<String> groupsForUser(String user) {
        IdentityService identityService = activitiEngine.getIdentityService();
        GroupQuery gq = identityService.createGroupQuery();
        gq.groupMember(user);
        List<String> groupNames = new ArrayList<String>();
        List<Group> groups = gq.list();
        LOGGER.trace("Activiti groups for " + user + ":");
        for (Group g : groups) {
            LOGGER.trace(" - group: id = " + g.getId() + ", name = " + g.getName());
            groupNames.add(g.getId());
        }
        return groupNames;
    }

    public void claimWorkItem(WorkItem workItem, String userId, OperationResult result) {
        TaskService taskService = activitiEngine.getTaskService();
        taskService.claim(workItem.getTaskId(), userId);
        result.recordSuccess();
    }

    public void releaseWorkItem(WorkItem workItem, OperationResult result) {
        TaskService taskService = activitiEngine.getTaskService();
        taskService.claim(workItem.getTaskId(), null);
        result.recordSuccess();
    }

    /**
     * Brutally hacked... should be completely rewritten
     */
    public PrismObject<? extends ObjectType> getWorkItemPrism(String taskId) throws SchemaException {

        Task task = activitiEngine.getTaskService().createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            return null;
        }

        // todo - use NS other than NS_C (at least for form properties)

        ComplexTypeDefinition ctd = new ComplexTypeDefinition(WORK_ITEM_NAME, WORK_ITEM_NAME, prismContext);
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_ID, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_TASK_NAME, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_ASSIGNEE, DOMUtil.XSD_STRING);
        ctd.createPropertyDefinifion(WORK_ITEM_CANDIDATES, DOMUtil.XSD_STRING);

        TaskFormData data = activitiEngine.getFormService().getTaskFormData(task.getId());

        for (FormProperty formProperty : data.getFormProperties()) {

            QName pname = new QName(SchemaConstants.NS_C, "WI_" + formProperty.getId());
            FormType t = formProperty.getType();
            String ts = t.getName();
            QName ptype;
            if ("string".equals(ts)) {
                ptype = DOMUtil.XSD_STRING;
            } else if ("boolean".equals(ts)) {
                ptype = DOMUtil.XSD_BOOLEAN;
            } else if ("long".equals(ts)) {
                ptype = DOMUtil.XSD_LONG;
            } else if ("date".equals(ts)) {
                ptype = new QName(W3C_XML_SCHEMA_NS_URI, "date",
                        DOMUtil.NS_W3C_XML_SCHEMA_PREFIX);
            } else if ("enum".equals(ts)) {
                ptype = DOMUtil.XSD_INT;        // TODO: implement somehow ...
            } else {
                LOGGER.warn("Unknown Activiti type: " + ts);
                continue;
            }

            ctd.createPropertyDefinifion(pname, ptype);
        }

        ctd.setObjectMarker(true);

        PrismObjectDefinition<ObjectType> prismObjectDefinition = new PrismObjectDefinition<ObjectType>(WORK_ITEM_NAME, ctd, prismContext, ObjectType.class);
        PrismObject<ObjectType> instance = prismObjectDefinition.instantiate();
        instance.findOrCreateProperty(WORK_ITEM_TASK_ID).setValue(new PrismPropertyValue<Object>(taskId));
        instance.findOrCreateProperty(WORK_ITEM_TASK_NAME).setValue(new PrismPropertyValue<Object>(task.getName()));
        if (task.getAssignee() != null) {
            instance.findOrCreateProperty(WORK_ITEM_ASSIGNEE).setValue(new PrismPropertyValue<Object>(task.getAssignee()));
        }
        String candidates = getCandidatesAsString(task);
        if (candidates != null) {
            instance.findOrCreateProperty(WORK_ITEM_CANDIDATES).setValue(new PrismPropertyValue<Object>(candidates));
        }

        for (FormProperty formProperty : data.getFormProperties()) {

            QName pname = new QName(SchemaConstants.NS_C, "WI_" + formProperty.getId());
            instance.findOrCreateProperty(pname).setValue(new PrismPropertyValue<Object>(formProperty.getValue()));
        }

        return instance;
    }

    private List<String> getCandidates(Task task) {

        List<String> retval = new ArrayList<String>();

        TaskService taskService = activitiEngine.getTaskService();

        List<IdentityLink> ils = taskService.getIdentityLinksForTask(task.getId());
        for (IdentityLink il : ils) {
            if ("candidate".equals(il.getType())) {
                if (il.getGroupId() != null) {
                    retval.add("G:" + il.getGroupId());
                }
                if (il.getUserId() != null) {
                    retval.add("U:" + il.getUserId());
                }
            }
        }

        return retval;
    }

    private String getCandidatesAsString(Task task) {

        StringBuffer retval = new StringBuffer();
        boolean first = true;
        for (String c : getCandidates(task)) {
            if (first) {
                first = false;
            } else {
                retval.append(", ");
            }
            retval.append(c);
        }
        return retval.toString();
    }

    public void saveWorkItemPrism(PrismObject object, OperationResult result) {

        String taskId = (String) object.getPropertyRealValue(WORK_ITEM_TASK_ID, String.class);

        LOGGER.trace("Saving work item " + taskId);

        FormService formService = activitiEngine.getFormService();
        Map<String,String> propertiesToSubmit = new HashMap<String,String>();
        TaskFormData data = activitiEngine.getFormService().getTaskFormData(taskId);

        for (FormProperty formProperty : data.getFormProperties()) {

            if (formProperty.isWritable()) {
                QName propertyName = new QName(SchemaConstants.NS_C, "WI_" + formProperty.getId());

                Object value = object.getPropertyRealValue(propertyName, Object.class);
                LOGGER.trace("Writable property " + formProperty.getId() + " has a value of " + value);
                if (value != null) {
                    propertiesToSubmit.put(formProperty.getId(), value.toString());
                }
            }
        }

        formService.submitTaskFormData(taskId, propertiesToSubmit);
    }


    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public WfConfiguration getWfConfiguration() {
        return wfConfiguration;
    }

    public boolean isEnabled() {
        return wfConfiguration.isEnabled();
    }
}

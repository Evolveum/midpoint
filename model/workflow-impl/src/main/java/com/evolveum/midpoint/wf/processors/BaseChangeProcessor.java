package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.jobs.Job;
import com.evolveum.midpoint.wf.jobs.JobCreateInstruction;
import com.evolveum.midpoint.wf.jobs.JobController;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.jobs.WfTaskUtil;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Element;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author mederly
 */
public abstract class BaseChangeProcessor implements ChangeProcessor, BeanNameAware, BeanFactoryAware {

    private static final Trace LOGGER = TraceManager.getTrace(BaseChangeProcessor.class);

    private static final String KEY_ENABLED = "enabled";
    private static final List<String> KNOWN_KEYS = Arrays.asList(KEY_ENABLED);

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected WfTaskUtil wfTaskUtil;

    @Autowired
    protected TaskManager taskManager;

    @Autowired
    protected JobController jobController;

    private Configuration processorConfiguration;

    private String beanName;
    private BeanFactory beanFactory;

    private boolean enabled = false;

    //region Initialization and configuration
    // =================================================================================== Initialization and configuration

    protected void initializeBaseProcessor() {
        initializeBaseProcessor(null);
    }

    protected void initializeBaseProcessor(List<String> locallyKnownKeys) {

        Validate.notNull(beanName, "Bean name was not set correctly.");

        Configuration c = wfConfiguration.getChangeProcessorsConfig().subset(beanName);
        if (c.isEmpty()) {
            LOGGER.info("Skipping reading configuration of " + beanName + ", as it is not on the list of change processors or is empty.");
            return;
        }

        List<String> allKnownKeys = new ArrayList<String>(KNOWN_KEYS);
        if (locallyKnownKeys != null) {
            allKnownKeys.addAll(locallyKnownKeys);
        }
        wfConfiguration.checkAllowedKeys(c, allKnownKeys);

        enabled = c.getBoolean(KEY_ENABLED, true);
        if (!enabled) {
            LOGGER.info("Change processor " + beanName + " is DISABLED.");
        }
        processorConfiguration = c;
    }

    protected Configuration getProcessorConfiguration() {
        return processorConfiguration;
    }

    protected String getBeanName() {
        return beanName;
    }

    protected BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setBeanName(String name) {
        LOGGER.trace("Setting bean name to {}", name);
        this.beanName = name;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    protected void validateElement(Element element) throws SchemaException {
        OperationResult result = new OperationResult("validateElement");
        Validator validator = new Validator(prismContext);
        validator.validateSchema(element, result);
        result.computeStatus();
        if (!result.isSuccess()) {
            throw new SchemaException(result.getMessage(), result.getCause());
        }
    }
    //endregion

    //region Processing model invocation
    // =================================================================================== Processing model invocation

    protected Job createRootJob(ModelContext context, Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        JobCreateInstruction rootInstruction = createInstructionForRoot(context, taskFromModel, prepareRootTaskName(context), determineTaskObject(context));
        return jobController.createJob(rootInstruction, determineParentTaskForRoot(taskFromModel), result);
    }

    // to which object (e.g. user) is the task related?
    protected PrismObject determineTaskObject(ModelContext context) {
        PrismObject taskObject = context.getFocusContext().getObjectNew();
        if (taskObject != null && taskObject.getOid() == null) {
            taskObject = null;
        }
        return taskObject;
    }

    protected JobCreateInstruction createInstructionForRoot(ModelContext modelContext, Task taskFromModel, String defaultTaskName, PrismObject taskObject) throws SchemaException {

        JobCreateInstruction instruction;
        if (modelContext != null) {
            instruction = JobCreateInstruction.createModelOperationRootJob(this, modelContext);
        } else {
            instruction = JobCreateInstruction.createNoModelOperationRootJob(this);
        }

        instruction.setTaskName(new PolyStringType(defaultTaskName));
        instruction.setTaskObject(taskObject);
        instruction.setTaskOwner(taskFromModel.getOwner());

        // At this moment, we HAVE NOT entered wait-for-tasks state, because we have no prerequisite tasks (in this case,
        // children) defined yet. Entering that state would result in immediate execution of this task. We have to
        // enter this state only after all children tasks are created.
        instruction.setCreateWaiting(true);

        return instruction;
    }

    protected String prepareRootTaskName(ModelContext context) {

        String operation;
        if (context.getFocusContext() != null && context.getFocusContext().getPrimaryDelta() != null) {
            operation = context.getFocusContext().getPrimaryDelta().getChangeType().toString().toLowerCase();
        } else {
            operation = "processing";
        }
        String name = MiscDataUtil.getObjectName(context);

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String time = dateFormat.format(new Date());

        return "Workflow for " + operation + " " + name + " (started " + time + ")";
    }

    protected Task determineParentTaskForRoot(Task taskFromModel) {

        // this is important: if existing task which we have got from model is transient (this is usual case), we create our root task as a task without parent!
        // however, if the existing task is persistent (perhaps because the model operation executes already in the context of a workflow), we create a subtask
        // todo think heavily about this; there might be a problem if a transient task from model gets (in the future) persistent
        // -- in that case, it would not wait for its workflow-related children (but that's its problem, because children could finish even before
        // that task is switched to background)

        if (taskFromModel.isTransient()) {
            return null;
        } else {
            return taskFromModel;
        }
    }

    protected Job createRootJob(JobCreateInstruction rootInstruction, Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Job rootJob = jobController.createJob(rootInstruction, determineParentTaskForRoot(taskFromModel), result);
        wfTaskUtil.setRootTaskOidImmediate(taskFromModel, rootJob.getTask().getOid(), result);
        return rootJob;
    }

    // todo what with this?
    public void prepareCommonInstructionAttributes(JobCreateInstruction instruction, String objectOid, PrismObject<UserType> requester) {
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID, requester.getOid());
        if (objectOid != null) {
            instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
        }

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_UTIL, new ActivitiUtil());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR, this.getClass().getName());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_START_TIME, new Date());
        instruction.setNoProcess(false);
    }

    protected void logTasksBeforeStart(Job rootJob, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        if (LOGGER.isTraceEnabled()) {
            Task rootTask = rootJob.getTask();
            LOGGER.trace("============ Situation just before root task starts waiting for subtasks ============");
            LOGGER.trace("Root task = " + rootTask.dump());
            if (wfTaskUtil.hasModelContext(rootTask)) {
                LOGGER.trace("Context in root task = " + wfTaskUtil.retrieveModelContext(rootTask, result).debugDump());
            }
            for (Task child : rootTask.listSubtasks(result)) {
                LOGGER.trace("Child task = " + child.dump());
                if (wfTaskUtil.hasModelContext(child)) {
                    LOGGER.trace("Context in child task = " + wfTaskUtil.retrieveModelContext(child, result).debugDump());
                }
            }
            LOGGER.trace("Now the root task starts waiting for child tasks");
        }
    }
    //endregion

    //region Getters and setters
    public WfTaskUtil getWfTaskUtil() {
        return wfTaskUtil;
    }
    //endregion

}

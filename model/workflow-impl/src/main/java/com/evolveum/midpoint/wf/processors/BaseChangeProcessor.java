package com.evolveum.midpoint.wf.processors;

import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.ProcessInstanceController;
import com.evolveum.midpoint.wf.StartProcessInstruction;
import com.evolveum.midpoint.wf.WfConfiguration;
import com.evolveum.midpoint.wf.WfTaskUtil;
import com.evolveum.midpoint.wf.activiti.ActivitiUtil;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.util.MiscDataUtil;
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
    protected ProcessInstanceController processInstanceController;

    private Configuration processorConfiguration;

    private String beanName;
    private BeanFactory beanFactory;

    private boolean enabled = false;

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


    protected void prepareAndSaveRootTask(ModelContext rootContext, Task task, String defaultTaskName, PrismObject taskObject, OperationResult result) throws SchemaException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareAndSaveRootTask starting; task = " + task + ", model context to be stored = " + (rootContext != null ? rootContext.debugDump() : "none"));
        }

        if (!task.isTransient()) {
            throw new IllegalStateException("Workflow-related task should be transient but this one is persistent; task = " + task);
        }

        wfTaskUtil.setTaskNameIfEmpty(task, new PolyStringType(defaultTaskName));
        task.setCategory(TaskCategory.WORKFLOW);

        if (taskObject != null) {
            task.setObjectTransient(taskObject);
        }

        // At this moment, we HAVE NOT entered wait-for-tasks state, because we have no prerequisite tasks (in this case,
        // children) defined yet. Entering that state would result in immediate execution of this task. We have to
        // enter this state after all children tasks are created.

        task.setInitialExecutionStatus(TaskExecutionStatus.WAITING);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Saving root task: " + task.dump());
        }

        taskManager.switchToBackground(task, result);
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

    protected void logTasksBeforeStart(Task rootTask, OperationResult result) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException {
        if (LOGGER.isTraceEnabled()) {
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

    protected void validateElement(Element element) throws SchemaException {
        OperationResult result = new OperationResult("validateElement");
        Validator validator = new Validator(prismContext);
        validator.validateSchema(element, result);
        result.computeStatus();
        if (!result.isSuccess()) {
            throw new SchemaException(result.getMessage(), result.getCause());
        }
    }

    public void prepareCommonInstructionAttributes(StartProcessInstruction instruction, String objectOid, String requesterOid) {

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID, requesterOid);
        if (objectOid != null) {
            instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID, objectOid);
        }

        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_UTIL, new ActivitiUtil());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_CHANGE_PROCESSOR, this.getClass().getName());
        instruction.addProcessVariable(CommonProcessVariableNames.VARIABLE_START_TIME, new Date());
        instruction.setNoProcess(false);
    }


}

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.impl.tasks.WfTask;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskController;
import com.evolveum.midpoint.wf.impl.tasks.WfTaskUtil;
import com.evolveum.midpoint.wf.impl.processors.ChangeProcessor;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.List;

/**
 * A job enhanced with PrimaryChangeProcessor-specific features.
 *
 * @author mederly
 */
public class PcpWfTask extends WfTask {

    PcpWfTask(WfTask original) {
        super(original);
    }

	public PcpWfTask(WfTaskController wfTaskController, Task task, String processInstanceId, ChangeProcessor changeProcessor) {
		super(wfTaskController, task, processInstanceId, changeProcessor);
	}

    public PrimaryChangeAspect getChangeAspect() {
        return getWfTaskUtil().getPrimaryChangeAspect(getTask(), getPcp().getAllChangeAspects());
    }

    private PrimaryChangeProcessor getPcp() {
        return (PrimaryChangeProcessor) getChangeProcessor();
    }

    private WfTaskUtil getWfTaskUtil() {
        return getPcp().getWfTaskUtil();
    }

    public void storeResultingDeltas(ObjectTreeDeltas deltas) throws SchemaException {
        getWfTaskUtil().storeResultingDeltas(deltas, getTask());
    }

    public ObjectTreeDeltas retrieveDeltasToProcess() throws SchemaException {
        return getWfTaskUtil().retrieveDeltasToProcess(getTask());
    }
}

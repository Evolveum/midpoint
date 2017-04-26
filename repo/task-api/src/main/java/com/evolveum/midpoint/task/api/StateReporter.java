/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.statistics.ProvisioningOperation;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;
import java.util.Date;

/**
 * TODO better name (ProgressReporter ? StatisticsReporter ? ...)
 *
 * Used to report state, progress and performance statistics to upper layers.
 * Generally a Task is the place where such information are reported and collected.
 *
 * However, because of a complex nature of some operations (namely, search) it tries
 * to remember the state of an operation.
 *
 * TODO maybe this could be simplified in the future.
 *
 * @author Pavol Mederly
 */
public class StateReporter {

    private static final Trace LOGGER = TraceManager.getTrace(StateReporter.class);

    private Task task;
    private String resourceOid;
    private String resourceName;            // lazily set when available

    public StateReporter() {
    }

    public StateReporter(String resourceOid, Task task) {
        this.resourceOid = resourceOid;
        this.task = task;
    }

    private String getResourceName() {
        if (resourceName != null) {
            return resourceName;
        } else {
            return resourceOid;
        }
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    // Operational information

    private ProvisioningOperation lastOperation = null;
    private ObjectClassComplexTypeDefinition lastObjectClass = null;
    private Date lastStarted = null;

    public void recordIcfOperationStart(ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDef, String identifier) {
        if (lastOperation != null) {
            LOGGER.warn("Unfinished operation: {}, resource: {}, OC: {}, started: {}", lastOperation, getResourceName(), lastObjectClass, lastStarted);
        }
        lastOperation = operation;
        lastObjectClass = objectClassDef;
        lastStarted = new Date();
        String object = "";
        if (identifier != null) {
            object = " " + identifier;
        }
        recordState("Starting " + operation + " of " + getObjectClassName(objectClassDef) + object + " on " + getResourceName());
    }

    // we just add duration, not count (we'll do this on end)
    public void recordIcfOperationSuspend(ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDef) {
        if (lastOperation != operation) {
            LOGGER.warn("Suspending operation other than current: finishing {}, last recorded {}",
                    operation, lastOperation);
        } else if (lastObjectClass == null || !lastObjectClass.getTypeName().equals(objectClassDef.getTypeName())) {
            LOGGER.warn("Suspending operation on object class other than current: finishing on {}, last recorded {}",
                    objectClassDef.getTypeName(), lastObjectClass != null ? lastObjectClass.getTypeName() : "(null)");
        } else {
            long duration = System.currentTimeMillis() - lastStarted.getTime();
            if (task != null) {
                task.recordProvisioningOperation(resourceOid, getResourceName(), objectClassDef.getTypeName(), lastOperation, true, 0, duration);
            } else {
                reportNoTask(resourceOid, lastOperation);
            }
        }
        lastOperation = null;
        recordState("Returned from " + operation + " of " + objectClassDef.getTypeName().getLocalPart() + " on " + getResourceName());
    }

    public void recordIcfOperationResume(ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDef) {
        if (lastOperation != null) {
            LOGGER.warn("Unfinished operation: {}, resource: {}, OC: {}, started: {}", lastOperation, getResourceName(), lastObjectClass, lastStarted);
        }
        lastOperation = operation;
        lastObjectClass = objectClassDef;
        lastStarted = new Date();
        recordState("Continuing " + operation + " of " + objectClassDef.getTypeName().getLocalPart() + " on " + getResourceName());
    }

    private String getObjectClassName(ObjectClassComplexTypeDefinition objectClassDef) {
        return objectClassDef != null && objectClassDef.getTypeName() != null ? objectClassDef.getTypeName().getLocalPart() : "(null)";
    }

    private QName getObjectClassQName(ObjectClassComplexTypeDefinition objectClassDef) {
        return objectClassDef != null ? objectClassDef.getTypeName() : null;
    }

    public void recordIcfOperationEnd(ProvisioningOperation operation, ObjectClassComplexTypeDefinition objectClassDef, Throwable ex, String identifier) {
        long duration = -1L;
        if (lastOperation != operation) {
            LOGGER.warn("Finishing operation other than current: finishing {}, last recorded {}",
                    operation, lastOperation);
        } else if (objectClassDef != null && (lastObjectClass == null || !lastObjectClass.getTypeName().equals(objectClassDef.getTypeName()))) {
            LOGGER.warn("Finishing operation on object class other than current: finishing on {}, last recorded {}",
                    getObjectClassName(objectClassDef), getObjectClassName(lastObjectClass));
        } else {
            duration = System.currentTimeMillis() - lastStarted.getTime();
        }

        String finished;
        if (ex == null) {
            finished = "Successfully finished";
        } else {
            finished = "Finished (unsuccessfully)";
        }
        String durationString;
        if (duration >= 0) {
            durationString = " in " + duration + " ms";
        } else {
            durationString = "";
        }
        String object = "";
        if (identifier != null) {
            object = " " + identifier;
        }
		final String stateMessage =
				finished + " " + operation + " of " + getObjectClassName(objectClassDef) + object + " on " + getResourceName() + durationString;
		recordState(stateMessage);
        if (task != null) {
            if (duration >= 0) {
                task.recordProvisioningOperation(resourceOid, getResourceName(), getObjectClassQName(objectClassDef), lastOperation, ex == null, 1, duration);
            } else {
				LOGGER.warn("Negative duration while recording provisiong operation: {}", stateMessage);
			}
        } else {
            reportNoTask(resourceOid, lastOperation);
        }
        lastOperation = null;
    }

    private void reportNoTask(String resourceOid, ProvisioningOperation operation) {
        LOGGER.warn("Couldn't report execution of ICF operation {} on resource {} because there is no task assigned.", operation, resourceOid);
    }

    private void recordState(String message) {
        if (task != null) {
            task.recordState(message);
        }
    }

    public void setTask(Task task) {
        this.task = task;
    }

	public Task getTask() {
		return task;
	}

	public String getResourceOid() {
		return resourceOid;
	}

	public void setResourceOid(String resourceOid) {
		this.resourceOid = resourceOid;
	}
}

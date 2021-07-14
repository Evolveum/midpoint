/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task.workers;

import org.springframework.stereotype.Component;

@Component
public class Temp {

//    public void reconcileWorkers(String coordinatorTaskOid, WorkersReconciliationOptions options, OperationResult parentResult)
//            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
//        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "reconcileWorkers");
//        result.addParam("coordinatorTaskOid", coordinatorTaskOid);
//        try {
//            workersManager.reconcileWorkers(coordinatorTaskOid, options, result);
//        } catch (Throwable t) {
//            result.recordFatalError("Couldn't reconcile workers", t);
//            throw t;
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//    }
//
//    public void deleteWorkersAndWorkState(String rootTaskOid, boolean deleteWorkers, long subtasksWaitTime,
//            OperationResult parentResult)
//            throws SchemaException, ObjectNotFoundException {
//        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "deleteWorkersAndWorkState");
//        result.addParam("rootTaskOid", rootTaskOid);
//        result.addParam("deleteWorkers", deleteWorkers);
//        result.addParam("subtasksWaitTime", subtasksWaitTime);
//        try {
//            workersManager.deleteWorkersAndWorkState(rootTaskOid, deleteWorkers, subtasksWaitTime, result);
//        } catch (Throwable t) {
//            result.recordFatalError("Couldn't delete workers and work state", t);
//            throw t;
//        } finally {
//            result.computeStatusIfUnknown();
//        }
//    }

}

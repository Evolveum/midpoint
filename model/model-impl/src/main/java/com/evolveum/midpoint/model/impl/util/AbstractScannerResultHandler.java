/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.util;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeResultHandler;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AbstractScannerResultHandler<O extends ObjectType> extends
        AbstractSearchIterativeResultHandler<O> {

    protected XMLGregorianCalendar lastScanTimestamp;
    private XMLGregorianCalendar thisScanTimestamp;

    protected AbstractScannerResultHandler(RunningTask coordinatorTask, String taskOperationPrefix,
            String processShortName, String contextDesc, TaskManager taskManager) {
        super(coordinatorTask, taskOperationPrefix, processShortName, contextDesc, taskManager);
    }

    public XMLGregorianCalendar getLastScanTimestamp() {
        return lastScanTimestamp;
    }

    public void setLastScanTimestamp(XMLGregorianCalendar lastScanTimestamp) {
        this.lastScanTimestamp = lastScanTimestamp;
    }

    public XMLGregorianCalendar getThisScanTimestamp() {
        return thisScanTimestamp;
    }

    void setThisScanTimestamp(XMLGregorianCalendar thisScanTimestamp) {
        this.thisScanTimestamp = thisScanTimestamp;
    }
}

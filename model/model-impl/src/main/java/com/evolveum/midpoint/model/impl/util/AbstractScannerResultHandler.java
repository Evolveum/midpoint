package com.evolveum.midpoint.model.impl.util;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class AbstractScannerResultHandler<O extends ObjectType> extends
		AbstractSearchIterativeResultHandler<O> {

	protected XMLGregorianCalendar lastScanTimestamp;
	protected XMLGregorianCalendar thisScanTimestamp;

	public AbstractScannerResultHandler(Task coordinatorTask, String taskOperationPrefix,
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

	public void setThisScanTimestamp(XMLGregorianCalendar thisScanTimestamp) {
		this.thisScanTimestamp = thisScanTimestamp;
	}



}

package com.evolveum.midpoint.model.util;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

public abstract class AbstractScannerResultHandler<O extends ObjectType> extends
		AbstractSearchIterativeResultHandler<O> {
	
	protected XMLGregorianCalendar lastScanTimestamp;
	protected XMLGregorianCalendar thisScanTimestamp;

	public AbstractScannerResultHandler(Task task, String taskOperationPrefix,
			String processShortName, String contextDesc) {
		super(task, taskOperationPrefix, processShortName, contextDesc);
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

package com.evolveum.midpoint.wf.impl.jobs;

import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.Map;

/**
 * @author mederly
 *
 * TODO place appropriately
 */
public class WfUtil {

	public static ObjectReferenceType toObjectReferenceType(LightweightObjectRef ref) {
		if (ref != null) {
			return ref.toObjectReferenceType();
		} else {
			return null;
		}
	}

}

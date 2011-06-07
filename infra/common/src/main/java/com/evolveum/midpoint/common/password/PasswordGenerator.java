package com.evolveum.midpoint.common.password;

import java.util.ArrayList;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.LimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PasswordPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.StringLimitType;

public class PasswordGenerator {

	private static final transient Trace logger = TraceManager.getTrace(PasswordGenerator.class);
	
	public static String generate (PasswordPolicyType pp, OperationResult generatorResult) {
		StringBuilder sb = new StringBuilder();
		
		
		
		
		generatorResult.recordSuccess();
		return sb.toString();
	}
	
	/******************************************************
	 *  Private helper methods
	 ******************************************************/
	
}

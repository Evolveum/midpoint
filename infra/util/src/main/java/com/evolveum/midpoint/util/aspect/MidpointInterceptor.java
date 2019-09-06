/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.util.aspect;

import com.evolveum.midpoint.util.statistics.OperationInvocationRecord;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 *  In this class, we define some Pointcuts in AOP meaning that will provide join points for most common
 *  methods used in main midPoint subsystems. We wrap these methods with profiling wrappers.
 *
 *  This class also serves another purpose - it is used for basic Method Entry/Exit or args profiling,
 *  results from which are dumped to idm.log (by default)
 *
 *  @author shood
 * */

@Order(value = Ordered.HIGHEST_PRECEDENCE)
public class MidpointInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		OperationInvocationRecord ctx = OperationInvocationRecord.create(invocation);
		try {
			return ctx.processReturnValue(invocation.proceed());
		} catch (Throwable e) {
			throw ctx.processException(e);
		} finally {
			ctx.afterCall(invocation);
		}
	}
}

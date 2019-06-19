/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

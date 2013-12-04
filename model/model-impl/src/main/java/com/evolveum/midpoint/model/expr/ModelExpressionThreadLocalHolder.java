/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.expr;

import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;

/**
 * @author Radovan Semancik
 *
 */
public class ModelExpressionThreadLocalHolder {
	
	private static ThreadLocal<LensContext<ObjectType, ShadowType>> lensContext = new ThreadLocal<LensContext<ObjectType, ShadowType>>();
	private static ThreadLocal<OperationResult> currentResult = new ThreadLocal<OperationResult>();
	private static ThreadLocal<Task> currentTask = new ThreadLocal<Task>();
	
	public static <F extends ObjectType, P extends ObjectType> void setLensContext(LensContext<F, P> ctx) {
		lensContext.set((LensContext<ObjectType, ShadowType>) ctx);
	}
	
	public static <F extends ObjectType, P extends ObjectType> void resetLensContext() {
		lensContext.set(null);
	}

	public static <F extends ObjectType, P extends ObjectType> LensContext<F, P> getLensContext() {
		return (LensContext<F, P>) lensContext.get();
	}
	
	public static void setCurrentResult(OperationResult result) {
		currentResult.set(result);
	}
	
	public static void resetCurrentResult() {
		currentResult.set(null);
	}

	public static OperationResult getCurrentResult() {
		return currentResult.get();
	}

	public static void setCurrentTask(Task task) {
		currentTask.set(task);
	}
	
	public static void resetCurrentTask() {
		currentTask.set(null);
	}

	public static Task getCurrentTask() {
		return currentTask.get();
	}

}

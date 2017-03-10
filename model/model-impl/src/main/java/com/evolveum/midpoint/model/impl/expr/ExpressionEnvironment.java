/**
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ExpressionEnvironment<F extends ObjectType> {

	private LensContext<F> lensContext;
	private LensProjectionContext projectionContext;
	private OperationResult currentResult;
	private Task currentTask;

	public ExpressionEnvironment() {
	}

	public ExpressionEnvironment(Task currentTask, OperationResult currentResult) {
		this.currentResult = currentResult;
		this.currentTask = currentTask;
	}

	public ExpressionEnvironment(LensContext<F> lensContext, LensProjectionContext projectionContext,
			Task currentTask, OperationResult currentResult) {
		this.lensContext = lensContext;
		this.projectionContext = projectionContext;
		this.currentResult = currentResult;
		this.currentTask = currentTask;
	}

	public LensContext<F> getLensContext() {
		return lensContext;
	}

	public void setLensContext(LensContext<F> lensContext) {
		this.lensContext = lensContext;
	}

	public LensProjectionContext getProjectionContext() {
		return projectionContext;
	}

	public void setProjectionContext(LensProjectionContext projectionContext) {
		this.projectionContext = projectionContext;
	}

	public OperationResult getCurrentResult() {
		return currentResult;
	}

	public void setCurrentResult(OperationResult currentResult) {
		this.currentResult = currentResult;
	}

	public Task getCurrentTask() {
		return currentTask;
	}

	public void setCurrentTask(Task currentTask) {
		this.currentTask = currentTask;
	}

	@Override
	public String toString() {
		return "ExpressionEnvironment(lensContext=" + lensContext + ", projectionContext="
				+ projectionContext + ", currentResult=" + currentResult + ", currentTask=" + currentTask
				+ ")";
	}
	
}

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

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mederly
 */
public class PipelineItem implements DebugDumpable, Serializable {

	@NotNull private PrismValue value;
	@NotNull private OperationResult result;
	// variables here are to be cloned-on-use (if they are not immutable)
	@NotNull private final Map<String,Object> variables = new HashMap<>();

	public PipelineItem(@NotNull PrismValue value, @NotNull OperationResult result) {
		this.value = value;
		this.result = result;
	}

	public PipelineItem(@NotNull PrismValue value, @NotNull OperationResult result, @NotNull Map<String, Object> variables) {
		this.value = value;
		this.result = result;
		this.variables.putAll(variables);
	}

	@NotNull
	public PrismValue getValue() {
		return value;
	}

	public void setValue(@NotNull PrismValue value) {
		this.value = value;
	}

	@NotNull
	public OperationResult getResult() {
		return result;
	}

	public void setResult(@NotNull OperationResult result) {
		this.result = result;
	}

	@NotNull
	public Map<String, Object> getVariables() {
		return variables;
	}

	// don't forget to clone on use
//	@SuppressWarnings("unchecked")
//	public <X> X getVariable(String name) {
//		return (X) variables.get(name);
//	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDumpWithLabelLn(sb, "value", value, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "result", result, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "variables", result, indent+1);
		return sb.toString();
	}

	public void computeResult() {
		result.computeStatus();
	}

	public PipelineItem cloneMutableState() {
		// note that variables are cloned on use
		return new PipelineItem(value, result.clone(), variables);
	}
}

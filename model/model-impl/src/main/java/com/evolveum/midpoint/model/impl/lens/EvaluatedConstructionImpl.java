/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author mederly
 */
public class EvaluatedConstructionImpl implements EvaluatedConstruction {

    final private PrismObject<ResourceType> resource;
    final private ShadowKindType kind;
    final private String intent;
    final private boolean directlyAssigned;
    final private boolean weak;

	public <F extends FocusType> EvaluatedConstructionImpl(Construction<F> construction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        resource = construction.getResource(task, result).asPrismObject();
        kind = construction.getKind();
        intent = construction.getIntent();
        directlyAssigned = construction.getAssignmentPath() == null || construction.getAssignmentPath().size() == 1;
        weak = construction.isWeak();
    }
    
    @Override
    public PrismObject<ResourceType> getResource() {
        return resource;
    }

    @Override
    public ShadowKindType getKind() {
        return kind;
    }

    @Override
    public String getIntent() {
        return intent;
    }

    @Override
    public boolean isDirectlyAssigned() {
        return directlyAssigned;
    }

    @Override
    public boolean isWeak() {
		return weak;
	}

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabelLn(sb, "EvaluatedConstruction", indent);
        DebugUtil.debugDumpWithLabelLn(sb, "resource", resource, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "kind", kind.value(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "intent", intent, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "directlyAssigned", directlyAssigned, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "weak", weak, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "EvaluatedConstruction(" +
                "resource=" + resource +
                ", kind=" + kind +
                ", intent='" + intent +
                ')';
    }
}

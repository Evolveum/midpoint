/*
 * Copyright (c) 2010-2015 Evolveum
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

    private PrismObject<ResourceType> resource;
    private ShadowKindType kind;
    private String intent;
    private boolean directlyAssigned;

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

    public void setDirectlyAssigned(boolean directlyAssigned) {
        this.directlyAssigned = directlyAssigned;
    }

    public <F extends FocusType> EvaluatedConstructionImpl(Construction<F> construction, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        resource = construction.getResource(task, result).asPrismObject();
        kind = construction.getKind();
        intent = construction.getIntent();
        directlyAssigned = construction.getAssignmentPath() == null || construction.getAssignmentPath().size() == 1;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    // TODO polish
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpLabel(sb, "EvaluatedConstruction", indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Resource", resource, indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Kind", kind.value(), indent + 1);
        sb.append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Intent", intent, indent + 1);
        return sb.toString();
    }

    // TODO polish
    @Override
    public String toString() {
        return "EvaluatedConstruction{" +
                "resource=" + resource +
                ", kind=" + kind +
                ", intent='" + intent + '\'' +
                '}';
    }
}

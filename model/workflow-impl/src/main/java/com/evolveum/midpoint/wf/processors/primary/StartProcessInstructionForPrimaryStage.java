/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.processors.primary;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.wf.StartProcessInstruction;
import com.evolveum.midpoint.wf.processors.primary.PrimaryApprovalProcessWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * StartProcessInstruction to be used in primary-stage approval processes.
 *
 * @author mederly
 */
public class StartProcessInstructionForPrimaryStage extends StartProcessInstruction implements DebugDumpable {

    private List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
    private PrimaryApprovalProcessWrapper wrapper;

    public List<ObjectDelta<? extends ObjectType>> getDeltas() {
        return deltas;
    }

    public void setDeltas(List<ObjectDelta<? extends ObjectType>> deltas) {
        this.deltas = deltas;
    }

    public void setDelta(ObjectDelta<? extends ObjectType> delta) {
        deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        deltas.add(delta);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(super.debugDump(indent));

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Deltas (count: " + deltas.size() + "):\n");
        for (ObjectDelta<? extends ObjectType> delta : deltas) {
            sb.append(delta.debugDump(indent+1));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    public PrimaryApprovalProcessWrapper getWrapper() {
        return wrapper;
    }

    public void setWrapper(PrimaryApprovalProcessWrapper wrapper) {
        this.wrapper = wrapper;
    }
}

package com.evolveum.midpoint.wf.processes;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

import java.util.ArrayList;
import java.util.List;

/**
 * StartProcessInstruction to be used in user approval processes.
 *
 * @author mederly
 */
public class UserChangeStartProcessInstruction extends StartProcessInstruction implements DebugDumpable {

    private List<ObjectDelta<UserType>> deltas = new ArrayList<ObjectDelta<UserType>>();

    public List<ObjectDelta<UserType>> getDeltas() {
        return deltas;
    }

    public void setDeltas(List<ObjectDelta<UserType>> deltas) {
        this.deltas = deltas;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(super.debugDump(indent));

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Deltas (count: " + deltas.size() + "):\n");
        for (ObjectDelta<UserType> delta : deltas) {
            sb.append(delta.debugDump(indent+1));
            sb.append("\n");
        }
        return sb.toString();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }
}

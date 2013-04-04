package com.evolveum.midpoint.wf.processes;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * StartProcessInstruction to be used in primary-stage approval processes.
 *
 * @author mederly
 */
public class StartProcessInstructionForPrimaryStage extends StartProcessInstruction implements DebugDumpable {

    private List<ObjectDelta<Objectable>> deltas = new ArrayList<ObjectDelta<Objectable>>();
    private PrimaryApprovalProcessWrapper wrapper;

    public List<ObjectDelta<Objectable>> getDeltas() {
        return deltas;
    }

    public void setDeltas(List<ObjectDelta<Objectable>> deltas) {
        this.deltas = deltas;
    }

    public void setDelta(ObjectDelta<Objectable> delta) {
        deltas = new ArrayList<ObjectDelta<Objectable>>();
        deltas.add(delta);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();

        sb.append(super.debugDump(indent));

        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Deltas (count: " + deltas.size() + "):\n");
        for (ObjectDelta<Objectable> delta : deltas) {
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

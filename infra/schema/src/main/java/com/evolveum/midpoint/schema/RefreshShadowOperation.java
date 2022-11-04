/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import java.util.Collection;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

public class RefreshShadowOperation implements DebugDumpable {

    private PrismObject<ShadowType> refreshedShadow;

    private Collection<ObjectDeltaOperation<ShadowType>> executedDeltas;
    private OperationResult refreshResult;

    public Collection<ObjectDeltaOperation<ShadowType>> getExecutedDeltas() {
        return executedDeltas;
    }

    public void setExecutedDeltas(Collection<ObjectDeltaOperation<ShadowType>> executedDeltas) {
        this.executedDeltas = executedDeltas;
    }

    public PrismObject<ShadowType> getRefreshedShadow() {
        return refreshedShadow;
    }

    public ShadowType getRefreshedShadowBean() {
        return asObjectable(refreshedShadow);
    }

    public void setRefreshedShadow(PrismObject<ShadowType> refreshedShadow) {
        this.refreshedShadow = refreshedShadow;
    }

    public OperationResult getRefreshResult() {
        return refreshResult;
    }

    public void setRefreshResult(OperationResult refreshResult) {
        this.refreshResult = refreshResult;
    }

    public Exception getCause(OperationResult result) {
       Throwable throwable = result.getCause();
       if (throwable == null) {
           return null;
       }
       if (Exception.class.isAssignableFrom(throwable.getClass())) {
           return (Exception) throwable;
       }

       return null;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((executedDeltas == null) ? 0 : executedDeltas.hashCode());
        result = prime * result + ((refreshedShadow == null) ? 0 : refreshedShadow.hashCode());
        return result;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        RefreshShadowOperation other = (RefreshShadowOperation) obj;
        if (executedDeltas == null) {
            if (other.executedDeltas != null) return false;
        } else if (!executedDeltas.equals(other.executedDeltas)) {
            return false;
        }
        if (refreshedShadow == null) {
            if (other.refreshedShadow != null) return false;
        } else if (!refreshedShadow.equals(other.refreshedShadow)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return getDebugDumpClassName() + "(" + executedDeltas
                + ": " + refreshedShadow + ")";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDump(sb, indent, true);
        return sb.toString();
    }

    public String shorterDebugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        debugDump(sb, indent, false);
        return sb.toString();
    }

    private void debugDump(StringBuilder sb, int indent, boolean detailedResultDump) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getDebugDumpClassName()).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "Delta", executedDeltas, indent + 1);
        sb.append("\n");
        if (detailedResultDump) {
            DebugUtil.debugDumpWithLabel(sb, "Refreshed shadow", refreshedShadow, indent + 1);
        } else {
            DebugUtil.debugDumpLabel(sb, "Refreshed shadow", indent + 1);
            if (refreshedShadow == null) {
                sb.append("null");
            } else {
                refreshedShadow.debugDump();
            }
        }
    }

    protected String getDebugDumpClassName() {
        return "RefreshShadowOperation";
    }


}


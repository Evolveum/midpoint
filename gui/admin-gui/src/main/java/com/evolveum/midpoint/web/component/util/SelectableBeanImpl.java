/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.util;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

/**
 * @author lazyman
 */
public class SelectableBeanImpl<T extends Serializable> extends Selectable<T> implements SelectableBean<T> {
    private static final long serialVersionUID = 1L;

    public static final String F_VALUE = "value";

    private static final Trace LOGGER = TraceManager.getTrace(SelectableBeanImpl.class);

    /**
     * Value of object that this bean represents. It may be null in case that non-success result is set.
     */
    private T value;

    //TODO probably this should not be here. find better place if needed, e.g. subclass with specific behaviour and attributes.
    private int activeSessions;
    private List<String> nodes;

    /**
     * Result of object retrieval (or attempt of object retrieval). It case that it is not error the result is optional.
     */
    private OperationResult result;

    public SelectableBeanImpl() {
    }

    public SelectableBeanImpl(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public OperationResult getResult() {
        return result;
    }

    public void setResult(OperationResult result) {
        this.result = result;
    }

    public void setResult(OperationResultType resultType) throws SchemaException {
        this.result = OperationResult.createOperationResult(resultType);
    }

    public void setActiveSessions(int activeSessions) {
        this.activeSessions = activeSessions;
    }

    public int getActiveSessions() {
        return activeSessions;
    }

    public List<String> getNodes() {
        return nodes;
    }

    public void setNodes(List<String> nodes) {
        this.nodes = nodes;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.result == null) ? 0 : this.result.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SelectableBeanImpl other = (SelectableBeanImpl) obj;
        if (result == null) {
            if (other.result != null) {
                return false;
            }
        } else if (!result.equals(other.result)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        // In case both values are objects then compare only OIDs.
        // that should be enough. Comparing complete objects may be slow
        // (e.g. if the objects have many assignments) and Wicket
        // invokes compare a lot ...
        } else if (!MiscSchemaUtil.quickEquals(value, other.value)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SelectableBean\n");
        DebugUtil.debugDumpWithLabelLn(sb, "value", value==null?null:value.toString(), indent+1);
        DebugUtil.debugDumpWithLabelLn(sb, "result", result==null?null:result.toString(), indent+1);
        return sb.toString();
    }

}

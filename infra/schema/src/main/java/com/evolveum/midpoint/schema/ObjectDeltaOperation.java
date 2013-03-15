/**
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * @author Radovan Semancik
 *
 */
public class ObjectDeltaOperation<T extends ObjectType> implements Dumpable, DebugDumpable {

	private ObjectDelta<T> objectDelta;
	private OperationResult executionResult;
	
	public ObjectDeltaOperation() {
		super();
	}
	
	public ObjectDeltaOperation(ObjectDelta<T> objectDelta) {
		super();
		this.objectDelta = objectDelta;
	}
	
	public ObjectDelta<T> getObjectDelta() {
		return objectDelta;
	}
	
	public void setObjectDelta(ObjectDelta<T> objectDelta) {
		this.objectDelta = objectDelta;
	}
	
	public OperationResult getExecutionResult() {
		return executionResult;
	}
	
	public void setExecutionResult(OperationResult executionResult) {
		this.executionResult = executionResult;
	}
	
	public boolean containsDelta(ObjectDelta<T> delta) {
		return objectDelta.equals(delta);
	}
	
	public static <T extends ObjectType> boolean containsDelta(Collection<? extends ObjectDeltaOperation<T>> deltaOps, ObjectDelta<T> delta) {
		if (deltaOps == null) {
			return false;
		}
		for (ObjectDeltaOperation<T> deltaOp: deltaOps) {
			if (deltaOp.containsDelta(delta)) {
				return true;
			}
		}
		return false;
	}
	
	public ObjectDeltaOperation<T> clone() {
		ObjectDeltaOperation<T> clone = new ObjectDeltaOperation<T>();
		if (this.objectDelta != null) {
			clone.objectDelta = this.objectDelta.clone();
		}
		clone.executionResult = this.executionResult;
		return clone;
	}
	
	public static void checkConsistence(Collection<? extends ObjectDeltaOperation<?>> deltas) {
		for (ObjectDeltaOperation<?> delta: deltas) {
			delta.checkConsistence();
		}
	}

	public void checkConsistence() {
		if (objectDelta != null) {
			objectDelta.checkConsistence();
		}
	}
	
	public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneCollection(
			Collection<ObjectDeltaOperation<? extends ObjectType>> origCollection) {
		Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>(origCollection.size());
		for (ObjectDeltaOperation<? extends ObjectType> origDeltaOp: origCollection) {
			ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = origDeltaOp.clone();
			clonedCollection.add(clonedDeltaOp);
		}
		return clonedCollection;
	}
	
	public static Collection<ObjectDeltaOperation<? extends ObjectType>> cloneDeltaCollection(
			Collection<ObjectDelta<? extends ObjectType>> origCollection) {
		Collection<ObjectDeltaOperation<? extends ObjectType>> clonedCollection = new ArrayList<ObjectDeltaOperation<? extends ObjectType>>(origCollection.size());
		for (ObjectDelta<? extends ObjectType> origDelta: origCollection) {
			ObjectDeltaOperation<? extends ObjectType> clonedDeltaOp = new ObjectDeltaOperation(origDelta.clone());
			clonedCollection.add(clonedDeltaOp);
		}
		return clonedCollection;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((executionResult == null) ? 0 : executionResult.hashCode());
		result = prime * result
				+ ((objectDelta == null) ? 0 : objectDelta.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ObjectDeltaOperation other = (ObjectDeltaOperation) obj;
		if (executionResult == null) {
			if (other.executionResult != null)
				return false;
		} else if (!executionResult.equals(other.executionResult))
			return false;
		if (objectDelta == null) {
			if (other.objectDelta != null)
				return false;
		} else if (!objectDelta.equals(other.objectDelta))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getDebugDumpClassName() + "(" + objectDelta
				+ ": " + executionResult + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(getDebugDumpClassName()).append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Delta", objectDelta, indent + 1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "Execution result", executionResult, indent + 1);
		return sb.toString();
	}

	@Override
	public String dump() {
		return debugDump();
	}
	
    protected String getDebugDumpClassName() {
        return "ObjectDeltaOperation";
    }
	
}

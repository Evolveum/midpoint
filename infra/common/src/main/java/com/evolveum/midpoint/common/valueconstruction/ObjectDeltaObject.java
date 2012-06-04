/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.valueconstruction;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;

/**
 * A DTO class defining old object state (before change), delta (change) and new object state (after change).
 * 
 * @author Radovan Semancik
 *
 */
public class ObjectDeltaObject<T extends ObjectType> implements Dumpable, DebugDumpable {
	
	private PrismObject<T> oldObject;
	private ObjectDelta<T> delta;
	private PrismObject<T> newObject;
	
	public ObjectDeltaObject(PrismObject<T> oldObject, ObjectDelta<T> delta, PrismObject<T> newObject) {
		super();
		this.oldObject = oldObject;
		this.delta = delta;
		this.newObject = newObject;
	}

	public PrismObject<T> getOldObject() {
		return oldObject;
	}
		
	public ObjectDelta<T> getDelta() {
		return delta;
	}
		
	public PrismObject<T> getNewObject() {
		return newObject;
	}
	
	public static <T extends ObjectType> ObjectDeltaObject<T> create(PrismObject<T> oldObject, ObjectDelta<T> delta) throws SchemaException {
		PrismObject<T> newObject = oldObject.clone();
		delta.applyTo(newObject);
		return new ObjectDeltaObject<T>(oldObject, delta, newObject);
	}
	
	public static <T extends ObjectType> ObjectDeltaObject<T> create(PrismObject<T> oldObject, ItemDelta<?>... itemDeltas) throws SchemaException {
		ObjectDelta<T> objectDelta = oldObject.createDelta(ChangeType.MODIFY);
		objectDelta.addModifications(itemDeltas);
		return create(oldObject, objectDelta);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((delta == null) ? 0 : delta.hashCode());
		result = prime * result + ((newObject == null) ? 0 : newObject.hashCode());
		result = prime * result + ((oldObject == null) ? 0 : oldObject.hashCode());
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
		ObjectDeltaObject other = (ObjectDeltaObject) obj;
		if (delta == null) {
			if (other.delta != null)
				return false;
		} else if (!delta.equals(other.delta))
			return false;
		if (newObject == null) {
			if (other.newObject != null)
				return false;
		} else if (!newObject.equals(other.newObject))
			return false;
		if (oldObject == null) {
			if (other.oldObject != null)
				return false;
		} else if (!oldObject.equals(other.oldObject))
			return false;
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
		sb.append("ObjectDeltaObject():");
		dumpObject(sb, oldObject, "old", indent +1);
		if (delta != null) {
			sb.append("\n");
			DebugUtil.indentDebugDump(sb, indent + 1);
			sb.append("delta:");
			if (delta == null) {
				sb.append(" null");
			} else {
				sb.append("\n");
				sb.append(delta.debugDump(indent + 2));
			}
		}
		dumpObject(sb, newObject, "new", indent +1);
		return sb.toString();
	}

	private void dumpObject(StringBuilder sb, PrismObject<T> object, String label, int indent) {
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(label).append(":");
		if (object == null) {
			sb.append(" null");
		} else {
			sb.append("\n");
			sb.append(object.debugDump(indent + 1));
		}
	}

	@Override
	public String dump() {
		return debugDump();
	}

	@Override
	public String toString() {
		return "ObjectDeltaObject(" + oldObject + " + " + delta + " = " + newObject + ")";
	}

}

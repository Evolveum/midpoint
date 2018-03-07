/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.Collection;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A DTO class defining old object state (before change), delta (change) and new object state (after change).
 *
 * @author Radovan Semancik
 *
 */
public class ObjectDeltaObject<O extends ObjectType> extends ItemDeltaItem<PrismContainerValue<O>,PrismObjectDefinition<O>> implements DebugDumpable {

	private PrismObject<O> oldObject;
	private ObjectDelta<O> delta;
	private PrismObject<O> newObject;

	public ObjectDeltaObject(PrismObject<O> oldObject, ObjectDelta<O> delta, PrismObject<O> newObject) {
		super();
		this.oldObject = oldObject;
		this.delta = delta;
		this.newObject = newObject;
	}

	public PrismObject<O> getOldObject() {
		return oldObject;
	}

	public ObjectDelta<O> getObjectDelta() {
		return delta;
	}

	public PrismObject<O> getNewObject() {
		return newObject;
	}

	// FIXME fragile!!! better don't use if you don't have to
	public void update(ItemDelta<?, ?> itemDelta) throws SchemaException {
		if (delta == null) {
			delta = ObjectDelta.createModifyDelta(getAnyObject().getOid(), itemDelta, getAnyObject().getCompileTimeClass(), getAnyObject().getPrismContext());
		} else {
			delta.swallow(itemDelta);
			itemDelta.applyTo(newObject);
		}
	}

	public PrismObject<O> getAnyObject() {
		if (newObject != null) {
			return newObject;
		}
		return oldObject;
	}

	@Override
	public ItemDelta<PrismContainerValue<O>,PrismObjectDefinition<O>> getDelta() {
		throw new UnsupportedOperationException("You probably wanted to call getObjectDelta()");
	}

	@Override
	public void setDelta(ItemDelta<PrismContainerValue<O>,PrismObjectDefinition<O>> delta) {
		throw new UnsupportedOperationException("You probably wanted to call setObjectDelta()");
	}

	@Override
	public boolean isContainer() {
		return true;
	}

	@Override
	public PrismObjectDefinition<O> getDefinition() {
		PrismObject<O> anyObject = getAnyObject();
		if (anyObject != null) {
			return anyObject.getDefinition();
		}
		if (delta != null) {
			return delta.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(delta.getObjectTypeClass());
		}
		return null;
	}

	@Override
	public <IV extends PrismValue,ID extends ItemDefinition> ItemDeltaItem<IV,ID> findIdi(ItemPath path) {
		Item<IV,ID> subItemOld = null;
		ItemPath subResidualPath = null;
		if (oldObject != null) {
			PartiallyResolvedItem<IV,ID> partialOld = oldObject.findPartial(path);
			if (partialOld != null) {
				subItemOld = partialOld.getItem();
				subResidualPath = partialOld.getResidualPath();
			}
		}
		Item<IV,ID> subItemNew = null;
		if (newObject != null) {
			PartiallyResolvedItem<IV,ID> partialNew = newObject.findPartial(path);
			if (partialNew != null) {
				subItemNew = partialNew.getItem();
				if (subResidualPath == null) {
					subResidualPath = partialNew.getResidualPath();
				}
			}
		}
		ItemDelta<IV,ID> itemDelta = null;
		Collection<? extends ItemDelta<?,?>> subSubItemDeltas = null;
		if (delta != null) {
			if (delta.getChangeType() == ChangeType.ADD) {
				PrismObject<O> objectToAdd = delta.getObjectToAdd();
	            PartiallyResolvedItem<IV,ID> partialValue = objectToAdd.findPartial(path);
	            if (partialValue != null && partialValue.getItem() != null) {
		            Item<IV,ID> item = partialValue.getItem();
		            itemDelta = item.createDelta();
		            itemDelta.addValuesToAdd(item.getClonedValues());
	            } else {
	            	// No item for this path, itemDelta will stay empty.
	            }
			} else if (delta.getChangeType() == ChangeType.DELETE) {
				if (subItemOld != null) {
					ItemPath subPath = subItemOld.getPath().remainder(path);
					PartiallyResolvedItem<IV,ID> partialValue = subItemOld.findPartial(subPath);
		            if (partialValue != null && partialValue.getItem() != null) {
			            Item<IV,ID> item = partialValue.getItem();
			            itemDelta = item.createDelta();
			            itemDelta.addValuesToDelete(item.getClonedValues());
		            } else {
		            	// No item for this path, itemDelta will stay empty.
		            }
				}
			} else if (delta.getChangeType() == ChangeType.MODIFY) {
				for (ItemDelta<?,?> modification: delta.getModifications()) {
	        		CompareResult compareComplex = modification.getPath().compareComplex(path);
	        		if (compareComplex == CompareResult.EQUIVALENT) {
	        			if (itemDelta != null) {
	        				throw new IllegalStateException("Conflicting modification in delta "+delta+": "+itemDelta+" and "+modification);
	        			}
	        			itemDelta = (ItemDelta<IV,ID>) modification;
	        		} else if (compareComplex == CompareResult.SUPERPATH) {
	        			if (subSubItemDeltas == null) {
	        				subSubItemDeltas = new ArrayList<>();
	        			}
	        			((Collection)subSubItemDeltas).add(modification);
	        		} else if (compareComplex == CompareResult.SUBPATH) {
	        			if (itemDelta != null) {
	        				throw new IllegalStateException("Conflicting modification in delta "+delta+": "+itemDelta+" and "+modification);
	        			}
	        			itemDelta = (ItemDelta<IV,ID>) modification.getSubDelta(path.substract(modification.getPath()));
	        		}
	        	}
			}
		}
		ItemDeltaItem<IV,ID> subIdi = new ItemDeltaItem<>(subItemOld, itemDelta, subItemNew);
		subIdi.setSubItemDeltas(subSubItemDeltas);
		subIdi.setResolvePath(path);
		subIdi.setResidualPath(subResidualPath);
		return subIdi;
	}

	public void recompute() throws SchemaException {
		if (delta == null) {
			newObject = oldObject.clone();
			return;
		}
		if (delta.isAdd()) {
			newObject = delta.getObjectToAdd();
			return;
		}
		if (delta.isDelete()) {
			newObject = null;
		}
		if (oldObject == null) {
			return;
		}
		newObject = oldObject.clone();
		delta.applyTo(newObject);
	}

	public static <T extends ObjectType> ObjectDeltaObject<T> create(PrismObject<T> oldObject, ObjectDelta<T> delta) throws SchemaException {
		PrismObject<T> newObject = oldObject.clone();
		delta.applyTo(newObject);
		return new ObjectDeltaObject<>(oldObject, delta, newObject);
	}

	public static <T extends ObjectType> ObjectDeltaObject<T> create(PrismObject<T> oldObject, ItemDelta<?,?>... itemDeltas) throws SchemaException {
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

	private void dumpObject(StringBuilder sb, PrismObject<O> object, String label, int indent) {
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
	public String toString() {
		return "ObjectDeltaObject(" + oldObject + " + " + delta + " = " + newObject + ")";
	}

	public ObjectDeltaObject<O> clone() {
		ObjectDeltaObject<O> clone = new ObjectDeltaObject<>(
				CloneUtil.clone(oldObject),
				CloneUtil.clone(delta),
				CloneUtil.clone(newObject));
		// TODO what about the internals?
		return clone;
	}

}

/*
 * Copyright (c) 2010-2014 Evolveum
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
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.SimpleVisitable;
import com.evolveum.midpoint.prism.SimpleVisitor;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Radovan Semancik
 */
public class DeltaMapTriple<K,V> implements DebugDumpable, Serializable, SimpleVisitable<Map.Entry<K, V>> {

    /**
     * Collection of values that were not changed.
     */
    protected Map<K,V> zeroMap;

    /**
     * Collection of values that were added.
     */
    protected Map<K,V> plusMap;

    /**
     * Collection of values that were deleted.
     */
    protected Map<K,V> minusMap;

    public DeltaMapTriple() {
        zeroMap = createMap();
        plusMap = createMap();
        minusMap = createMap();
    }

    public DeltaMapTriple(Map<K,V> zeroMap, Map<K,V> plusMap, Map<K,V> minusMap) {
        this.zeroMap = zeroMap;
        this.plusMap = plusMap;
        this.minusMap = minusMap;
    }

    protected Map<K,V> createMap() {
        return new HashMap<>();
    }

    public Map<K,V> getZeroMap() {
        return zeroMap;
    }

    public Map<K,V> getPlusMap() {
        return plusMap;
    }

    public Map<K,V> getMinusMap() {
        return minusMap;
    }

    public Map<K,V> getMap(PlusMinusZero plusMinusZero) {
    	if (plusMinusZero == null) {
    		return null;
    	}
    	switch (plusMinusZero) {
    		case PLUS: return plusMap;
    		case MINUS: return minusMap;
    		case ZERO: return zeroMap;
    	}
    	// notreached
    	throw new IllegalStateException();
    }

    public boolean hasPlusMap() {
    	return (plusMap != null && !plusMap.isEmpty());
    }

    public boolean hasZeroMap() {
    	return (zeroMap != null && !zeroMap.isEmpty());
    }

    public boolean hasMinusMap() {
    	return (minusMap != null && !minusMap.isEmpty());
    }

	public boolean isZeroOnly() {
		return hasZeroMap() && !hasPlusMap() && !hasMinusMap();
	}

    public void addToPlusMap(K key, V value) {
    	addToMap(plusMap, key, value);
    }

    public void addToMinusMap(K key, V value) {
    	addToMap(minusMap, key, value);
    }

    public void addToZeroMap(K key, V value) {
    	addToMap(zeroMap, key, value);
    }

    public void addAllToPlusMap(Map<K,V> map) {
    	addAllToMap(plusMap, map);
    }

	public void addAllToMinusMap(Map<K,V> map) {
		addAllToMap(minusMap, map);
    }

    public void addAllToZeroMap(Map<K,V> map) {
    	addAllToMap(zeroMap, map);

    }

    public void addAllToMap(PlusMinusZero destination, Map<K,V> map) {
    	if (destination == null) {
    		return;
    	} else if (destination == PlusMinusZero.PLUS) {
    		addAllToMap(plusMap, map);
    	} else if (destination == PlusMinusZero.MINUS) {
    		addAllToMap(minusMap, map);
    	} else if (destination == PlusMinusZero.ZERO) {
    		addAllToMap(zeroMap, map);
    	}
    }

	private void addAllToMap(Map<K,V> set, Map<K,V> items) {
		if (items == null) {
			return;
		}
		for (Entry<K, V> item: items.entrySet()) {
			addToMap(set, item.getKey(), item.getValue());
		}
	}

	private void addToMap(Map<K,V> set, K key, V value) {
		if (set == null) {
			set = createMap();
    	}
		set.put(key, value);
	}

	public void clearPlusMap() {
		clearMap(plusMap);
	}

	public void clearMinusMap() {
		clearMap(minusMap);
	}

	public void clearZeroMap() {
		clearMap(zeroMap);
	}

	private void clearMap(Map<K,V> set) {
		if (set != null) {
			set.clear();
		}
	}

	public int size() {
		return sizeMap(zeroMap) + sizeMap(plusMap) + sizeMap(minusMap);
	}

	private int sizeMap(Map<K,V> set) {
		if (set == null) {
			return 0;
		}
		return set.size();
	}

	public void merge(DeltaMapTriple<K,V> triple) {
		addAllToZeroMap(triple.zeroMap);
		addAllToPlusMap(triple.plusMap);
		addAllToMinusMap(triple.minusMap);
	}

	/**
     * Returns all values, regardless of the internal sets.
     */
    public Collection<K> unionKeySets() {
        return MiscUtil.union(zeroMap.keySet(), plusMap.keySet(), minusMap.keySet());
    }

	public DeltaMapTriple<K,V> clone(Cloner<Entry<K, V>> cloner) {
		DeltaMapTriple<K,V> clone = new DeltaMapTriple<K,V>();
		copyValues(clone, cloner);
		return clone;
	}

	protected void copyValues(DeltaMapTriple<K,V> clone, Cloner<Entry<K, V>> cloner) {
		clone.zeroMap = cloneSet(this.zeroMap, cloner);
		clone.plusMap = cloneSet(this.plusMap, cloner);
		clone.minusMap = cloneSet(this.minusMap, cloner);
	}

	private Map<K,V> cloneSet(Map<K,V> origSet, Cloner<Entry<K, V>> cloner) {
		if (origSet == null) {
			return null;
		}
		Map<K,V> clonedSet = createMap();
		for (Entry<K, V> origVal: origSet.entrySet()) {
			Entry<K, V> clonedVal = cloner.clone(origVal);
			clonedSet.put(clonedVal.getKey(), clonedVal.getValue());
		}
		return clonedSet;
	}

	public boolean isEmpty() {
		return isEmpty(minusMap) && isEmpty(plusMap) && isEmpty(zeroMap);
	}

	private boolean isEmpty(Map<K,V> set) {
		if (set == null) {
			return true;
		}
		return set.isEmpty();
	}

	@Override
	public void simpleAccept(SimpleVisitor<Entry<K, V>> visitor) {
		acceptMap(visitor, zeroMap);
		acceptMap(visitor, plusMap);
		acceptMap(visitor, minusMap);
	}

	private void acceptMap(SimpleVisitor<Entry<K, V>> visitor, Map<K,V> set) {
		if (set == null) {
			return;
		}
		for (Entry<K, V> element: set.entrySet()) {
			visitor.visit(element);
		}
	}

	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(debugName()).append("(");
        dumpMap(sb, "zero", zeroMap);
        dumpMap(sb, "plus", plusMap);
        dumpMap(sb, "minus", minusMap);
        sb.append(")");
        return sb.toString();
    }

    protected String debugName() {
    	return "DeltaMapTriple";
    }

    private void dumpMap(StringBuilder sb, String label, Map<K,V> set) {
        sb.append(label).append(": ").append(set).append("; ");
    }

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump()
	 */
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.util.DebugDumpable#debugDump(int)
	 */
	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
        sb.append("DeltaSetTriple:\n");
        debugDumpMap(sb, "zero", zeroMap, indent + 1);
        sb.append("\n");
        debugDumpMap(sb, "plus", plusMap, indent + 1);
        sb.append("\n");
        debugDumpMap(sb, "minus", minusMap, indent + 1);
        return sb.toString();
	}

	private void debugDumpMap(StringBuilder sb, String label, Map<K,V> set, int indent) {
		DebugUtil.debugDumpLabel(sb, label, indent);
		sb.append("\n");
		DebugUtil.debugDumpMapMultiLine(sb, set, indent + 1);
	}

}

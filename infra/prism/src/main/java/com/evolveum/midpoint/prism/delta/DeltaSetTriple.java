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
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.Transformer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * The triple of values (added, unchanged, deleted) that represents difference between two collections of values.
 * <p/>
 * The DeltaSetTriple is used as a result of a "diff" operation or it is constructed to determine a ObjectDelta or
 * PropertyDelta. It is a very useful structure in numerous situations when dealing with relative changes.
 * <p/>
 * DeltaSetTriple (similarly to other parts of this system) deal only with unordered values.
 *
 * @author Radovan Semancik
 */
public class DeltaSetTriple<T> implements DebugDumpable, Serializable, SimpleVisitable<T> {

    /**
     * Collection of values that were not changed.
     */
    protected Collection<T> zeroSet;

    /**
     * Collection of values that were added.
     */
    protected Collection<T> plusSet;

    /**
     * Collection of values that were deleted.
     */
    protected Collection<T> minusSet;

    public DeltaSetTriple() {
        zeroSet = createSet();
        plusSet = createSet();
        minusSet = createSet();
    }

    public DeltaSetTriple(Collection<T> zeroSet, Collection<T> plusSet, Collection<T> minusSet) {
        this.zeroSet = zeroSet;
        this.plusSet = plusSet;
        this.minusSet = minusSet;
    }

    /**
     * Compares two (unordered) collections and creates a triple describing the differences.
     */
    public static <T> DeltaSetTriple<T> diff(Collection<T> valuesOld, Collection<T> valuesNew) {
        DeltaSetTriple<T> triple = new DeltaSetTriple<T>();
        diff(valuesOld, valuesNew, triple);
        return triple;
    }
    
    protected static <T> void diff(Collection<T> valuesOld, Collection<T> valuesNew, DeltaSetTriple<T> triple) {
        if (valuesOld == null && valuesNew == null) {
        	// No values, no change -> empty triple
        	return;
        }
        if (valuesOld == null) {
        	triple.getPlusSet().addAll(valuesNew);
        	return;
        }
        if (valuesNew == null) {
        	triple.getMinusSet().addAll(valuesOld);
        	return;
        }
        for (T val : valuesOld) {
            if (valuesNew.contains(val)) {
                triple.getZeroSet().add(val);
            } else {
                triple.getMinusSet().add(val);
            }
        }
        for (T val : valuesNew) {
            if (!valuesOld.contains(val)) {
                triple.getPlusSet().add(val);
            }
        }
    }

    protected Collection<T> createSet() {
        return new ArrayList<T>();
    }

    public Collection<T> getZeroSet() {
        return zeroSet;
    }

    public Collection<T> getPlusSet() {
        return plusSet;
    }

    public Collection<T> getMinusSet() {
        return minusSet;
    }
    
    public boolean hasPlusSet() {
    	return (plusSet != null && !plusSet.isEmpty());
    }

    public boolean hasZeroSet() {
    	return (zeroSet != null && !zeroSet.isEmpty());
    }

    public boolean hasMinusSet() {
    	return (minusSet != null && !minusSet.isEmpty());
    }
    
	public boolean isZeroOnly() {
		return hasZeroSet() && !hasPlusSet() && !hasMinusSet();
	}

    public void addToPlusSet(T item) {
    	addToSet(plusSet, item);
    }

    public void addToMinusSet(T item) {
    	addToSet(minusSet, item);
    }

    public void addToZeroSet(T item) {
    	addToSet(zeroSet, item);
    }

    public void addAllToPlusSet(Collection<T> items) {
    	addAllToSet(plusSet, items);
    }

	public void addAllToMinusSet(Collection<T> items) {
    	addAllToSet(minusSet, items);
    }

    public void addAllToZeroSet(Collection<T> items) {
    	addAllToSet(zeroSet, items);

    }

    public void addAllToSet(PlusMinusZero destination, Collection<T> items) {
    	if (destination == null) {
    		return;
    	} else if (destination == PlusMinusZero.PLUS) {
    		addAllToSet(plusSet, items);
    	} else if (destination == PlusMinusZero.MINUS) {
    		addAllToSet(minusSet, items);
    	} else if (destination == PlusMinusZero.ZERO) {
    		addAllToSet(zeroSet, items);
    	}
    }
    
	private void addAllToSet(Collection<T> set, Collection<T> items) {
		if (items == null) {
			return;
		}
		for (T item: items) {
			addToSet(set, item);
		}
	}

	private void addToSet(Collection<T> set, T item) {
		if (set == null) {
			set = createSet();
    	}
		if (!set.contains(item)) {
			set.add(item);
		}
	}

    public boolean presentInPlusSet(T item) {
    	return presentInSet(plusSet, item);
    }

    public boolean presentInMinusSet(T item) {
    	return presentInSet(minusSet, item);
    }

    public boolean presentInZeroSet(T item) {
    	return presentInSet(zeroSet, item);
    }

	private boolean presentInSet(Collection<T> set, T item) {
		if (set == null) {
			return false;
		}
		return set.contains(item);
	}
	
	public void clearPlusSet() {
		clearSet(plusSet);
	}
	
	public void clearMinusSet() {
		clearSet(minusSet);
	}
	
	public void clearZeroSet() {
		clearSet(zeroSet);
	}

	private void clearSet(Collection<T> set) {
		if (set != null) {
			set.clear();
		}
	}
	
	public int size() {
		return sizeSet(zeroSet) + sizeSet(plusSet) + sizeSet(minusSet);
	}

	private int sizeSet(Collection<T> set) {
		if (set == null) {
			return 0;
		}
		return set.size();
	}

	/**
     * Returns all values, regardless of the internal sets.
     */
    public Collection<T> union() {
        return MiscUtil.union(zeroSet, plusSet, minusSet);
    }
    
    public Collection<T> getAllValues() {
    	Collection<T> allValues = new ArrayList<T>(size());
    	addAllValuesSet(allValues, zeroSet);
    	addAllValuesSet(allValues, plusSet);
    	addAllValuesSet(allValues, minusSet);
    	return allValues;
    }

	private void addAllValuesSet(Collection<T> allValues, Collection<T> set) {
		if (set == null) {
			return;
		}
		allValues.addAll(set);
	}

	public Collection<T> getNonNegativeValues() {
        return MiscUtil.union(zeroSet, plusSet);
    }
    
    public Collection<T> getNonPositiveValues() {
        return MiscUtil.union(zeroSet, minusSet);
    }
    
	public void merge(DeltaSetTriple<T> triple) {
		zeroSet.addAll(triple.zeroSet);
		plusSet.addAll(triple.plusSet);
		minusSet.addAll(triple.minusSet);
	}
	
	public DeltaSetTriple<T> clone(Cloner<T> cloner) {
		DeltaSetTriple<T> clone = new DeltaSetTriple<T>();
		copyValues(clone, cloner);
		return clone;
	}

	protected void copyValues(DeltaSetTriple<T> clone, Cloner<T> cloner) {
		clone.zeroSet = cloneSet(this.zeroSet, cloner);
		clone.plusSet = cloneSet(this.plusSet, cloner);
		clone.minusSet = cloneSet(this.minusSet, cloner);		
	}

	private Collection<T> cloneSet(Collection<T> origSet, Cloner<T> cloner) {
		if (origSet == null) {
			return null;
		}
		Collection<T> clonedSet = createSet();
		for (T origVal: origSet) {
			clonedSet.add(cloner.clone(origVal));
		}
		return clonedSet;
	}
	
	public boolean isEmpty() {
		return isEmpty(minusSet) && isEmpty(plusSet) && isEmpty(zeroSet);
	}
	
	private boolean isEmpty(Collection<T> set) {
		if (set == null) {
			return true;
		}
		return set.isEmpty();
	}
	
	@Override
	public void accept(SimpleVisitor<T> visitor) {
		acceptSet(visitor, zeroSet);
		acceptSet(visitor, plusSet);
		acceptSet(visitor, minusSet);
	}

	private void acceptSet(SimpleVisitor<T> visitor, Collection<T> set) {
		if (set == null) {
			return;
		}
		for (T element: set) {
			visitor.visit(element);
		}
	}
	
	public <X> void transform(DeltaSetTriple<X> transformTarget, Transformer<T,X> transformer) {
		for (T orig: getZeroSet()) {
			X transformed = transformer.transform(orig);
			transformTarget.addToZeroSet(transformed);
		}
		for (T orig: getPlusSet()) {
			X transformed = transformer.transform(orig);
			transformTarget.addToPlusSet(transformed);
		}
		for (T orig: getMinusSet()) {
			X transformed = transformer.transform(orig);
			transformTarget.addToMinusSet(transformed);
		}
	}

	@Override
    public String toString() {
		StringBuilder sb = new StringBuilder();
        sb.append(debugName()).append("(");
        dumpSet(sb, "zero", zeroSet);
        dumpSet(sb, "plus", plusSet);
        dumpSet(sb, "minus", minusSet);
        sb.append(")");
        return sb.toString();
    }
    
    protected String debugName() {
    	return "DeltaSetTriple";
    }

    private void dumpSet(StringBuilder sb, String label, Collection<T> set) {
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
        debugDumpSet(sb, "zero", zeroSet, indent + 1);
        sb.append("\n");
        debugDumpSet(sb, "plus", plusSet, indent + 1);
        sb.append("\n");
        debugDumpSet(sb, "minus", minusSet, indent + 1);
        return sb.toString();
	}

	private void debugDumpSet(StringBuilder sb, String label, Collection<T> set, int indent) {
		DebugUtil.indentDebugDump(sb, indent);
		sb.append(label).append(":");
		if (set == null) {
			sb.append(" null");
		} else {
			for (T val: set) {
				sb.append("\n");
				sb.append(DebugUtil.debugDump(val, indent +1));
			}
		}
	}
	
	public String toHumanReadableString() {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		first = toHumanReadableString(sb, "added", plusSet, first);
		first = toHumanReadableString(sb, "removed", minusSet, first);
		first = toHumanReadableString(sb, "unchanged", zeroSet, first);
		return sb.toString();
	}

	private boolean toHumanReadableString(StringBuilder sb, String label, Collection<T> set, boolean first) {
		if (set.isEmpty()) {
			return first;
		}
		if (!first) {
			sb.append("; ");
		}
		sb.append(label).append(": ");
		Iterator<T> iterator = set.iterator();
		while (iterator.hasNext()) {
			T item = iterator.next();
			toHumanReadableString(sb, item);
			if (iterator.hasNext()) {
				sb.append(", ");
			}
		}
		return false;
	}

	protected void toHumanReadableString(StringBuilder sb, T item) {
		sb.append(item);
	}


}

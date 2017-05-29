/*
 * Copyright (c) 2010-2017 Evolveum
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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Foreachable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.Processor;
import com.evolveum.midpoint.util.Transformer;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
public class DeltaSetTriple<T> implements DebugDumpable, Serializable, SimpleVisitable<T>, Foreachable<T> {

    /**
     * Collection of values that were not changed.
     */
    @NotNull
	final Collection<T> zeroSet;

    /**
     * Collection of values that were added.
     */
	@NotNull
	final Collection<T> plusSet;

    /**
     * Collection of values that were deleted.
     */
	@NotNull
	final Collection<T> minusSet;

    public DeltaSetTriple() {
        zeroSet = createSet();
        plusSet = createSet();
        minusSet = createSet();
    }

    public DeltaSetTriple(@NotNull Collection<T> zeroSet, @NotNull Collection<T> plusSet, @NotNull Collection<T> minusSet) {
        this.zeroSet = zeroSet;
        this.plusSet = plusSet;
        this.minusSet = minusSet;
    }

    /**
     * Compares two (unordered) collections and creates a triple describing the differences.
     */
    public static <T> DeltaSetTriple<T> diff(Collection<T> valuesOld, Collection<T> valuesNew) {
        DeltaSetTriple<T> triple = new DeltaSetTriple<>();
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

    private Collection<T> createSet() {
        return new ArrayList<>();
    }

    @NotNull
	public Collection<T> getZeroSet() {
        return zeroSet;
    }

    @NotNull
	public Collection<T> getPlusSet() {
        return plusSet;
    }

    @NotNull
	public Collection<T> getMinusSet() {
        return minusSet;
    }
    
    public boolean hasPlusSet() {
    	return !plusSet.isEmpty();
    }

    public boolean hasZeroSet() {
    	return !zeroSet.isEmpty();
    }

    public boolean hasMinusSet() {
    	return !minusSet.isEmpty();
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

    public Collection<T> getSet(PlusMinusZero whichSet) {
        switch (whichSet) {
            case ZERO: return getZeroSet();
            case PLUS: return getPlusSet();
            case MINUS: return getMinusSet();
            default: throw new IllegalArgumentException("Unexpected value: " + whichSet);
        }
    }

    public void addAllToSet(PlusMinusZero destination, Collection<T> items) {
    	if (destination == null) {
    		// no op
    	} else if (destination == PlusMinusZero.PLUS) {
    		addAllToSet(plusSet, items);
    	} else if (destination == PlusMinusZero.MINUS) {
    		addAllToSet(minusSet, items);
    	} else if (destination == PlusMinusZero.ZERO) {
    		addAllToSet(zeroSet, items);
    	}
    }
    
    public void addToSet(PlusMinusZero destination, T item) {
    	if (destination == null) {
			// no op
    	} else if (destination == PlusMinusZero.PLUS) {
    		addToSet(plusSet, item);
    	} else if (destination == PlusMinusZero.MINUS) {
    		addToSet(minusSet, item);
    	} else if (destination == PlusMinusZero.ZERO) {
    		addToSet(zeroSet, item);
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
		return set != null && set.contains(item);
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
    @SuppressWarnings("unchecked")
	public Collection<T> union() {
        return MiscUtil.union(zeroSet, plusSet, minusSet);
    }
    
    public T getAnyValue() {
    	if (!zeroSet.isEmpty()) {
    		return zeroSet.iterator().next();
    	}
    	if (!plusSet.isEmpty()) {
    		return plusSet.iterator().next();
    	}
    	if (!minusSet.isEmpty()) {
    		return minusSet.iterator().next();
    	}
    	return null;
    }
    
    public Collection<T> getAllValues() {
    	Collection<T> allValues = new ArrayList<>(size());
    	addAllValuesSet(allValues, zeroSet);
    	addAllValuesSet(allValues, plusSet);
    	addAllValuesSet(allValues, minusSet);
    	return allValues;
    }

    public Stream<T> stream() {
    	// concatenates the streams
    	return Stream.of(zeroSet.stream(), plusSet.stream(), minusSet.stream()).flatMap(Function.identity());
	}

	private void addAllValuesSet(Collection<T> allValues, Collection<T> set) {
		if (set == null) {
			return;
		}
		allValues.addAll(set);
	}

	@SuppressWarnings("unchecked")
	@NotNull
	public Collection<T> getNonNegativeValues() {
        return MiscUtil.union(zeroSet, plusSet);
    }

	@SuppressWarnings("unchecked")
	@NotNull
    public Collection<T> getNonPositiveValues() {
        return MiscUtil.union(zeroSet, minusSet);
    }
    
	public void merge(DeltaSetTriple<T> triple) {
		zeroSet.addAll(triple.zeroSet);
		plusSet.addAll(triple.plusSet);
		minusSet.addAll(triple.minusSet);
	}
	
	public DeltaSetTriple<T> clone(Cloner<T> cloner) {
		DeltaSetTriple<T> clone = new DeltaSetTriple<>();
		copyValues(clone, cloner);
		return clone;
	}

	protected void copyValues(DeltaSetTriple<T> clone, Cloner<T> cloner) {
    	clone.zeroSet.clear();
		clone.zeroSet.addAll(cloneSet(this.zeroSet, cloner));
		clone.plusSet.clear();
		clone.plusSet.addAll(cloneSet(this.plusSet, cloner));
		clone.minusSet.clear();
		clone.minusSet.addAll(cloneSet(this.minusSet, cloner));
	}

	@NotNull
	private Collection<T> cloneSet(@NotNull Collection<T> origSet, Cloner<T> cloner) {
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
		return set == null || set.isEmpty();
	}
	
	/**
	 * Process each element of every set.
	 * This is different from the visitor. Visitor will go
	 * deep inside, foreach will remain on the surface.
	 */
	@Override
	public void foreach(Processor<T> processor) {
		foreachSet(processor, zeroSet);
		foreachSet(processor, plusSet);
		foreachSet(processor, minusSet);
	}
	
	private void foreachSet(Processor<T> processor, Collection<T> set) {
		if (set == null) {
			return;
		}
		for (T element: set) {
			processor.process(element);
		}
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
		DebugUtil.debugDumpLabelLn(sb, "DeltaSetTriple", indent);
		
        debugDumpSets(sb, 
        		val -> sb.append(DebugUtil.debugDump(val, indent + 3)),
        		indent + 1);

        return sb.toString();
	}

	public void debugDumpSets(StringBuilder sb, Consumer<T> dumper, int indent) {
        debugDumpSet(sb, "zero", dumper, zeroSet, indent + 1);
        sb.append("\n");
        debugDumpSet(sb, "plus", dumper, plusSet, indent + 1);
        sb.append("\n");
        debugDumpSet(sb, "minus", dumper, minusSet, indent + 1);		
	}
	
	private void debugDumpSet(StringBuilder sb, String label, Consumer<T> dumper, Collection<T> set, int indent) {
		DebugUtil.debugDumpLabel(sb, label, indent);
		if (set == null) {
			sb.append(" null");
		} else {
			for (T val: set) {
				sb.append("\n");
				dumper.accept(val);
			}
		}
	}
	
	public String toHumanReadableString() {
		StringBuilder sb = new StringBuilder();
		boolean first = toHumanReadableString(sb, "added", plusSet, true);
		first = toHumanReadableString(sb, "removed", minusSet, first);
		toHumanReadableString(sb, "unchanged", zeroSet, first);
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

	public static <T> DeltaSetTriple<? extends T> find(Map<ItemPath, DeltaSetTriple<? extends T>> tripleMap, ItemPath path) {
		List<Map.Entry<ItemPath, DeltaSetTriple<? extends T>>> matching = tripleMap.entrySet().stream()
				.filter(e -> path.equivalent(e.getKey()))
				.collect(Collectors.toList());
		if (matching.isEmpty()) {
			return null;
		} else if (matching.size() == 1) {
			return matching.get(0).getValue();
		} else {
			throw new IllegalStateException("Multiple matching entries for key '" + path + "' in " + tripleMap);
		}
	}
}

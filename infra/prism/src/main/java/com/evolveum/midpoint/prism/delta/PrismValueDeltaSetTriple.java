/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.MiscUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * DeltaSetTriple that is limited to hold prism values. By limiting to the PrismValue descendants we gain advantage to be
 * clonnable and ability to compare real values. 
 *
 * @author Radovan Semancik
 */
public class PrismValueDeltaSetTriple<V extends PrismValue> extends DeltaSetTriple<V> 
				implements Dumpable, DebugDumpable, Visitable {

    public PrismValueDeltaSetTriple() {
    	super();
    }

    public PrismValueDeltaSetTriple(Collection<V> zeroSet, Collection<V> plusSet, Collection<V> minusSet) {
    	super(zeroSet, plusSet, minusSet);
    }
    
    /**
     * Compares two (unordered) collections and creates a triple describing the differences.
     */
    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> diff(Collection<V> valuesOld, Collection<V> valuesNew) {
    	PrismValueDeltaSetTriple<V> triple = new PrismValueDeltaSetTriple<V>();
        diff(valuesOld, valuesNew, triple);
        return triple;
    }

    /**
     * Distributes a value in this triple similar to the placement of other value in the other triple.
     * E.g. if the value "otherMember" is in the zero set in "otherTriple" then "myMember" will be placed
     * in zero set in this triple.
     */
    public <O extends PrismValue> void distributeAs(V myMember, PrismValueDeltaSetTriple<O> otherTriple, O otherMember) {
        if (otherTriple.getZeroSet() != null && PrismValue.containsRealValue(otherTriple.getZeroSet(), otherMember)) {
            zeroSet.add(myMember);
        }
        if (otherTriple.getPlusSet() != null && PrismValue.containsRealValue(otherTriple.getPlusSet(), otherMember)) {
            plusSet.add(myMember);
        }
        if (otherTriple.getMinusSet() != null && PrismValue.containsRealValue(otherTriple.getMinusSet(), otherMember)) {
            minusSet.add(myMember);
        }
    }
    
	public Class<V> getValueClass() {
		V anyValue = getAnyValue();
		if (anyValue == null) {
			return null;
		}
		return (Class<V>) anyValue.getClass();
	}
	
	public Class<?> getRealValueClass() {
		V anyValue = getAnyValue();
		if (anyValue == null) {
			return null;
		}
		if (anyValue instanceof PrismPropertyValue<?>) {
			PrismPropertyValue<?> pval = (PrismPropertyValue<?>)anyValue;
			Object realValue = pval.getValue();
			if (realValue == null) {
				return null;
			}
			return realValue.getClass();
		} else {
			return null;
		}
	}

	private V getAnyValue() {
		if (zeroSet != null && !zeroSet.isEmpty()) {
			return zeroSet.iterator().next();
		}
		if (plusSet != null && !plusSet.isEmpty()) {
			return plusSet.iterator().next();
		}
		if (minusSet != null && !minusSet.isEmpty()) {
			return minusSet.iterator().next();
		}
		return null;
	}

	public PrismValueDeltaSetTriple<V> clone() {
		PrismValueDeltaSetTriple<V> clone = new PrismValueDeltaSetTriple<V>();
		clone.zeroSet = cloneSet(this.zeroSet);
		clone.plusSet = cloneSet(this.plusSet);
		clone.minusSet = cloneSet(this.minusSet);
		return clone;
	}

	private Collection<V> cloneSet(Collection<V> origSet) {
		if (origSet == null) {
			return null;
		}
		Collection<V> clonedSet = createSet();
		for (V origVal: origSet) {
			clonedSet.add((V) origVal.clone());
		}
		return clonedSet;
	}
	
	@Override
	public void accept(Visitor visitor) {
		acceptSet(zeroSet, visitor);
		acceptSet(plusSet, visitor);
		acceptSet(minusSet, visitor);
	}

	private void acceptSet(Collection<V> set, Visitor visitor) {
		if (set == null) {
			return;
		}
		for (V val: set) {
			val.accept(visitor);
		}
	}
	
	protected String debugName() {
    	return "PVDeltaSetTriple";
    }

}

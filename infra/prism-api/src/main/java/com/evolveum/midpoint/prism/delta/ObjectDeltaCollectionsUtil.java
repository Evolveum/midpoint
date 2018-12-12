/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.*;

/**
 *
 */
public class ObjectDeltaCollectionsUtil {
	/**
	 * Returns a delta that is a "sum" of all the deltas in the collection.
	 * The deltas as processed as an ORDERED sequence. Therefore it correctly processes item overwrites and so on.
	 * It also means that if there is an ADD delta it has to be first.
	 */
	@SafeVarargs
	public static <T extends Objectable> ObjectDelta<T> summarize(ObjectDelta<T>... deltas) throws SchemaException {
		return summarize(Arrays.asList(deltas));
	}

	/**
	 * Returns a delta that is a "sum" of all the deltas in the collection.
	 * The deltas as processed as an ORDERED sequence. Therefore it correctly processes item overwrites and so on.
	 * It also means that if there is an ADD delta it has to be first.
	 */
	public static <T extends Objectable> ObjectDelta<T> summarize(List<ObjectDelta<T>> deltas) throws SchemaException {
		if (deltas == null || deltas.isEmpty()) {
			return null;
		}
		Iterator<ObjectDelta<T>> iterator = deltas.iterator();
		ObjectDelta<T> sumDelta = iterator.next().clone();
		while (iterator.hasNext()) {
			ObjectDelta<T> nextDelta = iterator.next();
			sumDelta.merge(nextDelta);
		}
		return sumDelta;
	}

	/**
	 * Union of several object deltas. The deltas are merged to create a single delta
	 * that contains changes from all the deltas.
	 *
	 * Union works on UNORDERED deltas.
	 */
	public static <T extends Objectable> ObjectDelta<T> union(ObjectDelta<T>... deltas) throws SchemaException {
	    List<ObjectDelta<T>> modifyDeltas = new ArrayList<>(deltas.length);
	    ObjectDelta<T> addDelta = null;
	    ObjectDelta<T> deleteDelta = null;
	    for (ObjectDelta<T> delta : deltas) {
	        if (delta == null) {
	            continue;
	        }
	        if (delta.getChangeType() == ChangeType.MODIFY) {
	            modifyDeltas.add(delta);
	        } else if (delta.getChangeType() == ChangeType.ADD) {
	            if (addDelta != null) {
	                // Maybe we can, be we do not want. This is usually an error anyway.
	                throw new IllegalArgumentException("Cannot merge two add deltas: " + addDelta + ", " + delta);
	            }
	            addDelta = delta;
	        } else if (delta.getChangeType() == ChangeType.DELETE) {
	            deleteDelta = delta;
	        }

	    }

	    if (deleteDelta != null && addDelta == null) {
	        // Merging DELETE with anything except ADD is still a DELETE
	        return deleteDelta.clone();
	    }

	    if (deleteDelta != null && addDelta != null) {
	        throw new IllegalArgumentException("Cannot merge add and delete deltas: " + addDelta + ", " + deleteDelta);
	    }

	    if (addDelta != null) {
	        return mergeToDelta(addDelta, modifyDeltas);
	    } else {
	        if (modifyDeltas.size() == 0) {
	            return null;
	        }
	        if (modifyDeltas.size() == 1) {
	            return modifyDeltas.get(0);
	        }
	        return mergeToDelta(modifyDeltas.get(0), modifyDeltas.subList(1, modifyDeltas.size()));
	    }
	}

	private static <T extends Objectable> ObjectDelta<T> mergeToDelta(ObjectDelta<T> firstDelta,
            List<ObjectDelta<T>> modifyDeltas) throws SchemaException {
        if (modifyDeltas.size() == 0) {
            return firstDelta;
        }
        ObjectDelta<T> delta = firstDelta.clone();
        for (ObjectDelta<T> modifyDelta : modifyDeltas) {
            if (modifyDelta == null) {
                continue;
            }
            if (modifyDelta.getChangeType() != ChangeType.MODIFY) {
                throw new IllegalArgumentException("Can only merge MODIFY changes, got " + modifyDelta.getChangeType());
            }
            delta.mergeModifications(modifyDelta.getModifications());
        }
        return delta;
    }

	public static void checkConsistence(Collection<? extends ObjectDelta<?>> deltas) {
		for (ObjectDelta<?> delta: deltas) {
			delta.checkConsistence();
		}
	}
}

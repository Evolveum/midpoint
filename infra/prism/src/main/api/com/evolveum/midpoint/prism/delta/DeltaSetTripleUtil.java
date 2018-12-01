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

import java.util.Collection;

/**
 *
 */
public class DeltaSetTripleUtil {
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
}

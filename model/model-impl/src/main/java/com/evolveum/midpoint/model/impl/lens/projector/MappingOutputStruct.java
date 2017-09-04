/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.lens.projector;

import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;

/**
 * @author semancik
 *
 */
public class MappingOutputStruct<V extends PrismValue> {

	private PrismValueDeltaSetTriple<V> outputTriple = null;
	private boolean strongMappingWasUsed = false;
	private boolean weakMappingWasUsed = false;

	public PrismValueDeltaSetTriple<V> getOutputTriple() {
		return outputTriple;
	}

	public void setOutputTriple(PrismValueDeltaSetTriple<V> outputTriple) {
		this.outputTriple = outputTriple;
	}

	public boolean isStrongMappingWasUsed() {
		return strongMappingWasUsed;
	}

	public void setStrongMappingWasUsed(boolean strongMappingWasUsed) {
		this.strongMappingWasUsed = strongMappingWasUsed;
	}

	public boolean isWeakMappingWasUsed() {
		return weakMappingWasUsed;
	}

	public void setWeakMappingWasUsed(boolean weakMappingWasUsed) {
		this.weakMappingWasUsed = weakMappingWasUsed;
	}

}

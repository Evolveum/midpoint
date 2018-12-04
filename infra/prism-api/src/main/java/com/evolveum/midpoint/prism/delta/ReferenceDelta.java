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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public interface ReferenceDelta extends ItemDelta<PrismReferenceValue,PrismReferenceDefinition> {

	@Override
	Class<PrismReference> getItemClass();

	@Override
	void setDefinition(PrismReferenceDefinition definition);

	@Override
	void applyDefinition(PrismReferenceDefinition definition) throws SchemaException;

	boolean isApplicableToType(Item item);

	@Override
	ReferenceDelta clone();

}

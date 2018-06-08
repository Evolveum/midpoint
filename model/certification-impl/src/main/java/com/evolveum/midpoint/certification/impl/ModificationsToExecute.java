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

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.prism.delta.ItemDelta;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Modifications to execute on an object (mostly on a campaign).
 *
 * Because there can be lots of modifications that could take literally hours to execute (sometimes blocking DB as described
 * e.g. in MID-4611), they are divided into smaller batches.
 *
 * @author mederly
 */
public class ModificationsToExecute {

	public static final int BATCH_SIZE = 50;

	public final List<List<ItemDelta<?, ?>>> batches = new ArrayList<>();

	public List<ItemDelta<?, ?>> getLastBatch() {
		return batches.get(batches.size() - 1);
	}

	public boolean isEmpty() {
		return batches.isEmpty();
	}

	public void add(ItemDelta<?, ?>... deltas) {
		add(Arrays.asList(deltas));
	}

	public void add(Collection<ItemDelta<?, ?>> deltas) {
		if (deltas.isEmpty()) {
			return;
		}
		if (isEmpty() || getLastBatch().size() + deltas.size() > BATCH_SIZE) {
			createNewBatch();
		}
		getLastBatch().addAll(deltas);
	}

	public void createNewBatch() {
		batches.add(new ArrayList<>());
	}

	public int getTotalDeltasCount() {
		int rv = 0;
		for (List<ItemDelta<?, ?>> batch : batches) {
			rv += batch.size();
		}
		return rv;
	}

	public List<ItemDelta<?, ?>> getAllDeltas() {
		return batches.stream().flatMap(Collection::stream).collect(Collectors.toList());
	}
}

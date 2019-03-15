/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.ucf.api.ListeningActivity;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Keeps track of active async update listening activities.
 * Externally visible methods are synchronized to ensure thread safety.
 */
@Component
public class AsyncUpdateListeningRegistry {

	private AtomicLong counter = new AtomicLong(0);

	private Map<String, ListeningActivity> listeningActivities = new HashMap<>();

	@NotNull
	synchronized String registerListeningActivity(ListeningActivity activity) {
		String handle = String.valueOf(counter.incrementAndGet());
		listeningActivities.put(handle, activity);
		return handle;
	}

	synchronized ListeningActivity removeListeningActivity(@NotNull String handle) {
		ListeningActivity listeningActivity = getListeningActivity(handle);
		if (listeningActivity == null) {
			throw new IllegalArgumentException("Listening activity handle " + handle + " is unknown at this moment");
		}
		listeningActivities.remove(handle);
		return listeningActivity;
	}

	synchronized ListeningActivity getListeningActivity(@NotNull String handle) {
		return listeningActivities.get(handle);
	}
}

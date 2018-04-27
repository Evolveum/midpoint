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

package com.evolveum.midpoint.model.intest;

import com.evolveum.midpoint.model.api.ProgressInformation;
import com.evolveum.midpoint.model.api.ProgressListener;
import com.evolveum.midpoint.model.api.context.ModelContext;

/**
 * @author mederly
 */
public class DelayingProgressListener implements ProgressListener {

	private final long delayMin, delayMax;

	public DelayingProgressListener(long delayMin, long delayMax) {
		this.delayMin = delayMin;
		this.delayMax = delayMax;
	}

	@Override
	public void onProgressAchieved(ModelContext modelContext, ProgressInformation progressInformation) {
		try {
			long delay = (long) (delayMin + Math.random() * (delayMax - delayMin));
			System.out.println("[" + Thread.currentThread().getName() + "] Delaying execution by " + delay + " ms for " + progressInformation);
			Thread.sleep(delay);
		} catch (InterruptedException e) {
			// ignore
		}
	}

	@Override
	public boolean isAbortRequested() {
		return false;
	}
}

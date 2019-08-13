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

import com.evolveum.midpoint.prism.PrismProperty;

/**
 *  EXPERIMENTAL
 */
@SuppressWarnings("unused")
public class SynchronizationOperationResult {
	private int changesProcessed;
	private int errors;
	private boolean suspendEncountered;
	private boolean haltingErrorEncountered;
	private PrismProperty<?> lastTokenSeen;
	private PrismProperty<?> taskTokenUpdatedTo;

	public int getChangesProcessed() {
		return changesProcessed;
	}

	public void setChangesProcessed(int changesProcessed) {
		this.changesProcessed = changesProcessed;
	}

	public int getErrors() {
		return errors;
	}

	public void setErrors(int errors) {
		this.errors = errors;
	}

	public boolean isSuspendEncountered() {
		return suspendEncountered;
	}

	public void setSuspendEncountered(boolean suspendEncountered) {
		this.suspendEncountered = suspendEncountered;
	}

	public boolean isHaltingErrorEncountered() {
		return haltingErrorEncountered;
	}

	public void setHaltingErrorEncountered(boolean haltingErrorEncountered) {
		this.haltingErrorEncountered = haltingErrorEncountered;
	}

	public PrismProperty<?> getLastTokenSeen() {
		return lastTokenSeen;
	}

	public void setLastTokenSeen(PrismProperty<?> lastTokenSeen) {
		this.lastTokenSeen = lastTokenSeen;
	}

	public PrismProperty<?> getTaskTokenUpdatedTo() {
		return taskTokenUpdatedTo;
	}

	public void setTaskTokenUpdatedTo(PrismProperty<?> taskTokenUpdatedTo) {
		this.taskTokenUpdatedTo = taskTokenUpdatedTo;
	}

	@Override
	public String toString() {
		return "changesProcessed=" + changesProcessed +
				", errors=" + errors +
				", suspendEncountered=" + suspendEncountered +
				", haltingErrorEncountered=" + haltingErrorEncountered +
				", lastTokenSeen=" + lastTokenSeen +
				", taskTokenUpdatedTo=" + taskTokenUpdatedTo;
	}

	public void incrementErrors() {
		errors++;
	}

	public void incrementChangesProcessed() {
		changesProcessed++;
	}
}

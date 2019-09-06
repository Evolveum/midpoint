/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.prism.PrismProperty;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *  EXPERIMENTAL
 */
@SuppressWarnings({ "unused", "WeakerAccess" })
public class SynchronizationOperationResult {
	private AtomicInteger changesProcessed = new AtomicInteger(0);
	private AtomicInteger errors = new AtomicInteger(0);
	private volatile boolean suspendEncountered;
	private volatile boolean haltingErrorEncountered;
	private PrismProperty<?> lastTokenSeen;
	private PrismProperty<?> taskTokenUpdatedTo;

	public int getChangesProcessed() {
		return changesProcessed.get();
	}

	public int getErrors() {
		return errors.get();
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
		return "changesProcessed=" + changesProcessed.get() +
				", errors=" + errors.get() +
				", suspendEncountered=" + suspendEncountered +
				", haltingErrorEncountered=" + haltingErrorEncountered +
				", lastTokenSeen=" + lastTokenSeen +
				", taskTokenUpdatedTo=" + taskTokenUpdatedTo;
	}

	public int incrementErrors() {
		return errors.incrementAndGet();
	}

	public int incrementChangesProcessed() {
		return changesProcessed.incrementAndGet();
	}
}

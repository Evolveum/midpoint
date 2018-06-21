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

package com.evolveum.midpoint.task.api;

/**
 * @author mederly
 */
public class StatisticsCollectionStrategy {

	private boolean startFromZero;
	private boolean maintainIterationStatistics;
	private boolean maintainSynchronizationStatistics;
	private boolean maintainActionsExecutedStatistics;

	public StatisticsCollectionStrategy() {
	}

	public StatisticsCollectionStrategy(boolean startFromZero) {
		this.startFromZero = startFromZero;
	}

	public StatisticsCollectionStrategy(boolean startFromZero, boolean maintainIterationStatistics,
			boolean maintainSynchronizationStatistics, boolean maintainActionsExecutedStatistics) {
		this.startFromZero = startFromZero;
		this.maintainIterationStatistics = maintainIterationStatistics;
		this.maintainSynchronizationStatistics = maintainSynchronizationStatistics;
		this.maintainActionsExecutedStatistics = maintainActionsExecutedStatistics;
	}

	public boolean isStartFromZero() {
		return startFromZero;
	}

	public void setStartFromZero(boolean startFromZero) {
		this.startFromZero = startFromZero;
	}

	public boolean isMaintainIterationStatistics() {
		return maintainIterationStatistics;
	}

	public void setMaintainIterationStatistics(boolean maintainIterationStatistics) {
		this.maintainIterationStatistics = maintainIterationStatistics;
	}

	public boolean isMaintainSynchronizationStatistics() {
		return maintainSynchronizationStatistics;
	}

	public void setMaintainSynchronizationStatistics(boolean maintainSynchronizationStatistics) {
		this.maintainSynchronizationStatistics = maintainSynchronizationStatistics;
	}

	public boolean isMaintainActionsExecutedStatistics() {
		return maintainActionsExecutedStatistics;
	}

	public void setMaintainActionsExecutedStatistics(boolean maintainActionsExecutedStatistics) {
		this.maintainActionsExecutedStatistics = maintainActionsExecutedStatistics;
	}

	public StatisticsCollectionStrategy fromZero() {
		this.startFromZero = true;
		return this;
	}

	public StatisticsCollectionStrategy fromStoredValues() {
		this.startFromZero = false;
		return this;
	}

	public StatisticsCollectionStrategy maintainIterationStatistics() {
		this.maintainIterationStatistics = true;
		return this;
	}

	public StatisticsCollectionStrategy maintainSynchronizationStatistics() {
		this.maintainSynchronizationStatistics = true;
		return this;
	}

	public StatisticsCollectionStrategy maintainActionsExecutedStatistics() {
		this.maintainActionsExecutedStatistics = true;
		return this;
	}
}

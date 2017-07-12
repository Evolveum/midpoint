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

package com.evolveum.midpoint.web.component.refresh;

import java.io.Serializable;

/**
 * @author mederly
 */
public class AutoRefreshDto implements Serializable {

	private long lastRefreshed = System.currentTimeMillis();		// currently not used (useful if the refresh should not be done on each timer click)
	private int interval;											// in milliseconds
	private boolean enabled = true;

	public AutoRefreshDto() {
	}

	public AutoRefreshDto(int refreshInterval) {
		this.interval = refreshInterval;
	}

	public int getInterval() {
		return interval;
	}

	public void setInterval(int interval) {
		this.interval = interval;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getLastRefreshed() {
		return lastRefreshed;
	}

	public void setLastRefreshed(long lastRefreshed) {
		this.lastRefreshed = lastRefreshed;
	}

	public boolean shouldRefresh() {
		return isEnabled() && System.currentTimeMillis() - lastRefreshed > interval;
	}

	public int getRefreshingIn() {
		long delta = interval - (System.currentTimeMillis() - lastRefreshed);
		if (delta > 0) {
			return (int) (delta / 1000);
		} else {
			return 0;
		}
	}

	public void recordRefreshed() {
		lastRefreshed = System.currentTimeMillis();
	}
}

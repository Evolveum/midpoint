/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.refresh;

import java.io.Serializable;

public class AutoRefreshDto implements Serializable {

    private long lastRefreshed = System.currentTimeMillis();        // currently not used (useful if the refresh should not be done on each timer click)
    private int interval;                                            // in milliseconds
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

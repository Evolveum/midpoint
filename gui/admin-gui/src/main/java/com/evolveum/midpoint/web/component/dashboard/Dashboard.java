/*
 * Copyright (c) 2013 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.dashboard;

import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.Component;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class Dashboard<T extends Serializable> implements Serializable {

    private static final int MAX_RELOAD_ATTEMPTS = 10;

    private DashboardPanel dashboardPanel;
    private int reloadAttempts;

    private boolean showMinimize;
    private boolean minimized;

    private boolean lazyLoading;
    private boolean loaded;

    private T object;

    public Dashboard() {
        this(false);
    }

    public Dashboard(boolean lazyLoading) {
        this(lazyLoading, false);
    }

    public Dashboard(boolean lazyLoading, boolean showMinimize) {
        this.lazyLoading = lazyLoading;
        this.showMinimize = showMinimize;
    }

    public boolean isLazyLoading() {
        return lazyLoading;
    }

    public void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isMinimized() {
        return minimized;
    }

    public boolean isShowMinimize() {
        return showMinimize;
    }

    public void setMinimized(boolean minimized) {
        this.minimized = minimized;
    }

    public void setShowMinimize(boolean showMinimize) {
        this.showMinimize = showMinimize;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }

    protected PageBase getPageBase() {
        return dashboardPanel.getBasePage();
    }

    public void recordAttempt() {
        reloadAttempts++;
    }

    public boolean isReloadTimeout() {
        return reloadAttempts >= MAX_RELOAD_ATTEMPTS;
    }
}

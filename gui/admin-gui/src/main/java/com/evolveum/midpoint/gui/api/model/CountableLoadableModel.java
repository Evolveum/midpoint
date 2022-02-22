/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.model;

import java.util.List;

/**
 * A loadable model of a list that knows how to provide count of list items without having to retrieve the actual data.
 * Useful e.g. to implement MID-3938 (Optimize midPoint for many focus assignments).
 */
public abstract class CountableLoadableModel<T> extends LoadableModel<List<T>> {

    public CountableLoadableModel() {
    }

    public CountableLoadableModel(boolean alwaysReload) {
        super(alwaysReload);
    }

    public int count() {
        if (isLoaded()) {
            List<T> object = getObject();
            return object != null ? object.size() : 0;
        } else {
            return countInternal();
        }
    }

    // This should be overridden to provide more efficient implementation, avoiding full loading of objects
    public int countInternal() {
        List<T> object = getObject();
        return object != null ? object.size() : 0;
    }

}

/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.model.IModel;

public abstract class LoadableModel<T> implements IModel<T> {

    private T object;
    private boolean loaded = false;
    private boolean allwaysReload = true;

    public LoadableModel() {
        this(null, true);
    }

    public LoadableModel(boolean allwaysReload) {
        this(null, allwaysReload);
    }

    public LoadableModel(T object) {
        this(object, true);
    }

    public LoadableModel(T object, boolean allwaysReload) {
        this.object = object;
        this.allwaysReload = allwaysReload;
    }

    public T getObject() {
        if (!loaded) {
            setObject(load());
            onLoad();
            this.loaded = true;
        }

        if (object instanceof IModel) {
            IModel model = (IModel) object;
            return (T) model.getObject();
        }
        return object;
    }

    public void setObject(T object) {
        if (this.object instanceof IModel) {
            ((IModel<T>) this.object).setObject(object);
        } else {
            this.object = object;
        }

        this.loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    protected abstract T load();

    protected void onLoad() {
    }

    public void reset() {
        loaded = false;
    }

    public void detach() {
        if (loaded && allwaysReload) {
            this.loaded = false;
            onDetach();
        }
    }

    protected void onDetach() {
    }

    public IModel getNestedModel() {
        if (object instanceof IModel) {
            return (IModel) object;
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(super.toString());
        sb.append(":attached=").append(loaded).append(":object=[").append(this.object).append("]");
        return sb.toString();
    }
}

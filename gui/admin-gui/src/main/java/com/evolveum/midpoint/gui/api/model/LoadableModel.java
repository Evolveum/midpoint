/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.model;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.apache.wicket.model.IModel;

/**
 * Lazy loading Wicket model. This is an IModel implementation
 * that loads the model content only when needed. This is supposed
 * to be used for objects that are expensive to load. So we can avoid
 * loading the at all when a component that uses them is not visible.
 *
 * @author lazyman
 */
public abstract class LoadableModel<T> implements IModel<T> {

    private T object;
    private boolean loaded = false;
    private boolean alwaysReload;

    public LoadableModel() {
        this(null, true);
    }

    public LoadableModel(boolean alwaysReload) {
        this(null, alwaysReload);
    }

    public LoadableModel(T object) {
        this(object, true);
    }

    public LoadableModel(T object, boolean alwaysReload) {
        this.object = object;
        this.alwaysReload = alwaysReload;
    }

    public static <T> LoadableModel<T> create(Producer<T> producer, boolean alwaysReload) {
        return new LoadableModel<T>(alwaysReload) {
            @Override
            protected T load() {
                return producer.run();
            }
        };
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

    public void reset() {
        loaded = false;
    }

    public void detach() {
        if (loaded && alwaysReload) {
            this.loaded = false;
            object = null;

            onDetach();
        }
    }

    public IModel getNestedModel() {
        if (object instanceof IModel) {
            return (IModel) object;
        } else {
            return null;
        }
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(":attached=").append(loaded).append(":object=[").append(this.object).append("]");
        return builder.toString();
    }

    protected abstract T load();

    protected void onLoad() {
    }

    protected void onDetach() {
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        if (isLoaded()) {
            WebComponentUtil.reviveObject(object, prismContext);
        }
    }

}

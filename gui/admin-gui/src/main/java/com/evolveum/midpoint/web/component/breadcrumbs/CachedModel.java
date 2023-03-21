/*
 * Copyright (C) 2022-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.breadcrumbs;

import org.apache.wicket.model.IModel;

/**
 * Breadcrumbs text is not changing over time, we'll try to use this behavior to cache string value created by underlying IModel object.
 * When value is cached, underlying model is forgotten to save space - we don't want to get value when creating cached model (too soon).
 *
 * Created by Viliam Repan (lazyman).
 */
public class CachedModel implements IModel<String> {

    private String cachedValue;

    private IModel<String> valueModel;

    public CachedModel(String value) {
        this.cachedValue = value;
    }

    public CachedModel(IModel<String> valueModel) {
        this.valueModel = valueModel;
    }

    @Override
    public String getObject() {
        if (cachedValue != null) {
            return cachedValue;
        }

        if (valueModel != null) {
            cachedValue = valueModel.getObject();
            valueModel = null;
        }

        return cachedValue;
    }

    @Override
    public void detach() {
        if (valueModel != null) {
            valueModel.detach();
        }
    }
}

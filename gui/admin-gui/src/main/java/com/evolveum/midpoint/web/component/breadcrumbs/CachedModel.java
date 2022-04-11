/*
 * Copyright (c) 2010-2022 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.breadcrumbs;

import org.apache.wicket.model.IModel;

/**
 * Breadcrums text is not changing over time, we'll try to use this behavior to cache string value created by underlying IModel object.
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

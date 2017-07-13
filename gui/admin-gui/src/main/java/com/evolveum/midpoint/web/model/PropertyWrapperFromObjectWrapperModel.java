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

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns property real values. This implementation works on ObjectWrapper models (not PrismObject).
 * 
 * Simple implementation, now it can't handle multivalue properties.
 *
 * @author lazyman
 * @author semancik
 */
public class PropertyWrapperFromObjectWrapperModel<T,O extends ObjectType> extends AbstractWrapperModel<PropertyWrapper<PrismProperty<T>,PrismPropertyDefinition<T>>,O> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyWrapperFromObjectWrapperModel.class);

    private ItemPath path;

    public PropertyWrapperFromObjectWrapperModel(IModel<ObjectWrapper<O>> model, QName item) {
        this(model, new ItemPath(item));
    }

    public PropertyWrapperFromObjectWrapperModel(IModel<ObjectWrapper<O>> model, ItemPath path) {
    	super(model);
        Validate.notNull(path, "Item path must not be null.");
        this.path = path;
    }

    @Override
    public PropertyWrapper<PrismProperty<T>, PrismPropertyDefinition<T>> getObject() {
    	PropertyWrapper<PrismProperty<T>, PrismPropertyDefinition<T>> propertyWrapper = getWrapper().findPropertyWrapper(path);
        return propertyWrapper;
    }

    @Override public void setObject(PropertyWrapper<PrismProperty<T>, PrismPropertyDefinition<T>> prismPropertyPropertyWrapper) {
        throw new UnsupportedOperationException("PropertyWrapperFromObjectWrapperModel.setObject called");
    }

    @Override
    public void detach() {
    }

}

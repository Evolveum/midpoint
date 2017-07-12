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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.model.IModel;

/**
 * Model that returns property real values. This implementation works on ObjectWrapper models (not PrismObject).
 * 
 * Simple implementation, now it can't handle multivalue properties.
 *
 * @author mederly
 */
public class ReadOnlyPrismObjectFromObjectWrapperModel<O extends ObjectType> implements IModel<PrismObject<O>> {

    private static final Trace LOGGER = TraceManager.getTrace(ReadOnlyPrismObjectFromObjectWrapperModel.class);

    private IModel<ObjectWrapper<O>> model;

    public ReadOnlyPrismObjectFromObjectWrapperModel(IModel<ObjectWrapper<O>> model) {
    	this.model = model;
    }

    @Override
    public PrismObject<O> getObject() {
        return model.getObject() != null ? model.getObject().getObject() : null;
    }

    @Override
    public void setObject(PrismObject<O> object) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void detach() {
    }
}

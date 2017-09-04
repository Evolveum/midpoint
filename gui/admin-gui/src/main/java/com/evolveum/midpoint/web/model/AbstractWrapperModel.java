/*
 * Copyright (c) 2015-2016 Evolveum
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
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;

/**
 * @author semancik
 */
public abstract class AbstractWrapperModel<T,O extends ObjectType> implements IModel<T> {

    private IModel<ObjectWrapper<O>> wrapperModel;

    public AbstractWrapperModel(IModel<ObjectWrapper<O>> wrapperModel) {
    	Validate.notNull(wrapperModel, "Wrapper model must not be null.");
        this.wrapperModel = wrapperModel;
    }

    public IModel<ObjectWrapper<O>> getWrapperModel() {
		return wrapperModel;
	}

    public ObjectWrapper<O> getWrapper() {
		return wrapperModel.getObject();
	}

    public O getObjectType() {
		return wrapperModel.getObject().getObject().asObjectable();
	}

    public PrismObject<O> getPrismObject() {
		return wrapperModel.getObject().getObject();
	}

    @Override
    public void detach() {
    }

}

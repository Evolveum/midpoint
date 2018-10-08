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

package com.evolveum.midpoint.gui.impl.model;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectPolicyConfigurationType;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns RealValue model. This implementation works on PropertyWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class RealValueFromSingleValuePropertyWrapperModel<T> implements IModel<T> {

	private static final long serialVersionUID = 1L;
   
	private IModel<PropertyWrapper<T>> model;

    public RealValueFromSingleValuePropertyWrapperModel(IModel<PropertyWrapper<T>> model) {
    	this.model = model;
    }

	@Override
	public void detach() {
	}

	@Override
	public T getObject() {
		
		if(model == null || model.getObject() == null || model.getObject().getItemDefinition() == null) {
			return null;
		}
		
		if(model.getObject().getItemDefinition().isSingleValue()) {
			if(model.getObject() == null || model.getObject().getValues() == null || model.getObject().getValues().get(0) == null
					|| model.getObject().getValues().get(0).getValue() == null) {
				return null;
			}
			return model.getObject().getValues().get(0).getValue().getRealValue();
		}
		throw new IllegalStateException("PropertyWrapper  " + model.getObject() + " isn't single value");
	}

	@Override
	public void setObject(T object) {
		throw new UnsupportedOperationException();
	}

}

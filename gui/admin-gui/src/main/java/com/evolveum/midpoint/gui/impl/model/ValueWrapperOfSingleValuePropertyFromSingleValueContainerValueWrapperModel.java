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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

/**
 * Model that returns ValueWrapper model. This implementation works on parent of ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel<T, C extends Containerable> implements IModel<ValueWrapper<T>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel.class);
   
	private IModel<ContainerValueWrapper<C>> model;
	private QName item;

    public ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel(IModel<ContainerValueWrapper<C>> model, QName item) {
    	this.model = model;
    	this.item = item;
    }
    
    public ValueWrapperOfSingleValuePropertyFromSingleValueContainerValueWrapperModel(ContainerValueWrapper<C> value, QName item) {
    	this.model = Model.of(value);
    	this.item = item;
    }

	@Override
	public void detach() {
	}

	@Override
	public ValueWrapper<T> getObject() {
		
		PropertyWrapperFromContainerValueWrapperModel<T, C> propertyModel = new PropertyWrapperFromContainerValueWrapperModel<T, C>(model, item);
		
		if(propertyModel == null || propertyModel.getObject() == null ||  propertyModel.getObject().getValues() == null) {
			return null;
		}
		return (ValueWrapper<T>)propertyModel.getObject().getValues().get(0);
	}

	@Override
	public void setObject(ValueWrapper<T> object) {
		throw new UnsupportedOperationException();
	}

}

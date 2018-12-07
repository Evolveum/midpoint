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
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns PropertyWrapper model. This implementation works on ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class PropertyWrapperFromContainerWrapperModel<T,C extends Containerable> implements IModel<PropertyWrapper<T>> {

	private static final long serialVersionUID = 1L;
   
   	private ContainerWrapper<C> wrapper;
   	private QName item;

    public PropertyWrapperFromContainerWrapperModel(IModel<ContainerWrapper<C>> model, QName item) {
    	this.wrapper = model.getObject();
    	this.item = item;
    }
    
    public PropertyWrapperFromContainerWrapperModel(ContainerWrapper<C> wrapper, QName item) {
    	this.wrapper = wrapper;
    	this.item = item;
    }

	@Override
	public void detach() {
	}

	@Override
	public PropertyWrapper<T> getObject() {
		
		if(wrapper == null || wrapper.getItemDefinition() == null) {
			return null;
		}
		
		if(wrapper.getItemDefinition().isSingleValue()) {
			if(wrapper.getValues() == null) {
				return null;
			}
			ContainerValueWrapper<C> containerValue = wrapper.getValues().get(0);
		
			return new PropertyWrapperFromContainerValueWrapperModel<T, C>(containerValue, item).getObject();
		}
		throw new IllegalStateException("ContainerWrapper  " + wrapper + " isn't single value");
	}

	@Override
	public void setObject(PropertyWrapper<T> object) {
		throw new UnsupportedOperationException();
	}

}

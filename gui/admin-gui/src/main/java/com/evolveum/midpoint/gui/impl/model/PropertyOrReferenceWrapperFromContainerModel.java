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
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.PrismWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns PropertyWrapper model. This implementation works on ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class PropertyOrReferenceWrapperFromContainerModel<C extends Containerable> implements IModel<PropertyOrReferenceWrapper> {

	private static final long serialVersionUID = 1L;
   
   	private PrismWrapper value;
   	private QName qname;

   	/** Model for property wrapper of single valued container.
	 * @param value single valued container
	 * @param item QName of property 
	 */
    public PropertyOrReferenceWrapperFromContainerModel(ContainerValueWrapper<C> value, QName qname) {
    	this.value = value;
    	this.qname = qname;
    }
    
	@Override
	public void detach() {
	}

	@Override
	public PropertyOrReferenceWrapper getObject() {
		if(value == null) {
			return null;
		}
		return (PropertyOrReferenceWrapper)((ContainerValueWrapper)value).findPropertyWrapper(new ItemPath(((ContainerValueWrapper)value).getPath(), qname));
	}

	@Override
	public void setObject(PropertyOrReferenceWrapper object) {
		throw new UnsupportedOperationException();
	}

}

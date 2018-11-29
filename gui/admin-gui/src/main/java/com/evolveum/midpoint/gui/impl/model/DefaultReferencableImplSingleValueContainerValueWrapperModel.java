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
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ReferenceWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns DefaultReferencableImpl model. This implementation works on ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class DefaultReferencableImplSingleValueContainerValueWrapperModel<C extends Containerable> implements IModel<DefaultReferencableImpl> {

	private static final long serialVersionUID = 1L;
   
	private IModel<ContainerValueWrapper<C>> model;
   	private QName item;

    public DefaultReferencableImplSingleValueContainerValueWrapperModel(IModel<ContainerValueWrapper<C>> model, QName item) {
    	this.model = model;
    	this.item = item;
    }

	@Override
	public void detach() {
	}

	@Override
	public DefaultReferencableImpl getObject() {
		
		PropertyOrReferenceWrapper ref = model.getObject().findPropertyWrapper(
				ItemPath.create(model.getObject().getPath(), item));
		
		if(!(ref instanceof ReferenceWrapper)){
			throw new IllegalStateException("Searched property is not ReferenceWrapper");
		}
		
		if(ref.getItemDefinition().isSingleValue()) {
			if(ref == null || ref.getValues() == null || ref.getValues().get(0) == null
					|| ((ValueWrapper<DefaultReferencableImpl>)((ReferenceWrapper)ref).getValues().get(0)).getValue() == null) {
				return null;
			}
			return ((ValueWrapper<DefaultReferencableImpl>)((ReferenceWrapper)ref).getValues().get(0)).getValue().getRealValue();
		}
		throw new IllegalStateException("ContainerValueWrapper  " + model.getObject() + " isn't single value");
	}

	@Override
	public void setObject(DefaultReferencableImpl object) {
		throw new UnsupportedOperationException();
	}

}

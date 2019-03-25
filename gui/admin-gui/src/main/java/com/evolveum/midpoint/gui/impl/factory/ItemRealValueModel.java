/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.gui.impl.factory;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.factory.RealValuable;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapperImpl;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;

public class ItemRealValueModel<T> extends PropertyModel<T>{

	private static final long serialVersionUID = 1L;
		
	
	public ItemRealValueModel(IModel<? extends PrismValueWrapper<T>> modelObject) {
		super(modelObject, "realValue");
	}
	
//	@Override
//	public T getObject() {
//		if (!isProperty && modelObject instanceof ValueWrapper && ((ValueWrapper)modelObject).getItem().getItemDefinition() instanceof PrismReferenceDefinition) {
//			PrismReferenceValue refValue = (PrismReferenceValue) ((ValueWrapper)modelObject).getValue();
//			if (refValue == null) {
//				return null;
//			}
//			return (T) refValue.asReferencable();
//		}
//		
//		return super.getObject();
//	}
	
//	@Override
//	public void setObject(T object) {
//		if (modelObject.getItem().getItemDefinition() instanceof PrismReferenceDefinition) {
//			modelObject.setValue(((Referencable) object).asReferenceValue()); 
//			return;
//		}
//		
//		super.setObject(object);
//	}
}

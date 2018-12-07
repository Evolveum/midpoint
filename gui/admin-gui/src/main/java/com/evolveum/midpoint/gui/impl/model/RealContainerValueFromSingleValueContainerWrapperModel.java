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
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Model that returns RealValue model. This implementation works on parent of ContainerWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class RealContainerValueFromSingleValueContainerWrapperModel<C extends Containerable> implements IModel<C> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(RealContainerValueFromSingleValueContainerWrapperModel.class);
   
	private IModel<ContainerWrapper<C>> model;

    public RealContainerValueFromSingleValueContainerWrapperModel(IModel<ContainerWrapper<C>> model) {
    	this.model = model;
    }
    
    public RealContainerValueFromSingleValueContainerWrapperModel(ContainerWrapper<C> wrapper) {
    	this.model = Model.of(wrapper);
    }

	@Override
	public void detach() {
	}

	@Override
	public C getObject() {
		
		if(model == null || model.getObject() == null || model.getObject().getItemDefinition() == null) {
			return null;
		}
		
		if(model.getObject().getItemDefinition().isSingleValue()) {
			if(model.getObject() == null || model.getObject().getValues() == null || model.getObject().getValues().get(0) == null 
					|| model.getObject().getValues().get(0).getContainerValue() == null) {
				return null;
			}
			return model.getObject().getValues().get(0).getContainerValue().getValue();
		}
		throw new IllegalStateException("ContainerWrapper  " + model + " isn't single value container");
	}

	@Override
	public void setObject(C object) {
		throw new UnsupportedOperationException();
	}

}

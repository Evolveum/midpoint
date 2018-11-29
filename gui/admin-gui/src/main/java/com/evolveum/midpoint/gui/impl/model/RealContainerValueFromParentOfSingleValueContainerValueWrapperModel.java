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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * Model that returns RealValue model. This implementation works on parent of ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class RealContainerValueFromParentOfSingleValueContainerValueWrapperModel<T extends Containerable,C extends Containerable> implements IModel<T> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(RealContainerValueFromParentOfSingleValueContainerValueWrapperModel.class);
   
	private IModel<ContainerValueWrapper<C>> model;
   	private ItemPath item;

    public RealContainerValueFromParentOfSingleValueContainerValueWrapperModel(IModel<ContainerValueWrapper<C>> model, ItemPath item) {
    	this.model = model;
    	this.item = item;
    }

	@Override
	public void detach() {
	}

	@Override
	public T getObject() {
		
		ContainerWrapper<T> child = model.getObject().findContainerWrapper(item);
		
		if(child == null) {
			LOGGER.debug("Child ContainerWrapper is null");
			return null;
		}
		
		RealContainerValueFromSingleValueContainerWrapperModel<T> value = new RealContainerValueFromSingleValueContainerWrapperModel<T>(Model.of(child));
		return value.getObject();
	}

	@Override
	public void setObject(T object) {
		throw new UnsupportedOperationException();
	}

}

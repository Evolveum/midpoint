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
import org.apache.wicket.model.PropertyModel;

/**
 * Model that returns RealValue model. This implementation works on parent of ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class ContainerRealValueModel<C extends Containerable> extends PropertyModel<C> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContainerRealValueModel.class);
   
	/** Model for real value of single valued container.
	 * @param value single valued container
	 */
    public ContainerRealValueModel(ContainerValueWrapper<C> value) {
    	super(value, "containerValue.value");
    }
    
    /** Model for real value of single valued container.
	 * @param wrapper single valued container wrapper
	 */
    public ContainerRealValueModel(ContainerWrapper<C> wrapper) {
    	super(wrapper, "values[0].containerValue.value");
    }
    
    /** Model for real value of single valued container.
	 * @param value parent of single valued container
	 * @param item path to single valued container
	 */
    public <T extends Containerable> ContainerRealValueModel(ContainerValueWrapper<T> value, ItemPath item) {
		this(value.findContainerWrapper(item));
    }
    
    @Override
	public C getObject() {
		
		return super.getObject();
	}

	@Override
	public void setObject(C object) {
		throw new UnsupportedOperationException();
	}

}

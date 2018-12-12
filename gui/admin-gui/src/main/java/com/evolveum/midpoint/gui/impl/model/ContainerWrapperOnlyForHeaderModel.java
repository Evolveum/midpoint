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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;

import javax.xml.namespace.QName;

import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * Model that returns RealValue model. This implementation works on parent of ContainerValueWrapper models (not PrismObject).
 *
 * @author skublik
 * 
 */
public class ContainerWrapperOnlyForHeaderModel<T extends Containerable, C extends Containerable> implements IModel<ContainerWrapper<T>> {
	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContainerWrapperOnlyForHeaderModel.class);
    
	private IModel<ContainerWrapper<C>> model;
	private QName qName;
	private PageBase pageBase;
	
    public ContainerWrapperOnlyForHeaderModel(IModel<ContainerWrapper<C>> model, QName name, PageBase pageBase) {
    	Validate.notNull(model, "no model");
    	this.model = model;
    	this.qName = name;
    	this.pageBase = pageBase;
    }
    
    
    @Override
	public ContainerWrapper<T> getObject() {
		
    	if(model.getObject().getValues().size() < 1) {
    		ContainerValueWrapper<C> value = WebModelServiceUtils.createNewItemContainerValueWrapper(pageBase, model);
    		return value.findContainerWrapper(qName);
    	} else {
    		return model.getObject().getValues().get(0).findContainerWrapper(qName);
    	}
	}

}

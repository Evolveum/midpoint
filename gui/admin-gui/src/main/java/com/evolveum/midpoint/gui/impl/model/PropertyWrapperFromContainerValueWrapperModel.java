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
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.model.AbstractWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingLevelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

/**
 * Model that returns property real values. This implementation works on ObjectWrapper models (not PrismObject).
 *
 * @author katkav
 * 
 */
public class PropertyWrapperFromContainerValueWrapperModel<T,C extends Containerable> implements IModel<PropertyWrapper<T>> {

	private static final long serialVersionUID = 1L;
   
   	private ContainerValueWrapper<C> value;
   	private QName item;

    public PropertyWrapperFromContainerValueWrapperModel(IModel<ContainerValueWrapper<C>> model, QName item) {
    	this.value = model.getObject();
    	this.item = item;
    }
    
    public PropertyWrapperFromContainerValueWrapperModel(ContainerValueWrapper<C> value, QName item) {
    	this.value = value;
    	this.item = item;
    }

	@Override
	public void detach() {
	}

	@Override
	public PropertyWrapper<T> getObject() {
		return (PropertyWrapper<T>)value.findPropertyWrapper(new ItemPath(value.getPath(), item));
	}

	@Override
	public void setObject(PropertyWrapper<T> object) {
		throw new UnsupportedOperationException();
	}

}

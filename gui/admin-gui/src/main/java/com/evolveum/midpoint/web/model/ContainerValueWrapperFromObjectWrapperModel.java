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

package com.evolveum.midpoint.web.model;

import org.apache.commons.lang.Validate;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Model that returns property real values. This implementation works on ObjectWrapper models (not PrismObject).
 *
 * @author katkav
 * 
 */
public class ContainerValueWrapperFromObjectWrapperModel<T extends Containerable, C extends Containerable> implements IModel<PrismContainerValueWrapper<C>> {

   private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValueWrapperFromObjectWrapperModel.class);

    private ItemPath path;
    private IModel<PrismContainerWrapper<T>> parent;

    public ContainerValueWrapperFromObjectWrapperModel(IModel<PrismContainerWrapper<T>> model, ItemPath path) {
    	Validate.notNull(path, "Item path must not be null.");
        this.path = path;
        this.parent = model;
    }


    @Override
    public void detach() {
    }

	@Override
	public PrismContainerValueWrapper<C> getObject() {
		PrismContainerValueWrapper<C> containerWrapper = null;
		try {
			containerWrapper = parent.getObject().findContainerValue(path);
		} catch (SchemaException e) {
			LOGGER.error("Cannot find container value wrapper, \nparent: {}, \npath: {}", parent.getObject(), path);
		}
		return containerWrapper;
	}

	@Override
	public void setObject(PrismContainerValueWrapper<C> arg0) {
		throw new UnsupportedOperationException("ContainerWrapperFromObjectWrapperModel.setObject called");

	}

}

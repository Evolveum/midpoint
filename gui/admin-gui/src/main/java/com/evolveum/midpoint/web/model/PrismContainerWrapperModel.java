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
package com.evolveum.midpoint.web.model;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperModel<T extends Containerable, C extends Containerable>  implements IModel<PrismContainerWrapper<C>> {

	private IModel<PrismContainerWrapper<T>> parent;
	private ItemName path;
	
	
	public PrismContainerWrapperModel(IModel<PrismContainerWrapper<T>> parent, ItemName path) {
		this.parent = parent;
		this.path = path;
	}
	
	@Override
	public PrismContainerWrapper<C> getObject() {
		PrismContainerWrapper<?> parentObject = parent.getObject();
		return parentObject.findContainer(path);
	}

	@Override
	public void setObject(PrismContainerWrapper<C> object) {
		// TODO Auto-generated method stub
		IModel.super.setObject(object);
	}
	
	
	

	
	
	
}

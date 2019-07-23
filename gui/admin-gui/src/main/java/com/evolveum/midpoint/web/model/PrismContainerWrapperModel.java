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
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperModel<C extends Containerable, T extends Containerable>  extends ItemWrapperModel<C, PrismContainerWrapper<T>> {

	private static final long serialVersionUID = 1L;

	
	PrismContainerWrapperModel(IModel<?> parent, ItemPath path, boolean fromContainerValue) {
		super(parent, path, fromContainerValue);
	}
	
	public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemPath path) {
		return new PrismContainerWrapperModel<C,T>(parent, path, false);
	}
	
	public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerWrapper(IModel<? extends PrismContainerWrapper<C>> parent, ItemName path) {
		return new PrismContainerWrapperModel<C,T>(parent, ItemPath.create(path), false);
	}
	
	public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemPath path) {
		return new PrismContainerWrapperModel<C,T>(parent, path, true);
	}
	
	public static <C extends Containerable, T extends Containerable> PrismContainerWrapperModel<C, T> fromContainerValueWrapper(IModel<PrismContainerValueWrapper<C>> parent, ItemName path) {
		return new PrismContainerWrapperModel<C,T>(parent, ItemPath.create(path), true);
	}
	
	
	@Override
	public PrismContainerWrapper<T> getObject() {
		return getItemWrapper(PrismContainerWrapper.class);
	}
		
	
}

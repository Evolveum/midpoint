/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 *
 */
public class PrismContainerWrapperHeaderModel<C extends Containerable, T extends Containerable> extends ItemWrapperModel<C, PrismContainerWrapper<T>>{

	private static final long serialVersionUID = 1L;
	private PageBase pageBase;
	
	public PrismContainerWrapperHeaderModel(IModel<?> parent, ItemPath path, PageBase pageBase) {
		super(parent, path, false);
		this.pageBase = pageBase;
	}
	
	@Override
	public PrismContainerWrapper<T> getObject() {
		PrismContainerWrapper<T> ret = (PrismContainerWrapper<T>) getItemWrapperForHeader(PrismContainerDefinition.class, pageBase);
		return ret;
	}

}
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
package com.evolveum.midpoint.gui.impl.prism;

import java.util.List;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ValueWrapperOld;

/**
 * @author katka
 *
 */
public class PrismPropertyWrapperImpl<T> extends ItemWrapperImpl<PrismPropertyValue<T>, PrismProperty<T>, PrismPropertyDefinition<T>>{

	private static final long serialVersionUID = 1L;

	/**
	 * @param parent
	 * @param item
	 * @param status
	 * @param fullPath
	 * @param prismContext
	 */
	public PrismPropertyWrapperImpl(ContainerValueWrapper<?> parent, PrismProperty<T> item, ItemStatus status, ItemPath fullPath,
			PrismContext prismContext) {
		super(parent, item, status, fullPath, prismContext);
		// TODO Auto-generated constructor stub
	}


	

}

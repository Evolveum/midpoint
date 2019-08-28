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
package com.evolveum.midpoint.gui.impl.prism;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;

/**
 * @author skublik
 *
 */
public class ProfilingClassLoggerPanel extends PrismContainerPanel<ClassLoggerConfigurationType> {

	public ProfilingClassLoggerPanel(String id, IModel<PrismContainerWrapper<ClassLoggerConfigurationType>> model, ItemPanelSettings settings) {
		super(id, model, new ItemPanelSettingsBuilder().visibilityHandler(itemWrapper -> checkVisibility(itemWrapper, settings.getVisibilityHandler())).build());
	}
	
	private static ItemVisibility checkVisibility(ItemWrapper itemWrapper, ItemVisibilityHandler visibilitytHandler) {

		if(itemWrapper.getItemName().equals(ClassLoggerConfigurationType.F_PACKAGE)) {
			return ItemVisibility.HIDDEN;
		}
		return visibilitytHandler != null ? visibilitytHandler.isVisible(itemWrapper) : ItemVisibility.AUTO;
	}
}

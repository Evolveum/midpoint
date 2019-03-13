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

import java.io.Serializable;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.component.PrismContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.PrismContainerHeaderPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;

/**
 * @author katka
 *
 */
public class PrismContainerWrapperImpl<C extends Containerable> extends ItemWrapperImpl<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>> implements PrismContainerWrapper<C>, Serializable{

	private static final long serialVersionUID = 1L;
	
	private boolean showOnTopLevel;
	
	private boolean expanded;
	
	/**
	 * @param parent
	 * @param item
	 * @param status
	 * @param fullPath
	 * @param prismContext
	 */
	public PrismContainerWrapperImpl(ContainerValueWrapper<?> parent, PrismContainer<C> item, ItemStatus status,
			ItemPath fullPath, PrismContext prismContext) {
		super(parent, item, status, fullPath, prismContext);
		this.expanded = !item.isEmpty();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setExpanded(boolean)
	 */
	@Override
	public void setExpanded(boolean expanded) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isExpanded()
	 */
	@Override
	public boolean isExpanded() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setShowOnTopLevel(boolean)
	 */
	@Override
	public void setShowOnTopLevel(boolean setShowOnTopLevel) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isShowOnTopLevel()
	 */
	@Override
	public boolean isShowOnTopLevel() {
		// TODO Auto-generated method stub
		return false;
	}

	
//	@Override
//	public boolean isVisible() {
//		
//		
//		PrismContainerDefinition<C> def = getItemDefinition();
//
//		if (def.getProcessing() != null && def.getProcessing() != ItemProcessing.AUTO) {
//			return false;		
//		}
//		
//		if (def.isOperational() && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
//			return false;
//		}
//		
//		if (def.isDeprecated() && isEmpty()) {
//			return false;
//		}
//		
//		
//
//		return false;
//	}
	

	
}

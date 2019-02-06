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
public class PrismContainerWrapperImpl<C extends Containerable> extends ItemWrapperImpl<PrismContainerValue<C>, PrismContainer<C>, PrismContainerDefinition<C>>implements PrismContainerWrapper<C>, Serializable{

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

	
	@Override
	public boolean isVisible() {
		
		
		PrismContainerDefinition<C> def = getItemDefinition();

		if (def.getProcessing() != null && def.getProcessing() != ItemProcessing.AUTO) {
			return false;		
		}
		
		if (def.isOperational() && (!def.getTypeName().equals(MetadataType.COMPLEX_TYPE))) {
			return false;
		}
		
		if (def.isDeprecated() && isEmpty()) {
			return false;
		}
		
		

		return false;
	}
	
	public Panel createHeader(String id) {
		PrismContainerHeaderPanel<C> header = new PrismContainerHeaderPanel<C>(id, Model.of(this)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
//				addOrReplaceProperties(model, form, isPanelVisible, true);
				target.add(getParent());
			}


    	};
        header.add(new VisibleEnableBehaviour(){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible(){
                return getItemDefinition().isMultiValue();
            }
        });
        header.setOutputMarkupId(true);
        return header;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.ItemWrapper#createPanel(java.lang.String, org.apache.wicket.markup.html.form.Form, com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler)
	 */
	@Override
	public Panel createPanel(String id, Form form, ItemVisibilityHandler visibilityHandler) {
		PrismContainerPanel<C> containerPanel = new PrismContainerPanel<C>(id, this, form, visibilityHandler);
		containerPanel.setOutputMarkupId(true);
		
		if(getItemDefinition().isMultiValue()) {
			containerPanel.add(AttributeModifier.append("class", "prism-multivalue-container"));
        }
		
		containerPanel.add( new VisibleEnableBehaviour() {
				private static final long serialVersionUID = 1L;

				@Override
	        	public boolean isVisible() {
	        		return PrismContainerWrapperImpl.this.isVisible(visibilityHandler);
	        	}
	        });
		
		return containerPanel;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setShowOnTopLevel()
	 */
	@Override
	public void setShowOnTopLevel(boolean showOnTopLevel) {
		this.showOnTopLevel = showOnTopLevel;		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.impl.prism.ItemWrapperImpl#isShowEmpty()
	 */
	@Override
	public boolean isShowEmpty() {
		// TODO Auto-generated method stub
		return super.isShowEmpty();
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#setExpanded()
	 */
	@Override
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isExpanded()
	 */
	@Override
	public boolean isExpanded() {
		return expanded;
	}


	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper#isShowOnTopLevel()
	 */
	@Override
	public boolean isShowOnTopLevel() {
		// TODO Auto-generated method stub
		return false;
	};
	
}

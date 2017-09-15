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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

/**
 * @author semancik
 *
 * WARNING: super ugly code ahead
 */
public abstract class PrismHeaderPanel<T extends PrismWrapper> extends BasePanel<T> {
	private static final long serialVersionUID = 1L;

	
	private static final String ID_LABEL = "label";

	private static final Trace LOGGER = TraceManager.getTrace(PrismHeaderPanel.class);


    public PrismHeaderPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

	private void initLayout() {

		setOutputMarkupId(true);
		
		initButtons();

        IModel<String> headerLabelModel = new AbstractReadOnlyModel<String>() {
        	private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {

				PrismWrapper wrapper = getModelObject();
				String displayName = "displayName.not.set";
				if (wrapper instanceof ContainerValueWrapper) {
					displayName = ((ContainerValueWrapper) wrapper).getDisplayName();
				} else if (wrapper instanceof ContainerWrapper) {
		    		displayName = ((ContainerWrapper)wrapper).getDisplayName();
		    	} else if (wrapper instanceof ObjectWrapper) {
		    		// HACK HACK HACK
			        // If we would display label for the object itself, display label for main container instead
			        // the "object label" is actually displayed in front of main container
					ContainerWrapper mainContainerWrapper = ((ObjectWrapper) wrapper).findMainContainerWrapper();
					if (mainContainerWrapper != null) {
						displayName = mainContainerWrapper.getDisplayName();
					} else {
						displayName = ((ObjectWrapper) wrapper).getDisplayName();		// e.g. resource wizard needs this
					}
		    	}
		    	return getString(displayName, null, displayName);
			}
		};

        add(new Label(ID_LABEL, headerLabelModel));
    }


    protected abstract void initButtons();
    
    protected void onButtonClick(AjaxRequestTarget target) {

    }

    public boolean isButtonsVisible() {
    	return true;
    }

}

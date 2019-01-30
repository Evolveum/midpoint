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

package com.evolveum.midpoint.gui.impl.component.prism;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemVisibilityHandler;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.commons.lang.Validate;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

/**
 * @author lazyman
 */
public abstract class AbstractPrismPropertyPanel<IW extends ItemWrapper> extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(AbstractPrismPropertyPanel.class);
    private static final String ID_LABEL = "label";

    private IModel<IW> model;

    private Form form;

    public AbstractPrismPropertyPanel(String id, final IModel<IW> model, Form form, ItemVisibilityHandler visibilityHandler) {
        super(id, model);
        Validate.notNull(model, "no model");
        this.model = model;
        this.form = form;

        LOGGER.trace("Creating property panel for {}", model.getObject());

        setOutputMarkupId(true);
        add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
            	IW propertyWrapper = model.getObject();
                boolean visible = AbstractPrismPropertyPanel.this.isVisible(visibilityHandler);
                LOGGER.trace("isVisible: {}: {}", propertyWrapper, visible);
                return visible;
            }

            @Override
            public boolean isEnabled() {
                return isPanelEnabled();
            }
        });

    }
    
    /* (non-Javadoc)
     * @see org.apache.wicket.MarkupContainer#onInitialize()
     */
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout(model, form);
    }
    
    protected boolean isPanelEnabled() {
    	return !model.getObject().isReadonly();
    }
    
    public boolean isVisible(ItemVisibilityHandler visibilityHandler) {
    	IW propertyWrapper = getModel().getObject();
    	
    	if (visibilityHandler != null) {
    		ItemVisibility visible = visibilityHandler.isVisible(propertyWrapper);
    		if (visible != null) {
    			switch (visible) {
    				case VISIBLE:
    					return true;
    				case HIDDEN:
    					return false;
    				default:
    					// automatic, go on ...
    			}
    		}
    	}
        return propertyWrapper.isVisible();
    }
    
    
    public IModel<IW> getModel() {
        return model;
    }
    
    protected abstract WebMarkupContainer getHeader(String idComponent);
    
    protected abstract <T> WebMarkupContainer getValues(String idComponent, final IModel<IW> model, final Form form);

    private void initLayout(final IModel<IW> model, final Form form) {
    	add(getHeader(ID_LABEL));
        add(getValues("values", model, form));
    }
    
    protected PageBase getPageBase() {
    	return (PageBase) getPage();
    }
}

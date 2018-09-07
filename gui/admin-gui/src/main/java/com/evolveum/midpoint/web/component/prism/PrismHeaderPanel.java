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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

/**
 * @author semancik
 *
 * WARNING: super ugly code ahead
 */
public abstract class PrismHeaderPanel<T extends PrismWrapper> extends BasePanel<T> {
	private static final long serialVersionUID = 1L;

	
	protected static final String ID_LABEL = "label";
	private static final String ID_EXPAND_COLLAPSE_CONTAINER = "expandCollapse";
	protected static final String ID_LABEL_CONTAINER = "labelContainer";
	protected static final String ID_HELP = "help";

	private static final Trace LOGGER = TraceManager.getTrace(PrismHeaderPanel.class);


    public PrismHeaderPanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

	private void initLayout() {

		setOutputMarkupId(true);
		
		add(initExpandCollapseButton(ID_EXPAND_COLLAPSE_CONTAINER));
		initButtons();
		initHeaderLabel();

    }

    protected WebMarkupContainer initExpandCollapseButton(String contentAreaId){
    	return new WebMarkupContainer(contentAreaId);
    }

	protected void initHeaderLabel(){
		
		WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        
        add(labelContainer);

        String displayName = getLabel();
        if (StringUtils.isEmpty(displayName)) {
            displayName = "displayName.not.set";
        }
        StringResourceModel headerLabelModel = createStringResource(displayName);
        labelContainer.add(new Label(ID_LABEL, headerLabelModel));
        
        labelContainer.add(getHelpLabel());
    }
	
	protected Label getHelpLabel() {
		final IModel<String> helpText = new LoadableModel<String>(false) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return getHelpText();
            }
        };
        Label help = new Label(ID_HELP);
        help.add(AttributeModifier.replace("title", helpText));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return StringUtils.isNotEmpty(helpText.getObject()) && isVisibleHelpText();
            }
        });
        return help;
	}
	
	protected String getHelpText() {
		return "";
	}

	protected boolean isVisibleHelpText() {
		return false;
	}

    protected abstract void initButtons();
    
    protected void onButtonClick(AjaxRequestTarget target) {

    }
    
    public abstract String getLabel();

    public boolean isButtonsVisible() {
    	return true;
    }

}

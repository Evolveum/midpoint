/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleButton;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

/**
 * @author semancik
 * 
 * WARNING: super ugly code ahead
 */
public class PrismHeaderPanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String ID_SHOW_EMPTY_FIELDS = "showEmptyFields";
    private static final String ID_SORT_PROPERTIES = "sortProperties";
	private static final String ID_LABEL = "label";

	private static final Trace LOGGER = TraceManager.getTrace(PrismHeaderPanel.class);

    
    public PrismHeaderPanel(String id, IModel model) {
        super(id);
        
        initLayout(model);
    }

	private void initLayout(final IModel model) {
		
		VisibleEnableBehaviour buttonsVisibleBehaviour = new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return PrismHeaderPanel.this.isButtonsVisible();
			}
		};
		
		ToggleButton showEmptyFieldsButton = new ToggleButton(ID_SHOW_EMPTY_FIELDS,
				GuiStyleConstants.CLASS_ICON_SHOW_EMPTY_FIELDS, GuiStyleConstants.CLASS_ICON_NOT_SHOW_EMPTY_FIELDS) {
			private static final long serialVersionUID = 1L;

			@Override
            public void onClick(AjaxRequestTarget target) {
            	ObjectWrapper objectWrapper = getObjectWrapper(model);
                objectWrapper.setShowEmpty(!objectWrapper.isShowEmpty());
                onButtonClick(target);
            }

			@Override
			public boolean isOn() {
				return getObjectWrapper(model).isShowEmpty();
			}
        };
        showEmptyFieldsButton.add(buttonsVisibleBehaviour);
        add(showEmptyFieldsButton);

        ToggleButton sortPropertiesButton = new ToggleButton(ID_SORT_PROPERTIES,
        		GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC, GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC) {
        	private static final long serialVersionUID = 1L;
        	
        	@Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper objectWrapper = getObjectWrapper(model);
                objectWrapper.setSorted(!objectWrapper.isSorted());
                objectWrapper.sort();

                onButtonClick(target);
            }
        	
        	@Override
			public boolean isOn() {
				return getObjectWrapper(model).isSorted();
			}
        };
        sortPropertiesButton.add(buttonsVisibleBehaviour);
        add(sortPropertiesButton);

        IModel<String> headerLabelModel = new PropertyModel<>(model, "displayName");
//
//        if (model.getObject().isMain()){
////            headerLabelModel = new StringResourceModel(resourceKey, this);
//            ContainerWrapper wrappper = model.getObject();
//            ObjectWrapper objwrapper = wrappper.getObject();
//            ObjectWrapper objectWrapper = getObjectWrapper(model);
//            final String key = objwrapper != null ? objwrapper.getDisplayName() : "";
//
//            headerLabelModel = new IModel<String>() {
//                @Override
//                public String getObject() {
//                    String displayName = PageBase.createStringResourceStatic(getPage(), key).getString();
//                    if (displayName.equals(key)){
//                        displayName = (new PropertyModel<String>(model, "displayName")).getObject();
//                    }
//                    return displayName;
//                }
//
//                @Override
//                public void setObject(String o) {
//
//                }
//
//                @Override
//                public void detach() {
//
//                }
//            };
//        } else {
//            headerLabelModel = new PropertyModel<>(model, "displayName");
//        }
        add(new Label(ID_LABEL, headerLabelModel));
    }
    
    private ObjectWrapper getObjectWrapper(IModel model) {
    	Object wrapper = model.getObject();
    	ObjectWrapper objectWrapper = null;
    	if (wrapper instanceof ContainerWrapper) {
    		return ((ContainerWrapper)wrapper).getObject();
    	} else if (wrapper instanceof ObjectWrapper) {
    		return (ObjectWrapper)wrapper;
    	}
    	return null;
    }
    
    protected void onButtonClick(AjaxRequestTarget target) {
    	
    }
    
    public boolean isButtonsVisible() {
    	return true;
    }

}

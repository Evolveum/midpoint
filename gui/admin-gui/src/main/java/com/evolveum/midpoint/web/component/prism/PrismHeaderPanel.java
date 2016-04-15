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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.objectdetails.FocusDetailsTabPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    
    public PrismHeaderPanel(String id, IModel model, boolean showButtons) {
        super(id);
        
        initLayout(model);
    }

	private void initLayout(final IModel model) {
        AjaxLink showEmptyFieldsButton = new AjaxLink(ID_SHOW_EMPTY_FIELDS) {
            @Override
            public void onClick(AjaxRequestTarget target) {
            	ObjectWrapper objectWrapper = getObjectWrapper(model);
                objectWrapper.setShowEmpty(!objectWrapper.isShowEmpty());
                onButtonClick(target);
            }
        };
        showEmptyFieldsButton.setEscapeModelStrings(false);
        showEmptyFieldsButton.setBody(new Model<String>(){
			@Override
			public String getObject() {
				ObjectWrapper objectWrapper = getObjectWrapper(model);
				if (objectWrapper.isShowEmpty()) {
					return "<i class=\""+GuiStyleConstants.CLASS_ICON_SHOW_EMPTY_FIELDS+"\"></i>";
				} else {
					return "<i class=\""+GuiStyleConstants.CLASS_ICON_NOT_SHOW_EMPTY_FIELDS+"\"></i>";
				}
			}
        });
        add(showEmptyFieldsButton);

        AjaxLink sortProperties = new AjaxLink(ID_SORT_PROPERTIES) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectWrapper objectWrapper = getObjectWrapper(model);
                objectWrapper.setSorted(!objectWrapper.isSorted());
                objectWrapper.sort();

                onButtonClick(target);
            }
        };
        sortProperties.setEscapeModelStrings(false);
        sortProperties.setBody(new Model<String>(){
			@Override
			public String getObject() {
				ObjectWrapper objectWrapper = getObjectWrapper(model);
				if (objectWrapper.isSorted()) {
					return "<i class=\""+GuiStyleConstants.CLASS_ICON_SORT_ALPHA_ASC+"\"></i>";
				} else {
					return "<i class=\""+GuiStyleConstants.CLASS_ICON_SORT_AMOUNT_ASC+"\"></i>";
				}
			}
        });
        add(sortProperties);

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

}

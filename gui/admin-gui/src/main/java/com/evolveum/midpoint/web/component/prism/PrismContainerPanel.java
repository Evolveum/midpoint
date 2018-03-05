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

import java.util.List;

import com.evolveum.midpoint.web.component.assignment.ConstructionDetailsPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 * @author semancik
 */
public class PrismContainerPanel<C extends Containerable> extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PrismContainerPanel.class);
    private static final String ID_HEADER = "header";
    private static final String STRIPED_CLASS = "striped";

    private PageBase pageBase;

    public PrismContainerPanel(String id, final IModel<ContainerWrapper<C>> model, boolean showHeader, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase) {
        super(id);
        setOutputMarkupId(true); 
		this.pageBase = pageBase;

        LOGGER.trace("Creating container panel for {}", model.getObject());

        //TODO: visible behaviour??
        add( new VisibleEnableBehaviour() {
        	
        	@Override
        	public boolean isVisible() {
        		if (isPanelVisible != null && model.getObject() != null && !isPanelVisible.isVisible(model.getObject())) {
        			return false;
        		}
                return model.getObject() != null && model.getObject().isVisible();
        	}
        });
        
        initLayout(model, form, isPanelVisible, showHeader);
        
    }

    private void initLayout(final IModel<ContainerWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean showHeader) {
//    	PrismContainerHeaderPanel header = new PrismContainerHeaderPanel(ID_HEADER, model) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void onButtonClick(AjaxRequestTarget target) {
//				addOrReplaceProperties(model, form, isPanelVisible, true);
//				target.add(PrismContainerPanel.this.findParent(PrismPanel.class));
//			}
//
//
//    	};
//        header.add(new VisibleEnableBehaviour(){
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible(){
//                return showHeader;
//            }
//        });
//        header.setOutputMarkupId(true);
//        add(header);

        addOrReplaceProperties(model, form, isPanelVisible, false);
    }

    public PageBase getPageBase(){
        return pageBase;
    }

    private IModel<String> createStyleClassModel(final IModel<ItemWrapper> wrapper) {
        return new AbstractReadOnlyModel<String>() {
        	private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
            	ItemWrapper property = wrapper.getObject();
                return property.isStripe() ? "stripe" : null;
            }
        };
    }

    private void addOrReplaceProperties(IModel<ContainerWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean isToBeReplaced){
    	
    	
    	ListView<ContainerValueWrapper<C>> values = new ListView<ContainerValueWrapper<C>>("values", new PropertyModel<>(model, "values")) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<C>> item) {
			    //todo simplified construction panel will be in the gui starting from 3.8
//                if (model.getObject().getName().equals(AssignmentType.F_CONSTRUCTION)){
//                    ConstructionDetailsPanel panel = new ConstructionDetailsPanel("value", item.getModel());
//                    panel.setOutputMarkupId(true);
//                    item.add(panel);
//                } else {
                    ContainerValuePanel<C> containerPanel = new ContainerValuePanel<>("value", item.getModel(), true, form, isPanelVisible, pageBase);
                    containerPanel.setOutputMarkupId(true);
                    item.add(containerPanel);
//                }
				
			}
			
		};
    	
    	values.setReuseItems(true);
        if (isToBeReplaced) {
            replace(values);
        } else {
            add(values);
        }
    }
}

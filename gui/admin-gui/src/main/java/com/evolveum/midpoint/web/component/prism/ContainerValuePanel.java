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

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 * @author semancik
 */
public class ContainerValuePanel<C extends Containerable> extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValuePanel.class);
    private static final String ID_HEADER = "header";
    private static final String STRIPED_CLASS = "striped";

    private PageBase pageBase;

    public ContainerValuePanel(String id, final IModel<ContainerValueWrapper<C>> model, boolean showHeader, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase) {
        super(id, model);
        setOutputMarkupId(true);
		this.pageBase = pageBase;
		
		add(new VisibleEnableBehaviour() {

			@Override
			public boolean isVisible() {
				return model.getObject().isVisible();
			}
		});

        LOGGER.trace("Creating container panel for {}", model.getObject());

        initLayout(model, form, isPanelVisible, showHeader);
    }

    private void initLayout(final IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanelVisible, boolean showHeader) {
    	PrismContainerValueHeaderPanel<C> header = new PrismContainerValueHeaderPanel<C>(ID_HEADER, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
				addOrReplaceProperties(model, form, isPanelVisible, true);
				target.add(ContainerValuePanel.this);
			}

			@Override
            protected void addNewContainerValuePerformed(AjaxRequestTarget ajaxRequestTarget){
                super.addNewContainerValuePerformed(ajaxRequestTarget);
                addOrReplaceProperties(model, form, isPanelVisible, true);
                ajaxRequestTarget.add(ContainerValuePanel.this);
            }


            protected void reloadParentContainerPanel(AjaxRequestTarget target){
                target.add(ContainerValuePanel.this);
            }
        };
        header.add(new VisibleEnableBehaviour() {
        	private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return showHeader;// && !model.getObject().isMain();
            }
        });
        add(header);
        header.setOutputMarkupId(true);

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

    private <IW extends ItemWrapper> void addOrReplaceProperties(IModel<ContainerValueWrapper<C>> model, final Form form, ItemVisibilityHandler isPanaleVisible, boolean isToBeReplaced){
        ListView<IW> properties = new ListView<IW>("properties",
            new PropertyModel<>(model, "properties")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(final ListItem<IW> item) {
				
				if (item.getModel().getObject() instanceof ContainerWrapper) {
					PrismContainerPanel<C> containerPanel = new PrismContainerPanel("property", (IModel<ContainerWrapper<C>>) item.getModel(), true, form, isPanaleVisible, pageBase);
					containerPanel.setOutputMarkupId(true);
					item.add(new VisibleEnableBehaviour(){
                        private static final long serialVersionUID = 1L;

                        public boolean isVisible(){
                            return containerPanel.isVisible();
                        }
                    });
					item.add(containerPanel);
					return;
				}
				
				PrismPropertyPanel propertyPanel = new PrismPropertyPanel("property", item.getModel(), form, isPanaleVisible, pageBase);
				propertyPanel.setOutputMarkupId(true);
                item.add(propertyPanel);
                item.add(AttributeModifier.append("class", createStyleClassModel((IModel<ItemWrapper>) item.getModel())));
               
            }
        };
        properties.setReuseItems(true);
        if (isToBeReplaced) {
            replace(properties);
        } else {
            add(properties);
        }
    }
}

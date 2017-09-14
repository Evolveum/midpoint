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
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author lazyman
 * @author semancik
 */
public class ContainerValuePanel extends Panel {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(ContainerValuePanel.class);
    private static final String ID_HEADER = "header";
    private static final String STRIPED_CLASS = "striped";

    private PageBase pageBase;

    public ContainerValuePanel(String id, final IModel<ContainerValueWrapper> model, boolean showHeader, Form form, PageBase pageBase) {
        super(id, model);
        setOutputMarkupId(true);
		this.pageBase = pageBase;

        LOGGER.trace("Creating container panel for {}", model.getObject());

        add(new VisibleEnableBehaviour() {
			private static final long serialVersionUID = 1L;

			@Override
            public boolean isVisible() {
                ContainerValueWrapper<Containerable> containerValueWrapper = model.getObject();
                return containerValueWrapper.isItemVisible();
//                PrismContainer prismContainer = containerWrapper.getItem();
//                if (containerWrapper.getItemDefinition().isOperational()) {
//                    return false;
//                }
//
//                // HACK HACK HACK
//                if (ShadowType.F_ASSOCIATION.equals(prismContainer.getElementName())) {
//                	return true;
//                }

//                containerValueWrapper.is
//                boolean isVisible = false;
//                for (ItemWrapper item : containerValueWrapper.getItems()) {
//                    if (containerValueWrapper.isItemVisible(item)) {
//                        isVisible = true;
//                        break;
//                    }
//                }

//                return !containerValueWrapper.getItems().isEmpty() && isVisible;
//                return true;
            }
        });

        initLayout(model, form, showHeader);
    }

    private void initLayout(final IModel<ContainerValueWrapper> model, final Form form, boolean showHeader) {
    	PrismHeaderPanel header = new PrismHeaderPanel(ID_HEADER, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
				addOrReplaceProperties(model, form, true);
				target.add(ContainerValuePanel.this.findParent(PrismObjectPanel.class));
			}

			@Override
			public boolean isButtonsVisible() {
				return true;
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

        addOrReplaceProperties(model, form, false);
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

    private <IW extends ItemWrapper> void addOrReplaceProperties(IModel<ContainerValueWrapper> model, final Form form, boolean isToBeReplaced){
        ListView<IW> properties = new ListView<IW>("properties",
                new PropertyModel<List<IW>>(model, "properties")) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(final ListItem<IW> item) {
				if (item.getModel().getObject() instanceof ContainerWrapper) {
					PrismContainerPanel containerPanel = new PrismContainerPanel("property", (IModel<ContainerWrapper>) item.getModel(), true, form, pageBase);
					containerPanel.setOutputMarkupId(true);
					item.add(containerPanel);
					return;
				}
				
				PrismPropertyPanel propertyPanel = new PrismPropertyPanel("property", item.getModel(), form, pageBase);
				propertyPanel.setOutputMarkupId(true);
                item.add(propertyPanel);
                item.add(new VisibleEnableBehaviour() {
                	private static final long serialVersionUID = 1L;

                	@Override
                	public boolean isVisible() {
                		return item.getModel().getObject().isVisible(model.getObject().isShowEmpty());
                	}
                });
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

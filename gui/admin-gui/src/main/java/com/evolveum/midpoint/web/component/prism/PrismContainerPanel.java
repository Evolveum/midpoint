/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class PrismContainerPanel extends Panel {

    private boolean showHeader;
    private PageBase pageBase;

    public PrismContainerPanel(String id, IModel<ContainerWrapper> model, Form form) {
        this(id, model, true, form, null);
    }

    public PrismContainerPanel(String id, final IModel<ContainerWrapper> model, boolean showHeader, Form form, PageBase pageBase) {
        super(id);
        this.showHeader = showHeader;
        this.pageBase = pageBase;

        add(new AttributeAppender("class", new Model<>("attributeComponent"), " "));
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ContainerWrapper<? extends PrismContainer> container = model.getObject();
                PrismContainer prismContainer = container.getItem();
                if (prismContainer.getDefinition().isOperational()) {
                    return false;
                }

                boolean isVisible = false;
                for (ItemWrapper item : container.getItems()) {
                    if (container.isItemVisible(item)) {
                        isVisible = true;
                        break;
                    }
                }

                return !container.getItems().isEmpty() && isVisible;
            }
        });

        initLayout(model, form);
    }

    private void initLayout(final IModel<ContainerWrapper> model, final Form form) {
        WebMarkupContainer header = new WebMarkupContainer("header");
        header.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !model.getObject().isMain();
            }
        });
        add(header);

        header.add(new Label("label", new PropertyModel<>(model, "displayName")));

        ListView<ItemWrapper> properties = new ListView<ItemWrapper>("properties",
                new PropertyModel(model, "properties")) {

            @Override
            protected void populateItem(ListItem<ItemWrapper> item) {
//            	if (item.getModel().getObject() instanceof PropertyWrapper){
	                item.add(new PrismPropertyPanel("property", item.getModel(), form, pageBase));
	                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
//            	} else if (item.getModel().getObject() instanceof ReferenceWrapper){
//            		 item.add(new PrismReferencePanel("property", item.getModel(), form, pageBase));
// 	                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));
//            	}
            }
        };
        properties.setReuseItems(true);
        add(properties);
    }

    protected PageBase getPageBase(){
        return pageBase;
    }

    private IModel<String> createStyleClassModel(final IModel<ItemWrapper> wrapper) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
            	ItemWrapper property = wrapper.getObject();
                return property.isVisible() ? "visible" : null;
            }
        };
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }
}

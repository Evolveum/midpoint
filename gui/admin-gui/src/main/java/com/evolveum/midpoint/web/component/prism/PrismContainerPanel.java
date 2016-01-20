/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

/**
 * @author lazyman
 */
public class PrismContainerPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(PrismContainerPanel.class);
    private static final String ID_SHOW_EMPTY_FIELDS = "showEmptyFields";

    private boolean showHeader;
    private boolean showEmptyFields;
    private PageBase pageBase;

    public PrismContainerPanel(String id, IModel<ContainerWrapper> model, Form form) {
        this(id, model, true, false, form, null);
    }

    public PrismContainerPanel(String id, final IModel<ContainerWrapper> model, boolean showHeader, boolean showEmptyFields, Form form, PageBase pageBase) {
        super(id);
        this.showHeader = showHeader;
        this.showEmptyFields = showEmptyFields;
        this.pageBase = pageBase;

        LOGGER.trace("Creating container panel for {}", model.getObject());
        
        add(new AttributeAppender("class", new Model<>("attributeComponent"), " "));
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ContainerWrapper<? extends PrismContainer> containerWrapper = model.getObject();
                PrismContainer prismContainer = containerWrapper.getItem();
                if (prismContainer.getDefinition().isOperational()) {
                    return false;
                }
                
                // HACK HACK HACK
                if (ShadowType.F_ASSOCIATION.equals(prismContainer.getElementName())) {
                	return true;
                }

                boolean isVisible = false;
                for (ItemWrapper item : containerWrapper.getItems()) {
                    if (containerWrapper.isItemVisible(item)) {
                        isVisible = true;
                        break;
                    }
                }

                return !containerWrapper.getItems().isEmpty() && isVisible;
            }
        });

        initLayout(model, form);
    }

    private void initLayout(final IModel<ContainerWrapper> model, final Form form) {
        WebMarkupContainer header = new WebMarkupContainer("header");
        header.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                //
                return true;
            }
        });


        AjaxLink showEmptyFieldsButton = new AjaxLink(ID_SHOW_EMPTY_FIELDS) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                showEmptyFields = !showEmptyFields;
                ContainerWrapper containerWrapper = model.getObject();
                ObjectWrapper objectWrapper = containerWrapper.getObject();
                objectWrapper.setShowEmpty(showEmptyFields);
                target.add(PrismContainerPanel.this);
            }
        };
        header.add(showEmptyFieldsButton);
        add(header);

        IModel headerLabelModel;

        if (model.getObject().isMain()){
//            headerLabelModel = new StringResourceModel(resourceKey, this);
            ContainerWrapper wrappper = model.getObject();
            ObjectWrapper objwrapper = wrappper.getObject();
            final String key = objwrapper.getDisplayName();

            headerLabelModel = new IModel<String>() {
                @Override
                public String getObject() {
                    String displayName = PageTemplate.createStringResourceStatic(getPage(), key).getString();
                    if (displayName.equals(key)){
                        displayName = (new PropertyModel<String>(model, "displayName")).getObject();
                    }
                    return displayName;
                }

                @Override
                public void setObject(String o) {

                }

                @Override
                public void detach() {

                }
            };
        } else {
            headerLabelModel = new PropertyModel<>(model, "displayName");
        }
        header.add(new Label("label", headerLabelModel));

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

    public PageBase getPageBase(){
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

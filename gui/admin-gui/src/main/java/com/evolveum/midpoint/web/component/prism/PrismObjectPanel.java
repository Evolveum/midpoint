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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PrismObjectPanel<O extends ObjectType> extends Panel {
	private static final long serialVersionUID = 1L;
	
	private static final String STRIPED_CLASS = "striped";
	
    private static final String ID_HEADER = "header";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";

    private static final Trace LOGGER = TraceManager.getTrace(PrismObjectPanel.class);

    private PageBase pageBase;

    public PrismObjectPanel(String id, IModel<ObjectWrapper<O>> model, ResourceReference image, Form<ObjectWrapper<O>> form, PageBase pageBase) {
        super(id);
        setOutputMarkupId(true);

        LOGGER.trace("Creating object panel for {}", model.getObject());

        this.pageBase = pageBase;
        initLayout(model, image, form);
    }
    
    private void initLayout(final IModel<ObjectWrapper<O>> model, ResourceReference image, final Form<ObjectWrapper<O>> form) {
        Component headerComponent = createHeader(ID_HEADER, model);
        add(headerComponent);


        ListView<ContainerWrapper> containers = new ListView<ContainerWrapper>(ID_CONTAINERS,
                createContainerModel(model)) {
			private static final long serialVersionUID = 1L;

			@Override
            protected void populateItem(ListItem<ContainerWrapper> item) {
                createContainerPanel(item, form);
            }
        };
        containers.setReuseItems(true);
        add(containers);
    }

    protected Component createHeader(String id, IModel<ObjectWrapper<O>> model) {
    	PrismHeaderPanel header = new PrismHeaderPanel(ID_HEADER, model, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
				target.add(PrismObjectPanel.this);
			}
    		
    	};
        return header;
    }


    protected IModel<List<ContainerWrapper>> createContainerModel(IModel<ObjectWrapper<O>> model){
        return new PropertyModel<>(model, ObjectWrapper.PROPERTY_CONTAINERS);
    }

    protected void createContainerPanel(ListItem<ContainerWrapper> item, Form form){
        PrismContainerPanel panel = new PrismContainerPanel(ID_CONTAINER, item.getModel(), true, form, pageBase);
        panel.setOutputMarkupPlaceholderTag(true);
        item.add(panel);
    }

}

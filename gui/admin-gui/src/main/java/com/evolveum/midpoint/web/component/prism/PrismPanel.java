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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.ResourceReference;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.MetadataPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class PrismPanel<C extends Containerable> extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String STRIPED_CLASS = "striped";

    private static final String ID_HEADER = "header";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";
    private static final String ID_CONTAINER_METADATA = "containerMetadata";

    private static final Trace LOGGER = TraceManager.getTrace(PrismPanel.class);

    private PageBase pageBase;

    public PrismPanel(String id, IModel<List<ContainerWrapper<C>>> model, ResourceReference image, Form form, ItemVisibilityHandler isPanelVisible, PageBase pageBase) {
        super(id, model);
        setOutputMarkupId(true);

        LOGGER.trace("Creating object panel for {}", model.getObject());

        this.pageBase = pageBase;
        initLayout(image, isPanelVisible, form);
    }

    private void initLayout(ResourceReference image, final ItemVisibilityHandler isPanelVisible, Form form) {
         addOrReplaceContainers(form, isPanelVisible, false);

    }

	public void removeAllContainerWrappers() {
		((ListView<ContainerWrapper>) get(ID_CONTAINERS)).removeAll();
	}


    private void addOrReplaceContainers(final Form form, ItemVisibilityHandler isPanelVisible, boolean isToBeReplaced){
        ListView<ContainerWrapper<C>> containers = new ListView<ContainerWrapper<C>>(ID_CONTAINERS,
                getModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ContainerWrapper<C>> item) {
                createContainerPanel(item, isPanelVisible, form);
                
//                createMetadataPanel(model, item, containerPanel);
            }
        };
        containers.setReuseItems(true);

        if (isToBeReplaced) {
            replace(containers);
        } else {
            add(containers);
        }
    }
    
    protected PrismContainerPanel createContainerPanel(ListItem<ContainerWrapper<C>> item, ItemVisibilityHandler isPanelVisible, Form form){
        PrismContainerPanel panel = new PrismContainerPanel(ID_CONTAINER, item.getModel(), false, form, isPanelVisible, pageBase);
        panel.setOutputMarkupPlaceholderTag(true);
        item.add(panel);
        return panel;
    }
    
    private IModel<List<ContainerWrapper<C>>> getModel() {
    	return (IModel<List<ContainerWrapper<C>>>) getInnermostModel();
    }

}

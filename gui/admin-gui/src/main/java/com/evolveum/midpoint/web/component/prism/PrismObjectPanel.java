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
public class PrismObjectPanel<O extends ObjectType> extends Panel {
	private static final long serialVersionUID = 1L;

	private static final String STRIPED_CLASS = "striped";

    private static final String ID_HEADER = "header";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_CONTAINER = "container";
    private static final String ID_CONTAINER_METADATA = "containerMetadata";

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
        Component headerComponent = createHeader(ID_HEADER, model, form);
//        add(headerComponent);

        addOrReplaceContainers(model, form, false);

    }

	public void removeAllContainerWrappers() {
		((ListView<ContainerWrapper>) get(ID_CONTAINERS)).removeAll();
	}

    protected Component createHeader(String id, final IModel<ObjectWrapper<O>> model, final Form<ObjectWrapper<O>> form) {
    	PrismHeaderPanel header = new PrismHeaderPanel(ID_HEADER, model) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onButtonClick(AjaxRequestTarget target) {
                addOrReplaceContainers(model, form, true);
				target.add(PrismObjectPanel.this);
			}

			@Override
			public boolean isButtonsVisible() {
				return true;
			}
    	};
        return header;
    }


    protected IModel<List<ContainerWrapper>> createContainerModel(IModel<ObjectWrapper<O>> model){
        return new PropertyModel<>(model, ObjectWrapper.PROPERTY_CONTAINERS);
    }

    protected PrismContainerPanel createContainerPanel(ListItem<ContainerWrapper> item, Form form){
        PrismContainerPanel panel = new PrismContainerPanel(ID_CONTAINER, item.getModel(), false, form, pageBase);
        panel.setOutputMarkupPlaceholderTag(true);
        item.add(panel);
        return panel;
    }

    protected void createMetadataPanel(IModel<ObjectWrapper<O>> model, ListItem<ContainerWrapper> item, PrismContainerPanel containerPanel){
        //check if metadata container exists for
        //the current item and create metadata panel if yes
        Component metadataPanel;
        Model<Boolean> metadataVisibility = Model.of(false);    //becomes visible only in case metadata exists
        ItemPath metadataContainerPath = item.getModelObject().isMain() ?
                new ItemPath(ObjectType.F_METADATA) :
                new ItemPath(item.getModelObject().getPath(), ObjectType.F_METADATA);

        if (model.getObject().findContainerWrapper(metadataContainerPath) != null){
            ContainerWrapper<MetadataType> metadataContainer = model.getObject().findContainerWrapper(metadataContainerPath);
            metadataVisibility.setObject(true);

            String containerName = item.getModelObject().isMain() ?
                    "Object" : StringUtils.capitalize(item.getModelObject().getPath().last().toString());
            metadataPanel = new MetadataPanel(ID_CONTAINER_METADATA, new AbstractReadOnlyModel<MetadataType>() {
                @Override
                public MetadataType getObject() {
                    return metadataContainer.getItem().getRealValue();
                }
            }, containerName, "");
        } else {
            metadataPanel = new WebMarkupContainer(ID_CONTAINER_METADATA);
        }
        metadataPanel.add(new VisibleEnableBehaviour(){
            @Override
            public boolean isVisible(){
                return containerPanel.isVisible()
                        && metadataVisibility.getObject()
                        && model.getObject().isShowMetadata()
                        && !ContainerStatus.ADDING.equals(model.getObject().getStatus());
            }
        });
        metadataPanel.setOutputMarkupId(true);
        item.add(metadataPanel);
    }

    private void addOrReplaceContainers(IModel<ObjectWrapper<O>> model, final Form form, boolean isToBeReplaced){
        ListView<ContainerWrapper> containers = new ListView<ContainerWrapper>(ID_CONTAINERS,
                createContainerModel(model)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ContainerWrapper> item) {
                PrismContainerPanel containerPanel = createContainerPanel(item, form);
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

}

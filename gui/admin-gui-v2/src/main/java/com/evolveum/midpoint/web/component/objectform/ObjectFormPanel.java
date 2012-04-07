/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.objectform;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public class ObjectFormPanel extends Panel {

    private IModel<PropertyContainerWrapper> model;
    private boolean showEmptyProperties;

    public ObjectFormPanel(String id, IModel<PropertyContainerWrapper> model) {
        super(id);
        Validate.notNull(model, "Model with property container must not be null.");

        this.model = model;
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(ObjectFormPanel.class, "ObjectFormPanel.css"));
    }

    public boolean isShowEmptyProperties() {
        return showEmptyProperties;
    }

    public void setShowEmptyProperties(boolean showEmptyProperties) {
        this.showEmptyProperties = showEmptyProperties;
    }

    private void initLayout() {
        //title
//        Label title = new Label("title", createContainerNameModel(model));
//        //todo title value and visibility
//        add(title);
        //attributes
        ListView<PropertyWrapper> attributes = new ListView<PropertyWrapper>("attributes", createAttributesModel()) {

            @Override
            protected void populateItem(ListItem<PropertyWrapper> listItem) {
                populateListItem(listItem);

                if (!showEmptyProperties && isEmpty(listItem)) {
                    listItem.setVisible(false);
                }
            }
        };
        add(attributes);

        //todo remove !!!
        final MultiLineLabel label = new MultiLineLabel("debug", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return showStatus();
            }
        });
        label.add(new AjaxEventBehavior("onClick") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                target.add(label);
            }
        });
        add(label);
    }

    private boolean isEmpty(ListItem<PropertyWrapper> attribute) {
        //todo set list item visibility
        return false;
    }

    private void populateListItem(ListItem<PropertyWrapper> listItem) {
        final IModel<PropertyWrapper> propertyModel = listItem.getModel();
        Label name = new Label("name", createPropertyNameModel(propertyModel));
        listItem.add(name);


        IModel<List<PropertyValueWrapper>> valuesModel = new LoadableDetachableModel<List<PropertyValueWrapper>>() {

            @Override
            protected List<PropertyValueWrapper> load() {
                return propertyModel.getObject().getPropertyValueWrappers();
            }
        };

        WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        listItem.add(container);

        ListView<PropertyValueWrapper> values = new ListView<PropertyValueWrapper>("values", valuesModel) {

            @Override
            protected void populateItem(ListItem<PropertyValueWrapper> listItem) {
                listItem.setRenderBodyOnly(true);

                ValueFormPanel value = new ValueFormPanel("value", listItem.getModel());
                value.setVisible(!ValueStatus.DELETED.equals(listItem.getModelObject().getStatus()));

                listItem.add(value);
            }
        };
        values.setOutputMarkupId(true);
        container.add(values);
    }

    private <T extends Item> IModel<String> createContainerNameModel(final IModel<PropertyContainerWrapper> model) {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                return PropertyContainerWrapper.getDisplayNameFromItem(model.getObject().getContainer());
            }
        };
    }

    private <T extends Item> IModel<String> createPropertyNameModel(final IModel<PropertyWrapper> model) {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                return PropertyContainerWrapper.getDisplayNameFromItem(model.getObject().getProperty());
            }
        };
    }

    private IModel<List<PropertyWrapper>> createAttributesModel() {
        return new LoadableModel<List<PropertyWrapper>>() {

            @Override
            protected List<PropertyWrapper> load() {
                return model.getObject().getPropertyWrappers();
            }
        };
    }

    @Deprecated
    private String showStatus() {
        StringBuilder builder = new StringBuilder();
        builder.append(model.getObject().toString());

        return builder.toString();
    }
}

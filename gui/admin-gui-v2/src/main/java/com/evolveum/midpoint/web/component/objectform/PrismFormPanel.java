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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismFormPanel extends Panel {

    private IModel<ContainerWrapper> model;
    private boolean showEmptyProperties;

    public PrismFormPanel(String id, IModel<ContainerWrapper> model) {
        super(id);
        Validate.notNull(model, "Model with property container must not be null.");

        add(new AttributeAppender("class", new Model<String>("objectForm"), " "));
        this.model = model;
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(PrismFormPanel.class, "PrismFormPanel.css"));
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
        ListView<PropertyItem> attributes = new ListView<PropertyItem>("attributes", createAttributesModel()) {

            @Override
            protected void populateItem(ListItem<PropertyItem> listItem) {
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

    private boolean isEmpty(ListItem<PropertyItem> attribute) {
        //todo set list item visibility
        return false;
    }

    private void populateListItem(ListItem<PropertyItem> listItem) {
        if (listItem.getModelObject() instanceof PropertyDivider) {
            listItem.add(new AttributeAppender("class", new Model("objectFormSeparator"), " "));

            listItem.add(new PropertyLabelPanel("labelTd",
                    new PropertyModel<String>(listItem.getModel(), "displayName"), true));

            EmptyPanel empty = new EmptyPanel("inputTd");
            empty.setVisible(false);
            listItem.add(empty);
        } else {
            listItem.add(new AttributeAppender("class", new Model<String>("objectFormAttribute"), " "));

            final IModel<PropertyWrapper> propertyModel = (IModel) listItem.getModel();
            PropertyLabelPanel labelPanel = new PropertyLabelPanel("labelTd", createPropertyNameModel(propertyModel), false);
            listItem.add(labelPanel);

            PropertyInputPanel propertyPanel = new PropertyInputPanel("inputTd", propertyModel);
            listItem.add(propertyPanel);
        }
    }

    private <T extends Item> IModel<String> createContainerNameModel(final IModel<ContainerWrapper> model) {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                return ContainerWrapper.getDisplayNameFromItem(model.getObject().getContainer());
            }
        };
    }

    private <T extends Item> IModel<String> createPropertyNameModel(final IModel<PropertyWrapper> model) {
        return new LoadableModel<String>() {

            @Override
            protected String load() {
                return ContainerWrapper.getDisplayNameFromItem(model.getObject().getProperty());
            }
        };
    }

    private IModel<List<PropertyItem>> createAttributesModel() {
        return new LoadableModel<List<PropertyItem>>() {

            @Override
            protected List<PropertyItem> load() {
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

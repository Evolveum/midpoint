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

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * @author lazyman
 */
public class PropertyInputPanel extends Panel {

    public PropertyInputPanel(String id, IModel<PropertyWrapper> model) {
        super(id);
        setOutputMarkupId(true);
        add(new AttributeAppender("class", new Model<String>("objectFormValues"), " "));

        initLayout(model);
    }

    public void initLayout(final IModel<PropertyWrapper> propertyModel) {
        IModel<List<PropertyValueWrapper>> valuesModel = new LoadableDetachableModel<List<PropertyValueWrapper>>() {

            @Override
            protected List<PropertyValueWrapper> load() {
                return propertyModel.getObject().getPropertyValueWrappers();
            }
        };

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
        add(values);
    }
}

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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismPropertyPanel extends Panel {

    public PrismPropertyPanel(String id, final IModel<PropertyWrapper> model) {
        super(id);

        setOutputMarkupId(true);
        add(new AttributeAppender("class", new Model<String>("objectFormPanel"), " "));
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                PropertyWrapper property = model.getObject();
                ContainerWrapper container = property.getContainer();
                return container.isPropertyVisible(property);
            }
        });

        initLayout(model);
    }

    private void initLayout(IModel<PropertyWrapper> model) {
        add(new Label("label", new PropertyModel(model, "displayName")));

        ListView<ValueWrapper> values = new ListView<ValueWrapper>("values",
                new PropertyModel<List<ValueWrapper>>(model, "values")) {

            @Override
            protected void populateItem(ListItem<ValueWrapper> item) {
                item.add(new PrismValuePanel("value", item.getModel()));
            }
        };
        add(values);
    }
}

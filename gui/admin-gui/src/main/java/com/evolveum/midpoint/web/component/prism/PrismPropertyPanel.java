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

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
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

import java.util.List;

/**
 * @author lazyman
 */
public class PrismPropertyPanel extends Panel {

    public PrismPropertyPanel(String id, final IModel<PropertyWrapper> model, Form form) {
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

        initLayout(model, form);
    }

    private void initLayout(final IModel<PropertyWrapper> model, final Form form) {
        final IModel<String> label = createDisplayName(model);
        add(new Label("label", label));

        WebMarkupContainer required = new WebMarkupContainer("required");
        required.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                PropertyWrapper wrapper = model.getObject();
                PrismProperty property = wrapper.getItem();
                PrismPropertyDefinition def = property.getDefinition();

                if (SchemaConstantsGenerated.C_NAME.equals(def.getName())) {
                    //fix for "name as required" MID-789
                    return true;
                }

                return def.isMandatory();
            }
        });
        add(required);

        WebMarkupContainer hasOutbound = new WebMarkupContainer("hasOutbound");
        hasOutbound.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                PropertyWrapper wrapper = model.getObject();
                PrismProperty property = wrapper.getItem();
                PrismPropertyDefinition def = property.getDefinition();
                if (!(def instanceof RefinedAttributeDefinition)) {
                    return false;
                }

                RefinedAttributeDefinition refinedDef = (RefinedAttributeDefinition) def;
                return refinedDef.hasOutboundExpression();
            }
        });
        add(hasOutbound);

        ListView<ValueWrapper> values = new ListView<ValueWrapper>("values",
                new PropertyModel<List<ValueWrapper>>(model, "values")) {

            @Override
            protected void populateItem(final ListItem<ValueWrapper> item) {
                item.add(new PrismValuePanel("value", item.getModel(), label, form));
                item.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        return isVisibleValue(item.getModel());
                    }
                });
            }
        };
        add(values);
    }

    private IModel<String> createDisplayName(final IModel<PropertyWrapper> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                PropertyWrapper wrapper = model.getObject();
                String displayName = wrapper.getDisplayName();
                return getString(displayName, null, displayName);
            }
        };
    }

    private boolean isVisibleValue(IModel<ValueWrapper> model) {
        ValueWrapper value = model.getObject();
        return !ValueStatus.DELETED.equals(value.getStatus());
    }
}

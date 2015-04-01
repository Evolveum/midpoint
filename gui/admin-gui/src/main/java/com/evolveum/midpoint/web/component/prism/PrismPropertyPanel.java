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

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.List;

/**
 * @author lazyman
 */
public class PrismPropertyPanel extends Panel {

    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyPanel.class);
    private static final String ID_HAS_PENDING_MODIFICATION = "hasPendingModification";
    private static final String ID_HELP = "help";
    private static final String ID_LABEL_CONTAINER = "labelContainer";

    private PageBase pageBase;

    public PrismPropertyPanel(String id, final IModel<PropertyWrapper> model, Form form, PageBase pageBase) {
        super(id);
        this.pageBase = pageBase;

        setOutputMarkupId(true);
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                PropertyWrapper property = model.getObject();
                return property.isVisible();
            }

            @Override
            public boolean isEnabled() {
                return !model.getObject().isReadonly();
            }
        });

        initLayout(model, form);
    }

    private void initLayout(final IModel<PropertyWrapper> model, final Form form) {
        WebMarkupContainer labelContainer = new WebMarkupContainer(ID_LABEL_CONTAINER);
        labelContainer.setOutputMarkupId(true);
        add(labelContainer);

        final IModel<String> label = createDisplayName(model);
        labelContainer.add(new Label("label", label));

        final IModel<String> helpText = new LoadableModel<String>(false) {

            @Override
            protected String load() {
                return loadHelpText(model);
            }
        };
        Label help = new Label(ID_HELP);
        help.add(AttributeModifier.replace("title", helpText));
        help.add(new InfoTooltipBehavior());
        help.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return StringUtils.isNotEmpty(helpText.getObject());
            }
        });
        labelContainer.add(help);

        WebMarkupContainer required = new WebMarkupContainer("required");
        required.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                PropertyWrapper wrapper = model.getObject();
                PrismProperty property = wrapper.getItem();
                PrismPropertyDefinition def = property.getDefinition();

                if (ObjectType.F_NAME.equals(def.getName())) {
                    //fix for "name as required" MID-789
                    return true;
                }

                return def.isMandatory();
            }
        });
        labelContainer.add(required);

        WebMarkupContainer hasOutbound = new WebMarkupContainer("hasOutbound");
        hasOutbound.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasOutbound(model);
            }
        });
        labelContainer.add(hasOutbound);

        WebMarkupContainer hasPendingModification = new WebMarkupContainer(ID_HAS_PENDING_MODIFICATION);
        hasPendingModification.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasPendingModification(model);
            }
        });
        labelContainer.add(hasPendingModification);

        ListView<ValueWrapper> values = new ListView<ValueWrapper>("values",
                new PropertyModel<List<ValueWrapper>>(model, "values")) {

            @Override
            protected void populateItem(final ListItem<ValueWrapper> item) {
                PrismValuePanel panel = new PrismValuePanel("value", item.getModel(), label, form, getValueCssClass(), getInputCssClass(), pageBase);
                item.add(panel);
                item.add(AttributeModifier.append("class", createStyleClassModel(item.getModel())));

                item.add(new VisibleEnableBehaviour() {

                    @Override
                    public boolean isVisible() {
                        return isVisibleValue(item.getModel());
                    }
                });
            }
        };
        values.add(new AttributeModifier("class", getValuesClass()));
        values.setReuseItems(true);
        add(values);
    }

    protected String getInputCssClass(){
        return"col-xs-9";
    }

    protected String getValuesClass(){
        return "col-md-6";
    }

    protected String getValueCssClass(){
        return "row";
    }

    private String loadHelpText(IModel<PropertyWrapper> model) {
        PrismProperty property = model.getObject().getItem();
        PrismPropertyDefinition def = property.getDefinition();
        String doc = def.getHelp();
        if (StringUtils.isEmpty(doc)) {
            return null;
        }

        return new StringResourceModel(doc, null, doc).getString();
    }

    private IModel<String> createStyleClassModel(final IModel<ValueWrapper> value) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (getIndexOfValue(value.getObject()) > 0) {
                    return "col-md-offset-4 prism-value";
                }

                return null;
            }
        };
    }

    private int getIndexOfValue(ValueWrapper value) {
        PropertyWrapper property = value.getProperty();
        List<ValueWrapper> values = property.getValues();
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i).equals(value)) {
                return i;
            }
        }

        return -1;
    }

    private boolean hasOutbound(IModel<PropertyWrapper> model) {
        PropertyWrapper wrapper = model.getObject();
        PrismProperty property = wrapper.getItem();
        PrismPropertyDefinition def = property.getDefinition();
        if (!(def instanceof RefinedAttributeDefinition)) {
            return false;
        }

        RefinedAttributeDefinition refinedDef = (RefinedAttributeDefinition) def;
        return refinedDef.hasOutboundMapping();
    }

    private boolean hasPendingModification(IModel<PropertyWrapper> model) {
        PropertyWrapper propertyWrapper = model.getObject();
        ContainerWrapper containerWrapper = propertyWrapper.getContainer();
        ObjectWrapper objectWrapper = containerWrapper.getObject();

        PrismObject prismObject = objectWrapper.getObject();
        if (!ShadowType.class.isAssignableFrom(prismObject.getCompileTimeClass())) {
            return false;
        }

        PrismProperty objectChange = prismObject.findProperty(ShadowType.F_OBJECT_CHANGE);
        if (objectChange == null || objectChange.getValue() == null) {
            return false;
        }

        ItemPath path = propertyWrapper.getItem().getPath();
        ObjectDeltaType delta = (ObjectDeltaType) objectChange.getValue().getValue();
        try {
            for (ItemDeltaType itemDelta : delta.getItemDelta()) {
                ItemDelta iDelta = DeltaConvertor.createItemDelta(itemDelta, (Class<? extends Objectable>)
                        prismObject.getCompileTimeClass(), prismObject.getPrismContext());
                if (iDelta.getPath().equivalent(path)) {
                    return true;
                }
            }
        } catch (SchemaException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't check if property has pending modification", ex);
        }

        return false;
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

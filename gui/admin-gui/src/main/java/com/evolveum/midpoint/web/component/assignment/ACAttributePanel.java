/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.assignment;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 */
public class ACAttributePanel extends BasePanel<ACAttributeDto> {

    private static final String ID_ATTRIBUTE_LABEL = "attributeLabel";
    private static final String ID_VALUES = "values";
    private static final String ID_VALUE = "value";
    private static final String ID_REQUIRED = "required";
    private static final String ID_HAS_OUTBOUND = "hasOutbound";

    public ACAttributePanel(String id, IModel<ACAttributeDto> model, boolean ignoreMandatoryAttributes) {
        super(id, model);
        initLayout(ignoreMandatoryAttributes);
    }

    protected void initLayout(boolean ignoreMandatoryAttributes) {
        Label attributeLabel = new Label(ID_ATTRIBUTE_LABEL,
                new PropertyModel<>(getModel(), ACAttributeDto.F_NAME));
        add(attributeLabel);

        WebMarkupContainer required = new WebMarkupContainer(ID_REQUIRED);
        required.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ACAttributeDto dto = getModel().getObject();
                PrismPropertyDefinition<?> def = dto.getDefinition();

                return def.isMandatory();
            }
        });
        add(required);

        WebMarkupContainer hasOutbound = new WebMarkupContainer(ID_HAS_OUTBOUND);
        hasOutbound.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return hasOutbound();
            }
        });
        add(hasOutbound);

        ListView<ACValueConstructionDto> values = new ListView<ACValueConstructionDto>(ID_VALUES,
                new PropertyModel<>(getModel(), ACAttributeDto.F_VALUES)) {

            @Override
            protected void populateItem(ListItem<ACValueConstructionDto> listItem) {
                Form<?> form = findParent(Form.class);
                listItem.add(new ACAttributeValuePanel(ID_VALUE, listItem.getModel(), ignoreMandatoryAttributes, form));
            }
        };
        add(values);
    }

    private boolean hasOutbound() {
        ACAttributeDto dto = getModel().getObject();
        PrismPropertyDefinition<?> def = dto.getDefinition();
        if (!(def instanceof RefinedAttributeDefinition)) {
            return false;
        }

        RefinedAttributeDefinition<?> refinedDef = (RefinedAttributeDefinition<?>) def;
        return refinedDef.hasOutboundMapping();
    }
}

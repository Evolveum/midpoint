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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

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
        Label attributeLabel = new Label(ID_ATTRIBUTE_LABEL, new PropertyModel(getModel(), ACAttributeDto.F_NAME));
        add(attributeLabel);

        WebMarkupContainer required = new WebMarkupContainer(ID_REQUIRED);
        required.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ACAttributeDto dto = getModel().getObject();
                PrismPropertyDefinition def = dto.getDefinition();

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
                new PropertyModel<List<ACValueConstructionDto>>(getModel(), ACAttributeDto.F_VALUES)) {

            @Override
            protected void populateItem(ListItem<ACValueConstructionDto> listItem) {
                Form form = findParent(Form.class);
                listItem.add(new ACAttributeValuePanel(ID_VALUE, listItem.getModel(), ignoreMandatoryAttributes, form));
            }
        };
        add(values);
    }

    private boolean hasOutbound() {
        ACAttributeDto dto = getModel().getObject();
        PrismPropertyDefinition def = dto.getDefinition();
        if (!(def instanceof RefinedAttributeDefinition)) {
            return false;
        }

        RefinedAttributeDefinition refinedDef = (RefinedAttributeDefinition) def;
        return refinedDef.hasOutboundMapping();
    }
}

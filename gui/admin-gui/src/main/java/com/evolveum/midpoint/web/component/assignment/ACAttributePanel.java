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

package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.web.component.util.BasePanel;
import org.apache.wicket.markup.html.basic.Label;
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

    public ACAttributePanel(String id, IModel<ACAttributeDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        Label attributeLabel = new Label(ID_ATTRIBUTE_LABEL, new PropertyModel(getModel(), ACAttributeDto.F_NAME));
        add(attributeLabel);

        ListView<ACValueConstructionDto> values = new ListView<ACValueConstructionDto>(ID_VALUES,
                new PropertyModel<List<ACValueConstructionDto>>(getModel(), ACAttributeDto.F_VALUES)) {

            @Override
            protected void populateItem(ListItem<ACValueConstructionDto> listItem) {
                listItem.add(new ACAttributeValuePanel(ID_VALUE, listItem.getModel()));
            }
        };
        add(values);
    }
}

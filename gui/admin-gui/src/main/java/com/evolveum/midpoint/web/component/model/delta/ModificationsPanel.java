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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.model.delta;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class ModificationsPanel extends SimplePanel<DeltaDto> {

    private static final Trace LOGGER = TraceManager.getTrace(ModificationsPanel.class);

    private static final String ID_MODIFICATION = "modification";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_CHANGE_TYPE = "changeType";
    private static final String ID_VALUE = "value";

    public ModificationsPanel(String id, IModel<DeltaDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        add(new ListView<ModificationDto>(ID_MODIFICATION, new PropertyModel(getModel(), DeltaDto.F_MODIFICATIONS)) {
            @Override
            protected void populateItem(ListItem<ModificationDto> item) {
                item.add(new Label(ID_ATTRIBUTE, new PropertyModel(item.getModel(), ModificationDto.F_ATTRIBUTE)));
                item.add(new Label(ID_CHANGE_TYPE, new PropertyModel(item.getModel(), ModificationDto.F_CHANGE_TYPE)));
                if (item.getModelObject().getValue() instanceof ContainerValueDto) {
                    item.add(new ContainerValuePanel(ID_VALUE, new Model((ContainerValueDto) item.getModelObject().getValue())));
                } else {    // should be String
                    item.add(new Label(ID_VALUE, new PropertyModel(item.getModel(), ModificationDto.F_VALUE)));
                }
            }
        });
    }
}

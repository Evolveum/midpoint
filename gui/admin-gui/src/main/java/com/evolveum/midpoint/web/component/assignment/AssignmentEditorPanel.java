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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.web.component.util.BasePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAssignmentDtoType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.*;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<UserAssignmentDto> {

    private static final String ID_NAME = "name";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_ACTIVATION = "activation";

    public AssignmentEditorPanel(String id, IModel<UserAssignmentDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        Label name = new Label("name", new PropertyModel<Object>(getModel(), UserAssignmentDto.F_NAME));
        add(name);

        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), UserAssignmentDto.F_DESCRIPTION));
        add(description);

        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                UserAssignmentDto dto = getModel().getObject();
                return UserAssignmentDtoType.ACCOUNT_CONSTRUCTION.equals(dto.getType());
            }
        });
        add(attributes);

        ListView<ACAttributeDto> attribute = new ListView<ACAttributeDto>(ID_ATTRIBUTE,
                new LoadableModel<List<ACAttributeDto>>(false) {

                    @Override
                    protected List<ACAttributeDto> load() {
                        return loadAttributes();
                    }
                }) {

            @Override
            protected void populateItem(ListItem<ACAttributeDto> listItem) {
                ACAttributePanel acAttribute = new ACAttributePanel(ID_AC_ATTRIBUTE, listItem.getModel());
                acAttribute.setRenderBodyOnly(true);
                listItem.add(acAttribute);
            }
        };
        attributes.add(attribute);

        //todo extension and activation
    }

    private List<ACAttributeDto> loadAttributes() {
        List<ACAttributeDto> attributes = new ArrayList<ACAttributeDto>();
        try {
            UserAssignmentDto dto = getModel().getObject();

            AssignmentType assignment = dto.getAssignment();
            AccountConstructionType construction = assignment.getAccountConstruction();
            ResourceType resource = construction.getResource();


            RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource.asPrismObject(),
                    getPageBase().getPrismContext());
            PrismContainerDefinition definition = refinedSchema.getAccountDefinition(construction.getType());

            Collection<ItemDefinition> definitions = definition.getDefinitions();
            for (ItemDefinition attrDef : definitions) {
                if (!(attrDef instanceof PrismPropertyDefinition)) {
                    //log skipping or something...
                    continue;
                }
                attributes.add(new ACAttributeDto((PrismPropertyDefinition) attrDef, null, null));
            }
        } catch (Exception ex) {
            //todo error handling
            ex.printStackTrace();
        }

        Collections.sort(attributes, new Comparator<ACAttributeDto>() {

            @Override
            public int compare(ACAttributeDto a1, ACAttributeDto a2) {
                return String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName());
            }
        });

        return attributes;
    }
}

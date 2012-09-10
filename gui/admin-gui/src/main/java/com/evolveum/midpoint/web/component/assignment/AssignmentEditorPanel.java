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
import com.evolveum.midpoint.web.component.util.LoadableModel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class AssignmentEditorPanel extends BasePanel<AssignmentEditorDto> {

    private static final String ID_NAME = "name";
    private static final String ID_ATTRIBUTES = "attributes";
    private static final String ID_ATTRIBUTE = "attribute";
    private static final String ID_AC_ATTRIBUTE = "acAttribute";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_EXTENSION = "extension";
    private static final String ID_ACTIVATION = "activation";

    public AssignmentEditorPanel(String id, IModel<AssignmentEditorDto> model) {
        super(id, model);
    }

    protected void initLayout() {
        Label name = new Label("name", new PropertyModel<Object>(getModel(), AssignmentEditorDto.F_NAME));
        add(name);

        TextField description = new TextField(ID_DESCRIPTION,
                new PropertyModel(getModel(), AssignmentEditorDto.F_DESCRIPTION));
        add(description);

        WebMarkupContainer attributes = new WebMarkupContainer(ID_ATTRIBUTES);
        attributes.setOutputMarkupId(true);
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
        //todo implement
        return new ArrayList<ACAttributeDto>();
    }
}

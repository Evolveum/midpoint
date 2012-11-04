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

package com.evolveum.midpoint.web.component.wizard.resource.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.wizard.resource.dto.ObjectClassDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ObjectClassPanel extends SimplePanel<ObjectClassDto> {

    private static final String ID_HEADER = "header";
    private static final String ID_VISIBLE = "visible";
    private static final String ID_VISIBLE_LABEL = "visibleLabel";
    private static final String ID_NAME = "name";
    private static final String ID_BODY = "body";
    private static final String ID_MAIN = "main";

    public ObjectClassPanel(String id, IModel<ObjectClassDto> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        add(header);

        AjaxLink visible = new AjaxLink(ID_VISIBLE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                visibleTogglePerformed(target);
            }
        };
        header.add(visible);

        Label visibleLabel = new Label(ID_VISIBLE_LABEL, createVisibleLabel());
        visibleLabel.setOutputMarkupId(true);
        visible.add(visibleLabel);

        Label name = new Label(ID_NAME, new PropertyModel(getModel(), ObjectClassDto.F_NAME));
        header.add(name);

        WebMarkupContainer body = new WebMarkupContainer(ID_BODY);
        body.setOutputMarkupId(true);
        add(body);

        //attributes list, now very simple
        Label main = new Label(ID_MAIN, new PropertyModel(getModel(), ObjectClassDto.F_ATTRIBUTES));
        main.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                ObjectClassDto dto = ObjectClassPanel.this.getModel().getObject();

                return dto.isAttributesVisible();
            }
        });
        body.add(main);
    }

    private IModel<String> createVisibleLabel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                ObjectClassDto dto = ObjectClassPanel.this.getModel().getObject();
                if (dto.isAttributesVisible()) {
                    return "-";
                }

                return "+";
            }
        };
    }

    private void visibleTogglePerformed(AjaxRequestTarget target) {
        ObjectClassDto dto = getModel().getObject();
        dto.setAttributesVisible(!dto.isAttributesVisible());

        target.add(get(ID_BODY));
        target.add(get(ID_HEADER + ":" + ID_VISIBLE + ":" + ID_VISIBLE_LABEL));
    }
}

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

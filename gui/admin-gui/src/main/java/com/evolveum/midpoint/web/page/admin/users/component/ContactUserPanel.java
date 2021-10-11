/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.users.dto.ContactUserDto;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class ContactUserPanel extends SimplePanel<ContactUserDto> {

    public ContactUserPanel(String id, IModel<ContactUserDto> model) {
        super(id, model);

        //move to html later
        add(AttributeModifier.append("class", "media"));
    }

    @Override
    protected void initLayout() {
        add(new Label("name", new PropertyModel(getModel(), "name")));
    }
}

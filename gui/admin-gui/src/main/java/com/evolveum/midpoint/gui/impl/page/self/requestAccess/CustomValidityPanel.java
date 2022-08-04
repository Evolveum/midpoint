/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.util.DateValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.Date;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CustomValidityPanel extends BasePanel<CustomValidity> {

    private static final long serialVersionUID = 1L;

    private static final String ID_FROM = "from";
    private static final String ID_TO = "to";

    public CustomValidityPanel(String id, IModel<CustomValidity> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form form = findParent(Form.class);
        form.add(new DateValidator(getFrom(), getTo()));
    }

    public DateInput getFrom() {
        return (DateInput) get(ID_FROM);
    }

    public DateInput getTo() {
        return (DateInput) get(ID_TO);
    }

    private void initLayout() {
        DateInput from = new DateInput(ID_FROM, new PropertyModel<>(getModel(), "from"));
        from.setOutputMarkupId(true);
        add(from);

        DateInput to = new DateInput(ID_TO, new PropertyModel<>(getModel(), "to"));
        to.setOutputMarkupId(true);
        add(to);
    }
}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.impl.component.input.DateTimePickerPanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.util.DateValidator;

import java.util.Date;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CustomValidityPanel extends BasePanel<CustomValidity> {

    private static final long serialVersionUID = 1L;

    private static final String ID_FROM = "from";
    private static final String ID_TO = "to";
    private final static String INVALID_FIELD_CLASS = "is-invalid";

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

    public TextField<Date> getFrom() {
        return ((DateTimePickerPanel)get(ID_FROM)).getBaseFormComponent();
    }

    public TextField<Date> getTo() {
        return ((DateTimePickerPanel)get(ID_TO)).getBaseFormComponent();
    }

    private void initLayout() {
        DateTimePickerPanel from = DateTimePickerPanel.createByDateModel(ID_FROM, new PropertyModel<>(getModel(), "from"));
        from.add(new VisibleBehaviour(this::isFromFieldVisible));
        from.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("ActivationType.validFrom")));
        from.getBaseFormComponent().setLabel(createStringResource("ActivationType.validFrom"));
        from.getBaseFormComponent().add(AttributeModifier.append("class", () -> {
            Form<?> form = findParent(Form.class);
            return form.hasErrorMessage() ? INVALID_FIELD_CLASS : "";
        }));
        from.setOutputMarkupId(true);
        add(from);

        DateTimePickerPanel to = DateTimePickerPanel.createByDateModel(ID_TO, new PropertyModel<>(getModel(), "to"));
        to.add(new VisibleBehaviour(this::isToFieldVisible));
        to.getBaseFormComponent().add(
                AttributeAppender.append("aria-label", createStringResource("ActivationType.validTo")));
        to.getBaseFormComponent().setLabel(createStringResource("ActivationType.validTo"));
        to.getBaseFormComponent().add(AttributeModifier.append("class", () -> {
            Form<?> form = findParent(Form.class);
            return form.hasErrorMessage() ? INVALID_FIELD_CLASS : "";
        }));
        to.setOutputMarkupId(true);
        add(to);
    }

    protected boolean isFromFieldVisible() {
        return true;
    }

    protected boolean isToFieldVisible() {
        return true;
    }

    protected void onError() {}
}

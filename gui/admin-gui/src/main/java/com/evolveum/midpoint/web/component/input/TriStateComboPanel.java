/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

/**
 * @author mserbak
 */
public class TriStateComboPanel extends InputPanel {

    private static final String ID_COMBO = "combo";

    public TriStateComboPanel(String id, final IModel<Boolean> model) {
        super(id);

        DropDownChoice<Boolean> combo = WebComponentUtil.createTriStateCombo(ID_COMBO, model);
        add(combo);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_COMBO);
    }
}

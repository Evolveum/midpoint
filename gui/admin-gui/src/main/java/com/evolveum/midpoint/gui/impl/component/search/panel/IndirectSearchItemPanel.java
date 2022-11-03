/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.IndirectSearchItemWrapper;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

import org.apache.wicket.model.PropertyModel;

public class IndirectSearchItemPanel extends SingleSearchItemPanel<IndirectSearchItemWrapper> {

    public IndirectSearchItemPanel(String id, IModel<IndirectSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        List<Boolean> choices = new ArrayList<>();
        choices.add(Boolean.TRUE);
        choices.add(Boolean.FALSE);
        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id,
                new PropertyModel<>(getModel(), IndirectSearchItemWrapper.F_VALUE),
                Model.ofList(choices),
                new ChoiceRenderer<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(Boolean val) {
                if (val) {
                    return getPageBase().createStringResource("Boolean.TRUE").getString();
                }
                return getPageBase().createStringResource("Boolean.FALSE").getString();
            }
        }, false);

        return inputPanel;
    }

}

/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest;

import java.util.List;

import org.apache.commons.text.WordUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.smart.api.conndev.ConnDevScriptFormat;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

/**
 * Script format selector for the script steps of the connector generator wizard.
 */
public class ScriptFormatSelectPanel extends BasePanel<ConnDevScriptFormat> {

    private static final String ID_SELECT = "select";

    public ScriptFormatSelectPanel(String id, IModel<ConnDevScriptFormat> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        add(createSelect());
    }

    private DropDownChoicePanel<ConnDevScriptFormat> createSelect() {
        IModel<List<ConnDevScriptFormat>> choices = Model.ofList(List.of(ConnDevScriptFormat.values()));

        DropDownChoicePanel<ConnDevScriptFormat> select = new DropDownChoicePanel<>(
                ID_SELECT, getModel(), choices,
                new ChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(ConnDevScriptFormat format) {
                        return format == ConnDevScriptFormat.YAML ? "YAML" : WordUtils.capitalizeFully(format.name());
                    }
                }, false);
        select.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onFormatChanged(target, select.getBaseFormComponent().getConvertedInput());
            }
        });
        return select;
    }

    /**
     * Called when the user selects a script format.
     *
     * @param target ajax request target
     * @param format selected script format
     */
    protected void onFormatChanged(AjaxRequestTarget target, ConnDevScriptFormat format) {
    }
}

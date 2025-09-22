/*
 * ~ Copyright (c) 2025 Evolveum
 * ~
 * ~ This work is dual-licensed under the Apache License 2.0
 * ~ and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application.component.catalog.marketplace;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class SearchBoxPanel extends BasePanel<String> {
    private final IModel<String> placeholderModel;

    public SearchBoxPanel(String id, IModel<String> placeholderModel) {
        super(id, Model.of(""));
        this.placeholderModel = placeholderModel;
    }

    public SearchBoxPanel(String id) {
        this(id, null);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Form<?> form = new Form<>("form");
        add(form);

        TextField<String> input = new TextField<>("input", getModel());
        if (placeholderModel != null) {
            input.add(new AttributeModifier("placeholder", placeholderModel));
        }
        input.add(new AjaxFormComponentUpdatingBehavior("keyup") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onSearch(target, getModel().getObject());
            }
        });
        form.add(input);
    }

    /**
     * Callback method called when the text changes in the searchbox.
     */
    protected abstract void onSearch(AjaxRequestTarget target, String query);
}

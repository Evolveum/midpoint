/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;

import java.util.Collection;

public class Select2MultiChoicePanel<T> extends InputPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_SELECT = "select";

    private final IModel<Collection<T>> model;
    private final ChoiceProvider<T> provider;
    private final int minimumInputLength;

    public Select2MultiChoicePanel(String id, IModel<Collection<T>> model, ChoiceProvider<T> provider) {
        this(id, model, provider, 2);
    }

    public Select2MultiChoicePanel(
            String id,
            IModel<Collection<T>> model,
            ChoiceProvider<T> provider,
            int minimumInputLength) {
        super(id);
        this.model = model;
        this.provider = provider;
        this.minimumInputLength = minimumInputLength;
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript("MidPointTheme.initSelect2MultiChoice(" + getMarkupId() + ");"));
    }

    private void initLayout() {
        setOutputMarkupId(true);
        Select2MultiChoice<T> multiselect = new Select2MultiChoice<>(ID_SELECT, model, provider);
        multiselect.getSettings()
                .setMinimumInputLength(minimumInputLength);
        multiselect.add(new EmptyOnChangeAjaxFormUpdatingBehavior());


        IModel<String> ariaLabelModel = getAriaLabelModel();
        if (ariaLabelModel != null) {
            multiselect.add(AttributeAppender.append("aria-label", ariaLabelModel));
        }

        if (isInColumn()) {
            multiselect.add(AttributeAppender.append("class", "form-control-sm"));
            multiselect.add(AttributeAppender.append("style", "width: 101%;"));
        }

        add(multiselect);
    }

    protected IModel<String> getAriaLabelModel() {
        return null;
    }

    protected boolean isInColumn() {
        return false;
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return (Select2MultiChoice) get(ID_SELECT);
    }
}

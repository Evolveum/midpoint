/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import java.util.Collection;

import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;

import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class AnalysisAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";

    protected IModel<Collection<PrismPropertyValueWrapper<ItemPathType>>> model;

    public AnalysisAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<Collection<PrismPropertyValueWrapper<ItemPathType>>> model) {
        super(id);
        this.model = model;
    }


    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        initSelectionFragment();
    }

    private void initSelectionFragment() {
        ChoiceProvider<PrismPropertyValueWrapper<ItemPathType>> choiceProvider = buildChoiceProvider();

        Select2MultiChoicePanel<PrismPropertyValueWrapper<ItemPathType>> multiselect = new Select2MultiChoicePanel<>(
                ID_MULTISELECT,
                getModel(),
                choiceProvider,
                0);
        add(multiselect);

    }

    @NotNull
    private ChoiceProvider<PrismPropertyValueWrapper<ItemPathType>> buildChoiceProvider() {

        return new AnalysisAttributeSelectionProvider(getModel().getObject().iterator().next().getParent(), getPageBase());
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC().getBaseFormComponent();
    }

    private Select2MultiChoicePanel<?> getFormC() {
        return (Select2MultiChoicePanel<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public IModel<Collection<PrismPropertyValueWrapper<ItemPathType>>> getModel() {
        return model;
    }
}

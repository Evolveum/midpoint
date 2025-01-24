/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import java.util.Collection;

import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Select2MultiChoice;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.input.Select2MultiChoicePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class AnalysisAttributeSelectorPanel extends InputPanel {
    private static final String ID_MULTISELECT = "multiselect";

    protected IModel<PrismPropertyWrapper<ItemPathType>> model;

    public AnalysisAttributeSelectorPanel(@NotNull String id,
            @NotNull IModel<PrismPropertyWrapper<ItemPathType>> model) {
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
        ChoiceProvider<ItemPathType> choiceProvider = buildChoiceProvider();

        Select2MultiChoice<ItemPathType> multiselect = new Select2MultiChoice<>(ID_MULTISELECT,
                initSelectedModel(),
                choiceProvider);

        multiselect.getSettings()
                .setMinimumInputLength(0);
        multiselect.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateModelWithRules(multiselect.getModel().getObject(), target);
                target.add(AnalysisAttributeSelectorPanel.this);
            }
        });
        multiselect.add(new EnableBehaviour(this::isEditable));
        add(multiselect);
    }

    private LoadableModel<Collection<ItemPathType>> initSelectedModel() {
        return new LoadableModel<>(true) {

            @Override
            protected Collection<ItemPathType> load() {

                return model.getObject()
                        .getValues()
                        .stream()
                        .filter(v -> v.getStatus() != ValueStatus.DELETED)
                        .map(v -> v.getRealValue())
                        .toList();
            }
        };
    }

    private void updateModelWithRules(Collection<ItemPathType> selectedRules, AjaxRequestTarget target) {
        PrismPropertyWrapper<ItemPathType> propertyWrapper = model.getObject();

        // add new values
        Collection<PrismPropertyValueWrapper<ItemPathType>> values = propertyWrapper.getValues();
        for (ItemPathType selectedRule : selectedRules) {
            if (!isAlreadyPresent(selectedRule, values)) {
                createNewValueWrapper(selectedRule, target);
            }
        }

        // mark values to be deleted. The delta is later, when wrappers are processed.
        for (PrismPropertyValueWrapper<ItemPathType> value : values) {
            if (!shouldValueExist(value, selectedRules)) {
                value.setStatus(ValueStatus.DELETED);
            }
        }
    }

    private boolean isAlreadyPresent(ItemPathType selectedRules, Collection<PrismPropertyValueWrapper<ItemPathType>> values) {
        return values.stream().anyMatch(r -> r.getRealValue().equivalent(selectedRules));
    }

    private boolean shouldValueExist(PrismPropertyValueWrapper<ItemPathType> value, Collection<ItemPathType> selectedRules) {
        return selectedRules.stream().anyMatch(r -> r.equivalent(value.getRealValue()));
    }

    private PrismPropertyValueWrapper<ItemPathType> createNewValueWrapper(ItemPathType path, AjaxRequestTarget target) {
        PrismPropertyWrapper<ItemPathType> propertyWrapper = model.getObject();
        return WebPrismUtil.createNewValueWrapper(propertyWrapper, PrismContext.get().itemFactory().createPropertyValue(path), getPageBase(), target);
    }


    @NotNull
    private ChoiceProvider<ItemPathType> buildChoiceProvider() {

        return new AnalysisAttributeSelectionProvider();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return getFormC().getBaseFormComponent();
    }

    private Select2MultiChoicePanel<?> getFormC() {
        return (Select2MultiChoicePanel<?>) get(getPageBase().createComponentPath(ID_MULTISELECT));
    }

    public boolean isEditable(){
        return true;
    }

}

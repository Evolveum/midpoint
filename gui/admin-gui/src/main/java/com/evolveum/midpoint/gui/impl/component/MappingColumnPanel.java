/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

public class MappingColumnPanel extends BasePanel<PrismContainerValueWrapper<MappingType>> {

    private static final String ID_MAPPING_ENABLED = "mappingEnabled";
    private static final String ID_MAPPING = "mapping";

    public MappingColumnPanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ReadOnlyModel<String> mappingDescription = new ReadOnlyModel<>(() -> {

            if (getModelObject() == null) {
                return null;
            }

            MappingType mappingType = getModelObject().getRealValue();
            if (mappingType == null) {
                return null;
            }

            List<VariableBindingDefinitionType> sources = mappingType.getSource();
            String sourceString = "";
            if (!sources.isEmpty()) {
                sourceString += "From: ";
            }
            for (VariableBindingDefinitionType s : sources) {
                if (s == null) {
                    continue;
                }
                sourceString += s.getPath().toString() + ", ";
            }
            String strength = "";
            if (mappingType.getStrength() != null) {
                strength = mappingType.getStrength().toString();
            }

            String target = "";
            VariableBindingDefinitionType targetv = mappingType.getTarget();
            if (targetv != null) {
                target += "To: " + targetv.getPath().toString();
            }

            if (target.isBlank()) {
                return sourceString + "(" + strength + ")";
            }

            return target + "(" + strength + ")";
        });

        TriStateComboPanel dropDownChoicePanel = new TriStateComboPanel(ID_MAPPING_ENABLED, Model.of(true));
        add(dropDownChoicePanel);

        Label label = new Label(ID_MAPPING, mappingDescription);
        add(label);

    }
}

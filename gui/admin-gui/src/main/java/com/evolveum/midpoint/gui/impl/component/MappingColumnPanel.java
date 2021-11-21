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
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.input.TriStateComboPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.List;

public class MappingColumnPanel extends BasePanel<PrismContainerWrapper<MappingType>> {

    private static final String ID_MAPPING = "mapping";
    private static final String ID_MAPPINGS = "mappings";

    public MappingColumnPanel(String id, IModel<PrismContainerWrapper<MappingType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        ListView<PrismContainerValueWrapper<MappingType>> mappings = new ListView<>(ID_MAPPINGS, new PropertyModel<>(getModel(), "values")) {

            @Override
            protected void populateItem(ListItem<PrismContainerValueWrapper<MappingType>> item) {
                Label label = new Label(ID_MAPPING, WebComponentUtil.createMappingDescription(item.getModel()));
                label.add(AttributeAppender.append("class", createEnabledDisabledStyles(item.getModelObject())));
                item.add(label);
            }
        };
        add(mappings);
    }

    private String createEnabledDisabledStyles(PrismContainerValueWrapper<MappingType> mapping) {
        if (mapping == null) {
            return "";
        }

        MappingType mappingType = mapping.getRealValue();
        if (mappingType == null) {
            return "";
        }

        return BooleanUtils.isFalse(mappingType.isEnabled()) ? "mapping-disabled" : "";
    }


}

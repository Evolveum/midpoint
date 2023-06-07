/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.SourceMappingProvider;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SourceOrTargetOfMappingPanelFactory extends VariableBindingDefinitionTypePanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())
                && (wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_TARGET))
                || wrapper.getPath().namedSegmentsOnly().equivalent(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                ResourceAttributeDefinitionType.F_OUTBOUND,
                MappingType.F_SOURCE)));
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {

        IModel<String> valueModel = new IModel<>() {
            @Override
            public String getObject() {
                VariableBindingDefinitionType value = panelCtx.getRealValueModel().getObject();
                return GuiDisplayNameUtil.getDisplayName(value);
            }

            @Override
            public void setObject(String object) {
                if (StringUtils.isBlank(object)) {
                    panelCtx.getRealValueModel().setObject(null);
                    return;
                }
                VariableBindingDefinitionType def = new VariableBindingDefinitionType()
                        .path(PrismContext.get().itemPathParser().asItemPathType(object));
                panelCtx.getRealValueModel().setObject(def);
            }
        };

        AutoCompleteTextPanel<String> panel = new AutoCompleteTextPanel<>(
                panelCtx.getComponentId(), valueModel, String.class, true) {
            @Override
            public Iterator<String> getIterator(String input) {
                SourceMappingProvider provider = new SourceMappingProvider(panelCtx.getItemWrapperModel());
                return provider.collectAvailableDefinitions(input).iterator();
            }
        };
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        return panel;
    }
}

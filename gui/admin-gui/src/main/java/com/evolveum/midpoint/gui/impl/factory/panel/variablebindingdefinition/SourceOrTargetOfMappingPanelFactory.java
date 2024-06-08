/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;
import org.wicketstuff.select2.ChoiceProvider;

import java.io.Serializable;
import java.util.*;

@Component
public class SourceOrTargetOfMappingPanelFactory extends VariableBindingDefinitionTypePanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SourceOrTargetOfMappingPanelFactory.class);

    protected static final List<ItemPath> MATCHED_PATHS = List.of(
            ResourceObjectTypeDefinitionType.F_ATTRIBUTE,

            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_ACTIVATION,
                    ResourceActivationDefinitionType.F_ADMINISTRATIVE_STATUS),
            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_ACTIVATION,
                    ResourceActivationDefinitionType.F_EXISTENCE),
            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_ACTIVATION,
                    ResourceActivationDefinitionType.F_VALID_TO),
            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_ACTIVATION,
                    ResourceActivationDefinitionType.F_VALID_FROM),
            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_ACTIVATION,
                    ResourceActivationDefinitionType.F_LOCKOUT_STATUS),

            ItemPath.create(
                    ResourceObjectTypeDefinitionType.F_CREDENTIALS,
                    ResourceCredentialsDefinitionType.F_PASSWORD)

    );

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        LOGGER.trace("Start of match for SourceOrTargetOfMappingPanelFactory, wrapper: " + wrapper + ", value: " + valueWrapper);
        boolean match = QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())
                && getMatchedPaths().stream().anyMatch(path -> {
            if (createTargetPath(path).equivalent(wrapper.getPath().namedSegmentsOnly())) {
                LOGGER.trace("Matches for target path: " + path);
                return true;
            }
            if (createSourcePath(path).equivalent(wrapper.getPath().namedSegmentsOnly())) {
                LOGGER.trace("Matches for source path: " + path);
                return true;
            }
            LOGGER.trace("Not found match for SourceOrTargetOfMappingPanelFactory for path: " + path);
            return false;
        });
        LOGGER.trace("Result of match for SourceOrTargetOfMappingPanelFactory is " + match);
        return match;
    }

    protected Collection<ItemPath> getMatchedPaths() {
        return MATCHED_PATHS;
    }

    protected ItemPath createTargetPath(ItemPath containerPath) {
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                containerPath,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_TARGET);
    }

    protected ItemPath createSourcePath(ItemPath containerPath) {
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                containerPath,
                ResourceAttributeDefinitionType.F_OUTBOUND,
                InboundMappingType.F_SOURCE);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<VariableBindingDefinitionType> panelCtx) {

        IModel<String> valueModel = new IModel<>() {
            @Override
            public String getObject() {
                VariableBindingDefinitionType value = panelCtx.getRealValueModel().getObject();
                return GuiDisplayNameUtil.getDisplayName(value, stripVariableSegment());
            }

            @Override
            public void setObject(String object) {
                if (StringUtils.isBlank(object)) {
                    panelCtx.getRealValueModel().setObject(null);
                    return;
                }
                UniformItemPath path = ItemPathHolder.parseFromString(
                        object,
                        PrismContext.get().getSchemaRegistry().getNamespacePrefixMapper().getNamespacesDeclaredByDefault());
                VariableBindingDefinitionType def = new VariableBindingDefinitionType()
                        .path(new ItemPathType(path));
                panelCtx.getRealValueModel().setObject(def);
            }
        };

        AutoCompleteTextPanel<String> panel = new AutoCompleteTextPanel<>(
                panelCtx.getComponentId(), valueModel, String.class, true) {
            @Override
            public Iterator<String> getIterator(String input) {
                return getAvailableVariables(input, panelCtx.getItemWrapperModel(), panelCtx.getPageBase());
            }
        };
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        return panel;
    }

    protected boolean stripVariableSegment() {
        return true;
    }

    protected Iterator<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        return new FocusDefinitionsMappingProvider(itemWrapperModel).collectAvailableDefinitions(input).iterator();
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}

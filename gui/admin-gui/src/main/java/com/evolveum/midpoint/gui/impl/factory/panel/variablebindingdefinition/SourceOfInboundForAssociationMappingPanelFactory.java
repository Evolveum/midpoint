/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.gui.impl.util.AssociationChildWrapperUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class SourceOfInboundForAssociationMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())
                && ItemPath.create(
                    SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION,
                    AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF,
                    AttributeInboundMappingsDefinitionType.F_MAPPING,
                    InboundMappingType.F_SOURCE)
                .equivalent(wrapper.getPath().namedSegmentsOnly());
    }

    @Override
    protected List<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        PrismContainerValueWrapper<MappingType> mapping = itemWrapperModel.getObject().getParentContainerValue(InboundMappingType.class);
        if (mapping == null) {
            return Collections.emptyList();
        }

        ShadowAssociationDefinition assocDef = AssociationChildWrapperUtil.getShadowAssociationDefinition(itemWrapperModel.getObject().getParent(), pageBase);
        if(assocDef == null) {
            return Collections.emptyList();
        }

        List<String> toSelect = new ArrayList<>();
        if (!assocDef.isComplex()) {
            return toSelect;
        }

        assocDef.getAssociationDataObjectDefinition().getSimpleAttributeDefinitions()
                .forEach(simpleAttr -> {
                    QName name = PrismContext.get().getSchemaRegistry().getNamespacePrefixMapper()
                            .setQNamePrefix(new QName(simpleAttr.getItemName().getNamespaceURI(), simpleAttr.getItemName().getLocalPart()));
                    toSelect.add("$shadow/attributes/" + name.getPrefix() + ":" + name.getLocalPart());
                });

        if (StringUtils.isNotBlank(input)) {
            return toSelect
                    .stream()
                    .filter(choice -> choice.contains(input))
                    .sorted()
                    .toList();
        }
        return toSelect.stream().sorted().toList();
    }

    @Override
    protected boolean stripVariableSegment() {
        return false;
    }
}

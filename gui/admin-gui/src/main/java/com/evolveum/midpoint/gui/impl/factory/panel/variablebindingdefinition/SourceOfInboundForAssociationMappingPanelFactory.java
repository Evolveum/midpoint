/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Component
public class SourceOfInboundForAssociationMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        return QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())
                && ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                ShadowAssociationDefinitionType.F_OBJECT_REF,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_SOURCE).equivalent(wrapper.getPath().namedSegmentsOnly());
    }

    @Override
    protected Iterator<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        PrismContainerValueWrapper<MappingType> mapping = itemWrapperModel.getObject().getParentContainerValue(InboundMappingType.class);
        if (mapping == null) {
            return Collections.emptyIterator();
        }

        ShadowReferenceAttributeDefinition shadowRefAttrParent = AssociationChildWrapperUtil.getShadowReferenceAttribute(itemWrapperModel.getObject().getParent(), pageBase);
        if(shadowRefAttrParent == null) {
            return Collections.emptyIterator();
        }

        List<String> toSelect = new ArrayList<>();
        shadowRefAttrParent.getRepresentativeTargetObjectDefinition().getSimpleAttributeDefinitions()
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
                    .toList()
                    .iterator();
        }
        return toSelect.stream().sorted().toList().iterator();
    }

    @Override
    protected boolean stripVariableSegment() {
        return false;
    }
}
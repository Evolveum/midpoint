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
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Component
public class SourceOfInboundMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SourceOfInboundMappingPanelFactory.class);

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        boolean match = QNameUtil.match(VariableBindingDefinitionType.COMPLEX_TYPE, wrapper.getTypeName())
                && getMatchedPaths().stream().anyMatch(path -> createSourcePath(path).equivalent(wrapper.getPath().namedSegmentsOnly()));
        return match;
    }

    @Override
    protected ItemPath createSourcePath(ItemPath containerPath) {
        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_OBJECT_TYPE,
                containerPath,
                ResourceAttributeDefinitionType.F_INBOUND,
                InboundMappingType.F_SOURCE);
    }

    @Override
    protected List<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {

        List<String> toSelect = new ArrayList<>();
        toSelect.addAll(super.getAvailableVariables(input, itemWrapperModel, pageBase));

        PrismPropertyWrapper<VariableBindingDefinitionType> itemWrapper = itemWrapperModel.getObject();

        PrismContainerValueWrapper<ResourceObjectTypeDefinitionType> objectType = itemWrapper.getParentContainerValue(ResourceObjectTypeDefinitionType.class);

        if (objectType == null || objectType.getRealValue() == null) {
            return toSelect;
        }

        ResourceSchema schema = null;
        try {
            schema = ResourceSchemaFactory.getCompleteSchema(
                    (ResourceType) itemWrapper.findObjectWrapper().getObjectOld().asObjectable());
        } catch (Exception e) {
            LOGGER.debug("Couldn't get complete resource schema", e);
        }

        if (schema == null) {
            schema = ResourceDetailsModel.getResourceSchema(itemWrapper.findObjectWrapper(), pageBase);
        }

        if (schema == null) {
            return toSelect;
        }

        ResourceObjectTypeDefinitionType objectTypeBean = objectType.getRealValue();

        WebPrismUtil.searchAttributeDefinitions(schema, objectTypeBean).stream()
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

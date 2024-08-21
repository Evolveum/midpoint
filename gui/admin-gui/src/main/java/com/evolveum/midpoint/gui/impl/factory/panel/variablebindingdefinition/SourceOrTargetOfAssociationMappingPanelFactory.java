/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.variablebindingdefinition;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.FocusDefinitionsMappingProvider;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Component
public class SourceOrTargetOfAssociationMappingPanelFactory extends SourceOrTargetOfMappingPanelFactory implements Serializable {

    protected ItemPath createTargetPath(ItemPath containerPath) {
        return ItemPath.create(
//                ResourceType.F_SCHEMA_HANDLING,
//                SchemaHandlingType.F_ASSOCIATION_TYPE,
//                ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                containerPath,
                AttributeInboundMappingsDefinitionType.F_MAPPING,
                MappingType.F_TARGET);
    }

    protected ItemPath createSourcePath(ItemPath containerPath) {
        return ItemPath.create(
//                ResourceType.F_SCHEMA_HANDLING,
//                SchemaHandlingType.F_ASSOCIATION_TYPE,
//                ShadowAssociationTypeDefinitionType.F_SUBJECT,
//                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                containerPath,
                AttributeInboundMappingsDefinitionType.F_MAPPING,
                MappingType.F_SOURCE);
    }

    protected Collection<ItemPath> getMatchedPaths() {
        List<ItemPath> list = new ArrayList<>();
        list.add(ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION,
                AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE));
        list.add(ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_SYNCHRONIZATION,
                AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF));
        list.add(ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION,
                AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE));
        list.add(ItemPath.create(SchemaConstantsGenerated.C_ASSOCIATION_CONSTRUCTION,
                AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF));
        return list;
    }

    @Override
    protected Iterator<String> getAvailableVariables(String input, IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> itemWrapperModel, PageBase pageBase) {
        FocusDefinitionsMappingProvider provider = new FocusDefinitionsMappingProvider(itemWrapperModel) {
            @Override
            protected PrismContainerDefinition<? extends Containerable> getFocusTypeDefinition(ResourceObjectTypeDefinitionType resourceObjectType) {
                return PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);
            }
        };
        List<String> values = new ArrayList<>(provider.collectAvailableDefinitions(input));
        values.removeIf(path -> path.startsWith(AssignmentType.F_METADATA.getLocalPart() + "/")
                || path.startsWith(AssignmentType.F_CONDITION.getLocalPart() + "/"));
        return values.iterator();
    }
}

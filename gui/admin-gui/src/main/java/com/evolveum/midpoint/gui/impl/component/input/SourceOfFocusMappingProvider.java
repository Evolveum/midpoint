/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

public class SourceOfFocusMappingProvider extends FocusDefinitionsMappingProvider {

    private static final long serialVersionUID = 1L;

    public <IW extends ItemWrapper> SourceOfFocusMappingProvider(IModel<PrismPropertyWrapper<VariableBindingDefinitionType>> rowModel) {
        super(rowModel);
    }

    @Override
    public List<String> collectAvailableDefinitions(String input) {

        List<String> availableDefinition = new ArrayList<>();

        QName focusType = AssignmentHolderType.COMPLEX_TYPE;
        PrismContainerValueWrapper<AssignmentType> assignmentWrapper =
                getRowModel().getObject().getParentContainerValue(AssignmentType.class);
        if (assignmentWrapper != null
                && assignmentWrapper.getRealValue() != null
                && assignmentWrapper.getRealValue().getFocusType() != null) {
            focusType = assignmentWrapper.getRealValue().getFocusType();
        }

        PrismContainerDefinition<? extends Containerable> focusDef =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(focusType);

        availableDefinition.addAll(collectAvailableDefinitions(input, focusDef));

        PrismContainerDefinition<AssignmentType> assignmentDef =
                PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssignmentType.class);

        List<String> assignmentAvailableDefs = new ArrayList<>();
        assignmentAvailableDefs.addAll(collectAvailableDefinitions(input, assignmentDef));
        assignmentAvailableDefs.removeIf(path -> path.startsWith(AssignmentType.F_CONDITION.getLocalPart() + "/"));

        availableDefinition.addAll(assignmentAvailableDefs.stream().map(value -> "$" + ExpressionConstants.VAR_ASSIGNMENT + "/" + value).toList());
        availableDefinition.addAll(assignmentAvailableDefs.stream().map(value -> "$" + ExpressionConstants.VAR_IMMEDIATE_ASSIGNMENT + "/" + value).toList());
        availableDefinition.addAll(assignmentAvailableDefs.stream().map(value -> "$" + ExpressionConstants.VAR_THIS_ASSIGNMENT + "/" + value).toList());
        availableDefinition.addAll(assignmentAvailableDefs.stream().map(value -> "$" + ExpressionConstants.VAR_FOCUS_ASSIGNMENT + "/" + value).toList());

        PrismObjectWrapper<ObjectType> objectWrapper = getRowModel().getObject().findObjectWrapper();
        if (objectWrapper != null) {
            PrismContainerDefinition<ObjectType> sourceDef = objectWrapper.getItem().getDefinition();
            List<String> sourceAvailableDefs = collectAvailableDefinitions(input, sourceDef);
            availableDefinition.addAll(sourceAvailableDefs.stream().map(value -> "$" + ExpressionConstants.VAR_SOURCE + "/" + value).toList());
        }

        return availableDefinition;
    }

    @Override
    protected boolean stripVariableSegment() {
        return false;
    }
}

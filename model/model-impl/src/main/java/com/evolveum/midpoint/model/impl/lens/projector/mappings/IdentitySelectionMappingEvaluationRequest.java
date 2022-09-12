package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

/**
 * Request to create/evaluate so-called "identity selection" mapping.
 *
 * TODO why unused?
 */
public class IdentitySelectionMappingEvaluationRequest extends TemplateMappingEvaluationRequest {

    public IdentitySelectionMappingEvaluationRequest(
            @NotNull ObjectTemplateMappingType mapping, @NotNull ObjectTemplateType objectTemplate) {
        super(mapping, objectTemplate);
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>, AH extends AssignmentHolderType> Source<V, D>
    constructDefaultSource(ObjectDeltaObject<AH> focusOdo) throws SchemaException {
        return new Source<>(
                focusOdo.findIdi(SchemaConstants.PATH_FOCUS_IDENTITY),
                ExpressionConstants.VAR_INPUT_QNAME);
    }
}

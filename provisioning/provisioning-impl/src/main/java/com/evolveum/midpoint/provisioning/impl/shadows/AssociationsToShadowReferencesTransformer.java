package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.query.FilterItemPathTransformer;
import com.evolveum.midpoint.prism.query.ItemFilter;

import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationsContainerDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class AssociationsToShadowReferencesTransformer implements FilterItemPathTransformer {

    @Override
    public @Nullable ItemPath transform(ItemPath parentPath, ItemDefinition<?> parentDefinition, ItemFilter filter) {
        if (associationsUnrelated(parentPath, filter.getFullPath())) {
            return null;
        }

        var rewritten = new ArrayList<>();
        var prevDef = parentDefinition;
        var fromObjectPath = parentPath;
        for (var seg : filter.getFullPath().getSegments()) {
            ItemDefinition<?> currentDef = null;
            if (ItemPath.isItemOrInfraItem(seg)) {
                var name = ItemPath.toName(seg);
                currentDef = prevDef.findItemDefinition(name, ItemDefinition.class);
            } else if (ItemPath.isObjectReference(seg)) {
                // Prev def should be PrismReferenceDefinition
                if (prevDef instanceof ShadowReferenceAttributeDefinition refAttrDef) {
                    currentDef = refAttrDef.getTargetObjectDefinition();
                }
                if (currentDef == null && prevDef instanceof PrismReferenceDefinition refDef) {
                    currentDef = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(refDef.getTargetTypeName());
                }
            }
            Preconditions.checkState(currentDef != null,
                    "No definition found for %s. Already transformed path: %s", seg, rewritten);
            if (currentDef instanceof ShadowAssociationsContainerDefinition) {
                rewritten.add(ShadowType.F_REFERENCE_ATTRIBUTES);
            } else if (currentDef instanceof ShadowAssociationDefinition assocDef) {
                // We are inside associations container - should we do something special? - if attributes are involved
                rewritten.add(assocDef.getItemName()); // Add association name (normalize it)
                if (assocDef.hasAssociationObject()) {
                    // Association has associated object, we should emit dereference to associated object
                    rewritten.add(new ObjectReferencePathSegment());
                }
                //currentDef = assocDef.getAssociationObjectDefinition().getPrismObjectDefinition();
            } else if (prevDef instanceof ShadowAssociationDefinition assocDef) {
                if (QNameUtil.match(ShadowAssociationValueType.F_OBJECTS,ItemPath.toName(seg))) {
                    if (assocDef.hasAssociationObject()) {
                        rewritten.add(ShadowType.F_REFERENCE_ATTRIBUTES);
                    } else {

                    }
                }

            } else if (ItemPath.isName(seg)) {
                // Normalize item name based on definition.
                rewritten.add(currentDef.getItemName());
            } else {
                rewritten.add(seg);
            }
            prevDef = currentDef;
        }

        return ItemPath.create(rewritten);
    }

    private boolean associationsUnrelated(ItemPath parentPath, ItemPath nestedPath) {
        if (parentPath.startsWithName(ShadowType.F_ASSOCIATIONS)) {
            return false;
        }
        if (nestedPath.startsWithName(ShadowType.F_ASSOCIATIONS)) {
            return false;
        }
        return true;
    }
}


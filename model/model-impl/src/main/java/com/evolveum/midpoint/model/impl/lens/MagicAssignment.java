/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens;

import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathImpl;
import com.evolveum.midpoint.model.impl.lens.assignments.AssignmentPathSegmentImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.lazy.GenericLazyPrismContainerValue;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.stateNonNull;

class MagicAssignment {

    @NotNull private final AssignmentPathImpl assignmentPath;

    MagicAssignment(@NotNull AssignmentPathImpl assignmentPath) {
        this.assignmentPath = assignmentPath;
    }

    /** Returns lazily evaluated IDI for the magic assignment. */
    ItemDeltaItem<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>> lazyIdi()
            throws SchemaException {

        if (assignmentPath.isEmpty()) {
            return null;
        }

        var itemOld = lazyItem(true);
        var itemNew = lazyItem(false);
        var firstSegmentIdi = assignmentPath.getAt(0).getAssignmentIdi();
        var delta = firstSegmentIdi.getDelta();
        var subItemDeltas = firstSegmentIdi.getSubItemDeltas();

        return new ItemDeltaItem<>(
                itemOld,
                delta != null ? delta.clone() : null,
                itemNew,
                firstSegmentIdi.getDefinition(),
                firstSegmentIdi.getResolvePath(),
                firstSegmentIdi.getResolvePath(),
                subItemDeltas != null ? ItemDeltaCollectionsUtil.cloneCollection(subItemDeltas) : null); // TODO strategy
    }

    private PrismContainer<AssignmentType> lazyItem(boolean old) throws SchemaException {

        assert !assignmentPath.isEmpty();

        if (assignmentPath.first().getAssignmentPcv(old) == null) {
            return null; // no assignment before/after
        }

        Holder<PrismContainerValue<AssignmentType>> lazyValueHolder = new Holder<>();

        GenericLazyPrismContainerValue<AssignmentType> lazyValue = GenericLazyPrismContainerValue.from(

                new GenericLazyPrismContainerValue.ValueSource<>() {

                    @Override
                    public PrismContainerValue<AssignmentType> get() {
                        var segments = assignmentPath.getSegments();

                        var magicAssignmentPcv =
                                Objects.requireNonNull(segments.get(0).getAssignmentPcv(old))
                                        .mutableCopy();

                        try {
                            for (int i = 0; i < segments.size(); i++) {
                                AssignmentPathSegmentImpl segment = segments.get(i);
                                if (i > 0) { // the first assignment extension is present in magic assignment PCV already
                                    mergeExtensionFromAssignment(magicAssignmentPcv, segment.getAssignmentPcv(old));
                                }
                                mergeExtensionFromObject(magicAssignmentPcv, segment.getSource());
                            }
                            magicAssignmentPcv.freeze();
                            return magicAssignmentPcv;
                        } catch (SchemaException e) {
                            throw SystemException.unexpected(e, "while merging assignment extension");
                        }
                    }

                    @Override
                    public Long getId() {
                        return assignmentPath.first().getAssignmentId();
                    }

                    @Override
                    public AssignmentType getRealValue() {
                        var assignment = new AssignmentType();
                        assignment.setupContainerValue(lazyValueHolder.getValue());
                        return assignment;
                    }
                });

        lazyValueHolder.setValue(lazyValue);

        // We cannot call asSingleValueContainer right on lazyValue, as it would cause immediate materialization.
        return PrismUtil.asSingleValuedContainer(SchemaConstantsGenerated.C_ASSIGNMENT, lazyValue, getAssignmentCtd());
    }

    private @NotNull ComplexTypeDefinition getAssignmentCtd() {
        var focus = stateNonNull(assignmentPath.first().getSource(), "No focus");
        var focusDef = stateNonNull(focus.asPrismObject().getDefinition(), "No focus definition");
        var assignmentDef = stateNonNull(
                focusDef.findContainerDefinition(AssignmentHolderType.F_ASSIGNMENT), "No assignment definition");
        return stateNonNull(assignmentDef.getComplexTypeDefinition(), "No assignment definition CTD");
    }

    private static void mergeExtensionFromAssignment(
            @NotNull PrismContainerValue<AssignmentType> destPcv, @Nullable PrismContainerValue<AssignmentType> srcPcv)
            throws SchemaException {
        if (srcPcv != null) {
            mergeExtension(
                    destPcv,
                    srcPcv.findContainer(AssignmentType.F_EXTENSION));
        }
    }

    private static void mergeExtensionFromObject(
            @NotNull PrismContainerValue<AssignmentType> destPcv, @Nullable Objectable srcObject)
            throws SchemaException {
        if (srcObject != null) {
            mergeExtension(
                    destPcv,
                    srcObject.asPrismContainerValue().findContainer(ObjectType.F_EXTENSION));
        }
    }

    private static void mergeExtension(
            @NotNull PrismContainerValue<AssignmentType> dstPcv,
            @Nullable PrismContainer<?> srcExtension)
            throws SchemaException {
        if (srcExtension != null && srcExtension.hasAnyValue()) {
            ObjectTypeUtil.mergeExtension(
                    dstPcv.findOrCreateContainer(AssignmentType.F_EXTENSION).getValue(),
                    srcExtension.getValue());
        }
    }
}

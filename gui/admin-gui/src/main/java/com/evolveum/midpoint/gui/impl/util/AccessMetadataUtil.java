/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.util;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccessMetadataUtil {

    public static List<AssignmentPathMetadataType> computeAssignmentPaths(ObjectReferenceType roleMembershipRef) {
        List<AssignmentPathMetadataType> assignmentPaths = new ArrayList<>();
        List<ProvenanceMetadataType> metadataValues = collectProvenanceMetadata(roleMembershipRef.asReferenceValue());
        if (metadataValues == null) {
            return assignmentPaths;
        }
        for (ProvenanceMetadataType metadataType : metadataValues) {
            assignmentPaths.add(metadataType.getAssignmentPath());
        }
        return assignmentPaths;
    }

    public static <PV extends PrismValue> List<ProvenanceMetadataType> collectProvenanceMetadata(PV rowValue) {
        List<ValueMetadataType> valueMetadataValues = collectValueMetadata(rowValue);
        if (valueMetadataValues == null) {
            return null;
        }

        return valueMetadataValues.stream()
                .map(valueMetadata -> valueMetadata.getProvenance())
                .collect(Collectors.toList());

    }

    public static <PV extends PrismValue> List<StorageMetadataType> collectStorageMetadata(PV rowValue){
        List<ValueMetadataType> valueMetadataValues = collectValueMetadata(rowValue);
        if (valueMetadataValues == null) {
            return null;
        }

        return valueMetadataValues.stream()
                .map(valueMetadataType -> valueMetadataType.getStorage())
                .collect(Collectors.toList());
    }

    private static <PV extends PrismValue> List<ValueMetadataType> collectValueMetadata(PV rowValue) {
        PrismContainer<ValueMetadataType> valueMetadataContainer = rowValue.getValueMetadataAsContainer();
        if (valueMetadataContainer == null) {
            return null;
        }
        return (List<ValueMetadataType>) valueMetadataContainer.getRealValues();

    }
}

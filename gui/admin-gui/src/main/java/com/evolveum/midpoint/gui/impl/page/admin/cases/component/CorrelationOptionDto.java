/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription;
import com.evolveum.midpoint.model.api.correlation.CorrelationPropertyDefinition;
import com.evolveum.midpoint.model.api.correlation.CorrelationCaseDescription.CorrelationPropertyValuesDescription;
import com.evolveum.midpoint.model.api.correlator.CorrelationExplanation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Represents a correlation option: a candidate owner or a "new owner".
 */
public abstract class CorrelationOptionDto implements Serializable {

    /**
     * Owner focus object: either existing one (i.e. the candidate), or a new one (i.e. the pre-focus).
     *
     * The latter contains the result of pre-mappings execution, so it is only partially filled-in, and has no OID.
     */
    @NotNull final PrismObject<?> object;

    /**
     * Identifier corresponding to this choice. It should be sent to the case management engine when completing this request.
     */
    @NotNull private final String identifier;

    /**
     * Creates a DTO in the case of existing owner candidate.
     */
    CorrelationOptionDto(@NotNull ObjectReferenceType objectReference, @NotNull String identifier) {
        this.object = MiscUtil.stateNonNull(
                ObjectTypeUtil.getPrismObjectFromReference(objectReference),
                "No focus object");
        this.identifier = MiscUtil.stateNonNull(identifier, "No option identifier");
    }

    /**
     * Returns all real values matching given item path. The path should not contain container IDs.
     */
    abstract CorrelationPropertyValues getPropertyValues(CorrelationPropertyDefinition correlationPropertyDef);

    public @NotNull PrismObject<?> getObject() {
        return object;
    }

    public abstract boolean isNewOwner();

    public @NotNull String getIdentifier() {
        return identifier;
    }

    /** Returns true if the option matches given case/work item outcome URI. */
    public boolean matches(@NotNull String outcome) {
        return identifier.equals(outcome);
    }

    /** Returns `null` if and only if the option is "new owner". */
    public abstract Double getCandidateConfidenceValue();

    /** Returns `null` if and only if the option is "new owner". */
    public abstract String getCandidateConfidenceString();

    /** Returns `null` if the option is "new owner" or if there's no explanation available (for a candidate). */
    public abstract String getCandidateExplanation();

    /** Option representing an existing owner candidate. */
    static class Candidate extends CorrelationOptionDto {

        @NotNull private final CorrelationCaseDescription.CandidateDescription<?> candidateDescription;

        Candidate(
                @NotNull CorrelationCaseDescription.CandidateDescription<?> candidateDescription,
                @NotNull ObjectReferenceType candidateOwnerRef,
                @NotNull String identifier) {
            super(candidateOwnerRef, identifier);
            this.candidateDescription = candidateDescription;
        }

        @Override
        public boolean isNewOwner() {
            return false;
        }

        @Override
        public Double getCandidateConfidenceValue() {
            return candidateDescription.getConfidence();
        }

        @Override
        public String getCandidateConfidenceString() {
            return ((int) (getCandidateConfidenceValue() * 100)) + "%";
        }

        @Override
        public String getCandidateExplanation() {
            CorrelationExplanation explanation = candidateDescription.getExplanation();
            return explanation != null ?
                    LocalizationUtil.translateMessage(explanation.toLocalizableMessage()) : null;
        }

        @Override
        CorrelationPropertyValues getPropertyValues(@NotNull CorrelationPropertyDefinition correlationPropertyDef) {
            return CorrelationPropertyValues.fromDescription(
                    getPropertyValuesDescription(correlationPropertyDef));
        }

        CorrelationPropertyValuesDescription getPropertyValuesDescription(
                @NotNull CorrelationPropertyDefinition correlationPropertyDef) {
            return candidateDescription.getPropertyValuesDescription(correlationPropertyDef);
        }
    }

    /** Option representing "no existing owner" ("new owner") situation. */
    static class NewOwner extends CorrelationOptionDto {

        NewOwner(@NotNull ObjectReferenceType preFocusRef, String identifier) {
            super(preFocusRef, identifier);
        }

        @Override
        public boolean isNewOwner() {
            return true;
        }

        @Override
        public Double getCandidateConfidenceValue() {
            return null;
        }

        @Override
        public String getCandidateConfidenceString() {
            return null;
        }

        @Override
        public String getCandidateExplanation() {
            return null;
        }

        @Override
        CorrelationPropertyValues getPropertyValues(CorrelationPropertyDefinition correlationPropertyDef) {
            return CorrelationPropertyValues.fromObject(object, correlationPropertyDef.getItemPath());
        }
    }
}

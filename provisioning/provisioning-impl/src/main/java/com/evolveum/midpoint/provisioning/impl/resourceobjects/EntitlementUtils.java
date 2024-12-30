/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.util.DebugUtil.lazy;

class EntitlementUtils {

    private static final Trace LOGGER = TraceManager.getTrace(EntitlementUtils.class);

    /**
     * Creates a query that will select the entitlements for the subject.
     *
     * Entitlements point to subject using referencing ("association") attribute e.g. `ri:members`.
     * Subject is pointed to using referenced ("value") attribute, e.g. `ri:dn`.
     *
     * @param referencingAttrDef Definition of the referencing ("association") attribute, e.g. "members"
     * @param referencedAttrValue Value of the referenced ("value") attribute. E.g. uid=jack,ou=People,dc=example,dc=org.
     */
    static <TV, TA> ObjectQuery createEntitlementQuery(
            ShadowSimpleAttributeDefinition<TA> referencingAttrDef, PrismPropertyValue<TV> referencedAttrValue) {

        // The "referencedAttrValue" is what we look for in the entitlements (e.g. specific DN that should be their member).
        // We don't need the normalization, as the value is used for search on the resource.
        LOGGER.trace("Going to look for entitlements using value: {} def={}", referencedAttrValue, referencingAttrDef);
        ObjectQuery query = PrismContext.get().queryFor(ShadowType.class)
                .item(referencingAttrDef.getStandardPath(), referencingAttrDef)
                .eq(referencedAttrValue)
                .build();
        query.setAllowPartialResults(true); // TODO are we sure? What about missing data?
        return query;
    }

    static <T> @NotNull PrismPropertyValue<T> getSingleValueRequired(
            ShadowType shadow, QName attrName, QName associationName, Object errorCtx)
            throws SchemaException {
        return ShadowUtil.getSingleValueRequired(
                shadow, attrName, lazy(() -> " in association '%s' in %s".formatted(associationName, errorCtx)));
    }

    public static <T> @Nullable PrismPropertyValue<T> getSingleValue(
            ShadowType shadow, QName attrName, QName associationName, Object errorCtx)
            throws SchemaException {
        return ShadowUtil.getSingleValue(
                shadow, attrName, lazy(() -> " in association '%s' in %s".formatted(associationName, errorCtx)));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isVisible(ShadowReferenceAttributeDefinition def, ProvisioningContext subjectCtx) {
        if (def.isVisible(subjectCtx)) {
            return true;
        } else {
            LOGGER.trace("Association {} is not visible, skipping", def);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isSimulated(ShadowReferenceAttributeDefinition def) {
        if (def.isSimulated()) {
            return true;
        } else {
            LOGGER.trace("Association {} is not simulated, skipping", def);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isSimulatedSubjectToObject(ShadowReferenceAttributeDefinition def) {
        var simulationDef = def.getSimulationDefinition();
        if (simulationDef != null && simulationDef.isSubjectToObject()) {
            return true;
        } else {
            LOGGER.trace("Association {} is not a simulated subject-to-object one, skipping", def);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isSimulatedObjectToSubject(ShadowReferenceAttributeDefinition def) {
        var simulationDef = def.getSimulationDefinition();
        if (simulationDef != null && simulationDef.isObjectToSubject()) {
            return true;
        } else {
            LOGGER.trace("Association {} is not a simulated object-to-subject one, skipping", def);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean requiresExplicitReferentialIntegrity(ShadowReferenceAttributeDefinition def) {
        var simulationDef = def.getSimulationDefinition();
        if (simulationDef != null && simulationDef.requiresExplicitReferentialIntegrity()) {
            return true;
        } else {
            LOGGER.trace("Association {} does not require explicit referential integrity assurance, skipping", def);
            return false;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean doesMatchSubjectDelineation(
            @NotNull ShadowReferenceAttributeDefinition associationDef,
            @NotNull ProvisioningContext subjectCtx) {
        return doesMatchSubjectDelineation(associationDef.getSimulationDefinitionRequired(), subjectCtx);
    }

    /** FIXME very imprecise implementation - deals only with aux OC specification! */
    private static boolean doesMatchSubjectDelineation(
            SimulatedShadowReferenceTypeDefinition simulationDef,
            ProvisioningContext subjectCtx) {
        // We assume the subjectDef reflects the actual aux classes possessed by the subject shadow.
        ResourceObjectDefinition actualSubjectDef = subjectCtx.getObjectDefinitionRequired();
        for (var subjectParticipantDef : simulationDef.getSubjects()) {
            QName requiredAuxiliaryObjectClass = subjectParticipantDef.getAuxiliaryObjectClassName();
            if (requiredAuxiliaryObjectClass != null && !actualSubjectDef.hasAuxiliaryObjectClass(requiredAuxiliaryObjectClass)) {
                LOGGER.trace("Ignoring association {} because subject does not have auxiliary object class {}, it has {}",
                        simulationDef, requiredAuxiliaryObjectClass, actualSubjectDef.getAuxiliaryDefinitions());
                return false;
            }
        }
        return true;
    }
}

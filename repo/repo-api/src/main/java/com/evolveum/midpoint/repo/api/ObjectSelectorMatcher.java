/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.api;

import static com.evolveum.midpoint.util.MiscUtil.getDiagInfo;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.query.ObjectFilterExpressionEvaluator;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * This code is independent on particular repository implementation; hence, it is part of the API package.
 */
class ObjectSelectorMatcher {

    static boolean selectorMatches(ObjectSelectorType objectSelector, PrismValue value,
            ObjectFilterExpressionEvaluator filterEvaluator, Trace logger, String logMessagePrefix,
            RepositoryService repositoryService)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (objectSelector == null) {
            logger.trace("{} null object specification", logMessagePrefix);
            return false;
        }

        if (value == null) {
            logger.trace("{} null object", logMessagePrefix);
            return false;
        }

        Object realValue = value.getRealValueIfExists();
        ObjectType objectBean = realValue instanceof ObjectType ? ((ObjectType) realValue) : null;

        SearchFilterType specFilterBean = objectSelector.getFilter();
        ObjectReferenceType specOrgRef = objectSelector.getOrgRef();
        QName specTypeQName = objectSelector.getType(); // now it does not matter if it's unqualified

        // Type
        if (specTypeQName != null) {
            if (!value.isOfType(specTypeQName)) {
                if (logger.isTraceEnabled()) {
                    logger.trace("{} type mismatch, expected {}, was {}",
                            logMessagePrefix,
                            PrettyPrinter.prettyPrint(specTypeQName),
                            PrettyPrinter.prettyPrint(value.getTypeName()));
                }
                return false;
            }
        }

        // Subtype
        String specSubtype = objectSelector.getSubtype();
        if (specSubtype != null) {
            Collection<String> actualSubtypeValues;
            if (realValue instanceof ObjectType) {
                actualSubtypeValues = FocusTypeUtil.determineSubTypes((ObjectType) realValue);
            } else if (realValue instanceof AssignmentType) {
                actualSubtypeValues = ((AssignmentType) realValue).getSubtype();
            } else {
                actualSubtypeValues = List.of();
            }
            if (!actualSubtypeValues.contains(specSubtype)) {
                logger.trace("{} subtype mismatch, expected {}, was {}",
                        logMessagePrefix, specSubtype, actualSubtypeValues);
                return false;
            }
        }

        // Archetype
        List<ObjectReferenceType> specArchetypeRefs = objectSelector.getArchetypeRef();
        if (!specArchetypeRefs.isEmpty()) {
            if (realValue instanceof AssignmentHolderType) {
                boolean match = false;
                List<ObjectReferenceType> actualArchetypeRefs =
                        ((AssignmentHolderType) realValue).getArchetypeRef();
                for (ObjectReferenceType specArchetypeRef : specArchetypeRefs) {
                    for (ObjectReferenceType actualArchetypeRef : actualArchetypeRefs) {
                        if (actualArchetypeRef.getOid().equals(specArchetypeRef.getOid())) {
                            match = true;
                            break;
                        }
                    }
                }
                if (!match) {
                    logger.trace("{} archetype mismatch, expected {}, was {}",
                            logMessagePrefix, specArchetypeRefs, actualArchetypeRefs);
                    return false;
                }
            } else {
                logger.trace("{} archetype mismatch, expected {} but object has none (it is not of AssignmentHolderType)",
                        logMessagePrefix, specArchetypeRefs);
                return false;
            }
        }

        // Filter
        if (specFilterBean != null) {
            if (realValue == null) {
                throw new UnsupportedOperationException(
                        "Object selector with filter cannot be used for value without real value: " + getDiagInfo(value));
            }
            // This may or may not work. The resolution of types in query converter should be fixed.
            ObjectFilter specFilter = PrismContext.get().getQueryConverter()
                    .parseFilter(specFilterBean, realValue.getClass());
            if (filterEvaluator != null) {
                specFilter = filterEvaluator.evaluate(specFilter);
            }
            if (specFilter != null) {
                ObjectTypeUtil.normalizeFilter(specFilter, SchemaService.get().relationRegistry()); // we assume object is already normalized
                ObjectQueryUtil.assertPropertyOnly(specFilter, logMessagePrefix + " filter is not property-only filter");

                if (!(value instanceof PrismContainerValue<?>)) {
                    // This is because of filter limitations;
                    // TODO we should support application of filters to reference values (and probably property values as well)
                    throw new UnsupportedOperationException(String.format(
                            "Object selector with filter cannot be used for values other than container ones: %s",
                            getDiagInfo(value)));
                }
                PrismContainerValue<?> pcv = (PrismContainerValue<?>) value;
                try {
                    if (!specFilter.match(pcv, SchemaService.get().matchingRuleRegistry())) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("{} object {}", logMessagePrefix, getDiagInfo(value));
                        }
                        return false;
                    }
                } catch (SchemaException ex) {
                    throw new SchemaException(String.format(
                            "%s could not apply for %s: %s", logMessagePrefix, getDiagInfo(value), ex.getMessage()), ex);
                }
            }
        }

        // Org
        if (specOrgRef != null) {
            if (objectBean != null) {
                if (!repositoryService.isDescendant(objectBean.asPrismObject(), specOrgRef.getOid())) {
                    logger.trace("{} object OID {} (org={})",
                            logMessagePrefix, objectBean.getOid(), specOrgRef.getOid());
                    return false;
                }
            } else {
                if (logger.isTraceEnabled()) {
                    logger.trace("{} non-prism object {} (org={})",
                            logMessagePrefix, getDiagInfo(value), specOrgRef.getOid());
                }
                return false;
            }
        }

        return true;
    }
}

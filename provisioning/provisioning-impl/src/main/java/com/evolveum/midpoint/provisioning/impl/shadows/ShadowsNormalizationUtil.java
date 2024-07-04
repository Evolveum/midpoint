/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.schema.processor.NormalizationAwareResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO update this doc + consider renaming the class
 *
 * Repository shadows contain normalized attribute values.
 *
 * This class provides the necessary support for value normalization when storing the data and querying it.
 */
public class ShadowsNormalizationUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowsNormalizationUtil.class);

    /**
     * Visits the query and transforms the values according to defined normalization.
     *
     * This is because the repository shadows have attribute values stored in the normalization-aware form.
     * Hence, when querying, we must query this (special) form.
     *
     * Does not modify input query, creates a clone instead.
     *
     * @see NormalizationAwareResourceAttributeDefinition
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    public static ObjectQuery transformQueryValues(ObjectQuery originalQuery, @NotNull ResourceObjectDefinition objectDef) {
        if (originalQuery == null) {
            return null;
        }

        ObjectQuery processedQuery = originalQuery.clone();
        ObjectFilter filter = processedQuery.getFilter();
        if (filter == null) {
            return originalQuery;
        }

        filter.transformItemPaths(ItemPath.EMPTY_PATH, objectDef.getPrismObjectDefinition(), new AssociationsToShadowReferencesTransformer());


        Visitor visitor = f -> {
            try {
                // TODO what about other kinds of filters?
                if (f instanceof EqualFilter<?> equalFilter) {
                    transformAttributesEqualsFilter(equalFilter, objectDef);
                }
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        };
        filter.accept(visitor);
        return processedQuery;
    }

    private static boolean usesAssociations(ItemFilter itemFilter) {
        var path = itemFilter.getFullPath();
        if (path.startsWithName(ShadowType.F_ASSOCIATIONS)) {
            return true;
        }
        return false;
    }

    private static <T, N> void transformAttributesEqualsFilter(EqualFilter<T> eqFilter, @NotNull ResourceObjectDefinition objectDef)
            throws SchemaException {
        if (!eqFilter.getParentPath().equivalent(ShadowType.F_ATTRIBUTES)) {
            return;
        }

        QName attrName = eqFilter.getElementName();
        ShadowSimpleAttributeDefinition<?> attrDef =
                objectDef.findSimpleAttributeDefinitionRequired(attrName, () -> "in filter " + eqFilter);

        if (attrDef.getNormalizer().isIdentity()) {
            return;
        }

        NormalizationAwareResourceAttributeDefinition<N> normAttrDef = attrDef.toNormalizationAware();

        List<?> origRealValues = PrismValueCollectionsUtil.unwrap(MiscUtil.emptyIfNull(eqFilter.getValues()));

        // Brutal and ugly hack. We cannot easily replace the filter with a new one, so we simply replace its content.
        //noinspection unchecked
        EqualFilter<N> castFilter = (EqualFilter<N>) eqFilter;
        castFilter.setDefinition(normAttrDef);

        List<N> adoptedRealValues = normAttrDef.adoptRealValues(origRealValues);
        castFilter.setValues(PrismValueCollectionsUtil.wrap(adoptedRealValues));

        LOGGER.trace("Replacing values for attribute {} in search filter with normalized values because there "
                + "is a matching rule. Normalized values: {}", attrName, adoptedRealValues);
        castFilter.setMatchingRule(PrismConstants.POLY_STRING_NORM_MATCHING_RULE_NAME);
    }
}

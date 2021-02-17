package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.Visitor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Helps with query processing.
 */
@Component
class QueryHelper {

    private static final Trace LOGGER = TraceManager.getTrace(Helper.class);

    @Autowired private MatchingRuleRegistry matchingRuleRegistry;

    /**
     * Visit the query and normalize values (or set matching rules) as needed
     */
    @Contract("null, _ -> null; !null, _ -> !null")
    ObjectQuery applyMatchingRules(ObjectQuery originalQuery, RefinedObjectClassDefinition objectClassDef) {
        if (originalQuery == null) {
            return null;
        }

        ObjectQuery processedQuery = originalQuery.clone();
        ObjectFilter filter = processedQuery.getFilter();
        Visitor visitor = f -> {
            try {
                if (f instanceof EqualFilter) {
                    applyMatchingRuleToEqFilter((EqualFilter<?>) f, objectClassDef);
                }
            } catch (SchemaException e) {
                throw new SystemException(e);
            }
        };
        filter.accept(visitor);
        return processedQuery;
    }

    private <T> void applyMatchingRuleToEqFilter(EqualFilter<T> eqFilter, RefinedObjectClassDefinition objectClassDef)
            throws SchemaException {
        if (!eqFilter.getParentPath().equivalent(SchemaConstants.PATH_ATTRIBUTES)) {
            return;
        }

        QName attrName = eqFilter.getElementName();
        RefinedAttributeDefinition<?> rAttrDef = MiscUtil.requireNonNull(
                objectClassDef.findAttributeDefinition(attrName),
                () -> "Unknown attribute " + attrName + " in filter " + eqFilter);

        QName matchingRuleQName = rAttrDef.getMatchingRuleQName();
        if (matchingRuleQName == null) {
            return;
        }

        MatchingRule<T> matchingRule = matchingRuleRegistry.getMatchingRule(matchingRuleQName, rAttrDef.getTypeName());

        if (eqFilter.getValues() != null) {
            List<PrismPropertyValue<T>> newValues = new ArrayList<>();
            for (PrismPropertyValue<T> oldValue : eqFilter.getValues()) {
                newValues.add(normalizeValue(matchingRule, oldValue));
            }
            eqFilter.getValues().clear();
            eqFilter.getValues().addAll(newValues);
            LOGGER.trace("Replacing values for attribute {} in search filter with normalized values because there "
                    + "is a matching rule. Normalized values: {}", attrName, newValues);
        }
    }

    @NotNull
    private <T> PrismPropertyValue<T> normalizeValue(MatchingRule<T> matchingRule, PrismPropertyValue<T> oldValue)
            throws SchemaException {
        T normalizedRealValue = matchingRule.normalize(oldValue.getValue());
        PrismPropertyValue<T> newValue = oldValue.clone();
        newValue.setValue(normalizedRealValue);
        return newValue;
    }

}

/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ComparativeFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.ConstantCondition;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 * @author mederly
 */
public class PropertyRestriction extends ItemValueRestriction<PropertyValueFilter> {

    private static final Trace LOGGER = TraceManager.getTrace(PropertyRestriction.class);

    private JpaLinkDefinition<JpaPropertyDefinition> linkDefinition;

    public PropertyRestriction(InterpretationContext context, PropertyValueFilter filter, JpaEntityDefinition baseEntityDefinition,
                               Restriction parent, JpaLinkDefinition<JpaPropertyDefinition> linkDefinition) {
        super(context, filter, baseEntityDefinition, parent);
        Validate.notNull(linkDefinition, "linkDefinition");
        this.linkDefinition = linkDefinition;
    }

    @Override
    public Condition interpretInternal() throws QueryException {

        JpaPropertyDefinition propertyDefinition = linkDefinition.getTargetDefinition();
        if (propertyDefinition.isLob()) {
            throw new QueryException("Can't query based on clob property value '" + linkDefinition + "'.");
        }

        String propertyValuePath = getHqlDataInstance().getHqlPath();
        if (filter.getRightHandSidePath() != null) {
            return createPropertyVsPropertyCondition(propertyValuePath);
        } else {
            Object value = getValueFromFilter(filter);
            if (value == null && propertyDefinition.isNeverNull()) {
                LOGGER.warn("Checking nullity of non-null property {} (filter = {})", propertyDefinition, filter);
                return new ConstantCondition(context.getHibernateQuery(), false);
            } else {
                Condition condition = createPropertyVsConstantCondition(propertyValuePath, value, filter);
                return addIsNotNullIfNecessary(condition, propertyValuePath);
            }
        }
    }

    Condition createPropertyVsPropertyCondition(String leftPropertyValuePath) throws QueryException {
        HqlDataInstance rightItem = getItemPathResolver().resolveItemPath(filter.getRightHandSidePath(),
                filter.getRightHandSideDefinition(), getBaseHqlEntityForChildren(), true);
        String rightHqlPath = rightItem.getHqlPath();
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();

        // TODO take data types into account, e.g.
        //  PolyString vs PolyString
        //  PolyString vs String (and vice versa)
        //  non-string extension items vs non-string static items
        //
        // And also take matching rules into account as well.

        if (filter instanceof EqualFilter) {
            // left = right OR (left IS NULL AND right IS NULL)
            Condition condition = hibernateQuery.createCompareXY(leftPropertyValuePath, rightHqlPath, "=", false);
            return hibernateQuery.createOr(
                    condition,
                    hibernateQuery.createAnd(
                            hibernateQuery.createIsNull(leftPropertyValuePath),
                            hibernateQuery.createIsNull(rightHqlPath)
                    )
            );
        } else if (filter instanceof ComparativeFilter) {
            ItemRestrictionOperation operation = findOperationForFilter(filter);
            return hibernateQuery.createCompareXY(leftPropertyValuePath, rightHqlPath, operation.symbol(), false);
        } else {
            throw new QueryException("Right-side ItemPath is supported currently only for EqualFilter or ComparativeFilter, not for " + filter);
        }
    }

    private Object getValueFromFilter(ValueFilter filter) throws QueryException {

        JpaPropertyDefinition def = linkDefinition.getTargetDefinition();

        Object value;
        if (filter instanceof PropertyValueFilter) {
            value = getValue((PropertyValueFilter) filter);
        } else {
            throw new QueryException("Unknown filter '" + filter + "', can't get value from it.");
        }

        Object adaptedValue = adaptValueType(value, filter);

        if (def.isEnumerated()) {
            return getRepoEnumValue((Enum) adaptedValue, def.getJpaClass());
        } else {
            return adaptedValue;
        }
    }

    private Object adaptValueType(Object value, ValueFilter filter) throws QueryException {

        Class<?> expectedType = linkDefinition.getTargetDefinition().getJaxbClass();
        if (expectedType == null || value == null) {
            return value;   // nothing to check here
        }

        Class<?> expectedWrappedType;
        if (expectedType.isPrimitive()) {
            expectedWrappedType = ClassUtils.primitiveToWrapper(expectedType);
        } else {
            expectedWrappedType = expectedType;
        }

        Object adaptedValue;
        //todo remove after some time [lazyman]
        //attempt to fix value type for polystring (if it was string in filter we create polystring from it)
        if (PolyString.class.equals(expectedWrappedType) && value instanceof String) {
            LOGGER.debug("Trying to query PolyString value but filter contains String '{}'.", filter);
            String orig = (String) value;
            adaptedValue = new PolyString(orig, context.getPrismContext().getDefaultPolyStringNormalizer().normalize(orig));
        } else if (PolyString.class.equals(expectedWrappedType) && value instanceof PolyStringType) {
            //attempt to fix value type for polystring (if it was polystring type in filter we create polystring from it)
            LOGGER.debug("Trying to query PolyString value but filter contains PolyStringType '{}'.", filter);
            PolyStringType type = (PolyStringType) value;
            adaptedValue = new PolyString(type.getOrig(), type.getNorm());
        } else if (String.class.equals(expectedWrappedType) && value instanceof QName) {
            //eg. shadow/objectClass
            adaptedValue = RUtil.qnameToString((QName) value);
        } else if (value instanceof RawType) {     // MID-3850: but it's quite a workaround. Maybe we should treat RawType's earlier than this.
            try {
                adaptedValue = ((RawType) value).getParsedRealValue(expectedWrappedType);
            } catch (SchemaException e) {
                throw new QueryException("Couldn't parse value " + value + " as " + expectedWrappedType + ": " + e.getMessage(), e);
            }
        } else {
            adaptedValue = value;
        }

        if (expectedWrappedType.isAssignableFrom(adaptedValue.getClass())) {
            return adaptedValue;
        } else {
            throw new QueryException("Value should be type of '" + expectedWrappedType + "' but it's '"
                    + value.getClass() + "', filter '" + filter + "'.");
        }
    }

    private Enum getRepoEnumValue(Enum<?> schemaValue, Class<?> repoType) throws QueryException {
        if (schemaValue == null) {
            return null;
        }

        if (SchemaEnum.class.isAssignableFrom(repoType)) {
            //noinspection unchecked
            return (Enum) RUtil.getRepoEnumValue(schemaValue, (Class<? extends SchemaEnum>) repoType);
        }

        Object[] constants = repoType.getEnumConstants();
        for (Object constant : constants) {
            Enum e = (Enum) constant;
            if (e.name().equals(schemaValue.name())) {
                return e;
            }
        }

        throw new QueryException("Unknown enum value '" + schemaValue + "', which is type of '"
                + schemaValue.getClass() + "'.");
    }

}

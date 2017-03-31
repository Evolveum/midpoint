/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query2.restriction;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ComparativeFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.resolution.HqlDataInstance;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaEntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaPropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaLinkDefinition;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.OrCondition;
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

        if (linkDefinition.getTargetDefinition().isLob()) {
            throw new QueryException("Can't query based on clob property value '" + linkDefinition + "'.");
        }

        String propertyValuePath = getHqlDataInstance().getHqlPath();
        if (filter.getRightHandSidePath() != null) {
            return createPropertyVsPropertyCondition(propertyValuePath);
        } else {
            Object value = getValueFromFilter(filter);
            Condition condition = createPropertyVsConstantCondition(propertyValuePath, value, filter);
            return addIsNotNullIfNecessary(condition, propertyValuePath);
        }
    }

    protected Condition createPropertyVsPropertyCondition(String leftPropertyValuePath) throws QueryException {
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
            OrCondition orCondition = hibernateQuery.createOr(
                    condition,
                    hibernateQuery.createAnd(
                            hibernateQuery.createIsNull(leftPropertyValuePath),
                            hibernateQuery.createIsNull(rightHqlPath)
                    )
            );
            return orCondition;
        } else if (filter instanceof ComparativeFilter) {
            ItemRestrictionOperation operation = findOperationForFilter(filter);
            Condition condition = hibernateQuery.createCompareXY(leftPropertyValuePath, rightHqlPath, operation.symbol(), false);
            return condition;
        } else {
            throw new QueryException("Right-side ItemPath is supported currently only for EqualFilter or ComparativeFilter, not for " + filter);
        }
    }

    protected Object getValueFromFilter(ValueFilter filter) throws QueryException {

        JpaPropertyDefinition def = linkDefinition.getTargetDefinition();

        Object value;
        if (filter instanceof PropertyValueFilter) {
            value = getValue((PropertyValueFilter) filter);
        } else {
            throw new QueryException("Unknown filter '" + filter + "', can't get value from it.");
        }

        value = checkValueType(value, filter);

        if (def.isEnumerated()) {
            value = getRepoEnumValue((Enum) value, def.getJpaClass());
        }

        return value;
    }

    private Object checkValueType(Object value, ValueFilter filter) throws QueryException {

        Class expectedType = linkDefinition.getTargetDefinition().getJaxbClass();
        if (expectedType == null || value == null) {
            return value;   // nothing to check here
        }

        if (expectedType.isPrimitive()) {
            expectedType = ClassUtils.primitiveToWrapper(expectedType);
        }

        //todo remove after some time [lazyman]
        //attempt to fix value type for polystring (if it was string in filter we create polystring from it)
        if (PolyString.class.equals(expectedType) && (value instanceof String)) {
            LOGGER.debug("Trying to query PolyString value but filter contains String '{}'.", filter);
			String orig = (String) value;
			value = new PolyString(orig, context.getPrismContext().getDefaultPolyStringNormalizer().normalize(orig));
        }
        //attempt to fix value type for polystring (if it was polystringtype in filter we create polystring from it)
        if (PolyString.class.equals(expectedType) && (value instanceof PolyStringType)) {
            LOGGER.debug("Trying to query PolyString value but filter contains PolyStringType '{}'.", filter);
            PolyStringType type = (PolyStringType) value;
            value = new PolyString(type.getOrig(), type.getNorm());
        }

        if (String.class.equals(expectedType) && (value instanceof QName)) {
            //eg. shadow/objectClass
            value = RUtil.qnameToString((QName) value);
        }

        if (value instanceof RawType) {     // MID-3850: but it's quite a workaround. Maybe we should treat RawType's earlier than this.
            try {
                return ((RawType) value).getParsedRealValue(expectedType);
            } catch (SchemaException e) {
                throw new QueryException("Couldn't parse value " + value + " as " + expectedType + ": " + e.getMessage(), e);
            }
        }

        if (!expectedType.isAssignableFrom(value.getClass())) {
            throw new QueryException("Value should be type of '" + expectedType + "' but it's '"
                    + value.getClass() + "', filter '" + filter + "'.");
        }
        return value;
    }

    private Enum getRepoEnumValue(Enum schemaValue, Class repoType) throws QueryException {
        if (schemaValue == null) {
            return null;
        }

        if (SchemaEnum.class.isAssignableFrom(repoType)) {
            return (Enum) RUtil.getRepoEnumValue(schemaValue, repoType);
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

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

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query2.InterpretationContext;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query2.QueryInterpreter2;
import com.evolveum.midpoint.repo.sql.query2.definition.AnyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.CollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.Definition;
import com.evolveum.midpoint.repo.sql.query2.definition.EntityDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.JpaDefinitionPath;
import com.evolveum.midpoint.repo.sql.query2.definition.PropertyDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.ReferenceDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualCollectionDefinition;
import com.evolveum.midpoint.repo.sql.query2.definition.VirtualQueryParam;
import com.evolveum.midpoint.repo.sql.query2.hqm.EntityReference;
import com.evolveum.midpoint.repo.sql.query2.hqm.HibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.JoinSpecification;
import com.evolveum.midpoint.repo.sql.query2.hqm.RootHibernateQuery;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.AndCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.Condition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNotNullCondition;
import com.evolveum.midpoint.repo.sql.query2.hqm.condition.IsNullCondition;
import com.evolveum.midpoint.repo.sql.query2.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract superclass for all value-related filters. There are two major problems solved:
 * 1) mapping from ItemPath to HQL property paths
 * 2) adding joined entities to the query, along with necessary conditions
 *    (there are two kinds of joins: left outer join and carthesian join)
 *
 * After the necessary entity is available, the fine work (creating one or more conditions
 * to execute the filtering) is done by subclasses of this path in the interpretInternal(..) method.
 *
 * @author lazyman
 * @author mederly
 */
public abstract class ItemRestriction<T extends ValueFilter> extends Restriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemRestriction.class);

    /**
     * Definition of the root entity. Root entity corresponds to the ObjectType class that was requested
     * by the search operation, or the one that was refined from abstract types (ObjectType, AbstractRoleType, ...)
     * in the process of restriction construction.
     */
    private EntityDefinition rootEntityDefinition;

    /**
     * Property path that is the base of this restriction.
     *
     * For simple cases like UserType: name, UserType: activation/administrativeStatus etc. the root
     * is the query primary entity alias (e.g. u in "RUser u").
     *
     * For "ForValue" filters the startPropertyPath corresponds to the base item pointed to by the filter.
     * E.g. in "UserType: ForValue (assignment)" it is "a" (provided that there is
     * "RUser u left join u.assignments a with ..." already defined).
     *
     * (Item restriction expects that its root is already defined in the FROM clause.)
     */
    private String startPropertyPath;

    /**
     * Definition of the entity when this restriction starts. It is usually the same as rootEntityDefinition,
     * but for ForValue children it is the entity pointed to by ForValue restriction. (I.e. assignment in the above
     * example.)
     */
    private EntityDefinition startEntityDefinition;

    public ItemRestriction(EntityDefinition rootEntityDefinition, String startPropertyPath, EntityDefinition startEntityDefinition) {
        Validate.notNull(rootEntityDefinition);
        Validate.notNull(startPropertyPath);
        Validate.notNull(startEntityDefinition);
        this.rootEntityDefinition = rootEntityDefinition;
        this.startPropertyPath = startPropertyPath;
        this.startEntityDefinition = startEntityDefinition;
    }

    @Override
    public Condition interpret() throws QueryException {

    	ItemPath path = filter.getFullPath();
        if (ItemPath.isNullOrEmpty(path)) {
            throw new QueryException("Null or empty path for ItemRestriction in " + filter.debugDump());
        }

        String hqlPropertyPath = prepareJoins(path);

        Condition condition = interpretInternal(hqlPropertyPath);
        return condition;
    }

    // returns property path that can be used to access the item values
    private String prepareJoins(ItemPath relativePath) throws QueryException {

        ItemPath fullPath = getFullPath(relativePath);
        LOGGER.trace("Updating query context based on path {}; full path is {}", relativePath, fullPath);

        /**
         * We have to do something like this - examples:
         * - activation.administrativeStatus -> (nothing, activation is embedded entity)
         * - assignment.targetRef -> "left join u.assignments a with ..."
         * - assignment.resourceRef -> "left join u.assignments a with ..."
         * - organization -> "left join u.organization o"
         *
         * Or more complex:
         * - assignment.modifyApproverRef -> "left join u.assignments a (...) left join a.modifyApproverRef m (...)"
         * - assignment.target.longs -> "left join u.assignments a (...), RObject o left join o.longs (...)"
         */
        JpaDefinitionPath jpaDefinitionPath = startEntityDefinition.translatePath(relativePath);
        String currentHqlPath = startPropertyPath;

        for (Definition definition : jpaDefinitionPath.getDefinitions()) {
            if (definition instanceof EntityDefinition) {
                EntityDefinition entityDef = (EntityDefinition) definition;
                if (!entityDef.isEmbedded()) {
                    LOGGER.trace("Adding join for '{}' to context", entityDef.getJpaName());
                    currentHqlPath = addJoin(entityDef, currentHqlPath);
                }
            } else if (definition instanceof AnyDefinition) {
                LOGGER.trace("Adding join for '{}' to context", definition.getJpaName());
                currentHqlPath = addJoin(definition, currentHqlPath);
                break;
            } else if (definition instanceof CollectionDefinition) {
                LOGGER.trace("Adding join for '{}' to context", definition.getJpaName());
                currentHqlPath = addJoin(definition, currentHqlPath);
            } else if (definition instanceof PropertyDefinition || definition instanceof ReferenceDefinition) {
                break;
            } else {
                throw new QueryException("Not implemented yet.");
            }
            // TODO entity crossjoin references (when crossing object boundaries)
        }

        return currentHqlPath;
    }

    protected String addJoin(Definition joinedItemDefinition, String currentHqlPath) throws QueryException {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        EntityReference entityReference = hibernateQuery.getPrimaryEntity();                    // TODO other references (in the future)
        String joinedItemJpaName = joinedItemDefinition.getJpaName();
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemDefinition);
        Condition condition = null;
        if (joinedItemDefinition instanceof VirtualCollectionDefinition) {
            VirtualCollectionDefinition vcd = (VirtualCollectionDefinition) joinedItemDefinition;
            List<Condition> conditions = new ArrayList<>(vcd.getAdditionalParams().length);
            for (VirtualQueryParam vqp : vcd.getAdditionalParams()) {
                // e.g. name = "assignmentOwner", type = RAssignmentOwner.class, value = "ABSTRACT_ROLE"
                Object value = createQueryParamValue(vqp);
                Condition c = hibernateQuery.createEq(joinedItemAlias + "." + vqp.name(), value);
                conditions.add(c);
            }
            if (conditions.size() > 1) {
                condition = hibernateQuery.createAnd(conditions);
            } else if (conditions.size() == 1) {
                condition = conditions.iterator().next();
            }
        }
        entityReference.addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, condition));
        return joinedItemAlias;
    }

    /**
     * This method provides transformation from {@link String} value defined in
     * {@link com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam#value()} to real object. Currently only
     * to simple types and enum values.
     */
    private Object createQueryParamValue(VirtualQueryParam param) throws QueryException {
        Class type = param.type();
        String value = param.value();

        try {
            if (type.isPrimitive()) {
                return type.getMethod("valueOf", new Class[]{String.class}).invoke(null, new Object[]{value});
            }

            if (type.isEnum()) {
                return Enum.valueOf(type, value);
            }
        } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException|RuntimeException ex) {
            throw new QueryException("Couldn't transform virtual query parameter '"
                    + param.name() + "' from String to '" + type + "', reason: " + ex.getMessage(), ex);
        }

        throw new QueryException("Couldn't transform virtual query parameter '"
                + param.name() + "' from String to '" + type + "', it's not yet implemented.");
    }


    protected String addJoinAny(String currentHqlPath, String anyAssociationName, QName itemName, RObjectExtensionType ownerType) {
        RootHibernateQuery hibernateQuery = context.getHibernateQuery();
        EntityReference entityReference = hibernateQuery.getPrimaryEntity();                    // TODO other references (in the future)
        String joinedItemJpaName = anyAssociationName;
        String joinedItemFullPath = currentHqlPath + "." + joinedItemJpaName;
        String joinedItemAlias = hibernateQuery.createAlias(joinedItemJpaName, false);

        Condition condition = hibernateQuery.createAnd(
                hibernateQuery.createEq(joinedItemAlias + ".ownerType", ownerType),
                hibernateQuery.createEq(joinedItemAlias + "." + RAnyValue.F_NAME, RUtil.qnameToString(itemName)));

        entityReference.addJoin(new JoinSpecification(joinedItemAlias, joinedItemFullPath, condition));
        return joinedItemAlias;
    }

    public abstract Condition interpretInternal(String hqlPath) throws QueryException;

    protected Condition createCondition(String propertyName, Object value, ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation;
        if (filter instanceof EqualFilter) {
            operation = ItemRestrictionOperation.EQ;
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter gf = (GreaterFilter) filter;
            operation = gf.isEquals() ? ItemRestrictionOperation.GE : ItemRestrictionOperation.GT;
        } else if (filter instanceof LessFilter) {
            LessFilter lf = (LessFilter) filter;
            operation = lf.isEquals() ? ItemRestrictionOperation.LE : ItemRestrictionOperation.LT;
        } else if (filter instanceof SubstringFilter) {
            SubstringFilter substring = (SubstringFilter) filter;
            if (substring.isAnchorEnd()) {
                operation = ItemRestrictionOperation.ENDS_WITH;
            } else if (substring.isAnchorStart()) {
                operation = ItemRestrictionOperation.STARTS_WITH;
            } else {
                operation = ItemRestrictionOperation.SUBSTRING;
            }
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation.");
        }

        InterpretationContext context = getContext();
        QueryInterpreter2 interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);

        String matchingRule = null;
        if (filter.getMatchingRule() != null){
        	matchingRule = filter.getMatchingRule().getLocalPart();
        }
        
        return matcher.match(null, operation, propertyName, value, matchingRule);
    }

    protected Object getValue(List<? extends PrismValue> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        PrismValue val = values.get(0);
        if (val instanceof PrismPropertyValue) {
            PrismPropertyValue propertyValue = (PrismPropertyValue) val;
            return propertyValue.getValue();
        }

        return null;
    }

    protected Object getValueFromFilter(ValueFilter filter, PropertyDefinition def) throws QueryException {
        Object value;
        if (filter instanceof PropertyValueFilter) {
            value = getValue(((PropertyValueFilter) filter).getValues());
        } else {
            throw new QueryException("Unknown filter '" + filter + "', can't get value from it.");
        }

        //todo remove after some time [lazyman]
        //attempt to fix value type for polystring (if it was string in filter we create polystring from it)
        if (PolyString.class.equals(def.getJaxbType()) && (value instanceof String)) {
            LOGGER.debug("Trying to query PolyString value but filter contains String '{}'.", new Object[]{filter});
            value = new PolyString((String) value, (String) value);
        }
        //attempt to fix value type for polystring (if it was polystringtype in filter we create polystring from it)
        if (PolyString.class.equals(def.getJaxbType()) && (value instanceof PolyStringType)) {
            LOGGER.debug("Trying to query PolyString value but filter contains PolyStringType '{}'.", new Object[]{filter});
            PolyStringType type = (PolyStringType) value;
            value = new PolyString(type.getOrig(), type.getNorm());
        }

        if (String.class.equals(def.getJaxbType()) && (value instanceof QName)) {
            //eg. shadow/objectClass
            value = RUtil.qnameToString((QName) value);
        }

        if (value != null && !def.getJaxbType().isAssignableFrom(value.getClass())) {
            throw new QueryException("Value should by type of '" + def.getJaxbType() + "' but it's '"
                    + value.getClass() + "', filter '" + filter + "'.");
        }

        if (def.isEnumerated()) {
            value = getRepoEnumValue((Enum) value, def.getJpaType());
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



    /**
     * Filter of type NOT(PROPERTY=VALUE) causes problems when there are entities with PROPERTY set to NULL.
     *
     * Such a filter has to be treated like
     *
     *      NOT (PROPERTY=VALUE & PROPERTY IS NOT NULL)
     *
     * TODO implement for restrictions other than PropertyRestriction.
     */
    protected Condition addIsNotNullIfNecessary(Condition condition, String propertyPath) {
        if (condition instanceof IsNullCondition || condition instanceof IsNotNullCondition) {
            return condition;
        }
        if (!isNegated()) {
            return condition;
        }
        RootHibernateQuery hibernateQuery = getContext().getHibernateQuery();
        AndCondition conjunction = hibernateQuery.createAnd();
        conjunction.add(condition);
        conjunction.add(hibernateQuery.createIsNotNull(propertyPath));
        return conjunction;
    }

}

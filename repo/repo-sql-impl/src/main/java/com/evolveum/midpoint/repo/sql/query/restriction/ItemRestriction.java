/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.query.restriction;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class ItemRestriction<T extends ValueFilter> extends Restriction<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ItemRestriction.class);

    @Override
    public boolean canHandle(ObjectFilter filter, QueryContext context) throws QueryException {
        Validate.notNull(filter, "Object filter must not be null.");
        if (!(filter instanceof ValueFilter)) {
            return false;
        }
        return true;
    }

    @Override
    public Criterion interpret(T filter) throws QueryException {

        ItemPath path = createFullPath(filter);
        if (path != null) {
            // at first we build criterias with aliases
            updateQueryContext(path);
        }

        Criterion main = interpretInternal(filter);
        Criterion virtual = createVirtualCriterion(path);
        if (virtual != null) {
            return Restrictions.and(virtual, main);
        }

        return main;
    }

    public abstract Criterion interpretInternal(T filter) throws QueryException;

    //todo reimplement, use DefinitionHandlers or maybe another great concept
    private void updateQueryContext(ItemPath path) throws QueryException {
        LOGGER.trace("Updating query context based on path {}", new Object[]{path.toString()});
        Class<? extends ObjectType> type = getContext().getType();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);

        List<ItemPathSegment> segments = path.getSegments();

        List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();
        ItemPath propPath;
        for (ItemPathSegment segment : segments) {
            QName qname = ItemPath.getName(segment);
            // create new property path
            propPathSegments.add(new NameItemPathSegment(qname));
            propPath = new ItemPath(propPathSegments);
            // get entity query definition

            Definition childDef = definition.findDefinition(qname, Definition.class);
            if (childDef == null) {
                throw new QueryException("Definition '" + definition + "' doesn't contain child definition '"
                        + qname + "'. Please check your path in query, or query entity/attribute mappings. "
                        + "Full path was '" + path + "'.");
            }

            //todo change this if instanceof and use DefinitionHandler [lazyman]
            if (childDef instanceof EntityDefinition) {
                EntityDefinition entityDef = (EntityDefinition) childDef;
                if (!entityDef.isEmbedded()) {
                    //create new criteria
                    LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                            new Object[]{entityDef.getJpaName(), propPath.toString()});
                    addNewCriteriaToContext(propPath, entityDef, entityDef.getJpaName());
                } else {
                    // we don't create new sub criteria, just add this new item path to aliases
                    addPathAliasToContext(propPath);
                }
                definition = entityDef;
            } else if (childDef instanceof AnyDefinition) {
                LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                        new Object[]{childDef.getJpaName(), propPath.toString()});
                addNewCriteriaToContext(propPath, childDef, childDef.getJpaName());
                break;
            } else if (childDef instanceof CollectionDefinition) {
                LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
                        new Object[]{childDef.getJpaName(), propPath.toString()});
                addNewCriteriaToContext(propPath, childDef, childDef.getJpaName());
                Definition def = ((CollectionDefinition) childDef).getDefinition();
                if (def instanceof EntityDefinition) {
                    definition = (EntityDefinition) def;
                }
            } else if (childDef instanceof PropertyDefinition || childDef instanceof ReferenceDefinition) {
                break;
            } else {
                //todo throw something here [lazyman]
                throw new QueryException("Not implemented yet.");
            }
        }
    }

    private Criterion createVirtualCriterion(ItemPath path) throws QueryException {
        LOGGER.trace("Scanning path for virtual definitions to create criteria {}", new Object[]{path.toString()});
        QueryContext context = getContext();
        Class<? extends ObjectType> type = context.getType();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        EntityDefinition definition = registry.findDefinition(type, null, EntityDefinition.class);

        List<Criterion> criterions = new ArrayList<Criterion>();

        List<ItemPathSegment> segments = path.getSegments();

        List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();
        ItemPath propPath;
        for (ItemPathSegment segment : segments) {
            QName qname = ItemPath.getName(segment);
            // create new property path
            propPathSegments.add(new NameItemPathSegment(qname));
            propPath = new ItemPath(propPathSegments);
            // get entity query definition

            Definition childDef = definition.findDefinition(qname, Definition.class);
            if (childDef == null) {
                throw new QueryException("Definition '" + definition + "' doesn't contain child definition '"
                        + qname + "'. Please check your path in query, or query entity/attribute mappings. "
                        + "Full path was '" + path + "'.");
            }

            //todo change this if instanceof and use DefinitionHandler [lazyman]
            if (childDef instanceof EntityDefinition) {
                definition = (EntityDefinition) childDef;
            } else if (childDef instanceof CollectionDefinition) {
                CollectionDefinition collection = (CollectionDefinition) childDef;
                if (childDef instanceof VirtualCollectionDefinition) {
                    VirtualCollectionDefinition virtual = (VirtualCollectionDefinition) childDef;

                    criterions.add(updateMainCriterionQueryParam(virtual.getAdditionalParams(), propPath));
                }

                Definition def = collection.getDefinition();
                if (def instanceof EntityDefinition) {
                    definition = (EntityDefinition) def;
                }
            } else if (childDef instanceof PropertyDefinition || childDef instanceof ReferenceDefinition
                    || childDef instanceof AnyDefinition) {
                break;
            } else {
                //todo throw something here [lazyman]
                throw new QueryException("Not implemented yet.");
            }
        }

        return andCriterions(criterions);
    }

    private Criterion andCriterions(List<Criterion> criterions) {
        switch (criterions.size()) {
            case 0:
                return null;
            case 1:
                return criterions.get(0);
            default:
                return Restrictions.and(criterions.toArray(new Criterion[criterions.size()]));
        }
    }

    private Criterion updateMainCriterionQueryParam(VirtualQueryParam[] params, ItemPath propPath)
            throws QueryException {
        List<Criterion> criterions = new ArrayList<Criterion>();

        String alias = getContext().getAlias(propPath);
        for (VirtualQueryParam param : params) {
            Criterion criterion = Restrictions.eq(alias + "." + param.name(), createQueryParamValue(param, propPath));
            criterions.add(criterion);
        }

        return andCriterions(criterions);
//        if (criterions.size() == 1) {
//            getContext().getCriteria(null).add(criterions.get(0));
//        } else if (criterions.size() > 1) {
//            Criterion c = Restrictions.and(criterions.toArray(new Criterion[criterions.size()]));
//            getContext().getCriteria(null).add(c);
//        }
    }

    private Object createQueryParamValue(VirtualQueryParam param, ItemPath propPath) throws QueryException {
        Class type = param.type();
        String value = param.value();

        try {
            if (type.isPrimitive()) {
                return type.getMethod("valueOf", new Class[]{String.class}).invoke(null, new Object[]{value});
            }

            if (type.isEnum()) {
                return Enum.valueOf(type, value);
            }
        } catch (Exception ex) {
            throw new QueryException("Couldn't transform virtual query parameter '"
                    + param.name() + "' from String to '" + type + "', reason: " + ex.getMessage(), ex);
        }

        throw new QueryException("Couldn't transform virtual query parameter '"
                + param.name() + "' from String to '" + type + "', it's not yet implemented.");
    }

    private void addPathAliasToContext(ItemPath path) {
        ItemPath lastPropPath = path.allExceptLast();
        if (ItemPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }

        String alias = getContext().getAlias(lastPropPath);
        getContext().addAlias(path, alias);
    }

    protected void addNewCriteriaToContext(ItemPath path, Definition def, String realName) {
        ItemPath lastPropPath = path.allExceptLast();
        if (ItemPath.EMPTY_PATH.equals(lastPropPath)) {
            lastPropPath = null;
        }

        final ItemPath virtualPath = lastPropPath != null ? new ItemPath(lastPropPath, new QName("", realName)) :
                new ItemPath(new QName("", realName));

        Criteria existing = getContext().getCriteria(path);
        if (existing != null) {
            return;
        }

        Criteria virtualCriteria = getContext().getCriteria(virtualPath);
        if (virtualCriteria != null) {
            getContext().addAlias(path, virtualCriteria.getAlias());
            getContext().addCriteria(path, virtualCriteria);
            return;
        }

        // get parent criteria
        Criteria pCriteria = getContext().getCriteria(lastPropPath);

        // create new criteria and alias for this relationship
        String alias = getContext().addAlias(path, def);
        Criteria criteria = pCriteria.createCriteria(realName, alias);
        getContext().addCriteria(path, criteria);
        //also add virtual path to criteria map
        getContext().addCriteria(virtualPath, criteria);
    }

    protected Criterion createCriterion(String propertyName, Object value, ValueFilter filter) throws QueryException {
        ItemRestrictionOperation operation;
        if (filter instanceof EqualsFilter) {
            operation = ItemRestrictionOperation.EQ;
        } else if (filter instanceof GreaterFilter) {
            GreaterFilter gf = (GreaterFilter) filter;
            operation = gf.isEquals() ? ItemRestrictionOperation.GE : ItemRestrictionOperation.GT;
        } else if (filter instanceof LessFilter) {
            LessFilter lf = (LessFilter) filter;
            operation = lf.isEquals() ? ItemRestrictionOperation.LE : ItemRestrictionOperation.LT;
        } else if (filter instanceof SubstringFilter) {
            operation = ItemRestrictionOperation.SUBSTRING;
        } else {
            throw new QueryException("Can't translate filter '" + filter + "' to operation.");
        }

        QueryContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);

        return matcher.match(operation, propertyName, value, filter.getMatchingRule());
    }

    protected List<Definition> createDefinitionPath(ItemPath path, QueryContext context) throws QueryException {
        List<Definition> definitions = new ArrayList<Definition>();
        if (path == null) {
            return definitions;
        }

        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();

        EntityDefinition lastDefinition = registry.findDefinition(context.getType(), null, EntityDefinition.class);
        for (ItemPathSegment segment : path.getSegments()) {
            if (lastDefinition == null) {
                break;
            }

            if (!(segment instanceof NameItemPathSegment)) {
                continue;
            }

            NameItemPathSegment named = (NameItemPathSegment) segment;
            Definition def = lastDefinition.findDefinition(named.getName(), Definition.class);
            definitions.add(def);

            if (def instanceof EntityDefinition) {
                lastDefinition = (EntityDefinition) def;
            } else {
                lastDefinition = null;
            }
        }

        return definitions;
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
        } else if (filter instanceof StringValueFilter) {
            value = ((StringValueFilter) filter).getValue();
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
}

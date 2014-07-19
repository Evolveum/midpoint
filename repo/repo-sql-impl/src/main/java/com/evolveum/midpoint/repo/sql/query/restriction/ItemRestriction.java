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
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.query.QueryContext;
import com.evolveum.midpoint.repo.sql.query.QueryDefinitionRegistry;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.QueryInterpreter;
import com.evolveum.midpoint.repo.sql.query.definition.*;
import com.evolveum.midpoint.repo.sql.query.matcher.Matcher;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import javax.xml.namespace.QName;

import java.util.*;

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

//        ItemPath path = RUtil.createFullPath(filter);
    	ItemPath path = filter.getFullPath();
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

    private TypeRestriction findTypeRestrictionParent(Restriction restriction) {
        if (restriction == null) {
            return null;
        }

        if (restriction instanceof TypeRestriction) {
            return (TypeRestriction) restriction;
        }

        return findTypeRestrictionParent(restriction.getParent());
    }

    private Set<Class<? extends ObjectType>> findOtherPossibleParents() {
        TypeRestriction typeRestriction = findTypeRestrictionParent(this);
        ObjectTypes typeClass;
        if (typeRestriction != null) {
            TypeFilter filter = typeRestriction.getFilter();
            typeClass = ObjectTypes.getObjectTypeFromTypeQName(filter.getType());
        } else {
            typeClass = ObjectTypes.getObjectType(getContext().getType());
        }

        Set<Class<? extends ObjectType>> classes = new HashSet<>();
        classes.add(typeClass.getClassDefinition());

        switch (typeClass) {
            case OBJECT:
                classes.addAll(ObjectTypes.getAllObjectTypes());
                break;
            case FOCUS_TYPE:
                classes.add(UserType.class);
            case ABSTRACT_ROLE:
                classes.add(RoleType.class);
                classes.add(OrgType.class);
        }

        LOGGER.trace("Found possible parents {} for entity definitions.", Arrays.toString(classes.toArray()));
        return classes;
    }

    protected <T extends Definition> T findProperDefinition(ItemPath path, Class<T> clazz) {
        QueryContext context = getContext();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        if (!ObjectType.class.equals(context.getType())) {
            return registry.findDefinition(context.getType(), path, clazz);
        }

        //we should try to find property in descendant classes
        for (Class type : findOtherPossibleParents()) {
            Definition def = registry.findDefinition(type, path, clazz);
            if (def != null) {
                return (T) def;
            }
        }

        return null;
    }

    protected EntityDefinition findProperEntityDefinition(ItemPath path) {
        QueryContext context = getContext();
        QueryDefinitionRegistry registry = QueryDefinitionRegistry.getInstance();
        if (!ObjectType.class.equals(context.getType())) {
            return registry.findDefinition(context.getType(), null, EntityDefinition.class);
        }

        EntityDefinition entity = null;
        // we should try to find property in descendant classes
        for (Class type : findOtherPossibleParents()) {
            entity = registry.findDefinition(type, null, EntityDefinition.class);
            Definition def = entity.findDefinition(path, Definition.class);
            if (def != null) {
                break;
            }
        }
         LOGGER.trace("Found proper entity definition for path {}, {}", path, entity.toString());
        return entity;
    }

    //todo reimplement, use DefinitionHandlers or maybe another great concept
    private void updateQueryContext(ItemPath path) throws QueryException {
        LOGGER.trace("Updating query context based on path {}", new Object[]{path.toString()});
        EntityDefinition definition =  findProperEntityDefinition(path);

        List<ItemPathSegment> segments = path.getSegments();

        List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();
        ItemPath propPath;
        for (ItemPathSegment segment : segments) {
            QName qname = ItemPath.getName(segment);
            if (ObjectType.F_METADATA.equals(qname)) {
                continue;
            }

            // create new property path
            propPathSegments.add(new NameItemPathSegment(qname));
            propPath = new ItemPath(propPathSegments);
            // get entity query definition
            if (QNameUtil.match(qname, ObjectType.F_EXTENSION) || QNameUtil.match(qname, ShadowType.F_ATTRIBUTES)) {
                break;
            }

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

    /**
     * This method scans {@link ItemPath} in {@link ValueFilter} and looks for virtual properties, collections
     * or entities in {@link QueryDefinitionRegistry}.
     * <p/>
     * Virtual definitions offer additional query params, which can be used for filtering - this method updates
     * criteria based on {@link VirtualQueryParam}. For example assignments and inducements are defined in two
     * collections in schema ({@link com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType}),
     * but in repository ({@link com.evolveum.midpoint.repo.sql.data.common.RAbstractRole}) they are stored in
     * single {@link java.util.Set}.
     * <p/>
     * TODO: implement definition handlers to get rid of these methods with many instanceOf comparisons.
     *
     * @param path
     * @return {@link Criterion} based on {@link VirtualQueryParam}
     * @throws QueryException
     */
    private Criterion createVirtualCriterion(ItemPath path) throws QueryException {
        LOGGER.trace("Scanning path for virtual definitions to create criteria {}", new Object[]{path.toString()});

        EntityDefinition definition = findProperEntityDefinition(path);

        List<Criterion> criterions = new ArrayList<Criterion>();

        List<ItemPathSegment> segments = path.getSegments();
        List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();

        ItemPath propPath;
        for (ItemPathSegment segment : segments) {
            QName qname = ItemPath.getName(segment);
            if (ObjectType.F_METADATA.equals(qname)) {
                continue;
            }
            // create new property path
            propPathSegments.add(new NameItemPathSegment(qname));
            propPath = new ItemPath(propPathSegments);

            if (QNameUtil.match(qname, ObjectType.F_EXTENSION) || QNameUtil.match(qname, ShadowType.F_ATTRIBUTES)) {
                break;
            }

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
    }

    /**
     * This method provides transformation from {@link String} value defined in
     * {@link com.evolveum.midpoint.repo.sql.query.definition.VirtualQueryParam#value()} to real object. Currently only
     * to simple types and enum values.
     *
     * @param param
     * @param propPath
     * @return real value
     * @throws QueryException
     */
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
        if (ItemPath.EMPTY_PATH.equivalent(lastPropPath)) {
            lastPropPath = null;
        }

        String alias = getContext().getAlias(lastPropPath);
        getContext().addAlias(path, alias);
    }

    protected void addNewCriteriaToContext(ItemPath path, Definition def, String realName) {
        ItemPath lastPropPath = path.allExceptLast();
        if (ItemPath.EMPTY_PATH.equivalent(lastPropPath)) {
            lastPropPath = null;
        }

        // Virtual path is defined for example for virtual collections. {c:role/c:assignment} and {c:role/c:iducement}
        // must use the same criteria, therefore {c:role/assigmnents} is also path under which is this criteria saved.
        final ItemPath virtualPath = lastPropPath != null ? new ItemPath(lastPropPath, new QName("", realName)) :
                new ItemPath(new QName("", realName));

        Criteria existing = getContext().getCriteria(path);
        if (existing != null) {
            return;
        }

        // If there is already criteria on virtual path, only add new path to aliases and criterias.
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
        Criteria criteria = pCriteria.createCriteria(realName, alias, JoinType.LEFT_OUTER_JOIN);
        getContext().addCriteria(path, criteria);
        //also add virtual path to criteria map
        getContext().addCriteria(virtualPath, criteria);
    }

    protected Criterion createCriterion(String propertyName, Object value, ValueFilter filter) throws QueryException {
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

        QueryContext context = getContext();
        QueryInterpreter interpreter = context.getInterpreter();
        Matcher matcher = interpreter.findMatcher(value);

        String matchingRule = null;
        if (filter.getMatchingRule() != null){
        	matchingRule = filter.getMatchingRule().getLocalPart();
        }
        
        return matcher.match(operation, propertyName, value, matchingRule);
    }

    protected List<Definition> createDefinitionPath(ItemPath path) throws QueryException {
        List<Definition> definitions = new ArrayList<Definition>();
        if (path == null) {
            return definitions;
        }

        EntityDefinition lastDefinition = findProperEntityDefinition(path);
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
}

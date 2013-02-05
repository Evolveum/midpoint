/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.GreaterFilter;
import com.evolveum.midpoint.prism.query.LessFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.data.common.RAnyConverter;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class SimpleOp extends Op {

	private static enum Operation {
		EQUAL, SUBSTRING, REF, LESS, GREATER
	}

	private static final Trace LOGGER = TraceManager.getTrace(SimpleOp.class);

	public SimpleOp(QueryInterpreter interpreter) {
		super(interpreter);
	}

	private Operation getOperationType(ObjectFilter filterPart) throws QueryException {
		if (filterPart instanceof EqualsFilter) {
			return Operation.EQUAL;
		} else if (filterPart instanceof SubstringFilter) {
			return Operation.SUBSTRING;
		} else if (filterPart instanceof RefFilter) {
			return Operation.REF;
		} else if (filterPart instanceof GreaterFilter) {
			return Operation.GREATER;
		} else if (filterPart instanceof LessFilter) {
			return Operation.LESS;
		}

		throw new QueryException("Unknown filter type '" + filterPart.getClass().getSimpleName() + "'.");
	}

	@Deprecated
	private Operation getOperationType(Element filterPart) throws QueryException {
		if (DOMUtil.isElementName(filterPart, SchemaConstantsGenerated.Q_EQUAL)) {
			return Operation.EQUAL;
		} else if (DOMUtil.isElementName(filterPart, SchemaConstantsGenerated.Q_SUBSTRING)) {
			return Operation.SUBSTRING;
		}

		throw new QueryException("Unknown filter type '" + DOMUtil.getQNameWithoutPrefix(filterPart) + "'.");
	}

	@Override
	public Criterion interpret(ObjectFilter filter, boolean pushNot) throws QueryException {
		LOGGER.trace("Interpreting '{}', pushNot '{}'", new Object[] { filter.getClass().getSimpleName(), pushNot });

		ValueFilter valueFilter = (ValueFilter) filter;
		ItemPath propertyPath = valueFilter.getParentPath();

		if (propertyPath != null) {
			// at first we build criterions with aliases
			updateQueryContext(propertyPath);
		}

		SimpleItem conditionItem = updateConditionItem(valueFilter.getDefinition(), propertyPath);

		LOGGER.trace("Condition item updated, updating value type.");
		Criterion criterion = null;
		if (!conditionItem.isReference) {
			// we're trying to create simple item condition

			Operation operation = getOperationType(valueFilter);
			Object filterValue = null;
			switch (operation) {
			case LESS:
			case GREATER:
			case EQUAL:
				PropertyValueFilter equalFilter = (PropertyValueFilter) valueFilter;
				PrismValue val = equalFilter.getValues().get(0);
				filterValue = ((PrismPropertyValue) val).getValue();
				if (conditionItem.isPolyString) {
					if (filterValue instanceof PolyStringType) {
						filterValue = ((PolyStringType) filterValue).getNorm();
					} else if (filterValue instanceof PolyString){
						filterValue = ((PolyString) filterValue).getNorm();
					}
				}
				// }
				break;
//			case REF:
//				RefFilter refFilter = (RefFilter) valueFilter;
//				filterValue = (PrismReferenceValue) refFilter.getValues().get(0); 
//				break;
			case SUBSTRING:
				SubstringFilter substringFilter = (SubstringFilter) valueFilter;
				filterValue = substringFilter.getValue();
				break;
			}

			Object testedValue = null;
			if (!conditionItem.isEnum) {
				// if it's not an enum, we're just convert condition element
				// from query type to real value
				// for example <c:name>foo<c:name> to String "foo"
				//
				// QName condQName = DOMUtil.getQNameWithoutPrefix(condition);
				// testedValue =
				// RAnyConverter.getRealRepoValue(getInterpreter().findDefinition(path,
				// condQName),
				// condition);
				testedValue = filterValue;
			} else {
				// if it's enum, we convert text content from condition to enum
				// object
				Class<?> type = conditionItem.enumType;
				if (type == null || !type.isEnum()) {
					throw new IllegalStateException("Type '" + type + "' was marked as enum but it is not enum.");
				}
				for (Object obj : type.getEnumConstants()) {
					Enum e = (Enum) obj;
					if (e.name().equals(filterValue)) {
						testedValue = e;
						break;
					}
				}
			}
			
			LOGGER.trace("Value updated to type '{}'", new Object[] { (testedValue == null ? null : testedValue
					.getClass().getName()) });
			if (conditionItem.isQname){
				criterion = interpretQname(conditionItem, (QName) testedValue, criterion);
			}
			criterion = createBaseCriteria(filter, pushNot, conditionItem, testedValue);

		}

		if (conditionItem.isAny) {
			criterion = interpretAny(valueFilter.getDefinition(), conditionItem, criterion);
		} else if (conditionItem.isReference) {
			criterion = interpretReference((PrismReferenceValue) ((RefFilter) valueFilter).getValues().get(0),
					conditionItem, filter, pushNot);	
		}

		return criterion;
	}

	private Criterion createBaseCriteria(ObjectFilter filter, boolean pushNot, SimpleItem item, Object testedValue)
			throws QueryException {
        if (item.isAny) {
            testedValue = RAnyConverter.getAggregatedRepoObject(testedValue);
        }

		// will be used later to support other filters than <equal>
		String name = item.getQueryableItem();
		Operation operation = getOperationType(filter);
		switch (operation) {
		case REF:
		case EQUAL:
			if (pushNot) {
				if (testedValue == null) {
					return (item.isMultiValue ? Restrictions.isNotEmpty(name) : Restrictions.isNotNull(name));
				}
				return Restrictions.ne(name, testedValue);
			} else {
				if (testedValue == null) {
					return (item.isMultiValue ? Restrictions.isEmpty(name) : Restrictions.isNull(name));
				}
				return (testedValue == null ? Restrictions.isEmpty(name) : Restrictions.eq(name, testedValue));
			}
		case SUBSTRING:
			if (pushNot) {
				return Restrictions.not(Restrictions.like(name, "%" + testedValue + "%"));
			} else {
				return Restrictions.like(name, "%" + testedValue + "%").ignoreCase();
			}
		case LESS:
			LessFilter less = (LessFilter) filter;
			if (pushNot) {
				return less.isEquals() ? Restrictions.not(Restrictions.le(name, testedValue)) : Restrictions.not(Restrictions.lt(name, testedValue));
			} else {
				return less.isEquals() ? Restrictions.le(name, testedValue) : Restrictions.lt(name, testedValue);
			}
		case GREATER:
			GreaterFilter greater = (GreaterFilter) filter;
			if (pushNot) {
				return greater.isEquals() ? Restrictions.not(Restrictions.ge(name, testedValue)) : Restrictions.not(Restrictions.gt(name, testedValue));
			} else {
				return greater.isEquals() ? Restrictions.ge(name, testedValue) : Restrictions.gt(name, testedValue);
			}
			
		}

		throw new IllegalStateException("Couldn't create base criteria for filter '"
				+ filter.getClass().getSimpleName() + "' and pushNot '" + pushNot + "'.");
	}

	private Criterion interpretAny(ItemDefinition itemDefinition, SimpleItem conditionItem, Criterion baseCriterion)
			throws QueryException {
		LOGGER.trace("Condition is type of any, creating conjunction for value and QName name, type");

		QName name = itemDefinition.getName();
		QName type = itemDefinition.getTypeName();

		if (name == null || type == null) {
			throw new QueryException("Couldn't get name or type for queried item '" + itemDefinition + "'");
		}

		// when we're interpreting any we have to use base criterion (criteria
		// based on condition element) and
		// add also QName name and type condition
		Conjunction conjunction = Restrictions.conjunction();
		conjunction.add(baseCriterion);
		conjunction.add(Restrictions.eq(conditionItem.alias + ".name", QNameType.optimizeQName(name)));
		conjunction.add(Restrictions.eq(conditionItem.alias + ".type", QNameType.optimizeQName(type)));

		return conjunction;
	}

	private Criterion interpretQname(SimpleItem conditionItem, QName testedValue, Criterion baseCriterion)
			throws QueryException {
		LOGGER.trace("Condition is type of any, creating conjunction for value and QName name, type");

//		QName name = itemDefinition.getName();
//		QName type = itemDefinition.getTypeName();
//
//		if (name == null || type == null) {
//			throw new QueryException("Couldn't get name or type for queried item '" + itemDefinition + "'");
//		}

		// when we're interpreting any we have to use base criterion (criteria
		// based on condition element) and
		// add also QName name and type condition
		Conjunction conjunction = Restrictions.conjunction();
		conjunction.add(baseCriterion);
		conjunction.add(Restrictions.eq(conditionItem.alias + ".localPart", testedValue.getLocalPart()));
		conjunction.add(Restrictions.eq(conditionItem.alias + ".namespace", testedValue.getNamespaceURI()));

		return conjunction;
	}

	
	private Criterion interpretReference(PrismReferenceValue refValue, SimpleItem conditionItem, ObjectFilter filter,
			boolean pushNot) throws QueryException {

		QName type = refValue.getTargetType();
		Criterion criterion = createBaseCriteria(filter, pushNot, conditionItem, refValue.getOid());

		// if reference type is available for reference, we're add another QName
		// criterion for object type
		if (type != null) {
			Conjunction conjunction = Restrictions.conjunction();
			conjunction.add(criterion);
			conjunction.add(Restrictions.eq("type", ClassMapper.getHQLTypeForQName(type)));

			criterion = conjunction;
		}

		return criterion;
	}

	private SimpleItem updateConditionItem(ItemDefinition itemDef, ItemPath path) throws QueryException {
		// QName conditionItem = DOMUtil.getQNameWithoutPrefix(condition);
		QName conditionItem = itemDef.getName();
		LOGGER.trace("Updating condition item '{}' on property path\n{}", new Object[] { conditionItem, path });
		SimpleItem item = new SimpleItem();
		// fetch definition from repository schema registry (not prism schema
		// registry)
		EntityDefinition definition = findDefinition(getInterpreter().getType(), path);

		if (path != null) {
			if (definition.isAny()) {
				try {
					item.isAny = true;
                    List<ItemPathSegment> segments = new ArrayList<ItemPathSegment>();
                    segments.addAll(path.getSegments());
					// get any type name (e.g. clobs, strings, dates,...) based
					// on definition
					String anyTypeName = RAnyConverter.getAnySetType(itemDef);
					segments.add(new NameItemPathSegment(new QName(RUtil.NS_SQL_REPO, anyTypeName)));

					path = new ItemPath(segments);
					LOGGER.trace("Condition item is from 'any' container, adding new criteria based on any type '{}'",
							new Object[] { anyTypeName });
					addNewCriteriaToContext(path, anyTypeName);
				} catch (SchemaException ex) {
					throw new QueryException(ex.getMessage(), ex);
				}
			}

			item.alias = getInterpreter().getAlias(path);
			LOGGER.trace("Found alias '{}' for path.", new Object[] { item.alias });
		}

		if (definition.isAny()) {
			item.item = "value";
		} else {
			Definition def = definition.findDefinition(conditionItem);
			if (def == null) {
				throw new QueryException("Couldn't find query definition for condition item '" + conditionItem + "' in definition for path '"+path+"'");
			}
			if (def.isEntity()) {
				throw new QueryException("Can't query entity for value, only attribute can be queried for value.");
			}

			AttributeDefinition attrDef = (AttributeDefinition) def;
			if (!attrDef.isIndexed()) {
				LOGGER.trace("You're probably querying by attribute ('" + attrDef + "') which is not indexed.");
			}
			
			if (attrDef.isReference()) {
                String realName = attrDef.getJpaName();
                ItemPath propPath = path;
                if (attrDef.isMultiValue()) {
                    if (propPath == null || propPath.isEmpty()) {
                        // used in references from main criteria
                        propPath = new ItemPath(new QName(RUtil.NS_SQL_REPO, realName));
                    }
                    addNewCriteriaToContext(propPath, realName);

                    item.item = "targetOid";
                } else {
                    propPath = null;
                    item.item = realName + ".targetOid";
                }
                item.isReference = true;
                item.alias = getInterpreter().getAlias(propPath);
                LOGGER.trace("Found alias '{}' for path.", new Object[] { item.alias });
			} else {
				item.item = attrDef.getRealName();
				if (attrDef.isPolyString()) {
					item.item += ".norm";
				}
			}

			item.isPolyString = attrDef.isPolyString();
			item.isEnum = attrDef.isEnumerated();
			item.enumType = attrDef.getClassType();
			item.isMultiValue = attrDef.isMultiValue();
			item.isQname = attrDef.isQname();
		}

		return item;
	}

	private EntityDefinition findDefinition(Class<? extends ObjectType> type, ItemPath path) throws QueryException {
		EntityDefinition definition = getClassTypeDefinition(type);
		if (path == null) {
			return definition;
		}

		Definition def;
		for (ItemPathSegment segment : path.getSegments()) {
			def = definition.findDefinition(ItemPath.getName(segment));
			if (!def.isEntity()) {
				throw new QueryException("Can't query attribute in attribute. Full path was '" + path + "'.");
			} else {
				definition = (EntityDefinition) def;
			}
		}

		return definition;
	}

	private EntityDefinition getClassTypeDefinition(Class<? extends ObjectType> type) throws QueryException {
		EntityDefinition definition = QueryRegistry.getInstance().findDefinition(type);
		if (definition == null) {
			throw new QueryException("Can't query, unknown type '" + type.getSimpleName() + "'.");
		}

		return definition;
	}

	/**
	 * Method creates hibernate criteria with aliases based on path which
	 * represents hibernate relationships
	 * 
	 * @param path
	 * @throws QueryException
	 */
	private void updateQueryContext(ItemPath path) throws QueryException {
		LOGGER.trace("Updating query context based on path\n{}", new Object[] { path.toString() });
		Class<? extends ObjectType> type = getInterpreter().getType();
		Definition definition = getClassTypeDefinition(type);

		List<ItemPathSegment> segments = path.getSegments();

		List<ItemPathSegment> propPathSegments = new ArrayList<ItemPathSegment>();
		ItemPath propPath;
		for (ItemPathSegment segment : segments) {
			QName qname = ItemPath.getName(segment);
			// create new property path
			propPathSegments.add(new NameItemPathSegment(qname));
			propPath = new ItemPath(propPathSegments);
			// get entity query definition
			definition = definition.findDefinition(qname);
			if (definition == null || !definition.isEntity()) {
				throw new QueryException("This definition '" + definition + "' is not entity definition, "
						+ "we can't query attribute in attribute. Please check your path in query, or query "
						+ "entity/attribute mappings. Full path was '" + path + "'.");
			}

			EntityDefinition entityDefinition = (EntityDefinition) definition;
			if (entityDefinition.isEmbedded()) {
				// for embedded relationships we don't have to create new
				// criteria
				LOGGER.trace("Skipping segment '{}' because it's embedded, sub path\n{}", new Object[] { qname,
						propPath.toString() });
				continue;
			}

			LOGGER.trace("Adding criteria '{}' to context based on sub path\n{}",
					new Object[] { definition.getRealName(), propPath.toString() });
			addNewCriteriaToContext(propPath, definition.getRealName());
		}
	}

	private void addNewCriteriaToContext(ItemPath propPath, String realName) {
		ItemPath lastPropPath = propPath.allExceptLast();
		if (ItemPath.EMPTY_PATH.equals(lastPropPath)) {
			lastPropPath = null;
		}
		// get parent criteria
		Criteria pCriteria = getInterpreter().getCriteria(lastPropPath);
		// create new criteria for this relationship
		String alias = getInterpreter().createAlias(ItemPath.getName(propPath.last()));
		Criteria criteria = pCriteria.createCriteria(realName, alias);
		// save criteria and alias to our query context
		getInterpreter().setCriteria(propPath, criteria);
		getInterpreter().setAlias(propPath, alias);
	}

	private static class SimpleItem {

		// property for querying
		String item;
		// alias for entity if available
		String alias;
		// condition item type (any, reference, enum, poly string)
		boolean isAny;
		boolean isReference;
		boolean isEnum;
		boolean isPolyString;
		boolean isMultiValue;
		boolean isQname;
		// if condition item is enum type, then this is enum class
		Class<?> enumType;

		String getQueryableItem() {
			StringBuilder builder = new StringBuilder();
			if (StringUtils.isNotEmpty(alias)) {
				builder.append(alias);
				builder.append(".");
			}
			builder.append(item);
			return builder.toString();
		}
	}

}

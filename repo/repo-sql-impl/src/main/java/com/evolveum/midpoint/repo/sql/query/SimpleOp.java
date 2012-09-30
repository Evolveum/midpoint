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
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.PropertyPathSegment;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.data.common.RAnyConverter;
import com.evolveum.midpoint.repo.sql.data.common.RUtil;
import com.evolveum.midpoint.repo.sql.type.QNameType;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
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
		EQUAL, SUBSTRING, REF
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
		LOGGER.debug("Interpreting '{}', pushNot '{}'", new Object[] { filter.getClass().getSimpleName(), pushNot });
		// validate(filter);

		ValueFilter valueFilter = (ValueFilter) filter;
		PropertyPath propertyPath = valueFilter.getPath();

		if (propertyPath != null) {
			// at first we build criterions with aliases
			updateQueryContext(propertyPath);
		}

		//
		// Element value = DOMUtil.getChildElement(filter,
		// SchemaConstantsGenerated.Q_VALUE);
		// if (value == null || DOMUtil.listChildElements(value).isEmpty()) {
		// throw new
		// QueryException("Equal without value element, or without element in <value> not supported now.");
		// }

		// Element condition = DOMUtil.listChildElements(value).get(0);

		SimpleItem conditionItem = updateConditionItem(valueFilter.getDefinition(), propertyPath);

		LOGGER.trace("Condition item updated, updating value type.");
		Criterion criterion = null;
		if (!conditionItem.isReference) {
			// we're trying to create simple item condition

			Operation operation = getOperationType(valueFilter);
			Object filterValue = null;
			switch (operation) {

			case EQUAL:
				EqualsFilter equalFilter = (EqualsFilter) valueFilter;
				PrismValue val = equalFilter.getValues().get(0);
//				if (val instanceof PrismReferenceValue) {
//					filterValue = ((PrismReferenceValue) val).getOid();
//				} else {
					filterValue = ((PrismPropertyValue) val).getValue();
//				}
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
		// will be used later to support other filters than <equal>
		String name = item.getQueryableItem();
		Operation operation = getOperationType(filter);
		switch (operation) {
		case REF:
		case EQUAL:
			if (pushNot) {
				if (testedValue == null){
					return (item.isMultiValue ? Restrictions.isNotEmpty(name):Restrictions.isNotNull(name));
				}
				return Restrictions.ne(name, testedValue);
			} else {
				if (testedValue == null){
					return (item.isMultiValue ? Restrictions.isEmpty(name):Restrictions.isNull(name));
				}
				return (testedValue == null ? Restrictions.isEmpty(name):Restrictions.eq(name, testedValue));
			}
		case SUBSTRING:
			if (pushNot) {
				return Restrictions.not(Restrictions.like(name, "%" + testedValue + "%"));
			} else {
				return Restrictions.like(name, "%" + testedValue + "%").ignoreCase();
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

	private SimpleItem updateConditionItem(ItemDefinition itemDef, PropertyPath path) throws QueryException {
		// QName conditionItem = DOMUtil.getQNameWithoutPrefix(condition);
		QName conditionItem = itemDef.getName();
		LOGGER.debug("Updating condition item '{}' on property path\n{}", new Object[] { conditionItem, path });
		SimpleItem item = new SimpleItem();
		// fetch definition from repository schema registry (not prism schema
		// registry)
		EntityDefinition definition = findDefinition(getInterpreter().getType(), path);

		if (path != null) {
			if (definition.isAny()) {
				try {
					item.isAny = true;
					List<PropertyPathSegment> segments = path.getSegments();
					// get any type name (e.g. clobs, strings, dates,...) based
					// on definition
					String anyTypeName = RAnyConverter.getAnySetType(itemDef);
					segments.add(new PropertyPathSegment(new QName(RUtil.NS_SQL_REPO, anyTypeName)));

					path = new PropertyPath(segments);
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
				throw new QueryException("Couldn't find query definition for condition item '" + conditionItem + "'.");
			}
			if (def.isEntity()) {
				throw new QueryException("Can't query entity for value, only attribute can be queried for value.");
			}

			AttributeDefinition attrDef = (AttributeDefinition) def;
			if (!attrDef.isIndexed()) {
				LOGGER.debug("You're probably querying by attribute ('" + attrDef + "') which is not indexed.");
			}
			
			if (attrDef.isReference()) {
				PropertyPath propPath = path;
				String realName = attrDef.getRealName();
				if (propPath == null || propPath.isEmpty()) {
					// used in references from main criteria
					propPath = new PropertyPath(new QName(RUtil.NS_SQL_REPO, realName));
				}
				
					addNewCriteriaToContext(propPath, realName);
				
				item.isReference = true;
				item.alias = getInterpreter().getAlias(propPath);
				LOGGER.trace("Found alias '{}' for path.", new Object[] { item.alias });
				item.item = "targetOid";
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
		}

		return item;
	}

	private EntityDefinition findDefinition(Class<? extends ObjectType> type, PropertyPath path) throws QueryException {
		EntityDefinition definition = getClassTypeDefinition(type);
		if (path == null) {
			return definition;
		}

		Definition def;
		for (PropertyPathSegment segment : path.getSegments()) {
			def = definition.findDefinition(segment.getName());
			if (!def.isEntity()) {
				throw new QueryException("Can't query attribute in attribute.");
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
	private void updateQueryContext(PropertyPath path) throws QueryException {
		LOGGER.debug("Updating query context based on path\n{}", new Object[] { path.toString() });
		Class<? extends ObjectType> type = getInterpreter().getType();
		Definition definition = getClassTypeDefinition(type);

		List<PropertyPathSegment> segments = path.getSegments();

		List<PropertyPathSegment> propPathSegments = new ArrayList<PropertyPathSegment>();
		PropertyPath propPath;
		for (PropertyPathSegment segment : segments) {
			QName qname = segment.getName();
			// create new property path
			propPathSegments.add(new PropertyPathSegment(qname));
			propPath = new PropertyPath(propPathSegments);
			// get entity query definition
			definition = definition.findDefinition(qname);
			if (definition == null || !definition.isEntity()) {
				throw new QueryException("This definition '" + definition + "' is not entity definition, "
						+ "we can't query attribute in attribute. Please check your path in query, or query "
						+ "entity/attribute mappings.");
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

	private void addNewCriteriaToContext(PropertyPath propPath, String realName) {
		PropertyPath lastPropPath = propPath.allExceptLast();
		if (PropertyPath.EMPTY_PATH.equals(lastPropPath)) {
			lastPropPath = null;
		}
		// get parent criteria
		Criteria pCriteria = getInterpreter().getCriteria(lastPropPath);
		// create new criteria for this relationship
		String alias = getInterpreter().createAlias(propPath.last().getName());
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

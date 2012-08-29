package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

public class QueryConvertor {

	public static ObjectQuery createObjectQuery(Class clazz, QueryType queryType, PrismContext prismContext)
			throws SchemaException {

		Element criteria = queryType.getFilter();

		if (criteria == null) {
			return null;
		}

		PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("cannot find obj definition");
		}

		try {
			ObjectFilter filter = parseFilter(objDef, criteria);
			ObjectQuery query = new ObjectQuery();
			query.setFilter(filter);
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

	public static QueryType createQueryType(ObjectQuery query) {

		ObjectFilter filter = query.getFilter();
		Document doc = DOMUtil.getDocument();
		Element filterType = createFilterType(filter, doc);
		QueryType queryType = new QueryType();
		queryType.setFilter(filterType);

		return queryType;

	}

	private static Element createFilterType(ObjectFilter filter, Document doc) {

		if (filter instanceof AndFilter) {
			return createAndFilterType((AndFilter) filter, doc);
		}
		if (filter instanceof OrFilter) {
			return createOrFilterType((OrFilter) filter, doc);
		}
		if (filter instanceof NotFilter) {
			return createNotFilterType((NotFilter) filter, doc);
		}
		if (filter instanceof EqualsFilter) {
			return createEqualsFilterType((EqualsFilter) filter, doc);
		}

		if (filter instanceof SubstringFilter) {
			return createSubstringFilterType((SubstringFilter) filter, doc);
		}

		if (filter instanceof OrgFilter) {
			return createOrgFilterType((OrgFilter) filter, doc);
		}

		throw new UnsupportedOperationException("Unsupported filter type: " + filter);
	}

	private static Element createAndFilterType(AndFilter filter, Document doc) {

		Element and = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_AND);

		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc);
			and.appendChild(element);
		}
		return and;
	}

	private static Element createOrFilterType(OrFilter filter, Document doc) {

		Element or = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_OR);
		for (ObjectFilter of : filter.getCondition()) {
			Element element = createFilterType(of, doc);
			or.appendChild(element);
		}
		return or;
	}

	private static Element createNotFilterType(NotFilter filter, Document doc) {

		Element not = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_NOT);

		Element element = createFilterType(filter.getFilter(), doc);
		not.appendChild(element);
		return not;
	}

	private static Element createEqualsFilterType(EqualsFilter filter, Document doc) {

		Element equal = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_EQUAL);
		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
		equal.appendChild(value);

		if (filter.getPath() != null) {
			Element path = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_PATH);
			XPathHolder xpath = new XPathHolder(filter.getPath());
			path.setTextContent(xpath.getXPath());
		}

		QName propertyName = filter.getDefinition().getName();
		for (PrismValue val : filter.getValues()) {
			Element propValue = DOMUtil.createElement(doc, propertyName);
			if (val instanceof PrismReferenceValue) {
				propValue.setTextContent(((PrismReferenceValue) val).getOid());
			} else {
				propValue.setTextContent(String.valueOf(((PrismPropertyValue) val).getValue()));
			}
			value.appendChild(propValue);
		}
		return equal;
	}

	private static Element createSubstringFilterType(SubstringFilter filter, Document doc) {
		Element substring = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_SUBSTRING);
		Element value = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_VALUE);
		substring.appendChild(value);

		if (filter.getPath() != null) {
			Element path = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_PATH);
			XPathHolder xpath = new XPathHolder(filter.getPath());
			path.setTextContent(xpath.getXPath());
		}

		QName propertyName = filter.getDefinition().getName();
		String val = filter.getValue();

		Element propValue = DOMUtil.createElement(doc, propertyName);
		propValue.setTextContent(val);

		value.appendChild(propValue);

		return substring;
	}

	private static Element createOrgFilterType(OrgFilter filter, Document doc) {
		Element org = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG);

		Element orgRef = null;
		if (filter.getOrgRef() != null) {
			orgRef = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_ORG_REF);
			orgRef.setAttribute("oid", filter.getOrgRef().getOid());
			org.appendChild(orgRef);
		}

		Element minDepth = null;
		if (filter.getMinDepth() != null) {
			minDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MIN_DEPTH);
			minDepth.setTextContent(filter.getMinDepth());
			org.appendChild(minDepth);
		}

		Element maxDepth = null;
		if (filter.getMaxDepth() != null) {
			maxDepth = DOMUtil.createElement(doc, SchemaConstantsGenerated.Q_MAX_DEPTH);
			maxDepth.setTextContent(filter.getMaxDepth());
			org.appendChild(maxDepth);
		}

		return org;
	}

	public static ObjectFilter parseFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_AND, filter)) {
			return createAndFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_EQUAL, filter)) {
			return createEqualFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_SUBSTRING, filter)) {
			return createSubstringFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_ORG, filter)) {
			return createOrgFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_OR, filter)) {
			return createOrFilter(pcd, filter);
		}

		if (QNameUtil.compareQName(SchemaConstantsGenerated.Q_NOT, filter)) {
			return createNotFilter(pcd, filter);
		}

		throw new UnsupportedOperationException("Unsupported query filter " + DOMUtil.printDom(filter));

	}

	private static AndFilter createAndFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}

		return AndFilter.createAnd(objectFilters);
	}

	private static OrFilter createOrFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		List<ObjectFilter> objectFilters = new ArrayList<ObjectFilter>();
		for (Element node : DOMUtil.listChildElements(filter)) {
			ObjectFilter objectFilter = parseFilter(pcd, node);
			objectFilters.add(objectFilter);
		}
		return OrFilter.createOr(objectFilters);
	}

	private static NotFilter createNotFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
//		NodeList filters = filter.getChildNodes();
		List<Element> filters = DOMUtil.listChildElements(filter);

		if (filters.size() < 1) {
			throw new SchemaException("NOT filter does not contain any values specified");
		}

		if (filters.size() > 1) {
			throw new SchemaException(
					"NOT filter can have only one value specified. For more value use OR/AND filter as a parent.");
		}

		ObjectFilter objectFilter = parseFilter(pcd, filters.get(0));
		return NotFilter.createNot(objectFilter);
	}

	private static EqualsFilter createEqualFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {
		
		PropertyPath path = getPath((Element) filter);

		List<Element> values = getValues(filter);
	

		Item item = getItem(values, pcd, path);
		ItemDefinition itemDef = item.getDefinition();
		if (itemDef == null) {
			throw new SchemaException("Item definition for property " + item.getName() + " in container definition " + pcd
					+ " not found.");
		}

		if (item.getValues().size() < 1) {
			throw new IllegalStateException("No values to search specified for item " + itemDef);
		}

		if (itemDef.isSingleValue()) {
			if (item.getValues().size() > 1) {
				throw new IllegalStateException("Single value property "+itemDef.getName()+"should have specified only one value.");
			}

			if (itemDef instanceof PrismReferenceDefinition) {
				return EqualsFilter.createReferenceEqual(path, itemDef, (PrismReferenceValue) item.getValues().get(0));
			}

			return EqualsFilter.createEqual(path, itemDef, item.getValues());
		}

		// TODO: support for multivalue reference
		if (itemDef instanceof PrismReferenceDefinition) {
			// TODO:
			if (item.getValues().size() == 1) {
				return EqualsFilter.createReferenceEqual(path, itemDef, (PrismReferenceValue)item.getValue(0));
			}
		}

		return EqualsFilter.createEqual(path, itemDef, item.getValues());

	}

	private static Item getItem(List<Element> values, PrismContainerDefinition pcd,
			PropertyPath path) throws SchemaException {
		
		if (path != null) {
			pcd = pcd.findContainerDefinition(path);
		}

		List<Object> objects = new ArrayList<Object>();
		objects.addAll(values);
		
		Collection<Item> items = pcd.getPrismContext().getPrismDomProcessor().parseContainerItems(pcd, objects);

		if (items.size() > 1) {
			throw new SchemaException("Expected presence of a single item (path " + path
					+ ") in a object modification, but found " + items.size() + " instead");
		}
		if (items.size() < 1) {
			throw new SchemaException("Expected presence of a value (path " + path
					+ ") in a object modification, but found nothing");
		}

		
		return  items.iterator().next();
		
	}

	private static PropertyPath getPath(Element filter) {
		Element path = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_PATH);
		XPathHolder xpath = new XPathHolder((Element) path);
		return xpath.toPropertyPath();
	}

	private static List<Element> getValues(Node filter) {
		Element value = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_VALUE);
		return DOMUtil.listChildElements(value);
	}

	private static SubstringFilter createSubstringFilter(PrismContainerDefinition pcd, Node filter)
			throws SchemaException {

		PropertyPath path = getPath((Element) filter);
		List<Element> values = getValues(filter);

		if (values.size() > 1) {
			throw new SchemaException("More than one value specified in substring filter.");
		}

		if (values.size() < 1) {
			throw new SchemaException("No value specified in substring filter.");
		}

		Item item = getItem(values, pcd, path);
		ItemDefinition itemDef = item.getDefinition();

		String substring = values.get(0).getTextContent();

		return SubstringFilter.createSubstring(path, itemDef, substring);
	}

	private static OrgFilter createOrgFilter(PrismContainerDefinition pcd, Node filter) throws SchemaException {

		Element orgRef = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_ORG_REF);

		if (orgRef == null) {
			throw new SchemaException("No organization refenrence defined in the search query.");
		}

		String orgOid = orgRef.getAttribute("oid");

		if (orgOid == null || StringUtils.isBlank(orgOid)) {
			throw new SchemaException("No oid attribute defined in the organization reference element.");
		}

		Element minDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MIN_DEPTH);

		String min = null;
		if (minDepth != null) {
			min = minDepth.getTextContent();
		}

		Element maxDepth = DOMUtil.getChildElement((Element) filter, SchemaConstantsGenerated.Q_MAX_DEPTH);
		String max = null;
		if (maxDepth != null) {
			max = maxDepth.getTextContent();
		}

		return OrgFilter.createOrg(orgOid, min, max);
	}

}

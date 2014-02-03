/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.schema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.PropertyValueFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * This is mostly legacy converter between JAXB/DOM representation of queries and filter and the native prism
 * representation. It is used only by the code that has to deal with JAXB/DOM (such as JAX-WS web service).
 * 
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 */
public class QueryJaxbConvertor {

	public static <O extends ObjectType> ObjectQuery createObjectQuery(Class<O> clazz, QueryType queryType, PrismContext prismContext)
			throws SchemaException {

		if (queryType == null){
			return null;
		}
		
		Element filterDom = queryType.getFilter();
		if (filterDom == null && queryType.getPaging() == null){
			return null;
		}
		
		PrismObjectDefinition<O> objDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);

		if (objDef == null) {
			throw new SchemaException("cannot find obj definition for class "+clazz);
		}

		try {
			ObjectQuery query = new ObjectQuery();
			
			Object condition = queryType.getCondition();
			if (condition != null){
				if (!(condition instanceof Element)){
					throw new SchemaException("Bad condition specified.");
				}
				Element conditionElement = (Element) condition;
				XNode conditionXNode = prismContext.getParserDom().parseElement(conditionElement);
				query.setCondition(conditionXNode);
			}
			
			if (filterDom != null) {
				XNode filterXNode = prismContext.getParserDom().parseElement(filterDom);
				ObjectFilter filter = QueryConvertor.parseFilter(filterXNode, prismContext);
				query.setFilter(filter);
			}

			if (queryType.getPaging() != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(queryType.getPaging());
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

	public static QueryType createQueryType(ObjectQuery query, PrismContext prismContext) throws SchemaException{

		ObjectFilter filter = query.getFilter();
		try{
			QueryType queryType = new QueryType();
			if (filter != null){
				MapXNode filterXnode = QueryConvertor.serializeFilter(filter, prismContext);
				Element filterElement = prismContext.getParserDom().serializeToElement(filterXnode, QueryConvertor.FILTER_ELEMENT_NAME);
				queryType.setFilter(filterElement);
			}
		
		
		queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
		queryType.setCondition(query.getCondition());
		return queryType;
		} catch (SchemaException ex){
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}

}

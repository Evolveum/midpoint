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

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

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

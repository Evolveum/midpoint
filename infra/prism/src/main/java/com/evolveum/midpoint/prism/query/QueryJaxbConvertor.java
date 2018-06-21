/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.query;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * This is mostly legacy converter between JAXB/DOM representation of queries and filter and the native prism
 * representation. It is used only by the code that has to deal with JAXB/DOM (such as JAX-WS web service).
 * 
 * @author Katka Valalikova
 * @author Radovan Semancik
 *
 */
public class QueryJaxbConvertor {

	public static ObjectQuery createTypeObjectQuery(QueryType queryType, PrismContext prismContext) throws SchemaException{
		if (queryType == null){
			return null;
		}
		
		if (queryType.getFilter() == null){
			return null;
		}
		
		if (queryType.getFilter().containsFilterClause()){
			MapXNode mapXnode = queryType.getFilter().getFilterClauseXNode();
			QName type = mapXnode.getParsedPrimitiveValue(QueryConvertor.ELEMENT_TYPE, DOMUtil.XSD_QNAME);
			if (type == null){
				throw new SchemaException("Query does not countain type filter. Cannot by parse.");
			}
			
			Class clazz = prismContext.getSchemaRegistry().determineCompileTimeClass(type);
			if (clazz == null){
				PrismObjectDefinition objDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
				if (objDef != null){
					clazz = objDef.getCompileTimeClass();
				}
			}
			
			if (clazz == null){
				throw new SchemaException("Type defined in query is not valid. " + type);
			}
			
			return createObjectQuery(clazz, queryType, prismContext);
			
		}
		return null;
	}
	
    public static <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, QueryType queryType, PrismContext prismContext)
            throws SchemaException {
        if (queryType == null) {
            return null;
        }
        return createObjectQueryInternal(clazz, queryType.getFilter(), queryType.getPaging(), prismContext);
    }

    public static <O extends Objectable> ObjectQuery createObjectQuery(Class<O> clazz, SearchFilterType filterType, PrismContext prismContext)
            throws SchemaException {
        return createObjectQueryInternal(clazz, filterType, null, prismContext);
    }

    public static <O extends Objectable> ObjectFilter createObjectFilter(Class<O> clazz, SearchFilterType filterType, PrismContext prismContext)
            throws SchemaException {
        ObjectQuery query = createObjectQueryInternal(clazz, filterType, null, prismContext);
        if (query == null) {
            return null;
        } else {
            return query.getFilter();
        }
    }
    
    public static <O extends Objectable> ObjectFilter createObjectFilter(PrismObjectDefinition<O> objectDefinition, SearchFilterType filterType, PrismContext prismContext)
            throws SchemaException {
        ObjectQuery query = createObjectQueryInternal(objectDefinition, filterType, null, prismContext);
        if (query == null) {
            return null;
        } else {
            return query.getFilter();
        }
    }
    
    public static <O extends Containerable> ObjectQuery createObjectQueryInternal(Class<O> clazz, SearchFilterType filterType, PagingType pagingType, PrismContext prismContext)
			throws SchemaException {
    	PrismContainerDefinition<O> objDef = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass(clazz);
		if (objDef == null) {
			throw new SchemaException("cannot find obj/container definition for class "+clazz);
		}
		return createObjectQueryInternal(objDef, filterType, pagingType, prismContext);
    }

    public static <O extends Containerable> ObjectQuery createObjectQueryInternal(PrismContainerDefinition<O> objDef, SearchFilterType filterType, PagingType pagingType, PrismContext prismContext)
			throws SchemaException {

		try {
			ObjectQuery query = new ObjectQuery();
			
			if (filterType != null && filterType.containsFilterClause()) {
				MapXNode rootFilter = filterType.getFilterClauseXNode();
				ObjectFilter filter = QueryConvertor.parseFilter(rootFilter, objDef);
				query.setFilter(filter);
			}

			if (pagingType != null) {
				ObjectPaging paging = PagingConvertor.createObjectPaging(pagingType);
				query.setPaging(paging);
			}
			return query;
		} catch (SchemaException ex) {
			throw new SchemaException("Failed to convert query. Reason: " + ex.getMessage(), ex);
		}

	}
	
    public static QueryType createQueryType(ObjectQuery query, PrismContext prismContext) throws SchemaException {
		ObjectFilter filter = query.getFilter();
		QueryType queryType = new QueryType();
		if (filter != null) {
			queryType.setFilter(createSearchFilterType(filter, prismContext));
		}
		queryType.setPaging(PagingConvertor.createPagingType(query.getPaging()));
		return queryType;
	}
    
    public static SearchFilterType createSearchFilterType(ObjectFilter filter, PrismContext prismContext) throws SchemaException {
		if (filter == null) {
			return null;
		}
		SearchFilterType filterType = new SearchFilterType();
        MapXNode filterXNode = QueryConvertor.serializeFilter(filter, prismContext);
		filterType.setFilterClauseXNode(filterXNode);
		return filterType;
	}

}

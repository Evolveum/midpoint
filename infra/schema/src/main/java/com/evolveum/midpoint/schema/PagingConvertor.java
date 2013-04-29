package com.evolveum.midpoint.schema;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.prism.xml.ns._public.query_2.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_2.PagingType;



public class PagingConvertor {
	
	public static ObjectPaging createObjectPaging(PagingType pagingType){
		if (pagingType == null){
			return null;
		}
		
		QName orderBy = null;
		if (pagingType.getOrderBy() != null){
			XPathHolder xpath = new XPathHolder(pagingType.getOrderBy());
			orderBy = ItemPath.getName(xpath.toItemPath().first());
		}
		
		return ObjectPaging.createPaging(pagingType.getOffset(), pagingType.getMaxSize(), orderBy, toOrderDirection(pagingType.getOrderDirection()));
		
	}

	
	private static OrderDirection toOrderDirection(OrderDirectionType directionType){
		if (directionType == null){
			return null;
		}
		
		if (OrderDirectionType.ASCENDING == directionType){
			return OrderDirection.ASCENDING;
		}
		if (OrderDirectionType.DESCENDING == directionType){
			return OrderDirection.DESCENDING;
		}
		return null;
	}
}

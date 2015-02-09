package com.evolveum.midpoint.prism.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathUtil {
	
	private ItemPathUtil(){
		
	}
	
	public static boolean isDefault(ItemPathType itemPathType){
		if (itemPathType == null){
			return true;
		}
		
		ItemPath itemPath = itemPathType.getItemPath();
		
		if (itemPath == null){
			return true;
		}
		
		return itemPath.isEmpty();
	}

	public static QName getOnlySegmentQName(ItemPathType pathType) {
		if (pathType == null) {
			return null;
		}
		return getOnlySegmentQName(pathType.getItemPath());
	}

	public static QName getOnlySegmentQName(ItemPath path) {
		if (path == null) {
			return null;
		}
		if (path.size() != 1) {
			throw new IllegalArgumentException("Expected a single-segment path, bug got "+path);
		}
		ItemPathSegment segment = path.first();
		if (segment instanceof NameItemPathSegment) {
			return ((NameItemPathSegment)segment).getName();
		} else {
			throw new IllegalArgumentException("Expected a path with a name segment, bug got "+path);
		}
	}

}

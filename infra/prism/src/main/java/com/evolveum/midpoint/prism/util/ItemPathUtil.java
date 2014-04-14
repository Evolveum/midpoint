package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.prism.xml.ns._public.types_2.ItemPathType;

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

}

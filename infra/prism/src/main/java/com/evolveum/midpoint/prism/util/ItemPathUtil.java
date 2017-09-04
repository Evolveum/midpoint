package com.evolveum.midpoint.prism.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.util.Map;

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

	// TODO consider requiring path to be non-null
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

	public static QName getOnlySegmentQNameRobust(ItemPathType pathType) {
		if (pathType == null) {
			return null;
		}
		return getOnlySegmentQNameRobust(pathType.getItemPath());
	}

	public static QName getOnlySegmentQNameRobust(ItemPath path) {
		if (path == null || path.size() != 1) {
			return null;
		}
		ItemPathSegment segment = path.first();
		if (segment instanceof NameItemPathSegment) {
			return ((NameItemPathSegment)segment).getName();
		} else {
			return null;
		}
	}

	public static <T> T putToMap(Map<ItemPath, T> map, ItemPath itemPath, T value) {
		for (ItemPath key : map.keySet()) {
			if (key.equivalent(itemPath)) {
				return map.put(key, value);
			}
		}
		return map.put(itemPath, value);
	}

	public static <T> void putAllToMap(Map<ItemPath, T> target, Map<ItemPath, T> source) {
		for (Map.Entry<ItemPath, T> entry : source.entrySet()) {
			putToMap(target, entry.getKey(), entry.getValue());
		}
	}

	public static <T> T getFromMap(Map<ItemPath, T> map, ItemPath itemPath) {
		for (Map.Entry<ItemPath, T> entry : map.entrySet()) {
			if (entry.getKey().equivalent(itemPath)) {
				return entry.getValue();
			}
		}
		return null;
	}
}

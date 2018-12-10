package com.evolveum.midpoint.prism.util;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ItemPathTypeUtil {

	@NotNull
	public static ItemName asSingleNameOrFail(@NotNull ItemPathType pathType) {
		return pathType.getItemPath().asSingleNameOrFail();
	}

	// todo consider what to do with this one
	@Nullable
	public static ItemName asSingleNameOrFailNullSafe(@Nullable ItemPathType pathType) {
		return pathType != null ? pathType.getItemPath().asSingleNameOrFail() : null;
	}

	public static QName asSingleName(ItemPathType pathType) {
		return pathType != null ? pathType.getItemPath().asSingleName() : null;
	}

	public static boolean isEmpty(ItemPathType pathType) {
		return pathType == null || ItemPath.isEmpty(pathType.getItemPath());
	}
}

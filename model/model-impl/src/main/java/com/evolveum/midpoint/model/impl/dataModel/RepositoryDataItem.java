package com.evolveum.midpoint.model.impl.dataModel;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class RepositoryDataItem extends DataItem {

	private static final String COLOR_USER = "darkred";
	private static final String COLOR_ROLE = "darkgreen";
	private static final String COLOR_ORG = "darkorange";
	private static final String COLOR_DEFAULT = "black";
	private static final String COLOR_FILL = "grey92";

	@NotNull private final QName typeName;
	@NotNull private final ItemPath itemPath;

	private PrismObjectDefinition<?> objectDefinition;

	public RepositoryDataItem(@NotNull QName typeName, @NotNull ItemPath itemPath) {
		this.typeName = typeName;
		if (itemPath.isEmpty()) {
			throw new IllegalArgumentException("Empty item path");
		}
		this.itemPath = itemPath;
	}

	@NotNull
	public QName getTypeName() {
		return typeName;
	}

	@NotNull
	public ItemPath getItemPath() {
		return itemPath;
	}

	public PrismObjectDefinition<?> getObjectDefinition() {
		return objectDefinition;
	}

	public void setObjectDefinition(PrismObjectDefinition<?> objectDefinition) {
		this.objectDefinition = objectDefinition;
	}

	public boolean matches(@NotNull QName typeName, @NotNull ItemPath path) {
		return QNameUtil.match(this.typeName, typeName) && itemPath.equivalent(path);
	}

	@Override
	public String getNodeName() {
		return "\"" + typeName.getLocalPart() + "." + itemPath + "\"";
	}

	@Override
	public String getNodeLabel() {
		String entity = StringUtils.removeEnd(typeName.getLocalPart(), "Type");
		String pathString = itemPath.toString();
		final String EXT = "extension/";
		if (pathString.startsWith(EXT)) {
			entity += " extension";
			pathString = pathString.substring(EXT.length());
		}
		return entity + "&#10;" + pathString;
	}

	@Override
	public String getNodeStyleAttributes() {
		return "style=filled, fillcolor=" + COLOR_FILL + ", color=" + getBorderColor();
	}

	private String getBorderColor() {
		if (QNameUtil.match(UserType.COMPLEX_TYPE, typeName)) {
			return COLOR_USER;
		} else if (QNameUtil.match(RoleType.COMPLEX_TYPE, typeName)) {
			return COLOR_ROLE;
		} else if (QNameUtil.match(OrgType.COMPLEX_TYPE, typeName)) {
			return COLOR_ORG;
		} else {
			return COLOR_DEFAULT;
		}
	}
}

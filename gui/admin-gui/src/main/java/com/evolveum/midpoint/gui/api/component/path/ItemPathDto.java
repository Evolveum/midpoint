package com.evolveum.midpoint.gui.api.component.path;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

public class ItemPathDto implements Serializable{
	private static final long serialVersionUID = 1L;

	private  QName objectType = FocusType.COMPLEX_TYPE;

	private ItemPathDto parentPath;

	private ItemDefinition<?> itemDef;

	private ItemPath path;

	public ItemPathDto() {
		// TODO Auto-generated constructor stub
	}

	public ItemPathDto(ItemPathType itemPathType) {
		if (itemPathType == null) {
			return;
		}
		this.path = itemPathType.getItemPath();
	}
	
	public ItemPathDto(ItemPathDto parentPath) {
		this.parentPath = parentPath;
		this.path = parentPath.toItemPath();
	}


	public QName getObjectType() {
		return objectType;
	}

	public void setObjectType(QName objectType) {
		this.objectType = objectType;
	}

	public ItemDefinition<?> getItemDef() {
		return itemDef;
	}

	public void setItemDef(ItemDefinition<?> itemDef) {
		if (parentPath == null) {
			this.path = itemDef.getName();
		} else {
			this.path = parentPath.toItemPath().append(itemDef.getName());
		}
		this.itemDef = itemDef;
	}

	public ItemPathDto getParentPath() {
		return parentPath;
	}

	public void setParentPath(ItemPathDto parentPath) {
		this.parentPath = parentPath;
	}

	public ItemPath toItemPath() {
		if (parentPath == null) {
			if (itemDef == null) {
				return path;
			}
			this.path = itemDef.getName();
		} else {
			if (itemDef == null) {
				return parentPath.toItemPath();
			}
			this.path = parentPath.toItemPath().append(itemDef.getName());
		}
		return path;

	}
	
	public boolean isPathDefined() {
		return (path != null && itemDef == null && parentPath == null);
	}



}

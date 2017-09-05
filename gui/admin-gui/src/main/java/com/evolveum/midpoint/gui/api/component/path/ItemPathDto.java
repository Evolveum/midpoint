package com.evolveum.midpoint.gui.api.component.path;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

public class ItemPathDto implements Serializable{
	private static final long serialVersionUID = 1L;

	private  QName objectType;

	private ItemPathDto parentPath;

	private ItemDefinition<?> itemDef;

	private ItemPath path;

	public ItemPathDto() {
		// TODO Auto-generated constructor stub
	}

	public ItemPathDto(ItemPathDto parentPath) {
		this.parentPath = parentPath;
		this.path = parentPath.toItemPath();
//		this.parent = parentPath.toItemPath();
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
			this.path = new ItemPath(itemDef.getName());
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
			this.path = new ItemPath(itemDef.getName());
		} else {
			if (itemDef == null) {
				return parentPath.toItemPath();
			}
			this.path = parentPath.toItemPath().append(itemDef.getName());
		}
		return path;

	}



}

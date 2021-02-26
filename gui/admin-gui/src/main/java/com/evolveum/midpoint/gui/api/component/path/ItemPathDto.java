/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
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

    private String pathStringValue;

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
            this.path = itemDef.getItemName();
        } else {
            this.path = parentPath.toItemPath().append(itemDef.getItemName());
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
            this.path = itemDef.getItemName();
        } else {
            if (itemDef == null) {
                return parentPath.toItemPath();
            }
            this.path = parentPath.toItemPath().append(itemDef.getItemName());
        }
        return path;

    }

    public boolean isPathDefined() {
        return (path != null && !path.isEmpty() && itemDef == null && parentPath == null);
    }

    public String getPathStringValue(){
        return pathStringValue;
    }

    public void setPathStringValue(String pathStringValue){
        this.pathStringValue = pathStringValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ObjectType: [").append("], Parent: [").append(parentPath).append("], ItemDef: [")
        .append(getItemDef()).append("], Path: [").append(path).append("] }");
        return sb.toString();
    }

}

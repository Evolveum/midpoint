/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class RepositoryDataItem extends DataItem {

    @NotNull protected final QName typeName;
    @NotNull protected final ItemPath itemPath;

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

}

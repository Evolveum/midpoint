/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableDefinition;

import javax.xml.namespace.QName;
import java.io.Serializable;

public abstract class DefinitionDto<D extends MutableDefinition> implements Serializable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_DISPLAY_ORDER = "displayOrder";
    public static final String F_TYPE = "type";

    private String displayName;
    private Integer displayOrder;
    private QName type;

    private D originalDefinition;


     public DefinitionDto(D definition) {
         this.displayName = definition.getDisplayName();
         this.displayOrder = definition.getDisplayOrder();
         this.type = definition.getTypeName();

         this.originalDefinition = definition;
     }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        this.originalDefinition.setDisplayName(displayName);
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
        this.originalDefinition.setDisplayOrder(displayOrder);
    }

//    public D getEditedDefinition() {
//        MutableDefinition mutableDefinition = originalDefinition.clone().toMutable();
//        mutableDefinition.setDisplayName(displayName);
//        mutableDefinition.setDisplayOrder(displayOrder);
//        return (D) mutableDefinition;
//    }

    public D getOriginalDefinition() {
        return originalDefinition;
    }

}

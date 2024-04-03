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

public abstract class DefinitionDto<D extends Definition> implements Serializable {

    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_DISPLAY_ORDER = "displayOrder";
    public static final String F_TYPE = "type";

    private QName type;

    private D originalDefinition;


     public DefinitionDto(D definition) {
         this.type = definition.getTypeName();

         this.originalDefinition = definition;
     }

    public String getDisplayName() {
        return originalDefinition.getDisplayName();
    }

    public Integer getDisplayOrder() {
        return originalDefinition.getDisplayOrder();
    }

    public void setDisplayName(String displayName) {
         if (originalDefinition instanceof MutableDefinition) {
             ((MutableDefinition) originalDefinition).setDisplayName(displayName);
         }
    }

    public void setDisplayOrder(Integer displayOrder) {
         if (originalDefinition instanceof MutableDefinition) {
             ((MutableDefinition) originalDefinition).setDisplayOrder(displayOrder);
         }
    }

    public D getOriginalDefinition() {
        return originalDefinition;
    }

}

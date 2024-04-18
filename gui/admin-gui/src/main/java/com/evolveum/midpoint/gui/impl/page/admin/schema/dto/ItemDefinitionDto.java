/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;

import org.apache.commons.lang3.BooleanUtils;

import java.io.Serializable;

public class ItemDefinitionDto<ID extends ItemDefinition> extends DefinitionDto<ID> implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_MIN_OCCURS = "minOccurs";
    public static final String F_MAX_OCCURS = "maxOccurs";
    public static final String F_MIN_MAX_OCCURS = "minMaxOccurs";
    public static final String F_INDEXED = "indexed";

//    private String name;

    public ItemDefinitionDto(ID definition) {
         super(definition);
//         this.name = definition.getItemName().getLocalPart();
     }

     public String getMinMaxOccurs() {
         return String.valueOf(getOriginalDefinition().getMinOccurs())
                 + '/'
                 + getOriginalDefinition().getMaxOccurs();

     }

     public String getMinOccurs() {
        return String.valueOf(getOriginalDefinition().getMinOccurs());
     }

     public void setMinOccurs(String minOccurs) {
        if (getOriginalDefinition() instanceof MutableItemDefinition<?>) {
            ((MutableItemDefinition<?>) getOriginalDefinition()).setMinOccurs(Integer.valueOf(minOccurs));
        }
     }

     public String getMaxOccurs() {
         return String.valueOf(getOriginalDefinition().getMaxOccurs());
     }

     public void setMaxOccurs(String maxOccurs) {
        if (getOriginalDefinition() instanceof MutableItemDefinition<?>) {
            ((MutableItemDefinition<?>) getOriginalDefinition()).setMaxOccurs(Integer.valueOf(maxOccurs));
        }
     }

     public boolean getIndexed() {
         if (getOriginalDefinition() instanceof PrismPropertyDefinition){
             //no value means indexed
             return BooleanUtils.isNotFalse(((PrismPropertyDefinition) getOriginalDefinition()).isIndexed());
        }
        return false;
     }

     public void setIndexed(boolean indexed) {
        if (getOriginalDefinition() instanceof MutablePrismPropertyDefinition<?>) {
            ((MutablePrismPropertyDefinition<?>) getOriginalDefinition()).setIndexed(indexed);
        }
     }

     public void setName(String name) {
        if (getOriginalDefinition() instanceof MutableItemDefinition<?>) {
            ((MutableItemDefinition<?>) getOriginalDefinition()).setItemName(new ItemName(getOriginalDefinition().getItemName().getNamespaceURI(), name));
        }
     }

    public String getName() {
         return getOriginalDefinition().getItemName().getLocalPart();
     }

    @Override
    public ID getOriginalDefinition() {
        return super.getOriginalDefinition();
    }
}

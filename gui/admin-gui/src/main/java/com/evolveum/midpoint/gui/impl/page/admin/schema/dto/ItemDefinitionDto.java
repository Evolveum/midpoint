/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;

import java.io.Serializable;

public class ItemDefinitionDto<ID extends MutableItemDefinition> extends DefinitionDto<ID> implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_MIN_OCCURS = "minOccurs";
    public static final String F_MAX_OCCURS = "maxOccurs";
    public static final String F_MIN_MAX_OCCURS = "minMaxOccurs";

    private String name;
    private Integer minOccurs;
    private Integer maxOccurs;


    public ItemDefinitionDto(ID definition) {
         super(definition);
         this.name = definition.getItemName().getLocalPart();
         this.minOccurs = definition.getMinOccurs();
         this.maxOccurs = definition.getMaxOccurs();
     }

     public String getMinMaxOccurs() {
         return String.valueOf(minOccurs)
                 + '/'
                 + maxOccurs;

     }

    @Override
    public ID getOriginalDefinition() {
        return super.getOriginalDefinition();
    }


}

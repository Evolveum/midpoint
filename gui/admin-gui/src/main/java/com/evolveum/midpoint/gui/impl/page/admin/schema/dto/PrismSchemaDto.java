/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.schema.dto;

import com.evolveum.midpoint.prism.schema.PrismSchema;

import java.io.Serializable;

public class PrismSchemaDto implements Serializable {

    private PrismSchema schema;

    public PrismSchemaDto(PrismSchema schema) {
        this.schema = schema;
    }


}

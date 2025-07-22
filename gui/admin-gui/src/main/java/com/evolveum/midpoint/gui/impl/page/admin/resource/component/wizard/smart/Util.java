/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.smart;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.SchemaException;

public class Util {
    static String serializeRealValue(Containerable suggestion, ItemName root) {
        try {
            return PrismContext.get().xmlSerializer().serializeRealValue(suggestion, root);
        } catch (SchemaException e) {
            return "Error serializing value: " + e.getMessage();
        }
    }
}

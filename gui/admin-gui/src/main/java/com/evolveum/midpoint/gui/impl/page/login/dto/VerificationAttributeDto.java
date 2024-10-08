/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login.dto;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class VerificationAttributeDto implements Serializable {

    private final PrismPropertyWrapper<?> itemWrapper;
    private final ItemPath itemPath;

    public VerificationAttributeDto(@NotNull PrismPropertyWrapper<?> itemWrapper, ItemPath itemPath) {
        this.itemWrapper = itemWrapper;
        this.itemPath = itemPath;
    }

    public PrismPropertyWrapper<?> getItemWrapper() {
        return itemWrapper;
    }

    public Object getValue() {
        try {
            return itemWrapper.getValue();
        } catch (SchemaException e) {
            return null;
        }
    }

    public ItemPath getItemPath() {
        return itemPath;
    }

}

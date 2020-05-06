/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.jetbrains.annotations.Nullable;

/**
 * Created by honchar
 */
public class CaseWorkItemTypeWrapper extends PrismContainerWrapperImpl<CaseWorkItemType> {

    public CaseWorkItemTypeWrapper(@Nullable PrismContainerValueWrapper parent, PrismContainer<CaseWorkItemType> container, ItemStatus status) {
        super(parent, container, status);
    }
}

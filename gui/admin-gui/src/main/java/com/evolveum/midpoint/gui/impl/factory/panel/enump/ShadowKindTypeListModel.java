/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.enump;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShadowKindTypeListModel extends LoadableDetachableModel<List<ShadowKindType>> {
    @Override
    protected List<ShadowKindType> load() {
        List<ShadowKindType> list = new ArrayList<>();
        Collections.addAll(list, ShadowKindType.class.getEnumConstants());
        list.removeIf(value -> ShadowKindType.ASSOCIATION == value);

        return list;
    }
}

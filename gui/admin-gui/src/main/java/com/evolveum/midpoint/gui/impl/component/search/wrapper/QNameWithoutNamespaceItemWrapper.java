/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ObjectTypeListUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultProcessedObjectType;

/**
 * Search wrapper for search only in memory for multivalue containers.
 */
public class QNameWithoutNamespaceItemWrapper extends ChoicesSearchItemWrapper<QName> {

    public QNameWithoutNamespaceItemWrapper() {
        super(SimulationResultProcessedObjectType.F_TYPE,
                ObjectTypeListUtil.createObjectTypesList().stream()
                        .map(o -> new DisplayableValueImpl<>(new QName(o.getTypeQName().getLocalPart()), WebComponentUtil.createEnumResourceKey(o)))
                        .collect(Collectors.toList()));
    }
}

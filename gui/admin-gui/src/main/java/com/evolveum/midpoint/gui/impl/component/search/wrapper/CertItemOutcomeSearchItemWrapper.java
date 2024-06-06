/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import java.util.List;

public class CertItemOutcomeSearchItemWrapper  extends ChoicesSearchItemWrapper<AccessCertificationResponseType> {

    public CertItemOutcomeSearchItemWrapper(List<DisplayableValue<AccessCertificationResponseType>> availableValues) {
        super(AccessCertificationWorkItemType.F_OUTPUT, availableValues);
    }

    @Override
    public DisplayableValue<AccessCertificationResponseType> getDefaultValue() {
        return new SearchValue(AccessCertificationResponseType.NO_RESPONSE);
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {

        //todo
        return null;
    }


}

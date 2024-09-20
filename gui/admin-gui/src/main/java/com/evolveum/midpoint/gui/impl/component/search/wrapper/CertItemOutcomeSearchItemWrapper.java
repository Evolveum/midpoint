/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import java.util.List;

public class CertItemOutcomeSearchItemWrapper  extends ChoicesSearchItemWrapper<AccessCertificationResponseType> {

    public CertItemOutcomeSearchItemWrapper(ItemPath path, List<DisplayableValue<AccessCertificationResponseType>> availableValues) {
        super(path, availableValues);
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getValue().getValue() == null) {
            return null;
        }
        AccessCertificationResponseType response = getValue().getValue();

        if (AccessCertificationResponseType.NO_RESPONSE.equals(response) &&
                AccessCertificationWorkItemType.class.equals(type)) {
            //work items without response have null outcome
            return PrismContext.get().queryFor(type)
                    .item(getPath()).isNull()
                    .buildFilter();
        }
        return PrismContext.get().queryFor(type)
                .item(getPath()).eq(OutcomeUtils.toUri(response)).buildFilter();
    }

    public boolean allowNull() {
        return true;
    }
}

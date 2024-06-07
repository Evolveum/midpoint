/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.CertItemOutcomeSearchItemWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CertItemOutcomeSearchItemWrapperFactory  extends
        AbstractSearchItemWrapperFactory<AccessCertificationResponseType, CertItemOutcomeSearchItemWrapper> {

    @Override
    protected CertItemOutcomeSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        List<AccessCertificationResponseType> values = Arrays.asList(AccessCertificationResponseType.values());
        List<DisplayableValue<AccessCertificationResponseType>> availableValues = new ArrayList<>();
        values.forEach(value -> {
            if (!value.equals(AccessCertificationResponseType.DELEGATE)) {
                availableValues.add(new SearchValue<>(value));
            }
        });
        CertItemOutcomeSearchItemWrapper wrapper = new CertItemOutcomeSearchItemWrapper(availableValues);
        return wrapper;
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                .equivalent(ctx.getPath()) && ctx.isVisible();
    }
}

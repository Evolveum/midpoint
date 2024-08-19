/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.CertItemOutcomeSearchItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationWorkItemType;

import java.util.ArrayList;
import java.util.List;

public class CertItemOutcomeSearchItemWrapperFactory  extends
        AbstractSearchItemWrapperFactory<AccessCertificationResponseType, CertItemOutcomeSearchItemWrapper> {

    @Override
    protected CertItemOutcomeSearchItemWrapper createSearchWrapper(SearchItemContext ctx) {
        List<DisplayableValue<AccessCertificationResponseType>> availableValues = getAvailableValues(ctx);
        CertItemOutcomeSearchItemWrapper wrapper = new CertItemOutcomeSearchItemWrapper(ctx.getPath(), availableValues);
        setDefaultValue(ctx, wrapper);
        return wrapper;
    }

    private List<DisplayableValue<AccessCertificationResponseType>> getAvailableValues(SearchItemContext ctx) {
        List<AccessCertificationResponseType> values = new AvailableResponses((PageBase) ctx.getModelServiceLocator()).getResponseValues();
        List<DisplayableValue<AccessCertificationResponseType>> availableValues = new ArrayList<>();
        values.forEach(value -> {
            if (!skipResponse(ctx, value)) {
                availableValues.add(new SearchValue<>(value));
            }
        });
        return availableValues;
    }

    private boolean skipResponse(SearchItemContext ctx, AccessCertificationResponseType value) {
//        if (isCertificationCaseOutcome(ctx)) {
            return AccessCertificationResponseType.DELEGATE.equals(value);
//        }
//        if (isWorkItemOutcome(ctx)) {
//            return AccessCertificationResponseType.NO_RESPONSE.equals(value)
//                    || AccessCertificationResponseType.DELEGATE.equals(value);
//        }
//        return false;
    }

    @Override
    public boolean match(SearchItemContext ctx) {
        return isCertificationCaseOutcome(ctx) || isWorkItemOutcome(ctx);
    }

    private boolean isCertificationCaseOutcome(SearchItemContext ctx) {
        return ItemPath.create(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME)
                .equivalent(ctx.getPath()) && ctx.isVisible();
    }

    private boolean isWorkItemOutcome(SearchItemContext ctx) {
        return ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                .equivalent(ctx.getPath()) && ctx.isVisible();
    }

    private void setDefaultValue(SearchItemContext ctx, CertItemOutcomeSearchItemWrapper wrapper) {
        if (isWorkItemOutcome(ctx)) {
            wrapper.setValue(new SearchValue<>(AccessCertificationResponseType.NO_RESPONSE));
        }
    }
}

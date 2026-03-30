/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
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
            ObjectFilter noOutcomeFilter = PrismContext.get().queryFor(type)
                    .item(getPath()).isNull()
                    .buildFilter();

            if (!CertMiscUtil.isCollectDecisionsFromAllReviewers(pageBase)) {
                // Also filter by parent case not having a response yet
                ObjectFilter caseNotDecidedFilter = PrismContext.get().queryFor(type)
                        .exists(PrismConstants.T_PARENT)
                        .block()
                            .item(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME)
                            .eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                        .endBlock()
                        .buildFilter();
                return PrismContext.get().queryFactory().createAnd(noOutcomeFilter, caseNotDecidedFilter);
            }
            return noOutcomeFilter;
        }
        return PrismContext.get().queryFor(type)
                .item(getPath()).eq(OutcomeUtils.toUri(response)).buildFilter();
    }

    public boolean allowNull() {
        return true;
    }
}

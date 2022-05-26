/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.cases.component.CaseOperationalButtonsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.page.admin.cases.CaseSummaryPanel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/case")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASES_ALL_URL,
                label = "PageAdminCases.auth.casesAll.label",
                description = "PageAdminCases.auth.casesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CASE_URL,
                label = "PageCase.auth.case.label",
                description = "PageCase.auth.case.description") })
public class PageCase extends PageAssignmentHolderDetails<CaseType, AssignmentHolderDetailsModel<CaseType>> {

    public PageCase(PageParameters pageParameters) {
        super(pageParameters);
    }

    public PageCase(PrismObject<CaseType> caseType) {
        super(caseType);
    }

    @Override
    public Class<CaseType> getType() {
        return CaseType.class;
    }

    @Override
    protected Panel createSummaryPanel(String id, IModel<CaseType> summaryModel) {
        return new CaseSummaryPanel(id, summaryModel, getSummaryPanelSpecification());
    }

    @Override
    protected AssignmentHolderDetailsModel<CaseType> createObjectDetailsModels(PrismObject<CaseType> object) {
        return new CaseDetailsModels(createPrismObjectModel(object), this);
    }

    @Override
    protected CaseOperationalButtonsPanel createButtonsPanel(String id, LoadableModel<PrismObjectWrapper<CaseType>> wrapperModel) {
        return new CaseOperationalButtonsPanel(id, wrapperModel);
    }

}

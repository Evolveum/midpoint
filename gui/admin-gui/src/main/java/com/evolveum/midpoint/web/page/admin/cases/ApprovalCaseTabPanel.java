/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.wf.SwitchableApprovalProcessPreviewsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

/**
 * Created by honchar
 */
public class ApprovalCaseTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static String ID_APPROVAL_CASE_PANEL = "approvalCasePanel";
    public ApprovalCaseTabPanel(String id, Form<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout(){
        SwitchableApprovalProcessPreviewsPanel approvalPanel = new SwitchableApprovalProcessPreviewsPanel(ID_APPROVAL_CASE_PANEL, Model.of(getObjectWrapper().getOid()),
                Model.of(WfContextUtil.isInStageBeforeLastOne(getObjectWrapper().getObject().asObjectable())), getPageBase());
        approvalPanel.setOutputMarkupId(true);
        add(approvalPanel);
    }

}

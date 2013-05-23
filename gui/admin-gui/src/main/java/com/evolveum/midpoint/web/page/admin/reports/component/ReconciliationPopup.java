/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.internal.dto.ResourceItemDto;
import com.evolveum.midpoint.web.page.admin.reports.dto.ReconciliationReportDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class ReconciliationPopup extends SimplePanel<ReconciliationReportDto> {

    public ReconciliationPopup(String id, IModel<ReconciliationReportDto> model,
                               IModel<List<ResourceItemDto>> resources) {
        super(id, model);

        initLayout(resources);
    }

    private void initLayout(IModel<List<ResourceItemDto>> resources) {

    }

    protected void onRunPerformed(AjaxRequestTarget target) {

    }
}

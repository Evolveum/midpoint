/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import java.util.List;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/certificationCampaignStatistics", action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION)
})
public class PageCertCampaignStatistics extends PageAdminCertification {

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaignStatistics.class);

    private static final String DOT_CLASS = PageCertCampaignStatistics.class.getName() + ".";

    private static final String ID_STATES = "states";
    private static final String ID_RESPONSE = "response";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEMS_REMEDIED = "itemsRemedied";

    private IModel<List<AccessCertificationCasesStatisticsEntryType>> statModel;

    PageParameters pageParameters;

    public PageCertCampaignStatistics() {
        this(new PageParameters());
    }

    public PageCertCampaignStatistics(final PageParameters pageParameters) {
        this.pageParameters = pageParameters;
        statModel = new LoadableModel<List<AccessCertificationCasesStatisticsEntryType>>(false) {

            @Override
            protected List<AccessCertificationCasesStatisticsEntryType> load() {
                StringValue campaignOid = pageParameters.get(OnePageParameterEncoder.PARAMETER);
                return loadStatistics(campaignOid);
            }
        };
        initLayout();
    }

    private void initLayout() {

        ListView<AccessCertificationCasesStatisticsEntryType> statistics = new ListView<AccessCertificationCasesStatisticsEntryType>(ID_STATES, statModel) {

            @Override
            protected void populateItem(ListItem<AccessCertificationCasesStatisticsEntryType> item) {
                AccessCertificationCasesStatisticsEntryType entry = item.getModelObject();

                Label response = new Label(ID_RESPONSE, entry.getResponse());
                response.setRenderBodyOnly(true);
                item.add(response);

                Label items = new Label(ID_ITEMS, entry.getMarkedAsThis());
                items.setRenderBodyOnly(true);
                item.add(items);

                Label itemsRemedied = new Label(ID_ITEMS_REMEDIED, entry.getMarkedAsThisAndResolved());
                itemsRemedied.setRenderBodyOnly(true);
                item.add(itemsRemedied);
            }
        };
        add(statistics);
    }

    private List<AccessCertificationCasesStatisticsEntryType> loadStatistics(StringValue campaignOid) {
        OperationResult result = new OperationResult("dummy");
        AccessCertificationCasesStatisticsType stat = null;
        try {
            Task task = createSimpleTask("dummy");
            stat = getCertificationManager().getCampaignStatistics(campaignOid.toString(), false, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get campaign statistics", ex);
            result.recordFatalError("Couldn't get campaign statistics.", ex);
        }
        result.recomputeStatus();

        if (!WebMiscUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }
        return stat.getEntry();
    }

}

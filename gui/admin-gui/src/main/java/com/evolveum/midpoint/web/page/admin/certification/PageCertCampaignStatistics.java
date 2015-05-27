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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_DELEGATE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_NOT_DECIDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REDUCE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REDUCE_AND_REMEDIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REVOKE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_MARKED_AS_REVOKE_AND_REMEDIED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType.F_WITHOUT_RESPONSE;

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

    private static final String ID_ACCEPT = "accept";
    private static final String ID_REVOKE = "revoke";
    private static final String ID_REVOKE_REMEDIED = "revokeRemedied";
    private static final String ID_REDUCE = "reduce";
    private static final String ID_REDUCE_REMEDIED = "reduceRemedied";
    private static final String ID_DELEGATE = "delegate";
    private static final String ID_NO_DECISION = "noDecision";
    private static final String ID_NO_RESPONSE = "noResponse";

    private IModel<AccessCertificationCasesStatisticsType> statModel;

    PageParameters pageParameters;

    public PageCertCampaignStatistics() {
        this(new PageParameters());
    }

    public PageCertCampaignStatistics(final PageParameters pageParameters) {
        this.pageParameters = pageParameters;
        statModel = new LoadableModel<AccessCertificationCasesStatisticsType>(false) {

            @Override
            protected AccessCertificationCasesStatisticsType load() {
                StringValue campaignOid = pageParameters.get(OnePageParameterEncoder.PARAMETER);
                return loadStatistics(campaignOid);
            }
        };
        initLayout();
    }

    private void initLayout() {

        add(createLabel(ID_ACCEPT, F_MARKED_AS_ACCEPT));
        add(createLabel(ID_REVOKE, F_MARKED_AS_REVOKE));
        add(createLabel(ID_REVOKE_REMEDIED, F_MARKED_AS_REVOKE_AND_REMEDIED));
        add(createLabel(ID_REDUCE, F_MARKED_AS_REDUCE));
        add(createLabel(ID_REDUCE_REMEDIED, F_MARKED_AS_REDUCE_AND_REMEDIED));
        add(createLabel(ID_DELEGATE, F_MARKED_AS_DELEGATE));
        add(createLabel(ID_NO_DECISION, F_MARKED_AS_NOT_DECIDE));
        add(createLabel(ID_NO_RESPONSE, F_WITHOUT_RESPONSE));
    }

    private Label createLabel(String id, QName property) {
        return new Label(id, new PropertyModel<Integer>(statModel, property.getLocalPart()));
    }

    private AccessCertificationCasesStatisticsType loadStatistics(StringValue campaignOid) {
        OperationResult result = new OperationResult("dummy");  // todo
        AccessCertificationCasesStatisticsType stat = null;
        try {
            Task task = createSimpleTask("dummy");  // todo
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
        return stat;
    }

}

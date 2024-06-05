/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

public class CertMiscUtil {

    public static String getStopReviewOnText(List<AccessCertificationResponseType> stopOn, PageBase page) {
        if (stopOn == null) {
            return page.getString("PageCertDefinition.stopReviewOnDefault");
        } else if (stopOn.isEmpty()) {
            return page.getString("PageCertDefinition.stopReviewOnNone");
        } else {
            List<String> names = new ArrayList<>(stopOn.size());
            for (AccessCertificationResponseType r : stopOn) {
                names.add(page.createStringResource(r).getString());
            }
            return StringUtils.join(names, ", ");
        }
    }

    public static LoadableModel<List<ProgressBar>> createCampaignProgressBarModel(AccessCertificationCampaignType campaign) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                float casesCount = campaign.getCase() != null ? campaign.getCase().size() : 0;
                if (casesCount == 0) {
                    ProgressBar allCasesProgressBar = new ProgressBar(casesCount, ProgressBar.State.SECONDARY);
                    return Collections.singletonList(allCasesProgressBar);
                }

                float completed = CertCampaignTypeUtil.getCasesCompletedPercentageAllStagesAllIterations(campaign);
                ProgressBar completedProgressBar = new ProgressBar(completed, ProgressBar.State.INFO);
                return Collections.singletonList(completedProgressBar);
            }
        };
    }
}

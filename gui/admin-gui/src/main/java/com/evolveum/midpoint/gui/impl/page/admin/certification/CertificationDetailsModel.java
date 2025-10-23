/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCasesStatisticsType;

import org.apache.wicket.model.LoadableDetachableModel;

import java.io.Serial;

public class CertificationDetailsModel extends AssignmentHolderDetailsModel<AccessCertificationCampaignType> {

    private static final Trace LOGGER = TraceManager.getTrace(CertificationDetailsModel.class);

    private static final String DOT_CLASS = CertificationDetailsModel.class.getName() + ".";
    private static final String OPERATION_LOAD_STATISTICS = DOT_CLASS + "loadCertificationStatistics";

    private final LoadableDetachableModel<AccessCertificationCasesStatisticsType> certStatisticsModel;

    public CertificationDetailsModel(LoadableDetachableModel<PrismObject<AccessCertificationCampaignType>> prismObjectModel,
            PageBase serviceLocator) {
        super(prismObjectModel, serviceLocator);

        certStatisticsModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected AccessCertificationCasesStatisticsType load() {
                Task task = getPageBase().createSimpleTask(OPERATION_LOAD_STATISTICS);
                OperationResult result = task.getResult();
                AccessCertificationCasesStatisticsType stat = null;
                try {
                    stat = getPageBase().getCertificationService().getCampaignStatistics(prismObjectModel.getObject().getOid(),
                            false, task, result);
                    result.recordSuccessIfUnknown();
                } catch (Exception ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign statistics", ex);
                    result.recordFatalError(getPageBase().getString("PageCertCampaign.message.loadStatistics.fatalerror"), ex);
                }
                result.recomputeStatus();

                if (!WebComponentUtil.isSuccessOrHandledError(result)) {
                    getPageBase().showResult(result);
                }
                return stat;
            }
        };
    }

    public LoadableDetachableModel<AccessCertificationCasesStatisticsType> getCertStatisticsModel() {
        return certStatisticsModel;
    }

    public void reset() {
        super.reset();
        certStatisticsModel.detach();
    }
}

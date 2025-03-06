/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.gui.impl.page.admin.certification.PageCertCampaign;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.or0;

public class CampaignProcessingHelper implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(CampaignProcessingHelper.class);
    private static final String DOT_CLASS = CampaignProcessingHelper.class.getName() + ".";
    private static final String OPERATION_DELETE_CAMPAIGNS = DOT_CLASS + "deleteCampaigns";
    private static final String OPERATION_OPEN_NEXT_STAGE = DOT_CLASS + "openNextStage";
    private static final String OPERATION_CLOSE_STAGE = DOT_CLASS + "closeStage";
    private static final String OPERATION_CLOSE_CAMPAIGN = DOT_CLASS + "closeCampaign";
    private static final String OPERATION_START_CAMPAIGN = DOT_CLASS + "startCampaign";
    private static final String OPERATION_START_REMEDIATION = DOT_CLASS + "startRemediation";
    private static final String OPERATION_REITERATE_CAMPAIGN = DOT_CLASS + "reiterateCampaign";

    public static void startRemediationPerformed(OperationResult result,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        AccessCertificationService acs = pageBase.getCertificationService();
        campaigns.forEach(campaign -> {
            LOGGER.debug("Start remediation performed for {}", campaign.asPrismObject());
            try {
                Task task = pageBase.createSimpleTask(OPERATION_START_REMEDIATION);
                acs.startRemediation(campaign.getOid(), task, result);
            } catch (Exception ex) {
                result.recordFatalError(ex);
            } finally {
                result.computeStatusIfUnknown();
            }
        });
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
    }

    public static void openNextStagePerformed(OperationResult result, AccessCertificationCampaignType campaign, PageBase pageBase) {
        LOGGER.debug("Start campaign / open next stage performed for {}", campaign.asPrismObject());
        AccessCertificationService acs = pageBase.getCertificationService();
        try {
            Task task = pageBase.createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
            acs.openNextStage(campaign, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
    }

    public static void closeCampaignConfirmedPerformed(OperationResult result, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        campaigns.forEach(campaign -> {
            try {
                LOGGER.debug("Close certification campaign performed for {}", campaign.asPrismObject());

                AccessCertificationService acs = pageBase.getCertificationService();
                Task task = pageBase.createSimpleTask(OPERATION_CLOSE_CAMPAIGN);
                acs.closeCampaign(campaign.getOid(), task, result);
            } catch (Exception ex) {
                result.recordFatalError(ex);
            } finally {
                result.computeStatusIfUnknown();
            }
        });

        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
    }

    public static void reiterateCampaignConfirmedPerformed(OperationResult result,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        Task task = pageBase.createSimpleTask(OPERATION_REITERATE_CAMPAIGN);
        AccessCertificationService acs = pageBase.getCertificationService();

        campaigns.forEach(campaign -> {
            LOGGER.debug("Reiterate certification campaign performed for {}", campaign.asPrismObject());

            try {
                acs.reiterateCampaign(campaign.getOid(), task, result);
            } catch (Exception ex) {
                result.recordFatalError(ex);
            } finally {
                result.computeStatusIfUnknown();
            }
        });
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
    }

    public static void closeStageConfirmedPerformed(OperationResult result, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        Task task = pageBase.createSimpleTask(OPERATION_CLOSE_STAGE);
        AccessCertificationService acs = pageBase.getCertificationService();

        campaigns.forEach(campaign -> {
            LOGGER.debug("Close certification stage performed for {}", campaign.asPrismObject());

            try {
                acs.closeCurrentStage(campaign.getOid(), task, result);
            } catch (Exception ex) {
                result.recordFatalError(ex);
            } finally {
                result.computeStatusIfUnknown();
            }
        });
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
    }

    public static void campaignDetailsPerformed(String oid, PageBase pageBase) {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);
        pageBase.navigateToNext(PageCertCampaign.class, parameters);
    }

    public static void deleteCampaignsPerformed(AjaxRequestTarget target, List<AccessCertificationCampaignType> itemsToDelete,
            PageBase pageBase) {
        if (itemsToDelete.isEmpty()) {
            pageBase.warn(pageBase.getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(pageBase.getFeedbackPanel());
            return;
        }

        OperationResult result = new OperationResult(OPERATION_DELETE_CAMPAIGNS);
        for (AccessCertificationCampaignType itemToDelete : itemsToDelete) {
            try {
                Task task = pageBase.createSimpleTask(OPERATION_DELETE_CAMPAIGNS);
                ObjectDelta<AccessCertificationCampaignType> delta = PrismContext.get().deltaFactory().object().createDeleteDelta(
                        AccessCertificationCampaignType.class, itemToDelete.getOid());
                pageBase.getModelService().executeChanges(MiscUtil.createCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError(pageBase.createStringResource(
                        "PageCertCampaigns.message.deleteCampaignsPerformed.partialError").getString(), ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete campaign", ex);
            }
        }

        result.recomputeStatus();
        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, pageBase.createStringResource(
                    "PageCertCampaigns.message.deleteCampaignsPerformed.success").getString());
        }

        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
        target.add(pageBase.getFeedbackPanel(), pageBase);
    }

    /**
     * this method should be called after action confirmation
     * @param campaigns
     * @param pageBase
     * @param target
     */
    public static void campaignActionConfirmed(List<AccessCertificationCampaignType> campaigns, CampaignStateHelper.CampaignAction action,
            PageBase pageBase, AjaxRequestTarget target, OperationResult result) {
        if (CollectionUtils.isEmpty(campaigns)) {
            pageBase.warn(pageBase.getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(pageBase.getFeedbackPanel());
            return;
        }
        String operationName = LocalizationUtil.translate(action.getActionLabelKey());

        try {
            if (CampaignStateHelper.CampaignAction.START_CAMPAIGN.equals(action)) {
                campaigns.forEach(campaign -> openNextStagePerformed(result, campaign, pageBase));
            } else if (CampaignStateHelper.CampaignAction.CLOSE_CAMPAIGN.equals(action)) {
                closeCampaignConfirmedPerformed(result, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.START_REMEDIATION.equals(action)) {
                startRemediationPerformed(result, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.CLOSE_STAGE.equals(action)) {
                closeStageConfirmedPerformed(result, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.REITERATE_CAMPAIGN.equals(action)) {
                reiterateCampaignConfirmedPerformed(result, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.OPEN_NEXT_STAGE.equals(action)) {
                campaigns.forEach(campaign -> openNextStagePerformed(result, campaign, pageBase));
            } else if (CampaignStateHelper.CampaignAction.REMOVE_CAMPAIGN.equals(action)) {
                deleteCampaignsPerformed(target, campaigns, pageBase);
            } else {
                throw new IllegalStateException("Unknown action: " + operationName);
            }
        } catch (Exception ex) {
            result.recordPartialError(pageBase.createStringResource(
                    "PageCertCampaigns.message.actOnCampaignsPerformed.partialError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process campaign", ex);
        }
    }

    public static String computeDeadlineAsString(AccessCertificationCampaignType campaign, PageBase page) {
        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        XMLGregorianCalendar end;
        Boolean stageLevelInfo;
        if (or0(campaign.getStageNumber()) == 0) {
            end = campaign.getEndTimestamp();            // quite useless, as "end" denotes real campaign end
            stageLevelInfo = false;
        } else if (currentStage != null) {
            end = currentStage.getDeadline();
            stageLevelInfo = true;
        } else {
            end = null;
            stageLevelInfo = null;
        }

        if (end == null) {
            return "";
        } else {
            long delta = XmlTypeConverter.toMillis(end) - System.currentTimeMillis();

            // round to hours; we always round down
            long precision = 3600000L;      // 1 hour
            if (Math.abs(delta) > precision) {
                delta = (delta / precision) * precision;
            }

            if (delta > 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.inForStage" : "PageCertCampaigns.inForCampaign";
                return LocalizationUtil.translate(key,
                        new Object[] { WebComponentUtil.formatDurationWordsForLocal(
                                delta, true, true, page)});
            } else if (delta < 0) {
                String key = stageLevelInfo ? "PageCertCampaigns.agoForStage" : "PageCertCampaigns.agoForCampaign";
                return LocalizationUtil.translate(key,
                        new Object[] { WebComponentUtil.formatDurationWordsForLocal(
                                -delta, true, true, page)});
            } else {
                String key = stageLevelInfo ? "PageCertCampaigns.nowForStage" : "PageCertCampaigns.nowForCampaign";
                return page.getString(key);
            }
        }
    }

    public static XMLGregorianCalendar computeDeadline(AccessCertificationCampaignType campaign, PageBase page) {
        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        XMLGregorianCalendar end;
        if (or0(campaign.getStageNumber()) == 0) {
            end = campaign.getEndTimestamp();            // quite useless, as "end" denotes real campaign end
        } else if (currentStage != null) {
            end = currentStage.getDeadline();
        } else {
            end = null;
        }

        return end;
    }

}

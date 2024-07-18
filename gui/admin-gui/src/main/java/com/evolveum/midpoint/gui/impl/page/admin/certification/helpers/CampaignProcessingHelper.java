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
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.gui.impl.page.admin.certification.PageCertCampaign;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.opensaml.xmlsec.signature.P;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;


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

    public static void closeStageConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        pageBase.showMainPopup(getCloseStageConfirmationPanel(campaigns, pageBase), target);
    }

    public static Popupable getCloseStageConfirmationPanel(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createCloseStageConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return pageBase.createStringResource("PageCertCampaigns.dialog.title.confirmCloseStage");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeStageConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void closeCampaignConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getCloseCampaignConfirmationPanel(campaigns, pageBase), target);
    }

    public static void campaignRemediationConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getRemediationConfirmationPanel(campaigns, pageBase), target);
    }

    public static void reiterateCampaignConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getReiterateCampaignConfirmationPanel(campaigns, pageBase), target);
    }

    public static Popupable getCloseCampaignConfirmationPanel(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createCloseCampaignConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeCampaignConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static Popupable getReiterateCampaignConfirmationPanel(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createReiterateCampaignConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmReiterateCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                reiterateCampaignConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void deleteCampaignConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getDeleteCampaignConfirmationPanel(campaigns, pageBase), target);
    }

    public static Popupable getDeleteCampaignConfirmationPanel(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createDeleteCampaignConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteCampaignConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void deleteCampaignConfirmedPerformed(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        deleteCampaignsPerformed(target, campaigns, pageBase);
    }

    public static Popupable getRemediationConfirmationPanel(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createRemediationCampaignConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCampaignRemediation");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                startRemediationPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void startRemediationPerformed(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_START_REMEDIATION);
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
        target.add(pageBase);
    }

    public static void openNextStagePerformed(AjaxRequestTarget target, AccessCertificationCampaignType campaign, PageBase pageBase) {
        LOGGER.debug("Start campaign / open next stage performed for {}", campaign.asPrismObject());
        OperationResult result = new OperationResult(OPERATION_OPEN_NEXT_STAGE);
        AccessCertificationService acs = pageBase.getCertificationService();
        try {
            Task task = pageBase.createSimpleTask(OPERATION_OPEN_NEXT_STAGE);
            acs.openNextStage(campaign.getOid(), task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
        WebComponentUtil.safeResultCleanup(result, LOGGER);
        pageBase.showResult(result);
        target.add(pageBase);
    }

    public static void closeCampaignConfirmedPerformed(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_CLOSE_CAMPAIGN);
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
        target.add(pageBase);
    }

    public static void reiterateCampaignConfirmedPerformed(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_REITERATE_CAMPAIGN);
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
        target.add(pageBase);
    }

    public static void closeStageConfirmedPerformed(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_CLOSE_STAGE);
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
        target.add(pageBase);
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

    public static void campaignActionPerformed(@NotNull AccessCertificationCampaignType campaign, PageBase pageBase,
            AjaxRequestTarget target) {
        CampaignStateHelper.CampaignAction action = new CampaignStateHelper(campaign).getNextAction();
        campaignActionPerformed(Collections.singletonList(campaign), action, pageBase, target);
    }

    public static void campaignActionPerformed(@NotNull AccessCertificationCampaignType campaign,
            CampaignStateHelper.CampaignAction action, PageBase pageBase, AjaxRequestTarget target) {
        campaignActionPerformed(Collections.singletonList(campaign), action, pageBase, target);
    }

    public static void campaignActionPerformed(@NotNull List<AccessCertificationCampaignType> campaigns,
            CampaignStateHelper.CampaignAction action, PageBase pageBase, AjaxRequestTarget target) {
        if (CollectionUtils.isEmpty(campaigns)) {
            pageBase.warn(pageBase.getString("PageCertCampaigns.message.noCampaignsSelected"));
            target.add(pageBase.getFeedbackPanel());
            return;
        }
        String operationName = LocalizationUtil.translate(action.getActionLabelKey());
        OperationResult result = new OperationResult(operationName);

        try {
            if (CampaignStateHelper.CampaignAction.START_CAMPAIGN.equals(action)) {
                campaigns.forEach(campaign -> openNextStagePerformed(target, campaign, pageBase));
            } else if (CampaignStateHelper.CampaignAction.CLOSE_CAMPAIGN.equals(action)) {
                closeCampaignConfirmation(target, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.START_REMEDIATION.equals(action)) {
                campaignRemediationConfirmation(target, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.CLOSE_STAGE.equals(action)) {
                closeStageConfirmation(target, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.REITERATE_CAMPAIGN.equals(action)) {
                reiterateCampaignConfirmation(target, campaigns, pageBase);
            } else if (CampaignStateHelper.CampaignAction.OPEN_NEXT_STAGE.equals(action)) {
                campaigns.forEach(campaign -> openNextStagePerformed(target, campaign, pageBase));
            } else if (CampaignStateHelper.CampaignAction.REMOVE_CAMPAIGN.equals(action)) {
                deleteCampaignConfirmation(target, campaigns, pageBase);
            } else {
                throw new IllegalStateException("Unknown action: " + operationName);
            }
        } catch (Exception ex) {
            result.recordPartialError(pageBase.createStringResource(
                    "PageCertCampaigns.message.actOnCampaignsPerformed.partialError").getString(), ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process campaign", ex);
        }
    }

    public static IModel<String> createCloseStageConfirmString(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return () -> {
            if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.closeStageConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return pageBase.createStringResource("PageCertCampaigns.message.closeStageConfirmMultiple",
                        campaigns.size()).getString();
            }
        };
    }

    public static IModel<String> createCloseCampaignConfirmString(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return () -> {
            if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.closeCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return pageBase.createStringResource("PageCertCampaigns.message.closeCampaignConfirmMultiple",
                        campaigns.size()).getString();
            }
        };
    }

    public static IModel<String> createReiterateCampaignConfirmString(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return () -> {
            if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return pageBase.createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmMultiple",
                        campaigns.size()).getString();
            }
        };
    }

    public static IModel<String> createCloseSelectedCampaignsConfirmString(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return () -> {
            if (campaigns.size() > 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.closeCampaignConfirmMultiple",
                        campaigns.size()).getString();
            } else if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.closeCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return "EMPTY";
            }
        };
    }

    public static IModel<String> createReiterateSelectedCampaignsConfirmString(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return () -> {
            if (campaigns.size() > 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmMultiple",
                        campaigns.size()).getString();
            } else if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.reiterateCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return "EMPTY";
            }
        };
    }

    public static IModel<String> createDeleteCampaignConfirmString(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return () -> {
            if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.deleteCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return pageBase.createStringResource("PageCertCampaigns.message.deleteCampaignConfirmMultiple",
                        campaigns.size()).getString();
            }
        };
    }

    public static IModel<String> createRemediationCampaignConfirmString(List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        return () -> {
            if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.remediationConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return pageBase.createStringResource("PageCertCampaigns.message.remediationConfirmMultiple",
                        campaigns.size()).getString();
            }
        };
    }

    public static IModel<String> createDeleteSelectedCampaignsConfirmString(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return () -> {
            if (campaigns.size() > 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.deleteCampaignConfirmMultiple",
                        campaigns.size()).getString();
            } else if (campaigns.size() == 1) {
                return pageBase.createStringResource("PageCertCampaigns.message.deleteCampaignConfirmSingle",
                        campaigns.get(0).getName()).getString();
            } else {
                return "EMPTY";
            }
        };
    }

    public static void reiterateSelectedCampaignsConfirmation(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        pageBase.showMainPopup(getReiterateSelectedCampaignsConfirmationPanel(campaigns, pageBase), target);
    }

    private static Popupable getReiterateSelectedCampaignsConfirmationPanel(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(),
                createReiterateSelectedCampaignsConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return pageBase.createStringResource("PageCertCampaigns.dialog.title.confirmReiterateCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                reiterateSelectedCampaignsConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    private static void reiterateSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        campaignActionPerformed(campaigns, CampaignStateHelper.CampaignAction.REITERATE_CAMPAIGN, pageBase, target);
    }

    public static void deleteSelectedCampaignsConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getDeleteSelectedCampaignsConfirmationPanel(campaigns, pageBase),
                target);
    }

    private static Popupable getDeleteSelectedCampaignsConfirmationPanel(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(),
                createDeleteSelectedCampaignsConfirmString(campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmDeleteCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                deleteCampaignsPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void closeSelectedCampaignsConfirmation(AjaxRequestTarget target, List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        pageBase.showMainPopup(getCloseSelectedCampaignsConfirmationPanel(campaigns, pageBase), target);
    }

    public static Popupable getCloseSelectedCampaignsConfirmationPanel(List<AccessCertificationCampaignType> campaigns,
            PageBase pageBase) {
        return new ConfirmationPanel(pageBase.getMainPopupBodyId(), createCloseSelectedCampaignsConfirmString(
                campaigns, pageBase)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("PageCertCampaigns.dialog.title.confirmCloseCampaign");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                closeSelectedCampaignsConfirmedPerformed(target, campaigns, pageBase);
            }

        };
    }

    public static void closeSelectedCampaignsConfirmedPerformed(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        campaignActionPerformed(campaigns, CampaignStateHelper.CampaignAction.CLOSE_CAMPAIGN, pageBase, target);
    }

    public static  void startSelectedCampaignsPerformed(AjaxRequestTarget target,
            List<AccessCertificationCampaignType> campaigns, PageBase pageBase) {
        campaignActionPerformed(campaigns, CampaignStateHelper.CampaignAction.START_CAMPAIGN, pageBase, target);
    }

    public static String computeDeadlineAsString(AccessCertificationCampaignType campaign, PageBase page) {
        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        XMLGregorianCalendar end;
        Boolean stageLevelInfo;
        if (campaign.getStageNumber() == 0) {
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
        if (campaign.getStageNumber() == 0) {
            end = campaign.getEndTimestamp();            // quite useless, as "end" denotes real campaign end
        } else if (currentStage != null) {
            end = currentStage.getDeadline();
        } else {
            end = null;
        }

        return end;
    }

}

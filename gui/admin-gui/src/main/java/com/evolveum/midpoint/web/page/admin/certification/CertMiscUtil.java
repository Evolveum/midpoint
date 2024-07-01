/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.certification;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.model.LoadableModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.component.action.*;
import com.evolveum.midpoint.web.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

import static java.util.Collections.singleton;

public class CertMiscUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CertMiscUtil.class);
    private static final String OPERATION_LOAD_CAMPAIGNS_OIDS = "loadCampaignsOids";
    private static final String OPERATION_LOAD_CASES_COUNT = "loadCampaignsOids";
    private static final String OPERATION_RECORD_ACTION = "recordAction";
    private static final String OPERATION_RECORD_ACTION_SELECTED = "recordActionSelected";

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

    public static LoadableModel<List<ProgressBar>> createCampaignProgressBarModel(AccessCertificationCampaignType campaign,
            MidPointPrincipal principal, PageBase pageBase) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                int currentStage = campaign.getStageNumber();

                //todo check if campaign is in review stage state

                OperationResult result = new OperationResult(OPERATION_LOAD_CASES_COUNT);
                Task task = pageBase.createSimpleTask(OPERATION_LOAD_CASES_COUNT);
                try {
                    ObjectQuery allWorkItems;
                    if (principal != null) {
                        allWorkItems = pageBase.getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                                .ownerId(campaign.getOid())
                                .and()
                                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                                .eq(currentStage)
                                .and()
                                .item(ItemPath.create(AbstractWorkItemType.F_ASSIGNEE_REF))
                                .ref(principal.getOid())
                                .build();
                    } else {
                        allWorkItems = pageBase.getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                                .ownerId(campaign.getOid())
                                .and()
                                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                                .eq(currentStage)
                                .build();
                    }

                    Integer allItemsCount = pageBase.getModelService()
                            .countContainers(AccessCertificationWorkItemType.class, allWorkItems, null, task, result);

                    if (allItemsCount == null || allItemsCount == 0) {
                        ProgressBar allItemsProgressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(allItemsProgressBar);
                    }

                    ObjectQuery processedItemsQuery;
                    if (principal != null) {
                        processedItemsQuery = pageBase.getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                                .ownerId(campaign.getOid())
                                .and()
                                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                                .eq(currentStage)
                                .and()
                                .item(ItemPath.create(AccessCertificationWorkItemType.F_ASSIGNEE_REF))
                                .ref(principal.getOid())
                                .and()
                                .not()
                                .item(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                                .isNull()
                                .build();
                    } else {
                        processedItemsQuery = pageBase.getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                                .ownerId(campaign.getOid())
                                .and()
                                .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                                .eq(currentStage)
                                .and()
                                .not()
                                .item(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                                .isNull()
                                .build();
                    }


                    Integer processedItemsCount = pageBase.getModelService()
                            .countContainers(AccessCertificationWorkItemType.class, processedItemsQuery, null, task, result);

                    if (processedItemsCount == null) {
                        ProgressBar progressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(progressBar);
                    }

                    float completed = (float) processedItemsCount / allItemsCount;
                    ProgressBar completedProgressBar = new ProgressBar(completed, ProgressBar.State.INFO);
                    return Collections.singletonList(completedProgressBar);
                } catch (Exception ex) {
                    LOGGER.error("Couldn't count certification cases", ex);
                    pageBase.showResult(result);
                }
                return Collections.emptyList();
            }
        };
    }

    public static AccessCertificationResponseType getStageOutcome(AccessCertificationCaseType aCase, int stageNumber) {
        Set<AccessCertificationResponseType> stageOutcomes = aCase.getEvent().stream()
                .filter(e -> e instanceof StageCompletionEventType && e.getStageNumber() == stageNumber)
                .map(e -> OutcomeUtils.normalizeToNonNull(OutcomeUtils.fromUri(((StageCompletionEventType) e).getOutcome())))
                .collect(Collectors.toSet());
        Collection<AccessCertificationResponseType> nonNullOutcomes = CollectionUtils.subtract(stageOutcomes, singleton(NO_RESPONSE));
        if (!nonNullOutcomes.isEmpty()) {
            return nonNullOutcomes.iterator().next();
        } else if (!stageOutcomes.isEmpty()) {
            return NO_RESPONSE;
        } else {
            return null;
        }
    }

    public static DoughnutChartConfiguration createDoughnutChartConfigForCampaigns(List<String> campaignOids, MidPointPrincipal principal,
            PageBase pageBase) {
        DoughnutChartConfiguration config = new DoughnutChartConfiguration();

        ChartData chartData = new ChartData();
        chartData.addDataset(createDataSet(campaignOids, principal, pageBase));

        config.setData(chartData);
        return config;
    }

    private static ChartDataset createDataSet(List<String> campaignOids, MidPointPrincipal principal, PageBase pageBase) {
        ChartDataset dataset = new ChartDataset();
//        dataset.setLabel("Not decided");

        dataset.setFill(true);

        long notDecidedCertItemsCount = countOpenCertItems(campaignOids, principal, true, pageBase);
        long allOpenCertItemsCount = countOpenCertItems(campaignOids, principal, false, pageBase);
        long decidedCertItemsCount = allOpenCertItemsCount - notDecidedCertItemsCount;

        dataset.addData(decidedCertItemsCount);
        dataset.addBackgroudColor("blue");

        dataset.addData(notDecidedCertItemsCount);
        dataset.addBackgroudColor("grey");

        return dataset;
    }

    public static long countOpenCertItems(List<String> campaignOids, MidPointPrincipal principal, boolean notDecidedOnly,
            PageBase pageBase) {
        long count = 0;

        Task task = pageBase.createSimpleTask("countCertificationWorkItems");
        OperationResult result = task.getResult();
        try {
            ObjectQuery query = QueryUtils.createQueryForOpenWorkItemsForCampaigns(campaignOids, principal, notDecidedOnly);
            count = pageBase.getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, query, null, task, result);
        } catch (Exception ex) {
            LOGGER.error("Couldn't count certification work items", ex);
            pageBase.showResult(result);
        }
        return count;
    }

    public static List<String> getActiveCampaignsOids(boolean onlyForLoggedInUser, PageBase pageBase) {
        OperationResult result = new OperationResult(OPERATION_LOAD_CAMPAIGNS_OIDS);
        ObjectQuery campaignsQuery;
        if (onlyForLoggedInUser) {
             campaignsQuery = getPrincipalActiveCampaignsQuery(pageBase);
        } else {
            campaignsQuery = getAllActiveCampaignsQuery(pageBase);
        }
        List<PrismObject<AccessCertificationCampaignType>> campaigns = WebModelServiceUtils.searchObjects(
                AccessCertificationCampaignType.class, campaignsQuery, null, result, pageBase);
        return campaigns.stream().map(PrismObject::getOid).toList();
    }

    public static ObjectQuery getPrincipalActiveCampaignsQuery(PageBase pageBase) {
        FocusType principal = pageBase.getPrincipalFocus();

        return pageBase.getPrismContext().queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_ASSIGNEE_REF)
                .ref(principal.getOid())
                .and()
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .build();
    }

    public static ObjectQuery getAllActiveCampaignsQuery(PageBase pageBase) {
        return pageBase.getPrismContext().queryFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_CASE, AccessCertificationCaseType.F_WORK_ITEM,
                        AccessCertificationWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull()
                .build();
    }

    public static LoadableModel<String> getCampaignStageLoadableModel(AccessCertificationCampaignType campaign) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (campaign == null) {
                    return "";
                }
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
                int stageNumber = stage != null ? stage.getNumber() : 0;
                int numberOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
                return stageNumber + "/" + numberOfStages;
            }
        };
    }

    public static LoadableModel<String> getCampaignIterationLoadableModel(AccessCertificationCampaignType campaign) {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (campaign == null) {
                    return "";
                }
                return "" + CertCampaignTypeUtil.norm(campaign.getIteration());
            }
        };
    }

    public static void recordCertItemResponse(@NotNull AccessCertificationWorkItemType item,
            AccessCertificationResponseType response, String comment, OperationResult result, Task task, PageBase pageBase) {
        try {
            AccessCertificationCaseType certCase = CertCampaignTypeUtil.getCase(item);
            //todo log error?
            if (certCase == null) {
                return;
            }
            AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(certCase);
            if (campaign == null) {
                return;
            }
            if (response == null) {
                String outcome = item.getOutput() != null ? item.getOutput().getOutcome() : null;
                response = OutcomeUtils.fromUri(outcome);
            }
            pageBase.getCertificationService().recordDecision(
                    campaign.getOid(),
                    certCase.getId(), item.getId(), response, comment, task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    public static List<AbstractGuiAction<AccessCertificationWorkItemType>> mergeCertItemsResponses
            (List<AccessCertificationResponseType> availableResponses, List<GuiActionType> actions, PageBase pageBase) {
        List<AbstractGuiAction<AccessCertificationWorkItemType>> availableActions =
                availableResponses.stream()
                .map(response -> createAction(response, pageBase))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(actions)) {
            return availableActions;
        }
        actions.forEach(action -> {
            AbstractGuiAction<AccessCertificationWorkItemType> actionInstance = createAction(action, pageBase);
            addOrReplaceAction(availableActions, actionInstance);
        });
        return availableActions;
    }

    private static void addOrReplaceAction(List<AbstractGuiAction<AccessCertificationWorkItemType>> availableActions,
            AbstractGuiAction<AccessCertificationWorkItemType> action) {
        availableActions.stream()
                .filter(a -> a.getClass().equals(action.getClass()))
                .findFirst().ifPresent(availableActions::remove);
        availableActions.add(action);
    }

    private static void addIfNotPresent(List<AccessCertificationResponseType> availableResponses,
            AccessCertificationResponseType response) {
        if (availableResponses.stream().noneMatch(r -> r.equals(response))) {
            availableResponses.add(response);
        }
    }

    private static void removeIfPresent(List<AccessCertificationResponseType> availableResponses,
            AccessCertificationResponseType response) {
        availableResponses.removeIf(r -> r.equals(response));
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> createAction(AccessCertificationResponseType response, PageBase pageBase) {
        CertificationItemResponseHelper helper = new CertificationItemResponseHelper(response);
        Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>> actionClass = helper.getGuiActionForResponse();
        if (actionClass == null) {
            pageBase.error("Unable to find action for response: " + response);
            return null;
        }
        return instantiateAction(actionClass, pageBase);
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> createAction(GuiActionType guiAction, PageBase pageBase) {
        Class<? extends AbstractGuiAction<?>> actionClass = pageBase.findGuiAction(guiAction.getIdentifier());
        if (actionClass == null) {
            pageBase.error("Unable to find action for identifier: " + guiAction.getIdentifier());
            return null;
        }
        GuiActionType preAction = guiAction.getPreAction();
        String preActionIdentifier = preAction != null ? preAction.getIdentifier() : null;
        AbstractGuiAction<AccessCertificationWorkItemType> preActionInstance = null;
        if (StringUtils.isNotEmpty(preActionIdentifier)) {
            Class<? extends AbstractGuiAction<?>> preActionClass = pageBase.findGuiAction(preActionIdentifier);
            if (preActionClass != null) {
                preActionInstance = instantiateAction(preActionClass, pageBase);
                if (preAction.getDisplay() != null) {
                    preActionInstance.setActionDisplayType(preAction.getDisplay());
                }
            }
        }
        AbstractGuiAction<AccessCertificationWorkItemType> actionInstance = instantiateAction(actionClass,
                preActionInstance, pageBase);
        if (actionInstance != null) {
                if (guiAction.getVisibility() != null) {
                    actionInstance.setVisible(WebComponentUtil.getElementVisibility(guiAction.getVisibility()));
                }
                if (guiAction.getDisplay() != null) {
                    actionInstance.setActionDisplayType(guiAction.getDisplay());
                }
        }
        return actionInstance;
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> instantiateAction(
            Class<? extends AbstractGuiAction<?>> actionClass, PageBase pageBase) {
        return instantiateAction(actionClass, null, pageBase);
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> instantiateAction(
            Class<? extends AbstractGuiAction<?>> actionClass, AbstractGuiAction<AccessCertificationWorkItemType> preAction,
            PageBase pageBase) {
        ActionType actionType = actionClass.getAnnotation(ActionType.class);
        Class<?> applicableFor = actionType.applicableForType();
        if (!applicableFor.isAssignableFrom(AccessCertificationWorkItemType.class)) {
            pageBase.error("The action is not applicable for AccessCertificationWorkItemType");
            return null;
        }
        if (preAction == null) {
            return WebComponentUtil.instantiateAction(
                    (Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>>) actionClass);
        } else {
            return WebComponentUtil.instantiateAction(
                    (Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>>) actionClass, preAction);
        }
    }

}

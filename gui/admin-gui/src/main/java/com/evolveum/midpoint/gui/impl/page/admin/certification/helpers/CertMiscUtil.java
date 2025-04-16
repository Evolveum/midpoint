/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.certification.helpers;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;
import com.evolveum.midpoint.gui.impl.page.admin.certification.column.AbstractGuiColumn;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.impl.PrismReferenceValueImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.prism.query.builder.S_MatchingRuleEntry;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.ActionType;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.wicket.chartjs.ChartData;
import com.evolveum.wicket.chartjs.ChartDataset;
import com.evolveum.wicket.chartjs.DoughnutChartConfiguration;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;

import static java.util.Collections.singleton;

public class CertMiscUtil {

    private static final Trace LOGGER = TraceManager.getTrace(CertMiscUtil.class);
    private static final String OPERATION_LOAD_CAMPAIGNS_OIDS = "loadCampaignsOids";
    private static final String OPERATION_COUNT_CASES_PROGRESS = "countCasesProgress";
    private static final String OPERATION_COUNT_WORK_ITEMS_PROGRESS = "countWorkItemsProgress";
    private static final String OPERATION_LOAD_ACCESS_CERT_DEFINITION = "loadAccessCertificationDefinition";
    private static final String OPERATION_LOAD_CERTIFICATION_CONFIG = "loadCertificationConfiguration";

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

    public static LoadableDetachableModel<List<ProgressBar>> createCampaignWorkItemsProgressBarModel(AccessCertificationCampaignType campaign,
            String principalOid, PageBase pageBase) {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                int currentStage = or0(campaign.getStageNumber());
                int currentIteration = campaign.getIteration();

                OperationResult result = new OperationResult(OPERATION_COUNT_WORK_ITEMS_PROGRESS);
                Task task = pageBase.createSimpleTask(OPERATION_COUNT_WORK_ITEMS_PROGRESS);

                S_FilterExit queryBasePart = pageBase.getPrismContext().queryFor(AccessCertificationWorkItemType.class)
                        .ownerId(campaign.getOid())
                        .and()
                        .item(AccessCertificationWorkItemType.F_ITERATION)
                        .eq(currentIteration)
                        .and()
                        .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                        .eq(currentStage);

                try {
                    ObjectQuery allWorkItems;
                    if (principalOid != null) {
                        allWorkItems = queryBasePart
                                .and()
                                .item(ItemPath.create(AbstractWorkItemType.F_ASSIGNEE_REF))
                                .ref(principalOid)
                                .build();
                    } else {
                        allWorkItems = queryBasePart
                                .build();
                    }

                    Integer allItemsCount = pageBase.getModelService()
                            .countContainers(AccessCertificationWorkItemType.class, allWorkItems, null, task, result);

                    if (allItemsCount == null || allItemsCount == 0) {
                        ProgressBar allItemsProgressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(allItemsProgressBar);
                    }

                    S_FilterExit processedItemsQueryPart = queryBasePart
                            .and()
                            .not()
                            .item(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                            .isNull();
                    ObjectQuery processedItemsQuery;
                    if (principalOid != null) {
                        processedItemsQuery = processedItemsQueryPart
                                .and()
                                .item(ItemPath.create(AccessCertificationWorkItemType.F_ASSIGNEE_REF))
                                .ref(principalOid)
                                .build();
                    } else {
                        processedItemsQuery = processedItemsQueryPart
                                .build();
                    }

                    Integer processedItemsCount = pageBase.getModelService()
                            .countContainers(AccessCertificationWorkItemType.class, processedItemsQuery, null, task, result);

                    if (processedItemsCount == null) {
                        ProgressBar progressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(progressBar);
                    }

                    float completed = (float) processedItemsCount / allItemsCount * 100;
                    ProgressBar completedProgressBar = new ProgressBar(completed, ProgressBar.State.INFO);
                    return Collections.singletonList(completedProgressBar);
                } catch (Exception ex) {
                    LOGGER.error("Couldn't count certification items", ex);
                    pageBase.showResult(result);
                }
                return Collections.emptyList();
            }
        };
    }

    public static LoadableDetachableModel<List<ProgressBar>> createCampaignCasesProgressBarModel(AccessCertificationCampaignType campaign,
            String principalOid, PageBase pageBase) {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                int currentStage = or0(campaign.getStageNumber());
                int currentIteration = campaign.getIteration();

                OperationResult result = new OperationResult(OPERATION_COUNT_CASES_PROGRESS);
                Task task = pageBase.createSimpleTask(OPERATION_COUNT_CASES_PROGRESS);

                S_FilterExit queryBasePart = pageBase.getPrismContext().queryFor(AccessCertificationCaseType.class)
                        .ownerId(campaign.getOid())
                        .and()
                        .item(AccessCertificationCaseType.F_ITERATION)
                        .eq(currentIteration)
                        .and()
                        .item(AccessCertificationCaseType.F_STAGE_NUMBER)
                        .eq(currentStage);

                try {
                    ObjectQuery allCases;
                    if (principalOid != null) {
                        allCases = queryBasePart
                                .and()
                                .item(ItemPath.create(AccessCertificationCaseType.F_WORK_ITEM, AbstractWorkItemType.F_ASSIGNEE_REF))
                                .ref(principalOid)
                                .build();
                    } else {
                        allCases = queryBasePart
                                .build();
                    }

                    Integer allCasesCount = pageBase.getModelService()
                            .countContainers(AccessCertificationCaseType.class, allCases, null, task, result);

                    if (allCasesCount == null || allCasesCount == 0) {
                        ProgressBar allCasesProgressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(allCasesProgressBar);
                    }

                    S_FilterExit processedCasesQueryPart = queryBasePart
                            .and()
                            .not()
                            .item(AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME)
                            .eq(OutcomeUtils.toUri(NO_RESPONSE));
                    ObjectQuery processedCasesQuery;
                    if (principalOid != null) {
                        processedCasesQuery = processedCasesQueryPart
                                .and()
                                .item(ItemPath.create(AccessCertificationCaseType.F_WORK_ITEM, AccessCertificationWorkItemType.F_ASSIGNEE_REF))
                                .ref(principalOid)
                                .build();
                    } else {
                        processedCasesQuery = processedCasesQueryPart
                                .build();
                    }

                    Integer processedCasesCount = pageBase.getModelService()
                            .countContainers(AccessCertificationCaseType.class, processedCasesQuery, null, task, result);

                    if (processedCasesCount == null) {
                        ProgressBar progressBar = new ProgressBar(0, ProgressBar.State.SECONDARY);
                        return Collections.singletonList(progressBar);
                    }

                    float completed = (float) processedCasesCount / allCasesCount * 100;
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
            if (query == null) {
                return 0;
            }
            count = pageBase.getModelService()
                    .countContainers(AccessCertificationWorkItemType.class, query, null, task, result);
        } catch (Exception ex) {
            LOGGER.error("Couldn't count certification work items", ex);
            pageBase.showResult(result);
        }
        return count;
    }

    public static long countCertItemsForClosedStageAndIteration(AccessCertificationCampaignType campaign,
            boolean notDecidedOnly, PageBase pageBase) {
        if (!AccessCertificationCampaignStateType.REVIEW_STAGE_DONE.equals(campaign.getState())) {
            return 0;
        }
        long count = 0;

        var campaignOid = campaign.getOid();
        var iteration = campaign.getIteration();
        var stage = or0(campaign.getStageNumber());

        Task task = pageBase.createSimpleTask("countCertificationWorkItems");
        OperationResult result = task.getResult();
        try {
            ObjectQuery query = null;

            S_MatchingRuleEntry queryPart = PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .ownerId(campaignOid)
                    .and()
                    .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                    .eq(stage)
                    .and()
                    .item(AccessCertificationWorkItemType.F_ITERATION)
                    .eq(iteration);
            if (notDecidedOnly) {
                query = queryPart
                        .and()
                        .item(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)
                        .isNull()
                        .build();
            } else {
                query = queryPart.build();
            }
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

    public static LoadableDetachableModel<String> getCampaignStageLoadableModel(AccessCertificationCampaignType campaign) {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                if (campaign == null) {
                    return "";
                }
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
                int stageNumber = stage != null ? or0(stage.getNumber()) : campaign.getStageNumber();
                if (CollectionUtils.isNotEmpty(campaign.getStage()) && campaign.getStage().size() < stageNumber) {
                    stageNumber = campaign.getStage().size();
                }
                int numberOfStages = CertCampaignTypeUtil.getNumberOfStages(campaign);
                StringBuilder sb = new StringBuilder();
                sb.append(stageNumber);
                if (numberOfStages > 0) {
                    sb.append("/").append(numberOfStages);
                }
                return sb.toString();
            }
        };
    }

    public static LoadableDetachableModel<String> getCampaignIterationLoadableModel(AccessCertificationCampaignType campaign) {
        return new LoadableDetachableModel<>() {
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
            if (actionInstance != null) {
                addOrReplaceAction(availableActions, actionInstance);
            }
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

    public static AbstractGuiAction<AccessCertificationWorkItemType> createAction(AccessCertificationResponseType response, PageBase pageBase) {
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
        return instantiateAction(actionClass, guiAction, pageBase);
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> instantiateAction(
            Class<? extends AbstractGuiAction<?>> actionClass, PageBase pageBase) {
        return instantiateAction(actionClass, null, pageBase);
    }

    private static AbstractGuiAction<AccessCertificationWorkItemType> instantiateAction(
            Class<? extends AbstractGuiAction<?>> actionClass, GuiActionType actionDto, PageBase pageBase) {

        ActionType actionType = actionClass.getAnnotation(ActionType.class);
        Class<?> applicableFor = actionType.applicableForType();
        if (!applicableFor.isAssignableFrom(AccessCertificationWorkItemType.class)) {
            pageBase.error("The action is not applicable for AccessCertificationWorkItemType");
            return null;
        }
        if (actionDto == null) {
            return WebComponentUtil.instantiateAction(
                    (Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>>) actionClass);
        } else {
            return WebComponentUtil.instantiateAction(
                    (Class<? extends AbstractGuiAction<AccessCertificationWorkItemType>>) actionClass, actionDto);
        }
    }

    public static InlineMenuItem createCampaignMenuItem(IModel<List<AccessCertificationCampaignType>> selectedCampaignsModel,
            CampaignStateHelper.CampaignAction action, PageBase pageBase) {
        InlineMenuItem item = new InlineMenuItem(pageBase.createStringResource(action.getActionLabelKey())) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        final List<AccessCertificationCampaignType> campaigns = new ArrayList<>(selectedCampaignsModel.getObject());
                        if (campaigns.isEmpty()) {
                            IModel<?> rowModel = getRowModel();
                            if (rowModel == null || rowModel.getObject() == null) {
                                pageBase.warn(pageBase.getString("PageCertCampaigns.message.noCampaignsSelected"));
                                target.add(pageBase.getFeedbackPanel());
                                return;
                            }
                            AccessCertificationCampaignType campaign =
                                    (AccessCertificationCampaignType) ((SelectableBean) rowModel.getObject()).getValue();
                            campaigns.add(campaign);
                        }

                        IModel<String> confirmModel;
                        if (campaigns.size() == 1) {
                            String campaignName = LocalizationUtil.translatePolyString(campaigns.get(0).getName());
                            confirmModel = pageBase.createStringResource(action.getConfirmation().getSingleConfirmationMessageKey(),
                                    campaignName);
                        } else {
                            confirmModel = pageBase.createStringResource(action.getConfirmation().getMultipleConfirmationMessageKey(),
                                    campaigns.size());
                        }
                        ConfirmationPanel confirmationPanel = new ConfirmationPanel(pageBase.getMainPopupBodyId(), confirmModel) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void yesPerformed(AjaxRequestTarget target) {
                                OperationResult result = new OperationResult("certificationItemAction");
                                CampaignProcessingHelper.campaignActionConfirmed(campaigns, action, pageBase, target, result);
                                target.add(pageBase);
                            }
                        };
                        pageBase.showMainPopup(confirmationPanel, target);
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem(){
                return action.isBulkAction();
            }
        };

        item.setVisibilityChecker((rowModel, isHeader) -> isMenuItemVisible(rowModel, action, isHeader));
        return item;
    }

    private static boolean isMenuItemVisible(IModel<?> rowModel, CampaignStateHelper.CampaignAction action, boolean isHeader) {
        if (rowModel == null || rowModel.getObject() == null) {
            return true;
        }
        AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) ((SelectableBean) rowModel.getObject()).getValue();
        CampaignStateHelper helper = new CampaignStateHelper(campaign);
        List<CampaignStateHelper.CampaignAction> actionsList = helper.getAvailableActions();
        return actionsList.contains(action);
    }

    public static List<ObjectReferenceType> loadCampaignReviewers(AccessCertificationCampaignType campaign, PageBase pageBase) {
        OperationResult result = new OperationResult("loadCampaignReviewers");
        try {
            var outcomePath = ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME);
            var spec = AggregateQuery.forType(AccessCertificationWorkItemType.class)
                    .resolveNames()
                    .retrieve(AccessCertificationWorkItemType.F_ASSIGNEE_REF) // Resolver assignee
                    .retrieve(AbstractWorkItemOutputType.F_OUTCOME, outcomePath)
                    .count(AccessCertificationCaseType.F_WORK_ITEM, ItemPath.SELF_PATH);

            var campaignOid = campaign.getOid();
            var iteration = campaign.getIteration();
            var stage = or0(campaign.getStageNumber());

            spec.filter(PrismContext.get().queryFor(AccessCertificationWorkItemType.class)
                    .ownerId(campaignOid)
                    .and()
                    .item(AccessCertificationWorkItemType.F_STAGE_NUMBER)
                    .eq(stage)
                    .and()
                    .item(AccessCertificationWorkItemType.F_ITERATION)
                    .eq(iteration)
                    .buildFilter()
            );
            spec.orderBy(spec.getResultItem(AccessCertificationWorkItemType.F_ASSIGNEE_REF), OrderDirection.DESCENDING);

            SearchResultList<PrismContainerValue<?>> reviewersOutcomes = pageBase.getRepositoryService().searchAggregate(spec, result);
            return collectReviewers(reviewersOutcomes);
        } catch (Exception ex) {
            LOGGER.error("Couldn't load campaign reviewers", ex);
            pageBase.showResult(result);
        }
        return new ArrayList<>();
    }

    private static List<ObjectReferenceType> collectReviewers(SearchResultList<PrismContainerValue<?>> reviewersOutcomes) {
        List<ObjectReferenceType> reviewersList = new ArrayList<>();
        reviewersOutcomes.forEach(reviewersOutcome -> {
            Item assigneeItem = reviewersOutcome.findItem(AccessCertificationWorkItemType.F_ASSIGNEE_REF);
            if (assigneeItem == null) {
                return;
            }
            assigneeItem.getValues()
                    .forEach(refValue -> {
                        PrismReferenceValueImpl assignee = (PrismReferenceValueImpl) refValue;
                        ObjectReferenceType assigneeRef = new ObjectReferenceType();
                        assigneeRef.setOid(assignee.getOid());
                        assigneeRef.setType(assignee.getTargetType());
                        if (!alreadyExistInList(reviewersList, assigneeRef)) {
                            reviewersList.add(assigneeRef);
                        }
                    });
        });
        return reviewersList;
    }

    private static boolean alreadyExistInList(List<ObjectReferenceType> reviewersList, ObjectReferenceType ref) {
        return reviewersList
                .stream()
                .anyMatch(r -> r.getOid().equals(ref.getOid()));
    }

    public static List<PrismObject<TaskType>> loadRunningCertTask(String campaignOid, OperationResult result, PageBase pageBase) {
        ObjectQuery query = PrismContext.get().queryFor(TaskType.class)
                .item(ItemPath.create(TaskType.F_AFFECTED_OBJECTS, TaskAffectedObjectsType.F_ACTIVITY,
                        ActivityAffectedObjectsType.F_OBJECTS, BasicObjectSetType.F_OBJECT_REF))
                .ref(campaignOid)
                .and()
                .item(TaskType.F_EXECUTION_STATE)
                .eq(TaskExecutionStateType.RUNNING)
                .build();
        return WebModelServiceUtils.searchObjects(TaskType.class, query, null, result, pageBase);
    }

    public static List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> createCertItemsColumns(
            CompiledObjectCollectionView view, CertificationColumnTypeConfigContext context) {
        PageBase pageBase = context.getPageBase();
        List<IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>> columns = new ArrayList<>();

        List<AbstractGuiColumn<?, ?>> guiColumns = new ArrayList<>();

        List<GuiObjectColumnType> viewColumns = getViewColumns(view);

        viewColumns.forEach(columnConfig -> {
            Class<? extends AbstractGuiColumn<?, ?>> columnClass = pageBase.findGuiColumn(columnConfig.getName());
            AbstractGuiColumn<?, ?> column = instantiateColumn(columnClass, columnConfig, context);
            if (column != null && column.isVisible()) {
                guiColumns.add(column);
            }
        });
        return guiColumns
                .stream()
                .map(column -> (IColumn<PrismContainerValueWrapper<AccessCertificationWorkItemType>, String>) column.createColumn())
                .collect(Collectors.toList());
    }

    private static List<GuiObjectColumnType> getViewColumns(CompiledObjectCollectionView view) {
        List<GuiObjectColumnType> defaultColumns = getCertItemViewDefaultColumns();
        if (view == null || view.getColumns() == null || view.getColumns().isEmpty()) {
            return defaultColumns;
        }
        if (view.isIncludeDefaultColumns()) {
            return applyCustomConfig(defaultColumns, view);
        }
        return new ArrayList<>();
    }

    private static List<GuiObjectColumnType> applyCustomConfig(List<GuiObjectColumnType> defaultColumns, CompiledObjectCollectionView view) {
        if (view == null || view.getColumns() == null) {
            return defaultColumns;
        }
        List<GuiObjectColumnType> columns = new ArrayList<>();
        defaultColumns
                .forEach(c -> {
                    GuiObjectColumnType column = findColumnByName(view.getColumns(), c.getName());
                    if (column == null) {
                        columns.add(c);
                    } else if (WebComponentUtil.getElementVisibility(column.getVisibility())) {
                        columns.add(column);
                    }
                });
        return columns;
    }

    private static GuiObjectColumnType findColumnByName(List<GuiObjectColumnType> columns, String name) {
        if (columns == null) {
            return null;
        }
        return columns.stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private static List<GuiObjectColumnType> getCertItemViewDefaultColumns() {
        List<GuiObjectColumnType> defaultColumns = new ArrayList<>();
        defaultColumns.add(new GuiObjectColumnType().name("certItemObject"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemObjectDisplayName"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemTarget"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemTargetDisplayName"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemReviewers"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemResponse"));
        defaultColumns.add(new GuiObjectColumnType().name("certItemCommentIcon"));
        return defaultColumns;
    }

    private static List<AbstractGuiColumn<?, ?>> addDefaultColumns(List<AbstractGuiColumn<?, ?>> guiColumns,
            CertificationColumnTypeConfigContext context) {
        PageBase pageBase = context.getPageBase();
        pageBase
                .findAllApplicableGuiColumns(AccessCertificationWorkItemType.class)
                .forEach(columnClass -> {
                    if (alreadyExistInColumnList(guiColumns, columnClass)) {
                        return;
                    }
                    AbstractGuiColumn<?, ?> guiColumn = instantiateColumn(columnClass, null, context);
                    if (guiColumn != null) {
                        guiColumns.add(guiColumn);
                    }
                });

        return guiColumns;
    }

    private static boolean alreadyExistInColumnList(List<AbstractGuiColumn<?, ?>> columns, Class<? extends AbstractGuiColumn> column) {
        return columns
                .stream()
                .anyMatch(c -> c.getClass().equals(column));
    }

    private static AbstractGuiColumn<?, ?> instantiateColumn(Class<? extends AbstractGuiColumn> columnClass, GuiObjectColumnType columnConfig,
            CertificationColumnTypeConfigContext context) {
        if (columnClass == null) {
            return null;
        }
        try {
            return ConstructorUtils.invokeConstructor(columnClass, columnConfig, context);
        } catch (Throwable e) {
            LOGGER.trace("No constructor found for column.", e);
        }
        return null;
    }

    public static List<AccessCertificationResponseType> gatherAvailableResponsesForCampaign(String campaignOid,
            PageBase pageBase) {
        List<AccessCertificationResponseType> availableResponses = new AvailableResponses(pageBase).getResponseValues();
        CompiledObjectCollectionView configuredActions = CertMiscUtil.loadCampaignView(pageBase,
                campaignOid);

        if (configuredActions != null) {
            configuredActions.getActions()
                    .forEach(action -> {
                        AccessCertificationResponseType configuredResponse = CertificationItemResponseHelper.getResponseForGuiAction(action);
                        if (configuredResponse == null) {
                            return;
                        }
                        if (!availableResponses.contains(configuredResponse)
                                && WebComponentUtil.getElementVisibility(action.getVisibility())) {
                            availableResponses.add(configuredResponse);
                            return;
                        }
                        if (availableResponses.contains(configuredResponse)
                                && !WebComponentUtil.getElementVisibility(action.getVisibility())) {
                            availableResponses.remove(configuredResponse);
                        }
                    });
        }
        return availableResponses;
    }

    public static CompiledObjectCollectionView loadCampaignView(PageBase pageBase, String campaignOid) {
        Task task = pageBase.createSimpleTask(OPERATION_LOAD_ACCESS_CERT_DEFINITION);
        OperationResult result = task.getResult();

        GuiObjectListViewType campaignDefinitionView = getCollectionViewConfigurationFromCampaignDefinition(campaignOid, task,
                result, pageBase);

        GuiObjectListViewType defaultView = null;
        try {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_CERTIFICATION_CONFIG);
            var certificationConfig = pageBase.getModelInteractionService().getCertificationConfiguration(subResult);
            if (certificationConfig != null) {
                defaultView = certificationConfig.getDefaultView();
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't load certification configuration from system configuration, ", e);
        }

        if (campaignDefinitionView == null && defaultView == null) {
            return null;
        }

        try {
            CompiledObjectCollectionView compiledView = new CompiledObjectCollectionView();
            compiledView.setContainerType(AccessCertificationWorkItemType.COMPLEX_TYPE);

            if (defaultView != null) {
                pageBase.getModelInteractionService().compileView(compiledView, defaultView, task, result);
            }
            if (campaignDefinitionView != null) {
                pageBase.getModelInteractionService().compileView(compiledView, campaignDefinitionView, task, result);
            }

            return compiledView;
        } catch (Exception e) {
            LOGGER.error("Couldn't load certification work items view, ", e);
        }
        return null;
    }

    private static GuiObjectListViewType getCollectionViewConfigurationFromCampaignDefinition(String campaignOid, Task task,
            OperationResult result, PageBase pageBase) {
        if (campaignOid == null) {
            return null;
        }
        var campaign = WebModelServiceUtils.loadObject(AccessCertificationCampaignType.class, campaignOid, pageBase, task, result);
        if (campaign == null) {
            return null;
        }
        var definitionRef = campaign.asObjectable().getDefinitionRef();
        if (definitionRef == null) {
            return null;
        }
        PrismObject<AccessCertificationDefinitionType> definitionObj = WebModelServiceUtils.loadObject(definitionRef, pageBase, task, result);
        if (definitionObj == null) {
            return null;
        }
        AccessCertificationDefinitionType definition = definitionObj.asObjectable();
        return definition.getView();
    }


}

/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.LinkedReferencePanel;
import com.evolveum.midpoint.web.page.admin.certification.PageAdminCertification;

import com.evolveum.midpoint.web.page.admin.certification.helpers.CertificationItemResponseHelper;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.NavigationPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/certification/campaign",
                        matchUrlForSecurity = "/admin/certification/campaign")
        },
        encoder = OnePageParameterEncoder.class,
        action = {
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_ALL,
                label = PageAdminCertification.AUTH_CERTIFICATION_ALL_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS,
                label = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_LABEL,
                description = PageAdminCertification.AUTH_CERTIFICATION_CAMPAIGNS_DESCRIPTION) }
)
public class PageCertCampaign extends PageAdmin {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageCertCampaign.class);
    private static final String DOT_CLASS = PageCertCampaign.class.getName() + ".";
    private static final String OPERATION_LOAD_CAMPAIGN = DOT_CLASS + "loadCampaign";
    private static final String OPERATION_LOAD_STATISTICS = DOT_CLASS + "loadStatistics";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_DETAILS = "details";
    private static final String ID_RESPONSES_CONTAINER = "responsesContainer";
    private static final String ID_RESPONSES = "responses";
    private static final String ID_ITEMS_TABBED_PANEL = "itemsTabbedPanel";
    private IModel<AccessCertificationCampaignType> campaignModel;
    private LoadableModel<AccessCertificationCasesStatisticsType> statisticsModel;

    private IModel<List<DetailsTableItem>> detailsModel;

    public PageCertCampaign() {
        this(new PageParameters());
    }

    public PageCertCampaign(PageParameters parameters) {
        super(parameters);

        initModels();
        initLayout();
    }

    private void initModels() {
        campaignModel = new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected AccessCertificationCampaignType load() {
                return loadCampaign();
            }
        };

        detailsModel = new LoadableModel<>(false) {

            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected List<DetailsTableItem> load() {
                List<DetailsTableItem> list = new ArrayList<>();
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.owner"),
                        () -> "" ) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new LinkedReferencePanel<>(id, Model.of(campaignModel.getObject().getOwnerRef())) {

                            @Override
                            protected String getAdditionalCssStyle() {
                                return "";
                            }
                        };
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.iteration"),
                        () -> "" + CertCampaignTypeUtil.norm(campaignModel.getObject().getIteration())));
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.progress"),
                        () -> "" ) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new ProgressBarPanel(id, createCampaignProgressModel());
                    }
                }); //todo calculate progress
                list.add(new DetailsTableItem(createStringResource("PageCertDefinition.numberOfStages"),
                        () -> "" + campaignModel.getObject().getStage().size()));
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaignModel.getObject());
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.currentState"),
                        null) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        BadgePanel status = new BadgePanel(id, createBadgeModel());
                        status.setOutputMarkupId(true);
                        return status;
                    }

                    private IModel<Badge> createBadgeModel() {
                        Badge badge = new Badge("colored-form-info", resolveCurrentStateName());
                        return Model.of(badge);
                    }
                });
                list.add(new DetailsTableItem(createStringResource("PageCertCampaign.table.deadline"),
                        null) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public Component createValueComponent(String id) {
                        return new DeadlinePanel(id, getDeadlineModel());
                    }

                    private IModel<XMLGregorianCalendar> getDeadlineModel() {
                        return () -> stage != null ? stage.getDeadline() : null;
                    }
                });

                return list;
            }
        };

        statisticsModel = new LoadableModel<>(false) {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected AccessCertificationCasesStatisticsType load() {
                return loadStatistics();
            }
        };
    }

    private AccessCertificationCasesStatisticsType loadStatistics() {
        Task task = createSimpleTask(OPERATION_LOAD_STATISTICS);
        OperationResult result = task.getResult();
        AccessCertificationCasesStatisticsType stat = null;
        try {
            stat = getCertificationService().getCampaignStatistics(campaignModel.getObject().getOid(),
                    false, task, result);
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign statistics", ex);
            result.recordFatalError(getString("PageCertCampaign.message.loadStatistics.fatalerror"), ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }
        return stat;
    }

    @NotNull
    private AccessCertificationCampaignType loadCampaign() {
        Task task = createSimpleTask(OPERATION_LOAD_CAMPAIGN);
        OperationResult result = task.getResult();
        AccessCertificationCampaignType campaign = null;
        try {
            String campaignOid = OnePageParameterEncoder.getParameter(this);
            PrismObject<AccessCertificationCampaignType> campaignObject =
                    WebModelServiceUtils.loadObject(AccessCertificationCampaignType.class, campaignOid,
                            PageCertCampaign.this, task, result);
            if (campaignObject != null) {
                campaign = campaignObject.asObjectable();
            }
            result.recordSuccessIfUnknown();
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get campaign", ex);
            result.recordFatalError(getString("PageCertCampaign.message.loadCampaign.fatalerror"), ex);
        }
        result.recomputeStatus();

        if (!WebComponentUtil.isSuccessOrHandledError(result)) {
            showResult(result);
        }
        if (campaign != null) {
            return campaign;
        } else {
            throw redirectBackViaRestartResponseException();
        }
    }

    private void initLayout() {
        NavigationPanel navigation = new NavigationPanel(ID_NAVIGATION) {

            @Serial private static final long serialVersionUID = 1L;
            @Override
            protected IModel<String> createTitleModel() {
                return createStringResource("PageCertCampaign.campaignView");
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                PageCertCampaign.this.onBackPerformed();
            }

            @Override
            protected Component createNextButton(String id, IModel<String> nextTitle) {
                return new WebMarkupContainer(id);
            }
        };
        add(navigation);

        DisplayType displayType = new DisplayType()
                .label(WebComponentUtil.getName(campaignModel.getObject()))
                .help(campaignModel.getObject().getDescription())
                .icon(new IconType()
                        .cssClass(getDetailsTablePanelIconCssClass()));
        DetailsTablePanel details = new DetailsTablePanel(ID_DETAILS, Model.of(displayType), detailsModel);
        details.setOutputMarkupId(true);
        add(details);

        SimpleContainerPanel responsesContainer = new SimpleContainerPanel(ID_RESPONSES_CONTAINER,
                createStringResource("PageCertCampaign.statistics.responses"));
        responsesContainer.add(AttributeModifier.append("class", "card p-4"));
        responsesContainer.setOutputMarkupId(true);
        add(responsesContainer);

        ProgressBarPanel responsesPanel = new ProgressBarPanel(ID_RESPONSES, createResponseStatisticsModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isSimpleView() {
                return false;
            }

            @Override
            protected boolean isPercentageBar() {
                return false;
            }
        };
        responsesPanel.setOutputMarkupId(true);
        responsesContainer.add(responsesPanel);


        CertificationItemsTabbedPanel items = new CertificationItemsTabbedPanel(ID_ITEMS_TABBED_PANEL, campaignModel);
        items.setOutputMarkupId(true);
        add(items);
    }

    private String getDetailsTablePanelIconCssClass() {
        return IconAndStylesUtil.createDefaultColoredIcon(AccessCertificationCampaignType.COMPLEX_TYPE);
    }

    private void onBackPerformed() {
        redirectBack();
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return () -> null;
    }

    private IModel<String> createTitleModel() {
        return () -> WebComponentUtil.getDisplayNameOrName(campaignModel.getObject().asPrismObject());
    }

    @Override
    protected void createBreadcrumb() {
        addBreadcrumb(new Breadcrumb(createTitleModel(), this.getClass(), getPageParameters()));
    }

    private String resolveCurrentStateName() {
        int stageNumber = campaignModel.getObject().getStageNumber();
        AccessCertificationCampaignStateType state = campaignModel.getObject().getState();
        switch (state) {
            case CREATED:
            case IN_REMEDIATION:
            case CLOSED:
                return createStringResourceStatic(PageCertCampaign.this, state).getString();
            case IN_REVIEW_STAGE:
            case REVIEW_STAGE_DONE:
                AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaignModel.getObject());
                String stageName = stage != null ? stage.getName() : null;
                if (stageName != null) {
                    String key = createEnumResourceKey(state) + "_FULL";
                    return createStringResourceStatic(key, stageNumber, stageName).getString();
                } else {
                    String key = createEnumResourceKey(state);
                    return createStringResourceStatic(key).getString() + " " + stageNumber;
                }
            default:
                return null;        // todo warning/error?
        }
    }

    private @NotNull LoadableModel<List<ProgressBar>> createCampaignProgressModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                AccessCertificationCampaignType campaign = campaignModel.getObject();
                float completed = CertCampaignTypeUtil.getCasesCompletedPercentageAllStagesAllIterations(campaign);

                ProgressBar progressBar = new ProgressBar(completed, ProgressBar.State.INFO);
                return Collections.singletonList(progressBar);
            }
        };
    }

    private @NotNull LoadableModel<List<ProgressBar>> createResponseStatisticsModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {

                List<ProgressBar> progressBars = new ArrayList<>();

                CertificationItemResponseHelper responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.ACCEPT);
                ProgressBar accepted = new ProgressBar(statisticsModel.getObject().getMarkedAsAccept(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(accepted);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REVOKE);
                ProgressBar revoked = new ProgressBar(statisticsModel.getObject().getMarkedAsRevoke(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(revoked);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REDUCE);
                ProgressBar reduced = new ProgressBar(statisticsModel.getObject().getMarkedAsReduce(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(reduced);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.NOT_DECIDED);
                ProgressBar notDecided = new ProgressBar(statisticsModel.getObject().getMarkedAsNotDecide(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(notDecided);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.NO_RESPONSE);
                ProgressBar noResponse = new ProgressBar(statisticsModel.getObject().getWithoutResponse(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(noResponse);
                return progressBars;
            }
        };
    }

    private double getResponsesProgressBarValue(int responsesCount) {
        int totalCount = statisticsModel.getObject().getMarkedAsAccept() +
                statisticsModel.getObject().getMarkedAsRevoke() +
                statisticsModel.getObject().getMarkedAsReduce() +
                statisticsModel.getObject().getMarkedAsNotDecide() +
                statisticsModel.getObject().getWithoutResponse();
        return totalCount > 0 ? (double) responsesCount / totalCount * 100 : 0;
    }
}

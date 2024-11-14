/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.AvailableResponses;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimpleContainerPanel;
import com.evolveum.midpoint.util.SingleLocalizableMessage;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertificationItemResponseHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "certResponses")
@PanelInstance(identifier = "certResponses",
        applicableForType = AccessCertificationCampaignType.class,
        defaultPanel = true,// change later to assignmentHolder type, probably we will want org assignments later
        display = @PanelDisplay(label = "ResponsesOverviewPanel.responsesPanel.label" /*TODO better name*/,
                icon = GuiStyleConstants.CLASS_OBJECT_CERT_CAMPAIGN_ICON, order = 1))
// TODO @Counter(provider = AssignmentCounter.class)
public class ResponsesOverviewPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResponsesOverviewPanel.class);

    private static final String DOT_CLASS = ResponsesOverviewPanel.class.getName() + ".";

    private static final String ID_RESPONSES_CONTAINER = "responsesContainer";
    private static final String ID_RESPONSES = "responses";
    private static final String ID_REMEDIED_CONTAINER = "remediedContainer";
    private static final String ID_REMEDIED_ITEMS_PANEL = "remediedItemsPanel";
    private static final String ID_ITEMS_TABBED_PANEL = "itemsTabbedPanel";

    public ResponsesOverviewPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        initResponsesProgressBarPanel();
        initRemediedItemsProgressBarPanel();

        addOrReplaceCertItemsTabbedPanel();
    }

    private void initResponsesProgressBarPanel() {
        SimpleContainerPanel responsesContainer = new SimpleContainerPanel(ID_RESPONSES_CONTAINER,
                createStringResource("PageCertCampaign.statistics.responses"));
        responsesContainer.setOutputMarkupId(true);
        responsesContainer.add(AttributeAppender.append("class", isRemediedItemsVisible() ? "col-8" : "w-100"));
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

            protected IModel<String> getFormattedProgressBarValueModel(ProgressBar progressBar) {
                return () -> (int) progressBar.getValue() + "";
            }
        };
        responsesPanel.setOutputMarkupId(true);
        responsesContainer.add(responsesPanel);
    }

    private void initRemediedItemsProgressBarPanel() {
        SimpleContainerPanel remediedItemsContainer = new SimpleContainerPanel(ID_REMEDIED_CONTAINER,
                createStringResource("PageCertCampaign.statistics.remediedItems"));
        remediedItemsContainer.setOutputMarkupId(true);
        remediedItemsContainer.add(new VisibleBehaviour(this::isRemediedItemsVisible));
        add(remediedItemsContainer);

        ProgressBarPanel remediedItemsPanel = new ProgressBarPanel(ID_REMEDIED_ITEMS_PANEL, createRemediedItemsStatisticModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected boolean isSimpleView() {
                return false;
            }

            @Override
            protected boolean isPercentageBar() {
                return false;
            }

            protected IModel<String> getFormattedProgressBarValueModel(ProgressBar progressBar) {
                return () -> (int) progressBar.getValue() + "";
            }
        };
        remediedItemsPanel.setOutputMarkupId(true);
        remediedItemsContainer.add(remediedItemsPanel);
    }

    private boolean isRemediedItemsVisible() {
        return getStatistics().getMarkedAsRevokeAndRemedied() > 0 || getStatistics().getMarkedAsReduceAndRemedied() > 0;
    }

    private AccessCertificationCasesStatisticsType getStatistics() {
        return getObjectDetailsModels().getCertStatisticsModel().getObject();
    }

    private @NotNull LoadableModel<List<ProgressBar>> createResponseStatisticsModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                List<ProgressBar> progressBars = new ArrayList<>();

                List<AccessCertificationResponseType> availableResponses = getAvailableResponses();

                AccessCertificationCasesStatisticsType statisticsType = getStatistics();
                CertificationItemResponseHelper responseHelper = null;
                if (availableResponses.contains(AccessCertificationResponseType.ACCEPT)) {
                    responseHelper = new CertificationItemResponseHelper(AccessCertificationResponseType.ACCEPT);
                    ProgressBar accepted = new ProgressBar(statisticsType.getMarkedAsAccept(),
                            responseHelper.getProgressBarState(),
                            new SingleLocalizableMessage(responseHelper.getLabelKey()));
                    progressBars.add(accepted);
                }

                if (availableResponses.contains(AccessCertificationResponseType.REVOKE)) {
                    responseHelper =
                            new CertificationItemResponseHelper(AccessCertificationResponseType.REVOKE);
                    ProgressBar revoked = new ProgressBar(statisticsType.getMarkedAsRevoke(),
                            responseHelper.getProgressBarState(),
                            new SingleLocalizableMessage(responseHelper.getLabelKey()));
                    progressBars.add(revoked);
                }

                if (availableResponses.contains(AccessCertificationResponseType.REDUCE)) {
                    responseHelper =
                            new CertificationItemResponseHelper(AccessCertificationResponseType.REDUCE);
                    ProgressBar reduced = new ProgressBar(statisticsType.getMarkedAsReduce(),
                            responseHelper.getProgressBarState(),
                            new SingleLocalizableMessage(responseHelper.getLabelKey()));
                    progressBars.add(reduced);
                }

                if (availableResponses.contains(AccessCertificationResponseType.NOT_DECIDED)) {
                    responseHelper =
                            new CertificationItemResponseHelper(AccessCertificationResponseType.NOT_DECIDED);
                    ProgressBar notDecided = new ProgressBar(statisticsType.getMarkedAsNotDecide(),
                            responseHelper.getProgressBarState(),
                            new SingleLocalizableMessage(responseHelper.getLabelKey()));
                    progressBars.add(notDecided);
                }

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.NO_RESPONSE);
                ProgressBar noResponse = new ProgressBar(statisticsType.getWithoutResponse(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(noResponse);
                return progressBars;
            }
        };
    }

    private List<AccessCertificationResponseType> getAvailableResponses() {
        return CertMiscUtil.gatherAvailableResponsesForCampaign(getObjectWrapperModel().getObject().getOid(), getPageBase());
    }

    private @NotNull LoadableModel<List<ProgressBar>> createRemediedItemsStatisticModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<ProgressBar> load() {
                List<ProgressBar> progressBars = new ArrayList<>();

                AccessCertificationCasesStatisticsType statisticsType = getStatistics();
                CertificationItemResponseHelper responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REVOKE);
                ProgressBar remediedRevoked = new ProgressBar(statisticsType.getMarkedAsRevokeAndRemedied(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(remediedRevoked);

                responseHelper =
                        new CertificationItemResponseHelper(AccessCertificationResponseType.REDUCE);
                ProgressBar reduced = new ProgressBar(statisticsType.getMarkedAsReduceAndRemedied(),
                        responseHelper.getProgressBarState(),
                        new SingleLocalizableMessage(responseHelper.getLabelKey()));
                progressBars.add(reduced);

                return progressBars;
            }
        };
    }

    private void addOrReplaceCertItemsTabbedPanel() {
        CertificationItemsTabbedPanel items = new CertificationItemsTabbedPanel(ID_ITEMS_TABBED_PANEL,
                new LoadableDetachableModel<PrismObjectWrapper<AccessCertificationCampaignType>>() {
                    @Override
                    protected PrismObjectWrapper<AccessCertificationCampaignType> load() {
                        return getObjectWrapper();
                    }
                });
        items.setOutputMarkupId(true);
        addOrReplace(items);
    }
}

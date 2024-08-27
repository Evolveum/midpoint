/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBar;
import com.evolveum.midpoint.gui.api.component.progressbar.ProgressBarPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.PageCertCampaign;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.IsolatedCheckBoxPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CertMiscUtil;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignProcessingHelper;
import com.evolveum.midpoint.gui.impl.page.admin.certification.helpers.CampaignStateHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serial;
import java.util.Collections;
import java.util.List;

public class CampaignTilePanel extends BasePanel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> {

    @Serial private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(CampaignTilePanel.class);
    private static final String DOT_CLASS = CampaignTilePanel.class.getName() + ".";

    private static final String ID_SELECT_TILE_CHECKBOX = "selectTileCheckbox";
    private static final String ID_STATUS = "status";
    private static final String ID_REDIRECT_TO_TASK_BUTTON = "redirectToTaskButton";
    private static final String ID_MENU = "menu";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_PROGRESS_BAR = "progressBar";
    private static final String ID_DEADLINE = "deadline";
    private static final String ID_STAGE = "stage";
    private static final String ID_ITERATION = "iteration";
    private static final String ID_ACTION_BUTTON = "actionButton";
    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_LABEL = "detailsLabel";

    String runningTaskOid;

    public CampaignTilePanel(String id, IModel<TemplateTile<SelectableBean<AccessCertificationCampaignType>>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {
        add(AttributeAppender.append("class",
                "campaign-tile-panel catalog-tile-panel d-flex flex-column align-items-center rounded p-3 elevation-1"));

        setOutputMarkupId(true);

        IsolatedCheckBoxPanel selectTileCheckbox = new IsolatedCheckBoxPanel(ID_SELECT_TILE_CHECKBOX, getSelectedModel());
        selectTileCheckbox.setOutputMarkupId(true);
        selectTileCheckbox.setVisible(false); // TODO temp set visible to true after bulk actions are implemented
        add(selectTileCheckbox);

        BadgePanel status = new BadgePanel(ID_STATUS, getStatusModel());
        status.setOutputMarkupId(true);
        status.add(new VisibleBehaviour(this::isAuthorizedForCampaignActions));
        add(status);

        AjaxLink<Void> redirectToTaskButton = new AjaxLink<>(ID_REDIRECT_TO_TASK_BUTTON) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(runningTaskOid, ObjectTypes.TASK);
                DetailsPageUtil.dispatchToObjectDetailsPage(ref, CampaignTilePanel.this, false);
            }
        };
        redirectToTaskButton.setOutputMarkupId(true);
        redirectToTaskButton.add(AttributeAppender.append("title", createStringResource("PageCertCampaign.button.showRunningTask")));
        redirectToTaskButton.add(new VisibleBehaviour(() -> isAuthorizedForCampaignActions() && StringUtils.isNotEmpty(runningTaskOid)));
        add(redirectToTaskButton);

        DropdownButtonPanel menu = new DropdownButtonPanel(ID_MENU, createMenuDropDownButtonModel().getObject()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String getSpecialButtonClass() {
                return "";
            }

            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

        };
        menu.add(new VisibleBehaviour(this::isAuthorizedForCampaignActions));
        menu.setOutputMarkupId(true);
        add(menu);

        Label title = new Label(ID_TITLE, getTitleModel());
        title.add(AttributeAppender.append("title", getTitleModel()));
        title.setOutputMarkupId(true);
        add(title);

        Label description = new Label(ID_DESCRIPTION, Model.of(getModelObject().getDescription()));
        description.setOutputMarkupId(true);
        add(description);

        ProgressBarPanel progressBar = new ProgressBarPanel(ID_PROGRESS_BAR,
                createCampaignProgressModel());
        progressBar.setOutputMarkupId(true);
        add(progressBar);

        DeadlinePanel deadline = new DeadlinePanel(ID_DEADLINE, getDeadlineModel());
        deadline.setOutputMarkupId(true);
        add(deadline);

        Label stage = new Label(ID_STAGE, getStageModel());
        stage.setOutputMarkupId(true);
        add(stage);

        Label iteration = new Label(ID_ITERATION, getIterationModel());
        iteration.setOutputMarkupId(true);
        add(iteration);

        LoadableDetachableModel<String> buttonLabelModel = getActionButtonTitleModel();

        CampaignActionButton actionButton = new CampaignActionButton(ID_ACTION_BUTTON, getPageBase(), getCampaignModel(), buttonLabelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void refresh(AjaxRequestTarget target) {
                runningTaskOid = getRunningTaskOid();
                buttonLabelModel.detach();
                target.add(CampaignTilePanel.this);
                Component feedbackPanel = getPageBase().getFeedbackPanel();
                target.add(feedbackPanel);
            }

        };
        actionButton.setOutputMarkupPlaceholderTag(true);
        actionButton.add(AttributeModifier.append("class", getActionButtonCssModel()));
        actionButton.setOutputMarkupId(true);
        actionButton.add(new VisibleBehaviour(this::isAuthorizedForCampaignActions));
        add(actionButton);

        AjaxLink<Void> details = new AjaxLink<>(ID_DETAILS) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                detailsButtonClickPerformed(target);
            }
        };
        details.setOutputMarkupId(true);
        add(details);

        Label detailsLabel = new Label(ID_DETAILS_LABEL, getDetailsButtonLabelModel());
        details.add(detailsLabel);

    }

    private LoadableModel<String> getActionButtonCssModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getCampaign());
                return campaignStateHelper.getNextAction().getActionCssClass();
            }
        };
    }

    private IModel<Boolean> getSelectedModel() {
        return new IModel<>() {
            @Serial private static final long serialVersionUID = 1L;
            @Override
            public Boolean getObject() {
                return getModelObject().isSelected();
            }

            @Override
            public void setObject(Boolean object) {
                getModelObject().setSelected(object);
            }
        };
    }

    private LoadableDetachableModel<Badge> getStatusModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Badge load() {
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getCampaign());
                return campaignStateHelper.createBadge();
            }
        };
    }

    private LoadableModel<DropdownButtonDto> createMenuDropDownButtonModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DropdownButtonDto load() {
                DropdownButtonDto button = new DropdownButtonDto(null, "fa fa-ellipsis-v", null,
                        createMenuItemsModel().getObject());
                return button;
            }
        };
    }

    private LoadableModel<List<InlineMenuItem>> createMenuItemsModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<InlineMenuItem> load() {
                List<AccessCertificationCampaignType> campaignList = Collections.singletonList(getCampaign());
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getCampaign());
                List<CampaignStateHelper.CampaignAction> actionsList = campaignStateHelper.getAvailableActions();
                return actionsList
                        .stream()
                        .map(a -> CertMiscUtil.createCampaignMenuItem(Model.ofList(campaignList), a, getPageBase()))
                        .toList();
            }

        };
    }

    private LoadableModel<String> getTitleModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return WebComponentUtil.getName(getCampaign());
            }
        };
    }

    public LoadableDetachableModel<AccessCertificationCampaignType> getCampaignModel() {
        return new LoadableDetachableModel<AccessCertificationCampaignType>() {
            @Override
            protected AccessCertificationCampaignType load() {
                return getCampaign();
            }
        };
    }

    protected AccessCertificationCampaignType getCampaign() {
        return getModelObject().getValue().getValue();
    }

    private LoadableModel<XMLGregorianCalendar> getDeadlineModel() {
        return new LoadableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected XMLGregorianCalendar load() {
                return CampaignProcessingHelper.computeDeadline(getCampaign(), getPageBase());
            }
        };
    }

    private LoadableModel<String> getStageModel() {
        return CertMiscUtil.getCampaignStageLoadableModel(getCampaign());
    }

    private LoadableModel<String> getIterationModel() {
        return CertMiscUtil.getCampaignIterationLoadableModel(getCampaign());
    }

    protected boolean isAuthorizedForCampaignActions() {
        return true;
    }

    protected MidPointPrincipal getPrincipal() {
        return null;
    }

    protected IModel<String> getDetailsButtonLabelModel() {
        return createStringResource("CatalogTilePanel.details");
    }

    protected void detailsButtonClickPerformed(AjaxRequestTarget target) {
        CampaignProcessingHelper.campaignDetailsPerformed(getCampaign().getOid(), getPageBase());
    }

    /**
     * In case the campaign tile is a part of campaign view,
     * the progress of the processed cases should be counted.
     * In case the campaign tile is a part of certification items view,
     * the progress of the processed cert. items should be counted.
     *
     * @return
     */
    protected LoadableModel<List<ProgressBar>> createCampaignProgressModel() {
        return null;
    }

    private LoadableDetachableModel<String> getActionButtonTitleModel() {
        return new LoadableDetachableModel<>() {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                CampaignStateHelper campaignStateHelper = new CampaignStateHelper(getCampaign());
                return createStringResource(campaignStateHelper.getNextAction().getActionLabelKey()).getString();
            }
        };
    }
}

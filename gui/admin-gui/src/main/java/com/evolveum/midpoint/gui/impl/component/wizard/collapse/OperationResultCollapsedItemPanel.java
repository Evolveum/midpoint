/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class OperationResultCollapsedItemPanel extends BasePanel<OperationResultCollapsedItem> {

    public enum ResultType {
        ERROR("fa fa-exclamation-circle text-danger", "callout callout-danger"),
        WARNING("fa fa-exclamation-triangle text-warning", "callout callout-warning"),
        UNKNOWN("fa fa-question-circle text-info", "callout callout-info");

        final String css;
        final String icon;

        ResultType(String icon, String css) {
            this.css = css;
            this.icon = icon;
        }

        public String getCss() {
            return css;
        }

        public String getIcon() {
            return icon;
        }
    }

    private final WizardModelWithParentSteps wizardModel;

    public OperationResultCollapsedItemPanel(String id, IModel<OperationResultCollapsedItem> model, WizardModelWithParentSteps wizardModel) {
        super(id, model);
        this.wizardModel = wizardModel;
    }

    private static final String ID_RESULT = "result";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_TEXT = "detailsText";
    private static final String ID_FIX_BUTTON = "fixButton";
    private static final String ID_SHOW_DETAILS_BUTTON = "showDetailsButton";

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        ListView<OperationResultWrapper> results = new ListView<>(ID_RESULT, () -> getModelObject().getResults()) {
            @Override
            protected void populateItem(ListItem<OperationResultWrapper> item) {
                OperationResultWrapper resultWrapper = item.getModelObject();
                OperationResult result = resultWrapper.getResult();
                ResultType resultType = defineResultType(result);
                OpResult opResult = OpResult.getOpResult(getPageBase(), result);

                item.add(AttributeAppender.append("class", resultType.getCss()));

                Label title = getTitle(opResult);
                item.add(title);

                WebMarkupContainer iconButton = new WebMarkupContainer(ID_ICON);
                iconButton.add(AttributeAppender.append("class", resultType.getIcon()));
                item.add(iconButton);

                WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
                details.setOutputMarkupId(true);
                details.add(new VisibleBehaviour(resultWrapper::isExpanded));
                item.add(details);

                AjaxIconButton expandButton = getExpandButton(resultWrapper, details);
                item.add(expandButton);

                Label detailText = new Label(ID_DETAILS_TEXT, opResult.getExceptionMessage());
                detailText.add(AttributeAppender.append("title", opResult.getExceptionMessage()));
                detailText.setOutputMarkupId(true);
                details.add(detailText);

                AjaxIconButton fixButton = getFixButton(resultWrapper);
                details.add(fixButton);

                AjaxButton showDetailsButton = getShowDetailsButton(result);
                details.add(showDetailsButton);
            }

            private @NotNull AjaxButton getShowDetailsButton(OperationResult result) {
                AjaxButton showDetailsButton = new AjaxButton(ID_SHOW_DETAILS_BUTTON,
                        createStringResource("OperationResultCollapsedItemPanel.showDetailsButton")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        OperationResultPopupPanel body = new OperationResultPopupPanel(
                                getPageBase().getMainPopupBodyId(),
                                new Model<>(result));
                        body.setOutputMarkupId(true);
                        getPageBase().showMainPopup(body, target);
                    }
                };
                showDetailsButton.setOutputMarkupId(true);
                return showDetailsButton;
            }

            private @NotNull AjaxIconButton getFixButton(OperationResultWrapper resultWrapper) {
                AjaxIconButton fixButton = new AjaxIconButton(ID_FIX_BUTTON,
                        () -> "fa fa-wrench",
                        createStringResource("OperationResultCollapsedItemPanel.fixButton")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        wizardModel.setActiveStepById(resultWrapper.getFixPanelId());
                        wizardModel.fireActiveStepChanged();
                        target.add(wizardModel.getPanel());
                    }
                };
                fixButton.add(new VisibleBehaviour(() -> !Strings.CS.equals(wizardModel.getActiveStep().getStepId(), resultWrapper.getFixPanelId())));
                fixButton.setOutputMarkupId(true);
                fixButton.showTitleAsLabel(true);
                return fixButton;
            }

            private @NotNull AjaxIconButton getExpandButton(OperationResultWrapper resultWrapper, WebMarkupContainer details) {
                AjaxIconButton expandButton = new AjaxIconButton(ID_EXPAND_COLLAPSE_BUTTON,
                        () -> resultWrapper.isExpanded() ? "fa fa-chevron-down" : "fa fa-chevron-right",
                        () -> resultWrapper.isExpanded() ? getString("OperationResultCollapsedItemPanel.collapse") : getString("OperationResultCollapsedItemPanel.expand")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        resultWrapper.setExpanded(!resultWrapper.isExpanded());
                        target.add(OperationResultCollapsedItemPanel.this);
                        target.add(details);
                    }
                };
                expandButton.setOutputMarkupId(true);
                return expandButton;
            }

            private @NotNull Label getTitle(OpResult result) {
                IModel<String> titleModel = () -> {
                    LocalizableMessage userFriendlyMessage = result.getUserFriendlyMessage();
                    String bodyMessage;
                    if (userFriendlyMessage != null) {
                        bodyMessage = WebComponentUtil.resolveLocalizableMessage(userFriendlyMessage, getPageBase());
                    } else {
                        bodyMessage = getString(result.getMessage(), null, result.getMessage());
                    }
                    return bodyMessage;
                };
                Label detailLabel = new Label(ID_TITLE, titleModel);
                detailLabel.add(AttributeAppender.append("title", titleModel));
                detailLabel.setOutputMarkupId(true);
                return detailLabel;
            }
        };
        results.setOutputMarkupId(true);
        add(results);
    }

    private ResultType defineResultType(OperationResult result) {
        if (result.isError()) {
            return ResultType.ERROR;
        }
        if (result.isWarning()) {
            return ResultType.WARNING;
        }
        return ResultType.UNKNOWN;
    }

}

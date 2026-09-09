/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse;

import java.io.Serial;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.OpResult;
import com.evolveum.midpoint.gui.api.component.result.OperationResultPopupPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.OperationResultCollapsedItemPanel.ResultType;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.ProcessedExceptionWrapper.Key;
import com.evolveum.midpoint.gui.impl.component.wizard.withnavigation.WizardModelWithParentSteps;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Shows one processed exception in the wizard drawer: a title, the script it comes from, a map of
 * key-value pairs, a link to the full stack trace and an explanatory text above the action buttons.
 *
 * The fix button behaves the same as in the plain operation result entry - it navigates to the
 * script the failure belongs to, or runs the action the caller supplied instead.
 */
public class ProcessedExceptionPanel extends BasePanel<OperationResultWrapper> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";
    private static final String ID_DETAILS = "details";
    private static final String ID_SCRIPT_ROW = "scriptRow";
    private static final String ID_SCRIPT_VALUE = "scriptValue";
    private static final String ID_MAP_ROW = "mapRow";
    private static final String ID_MAP_LABEL = "mapLabel";
    private static final String ID_MAP_VALUE = "mapValue";
    private static final String ID_STACK_TRACE_LINK = "stackTraceLink";
    private static final String ID_TEXT = "text";
    private static final String ID_IGNORE_BUTTON = "ignoreButton";
    private static final String ID_FIX_BUTTON = "fixButton";

    private static final String KEY_MAP_PREFIX = "ProcessedExceptionPanel.key.";

    private final WizardModelWithParentSteps wizardModel;

    private ProcessedExceptionWrapper exception;

    public ProcessedExceptionPanel(
            String id, IModel<OperationResultWrapper> model, WizardModelWithParentSteps wizardModel) {
        super(id, model);
        this.wizardModel = wizardModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        setOutputMarkupId(true);
        exception = ProcessedExceptionWrapper.from(
                getResult(), getOpResult().getExceptionMessage(), getModelObject().getFixPanelId());

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = createContainer();
        add(container);

        container.add(createIcon());
        container.add(createTitle());
        container.add(createExpandCollapseButton());

        WebMarkupContainer details = createDetails();
        container.add(details);

        details.add(createScriptRow());
        details.add(createMapRows());
        details.add(createStackTraceLink());
        details.add(createText());
        details.add(createIgnoreButton());
        details.add(createFixButton());
    }

    private @NotNull WebMarkupContainer createContainer() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.add(AttributeAppender.append("class", this::getAccentCssClass));
        return container;
    }

    private String getAccentCssClass() {
        return switch (getResultType()) {
            case ERROR -> "border-danger";
            case WARNING -> "border-warning";
            case UNKNOWN -> "border-info";
        };
    }

    private @NotNull WebMarkupContainer createIcon() {
        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getResultType().getIcon()));
        return icon;
    }

    private @NotNull Label createTitle() {
        IModel<String> titleModel = createStringResource("ProcessedExceptionPanel.title");
        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.append("title", titleModel));
        return title;
    }

    private @NotNull AjaxIconButton createExpandCollapseButton() {
        AjaxIconButton expandButton = new AjaxIconButton(ID_EXPAND_COLLAPSE_BUTTON,
                () -> getModelObject().isExpanded() ? "fa fa-chevron-down" : "fa fa-chevron-right",
                () -> getModelObject().isExpanded()
                        ? getString("ProcessedExceptionPanel.collapse")
                        : getString("ProcessedExceptionPanel.expand")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResultWrapper wrapper = ProcessedExceptionPanel.this.getModelObject();
                wrapper.setExpanded(!wrapper.isExpanded());
                target.add(ProcessedExceptionPanel.this);
            }
        };
        expandButton.setOutputMarkupId(true);
        return expandButton;
    }

    private @NotNull WebMarkupContainer createDetails() {
        WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
        details.setOutputMarkupId(true);
        details.add(new VisibleBehaviour(() -> getModelObject().isExpanded()));
        return details;
    }

    private @NotNull WebMarkupContainer createScriptRow() {
        WebMarkupContainer row = new WebMarkupContainer(ID_SCRIPT_ROW);
        row.add(new Label(ID_SCRIPT_VALUE, () -> exception.getScript()));
        row.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(exception.getScript())));
        return row;
    }

    private @NotNull ListView<Map.Entry<Key, String>> createMapRows() {
        return new ListView<>(ID_MAP_ROW, () -> List.copyOf(exception.getMap().entrySet())) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Map.Entry<Key, String>> item) {
                Map.Entry<Key, String> entry = item.getModelObject();
                item.add(new Label(ID_MAP_LABEL,
                        createStringResource(KEY_MAP_PREFIX + entry.getKey().name())));
                item.add(new Label(ID_MAP_VALUE, entry.getValue()));
            }
        };
    }

    private @NotNull AjaxLink<Void> createStackTraceLink() {
        return new AjaxLink<>(ID_STACK_TRACE_LINK) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResultPopupPanel body = new OperationResultPopupPanel(
                        getPageBase().getMainPopupBodyId(),
                        new Model<>(exception.getOriginalException()));
                body.setOutputMarkupId(true);
                getPageBase().showMainPopup(body, target);
            }
        };
    }

    private @NotNull Label createText() {
        return new Label(ID_TEXT, createStringResource("ProcessedExceptionPanel.text"));
    }

    private @NotNull AjaxButton createIgnoreButton() {
        AjaxButton ignoreButton = new AjaxButton(ID_IGNORE_BUTTON,
                createStringResource("ProcessedExceptionPanel.ignoreButton")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                // Ignoring a problem is not decided yet, see MID-11804.
            }
        };
        ignoreButton.setOutputMarkupId(true);
        return ignoreButton;
    }

    private @NotNull AjaxButton createFixButton() {
        OperationResultWrapper wrapper = getModelObject();

        AjaxButton fixButton = new AjaxButton(ID_FIX_BUTTON,
                wrapper.getFixButtonLabelKey() != null
                        ? createStringResource(wrapper.getFixButtonLabelKey())
                        : createStringResource("OperationResultCollapsedItemPanel.fixButton")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (wrapper.getFixAction() != null) {
                    wrapper.getFixAction().accept(target);
                    target.add(ProcessedExceptionPanel.this);
                    return;
                }
                wizardModel.setActiveStepById(wrapper.getFixPanelId());
                wizardModel.fireActiveStepChanged();
                target.add(wizardModel.getPanel());
            }
        };
        fixButton.setOutputMarkupId(true);
        return fixButton;
    }

    private OperationResult getResult() {
        return getModelObject().getResult();
    }

    private OpResult getOpResult() {
        return OpResult.getOpResult(getPageBase(), getResult());
    }

    private ResultType getResultType() {
        OperationResult result = getResult();
        if (result.isError()) {
            return ResultType.ERROR;
        }
        if (result.isWarning()) {
            return ResultType.WARNING;
        }
        return ResultType.UNKNOWN;
    }
}

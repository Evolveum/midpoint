/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author katkav
 */
public class OperationResultPanel extends BasePanel<OpResult> implements Popupable {
    private static final long serialVersionUID = 1L;

    private static final String ID_DETAILS_BOX = "detailsBox";
    private static final String ID_ICON_TYPE = "iconType";
    private static final String ID_MESSAGE = "message";
    private static final String ID_MESSAGE_LABEL = "messageLabel";
    private static final String ID_PARAMS = "params";
    private static final String ID_BACKGROUND_TASK_LINK = "backgroundTaskLink";
    private static final String ID_BACKGROUND_TASK_EXISTS = "backgroundTaskExists";
    private static final String ID_CASE = "case";
    private static final String ID_SHOW_ALL = "showAll";
    private static final String ID_HIDE_ALL = "hideAll";
    private static final String ID_ERROR_STACK_TRACE = "errorStackTrace";

    static final String OPERATION_RESOURCE_KEY_PREFIX = "operation.";

    private static final Trace LOGGER = TraceManager.getTrace(OperationResultPanel.class);

    public OperationResultPanel(String id, IModel<OpResult> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void initLayout() {

        WebMarkupContainer detailsBox = new WebMarkupContainer(ID_DETAILS_BOX);
        detailsBox.setOutputMarkupId(true);
        detailsBox.add(AttributeModifier.append("class", createHeaderCss()));
        add(detailsBox);

        initHeader(detailsBox);
        initDetails(detailsBox);
    }

    private void initHeader(WebMarkupContainer box) {
        WebMarkupContainer iconType = new WebMarkupContainer(ID_ICON_TYPE);
        iconType.add(AttributeAppender.append("class", new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                OpResult message = getModelObject();

                switch (message.getStatus()) {
                    case IN_PROGRESS:
                    case NOT_APPLICABLE:
                        sb.append(" fa-info");
                        break;
                    case SUCCESS:
                        sb.append(" fa-check");
                        break;
                    case FATAL_ERROR:
                        sb.append(" fa-ban");
                        break;
                    case PARTIAL_ERROR:
                    case UNKNOWN:
                    case WARNING:
                    case HANDLED_ERROR:
                    default:
                        sb.append(" fa-exclamation-triangle");
                }

                return sb.toString();
            }
        }));

        WebMarkupContainer message = createMessage();
        message.add(iconType);

        box.add(message);

        AjaxLink<String> backgroundTaskLink = new AjaxLink<>(ID_BACKGROUND_TASK_LINK) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                final OpResult opResult = OperationResultPanel.this.getModelObject();
                String oid = opResult.getBackgroundTaskOid();
                if (oid == null || !opResult.isBackgroundTaskVisible()) {
                    return; // just for safety
                }
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(oid, ObjectTypes.TASK);
                DetailsPageUtil.dispatchToObjectDetailsPage(ref, getPageBase(), false);
            }
        };
        backgroundTaskLink.add(new VisibleBehaviour(
                () -> getModelObject().getBackgroundTaskOid() != null
                        && getModelObject().isBackgroundTaskVisible()));
        message.add(backgroundTaskLink);

        Label backgroundTaskExists = new Label(
                ID_BACKGROUND_TASK_EXISTS,
                createStringResource("OperationResultPanel.taskExists"));
        backgroundTaskExists.add(new VisibleBehaviour(
                () -> getModelObject().getBackgroundTaskOid() != null
                        && !getModelObject().isBackgroundTaskVisible()));
        message.add(backgroundTaskExists);

        AjaxLink<String> aCase = new AjaxLink<>(ID_CASE) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                final OpResult opResult = OperationResultPanel.this.getModelObject();
                String oid = opResult.getCaseOid();
                if (oid == null || !opResult.isCaseVisible()) {
                    return; // just for safety
                }
                ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(oid, ObjectTypes.CASE);
                DetailsPageUtil.dispatchToObjectDetailsPage(ref, getPageBase(), false);
            }
        };
        aCase.add(new VisibleBehaviour(() -> getModelObject().getCaseOid() != null && getModelObject().isCaseVisible()));
        message.add(aCase);

        AjaxLink<String> showAll = new AjaxLink<>(ID_SHOW_ALL) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showHideAll(true, target);
            }
        };
        showAll.add(new VisibleBehaviour(() -> !OperationResultPanel.this.getModelObject().isShowMore()));
        box.add(showAll);

        AjaxLink<String> hideAll = new AjaxLink<>(ID_HIDE_ALL) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                showHideAll(false, target);
            }
        };
        hideAll.add(new VisibleBehaviour(() -> OperationResultPanel.this.getModelObject().isShowMore()));
        box.add(hideAll);

        AjaxLink<String> close = new AjaxLink<>("close") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                close(target, true);
            }
        };
        box.add(close);

        DownloadLink downloadXml = new DownloadLink("downloadXml", new IModel<>() {
            private static final long serialVersionUID = 1L;

            @Override
            public File getObject() {
                String home = getMidpointConfiguration().getMidpointHome();
                File f = new File(home, "result");
                try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(f))) {
                    dos.writeBytes(OperationResultPanel.this.getModel().getObject().getXml());
                } catch (IOException e) {
                    LOGGER.error("Could not download result: {}", e.getMessage(), e);
                }
                return f;
            }

        });
        downloadXml.add(new VisibleBehaviour(() -> getModelObject().isParent()));
        downloadXml.setDeleteAfterDownload(true);
        box.add(downloadXml);
    }

    public void close(AjaxRequestTarget target, boolean parent) {
        if (parent) {
            if (this.getParent() != null) {
                target.add(this.getParent().setVisible(false));
            }
        } else {
            this.setVisible(false);
            target.add(this);
        }
    }

    private WebMarkupContainer createMessage() {
        Label messageLabel = new Label(ID_MESSAGE_LABEL, (IModel<String>) () -> {
            OpResult result = OperationResultPanel.this.getModel().getObject();

            PageAdminLTE page = WebComponentUtil.getPage(OperationResultPanel.this, PageAdminLTE.class);

            String msg = null;
            if (result.getUserFriendlyMessage() != null) {
                //TODO: unify with WebModelServiceUtil.translateMessage()
                LocalizationService service = page.getLocalizationService();
                Locale locale = page.getSession().getLocale();

                msg = service.translate(result.getUserFriendlyMessage(), locale);
            }

            if (StringUtils.isNotBlank(msg)) {
                return msg;
            }

            msg = result.getMessage();
            if (StringUtils.isNotBlank(msg)) {
                return msg;
            }

            String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
            return page.getString(resourceKey, null, result.getOperation());
        });
        messageLabel.setRenderBodyOnly(true);

        WebMarkupContainer message = new WebMarkupContainer(ID_MESSAGE);
        message.add(messageLabel);
        message.add(new AjaxEventBehavior("click") {

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                OpResult result = OperationResultPanel.this.getModelObject();
                result.setShowMore(!result.isShowMore());
                result.setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
                target.add(OperationResultPanel.this);
            }
        });

        return message;
    }

    private void initDetails(WebMarkupContainer box) {
        final WebMarkupContainer details = new WebMarkupContainer("details", getModel());
        details.setOutputMarkupId(true);
        details.add(new VisibleBehaviour(() -> getModelObject().isShowMore()));
        box.add(details);

        DetailsPanel operation = new DetailsPanel("operation", createStringResource("FeedbackAlertMessageDetails.operation"));
        details.add(operation);
        Label operationBody = new Label("operationBody", () -> {
            OpResult result = getModelObject();

            String resourceKey = OPERATION_RESOURCE_KEY_PREFIX + result.getOperation();
            return getString(resourceKey, null, result.getOperation());
        });
        operation.add(operationBody);

        DetailsPanel message = new DetailsPanel("message", createStringResource("FeedbackAlertMessageDetails.message"));
        message.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(getModelObject().getMessage())));
        details.add(message);
        Label resultMessage = new Label("resultMessage", new PropertyModel<String>(getModel(), "message").getObject());
        resultMessage.setRenderBodyOnly(true);
        message.add(resultMessage);

        initParams(details);
        initContexts(details);

        DetailsPanel countContainer = new DetailsPanel("countContainer", createStringResource("FeedbackAlertMessageDetails.count"));
        countContainer.add(new VisibleBehaviour(() -> getModelObject().getCount() > 1));
        details.add(countContainer);

        Label count = new Label("count", () -> getModelObject().getCount());
        count.add(new VisibleBehaviour(() -> getModelObject().getCount() > 1));
        countContainer.add(count);

        initError(details);

        ListView<OpResult> subresults = new ListView<>("subresults", createSubresultsModel(getModel())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(final ListItem<OpResult> item) {
                Panel subresult = new OperationResultPanel("subresult", item.getModel());
                subresult.add(new VisibleBehaviour(() -> item.getModel() != null && item.getModelObject() != null));
                subresult.setOutputMarkupId(true);
                item.add(subresult);
            }
        };
        subresults.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(getModelObject().getSubresults())));
        details.add(subresults);
    }

    private void initParams(WebMarkupContainer details) {
        DetailsPanel paramsContainer = new DetailsPanel("paramsContainer", createStringResource("FeedbackAlertMessageDetails.params"));
        paramsContainer.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(getModelObject().getParams())));
        details.add(paramsContainer);

        ListView<Param> params = new ListView<>(ID_PARAMS, createParamsModel(getModel())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Param> item) {
                item.add(new Label("paramName", new PropertyModel<>(item.getModel(), "name")));
                item.add(new Label("paramValue", new PropertyModel<>(item.getModel(), "value")));
            }
        };
        paramsContainer.add(params);
    }

    private void initContexts(WebMarkupContainer details) {
        DetailsPanel contextsContainer = new DetailsPanel("contextsContainer", createStringResource("FeedbackAlertMessageDetails.contexts"));
        contextsContainer.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(getModelObject().getContexts())));
        details.add(contextsContainer);

        ListView<Context> contexts = new ListView<>("contexts", createContextsModel(getModel())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Context> item) {
                item.add(new Label("contextName", new PropertyModel<>(item.getModel(), "name")));
                item.add(new Label("contextValue", new PropertyModel<>(item.getModel(), "value")));
            }
        };
        contextsContainer.add(contexts);
    }

    private void initError(WebMarkupContainer details) {
        DetailsPanel errorContainer = new DetailsPanel("errorContainer", createStringResource("FeedbackAlertMessageDetails.error"));
        errorContainer.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(getModelObject().getExceptionsStackTrace())));
        details.add(errorContainer);

        Label errorMessage = new Label("errorMessage", () -> getModelObject().getExceptionMessage());
        errorContainer.add(errorMessage);

        Label errorStackTrace = new Label(ID_ERROR_STACK_TRACE, () -> getModelObject().getExceptionsStackTrace());
        errorStackTrace.add(new VisibleBehaviour(() -> getModelObject().isShowError()));
        errorContainer.add(errorStackTrace);

        Label linkText = new Label("linkText", () -> {
            String key = getModelObject().isShowError() ? "operationResultPanel.hideStack" : "operationResultPanel.showStack";
            return getString(key);
        });

        AjaxLink<Void> errorStackTraceLink = new AjaxLink<>("errorStackTraceLink") {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                OpResult result = OperationResultPanel.this.getModelObject();
                result.setShowError(!result.isShowError());
                result.setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
                target.add(OperationResultPanel.this);
            }
        };
        errorStackTraceLink.add(linkText);
        errorContainer.add(errorStackTraceLink);
    }

    private void showHideAll(final boolean show, AjaxRequestTarget target) {
        getModelObject().setShowMoreAll(show);
        getModelObject().setAlreadyShown(false);  // hack to be able to expand/collapse OpResult after rendered.
        target.add(OperationResultPanel.this);
    }

    private IModel<String> createHeaderCss() {
        return () -> {
            OpResult result = getModelObject();

            if (result == null || result.getStatus() == null) {
                return "card-warning";
            }

            switch (result.getStatus()) {
                case IN_PROGRESS:
                case NOT_APPLICABLE:
                    return "card-info";
                case SUCCESS:
                    return "card-success";
                case HANDLED_ERROR:
                    return "card-secondary";
                case FATAL_ERROR:
                    return "card-danger";
                case UNKNOWN:
                case PARTIAL_ERROR:
                case WARNING:
                default:
                    return "card-warning";
            }
        };
    }

    static IModel<List<Param>> createParamsModel(final IModel<OpResult> model) {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<Param> load() {
                OpResult result = model.getObject();
                return result.getParams();
            }
        };
    }

    static IModel<List<Context>> createContextsModel(final IModel<OpResult> model) {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<Context> load() {
                OpResult result = model.getObject();
                return result.getContexts();
            }
        };
    }

    private IModel<List<OpResult>> createSubresultsModel(final IModel<OpResult> model) {
        return new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<OpResult> load() {
                OpResult result = model.getObject();
                List<OpResult> subresults = result.getSubresults();
                if (subresults == null) {
                    subresults = new ArrayList<>();
                }

                return subresults;
            }
        };
    }

    @Override
    public int getWidth() {
        return 900;
    }

    @Override
    public int getHeight() {
        return 500;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("OperationResultPanel.result");
    }
}

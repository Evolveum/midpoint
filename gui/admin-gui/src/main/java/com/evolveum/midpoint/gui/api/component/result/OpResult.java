/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.result;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.prism.PrismContext;

import org.apache.commons.lang3.Validate;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author lazyman
 */
public class OpResult implements Serializable, Visitable {

    private static final Trace LOGGER = TraceManager.getTrace(OpResult.class);

    private static final String OPERATION_CHECK_TASK_VISIBILITY = OpResult.class.getName() + ".checkTaskVisibility";
    private static final String OPERATION_CHECK_CASE_VISIBILITY = OpResult.class.getName() + ".checkCaseVisibility";

    private OperationResultStatus status;
    private String operation;
    private String message;
    private List<Param> params;
    private List<Context> contexts;
    private String exceptionMessage;
    private String exceptionsStackTrace;
    private List<OpResult> subresults;
    private OpResult parent;
    private int count;
    private IModel<String> xml;
    private LocalizableMessage userFriendlyMessage;

    // we assume there is at most one background task created (TODO revisit this assumption)
    private String backgroundTaskOid;
    private Boolean backgroundTaskVisible; // available on root opResult only

    private String caseOid;
    private Boolean caseVisible;

    private boolean showMore;
    private boolean showError;

    private boolean alreadyShown;

    public LocalizableMessage getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    public boolean isAlreadyShown() {
        return alreadyShown;
    }

    public void setAlreadyShown(boolean alreadyShown) {
        this.alreadyShown = alreadyShown;
    }

    public static OpResult getOpResult(PageAdminLTE page, OperationResult result) {
        OpResult opResult = new OpResult();
        Validate.notNull(result, "Operation result must not be null.");
        Validate.notNull(result.getStatus(), "Operation result status must not be null.");

        if (result.getCause() != null && result.getCause() instanceof CommonException) {
            LocalizableMessage localizableMessage = ((CommonException) result.getCause()).getUserFriendlyMessage();
            if (localizableMessage != null) {
                opResult.message = WebComponentUtil.resolveLocalizableMessage(localizableMessage, page);

                // Exclamation code:
//                String key = localizableMessage.getKey() != null ? localizableMessage.getKey() : localizableMessage.getFallbackMessage();
//                StringResourceModel stringResourceModel = new StringResourceModel(key, page).setModel(new Model<String>()).setDefaultValue(localizableMessage.getFallbackMessage())
//                .setParameters(localizableMessage.getArgs());
//                opResult.message = stringResourceModel.getString();
            }
        }

        if (opResult.message == null) {
            opResult.message = result.getMessage();
        }
        opResult.operation = result.getOperation();
        opResult.status = result.getStatus();
        opResult.count = result.getCount();
        opResult.userFriendlyMessage = result.getUserFriendlyMessage();

        if (result.getCause() != null) {
            Throwable cause = result.getCause();
            opResult.exceptionMessage = cause.getMessage();

            Writer writer = new StringWriter();
            cause.printStackTrace(new PrintWriter(writer));
            opResult.exceptionsStackTrace = writer.toString();
        }

        for (Map.Entry<String, Collection<String>> entry : result.getParams().entrySet()) {
            String paramValue = null;
            Collection<String> values = entry.getValue();
            if (values != null) {
                paramValue = values.toString();
            }
            opResult.getParams().add(new Param(entry.getKey(), paramValue));
        }

        for (Map.Entry<String, Collection<String>> entry : result.getContext().entrySet()) {
            String contextValue = null;
            Collection<String> values = entry.getValue();
            if (values != null) {
                contextValue = values.toString();
            }
            opResult.getContexts().add(new Context(entry.getKey(), contextValue));
        }

        for (OperationResult subresult : result.getSubresults()) {
            OpResult subOpResult = OpResult.getOpResult(page, subresult);
            opResult.getSubresults().add(subOpResult);
            subOpResult.parent = opResult;
        }

        String asynchronousOperationReference = result.findAsynchronousOperationReference();
        opResult.backgroundTaskOid = OperationResult.referenceToTaskOid(asynchronousOperationReference);
        opResult.caseOid = OperationResult.referenceToCaseOid(asynchronousOperationReference);

        if (opResult.parent == null) {
            opResult.xml = createXmlModel(result, page);
        }
        return opResult;
    }

    private static LoadableModel<String> createXmlModel(OperationResult result, PageAdminLTE page) {
        return new LoadableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                try {
                    OperationResultType resultType = result.createOperationResultType();
                    try {
                        return PrismContext.get().xmlSerializer().serializeAnyData(resultType, SchemaConstants.C_RESULT);
                    } catch (SchemaException e) {
                        throw new TunnelException(e);
                    }
                } catch (RuntimeException ex) {
                    String m = "Can't create xml: " + ex;
                    return "<?xml version='1.0'?><message>" + StringEscapeUtils.escapeXml10(m) + "</message>";
                }
            }
        };
    }

    // This method should be called along with getOpResult for root operationResult. However, it might take some time,
    // and there might be situations in which it is not required -- so we opted for calling it explicitly.
    public void determineObjectsVisibility(PageAdminLTE pageBase, Options options) {
        determineBackgroundTaskVisibility(pageBase, options.hideTaskLinks());
        determineCaseVisibility(pageBase);
    }

    private void determineBackgroundTaskVisibility(PageAdminLTE pageBase, boolean explicitlyHidden) {
        if (backgroundTaskOid == null) {
            return;
        }
        if (explicitlyHidden) {
            backgroundTaskVisible = false;
            return;
        }
        try {
            if (pageBase.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL)) {
                backgroundTaskVisible = true;
                return;
            }
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            backgroundTaskVisible = false;
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine background task visibility", e);
            return;
        }

        Task task = pageBase.createSimpleTask(OPERATION_CHECK_TASK_VISIBILITY);
        try {
            pageBase.getModelService().getObject(TaskType.class, backgroundTaskOid, null, task, task.getResult());
            backgroundTaskVisible = true;
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.debug("Task {} is not visible by the current user: {}: {}", backgroundTaskOid, e.getClass(), e.getMessage());
            backgroundTaskVisible = false;
        }
    }

    private void determineCaseVisibility(PageAdminLTE pageBase) {
        if (getStatus().equals(OperationResultStatus.FATAL_ERROR)) {
            caseVisible = false;
            return;
        }
        if (caseOid == null) {
            return;
        }
        try {
            if (pageBase.isAuthorized(AuthorizationConstants.AUTZ_ALL_URL)) {
                caseVisible = true;
                return;
            }
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                ConfigurationException | SecurityViolationException e) {
            caseVisible = false;
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine case visibility", e);
            return;
        }

        Task task = pageBase.createSimpleTask(OPERATION_CHECK_CASE_VISIBILITY);
        try {
            pageBase.getModelService().getObject(CaseType.class, caseOid, null, task, task.getResult());
            caseVisible = true;
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException e) {
            LOGGER.debug("Case {} is not visible by the current user: {}: {}", caseOid, e.getClass(), e.getMessage());
            caseVisible = false;
        }
    }

    public boolean isShowMore() {
        return showMore;
    }

    public void setShowMore(boolean showMore) {
        this.showMore = showMore;
    }

    public boolean isShowError() {
        return showError;
    }

    public void setShowError(boolean showError) {
        this.showError = showError;
    }

    public List<OpResult> getSubresults() {
        if (subresults == null) {
            subresults = new ArrayList<>();
        }
        return subresults;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public String getExceptionsStackTrace() {
        return exceptionsStackTrace;
    }

    public String getMessage() {
        return message;
    }

    public String getOperation() {
        return operation;
    }

    public List<Param> getParams() {
        if (params == null) {
            params = new ArrayList<>();
        }
        return params;
    }

    public List<Context> getContexts() {
        if (contexts == null) {
            contexts = new ArrayList<>();
        }
        return contexts;
    }

    public OperationResultStatus getStatus() {
        return status;
    }

    public int getCount() {
        return count;
    }

    public boolean isParent() {
        return parent == null;
    }

    public String getXml() {
        return xml.getObject();
    }

    public String getBackgroundTaskOid() {
        return backgroundTaskOid;
    }

    boolean isBackgroundTaskVisible() {
        if (backgroundTaskVisible != null) {
            return backgroundTaskVisible;
        }
        if (parent != null) {
            return parent.isBackgroundTaskVisible();
        }
        return true; // at least as for now
    }

    public String getCaseOid() {
        return caseOid;
    }

    public boolean isCaseVisible() {
        if (caseVisible != null) {
            return caseVisible;
        }
        if (parent != null) {
            return parent.isCaseVisible();
        }
        return true; // at least as for now
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);

        for (OpResult result : this.getSubresults()) {
            result.accept(visitor);
        }
    }

    public void setShowMoreAll(final boolean show) {
        Visitor visitor = visitable -> {
            if (!(visitable instanceof OpResult)) {
                return;
            }

            OpResult result = (OpResult) visitable;
            result.setShowMore(show);

        };

        accept(visitor);
    }

    /** Options for showing {@link OpResult} objects. */
    public record Options (boolean hideSuccess,
                           boolean hideInProgress,
                           boolean hideTaskLinks) implements Serializable {

        public static Options create() {
            return new Options(false, false, false);
        }

        /** `SUCCESS` messages are hidden. */
        public Options withHideSuccess(boolean value) {
            return new Options(value, hideInProgress, hideTaskLinks);
        }

        /** `IN_PROGRESS` messages are hidden. */
        public Options withHideInProgress(boolean value) {
            return new Options(hideSuccess, value, hideTaskLinks);
        }

        /** Information about background tasks is not clickable. TODO better name? */
        public Options withHideTaskLinks(boolean value) {
            return new Options(hideSuccess, hideInProgress, value);
        }
    }
}

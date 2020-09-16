/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.util.*;

import com.evolveum.midpoint.util.logging.*;
import com.evolveum.midpoint.util.statistics.OperationInvocationRecord;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ParamsTypeUtil;
import com.evolveum.midpoint.util.exception.CommonException;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultImportanceType.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Nested Operation Result.
 *
 * This class provides information for better error handling in complex
 * operations. It contains a status (success, failure, warning, ...) and an
 * error message. It also contains a set of sub-results - results on inner
 * operations.
 *
 * This object can be used by GUI to display smart (and interactive) error
 * information. It can also be used by the client code to detect deeper problems
 * in the invocations, retry or otherwise compensate for the errors or decide
 * how severe the error was and it is possible to proceed.
 *
 * @author lazyman
 * @author Radovan Semancik
 *
 */
public class OperationResult implements Serializable, DebugDumpable, ShortDumpable, Cloneable, OperationResultBuilder, Visitable<OperationResult> {

    private static final long serialVersionUID = -2467406395542291044L;
    private static final String VARIOUS_VALUES = "[various values]";
    private static final String INDENT_STRING = "    ";

    private static final String TASK_OID_PREFIX = "taskOid:";
    private static final String CASE_OID_PREFIX = "caseOid:";

    /**
     * This constant provides count threshold for same subresults (same operation and
     * status) during summarize operation.
     */
    private static final int DEFAULT_SUBRESULT_STRIP_THRESHOLD = 10;

    @NotNull private static final OperationResultHandlingStrategyType DEFAULT_HANDLING_STRATEGY = new OperationResultHandlingStrategyType();
    @NotNull private volatile static List<OperationResultHandlingStrategyType> handlingStrategies = emptyList();
    @NotNull private static OperationResultHandlingStrategyType globalHandlingStrategy = DEFAULT_HANDLING_STRATEGY;
    private static final ThreadLocal<OperationResultHandlingStrategyType> LOCAL_HANDLING_STRATEGY =
            new ThreadLocal<>();

    private static final AtomicInteger LOG_SEQUENCE_COUNTER = new AtomicInteger(0);

    public static final String CONTEXT_IMPLEMENTATION_CLASS = "implementationClass";
    public static final String CONTEXT_PROGRESS = "progress";
    public static final String CONTEXT_OID = "oid";
    public static final String CONTEXT_OBJECT = "object";
    public static final String CONTEXT_ITEM = "item";
    public static final String CONTEXT_TASK = "task";
    public static final String CONTEXT_RESOURCE = "resource";
    public static final String CONTEXT_REASON = "reason";

    public static final String PARAM_OID = "oid";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_OPTIONS = "options";
    public static final String PARAM_TASK = "task";
    public static final String PARAM_OBJECT = "object";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_PROJECTION = "projection";
    public static final String PARAM_LANGUAGE = "language";

    public static final String RETURN_COUNT = "count";
    public static final String RETURN_BACKGROUND_TASK_OID = "backgroundTaskOid";
    public static final String RETURN_COMMENT = "comment";
    public static final String DEFAULT = "";

    private static long tokenCount = 1000000000000000000L;
    private String operation;
    private OperationResultStatus status;

    // Values of the following maps should NOT be null. But in reality it does happen.
    // If there is a null value, it should be stored as a single-item collection, where the item is null.
    // But the collection should not be null. TODO; fix this
    private Map<String, Collection<String>> params;
    private Map<String, Collection<String>> context;
    private Map<String, Collection<String>> returns;
    private final List<String> qualifiers = new ArrayList<>();

    private long token;
    private String messageCode;
    private String message;
    private LocalizableMessage userFriendlyMessage;
    private Throwable cause;
    private int count = 1;
    private int hiddenRecordsCount;
    private List<OperationResult> subresults;
    private List<String> details;
    private boolean summarizeErrors;
    private boolean summarizePartialErrors;
    private boolean summarizeSuccesses;
    private OperationResultImportanceType importance = NORMAL;

    private boolean building;        // experimental (NOT SERIALIZED)
    private OperationResult futureParent;   // experimental (NOT SERIALIZED)

    private Long start;
    private Long end;
    private Long microseconds;
    private Long invocationId;

    private final List<LogSegmentType> logSegments = new ArrayList<>();

    private CompiledTracingProfile tracingProfile;      // NOT SERIALIZED
    private boolean collectingLogEntries;               // NOT SERIALIZED
    private boolean startedLoggingOverride;             // NOT SERIALIZED

    /**
     * After a trace rooted at this operation result is stored, the dictionary that was extracted is stored here.
     * It is necessary to correctly interpret traces in this result and its subresults.
     */
    private TraceDictionaryType extractedDictionary;    // NOT SERIALIZED

    private final List<TraceType> traces = new ArrayList<>();

    // Caller can specify the reason of the operation invocation.
    // (Normally, the reason is known from the parent opResult; but it might be an overkill to create operation result
    // only to specify the reason.)
    //
    // Use of this attribute assumes that the callee will ALWAYS create OperationResult as its first step.
    // TODO reconsider ... it looks like a really ugly hack
    private transient String callerReason;

    /**
     * Reference to an asynchronous operation that can be used to retrieve
     * the status of the running operation. This may be a task identifier,
     * identifier of a ticket in ITSM system or anything else. The exact
     * format of this reference depends on the operation which is being
     * executed.
     */
    private String asynchronousOperationReference;

    private OperationInvocationRecord invocationRecord;

    private static final Trace LOGGER = TraceManager.getTrace(OperationResult.class);

    public OperationResult(String operation) {
        this(operation, null, OperationResultStatus.UNKNOWN, 0, null, null, null, null, null);
    }

    public OperationResult(String operation, OperationResultStatus status, LocalizableMessage userFriendlyMessage) {
        this(operation, null, status, 0, null, null, userFriendlyMessage, null, null);
    }

    public OperationResult(String operation, OperationResultStatus status, String message) {
        this(operation, null, status, 0, null, message, null, null, null);
    }

    public OperationResult(String operation, Map<String, Collection<String>> params, OperationResultStatus status,
            long token, String messageCode, String message, LocalizableMessage userFriendlyMessage, Throwable cause, List<OperationResult> subresults) {
        this(operation, params, null, null, status, token, messageCode, message, userFriendlyMessage, cause,
                subresults);
    }

    public OperationResult(String operation, Map<String, Collection<String>> params, Map<String, Collection<String>> context,
                           Map<String, Collection<String>> returns, OperationResultStatus status, long token, String messageCode,
                           String message, LocalizableMessage userFriendlyMessage, Throwable cause, List<OperationResult> subresults) {
        if (StringUtils.isEmpty(operation)) {
            throw new IllegalArgumentException("Operation argument must not be null or empty.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Operation status must not be null.");
        }
        this.operation = operation;
        this.params = params;
        this.context = context;
        this.returns = returns;
        this.status = status;
        this.token = token;
        this.messageCode = messageCode;
        this.message = message;
        this.userFriendlyMessage = userFriendlyMessage;
        this.cause = cause;
        this.subresults = subresults;
        this.details = new ArrayList<>();
    }

    public static OperationResult keepRootOnly(OperationResult result) {
        return result != null ? result.keepRootOnly() : null;
    }

    private OperationResult keepRootOnly() {
        return new OperationResult(getOperation(), null, getStatus(), 0, getMessageCode(), getMessage(), null, null, null);
    }

    public OperationResultBuilder subresult(String operation) {
        OperationResult subresult = new OperationResult(operation);
        subresult.building = true;
        subresult.futureParent = this;
        subresult.tracingProfile = tracingProfile;
        return subresult;
    }

    public static OperationResultBuilder createFor(String operation) {
        OperationResult rv = new OperationResult(operation);
        rv.building = true;
        return rv;
    }

    @Override
    public OperationResult build() {
        if (!building) {
            throw new IllegalStateException("Not being built");
        }
        recordStart(this, operation, createArguments());
        building = false;
        if (futureParent != null) {
            futureParent.addSubresult(this);
            recordCallerReason(futureParent);
        }
        return this;
    }

    private void recordCallerReason(OperationResult parent) {
        if (parent.getCallerReason() != null) {
            addContext(CONTEXT_REASON, parent.getCallerReason());
            parent.setCallerReason(null);
        }
    }

    private static void recordStart(OperationResult result, String operation, Object[] arguments) {
        result.collectingLogEntries = result.tracingProfile != null && result.tracingProfile.isCollectingLogEntries();
        if (result.collectingLogEntries) {
            LoggingLevelOverrideConfiguration loggingOverrideConfiguration = result.tracingProfile.getLoggingLevelOverrideConfiguration();
            if (loggingOverrideConfiguration != null && !LevelOverrideTurboFilter.isActive()) {
                LevelOverrideTurboFilter.overrideLogging(loggingOverrideConfiguration);
                result.startedLoggingOverride = true;
            }
            TracingAppender.openSink(result::appendLoggedEvents);
        }
        // TODO for very minor operation results (e.g. those dealing with mapping and script execution)
        //  we should consider skipping creation of invocationRecord. It includes some string manipulation(s)
        //  and a call to System.nanoTime that could unnecessarily slow down midPoint operation.
        result.invocationRecord = OperationInvocationRecord.create(operation, arguments);
        result.invocationId = result.invocationRecord.getInvocationId();
        result.start = System.currentTimeMillis();
    }

    private void appendLoggedEvents(LoggingEventSink loggingEventSink) {
        if (!loggingEventSink.getEvents().isEmpty()) {
            LogSegmentType segment = new LogSegmentType();
            segment.setSequenceNumber(LOG_SEQUENCE_COUNTER.getAndIncrement());
            for (LoggedEvent event : loggingEventSink.getEvents()) {
                segment.getEntry().add(event.getText());
            }
            logSegments.add(segment);
            loggingEventSink.clearEvents();
        }
    }

    private Object[] createArguments() {
        List<String> arguments = new ArrayList<>();
        getParams().forEach((key, value) -> arguments.add(key + " => " + value));       // todo what with large values?
        getContext().forEach((key, value) -> arguments.add("c:" + key + " => " + value));
        return arguments.toArray();
    }

    public OperationResult createSubresult(String operation) {
        return createSubresult(operation, false, new Object[0]);
    }

    public OperationResult createMinorSubresult(String operation) {
        return createSubresult(operation, true, null);
    }

//    public static OperationResult createProfiled(String operation) {
//        return createProfiled(operation, new Object[0]);
//    }
//
//    public static OperationResult createProfiled(String operation, Object[] arguments) {
//        OperationResult result = new OperationResult(operation);
//        recordStart(result, operation, arguments);
//        return result;
//    }

    private OperationResult createSubresult(String operation, boolean minor, Object[] arguments) {
        OperationResult subresult = new OperationResult(operation);
        subresult.recordCallerReason(this);
        addSubresult(subresult);
        recordStart(subresult, operation, arguments);
        subresult.importance = minor ? MINOR : NORMAL;
        return subresult;
    }

    // todo determine appropriate places where recordEnd() should be called
    public void recordEnd() {
        if (invocationRecord != null) {
            // This is not quite clean. We should report the exception via processException method - but that does not allow
            // showing return values that can be present in operation result. So this is a hack until InvocationRecord is fixed.
            invocationRecord.processReturnValue(getReturns(), cause);
            invocationRecord.afterCall();
            microseconds = invocationRecord.getElapsedTimeMicros();
            if (collectingLogEntries) {
                TracingAppender.closeCurrentSink();
                collectingLogEntries = false;
            }
            invocationRecord = null;
        }
        end = System.currentTimeMillis();
        if (startedLoggingOverride) {
            LevelOverrideTurboFilter.cancelLoggingOverride();
            startedLoggingOverride = false;
        }
    }

    // This is not quite useful: We want to record the "real" end, i.e. when the control leaves the region belonging to
    // the operation result. So it's actually OK to rewrite the end timestamp, even if it was already set.
    // A small price to pay is that "end" could get a bit inconsistent with "microseconds", that was presumably set earlier.
    // But that's quite good - when analyzing we can see that such an inconsistency arose and we can deal with it.

//    public void recordEndIfNeeded() {
//        if (invocationRecord != null || end == null) {
//            recordEnd();
//        }
//    }

    /**
     * Reference to an asynchronous operation that can be used to retrieve
     * the status of the running operation. This may be a task identifier,
     * identifier of a ticket in ITSM system or anything else. The exact
     * format of this reference depends on the operation which is being
     * executed.
     */
    public String getAsynchronousOperationReference() {
        return asynchronousOperationReference;
    }

    public void setAsynchronousOperationReference(String asynchronousOperationReference) {
        this.asynchronousOperationReference = asynchronousOperationReference;
    }

    /**
     * This method partially duplicates functionality of computeStatus. However, computeStatus
     * currently does not propagate taskOid from tasks switched to background, because switchToBackground
     * sets its result to SUCCESS (not IN_PROGRESS) because of some historical reasons. So,
     * until this is fixed somehow, this is a bit of hack to fetch asynchronous operation reference
     * even in such cases.
     */
    public String findAsynchronousOperationReference() {
        if (asynchronousOperationReference != null) {
            return asynchronousOperationReference;
        }
        for (OperationResult subresult : emptyIfNull(subresults)) {
            String s = subresult.findAsynchronousOperationReference();
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    public static boolean isTaskOid(String ref) {
        return ref != null && ref.startsWith(TASK_OID_PREFIX);
    }

    public static String referenceToTaskOid(String ref) {
        return isTaskOid(ref) ? ref.substring(TASK_OID_PREFIX.length()) : null;
    }

    public static String referenceToCaseOid(String ref) {
        return ref != null && ref.startsWith(CASE_OID_PREFIX) ? ref.substring(CASE_OID_PREFIX.length()) : null;
    }

    /**
     * Contains operation name. Operation name must be defined as {@link String}
     * constant in module interface with description and possible parameters. It
     * can be used for further processing. It will be used as key for
     * translation in admin-gui.
     *
     * @return always return non null, non empty string
     */
    public String getOperation() {
        return operation;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incrementCount() {
        this.count++;
    }

    public int getHiddenRecordsCount() {
        return hiddenRecordsCount;
    }

    public void setHiddenRecordsCount(int hiddenRecordsCount) {
        this.hiddenRecordsCount = hiddenRecordsCount;
    }

    public boolean representsHiddenRecords() {
        return this.hiddenRecordsCount > 0;
    }

    public boolean isSummarizeErrors() {
        return summarizeErrors;
    }

    public void setSummarizeErrors(boolean summarizeErrors) {
        this.summarizeErrors = summarizeErrors;
    }

    public boolean isSummarizePartialErrors() {
        return summarizePartialErrors;
    }

    public void setSummarizePartialErrors(boolean summarizePartialErrors) {
        this.summarizePartialErrors = summarizePartialErrors;
    }

    public boolean isSummarizeSuccesses() {
        return summarizeSuccesses;
    }

    public void setSummarizeSuccesses(boolean summarizeSuccesses) {
        this.summarizeSuccesses = summarizeSuccesses;
    }

    public boolean isEmpty() {
        return (status == null || status == OperationResultStatus.UNKNOWN) &&
                (subresults == null || subresults.isEmpty());
    }


    /**
     * Method returns list of operation subresults @{link
     * {@link OperationResult}.
     *
     * @return never returns null
     */
    @NotNull
    public List<OperationResult> getSubresults() {
        if (subresults == null) {
            subresults = new ArrayList<>();
        }
        return subresults;
    }

    /**
     * @return last subresult, or null if there are no subresults.
     */
    public OperationResult getLastSubresult() {
        if (subresults == null || subresults.isEmpty()) {
            return null;
        } else {
            return subresults.get(subresults.size()-1);
        }
    }

    public void removeLastSubresult() {
        if (subresults != null && !subresults.isEmpty()) {
            subresults.remove(subresults.size()-1);
        }
    }

    /**
     * @return last subresult status, or null if there are no subresults.
     */
    public OperationResultStatus getLastSubresultStatus() {
        OperationResult last = getLastSubresult();
        return last != null ? last.getStatus() : null;
    }

    public void addSubresult(OperationResult subresult) {
        getSubresults().add(subresult);
        if (subresult.tracingProfile == null) {
            subresult.tracingProfile = tracingProfile;
        }
    }

    public OperationResult findSubresult(String operation) {
        if (subresults == null) {
            return null;
        }
        for (OperationResult subResult: getSubresults()) {
            if (operation.equals(subResult.getOperation())) {
                return subResult;
            }
        }
        return null;
    }

    public List<OperationResult> findSubresults(String operation) {
        List<OperationResult> found = new ArrayList<>();
        if (subresults == null) {
            return found;
        }
        for (OperationResult subResult: getSubresults()) {
            if (operation.equals(subResult.getOperation())) {
                found.add(subResult);
            }
        }
        return found;
    }

    /**
     * Contains operation status as defined in {@link OperationResultStatus}
     *
     * @return never returns null
     */
    public OperationResultStatus getStatus() {
        return status;
    }

    public void setStatus(OperationResultStatus status) {
        this.status = status;
    }

    /**
     * Returns true if the result is success.
     *
     * This returns true if the result is absolute success. Presence of partial
     * failures or warnings fail this test.
     *
     * @return true if the result is success.
     */
    public boolean isSuccess() {
        return (status == OperationResultStatus.SUCCESS);
    }

    public boolean isWarning() {
        return status == OperationResultStatus.WARNING;
    }

    /**
     * Returns true if the result is acceptable for further processing.
     *
     * In other words: if there were no fatal errors. Warnings and partial
     * errors are acceptable. Yet, this test also fails if the operation state
     * is not known.
     *
     * @return true if the result is acceptable for further processing.
     */
    public boolean isAcceptable() {
        return (status != OperationResultStatus.FATAL_ERROR);
    }

    public boolean isUnknown() {
        return (status == OperationResultStatus.UNKNOWN);
    }

    public boolean isInProgress() {
        return (status == OperationResultStatus.IN_PROGRESS);
    }

    public boolean isError() {
        return (status == OperationResultStatus.FATAL_ERROR) ||
                    (status == OperationResultStatus.PARTIAL_ERROR);
    }

    public boolean isFatalError() {
        return (status == OperationResultStatus.FATAL_ERROR);
    }

    public boolean isPartialError() {
        return (status == OperationResultStatus.PARTIAL_ERROR);
    }

    public boolean isHandledError() {
        return (status == OperationResultStatus.HANDLED_ERROR);
    }

    public boolean isNotApplicable() {
        return (status == OperationResultStatus.NOT_APPLICABLE);
    }

    /**
     * Set all error status in this result and all subresults as handled.
     */
    public void setErrorsHandled() {
        if (isError()) {
            setStatus(OperationResultStatus.HANDLED_ERROR);
        }
        for(OperationResult subresult: getSubresults()) {
            subresult.setErrorsHandled();
        }
    }


    /**
     * Computes operation result status based on subtask status and sets an
     * error message if the status is FATAL_ERROR.
     *
     * @param errorMessage
     *            error message
     */
    public void computeStatus(String errorMessage) {
        computeStatus(errorMessage, errorMessage);
    }

    public void computeStatus(String errorMessage, String warnMessage) {
        Validate.notEmpty(errorMessage, "Error message must not be null.");

        // computeStatus sets a message if none is set,
        // therefore we need to check before calling computeStatus
        boolean noMessage = StringUtils.isEmpty(message);
        computeStatus();

        switch (status) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                if (noMessage) {
                    message = errorMessage;
                }
                break;
            case UNKNOWN:
            case WARNING:
            case NOT_APPLICABLE:
                if (noMessage) {
                    if (StringUtils.isNotEmpty(warnMessage)) {
                        message = warnMessage;
                    } else {
                        message = errorMessage;
                    }
                }
                break;
        }
    }

    /**
     * Computes operation result status based on subtask status.
     */
    public void computeStatus() {
        computeStatus(false);
    }

    public void computeStatus(boolean skipFinish) {
        if (!skipFinish) {
            recordEnd();
        }
        if (getSubresults().isEmpty()) {
            if (status == OperationResultStatus.UNKNOWN) {
                status = OperationResultStatus.SUCCESS;
            }
            return;
        }
        if (status == OperationResultStatus.FATAL_ERROR) {
            return;
        }
        OperationResultStatus newStatus = OperationResultStatus.UNKNOWN;
        boolean allSuccess = true;
        boolean allNotApplicable = true;
        String newMessage = null;
        LocalizableMessage newUserFriendlyMessage = null;
        for (OperationResult sub : getSubresults()) {
            if (sub == null) {
                continue;
            }

            if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
                allNotApplicable = false;
            }
            if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
                status = OperationResultStatus.FATAL_ERROR;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ": " + sub.getMessage();
                }
                updateLocalizableMessage(sub);
                return;
            }
            if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
                status = OperationResultStatus.IN_PROGRESS;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ": " + sub.getMessage();
                }
                updateLocalizableMessage(sub);
                if (asynchronousOperationReference == null) {
                    asynchronousOperationReference = sub.getAsynchronousOperationReference();
                }
                return;
            }
            if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
                newStatus = OperationResultStatus.PARTIAL_ERROR;
                newMessage = sub.getMessage();
                newUserFriendlyMessage = sub.getUserFriendlyMessage();
            }
            if (newStatus != OperationResultStatus.PARTIAL_ERROR){
            if (sub.getStatus() == OperationResultStatus.HANDLED_ERROR) {
                newStatus = OperationResultStatus.HANDLED_ERROR;
                newMessage = sub.getMessage();
                newUserFriendlyMessage = sub.getUserFriendlyMessage();
            }
            }
            if (sub.getStatus() != OperationResultStatus.SUCCESS
                    && sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
                allSuccess = false;
            }
            if (newStatus != OperationResultStatus.HANDLED_ERROR) {
                if (sub.getStatus() == OperationResultStatus.WARNING) {
                    newStatus = OperationResultStatus.WARNING;
                    newMessage = sub.getMessage();
                    newUserFriendlyMessage = sub.getUserFriendlyMessage();
                }
            }
        }

        if (allNotApplicable && !getSubresults().isEmpty()) {
            status = OperationResultStatus.NOT_APPLICABLE;
        } else if (allSuccess && !getSubresults().isEmpty()) {
            status = OperationResultStatus.SUCCESS;
        } else {
            status = newStatus;
            if (message == null) {
                message = newMessage;
            } else {
                message = message + ": " + newMessage;
            }
            updateLocalizableMessage(newUserFriendlyMessage);
        }
    }

    private void updateLocalizableMessage(OperationResult subResult) {
        if (userFriendlyMessage == null) {
            userFriendlyMessage = subResult.getUserFriendlyMessage();
        } else {
            updateLocalizableMessage(subResult.getUserFriendlyMessage());
        }
    }

    private void updateLocalizableMessage(LocalizableMessage localizableMessage) {
        if (localizableMessage == null) {
            return;
        }
        if (userFriendlyMessage instanceof SingleLocalizableMessage) {
            userFriendlyMessage = new LocalizableMessageListBuilder()
                    .message(userFriendlyMessage)
                    .message(localizableMessage)
                    .separator(LocalizableMessageList.SPACE).build();
        } else if (userFriendlyMessage instanceof LocalizableMessageList) {
            LocalizableMessageList userFriendlyMessageList = (LocalizableMessageList) this.userFriendlyMessage;
            userFriendlyMessage = new LocalizableMessageList(
                    mergeMessages(userFriendlyMessageList, localizableMessage),
                    userFriendlyMessageList.getSeparator(),
                    userFriendlyMessageList.getPrefix(),
                    userFriendlyMessageList.getPostfix());
        }
    }

    private List<LocalizableMessage> mergeMessages(LocalizableMessageList sum, LocalizableMessage increment) {
        List<LocalizableMessage> rv = new ArrayList<>(sum.getMessages());
        if (increment instanceof SingleLocalizableMessage) {
            rv.add(increment);
        } else if (increment instanceof LocalizableMessageList) {
            rv.addAll(((LocalizableMessageList) increment).getMessages());
        } else {
            throw new IllegalArgumentException("increment: " + increment);
        }
        return rv;
    }

    /**
     * Used when the result contains several composite sub-result that are of equivalent meaning.
     * If all of them fail the result will be fatal error as well. If only some of them fail the
     * result will be partial error. Handled error is considered a success.
     */
    public void computeStatusComposite() {
        recordEnd();
        if (getSubresults().isEmpty()) {
            if (status == OperationResultStatus.UNKNOWN) {
                status = OperationResultStatus.NOT_APPLICABLE;
            }
            return;
        }

        boolean allFatalError = true;
        boolean allNotApplicable = true;
        boolean hasInProgress = false;
        boolean hasHandledError = false;
        boolean hasError = false;
        boolean hasWarning = false;
        for (OperationResult sub : getSubresults()) {
            if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
                allNotApplicable = false;
            }
            if (sub.getStatus() != OperationResultStatus.FATAL_ERROR) {
                allFatalError = false;
            }
            if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
                hasError = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
            if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
                hasError = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
            if (sub.getStatus() == OperationResultStatus.HANDLED_ERROR) {
                hasHandledError = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
            if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
                hasInProgress = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
                if (asynchronousOperationReference == null) {
                    asynchronousOperationReference = sub.getAsynchronousOperationReference();
                }
            }
            if (sub.getStatus() == OperationResultStatus.WARNING) {
                hasWarning = true;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ", " + sub.getMessage();
                }
            }
        }

        if (allNotApplicable) {
            status = OperationResultStatus.NOT_APPLICABLE;
        } else if (allFatalError) {
            status = OperationResultStatus.FATAL_ERROR;
        } else if (hasInProgress) {
            status = OperationResultStatus.IN_PROGRESS;
        } else if (hasError) {
            status = OperationResultStatus.PARTIAL_ERROR;
        } else if (hasWarning) {
            status = OperationResultStatus.WARNING;
        } else if (hasHandledError) {
            status = OperationResultStatus.HANDLED_ERROR;
        } else {
            status = OperationResultStatus.SUCCESS;
        }
    }

    public void addTrace(TraceType trace) {
        traces.add(trace);
    }

    @Override
    public OperationResultBuilder tracingProfile(CompiledTracingProfile profile) {
        this.tracingProfile = profile;
        return this;
    }

    public <T> T getFirstTrace(Class<T> traceClass) {
        Optional<TraceType> first = traces.stream().filter(t -> traceClass.isAssignableFrom(t.getClass())).findFirst();
        if (first.isPresent()) {
            //noinspection unchecked
            return (T) first.get();
        } else {
            for (OperationResult subresult : getSubresults()) {
                T firstInSubresult = subresult.getFirstTrace(traceClass);
                if (firstInSubresult != null) {
                    return firstInSubresult;
                }
            }
            return null;
        }
    }

    public void addReturnComment(String comment) {
        addReturn(RETURN_COMMENT, comment);
    }

    public boolean isTracingNormal(Class<? extends TraceType> traceClass) {
        return isTracing(traceClass, TracingLevelType.NORMAL);
    }

    public boolean isTracingMinimal(Class<? extends TraceType> traceClass) {
        return isTracing(traceClass, TracingLevelType.MINIMAL);
    }

    public boolean isTracingDetailed(Class<? extends TraceType> traceClass) {
        return isTracing(traceClass, TracingLevelType.DETAILED);
    }

    public boolean isTracing(Class<? extends TraceType> traceClass, TracingLevelType level) {
        return getTracingLevel(traceClass).ordinal() >= level.ordinal();
    }

    @NotNull
    public TracingLevelType getTracingLevel(Class<? extends TraceType> traceClass) {
        if (tracingProfile != null) {
            return tracingProfile.getLevel(traceClass);
        } else {
            return TracingLevelType.OFF;
        }
    }

    public void clearTracingProfile() {
        tracingProfile = null;
    }

    @Override
    public void accept(Visitor<OperationResult> visitor) {
        visitor.visit(this);
        if (subresults != null) {
            for (OperationResult subresult : subresults) {
                subresult.accept(visitor);
            }
        }
    }

    public Stream<OperationResult> getResultStream() {
        return Stream.concat(Stream.of(this),
                getSubresults().stream()
                        .map(subresult -> subresult.getResultStream())
                        .flatMap(stream -> stream));
    }

    public static final class PreviewResult {
        public final OperationResultStatus status;
        public final String message;

        private PreviewResult(OperationResultStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }

    // This is quite ugly. We should compute the preview in a way that does not modify existing object.
    public PreviewResult computePreview() {
        OperationResultStatus origStatus = status;
        String origMessage = message;
        computeStatus(true);
        PreviewResult rv = new PreviewResult(status, message);
        status = origStatus;
        message = origMessage;
        return rv;
    }

    public OperationResultStatus getComputeStatus() {
        return computePreview().status;
    }

    public void computeStatusIfUnknown() {
        recordEnd();
        if (isUnknown()) {
            computeStatus();
        }
    }

    public void recomputeStatus() {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original
        // status
        if (subresults != null && !subresults.isEmpty()) {
            computeStatus();
        }
    }

    public void recomputeStatus(String message) {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original
        // status
        if (subresults != null && !subresults.isEmpty()) {
            computeStatus(message);
        }
    }

    public void recomputeStatus(String errorMessage, String warningMessage) {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original
        // status
        if (subresults != null && !subresults.isEmpty()) {
            computeStatus(errorMessage, warningMessage);
        }
    }

    public void recordSuccessIfUnknown() {
        recordEnd();
        if (isUnknown()) {
            recordSuccess();
        }
    }

    public void recordNotApplicableIfUnknown() {
        recordEnd();
        if (isUnknown()) {
            status = OperationResultStatus.NOT_APPLICABLE;
        }
    }

    public void recordNotApplicable() {
        recordStatus(OperationResultStatus.NOT_APPLICABLE, (String) null);
    }

    public boolean isMinor() {
        return importance == MINOR;
    }

    /**
     * Method returns {@link Map} with operation parameters. Parameters keys are
     * described in module interface for every operation.
     */
    @NotNull
    public Map<String, Collection<String>> getParams() {
        if (params == null) {
            params = new HashMap<>();
        }
        return params;
    }

    public Collection<String> getParam(String name) {
        return getParams().get(name);
    }

    public String getParamSingle(String name) {
        Collection<String> values = getParams().get(name);
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException("More than one parameter "+name+" in "+this);
        }
        return values.iterator().next();
    }

    @Override
    public OperationResult addQualifier(String value) {
        qualifiers.add(value);
        return this;
    }

    @Override
    public OperationResult addParam(String name, String value) {
        getParams().put(name, collectionize(value));
        return this;
    }

    @Override
    public OperationResult addParam(String name, PrismObject<? extends ObjectType> value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectType value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, boolean value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, long value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, int value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult addParam(String name, Class<?> value) {
        if (value != null && ObjectType.class.isAssignableFrom(value)) {
            getParams().put(name, collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>)value).getObjectTypeUri()));
        } else {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, QName value) {
        getParams().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, PolyString value) {
        getParams().put(name, collectionize(value == null ? null : value.getOrig()));
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectQuery value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectDelta<?> value) {
        getParams().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addParam(String name, String... values) {
        getParams().put(name, collectionize(values));
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectAsParam(String paramName, Object paramValue) {
        getParams().put(paramName, collectionize(stringify(paramValue)));
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectCollectionAsParam(String name, Collection<?> value) {
        getParams().put(name, stringifyCol(value));
        return this;
    }

    public Map<String, Collection<String>> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }
        return context;
    }

    @Override
    public OperationResult addContext(String name, String value) {
        getContext().put(name, collectionize(value));
        return this;
    }

    @Override
    public OperationResult addContext(String name, PrismObject<? extends ObjectType> value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectType value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, boolean value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, long value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, int value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult addContext(String name, Class<?> value) {
        if (value != null && ObjectType.class.isAssignableFrom(value)) {
            getContext().put(name, collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>)value).getObjectTypeUri()));
        } else {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, QName value) {
        getContext().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, PolyString value) {
        getContext().put(name, collectionize(value == null ? null : value.getOrig()));
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectQuery value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectDelta<?> value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addContext(String name, String... values) {
        getContext().put(name, collectionize(values));
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectAsContext(String name, Object value) {
        getContext().put(name, collectionize(stringify(value)));
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectCollectionAsContext(String paramName, Collection<?> paramValue) {
        getContext().put(paramName, stringifyCol(paramValue));
        return this;
    }

    @NotNull
    public Map<String, Collection<String>> getReturns() {
        if (returns == null) {
            returns = new HashMap<>();
        }
        return returns;
    }

    public Collection<String> getReturn(String name) {
        return getReturns().get(name);
    }

    public String getReturnSingle(String name) {
        Collection<String> values = getReturns().get(name);
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException("More than one return "+name+" in "+this);
        }
        return values.iterator().next();
    }

    public void addReturn(String name, String value) {
        getReturns().put(name, collectionize(value));
    }

    public void addReturn(String name, PrismObject<? extends ObjectType> value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addReturn(String name, ObjectType value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addReturn(String name, boolean value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addReturn(String name, long value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addReturn(String name, int value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    @SuppressWarnings("unchecked")
    public void addReturn(String name, Class<?> value) {
        if (value != null && ObjectType.class.isAssignableFrom(value)) {
            getReturns().put(name, collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>)value).getObjectTypeUri()));
        } else {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, QName value) {
        getReturns().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
    }

    public void addReturn(String name, PolyString value) {
        getReturns().put(name, collectionize(value == null ? null : value.getOrig()));
    }

    public void addReturn(String name, ObjectQuery value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addReturn(String name, ObjectDelta<?> value) {
        getReturns().put(name, collectionize(stringify(value)));
    }


    public void addReturn(String name, String... values) {
        getReturns().put(name, collectionize(values));
    }

    public void addArbitraryObjectAsReturn(String name, Object value) {
        getReturns().put(name, collectionize(stringify(value)));
    }

    public void addArbitraryObjectCollectionAsReturn(String paramName, Collection<?> paramValue) {
        getReturns().put(paramName, stringifyCol(paramValue));
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        } else {
            return value.toString();
        }
    }

    private Collection<String> collectionize(String value) {
        Collection<String> out = new ArrayList<>(1);
        out.add(value);
        return out;
    }

    private Collection<String> collectionize(String... values) {
        return Arrays.asList(values);
    }

    private Collection<String> stringifyCol(Collection<?> values) {
        if (values == null) {
            return null;
        }
        Collection<String> out = new ArrayList<>(values.size());
        for (Object value: values) {
            if (value == null) {
                out.add(null);
            } else {
                out.add(value.toString());
            }
        }
        return out;
    }


    /**
     * @return Contains random long number, for better searching in logs.
     */
    public long getToken() {
        if (token == 0) {
            token = tokenCount++;
        }
        return token;
    }

    /**
     * Contains message code based on module error catalog.
     *
     * @return Can return null.
     */
    public String getMessageCode() {
        return messageCode;
    }

    /**
     * @return Method returns operation result message. Message is required. It
     *         will be key for translation in admin-gui.
     */
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalizableMessage getUserFriendlyMessage() {
        return userFriendlyMessage;
    }

    public void setUserFriendlyMessage(LocalizableMessage userFriendlyMessage) {
        this.userFriendlyMessage = userFriendlyMessage;
    }

    /**
     * @return Method returns operation result exception. Not required, can be
     *         null.
     */
    public Throwable getCause() {
        return cause;
    }

    public void recordSuccess() {
        recordEnd();
        // Success, no message or other explanation is needed.
        status = OperationResultStatus.SUCCESS;
    }

    public void recordInProgress() {
        status = OperationResultStatus.IN_PROGRESS;
    }

    public void recordUnknown() {
        status = OperationResultStatus.UNKNOWN;
    }

    public void recordFatalError(Throwable cause) {
        recordStatus(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
    }

    public void recordFatalErrorNotFinish(Throwable cause) {
        recordStatusNotFinish(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
    }

    /**
     * If the operation is an error then it will switch the status to EXPECTED_ERROR.
     * This is used if the error is expected and properly handled.
     */
    public void muteError() {
        if (isError()) {
            status = OperationResultStatus.HANDLED_ERROR;
        }
    }

    public void muteLastSubresultError() {
        OperationResult lastSubresult = getLastSubresult();
        if (lastSubresult != null) {
            lastSubresult.muteError();
        }
    }

    public void deleteLastSubresultIfError() {
        OperationResult lastSubresult = getLastSubresult();
        if (lastSubresult != null && lastSubresult.isError()) {
            removeLastSubresult();
        }
    }

    public void recordPartialError(Throwable cause) {
        recordStatus(OperationResultStatus.PARTIAL_ERROR, cause.getMessage(), cause);
    }

    public void recordWarning(Throwable cause) {
        recordStatus(OperationResultStatus.WARNING, cause.getMessage(), cause);
    }

    public void recordStatus(OperationResultStatus status, Throwable cause) {
        recordEnd();
        this.status = status;
        this.cause = cause;
        // No other message was given, so use message from the exception
        // not really correct, but better than nothing.
        message = cause.getMessage();
    }

    public void recordFatalError(String message, Throwable cause) {
        recordStatus(OperationResultStatus.FATAL_ERROR, message, cause);
    }

    public void recordPartialError(String message, Throwable cause) {
        recordStatus(OperationResultStatus.PARTIAL_ERROR, message, cause);
    }

    public void recordWarning(String message, Throwable cause) {
        recordStatus(OperationResultStatus.WARNING, message, cause);
    }

    public void recordHandledError(String message) {
        recordStatus(OperationResultStatus.HANDLED_ERROR, message);
    }

    public void recordHandledError(String message, Throwable cause) {
        recordStatus(OperationResultStatus.HANDLED_ERROR, message, cause);
    }

    public void recordHandledError(Throwable cause) {
        recordStatus(OperationResultStatus.HANDLED_ERROR, cause.getMessage(), cause);
    }

    public void recordStatus(OperationResultStatus status, String message, Throwable cause) {
        recordEnd();
        recordStatusNotFinish(status, message, cause);
    }

    public void recordStatusNotFinish(OperationResultStatus status, String message, Throwable cause) {
        this.status = status;
        this.message = message;
        this.cause = cause;
    }

    public void recordFatalError(String message) {
        recordStatus(OperationResultStatus.FATAL_ERROR, message);
    }

    public void recordPartialError(String message) {
        recordStatus(OperationResultStatus.PARTIAL_ERROR, message);
    }

    public void recordWarning(String message) {
        recordStatus(OperationResultStatus.WARNING, message);
    }

    /**
     * Records result from a common exception type. This automatically
     * determines status and also sets appropriate message.
     *
     * @param exception
     *            common exception
     */
    public void record(CommonException exception) {
        // TODO: switch to a localized message later
        // Exception is a fatal error in this context
        recordFatalError(exception.getErrorTypeMessage(), exception);
    }

    public void recordStatus(OperationResultStatus status, String message) {
        recordEnd();
        this.status = status;
        this.message = message;
    }

    /**
     * Returns true if result status is UNKNOWN or any of the subresult status
     * is unknown (recursive).
     *
     * May come handy in tests to check if all the operations fill out the
     * status as they should.
     */
    public boolean hasUnknownStatus() {
        if (status == OperationResultStatus.UNKNOWN) {
            return true;
        }
        for (OperationResult subresult : getSubresults()) {
            if (subresult.hasUnknownStatus()) {
                return true;
            }
        }
        return false;
    }

    public void appendDetail(String detailLine) {
        // May be switched to a more structured method later
        details.add(detailLine);
    }

    public List<String> getDetail() {
        return details;
    }

    @Contract("null -> null; !null -> !null")
    public static OperationResult createOperationResult(OperationResultType result) {
        if (result == null) {
            return null;
        }

        Map<String, Collection<String>> params = ParamsTypeUtil.fromParamsType(result.getParams());
        Map<String, Collection<String>> context = ParamsTypeUtil.fromParamsType(result.getContext());
        Map<String, Collection<String>> returns = ParamsTypeUtil.fromParamsType(result.getReturns());

        List<OperationResult> subresults = null;
        if (!result.getPartialResults().isEmpty()) {
            subresults = new ArrayList<>();
            for (OperationResultType subResult : result.getPartialResults()) {
                subresults.add(createOperationResult(subResult));
            }
        }

        LocalizableMessage localizableMessage = null;
        LocalizableMessageType message = result.getUserFriendlyMessage();
        if (message != null) {
            localizableMessage = LocalizationUtil.toLocalizableMessage(message);
        }

        OperationResult opResult = new OperationResult(result.getOperation(), params, context, returns,
                OperationResultStatus.parseStatusType(result.getStatus()), result.getToken(),
                result.getMessageCode(), result.getMessage(), localizableMessage,  null,
                subresults);
        opResult.getQualifiers().addAll(result.getQualifier());
        opResult.setImportance(result.getImportance());
        opResult.setAsynchronousOperationReference(result.getAsynchronousOperationReference());
        if (result.getCount() != null) {
            opResult.setCount(result.getCount());
        }
        if (result.getHiddenRecordsCount() != null) {
            opResult.setHiddenRecordsCount(result.getHiddenRecordsCount());
        }
        if (result.getStart() != null) {
            opResult.setStart(XmlTypeConverter.toMillis(result.getStart()));
        }
        if (result.getEnd() != null) {
            opResult.setEnd(XmlTypeConverter.toMillis(result.getEnd()));
        }
        opResult.setMicroseconds(result.getMicroseconds());
        opResult.setInvocationId(result.getInvocationId());
        opResult.logSegments.addAll(result.getLog());
        return opResult;
    }

    public OperationResultType createOperationResultType() {
        return createOperationResultType(null);
    }

    public OperationResultType createOperationResultType(Function<LocalizableMessage, String> resolveKeys) {
        return createOperationResultType(this, resolveKeys);
    }

    private static OperationResultType createOperationResultType(OperationResult opResult, Function<LocalizableMessage, String> resolveKeys) {
        OperationResultType resultType = new OperationResultType();
        resultType.setToken(opResult.getToken());
        resultType.setStatus(OperationResultStatus.createStatusType(opResult.getStatus()));
        resultType.setImportance(opResult.getImportance());
        if (opResult.getCount() != 1) {
            resultType.setCount(opResult.getCount());
        }
        if (opResult.getHiddenRecordsCount() != 0) {
            resultType.setHiddenRecordsCount(opResult.getHiddenRecordsCount());
        }
        resultType.setOperation(opResult.getOperation());
        resultType.getQualifier().addAll(opResult.getQualifiers());
        resultType.setMessage(opResult.getMessage());
        resultType.setMessageCode(opResult.getMessageCode());

        if (opResult.getCause() != null || !opResult.details.isEmpty()) {
            StringBuilder detailsb = new StringBuilder();

            // Record text messages in details (if present)
            if (!opResult.details.isEmpty()) {
                for (String line : opResult.details) {
                    detailsb.append(line);
                    detailsb.append("\n");
                }
            }

            // Record stack trace in details if a cause is present
            if (opResult.getCause() != null) {
                Throwable ex = opResult.getCause();
                detailsb.append(ex.getClass().getName());
                detailsb.append(": ");
                detailsb.append(ex.getMessage());
                detailsb.append("\n");
                StackTraceElement[] stackTrace = ex.getStackTrace();
                for (StackTraceElement aStackTrace : stackTrace) {
                    detailsb.append(aStackTrace.toString());
                    detailsb.append("\n");
                }
            }

            resultType.setDetails(detailsb.toString());
        }

        if (opResult.getUserFriendlyMessage() != null) {
            LocalizableMessageType msg = LocalizationUtil.createLocalizableMessageType(opResult.getUserFriendlyMessage(), resolveKeys);
            resultType.setUserFriendlyMessage(msg);
        }

        resultType.setParams(ParamsTypeUtil.toParamsType(opResult.getParams()));
        resultType.setContext(ParamsTypeUtil.toParamsType(opResult.getContext()));
        resultType.setReturns(ParamsTypeUtil.toParamsType(opResult.getReturns()));

        for (OperationResult subResult : opResult.getSubresults()) {
            resultType.getPartialResults().add(createOperationResultType(subResult, resolveKeys));
        }

        resultType.setAsynchronousOperationReference(opResult.getAsynchronousOperationReference());

        resultType.setStart(XmlTypeConverter.createXMLGregorianCalendar(opResult.start));
        resultType.setEnd(XmlTypeConverter.createXMLGregorianCalendar(opResult.end));
        resultType.setMicroseconds(opResult.microseconds);
        resultType.setInvocationId(opResult.invocationId);
        resultType.getLog().addAll(opResult.logSegments);           // consider cloning here
        resultType.getTrace().addAll(opResult.traces);           // consider cloning here
        return resultType;
    }

    public void summarize() {
        summarize(false);
    }

    public void summarize(boolean alsoSubresults) {

        if (isTraced()) {
            return;
        }

        int subresultStripThreshold = getSubresultStripThreshold();

        // first phase: summarizing records if explicitly requested
        Iterator<OperationResult> iterator = getSubresults().iterator();
        while (iterator.hasNext()) {
            OperationResult subresult = iterator.next();
            if (subresult.getCount() > 1) {
                // Already summarized
                continue;
            }
            if (subresult.isError() && summarizeErrors) {
                // go on
            } else if (subresult.isPartialError() && summarizePartialErrors) {
                // go on
            } else if (subresult.isSuccess() && summarizeSuccesses) {
                // go on
            } else {
                continue;
            }
            OperationResult similar = findSimilarSubresult(subresult);
            if (similar == null) {
                // Nothing to summarize to
                continue;
            }
            merge(similar, subresult);
            iterator.remove();
        }

        // second phase: summarizing (better said, eliminating or hiding) subresults if there are too many of them
        // (we strip subresults that have same operation name and status, if there are more of them than given threshold)
        //
        // We implement quite a complex algorithm to ensure "incremental stripping", i.e. calling summarize() repeatedly
        // on an OperationResult to which new standard entries are continually added. The requirement is that there must
        // be at most one summarization record, and it must be placed after all standard records of given type.
        Map<OperationStatusKey, OperationStatusCounter> recordsCounters = new HashMap<>();
        iterator = getSubresults().iterator();
        while (iterator.hasNext()) {
            OperationResult sr = iterator.next();
            OperationStatusKey key = new OperationStatusKey(sr.getOperation(), sr.getStatus());
            if (recordsCounters.containsKey(key)) {
                OperationStatusCounter counter = recordsCounters.get(key);
                if (!sr.representsHiddenRecords()) {
                    if (counter.shownRecords < subresultStripThreshold) {
                        counter.shownRecords++;
                        counter.shownCount += sr.count;
                    } else {
                        counter.hiddenCount += sr.count;
                        iterator.remove();
                    }
                } else {
                    counter.hiddenCount += sr.hiddenRecordsCount;
                    iterator.remove();        // will be re-added at the end (potentially with records counters)
                }
            } else {
                OperationStatusCounter counter = new OperationStatusCounter();
                if (!sr.representsHiddenRecords()) {
                    counter.shownRecords = 1;
                    counter.shownCount = sr.count;
                } else {
                    counter.hiddenCount = sr.hiddenRecordsCount;
                    iterator.remove();        // will be re-added at the end (potentially with records counters)
                }
                recordsCounters.put(key, counter);
            }
        }
        for (Map.Entry<OperationStatusKey, OperationStatusCounter> repeatingEntry : recordsCounters.entrySet()) {
            int shownCount = repeatingEntry.getValue().shownCount;
            int hiddenCount = repeatingEntry.getValue().hiddenCount;
            if (hiddenCount > 0) {
                OperationStatusKey key = repeatingEntry.getKey();
                OperationResult hiddenRecordsEntry = new OperationResult(key.operation, key.status,
                        hiddenCount + " record(s) were hidden to save space. Total number of records: " + (shownCount + hiddenCount));
                hiddenRecordsEntry.setHiddenRecordsCount(hiddenCount);
                addSubresult(hiddenRecordsEntry);
            }
        }

        // And now, summarize each of the subresults
        if (alsoSubresults) {
            iterator = getSubresults().iterator();
            while (iterator.hasNext()) {
                iterator.next().summarize(true);
            }
        }
    }

    private void merge(OperationResult target, OperationResult source) {
        mergeMap(target.getParams(), source.getParams());
        mergeMap(target.getContext(), source.getContext());
        mergeMap(target.getReturns(), source.getReturns());
        target.incrementCount();
    }

    private void mergeMap(Map<String, Collection<String>> targetMap, Map<String, Collection<String>> sourceMap) {
        for (Entry<String, Collection<String>> targetEntry: targetMap.entrySet()) {
            String targetKey = targetEntry.getKey();
            Collection<String> targetValues = targetEntry.getValue();
            if (targetValues != null && targetValues.contains(VARIOUS_VALUES)) {
                continue;
            }
            Collection<String> sourceValues = sourceMap.get(targetKey);
            if (MiscUtil.equals(targetValues, sourceValues)) {
                // Entries match, nothing to do
                continue;
            }
            // Entries do not match. The target entry needs to be marked as VariousValues
            targetEntry.setValue(createVariousValues());
        }
        for (Entry<String, Collection<String>> sourceEntry: sourceMap.entrySet()) {
            String sourceKey = sourceEntry.getKey();
            if (!targetMap.containsKey(sourceKey)) {
                targetMap.put(sourceKey, createVariousValues());
            }
        }
    }

    private Collection<String> createVariousValues() {
        List<String> out = new ArrayList<>(1);
        out.add(VARIOUS_VALUES);
        return out;
    }

    private OperationResult findSimilarSubresult(OperationResult subresult) {
        OperationResult similar = null;
        for (OperationResult sub: getSubresults()) {
            if (sub == subresult) {
                continue;
            }
            if (!sub.operation.equals(subresult.operation)) {
                continue;
            }
            if (sub.status != subresult.status) {
                continue;
            }
            if (!MiscUtil.equals(sub.message, subresult.message)) {
                continue;
            }
            if (similar == null || (similar.count < sub.count)) {
                similar = sub;
            }
        }
        return similar;
    }

    // experimental/temporary
    public void cleanupResultDeeply() {
        cleanupResult();
        emptyIfNull(subresults).forEach(OperationResult::cleanupResultDeeply);
    }

    /**
     * Removes all the successful minor results. Also checks if the result is roughly consistent
     * and complete. (e.g. does not have unknown operation status, etc.)
     */
    public void cleanupResult() {
        cleanupResult(null);
    }

    /**
     * Removes all the successful minor results. Also checks if the result is roughly consistent
     * and complete. (e.g. does not have unknown operation status, etc.)
     *
     * The argument "e" is for easier use of the cleanup in the exceptions handlers. The original exception is passed
     * to the IAE that this method produces for easier debugging.
     */
    public void cleanupResult(Throwable e) {

        if (isTraced()) {
            return;         // TEMPORARY fixme
        }

        OperationResultImportanceType preserveDuringCleanup = getPreserveDuringCleanup();

        if (status == OperationResultStatus.UNKNOWN) {
            LOGGER.error("Attempt to cleanup result of operation " + operation + " that is still UNKNOWN:\n{}", this.debugDump());
            throw new IllegalStateException("Attempt to cleanup result of operation "+operation+" that is still UNKNOWN");
        }
        if (subresults == null) {
            return;
        }
        Iterator<OperationResult> iterator = subresults.iterator();
        while (iterator.hasNext()) {
            OperationResult subresult = iterator.next();
            if (subresult.getStatus() == OperationResultStatus.UNKNOWN) {
                String message = "Subresult "+subresult.getOperation()+" of operation "+operation+" is still UNKNOWN during cleanup";
                LOGGER.error("{}:\n{}", message, this.debugDump(), e);
                if (e == null) {
                    throw new IllegalStateException(message);
                } else {
                    throw new IllegalStateException(message+"; during handling of exception "+e, e);
                }
            }
            if (subresult.canCleanup(preserveDuringCleanup)) {
                iterator.remove();
            }
        }
    }

    private boolean canCleanup(OperationResultImportanceType preserveDuringCleanup) {
        return isLesserThan(importance, preserveDuringCleanup) && (status == OperationResultStatus.SUCCESS || status == OperationResultStatus.NOT_APPLICABLE);
    }

    /**
     * @return true if x < y
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isLesserThan(OperationResultImportanceType x, @NotNull OperationResultImportanceType y) {
        switch (y) {
            case MAJOR: return x != MAJOR;
            case NORMAL: return x == MINOR;
            case MINOR: return false;
            default: throw new IllegalArgumentException("importance: " + y);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        dumpIndent(sb, indent, true);
        return sb.toString();
    }

    public String dump(boolean withStack) {
        StringBuilder sb = new StringBuilder();
        dumpIndent(sb, 0, withStack);
        return sb.toString();
    }

    private void dumpIndent(StringBuilder sb, int indent, boolean printStackTrace) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("*op* ");
        sb.append(operation);
        sb.append(", st: ");
        sb.append(status);
        if (importance == MINOR) {
            sb.append(", MINOR");
        } else if (importance == MAJOR) {
            sb.append(", MAJOR");
        }
        sb.append(", msg: ");
        sb.append(message);

        if (count > 1) {
            sb.append(" x");
            sb.append(count);
        }
        if (userFriendlyMessage != null) {
            sb.append("\n");
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("userFriendlyMessage=");
            userFriendlyMessage.shortDump(sb);
        }
        if (asynchronousOperationReference != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "asynchronousOperationReference", asynchronousOperationReference, indent + 2);
        }
        sb.append("\n");

        for (Map.Entry<String, Collection<String>> entry : getParams().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[p]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(indent+2, entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getContext().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[c]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(indent+2, entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getReturns().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[r]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(indent+2, entry.getValue()));
            sb.append("\n");
        }

        for (String line : details) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[d]");
            sb.append(line);
            sb.append("\n");
        }

        if (cause != null) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[cause]");
            sb.append(cause.getClass().getSimpleName());
            sb.append(":");
            sb.append(cause.getMessage());
            sb.append("\n");
            if (printStackTrace) {
                dumpStackTrace(sb, cause.getStackTrace(), indent + 4);
                dumpInnerCauses(sb, cause.getCause(), indent + 3);
            }
        }

        for (OperationResult sub : getSubresults()) {
            sub.dumpIndent(sb, indent + 1, printStackTrace);
        }
    }

    private String dumpEntry(int indent, Collection<String> values) {
        if (values == null) {
            return null;
        }
        if (values.size() == 0) {
            return "(empty)";
        }
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return values.toString();
    }

    private void dumpInnerCauses(StringBuilder sb, Throwable innerCause, int indent) {
        if (innerCause == null) {
            return;
        }
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Caused by ");
        sb.append(innerCause.getClass().getName());
        sb.append(": ");
        sb.append(innerCause.getMessage());
        sb.append("\n");
        dumpStackTrace(sb, innerCause.getStackTrace(), indent + 1);
        dumpInnerCauses(sb, innerCause.getCause(), indent);
    }

    private static void dumpStackTrace(StringBuilder sb, StackTraceElement[] stackTrace, int indent) {
        for (StackTraceElement aStackTrace : stackTrace) {
            DebugUtil.indentDebugDump(sb, indent);
            StackTraceElement element = aStackTrace;
            sb.append(element.toString());
            sb.append("\n");
        }
    }

    @Override
    public void shortDump(StringBuilder sb) {
        sb.append(operation).append(" ").append(status).append(" ").append(message);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("R(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }


    public void setBackgroundTaskOid(String oid) {
        setAsynchronousOperationReference(TASK_OID_PREFIX + oid);
        addReturn(RETURN_BACKGROUND_TASK_OID, oid); // deprecated
    }

    public void setCaseOid(String oid) {
        setAsynchronousOperationReference(CASE_OID_PREFIX + oid);
    }

    @Deprecated // use asynchronous operation reference
    public String getBackgroundTaskOid() {
        return getReturnSingle(RETURN_BACKGROUND_TASK_OID);
    }

    @Deprecated
    @Override
    public OperationResult setMinor(boolean value) {
        this.importance = value ? MINOR : NORMAL;
        return this;
    }

    @Override
    public OperationResultBuilder setMinor() {
        return setImportance(MINOR);
    }

    public OperationResultImportanceType getImportance() {
        return importance;
    }

    @Override
    public OperationResult setImportance(OperationResultImportanceType value) {
        this.importance = value;
        return this;
    }

    public void recordThrowableIfNeeded(Throwable t) {
        if (isUnknown()) {
            recordFatalError(t.getMessage(), t);
        }
    }

    public static OperationResult createSubResultOrNewResult(OperationResult parentResult, String operation) {
        if (parentResult == null) {
            return new OperationResult(operation);
        } else {
            return parentResult.createSubresult(operation);
        }
    }

    // primitive implementation - uncomment it if needed
//    public OperationResult clone() {
//        return CloneUtil.clone(this);
//    }

    private static final class OperationStatusKey {

        private String operation;
        private OperationResultStatus status;

        private OperationStatusKey(String operation, OperationResultStatus status) {
            this.operation = operation;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            OperationStatusKey that = (OperationStatusKey) o;

            if (operation != null ? !operation.equals(that.operation) : that.operation != null) return false;
            if (status != that.status) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = operation != null ? operation.hashCode() : 0;
            result = 31 * result + (status != null ? status.hashCode() : 0);
            return result;
        }
    }

    private static class OperationStatusCounter {
        private int shownRecords;        // how many actual records will be shown (after this wave of stripping)
        private int shownCount;            // how many entries will be shown (after this wave of stripping)
        private int hiddenCount;        // how many entries will be hidden (after this wave of stripping)
    }

    public OperationResult clone() {
        return clone(null, true);
    }

    public OperationResult clone(Integer maxDepth, boolean full) {
        OperationResult clone = new OperationResult(operation);

        clone.status = status;
        clone.qualifiers.addAll(qualifiers);
        clone.params = cloneParams(params, full);
        clone.context = cloneParams(context, full);
        clone.returns = cloneParams(returns, full);
        clone.token = token;
        clone.messageCode = messageCode;
        clone.message = message;
        clone.userFriendlyMessage = CloneUtil.clone(userFriendlyMessage);
        // I think it's not necessary to (deeply) clone the exception. They are usually not cloneable. See also MID-5379.
        clone.cause = cause;
        clone.count = count;
        clone.hiddenRecordsCount = hiddenRecordsCount;
        if (subresults != null && (maxDepth == null || maxDepth > 0)) {
            clone.subresults = new ArrayList<>(subresults.size());
            for (OperationResult subresult : subresults) {
                if (subresult != null) {
                    clone.subresults.add(subresult.clone(maxDepth != null ? maxDepth - 1 : null, full));
                }
            }
        }
        clone.details = full ? CloneUtil.clone(details) : details;
        clone.summarizeErrors = summarizeErrors;
        clone.summarizePartialErrors = summarizePartialErrors;
        clone.summarizeSuccesses = summarizeSuccesses;
        clone.importance = importance;
        clone.asynchronousOperationReference = asynchronousOperationReference;

        clone.start = start;
        clone.end = end;
        clone.microseconds = microseconds;
        clone.invocationId = invocationId;
        clone.traces.addAll(CloneUtil.cloneCollectionMembers(traces));

        clone.building = building;
        clone.futureParent = futureParent;

        // todo invocationRecord?

        return clone;
    }

    private Map<String, Collection<String>> cloneParams(Map<String, Collection<String>> map, boolean full) {
        if (full) {
            // TODO: implement more efficient clone
            return CloneUtil.clone(map);
        } else {
            return map;
        }
    }

    @NotNull
    private static OperationResultHandlingStrategyType getCurrentHandlingStrategy() {
        OperationResultHandlingStrategyType local = LOCAL_HANDLING_STRATEGY.get();
        if (local != null) {
            return local;
        } else {
            return globalHandlingStrategy;
        }
    }

    private static int getSubresultStripThreshold() {
        return defaultIfNull(getCurrentHandlingStrategy().getSubresultStripThreshold(), DEFAULT_SUBRESULT_STRIP_THRESHOLD);
    }

    @NotNull
    private static OperationResultImportanceType getPreserveDuringCleanup() {
        return defaultIfNull(getCurrentHandlingStrategy().getPreserveDuringCleanup(), NORMAL);
    }

    public static void applyOperationResultHandlingStrategy(
            @NotNull List<OperationResultHandlingStrategyType> configuredStrategies, Integer stripThresholdDeprecated) {
        if (!configuredStrategies.isEmpty()) {
            handlingStrategies = CloneUtil.cloneCollectionMembers(configuredStrategies);
        } else if (stripThresholdDeprecated != null) {
            handlingStrategies = singletonList(new OperationResultHandlingStrategyType().subresultStripThreshold(stripThresholdDeprecated));
        } else {
            handlingStrategies = singletonList(DEFAULT_HANDLING_STRATEGY);
        }
        selectGlobalHandlingStrategy();
    }

    private static void selectGlobalHandlingStrategy() {
        if (handlingStrategies.isEmpty()) {
            globalHandlingStrategy = DEFAULT_HANDLING_STRATEGY;
        } else if (handlingStrategies.size() == 1) {
            globalHandlingStrategy = handlingStrategies.get(0);
        } else {
            List<OperationResultHandlingStrategyType> globalOnes = handlingStrategies.stream()
                    .filter(s -> Boolean.TRUE.equals(s.isGlobal())).collect(Collectors.toList());
            if (globalOnes.isEmpty()) {
                globalHandlingStrategy = handlingStrategies.get(0);
                LOGGER.warn("Found no handling strategy marked as global, selecting the first one among all strategies: {}",
                        globalHandlingStrategy);
            } else if (globalOnes.size() == 1) {
                globalHandlingStrategy = globalOnes.get(0);
            } else {
                globalHandlingStrategy = globalOnes.get(0);
                LOGGER.warn("Found {} handling strategies marked as global, selecting the first one among them: {}",
                        globalOnes.size(), globalHandlingStrategy);
            }
        }
    }

    public static void setThreadLocalHandlingStrategy(@Nullable String strategyName) {
        OperationResultHandlingStrategyType selected;
        if (strategyName == null) {
            selected = globalHandlingStrategy;
        } else {
            Optional<OperationResultHandlingStrategyType> found = handlingStrategies.stream()
                    .filter(s -> strategyName.equals(s.getName()))
                    .findFirst();
            if (found.isPresent()) {
                selected = found.get();
            } else {
                LOGGER.error("Couldn't find operation result handling strategy '{}' - using the global one", strategyName);
                selected = globalHandlingStrategy;
            }
        }
        LOGGER.trace("Selected handling strategy: {}", selected);
        LOCAL_HANDLING_STRATEGY.set(selected);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OperationResult)) return false;
        OperationResult result = (OperationResult) o;
        return token == result.token &&
                count == result.count &&
                hiddenRecordsCount == result.hiddenRecordsCount &&
                summarizeErrors == result.summarizeErrors &&
                summarizePartialErrors == result.summarizePartialErrors &&
                summarizeSuccesses == result.summarizeSuccesses &&
                importance == result.importance &&
                building == result.building &&
                Objects.equals(start, result.start) &&
                Objects.equals(end, result.end) &&
                Objects.equals(microseconds, result.microseconds) &&
                Objects.equals(invocationId, result.invocationId) &&
                Objects.equals(tracingProfile, result.tracingProfile) &&
                Objects.equals(operation, result.operation) &&
                Objects.equals(qualifiers, result.qualifiers) &&
                status == result.status &&
                Objects.equals(params, result.params) &&
                Objects.equals(context, result.context) &&
                Objects.equals(returns, result.returns) &&
                Objects.equals(messageCode, result.messageCode) &&
                Objects.equals(message, result.message) &&
                Objects.equals(userFriendlyMessage, result.userFriendlyMessage) &&
                Objects.equals(cause, result.cause) &&
                Objects.equals(subresults, result.subresults) &&
                Objects.equals(details, result.details) &&
                Objects.equals(traces, result.traces) &&
                Objects.equals(asynchronousOperationReference, result.asynchronousOperationReference);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(operation, qualifiers, status, params, context, returns, token, messageCode, message, userFriendlyMessage, cause, count,
                        hiddenRecordsCount, subresults, details, summarizeErrors, summarizePartialErrors, summarizeSuccesses,
                        building, start, end, microseconds, invocationId, traces,
                        asynchronousOperationReference);
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getEnd() {
        return end;
    }

    public void setEnd(Long end) {
        this.end = end;
    }

    public Long getMicroseconds() {
        return microseconds;
    }

    public void setMicroseconds(Long microseconds) {
        this.microseconds = microseconds;
    }

    public Long getInvocationId() {
        return invocationId;
    }

    public void setInvocationId(Long invocationId) {
        this.invocationId = invocationId;
    }

    public boolean isTraced() {
        return tracingProfile != null;
    }

    public CompiledTracingProfile getTracingProfile() {
        return tracingProfile;
    }

    public List<TraceType> getTraces() {
        return traces;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }

    public String getCallerReason() {
        return callerReason;
    }

    public void setCallerReason(String callerReason) {
        this.callerReason = callerReason;
    }

    public List<LogSegmentType> getLogSegments() {
        return logSegments;
    }

    public TraceDictionaryType getExtractedDictionary() {
        return extractedDictionary;
    }

    public void setExtractedDictionary(TraceDictionaryType extractedDictionary) {
        this.extractedDictionary = extractedDictionary;
    }
}

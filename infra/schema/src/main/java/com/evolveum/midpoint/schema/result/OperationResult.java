/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.result;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import static com.evolveum.midpoint.util.NoValueUtil.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

import static com.evolveum.midpoint.prism.util.CloneUtil.cloneCloneable;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultImportanceType.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.statistics.BasicComponentStructure;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.statistics.OperationsPerformanceMonitorImpl;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.internals.ThreadLocalOperationsMonitor;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.schema.util.ParamsTypeUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LevelOverrideTurboFilter;
import com.evolveum.midpoint.util.logging.LoggingLevelOverrideConfiguration;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.util.statistics.OperationInvocationRecord;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * Provides rich information about an operation being executed; mainly for the sake of error reporting and functional/performance troubleshooting.
 *
 * == Information Collected
 *
 * There is a lot of information collected, but the following properties are the most important:
 *
 * - result *status* ({@link OperationResultStatus}): success, partial/fatal error, warning, ..., along with
 * an optional *message* and *Java exception*,
 * - operation invocation *parameters*, *return value(s)*, and sometimes information about the execution *context* (e.g. implementation
 * class name),
 * - *performance-related information*, like start/end timestamps, or duration (for performance diagnostics),
 * - {@link TraceType} records (for troubleshooting),
 * - *logfile lines* produced during the operation execution (for troubleshooting).
 *
 * The structure is hierarchical, i.e. an {@link OperationResult} instance contains a set of results of inner operations.
 *
 * == Typical Uses
 *
 * - This object can be used by GUI to display smart (and interactive) error information.
 * - It can also be used by the client code (Java or REST) to detect deeper problems in the invocations, retry or otherwise
 * compensate for the errors or decide how severe the error was and whether it is possible to proceed.
 * - The performance information collected is recorded in tasks (see {@link OperationsPerformanceInformationType}) and shown in GUI.
 * See also {@link OperationsPerformanceMonitorImpl} class.
 * - The functional and performance data collected are used for (experimental)
 * link:https://docs.evolveum.com/midpoint/reference/diag/troubleshooting/troubleshooting-with-traces/[troubleshooting
 * with traces].
 *
 * == Developer's Guidelines
 *
 * In order to ensure that all necessary information is correctly captured, a developer of any method that writes into
 * {@link OperationResult} instances has to follow a couple of basic principles:
 *
 * === Principle 1: Correct Closure
 *
 * Any operation result created has to be correctly _closed_. This means that its {@link #status} should be changed from
 * the initial {@link OperationResultStatus#UNKNOWN} value to a more specific one. (Otherwise, a run-time exception is
 * thrown in {@link #cleanupResult(Throwable)} method.) Also, to ensure correct fulfillment of the other duties,
 * {@link #recordEnd()} method has be - directly or indirectly - called as well (see below).
 *
 * === Principle 2: Single Result Per Thread
 *
 * At any instant, in any thread, there should be at most one {@link OperationResult} "active", i.e. opened by
 * {@link #createSubresult(String)} or its variants (e.g. {@link #createMinorSubresult(String)}, {@link #subresult(String)}, ...),
 * and not yet closed nor "superseded" by creating its own child result. This is to ensure that logfile lines will be correctly
 * captured, if tracing is enabled. It is also required for correct collection of performance data.
 *
 * === Principle 3: Opening-Closure Pairing
 *
 * Because the operation result is used also to troubleshoot performance issues, it should be clear where (in the code)
 * the result is created and where it is closed.
 *
 * When is result closed, anyway? The result is automatically closed when the status is recorded: either _explicitly_
 * (like in {@link #recordSuccess()}) or _implicitly_ (e.g. in {@link #computeStatus()}). All those methods invoke
 * {@link #recordEnd()} that does all the magic related to performance information capturing, log records capturing,
 * and (occasionally used) method call logging.
 *
 * To ensure easy, clear and unambiguous interpretation of the performance data, the developer should make sure that
 * for each {@link OperationResult} created, it is obvious _where_ the result is closed. The best way how to ensure
 * this is to create the result at the beginning (or near the beginning) of a related method, and close it - including writing
 * the status - at the end (or near the end) of the same method. No recording of the status should be done between these points.
 *
 * If there's a need to set the value of {@link #status} somewhere during the operation execution (assuming that
 * there's non-negligible processing after that point), {@link #setStatus(OperationResultStatus)} method or its variants
 * (like {@link #setSuccess()}) can be used. These fill-in {@link #status} field without closing the whole operation result.
 *
 * === Principle 4: Preserve component boundaries
 *
 * {@link OperationResult} objects are used to monitor processing time spent in individual midPoint components.
 * (See {@link BasicComponentStructure} for the mostly used structure.) Hence, it's crucial that an operation result
 * is created when crossing component boundaries.
 *
 * A special case are result handlers, where the control flows upwards the structure.
 * See {@link ResultHandler#providingOwnOperationResult(String)} for more information.
 *
 * === Note: Recording Exceptions
 *
 * The Correct Closure Principle (#1) dictates that the operation result has to be correctly closed even in the case
 * of exception occurring. That is why we usually create a "try-catch" block for the code covered by a single operation result.
 * Traditionally, the {@link #recordFatalError(Throwable)} call was put into the `catch` code.
 *
 * However, there may be situations where a different handling is required:
 *
 * . If the exception is non-fatal or even completely benign. This may be the case of e.g. expected ("allowed")
 * object-not-found conditions. See {@link ObjectNotFoundException#getSeverity()}.
 * . If the exception was processed by other means. For example, a custom error message was already provided.
 *
 * To handle these cases, {@link #recordException(Throwable)} should be used instead of {@link #recordFatalError(Throwable)}.
 * The difference is that the former checks the {@link #exceptionRecorded} flag to see if the exception was already
 * processed. See also {@link #markExceptionRecorded()}.
 *
 * NOTE: This mechanism is *experimental*.
 *
 * === Suggested Use
 *
 * Stemming from the above, the following can be seen as a suggested way how to use the operation result:
 *
 * [source,java]
 * ----
 * private static final OP_SOME_METHOD = ThisClass.class.getName() + ".someMethod";
 *
 * void someMethod(String param1, Object param2, OperationResult parentResult) throws SomeException {
 *     OperationResult result = parentResult.subresult(OP_SOME_METHOD)
 *                     .addParam("param1", param1)
 *                     .addArbitraryObjectAsParam("param2", param2)
 *                     .build();
 *     try {
 *         // ... some meat here ...
 *     } catch (SomeException | RuntimeException e) {
 *         result.recordException(e);
 *         throw e;
 *     } finally {
 *         result.close();
 *     }
 * }
 * ----
 *
 * Note that the {@link #close()} method (a newer form of legacy {@link #computeStatusIfUnknown()}) automatically computes
 * the result based on subresults (assuming success if there's no indication of a failure). In theory we could put it inside
 * the `try` block (because `recordFatalError` effectively closes the result as well), but using `finally` is perhaps more
 * explicit. It may be also more future-proof if we would decide to add some extra functionality right into {@link #close()}
 * method itself.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
public class OperationResult
        implements Serializable, DebugDumpable, ShortDumpable, Cloneable,
        OperationResultBuilder, Visitable<OperationResult> {

    @Serial private static final long serialVersionUID = -2467406395542291044L;
    private static final String VARIOUS_VALUES = "[various values]";

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

    public static final String CONTEXT_IMPLEMENTATION_CLASS = "implementationClass";
    public static final String CONTEXT_PROGRESS = "progress";
    public static final String CONTEXT_OID = "oid";
    public static final String CONTEXT_OBJECT = "object";
    public static final String CONTEXT_ITEM = "item";
    public static final String CONTEXT_REASON = "reason";

    public static final String PARAM_OID = "oid";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_TYPE = "type";
    public static final String PARAM_OPTIONS = "options";
    public static final String PARAM_RESOURCE = "resource";
    public static final String PARAM_TASK = "task";
    public static final String PARAM_OBJECT = "object";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_PROJECTION = "projection";
    public static final String PARAM_LANGUAGE = "language";
    public static final String PARAM_POLICY_RULE = "policyRule";
    public static final String PARAM_POLICY_RULE_ID = "policyRuleId";

    public static final String RETURN_COUNT = "count";
    public static final String RETURN_COMMENT = "comment";
    public static final String DEFAULT = "";

    /**
     * Standard name for a method that handles an object found in a search operation.
     * See {@link ResultHandler#providingOwnOperationResult(String)}.
     */
    public static final String HANDLE_OBJECT_FOUND = "handleObjectFound";

    private static long tokenCount = 1000000000000000000L;

    private final String operation;
    private OperationKindType operationKind;
    private OperationResultStatus status;

    // Values of the following maps should NOT be null. But in reality it does happen.
    // If there is a null value, it should be stored as a single-item collection, where the item is null.
    // But the collection should not be null. TODO; fix this
    private Map<String, Collection<String>> params;
    private Map<String, Collection<String>> context;
    private Map<String, Collection<String>> returns;
    @NotNull private final List<String> qualifiers = new ArrayList<>();

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

    // The following fields use "long" instead of "Long" because of performance reasons.

    @CanBeNone private long start = NONE_LONG;
    @CanBeNone private long end = NONE_LONG;
    @CanBeNone private long microseconds = NONE_LONG;
    @CanBeNone private long ownMicroseconds = NONE_LONG;
    @CanBeNone private long cpuMicroseconds = NONE_LONG;
    @CanBeNone private long invocationId = NONE_LONG;

    private final List<LogSegmentType> logSegments = new ArrayList<>();

    /** See {@link #markExceptionRecorded()}. */
    private boolean exceptionRecorded;

    // The following properties are NOT SERIALIZED
    private CompiledTracingProfile tracingProfile;

    /** Whether we should preserve the content of the result e.g. for the sake of reporting. */
    private boolean preserve;

    /**
     * If `false`, all the parameters, return values, and context information is omitted.
     * Useful for improving task performance, as it should spare a lot of `toString()` calls.
     *
     * If {@link #preserve} is `true` or {@link #tracingProfile} is set up, this one should be `true`.
     */
    private boolean recordingValues = true;

    /**
     * True if we collect log entries.
     * Maybe it could be replaced by checking {@link #logRecorder} being not null and open?
     */
    private boolean collectingLogEntries;

    /**
     * Collects log entries for the current operation result. Directly updates {@link #logSegments}.
     */
    private transient LogRecorder logRecorder;

    /**
     * Log recorder for the parent operation result. Kept for safety check reasons.
     */
    private transient LogRecorder parentLogRecorder;

    private boolean startedLoggingOverride;

    /**
     * After a trace rooted at this operation result is stored, the dictionary that was extracted is stored here.
     * It is necessary to correctly interpret traces in this result and its subresults.
     */
    private TraceDictionaryType extractedDictionary; // NOT SERIALIZED

    private final List<TraceType> traces = new ArrayList<>();

    /** The operation monitoring configuration for the current thread when the current operation started. */
    private OperationMonitoringConfiguration operationMonitoringConfigurationAtStart;

    /**
     * Statistics related to executed monitored operations at start - needed to compute the difference,
     * to be stored in this result in {@link #monitoredOperations}.
     */
    private ThreadLocalOperationsMonitor.ExecutedOperations executedMonitoredOperationsAtStart;

    /**
     * Monitored operations that occurred during this operation execution.
     */
    private MonitoredOperationsStatisticsType monitoredOperations;

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

    /**
     * Resolving that status HANDLED_ERROR will be propagated to parent as SUCCESS.
     */
    private boolean propagateHandledErrorAsSuccess = true;

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

    public OperationResult keepRootOnly() {
        return new OperationResult(getOperation(), null, getStatus(), 0, getMessageCode(), getMessage(), null, null, null);
    }

    public static OperationResultBuilder newResult(String operation) {
        OperationResult result = new OperationResult(operation);
        result.building = true;
        return result;
    }

    public OperationResultBuilder subresult(String operation) {
        OperationResult subresult = new OperationResult(operation);
        subresult.building = true;
        subresult.futureParent = this;
        subresult.tracingProfile = tracingProfile;
        subresult.preserve = preserve;
        subresult.recordingValues = recordingValues;
        subresult.parentLogRecorder = logRecorder;
        subresult.propagateHandledErrorAsSuccess = propagateHandledErrorAsSuccess;
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
        recordStart(operation, createArguments());
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

    private void recordStart(String operation, Object[] arguments) {
        if (tracingProfile != null) {
            startLoggingIfRequested();
            startOperationMonitoring();
        }
        // TODO for very minor operation results (e.g. those dealing with mapping and script execution)
        //  we should consider skipping creation of invocationRecord. It includes some string manipulation(s)
        //  and a call to System.nanoTime that could unnecessarily slow down midPoint operation.
        //  But beware, it would disable measurements of these operations e.g. in task internal performance info panel.
        boolean measureCpuTime = tracingProfile != null && tracingProfile.isMeasureCpuTime();
        invocationRecord = OperationInvocationRecord.create(operation, arguments, measureCpuTime);
        invocationId = invocationRecord.getInvocationId();
        start = System.currentTimeMillis();
    }

    private void startOperationMonitoring() {
        ThreadLocalOperationsMonitor monitor = ThreadLocalOperationsMonitor.get();
        operationMonitoringConfigurationAtStart = monitor.getConfiguration();
        executedMonitoredOperationsAtStart = monitor.getExecutedOperations().copy();
        monitor.setConfiguration(
                tracingProfile.getOperationMonitoringConfiguration());
    }

    private void stopOperationMonitoring() {
        ThreadLocalOperationsMonitor monitor = ThreadLocalOperationsMonitor.get();
        setMonitoredOperations(
                monitor.getMonitoredOperationsRecord(executedMonitoredOperationsAtStart));
        monitor.setConfiguration(operationMonitoringConfigurationAtStart);
    }

    private void startLoggingIfRequested() {
        collectingLogEntries = tracingProfile.isCollectingLogEntries();
        if (collectingLogEntries) {
            LoggingLevelOverrideConfiguration loggingOverrideConfiguration =
                    tracingProfile.getLoggingLevelOverrideConfiguration();
            if (loggingOverrideConfiguration != null && !LevelOverrideTurboFilter.isActive()) {
                LevelOverrideTurboFilter.overrideLogging(loggingOverrideConfiguration);
                startedLoggingOverride = true;
            }
            logRecorder = LogRecorder.open(logSegments, parentLogRecorder, this);
        }
    }

    private Object[] createArguments() {
        List<String> arguments = new ArrayList<>();
        getParams().forEach((key, value) -> arguments.add(key + " => " + value)); // todo what with large values?
        getContext().forEach((key, value) -> arguments.add("c:" + key + " => " + value));
        return arguments.toArray();
    }

    public OperationResult createSubresult(String operation) {
        return createSubresult(operation, false, new Object[0]);
    }

    public OperationResult createMinorSubresult(String operation) {
        return createSubresult(operation, true, null);
    }

    private OperationResult createSubresult(String operation, boolean minor, Object[] arguments) {
        OperationResult subresult = new OperationResult(operation);
        subresult.recordCallerReason(this);
        addSubresult(subresult);
        subresult.parentLogRecorder = logRecorder;
        subresult.importance = minor ? MINOR : NORMAL;
        subresult.recordStart(operation, arguments);
        return subresult;
    }

    // todo determine appropriate places where recordEnd() should be called
    public void recordEnd() {
        if (invocationRecord != null) {
            // This is not quite clean. We should report the exception via processException method - but that does not allow
            // showing return values that can be present in operation result. So this is a hack until InvocationRecord is fixed.
            invocationRecord.processReturnValue(getReturns(), cause);
            invocationRecord.afterCall(
                    computeNotOwnTimeMicros());
            microseconds = invocationRecord.getElapsedTimeMicros();
            ownMicroseconds = invocationRecord.getOwnTimeMicros();
            cpuMicroseconds = invocationRecord.getCpuTimeMicros();
            if (collectingLogEntries) {
                logRecorder.close();
                collectingLogEntries = false;
            }
            invocationRecord = null;
        }
        end = System.currentTimeMillis();
        if (startedLoggingOverride) {
            LevelOverrideTurboFilter.cancelLoggingOverride();
            startedLoggingOverride = false;
        }
        if (executedMonitoredOperationsAtStart != null) {
            stopOperationMonitoring();
        }
    }

    private long computeNotOwnTimeMicros() {
        long total = 0;
        for (OperationResult subresult : getSubresults()) {
            total += zeroIfNone(subresult.microseconds);
        }
        return total;
    }

    /**
     * Checks if the recorder was correctly flushed. Used before writing the trace file.
     */
    public void checkLogRecorderFlushed() {
        if (logRecorder != null) {
            logRecorder.checkFlushed();
        }
        getSubresults().forEach(OperationResult::checkLogRecorderFlushed);
    }

    /**
     * Reference to an asynchronous operation that can be used to retrieve
     * the status of the running operation. This may be a task identifier,
     * identifier of a ticket in ITSM system or anything else. The exact
     * format of this reference depends on the operation which is being
     * executed.
     *
     * Looks only in the current result. See {@link #findAsynchronousOperationReference()} for the recursive version.
     */
    public String getAsynchronousOperationReference() {
        return asynchronousOperationReference;
    }

    public void setAsynchronousOperationReference(String asynchronousOperationReference) {
        this.asynchronousOperationReference = asynchronousOperationReference;
    }

    @VisibleForTesting
    public void clearAsynchronousOperationReferencesDeeply() {
        setAsynchronousOperationReference(null);
        getSubresults().forEach(OperationResult::clearAsynchronousOperationReferencesDeeply);
    }

    /**
     * This method partially duplicates functionality of computeStatus. However, computeStatus
     * currently does not propagate taskOid from tasks switched to background, because switchToBackground
     * sets its result to SUCCESS (not IN_PROGRESS) because of some historical reasons. So,
     * until this is fixed somehow, this is a bit of hack to fetch asynchronous operation reference
     * even in such cases.
     */
    public @Nullable String findAsynchronousOperationReference() {
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

    /** A convenience method. (Assumes we have only a single asynchronous operation reference in the tree.) */
    public @Nullable String findTaskOid() {
        return referenceToTaskOid(
                findAsynchronousOperationReference());
    }

    /** A convenience method. (Assumes we have only a single asynchronous operation reference in the tree.) */
    public @Nullable String findCaseOid() {
        return referenceToCaseOid(
                findAsynchronousOperationReference());
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

    private void incrementCount() {
        this.count++;
    }

    public int getHiddenRecordsCount() {
        return hiddenRecordsCount;
    }

    public void setHiddenRecordsCount(int hiddenRecordsCount) {
        this.hiddenRecordsCount = hiddenRecordsCount;
    }

    private boolean representsHiddenRecords() {
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
     * Method returns list of operation subresults {@link OperationResult}.
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
            return subresults.get(subresults.size() - 1);
        }
    }

    public void removeLastSubresult() {
        if (subresults != null && !subresults.isEmpty()) {
            subresults.remove(subresults.size() - 1);
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
            subresult.recordingValues = recordingValues;
        } else {
            subresult.recordingValues = true;
        }
        subresult.preserve = preserve;
        subresult.propagateHandledErrorAsSuccess = propagateHandledErrorAsSuccess;
    }

    public OperationResult findSubresult(String operation) {
        if (subresults == null) {
            return null;
        }
        for (OperationResult subResult : getSubresults()) {
            if (operation.equals(subResult.getOperation())) {
                return subResult;
            }
        }
        return null;
    }

    /** Finds given operation in a subtree rooted by the current op. result. Includes this one! */
    public @NotNull List<OperationResult> findSubresultsDeeply(@NotNull String operation) {
        List<OperationResult> matching = new ArrayList<>();
        collectMatchingSubresults(operation, matching);
        return matching;
    }

    private void collectMatchingSubresults(String operation, List<OperationResult> matching) {
        if (operation.equals(this.operation)) {
            matching.add(this);
        }
        if (subresults != null) {
            subresults.forEach(
                    subresult -> subresult.collectMatchingSubresults(operation, matching));
        }
    }

    /** Finds given operation among subresults of the current op. result. */
    public List<OperationResult> findSubresults(String operation) {
        List<OperationResult> found = new ArrayList<>();
        if (subresults == null) {
            return found;
        }
        for (OperationResult subresult : subresults) {
            if (operation.equals(subresult.getOperation())) {
                found.add(subresult);
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

    public OperationResultStatusType getStatusBean() {
        return OperationResultStatus.createStatusType(status);
    }

    /**
     * Sets the status _without_ recording operation end.
     */
    public void setStatus(OperationResultStatus status) {
        this.status = status;
    }

    /**
     * Returns true if the result is success.
     * <p>
     * This returns true if the result is absolute success. Presence of partial
     * failures or warnings fail this test.
     *
     * @return true if the result is success.
     */
    public boolean isSuccess() {
        return status == OperationResultStatus.SUCCESS;
    }

    public boolean isWarning() {
        return status == OperationResultStatus.WARNING;
    }

    /**
     * Returns true if the result is acceptable for further processing.
     * <p>
     * In other words: if there were no fatal errors. Warnings and partial
     * errors are acceptable. Yet, this test also fails if the operation state
     * is not known.
     *
     * @return true if the result is acceptable for further processing.
     */
    public boolean isAcceptable() {
        return status != OperationResultStatus.FATAL_ERROR;
    }

    public boolean isUnknown() {
        return status == OperationResultStatus.UNKNOWN;
    }

    public boolean isInProgress() {
        return (status == OperationResultStatus.IN_PROGRESS);
    }

    public boolean isError() {
        return status != null && status.isError();
    }

    public boolean isFatalError() {
        return status == OperationResultStatus.FATAL_ERROR;
    }

    public boolean isPartialError() {
        return status == OperationResultStatus.PARTIAL_ERROR;
    }

    public boolean isHandledError() {
        return status == OperationResultStatus.HANDLED_ERROR;
    }

    public boolean isNotApplicable() {
        return status == OperationResultStatus.NOT_APPLICABLE;
    }

    /**
     * Set all error status in this result and all subresults as handled.
     */
    public void muteErrorsRecursively() {
        muteError();
        for (OperationResult subresult : getSubresults()) {
            subresult.muteErrorsRecursively();
        }
    }

    /**
     * Computes operation result status based on subtask status and sets an
     * error message if the status is FATAL_ERROR.
     *
     * @param errorMessage error message
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

            OperationResultStatus subStatus = sub.getStatus();
            if (subStatus != OperationResultStatus.NOT_APPLICABLE) {
                allNotApplicable = false;
            }
            if (subStatus == OperationResultStatus.FATAL_ERROR) {
                status = OperationResultStatus.FATAL_ERROR;
                if (message == null) {
                    message = sub.getMessage();
                } else {
                    message = message + ": " + sub.getMessage();
                }
                updateLocalizableMessage(sub);
                return;
            }
            if (subStatus == OperationResultStatus.IN_PROGRESS) {
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
            if (subStatus == OperationResultStatus.PARTIAL_ERROR) {
                newStatus = OperationResultStatus.PARTIAL_ERROR;
                newMessage = sub.getMessage();
                newUserFriendlyMessage = sub.getUserFriendlyMessage();
            }
            if (!propagateHandledErrorAsSuccess
                    && newStatus != OperationResultStatus.PARTIAL_ERROR
                    && subStatus == OperationResultStatus.HANDLED_ERROR) {
                newStatus = OperationResultStatus.HANDLED_ERROR;
                newMessage = sub.getMessage();
                newUserFriendlyMessage = sub.getUserFriendlyMessage();
            }
            if (subStatus != OperationResultStatus.SUCCESS && subStatus != OperationResultStatus.NOT_APPLICABLE
                    && (!propagateHandledErrorAsSuccess || subStatus != OperationResultStatus.HANDLED_ERROR)) {
                allSuccess = false;
            }
            if (newStatus != OperationResultStatus.HANDLED_ERROR
                    && subStatus == OperationResultStatus.WARNING) {
                newStatus = OperationResultStatus.WARNING;
                newMessage = sub.getMessage();
                newUserFriendlyMessage = sub.getUserFriendlyMessage();
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
        if (userFriendlyMessage == null) {
            userFriendlyMessage = localizableMessage;
        } else if (userFriendlyMessage instanceof SingleLocalizableMessage) {
            if (!userFriendlyMessage.equals(localizableMessage)) {
                userFriendlyMessage = new LocalizableMessageListBuilder()
                        .message(userFriendlyMessage)
                        .message(localizableMessage)
                        .separator(LocalizableMessageList.SPACE).build();
            }
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
        if(rv.contains(increment)){
            return rv;
        }

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
        StringJoiner operationMessageJoiner = new StringJoiner(", ");
        for (OperationResult sub : getSubresults()) {
            if (sub.getStatus() != OperationResultStatus.NOT_APPLICABLE) {
                allNotApplicable = false;
            }
            if (sub.getStatus() != OperationResultStatus.FATAL_ERROR) {
                allFatalError = false;
            }
            if (sub.getStatus() == OperationResultStatus.FATAL_ERROR) {
                hasError = true;
                if (sub.getMessage() != null) {
                    operationMessageJoiner.add(sub.getMessage());
                }
            }
            if (sub.getStatus() == OperationResultStatus.PARTIAL_ERROR) {
                hasError = true;
                if (sub.getMessage() != null) {
                    operationMessageJoiner.add(sub.getMessage());
                }
            }
            if (sub.getStatus() == OperationResultStatus.HANDLED_ERROR) {
                hasHandledError = true;
                if (sub.getMessage() != null) {
                    operationMessageJoiner.add(sub.getMessage());
                }
            }
            if (sub.getStatus() == OperationResultStatus.IN_PROGRESS) {
                hasInProgress = true;
                if (sub.getMessage() != null) {
                    operationMessageJoiner.add(sub.getMessage());
                }
                if (asynchronousOperationReference == null) {
                    asynchronousOperationReference = sub.getAsynchronousOperationReference();
                }
            }
            if (sub.getStatus() == OperationResultStatus.WARNING) {
                hasWarning = true;
                if (sub.getMessage() != null) {
                    operationMessageJoiner.add(sub.getMessage());
                }
            }
        }
        if (operationMessageJoiner.length() > 0) {
            message = operationMessageJoiner.toString();
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

    /** TEMPORARY. We need to find a way how to override this when we need the recording for specific task. */
    public OperationResultBuilder notRecordingValues() {
        this.recordingValues = false;
        return this;
    }

    @Override
    public OperationResultBuilder tracingProfile(CompiledTracingProfile profile) {
        this.tracingProfile = profile;
        if (profile != null) {
            this.recordingValues = true;
        }
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

    public boolean isTracingAny(Class<? extends TraceType> traceClass) {
        return isTracing(traceClass, TracingLevelType.MINIMAL);
    }

    public boolean isTracingDetailed(Class<? extends TraceType> traceClass) {
        return isTracing(traceClass, TracingLevelType.DETAILED);
    }

    public boolean isTracing(Class<? extends TraceType> traceClass, TracingLevelType threshold) {
        return TraceUtil.isAtLeast(getTracingLevel(traceClass), threshold);
    }

    @NotNull
    public TracingLevelType getTracingLevel(Class<? extends TraceType> traceClass) {
        if (tracingProfile != null) {
            return tracingProfile.getLevel(traceClass);
        } else {
            return TracingLevelType.OFF;
        }
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
                        .flatMap(OperationResult::getResultStream));
    }

    public OperationResult findSubResultStrictly(String operation) {
        return emptyIfNull(subresults).stream()
                .filter(r -> operation.equals(r.getOperation()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Subresult '" + operation + "' not found"));
    }

    /**
     * This is used in situations where handled error is actually the success.
     *
     * For example, when writing an operation execution record to an object which we expect to be deleted,
     * we consider such an operation to be a success. We do not want to bother user or administrator with
     * the information that there was something that went wrong - when, in fact, it was really expected.
     */
    @Experimental
    public void switchHandledErrorToSuccess() {
        if (status == OperationResultStatus.HANDLED_ERROR) {
            status = OperationResultStatus.SUCCESS;
        }
    }

    /**
     * Removes subresults if they can be safely removed.
     *
     * Used in places where we can expect a lot of children (although possibly summarized).
     *
     * Must be called on closed result, in order to avoid mis-computation of own time.
     */
    public void deleteSubresultsIfPossible() {
        stateCheck(isClosed(), "operation result is not closed: %s", this);
        if (isSuccess() && canBeCleanedUp()) {
            getSubresults().clear();
        }
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

    public void close() {
        computeStatusIfUnknown();
    }

    public boolean isClosed() {
        return status != OperationResultStatus.UNKNOWN
                && status != null
                && end != NONE_LONG
                && invocationRecord == null;
    }

    public void computeStatusIfUnknown() {
        recordEnd();
        if (isUnknown()) {
            computeStatus();
        }
    }

    // TODO maybe we should declare the whole operation result as "composite"
    public void computeStatusIfUnknownComposite() {
        recordEnd();
        if (isUnknown()) {
            computeStatusComposite();
        }
    }

    public void recomputeStatus() {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original status
        if (subresults != null && !subresults.isEmpty()) {
            computeStatus();
        }
    }

    public void recomputeStatus(String errorMessage) {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original status
        if (subresults != null && !subresults.isEmpty()) {
            computeStatus(errorMessage);
        }
    }

    public void recomputeStatus(String errorMessage, String warningMessage) {
        recordEnd();
        // Only recompute if there are subresults, otherwise keep original status
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

    public void recordNotApplicable(String message) {
        recordStatus(OperationResultStatus.NOT_APPLICABLE, message);
    }

    public void setNotApplicable() {
        setNotApplicable(null);
    }

    public void setNotApplicable(String message) {
        setStatus(OperationResultStatus.NOT_APPLICABLE, message, null);
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

    public @Nullable ParamsType getParamsBean() {
        return ParamsTypeUtil.toParamsType(getParams());
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
            throw new IllegalStateException("More than one parameter " + name + " in " + this);
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
        if (recordingValues) {
            getParams().put(name, collectionize(value));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, PrismObject<? extends ObjectType> value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectType value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, boolean value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, long value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, int value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult addParam(String name, Class<?> value) {
        if (recordingValues) {
            if (value != null && ObjectType.class.isAssignableFrom(value)) {
                getParams().put(
                        name,
                        collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>) value).getObjectTypeUri()));
            } else {
                getParams().put(name, collectionize(stringify(value)));
            }
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, QName value) {
        if (recordingValues) {
            getParams().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, PolyString value) {
        if (recordingValues) {
            getParams().put(name, collectionize(value == null ? null : value.getOrig()));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectQuery value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, ObjectDelta<?> value) {
        if (recordingValues) {
            getParams().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addParam(String name, String... values) {
        if (recordingValues) {
            getParams().put(name, collectionize(values));
        }
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectAsParam(String paramName, Object paramValue) {
        if (recordingValues) {
            getParams().put(paramName, collectionize(stringify(paramValue)));
        }
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectCollectionAsParam(String name, Collection<?> value) {
        if (recordingValues) {
            getParams().put(name, stringifyCol(value));
        }
        return this;
    }

    public @NotNull Map<String, Collection<String>> getContext() {
        if (context == null) {
            context = new HashMap<>();
        }
        return context;
    }

    public @Nullable ParamsType getContextBean() {
        return ParamsTypeUtil.toParamsType(getContext());
    }

    @Override
    public OperationResult addContext(String name, String value) {
        if (recordingValues) {
            getContext().put(name, collectionize(value));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, PrismObject<? extends ObjectType> value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectType value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, boolean value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, long value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, int value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public OperationResult addContext(String name, Class<?> value) {
        if (recordingValues) {
            if (value != null && ObjectType.class.isAssignableFrom(value)) {
                getContext().put(
                        name,
                        collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>) value).getObjectTypeUri()));
            } else {
                getContext().put(name, collectionize(stringify(value)));
            }
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, QName value) {
        if (recordingValues) {
            getContext().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, PolyString value) {
        if (recordingValues) {
            getContext().put(name, collectionize(value == null ? null : value.getOrig()));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectQuery value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, ObjectDelta<?> value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addContext(String name, String... values) {
        if (recordingValues) {
            getContext().put(name, collectionize(values));
        }
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectAsContext(String name, Object value) {
        if (recordingValues) {
            getContext().put(name, collectionize(stringify(value)));
        }
        return this;
    }

    @Override
    public OperationResult addArbitraryObjectCollectionAsContext(String paramName, Collection<?> paramValue) {
        if (recordingValues) {
            getContext().put(paramName, stringifyCol(paramValue));
        }
        return this;
    }

    @NotNull
    public Map<String, Collection<String>> getReturns() {
        if (returns == null) {
            returns = new HashMap<>();
        }
        return returns;
    }

    public @Nullable ParamsType getReturnsBean() {
        return ParamsTypeUtil.toParamsType(getReturns());
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
            throw new IllegalStateException("More than one return " + name + " in " + this);
        }
        return values.iterator().next();
    }

    public void addReturn(String name, String value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(value));
        }
    }

    public void addReturn(String name, PrismObject<? extends ObjectType> value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, ObjectType value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, Boolean value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, Long value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, Integer value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    @SuppressWarnings("unchecked")
    public void addReturn(String name, Class<?> value) {
        if (recordingValues) {
            if (value != null && ObjectType.class.isAssignableFrom(value)) {
                getReturns().put(name, collectionize(ObjectTypes.getObjectType((Class<? extends ObjectType>) value).getObjectTypeUri()));
            } else {
                getReturns().put(name, collectionize(stringify(value)));
            }
        }
    }

    public void addReturn(String name, QName value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(value == null ? null : QNameUtil.qNameToUri(value)));
        }
    }

    public void addReturn(String name, PolyString value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(value == null ? null : value.getOrig()));
        }
    }

    public void addReturn(String name, ObjectQuery value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, ObjectDelta<?> value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addReturn(String name, String... values) {
        if (recordingValues) {
            getReturns().put(name, collectionize(values));
        }
    }

    public void addArbitraryObjectAsReturn(String name, Object value) {
        if (recordingValues) {
            getReturns().put(name, collectionize(stringify(value)));
        }
    }

    public void addArbitraryObjectCollectionAsReturn(String paramName, Collection<?> paramValue) {
        if (recordingValues) {
            getReturns().put(paramName, stringifyCol(paramValue));
        }
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
        for (Object value : values) {
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
     * will be key for translation in admin-gui.
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
     * null.
     */
    public Throwable getCause() {
        return cause;
    }

    public void recordSuccess() {
        recordEnd();
        setSuccess();
    }

    public void setSuccess() {
        // Success, no message or other explanation is needed.
        status = OperationResultStatus.SUCCESS;
    }

    public void recordInProgress() {
        recordEnd();
        setInProgress();
    }

    public void setInProgress() {
        status = OperationResultStatus.IN_PROGRESS;
    }

    public void setInProgress(String message) {
        setInProgress();
        setMessage(message);
    }

    public void setUnknown() {
        status = OperationResultStatus.UNKNOWN;
    }

    public void recordFatalError(Throwable cause) {
        recordStatus(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
    }

    /**
     * A more sophisticated replacement for {@link #recordFatalError(String, Throwable)}.
     *
     * . Takes care not to overwrite the exception if it was already processed.
     * . Marks the exception as processed.
     * . Sets the appropriate result status.
     *
     * See the class javadoc.
     */
    public void recordException(String message, @NotNull Throwable cause) {
        if (!exceptionRecorded) {
            recordStatus(
                    OperationResultStatus.forThrowable(cause),
                    message,
                    cause);
            markExceptionRecorded();
        }
    }

    /** Convenience version of {@link #recordException(String, Throwable)} (with no custom message). */
    public void recordException(@NotNull Throwable cause) {
        recordException(cause.getMessage(), cause);
    }

    /** As {@link #recordException(String, Throwable)} but does not mark operation as finished. */
    public void recordExceptionNotFinish(String message, @NotNull Throwable cause) {
        if (!exceptionRecorded) {
            setStatus(
                    OperationResultStatus.forThrowable(cause),
                    message,
                    cause);
            markExceptionRecorded();
        }
    }

    /** Convenience version of {@link #recordExceptionNotFinish(String, Throwable)} (with no custom message). */
    public void recordExceptionNotFinish(@NotNull Throwable cause) {
        recordExceptionNotFinish(cause.getMessage(), cause);
    }

    /**
     * Marks the current exception (that is expected to be throws outside the context of the current operation)
     * as already processed - so no further actions (besides closing the result) are necessary.
     *
     * See also the class javadoc.
     *
     * @see #unmarkExceptionRecorded()
     */
    @Experimental
    public void markExceptionRecorded() {
        exceptionRecorded = true;
    }

    /**
     * "Un-marks" the exception as being recorded. To be used when the code decides e.g. that the exception will not
     * be thrown out of the context of the current operation.
     */
    @SuppressWarnings({ "unused", "WeakerAccess" }) // Waiting to be used
    public void unmarkExceptionRecorded() {
        exceptionRecorded = false;
    }

    public void setFatalError(Throwable cause) {
        setStatus(OperationResultStatus.FATAL_ERROR, cause.getMessage(), cause);
    }

    public void setFatalError(String message, Throwable cause) {
        setStatus(OperationResultStatus.FATAL_ERROR, message, cause);
    }

    /**
     * If the operation is an error then it will switch the status to HANDLED_ERROR.
     * This is used if the error is expected and properly handled.
     */
    public void muteError() {
        if (isError()) {
            status = OperationResultStatus.HANDLED_ERROR;
        }
    }

    public void muteAllSubresultErrors() {
        for (OperationResult subresult : getSubresults()) {
            subresult.muteError();
        }
    }

    public void muteLastSubresultError() {
        OperationResult lastSubresult = getLastSubresult();
        if (lastSubresult != null) {
            lastSubresult.muteError();
        }
    }

    // Temporary solution
    public void clearLastSubresultError() {
        OperationResult lastSubresult = getLastSubresult();
        if (lastSubresult != null && (lastSubresult.isError() || lastSubresult.isHandledError())) {
            lastSubresult.status = OperationResultStatus.SUCCESS;
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

    public void recordWarningNotFinish(String message, Throwable cause) {
        setStatus(OperationResultStatus.WARNING, message, cause);
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
        setStatus(status, message, cause);
    }

    private void setStatus(OperationResultStatus status, String message, Throwable cause) {
        this.status = status;
        this.message = message;
        this.cause = cause;
        setUserFriendlyMessage(cause);
    }

    private void setUserFriendlyMessage(Throwable cause) {
        if (cause instanceof CommonException) {
            setUserFriendlyMessage(((CommonException) cause).getUserFriendlyMessage());
        }
    }

    public void setPropagateHandledErrorAsSuccess(boolean propagateHandledErrorAsSuccess) {
        this.propagateHandledErrorAsSuccess = propagateHandledErrorAsSuccess;
    }

    public void recordFatalError(String message) {
        recordStatus(OperationResultStatus.FATAL_ERROR, message);
    }

    public void recordPartialError(String message) {
        recordStatus(OperationResultStatus.PARTIAL_ERROR, message);
    }

    public void setPartialError(String message) {
        setStatus(OperationResultStatus.PARTIAL_ERROR, message, null);
    }

    public void recordWarning(String message) {
        recordStatus(OperationResultStatus.WARNING, message);
    }

    /**
     * Records result from a common exception type. This automatically
     * determines status and also sets appropriate message.
     *
     * @param exception common exception
     */
    public void record(CommonException exception) {
        // TODO: switch to a localized message later
        // Exception is a fatal error in this context
        recordFatalError(exception.getErrorTypeMessage(), exception);
    }

    public void recordStatus(OperationResultStatus status) {
        recordStatus(status, (String) null);
    }

    public void recordStatus(OperationResultStatus status, String message) {
        recordEnd();
        this.status = status;
        this.message = message;
    }

    /**
     * Returns true if result status is UNKNOWN or any of the subresult status
     * is unknown (recursive).
     * <p>
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
    public static OperationResult createOperationResult(OperationResultType bean) {
        if (bean == null) {
            return null;
        }

        Map<String, Collection<String>> params = ParamsTypeUtil.fromParamsType(bean.getParams());
        Map<String, Collection<String>> context = ParamsTypeUtil.fromParamsType(bean.getContext());
        Map<String, Collection<String>> returns = ParamsTypeUtil.fromParamsType(bean.getReturns());

        List<OperationResult> subresults = null;
        if (!bean.getPartialResults().isEmpty()) {
            subresults = new ArrayList<>();
            for (OperationResultType subResult : bean.getPartialResults()) {
                subresults.add(createOperationResult(subResult));
            }
        }

        LocalizableMessage localizableMessage = null;
        LocalizableMessageType message = bean.getUserFriendlyMessage();
        if (message != null) {
            localizableMessage = LocalizationUtil.toLocalizableMessage(message);
        }

        OperationResult result = new OperationResult(bean.getOperation(), params, context, returns,
                OperationResultStatus.parseStatusType(bean.getStatus()), or0(bean.getToken()),
                bean.getMessageCode(), bean.getMessage(), localizableMessage, null,
                subresults);
        result.operationKind(bean.getOperationKind());
        result.getQualifiers().addAll(bean.getQualifier());
        result.setImportance(bean.getImportance());
        result.setAsynchronousOperationReference(bean.getAsynchronousOperationReference());
        if (bean.getCount() != null) {
            result.setCount(bean.getCount());
        }
        if (bean.getHiddenRecordsCount() != null) {
            result.setHiddenRecordsCount(bean.getHiddenRecordsCount());
        }
        if (bean.getStart() != null) {
            result.setStartFromNullable(XmlTypeConverter.toMillis(bean.getStart()));
        }
        if (bean.getEnd() != null) {
            result.setEndFromNullable(XmlTypeConverter.toMillis(bean.getEnd()));
        }
        result.setMicrosecondsFromNullable(bean.getMicroseconds());
        result.setOwnMicrosecondsFromNullable(bean.getOwnMicroseconds());
        result.setCpuMicrosecondsFromNullable(bean.getCpuMicroseconds());
        result.setInvocationIdFromNullable(bean.getInvocationId());
        result.logSegments.addAll(bean.getLog());
        result.setMonitoredOperations(bean.getMonitoredOperations());
        return result;
    }

    /**
     * As {@link #createOperationResultType()} but does not export minor success entries. This is needed to reduce
     * the size of e.g. {@link ShadowType} objects with fetchResult that includes full traced clockwork processing.
     */
    public OperationResultType createBeanReduced() {
        return createOperationResultBean(this, null, BeanContent.REDUCED);
    }

    /**
     * As {@link #createOperationResultType()} but exports only the root result.
     */
    public OperationResultType createBeanRootOnly() {
        return createOperationResultBean(this, null, BeanContent.ROOT_ONLY);
    }

    public @NotNull OperationResultType createOperationResultType() {
        return createOperationResultType(null);
    }

    public @NotNull OperationResultType createOperationResultType(Function<LocalizableMessage, String> resolveKeys) {
        return createOperationResultBean(this, resolveKeys, BeanContent.FULL);
    }

    private static @NotNull OperationResultType createOperationResultBean(
            @NotNull OperationResult opResult,
            @Nullable Function<LocalizableMessage, String> resolveKeys,
            @NotNull BeanContent beanContent) {
        OperationResultType bean = new OperationResultType();
        bean.setOperationKind(opResult.getOperationKind());
        bean.setToken(opResult.getToken());
        bean.setStatus(OperationResultStatus.createStatusType(opResult.getStatus()));
        bean.setImportance(opResult.getImportance());
        if (opResult.getCount() != 1) {
            bean.setCount(opResult.getCount());
        }
        if (opResult.getHiddenRecordsCount() != 0) {
            bean.setHiddenRecordsCount(opResult.getHiddenRecordsCount());
        }
        bean.setOperation(opResult.getOperation());
        bean.getQualifier().addAll(opResult.getQualifiers());
        bean.setMessage(opResult.getMessage());
        bean.setMessageCode(opResult.getMessageCode());

        if (opResult.getCause() != null || !opResult.details.isEmpty()) {
            StringBuilder detailSb = new StringBuilder();

            // Record text messages in details (if present)
            if (!opResult.details.isEmpty()) {
                for (String line : opResult.details) {
                    detailSb.append(line);
                    detailSb.append("\n");
                }
            }

            // Record stack trace in details if a cause is present
            if (opResult.getCause() != null) {
                Throwable ex = opResult.getCause();
                detailSb.append(ex.getClass().getName());
                detailSb.append(": ");
                detailSb.append(ex.getMessage());
                detailSb.append("\n");
                StackTraceElement[] stackTrace = ex.getStackTrace();
                for (StackTraceElement aStackTrace : stackTrace) {
                    detailSb.append(aStackTrace.toString());
                    detailSb.append("\n");
                }
            }

            bean.setDetails(detailSb.toString());
        }

        if (opResult.getUserFriendlyMessage() != null) {
            LocalizableMessageType msg =
                    LocalizationUtil.createLocalizableMessageType(
                            opResult.getUserFriendlyMessage(), resolveKeys);
            bean.setUserFriendlyMessage(msg);
        }

        bean.setParams(opResult.getParamsBean());
        bean.setContext(opResult.getContextBean());
        bean.setReturns(opResult.getReturnsBean());

        if (beanContent != BeanContent.ROOT_ONLY) {
            for (OperationResult subResult : opResult.getSubresults()) {
                if (beanContent == BeanContent.REDUCED && subResult.isMinor() && subResult.isSuccess()) {
                    continue;
                }
                bean.getPartialResults().add(
                        createOperationResultBean(subResult, resolveKeys, beanContent));
            }
        }

        bean.setAsynchronousOperationReference(opResult.getAsynchronousOperationReference());

        bean.setStart(XmlTypeConverter.createXMLGregorianCalendar(toNullable(opResult.start)));
        bean.setEnd(XmlTypeConverter.createXMLGregorianCalendar(toNullable(opResult.end)));
        bean.setMicroseconds(opResult.getMicroseconds());
        bean.setOwnMicroseconds(opResult.getOwnMicroseconds());
        bean.setCpuMicroseconds(opResult.getCpuMicroseconds());
        bean.setInvocationId(opResult.getInvocationId());
        bean.getLog().addAll(opResult.logSegments); // consider cloning here
        bean.getTrace().addAll(opResult.traces); // consider cloning here
        bean.setMonitoredOperations(cloneCloneable(opResult.getMonitoredOperations()));
        return bean;
    }

    public void summarize() {
        summarize(false);
    }

    public void summarize(boolean alsoSubresults) {

        if (!canBeCleanedUp()) {
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
            if (!subresult.canBeCleanedUp()) {
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
        //
        // TODO consider tracing here
        Map<OperationStatusKey, OperationStatusCounter> recordsCounters = new HashMap<>();
        iterator = getSubresults().iterator();
        while (iterator.hasNext()) {
            OperationResult sr = iterator.next();
            OperationStatusKey key = new OperationStatusKey(sr.getOperation(), sr.getStatus());
            if (recordsCounters.containsKey(key)) {
                OperationStatusCounter counter = recordsCounters.get(key);
                if (sr.representsHiddenRecords()) {
                    counter.hiddenCount += sr.hiddenRecordsCount;
                    counter.addHiddenMicroseconds(sr.microseconds, sr.ownMicroseconds, sr.cpuMicroseconds);
                    iterator.remove(); // will be re-added at the end (potentially with records counters)
                } else {
                    if (counter.shownRecords < subresultStripThreshold) {
                        counter.shownRecords++;
                        counter.shownCount += sr.count;
                    } else {
                        counter.hiddenCount += sr.count;
                        counter.addHiddenMicroseconds(sr.microseconds, sr.ownMicroseconds, sr.cpuMicroseconds);
                        iterator.remove();
                    }
                }
            } else {
                OperationStatusCounter counter = new OperationStatusCounter();
                if (sr.representsHiddenRecords()) {
                    counter.hiddenCount = sr.hiddenRecordsCount;
                    counter.addHiddenMicroseconds(sr.microseconds, sr.ownMicroseconds, sr.cpuMicroseconds);
                    iterator.remove(); // will be re-added at the end (potentially with records counters)
                } else {
                    counter.shownRecords = 1;
                    counter.shownCount = sr.count;
                }
                recordsCounters.put(key, counter);
            }
        }
        for (Map.Entry<OperationStatusKey, OperationStatusCounter> repeatingEntry : recordsCounters.entrySet()) {
            OperationStatusCounter value = repeatingEntry.getValue();
            int shownCount = value.shownCount;
            int hiddenCount = value.hiddenCount;
            if (hiddenCount > 0) {
                OperationStatusKey key = repeatingEntry.getKey();
                OperationResult hiddenRecordsEntry = new OperationResult(
                        key.operation, key.status,
                        "%d record(s) were hidden to save space. Total number of records: %d".formatted(
                                hiddenCount, shownCount + hiddenCount));
                hiddenRecordsEntry.microseconds = value.hiddenMicroseconds;
                hiddenRecordsEntry.ownMicroseconds = value.hiddenOwnMicroseconds;
                hiddenRecordsEntry.cpuMicroseconds = value.hiddenCpuMicroseconds;
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
        target.microseconds = addMicroseconds(target.microseconds, source.microseconds);
        target.ownMicroseconds = addMicroseconds(target.ownMicroseconds, source.ownMicroseconds);
        target.cpuMicroseconds = addMicroseconds(target.cpuMicroseconds, source.cpuMicroseconds);
    }

    private static @CanBeNone long addMicroseconds(@CanBeNone long a, @CanBeNone long b) {
        return a != NONE_LONG || b != NONE_LONG ? zeroIfNone(a) + zeroIfNone(b) : NONE_LONG;
    }

    private void mergeMap(Map<String, Collection<String>> targetMap, Map<String, Collection<String>> sourceMap) {
        for (Entry<String, Collection<String>> targetEntry : targetMap.entrySet()) {
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
        for (Entry<String, Collection<String>> sourceEntry : sourceMap.entrySet()) {
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
        for (OperationResult sub : getSubresults()) {
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
     * As {@link #cleanupResult(Throwable)} but uses the recorded exception for diagnostics. It is more convenient than that
     * method, as it can be used in the `finally` block - assuming that the exception was recorded in the `catch` block
     * or earlier.
     */
    public void cleanup() {
        cleanupInternal(cause);
    }

    /**
     * Removes all the successful minor results. Also checks if the result is roughly consistent
     * and complete. (e.g. does not have unknown operation status, etc.)
     */
    @Deprecated // use cleanup()
    public void cleanupResult() {
        cleanupInternal(null);
    }

    /**
     * Removes all the successful minor results. Also checks if the result is roughly consistent
     * and complete. (e.g. does not have unknown operation status, etc.)
     * <p>
     * The argument "e" is for easier use of the cleanup in the exceptions handlers. The original exception is passed
     * to the IAE that this method produces for easier debugging.
     */
    @Deprecated // use cleanup() with recordException()
    public void cleanupResult(Throwable e) {
        cleanupInternal(e);
    }

    private void cleanupInternal(Throwable e) {
        if (!canBeCleanedUp()) {
            return; // TEMPORARY fixme
        }

        OperationResultImportanceType preserveDuringCleanup = getPreserveDuringCleanup();

        if (status == OperationResultStatus.UNKNOWN) {
            IllegalStateException illegalStateException = new IllegalStateException("Attempt to cleanup result of operation " + operation + " that is still UNKNOWN");
            LOGGER.error("Attempt to cleanup result of operation " + operation + " that is still UNKNOWN:\n{}", this.debugDump(), illegalStateException);
            throw illegalStateException;
        }
        if (subresults == null) {
            return;
        }
        Iterator<OperationResult> iterator = subresults.iterator();
        while (iterator.hasNext()) {
            OperationResult subresult = iterator.next();
            if (subresult.getStatus() == OperationResultStatus.UNKNOWN) {
                String message = "Subresult " + subresult.getOperation() + " of operation " + operation + " is still UNKNOWN during cleanup";
                LOGGER.error("{}:\n{}", message, this.debugDump(), e);
                if (e == null) {
                    throw new IllegalStateException(message);
                } else {
                    throw new IllegalStateException(message + "; during handling of exception " + e, e);
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

    // TODO better name
    private boolean canBeCleanedUp() {
        return !isTraced() && !preserve;
    }

    /**
     * @return true if x < y
     */
    @SuppressWarnings("WeakerAccess")
    public static boolean isLesserThan(OperationResultImportanceType x, @NotNull OperationResultImportanceType y) {
        switch (y) {
            case MAJOR:
                return x != MAJOR;
            case NORMAL:
                return x == MINOR;
            case MINOR:
                return false;
            default:
                throw new IllegalArgumentException("importance: " + y);
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

        if (DebugUtil.isDetailedDebugDump()) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(
                    sb, "duration",
                    "%,.3f ms (%,.3f own)".formatted(
                            formatMilliseconds(microseconds), formatMilliseconds(ownMicroseconds)),
                    indent + 2);
        }

        sb.append("\n");

        for (Map.Entry<String, Collection<String>> entry : getParams().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[p]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getContext().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[c]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getReturns().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[r]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }

        for (String line : details) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[d]");
            sb.append(line);
            sb.append("\n");
        }

        if (cause != null) {
            DebugUtil.dumpThrowable(sb, "[cause]", cause, indent + 2, printStackTrace);
        }

        for (OperationResult sub : getSubresults()) {
            sub.dumpIndent(sb, indent + 1, printStackTrace);
        }
    }

    private Float formatMilliseconds(long value) {
        return value != NONE_LONG ? value / 1000.0f : null;
    }

    @Experimental
    public void dumpBasicInfo(StringBuilder sb, String prefix, int indent) {
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(prefix);
        sb.append(operation);
        sb.append(" (").append(status).append(")");
        if (importance == MINOR) {
            sb.append(", MINOR");
        } else if (importance == MAJOR) {
            sb.append(", MAJOR");
        }
        if (count > 1) {
            sb.append(" x");
            sb.append(count);
        }
        sb.append("\n");

        for (Map.Entry<String, Collection<String>> entry : getParams().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[p]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getContext().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[c]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }

        for (Map.Entry<String, Collection<String>> entry : getReturns().entrySet()) {
            DebugUtil.indentDebugDump(sb, indent + 2);
            sb.append("[r]");
            sb.append(entry.getKey());
            sb.append("=");
            sb.append(dumpEntry(entry.getValue()));
            sb.append("\n");
        }
    }

    private String dumpEntry(Collection<String> values) {
        if (values == null) {
            return null;
        }
        if (values.isEmpty()) {
            return "(empty)";
        }
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return values.toString();
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
    }

    public void setCaseOid(String oid) {
        setAsynchronousOperationReference(CASE_OID_PREFIX + oid);
    }

    @Override
    public OperationResult operationKind(OperationKindType value) {
        this.operationKind = value;
        return this;
    }

    public OperationKindType getOperationKind() {
        return operationKind;
    }

    @Override
    public OperationResultBuilder preserve() {
        this.preserve = true;
        this.recordingValues = true;
        return this;
    }

    public boolean isPreserve() {
        return preserve;
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

    private record OperationStatusKey(String operation, OperationResultStatus status) {

    }

    private static class OperationStatusCounter {

        /** How many actual records will be shown (after this wave of stripping) */
        private int shownRecords;

        /** How many entries will be shown (after this wave of stripping) */
        private int shownCount;

        /** How many entries will be hidden (after this wave of stripping) */
        private int hiddenCount;

        /** How many microseconds are in the hidden entries? */
        @CanBeNone private long hiddenMicroseconds = NONE_LONG;

        /** How many own microseconds are in the hidden entries? */
        @CanBeNone private long hiddenOwnMicroseconds = NONE_LONG;

        /** How many CPU microseconds are in the hidden entries? */
        @CanBeNone private long hiddenCpuMicroseconds = NONE_LONG;

        void addHiddenMicroseconds(
                @CanBeNone long deltaMicroseconds, @CanBeNone long deltaOwnMicroseconds, @CanBeNone long deltaCpuMicroseconds) {
            hiddenMicroseconds = addMicroseconds(hiddenMicroseconds, deltaMicroseconds);
            hiddenOwnMicroseconds = addMicroseconds(hiddenOwnMicroseconds, deltaOwnMicroseconds);
            hiddenCpuMicroseconds = addMicroseconds(hiddenCpuMicroseconds, deltaCpuMicroseconds);
        }
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public OperationResult clone() {
        return clone(null, true);
    }

    public OperationResult clone(Integer maxDepth, boolean full) {
        OperationResult clone = new OperationResult(operation);

        clone.operationKind = operationKind;
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
        clone.cpuMicroseconds = cpuMicroseconds;
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
        return Objects.requireNonNullElse(
                LOCAL_HANDLING_STRATEGY.get(),
                globalHandlingStrategy);
    }

    private static int getSubresultStripThreshold() {
        return defaultIfNull(getCurrentHandlingStrategy().getSubresultStripThreshold(), DEFAULT_SUBRESULT_STRIP_THRESHOLD);
    }

    @NotNull
    private static OperationResultImportanceType getPreserveDuringCleanup() {
        return defaultIfNull(getCurrentHandlingStrategy().getPreserveDuringCleanup(), NORMAL);
    }

    public static void applyOperationResultHandlingStrategy(
            @NotNull List<OperationResultHandlingStrategyType> configuredStrategies) {
        if (!configuredStrategies.isEmpty()) {
            handlingStrategies = CloneUtil.cloneCollectionMembers(configuredStrategies);
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
        if (this == o) {
            return true;
        }
        if (!(o instanceof OperationResult)) {
            return false;
        }
        OperationResult result = (OperationResult) o;
        return token == result.token &&
                count == result.count &&
                hiddenRecordsCount == result.hiddenRecordsCount &&
                summarizeErrors == result.summarizeErrors &&
                summarizePartialErrors == result.summarizePartialErrors &&
                summarizeSuccesses == result.summarizeSuccesses &&
                importance == result.importance &&
                building == result.building &&
                start == result.start &&
                end == result.end &&
                microseconds == result.microseconds &&
                cpuMicroseconds == result.cpuMicroseconds &&
                invocationId == result.invocationId &&
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
        return Objects.hash(
                operation, qualifiers, status, params, context, returns, token, messageCode,
                message, userFriendlyMessage, cause, count, hiddenRecordsCount, subresults, details,
                summarizeErrors, summarizePartialErrors, summarizeSuccesses, building, start, end,
                microseconds, cpuMicroseconds, invocationId, traces, asynchronousOperationReference);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getStart() {
        return toNullable(start);
    }

    private void setStartFromNullable(Long start) {
        this.start = fromNullable(start);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getEnd() {
        return toNullable(end);
    }

    private void setEndFromNullable(Long end) {
        this.end = fromNullable(end);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getMicroseconds() {
        return toNullable(microseconds);
    }

    private void setMicrosecondsFromNullable(Long microseconds) {
        this.microseconds = fromNullable(microseconds);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getOwnMicroseconds() {
        return toNullable(ownMicroseconds);
    }

    private void setOwnMicrosecondsFromNullable(Long ownMicroseconds) {
        this.ownMicroseconds = fromNullable(ownMicroseconds);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getCpuMicroseconds() {
        return toNullable(cpuMicroseconds);
    }

    private void setCpuMicrosecondsFromNullable(Long cpuMicroseconds) {
        this.cpuMicroseconds = fromNullable(cpuMicroseconds);
    }

    /** This is public API; we should not use {@link NoValueUtil#NONE_LONG} here. */
    public Long getInvocationId() {
        return toNullable(invocationId);
    }

    private void setInvocationIdFromNullable(Long invocationId) {
        this.invocationId = fromNullable(invocationId);
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

    public MonitoredOperationsStatisticsType getMonitoredOperations() {
        return monitoredOperations;
    }

    public void setMonitoredOperations(MonitoredOperationsStatisticsType operations) {
        this.monitoredOperations = operations;
    }

    public @NotNull List<String> getQualifiers() {
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

    /** What should be serialized into {@link OperationResultType} bean? */
    private enum BeanContent {

        /** Everything. */
        FULL,

        /** Everything except for minor success children. */
        REDUCED,

        /** No children. */
        ROOT_ONLY
    }
}

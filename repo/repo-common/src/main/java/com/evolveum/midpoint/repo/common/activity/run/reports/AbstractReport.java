/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.reports;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;

import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.task.api.RunningTask;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.run.state.CurrentActivityState;
import com.evolveum.midpoint.repo.common.reports.ReportSupportUtil;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public abstract class AbstractReport {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractReport.class);

    /** The state of this object. */
    @NotNull private State state = State.CLOSED;

    /** The report data object to which the data is being written. Non-null if open. */
    private ReportDataType reportData;

    /** Writer of the data. Non-null if open. */
    private CsvWriter csvWriter;

    /** Report definition. Must be non-null if the report is to be really used. */
    private final AbstractActivityReportDefinitionType definition;

    /** State of the current activity. References to reports are stored there. */
    @NotNull private final CurrentActivityState<?> activityState;

    /** Useful beans. */
    @NotNull private final CommonTaskBeans beans;

    /** Definition of records to be written. */
    @NotNull private final ComplexTypeDefinition recordDefinition;

    /** What record items (~ columns) should be included in the report? Empty means all. */
    @NotNull private final Collection<ItemName> itemsIncluded;

    /** Parsed filter to decide if individual records should be accepted or rejected. */
    @Nullable private final ObjectFilter recordFilter;

    /** Expression that decides if individual records should be accepted or rejected. */
    @Nullable private final ExpressionType recordFilteringExpression;

    AbstractReport(@Nullable AbstractActivityReportDefinitionType definition,
            @NotNull QName recordTypeName, @NotNull CurrentActivityState<?> activityState) {
        this(definition, recordTypeName, activityState, List.of());
    }

    AbstractReport(@Nullable AbstractActivityReportDefinitionType definition, @NotNull QName recordTypeName,
            @NotNull CurrentActivityState<?> activityState,
            @NotNull Collection<ItemName> itemsIncluded) {
        this.definition = definition;
        this.activityState = activityState;
        this.beans = activityState.getActivityRun().getBeans();
        this.recordDefinition =
                MiscUtil.requireNonNull(
                        PrismContext.get().getSchemaRegistry()
                                .findComplexTypeDefinitionByType(recordTypeName),
                        () -> new IllegalStateException("No definition for " + recordTypeName));
        this.itemsIncluded = itemsIncluded;
        this.recordFilter = parseRecordFilter();
        this.recordFilteringExpression = definition != null ? definition.getRecordFilteringExpression() : null;
    }

    private ObjectFilter parseRecordFilter() {
        SearchFilterType filterBean = definition != null ? definition.getRecordFilter() : null;
        if (filterBean != null) {
            PrismContext prismContext = PrismContext.get();
            // This fake PCD creation can be removed after query converter will stop requiring it.
            PrismContainerDefinition<?> fakePcd = prismContext.definitionFactory()
                    .createContainerDefinition(new QName("dummy"), recordDefinition);
            try {
                return prismContext.getQueryConverter().parseFilter(filterBean, fakePcd);
            } catch (SchemaException e) {
                throw new SystemException("Couldn't parse record filter: " + e.getMessage(), e);
            }
        } else {
            return null;
        }
    }

    public boolean isEnabled() {
        return definition != null && !Boolean.FALSE.equals(definition.isEnabled());
    }

    void openIfClosed(OperationResult result) {
        if (isClosed()) {
            initialize(result);
        }
        checkConsistence();
    }

    /**
     * Creates a new report data object or continues writing existing one.
     */
    private void initialize(OperationResult result) {
        try {
            initializeReportDataObject(result);
            openReportDataWriter();
            setOpen();
        } catch (Exception e) {
            // We could be not this harsh. However, in such case we would not know about reporting issues.
            throw new SystemException("Report could not be initialized: " + e.getMessage(), e);
        }
    }

    private void openReportDataWriter() throws IOException {
        if (isError()) {
            return;
        }
        File file =
                new File(
                        MiscUtil.requireNonNull(
                                reportData.getFilePath(),
                                () -> new IllegalStateException("No file path in " + reportData)));

        boolean exists = file.exists();
        csvWriter =
                new CsvWriter(
                        new PrintWriter(
                                new FileWriter(file, StandardCharsets.UTF_8, true)),
                        recordDefinition, itemsIncluded);

        if (!exists) {
            csvWriter.writeHeader();
        }
        LOGGER.debug("Opened report file {}", reportData.getFilePath());
    }

    private void initializeReportDataObject(OperationResult result) throws CommonException, ActivityRunException {
        for (;;) {
            String existingDataOid = findCurrentNodeDataOid();
            if (existingDataOid == null) {
                createAndLinkReportDataObject(result);
                break;
            } else {
                LOGGER.trace("Found existing report data object reference: {}", existingDataOid);
                try {
                    reportData = beans.repositoryService.getObject(ReportDataType.class, existingDataOid, null, result)
                            .asObjectable();
                    break;
                } catch (ObjectNotFoundException e) {
                    LOGGER.warn("Report data object {} referenced in the activity does not exist", existingDataOid);
                    LOGGER.trace("Will delete hanging reference and try to find another one");
                    deleteReference(existingDataOid, result);
                    // continuing
                } catch (Exception e) {
                    // Or should we throw an exception here?
                    LOGGER.warn("Couldn't retrieve report data object {} referenced in the activity. It will not be updated.",
                            existingDataOid);
                    setError();
                    return;
                }
            }
        }
    }

    private void createAndLinkReportDataObject(OperationResult result) throws CommonException, ActivityRunException {
        String bareFileName = createFileName();
        File filePath = new File(ReportSupportUtil.getExportDir(), bareFileName + ".csv");

        ReportDataType reportDataObject = createReportDataObject(bareFileName, filePath, result);
        linkReportDataObject(reportDataObject, result);

        LOGGER.info("Created and linked report data object for {}: {}", filePath, reportDataObject.getOid());
        reportData = reportDataObject;
    }

    @NotNull
    private ReportDataType createReportDataObject(String bareFileName, File filePath, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        ReportDataType reportDataObject = new ReportDataType()
                .name(bareFileName)
                .fileFormat(FileFormatTypeType.CSV)
                //.archetypeRef(SystemObjectsType.ARCHETYPE_TRACE.value(), ArchetypeType.COMPLEX_TYPE) // TODO archetype
                .filePath(filePath.getAbsolutePath())
                .nodeRef(ObjectTypeUtil.createObjectRef(beans.taskManager.getLocalNode()));
        beans.repositoryService.addObject(reportDataObject.asPrismObject(), null, result);
        return reportDataObject;
    }

    private void linkReportDataObject(ReportDataType reportDataObject, OperationResult result)
            throws ActivityRunException {
        activityState.addDeleteItemRealValues(
                getStateItemPath().append(ActivityReportCollectionType.F_RAW_DATA_REF),
                List.of(new ObjectReferenceType()
                        .oid(reportDataObject.getOid())
                        .type(ReportDataType.COMPLEX_TYPE)
                        .description(getLocalNodeId())),
                List.of());
        activityState.flushPendingTaskModificationsChecked(result);
    }

    private @NotNull String getLocalNodeId() {
        return beans.taskManager.getNodeId();
    }

    private String createFileName() {
        String activityPath = String.join(".", activityState.getActivityPath().getIdentifiers());
        String taskOid = activityState.getActivityRun().getRunningTask().getOid();
        return MiscUtil.fixFileName(
                String.format("%s-for-%s-in-%s-on-%s-%d",
                        getReportType(),
                        activityPath.isEmpty() ? "root" : activityPath,
                        taskOid,
                        getLocalNodeId(),
                        System.currentTimeMillis()));
    }

    abstract String getReportType();

    /** Deletes a reference(s) to given report data object from the activity state. */
    private void deleteReference(@NotNull String oid, OperationResult result) throws ActivityRunException {
        ActivityReportCollectionType collection = getReportsCollection();
        if (collection != null) {
            List<ObjectReferenceType> references = collection.getRawDataRef().stream()
                    .filter(ref -> oid.equals(ref.getOid()))
                    .collect(Collectors.toList());
            if (!references.isEmpty()) {
                activityState.addDeleteItemRealValues(
                        getStateItemPath().append(ActivityReportCollectionType.F_RAW_DATA_REF),
                        List.of(),
                        CloneUtil.cloneCollectionMembers(references)
                );
                activityState.flushPendingTaskModificationsChecked(result);
            }
        }
    }

    private String findCurrentNodeDataOid() {
        ActivityReportCollectionType collection = getReportsCollection();
        if (collection == null) {
            return null;
        } else {
            String localNodeId = getLocalNodeId();
            return collection.getRawDataRef().stream()
                    .filter(ref -> localNodeId.equals(ref.getDescription()))
                    .map(ObjectReferenceType::getOid)
                    .findAny().orElse(null);
        }
    }

    private ActivityReportCollectionType getReportsCollection() {
        return activityState.getWorkStateItemRealValueClone(getStateItemPath(), ActivityReportCollectionType.class);
    }

    abstract @NotNull ItemPath getStateItemPath();

    private void setOpen() {
        state = State.OPEN;
    }

    private boolean isOpen() {
        return state == State.OPEN;
    }

    private void setClosed() {
        state = State.CLOSED;
    }

    private boolean isClosed() {
        return state == State.CLOSED;
    }

    private void setError() {
        state = State.ERROR;
    }

    private boolean isError() {
        return state == State.ERROR;
    }

    public void checkConsistence() {
        if (isOpen()) {
            stateCheck(reportData != null, "Initialized report without data object");
            stateCheck(csvWriter != null, "Initialized report without CSV writer");
        }
    }

    public synchronized void close() {
        if (isClosed()) {
            return;
        }

        try {
            if (csvWriter != null) {
                LOGGER.debug("Closing print writer for {}", reportData);
                csvWriter.close();
            }
        } finally {
            csvWriter = null;
            reportData = null;
            setClosed();
        }
    }

    void writeRecord(Containerable record) {
        if (isOpen()) {
            csvWriter.writeRecord(record);
        } else {
            LOGGER.warn("Couldn't write to the {} report because the state is {}", getReportType(), state);
        }
    }

    void writeRecords(List<? extends Containerable> records) {
        if (isOpen()) {
            records.forEach(r -> csvWriter.writeRecordNoFlush(r));
            csvWriter.flush();
        } else {
            LOGGER.warn("Couldn't write to the {} report because the state is {}", getReportType(), state);
        }
    }

    boolean isRejected(@NotNull Containerable record, @NotNull RunningTask task, @NotNull OperationResult result) {
        return isRejectedByFilter(record) || isRejectedByExpression(record, task, result);
    }

    private boolean isRejectedByExpression(@NotNull Containerable record, @NotNull RunningTask task,
            @NotNull OperationResult result) {
        if (recordFilteringExpression == null) {
            return false;
        }
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_RECORD, record, record.getClass());
        try {
            return !ExpressionUtil.evaluateConditionDefaultTrue(variables, recordFilteringExpression,
                    null, beans.expressionFactory, "record filtering expression", task, result);
        } catch (CommonException e) {
            // Maybe we could propagate the checked exception upwards (later!)
            throw new SystemException("Couldn't evaluate record filtering expression for " + record + ": " + e.getMessage(), e);
        }
    }

    private boolean isRejectedByFilter(@NotNull Containerable record) {
        try {
            return recordFilter != null &&
                    !recordFilter.match(record.asPrismContainerValue(), SchemaService.get().matchingRuleRegistry());
        } catch (SchemaException e) {
            // Maybe we could propagate the schema exception upwards (later!)
            throw new SystemException("Couldn't match record against a filter: " + record + " vs " + recordFilter +
                    ": " + e.getMessage(), e);
        }
    }

    private enum State {
        CLOSED, OPEN, ERROR
    }
}

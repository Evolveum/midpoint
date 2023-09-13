/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.util.BasicLightweightIdentifierGenerator;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.merger.SimpleObjectMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ObjectFactory;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class InitialObjectsAction extends Action<InitialObjectsOptions, ActionResult<InitialObjectsResult>> {

    private static final String INITIAL_OBJECTS_RESOURCE_PATTERN = "classpath*:/initial-objects/**/*.xml";

    private static final String OPERATION_UPDATE_OBJECTS = "Initial objects update";
    private static final String OPERATION_PROCESS_FILE = "Process file";

    @Override
    public String getOperationName() {
        return "initial objects";
    }

    @Override
    public LogTarget getLogTarget() {
        if (!options.isReport()) {
            return LogTarget.SYSTEM_OUT;
        }

        return options.getOutput() != null ? LogTarget.SYSTEM_OUT : LogTarget.SYSTEM_ERR;
    }

    @Override
    public ActionResult<InitialObjectsResult> execute() throws Exception {
        InitialObjectsResult actionResult = new InitialObjectsResult();

        Writer writer = null;
        try {
            if (options.isReport()) {
                writer = NinjaUtils.createWriter(
                        options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out);

                if (options.getReportStyle() == InitialObjectsOptions.ReportStyle.DELTA) {
                    writer.write(NinjaUtils.XML_DELTAS_PREFIX);
                } else {
                    writer.write(NinjaUtils.XML_OBJECTS_PREFIX);
                }
            }

            OperationResult result = new OperationResult(OPERATION_UPDATE_OBJECTS);

            List<Resource> resources = new ArrayList<>();
            List<File> files = options.getFiles();
            if (files != null && !files.isEmpty()) {
                for (File file : options.getFiles()) {
                    if (file.isDirectory()) {
                        FileUtils.listFiles(file, new String[] { "xml" }, true)
                                .forEach(f -> resources.add(new FileSystemResource(f)));
                    } else {
                        resources.add(new org.springframework.core.io.FileSystemResource(file));
                    }
                }
            } else {
                Resource[] array = new PathMatchingResourcePatternResolver().getResources(INITIAL_OBJECTS_RESOURCE_PATTERN);
                resources.addAll(Arrays.asList(array));
            }

            resources.sort(Comparator.comparing(Resource::getFilename));

            List<ObjectReferenceType> refs = new ArrayList<>();

            for (Resource resource : resources) {
                actionResult.incrementTotal();

                ObjectReferenceType ref = processFile(resource, result, actionResult, writer);
                if (ref != null) {
                    refs.add(ref);
                }
            }

            log.info("");
            if (!refs.isEmpty()) {
                PrismObject<TaskType> task = createRecomputeTask(refs);
                context.getRepository().addObject(task, null, result);
                log.info("Recompute task {} created, it will be started after midpoint starts and will recompute {} objects.", task, refs.size());
            } else {
                log.info("Recompute task not created, no objects were changed in repository.");
            }
        } finally {
            if (writer != null) {
                if (options.getReportStyle() == InitialObjectsOptions.ReportStyle.DELTA) {
                    writer.write(NinjaUtils.XML_DELTAS_SUFFIX);
                } else {
                    writer.write(NinjaUtils.XML_OBJECTS_SUFFIX);
                }

                writer.flush();

                if (options.getOutput() != null) {
                    // todo this should be handled better, not manually on multiple places
                    // we don't want to close stdout, e.g. only if we were writing to file
                    IOUtils.closeQuietly(writer);
                }
            }
        }

        int status = actionResult.getError() == 0 ? 0 : 1;

        log.info("");
        log.info(
                "Initial objects update finished. {}, {}, {} and {}, total: {} objects processed.",
                ConsoleFormat.formatMessageWithSuccessParameters("{} added", actionResult.getAdded()),
                ConsoleFormat.formatMessageWithInfoParameters("{} merged", actionResult.getMerged()),
                ConsoleFormat.formatMessageWithParameters(
                        "{} unchanged", new Object[] { actionResult.getUnchanged() }, ConsoleFormat.Color.DEFAULT),
                ConsoleFormat.formatMessageWithErrorParameters("{} errors", actionResult.getError()),
                actionResult.getTotal());

        return new ActionResult<>(actionResult, status);
    }

    private <O extends ObjectType> ObjectReferenceType processFile(
            Resource resource, OperationResult parentResult, InitialObjectsResult actionResult, Writer writer) {

        OperationResult result = parentResult.createSubresult(OPERATION_PROCESS_FILE);

        final PrismContext prismContext = context.getPrismContext();
        final RepositoryService repository = context.getRepository();

        log.debug("File: {}", resource.getFilename());

        try (InputStream is = resource.getInputStream()) {
            String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
            PrismObject<O> object = prismContext.parseObject(xml);

            PrismObject<O> existing = null;
            try {
                Class<O> type = object.getCompileTimeClass();

                GetOperationOptionsBuilder optionsBuilder = context.getSchemaService().getOperationOptionsBuilder();
                NinjaUtils.addIncludeOptionsForExport(optionsBuilder, type);

                existing = repository.getObject(type, object.getOid(), optionsBuilder.build(), result);
            } catch (ObjectNotFoundException ex) {
                // this is ok, object will be added, no merge needed
            }

            boolean changed = false;
            if (existing == null) {
                // we'll just import object, since it's new one
                changed = addObject(object, result, actionResult, writer, false);
            } else {
                if (options.isNoMerge()) {
                    if (!object.equivalent(existing)) {
                        changed = addObject(object, result, actionResult, writer, true);
                    } else {
                        log.info("Object {} unchanged, skipping add.", NinjaUtils.printObjectNameOidAndType(existing));
                        actionResult.incrementUnchanged();
                    }
                } else {
                    changed = mergeObject(object, existing, result, actionResult, writer);
                }
            }

            if (changed) {
                return new ObjectReferenceType()
                        .oid(object.getOid())
                        .type(object.getComplexTypeDefinition().getTypeName());
            }
        } catch (Exception ex) {
            log.error("Unexpected exception occurred processing file {}", ex, resource.getFilename());
            actionResult.incrementError();
        }

        return null;
    }

    /**
     * @return true if object was added/modified/merged in repository, false otherwise
     */
    private <O extends ObjectType> boolean mergeObject(
            PrismObject<O> initial, PrismObject<O> existing, OperationResult result, InitialObjectsResult actionResult, Writer writer)
            throws SchemaException, ConfigurationException, IOException {

        log.debug("Merging object {}", NinjaUtils.printObjectNameOidAndType(existing));

        final PrismObject<O> merged = existing.clone();

        boolean mergeExecuted = mergeObject(merged, initial);
        if (!mergeExecuted) {
            log.error("Skipping object update, merge operation not supported for object {}.", NinjaUtils.printObjectNameOidAndType(existing));
            actionResult.incrementError();
            return false;
        }

        ObjectDelta<O> delta = existing.diff(merged);
        if (delta.isEmpty()) {
            log.info("Skipping object update, object {} merged, no differences found.", NinjaUtils.printObjectNameOidAndType(existing));

            actionResult.incrementUnchanged();
            return false;
        }

        if (options.getReportStyle() == InitialObjectsOptions.ReportStyle.DELTA) {
            reportDelta(delta, writer);
        } else {
            reportObject(merged, writer);
        }

        boolean modified = false;
        try {
            log.info(
                    "Updating object {} in repository {}",
                    NinjaUtils.printObjectNameOidAndType(existing), options.isDryRun() ? "(dry run)" : "");

            if (!options.isDryRun()) {
                context.getRepository().modifyObject(delta.getObjectTypeClass(), delta.getOid(), delta.getModifications(), result);
                modified = true;
            }

            actionResult.incrementMerged();
        } catch (ObjectNotFoundException | ObjectAlreadyExistsException | SchemaException ex) {
            log.error(
                    "Couldn't modify object {} ({}, {})",
                    ex, existing.getName(), existing.getOid(), existing.toDebugType());

            actionResult.incrementError();
        }

        return modified;
    }

    /**
     * @return true if merge operation was executed, false otherwise
     */
    private <O extends ObjectType> boolean mergeObject(PrismObject<O> target, PrismObject<O> source)
            throws SchemaException, ConfigurationException {

        if (target.equivalent(source)) {
            return true;
        }

        if (!SimpleObjectMergeOperation.isMergeSupported(target)) {
            return false;
        }

        SimpleObjectMergeOperation.merge(target, source);
        return true;
    }

    private <O extends ObjectType> void reportObject(PrismObject<O> object, Writer writer) throws SchemaException, IOException {
        String xml = context.getPrismContext().xmlSerializer()
                .serialize(object.getValue(), SchemaConstantsGenerated.C_OBJECT);
        writer.write(xml);
    }

    private <O extends ObjectType> void reportAddDelta(PrismObject<O> object, Writer writer) throws SchemaException, IOException {
        if (options.getReportStyle() == InitialObjectsOptions.ReportStyle.FULL_OBJECT) {
            reportObject(object, writer);
            return;
        }

        ObjectDelta<O> delta = context.getPrismContext().deltaFactory()
                .object()
                .createEmptyAddDelta(object.getCompileTimeClass(), object.getOid());
        delta.setObjectToAdd(object);

        reportDelta(delta, writer);
    }

    private <O extends ObjectType> void reportDelta(ObjectDelta<O> delta, Writer writer) throws SchemaException, IOException {
        if (writer == null || !options.isReport()) {
            return;
        }

        ObjectDeltaType deltaType = DeltaConvertor.toObjectDeltaType(delta);
        String xml = context.getPrismContext().xmlSerializer()
                .serializeRealValue(deltaType, NinjaUtils.DELTA_LIST_DELTA);
        writer.write(xml);
    }

    /**
     * @return true if object was added, false if it was skipped
     */
    private <O extends ObjectType> boolean addObject(
            PrismObject<O> object, OperationResult result, InitialObjectsResult actionResult, Writer writer, boolean overwrite)
            throws SchemaException, IOException {

        if (!options.isForceAdd() && !overwrite) {
            log.info(
                    "Skipping object add (force-add options is not set), object {} will be correctly added during midpoint startup.",
                    NinjaUtils.printObjectNameOidAndType(object));
            return false;
        }

        reportAddDelta(object, writer);

        boolean added = false;
        try {
            log.info(
                    "Adding object {} {} to repository {}",
                    overwrite ? "(overwrite)" : "",
                    NinjaUtils.printObjectNameOidAndType(object), options.isDryRun() ? "(dry run)" : "");

            if (!options.isDryRun()) {
                Protector protector = context.getApplicationContext().getBean(Protector.class);
                CryptoUtil.encryptValues(protector, object);

                RepoAddOptions opts = overwrite ? RepoAddOptions.createOverwrite() : null;

                context.getRepository().addObject(object, opts, result);
                added = true;
            }

            actionResult.incrementAdded();
        } catch (ObjectAlreadyExistsException | SchemaException | EncryptionException ex) {
            log.error("Couldn't add object {} to repository", ex, NinjaUtils.printObjectNameOidAndType(object));

            actionResult.incrementError();
        }

        return added;
    }

    private PrismObject<TaskType> createRecomputeTask(List<ObjectReferenceType> refs) throws SchemaException {
        TaskType task = new TaskType();
        task.setOid(UUID.randomUUID().toString());
        task.setName(new PolyStringType("Initial objects recompute after upgrade to 4.8"));
        task.setExecutionState(TaskExecutionStateType.RUNNABLE);
        task.setTaskIdentifier(new BasicLightweightIdentifierGenerator().generate().toString());

        task.setOwnerRef(new ObjectReferenceType()
                .oid(SystemObjectsType.USER_ADMINISTRATOR.value())
                .type(UserType.COMPLEX_TYPE));

        ObjectReferenceType archetypeRef = new ObjectReferenceType()
                .oid(SystemObjectsType.ARCHETYPE_ITERATIVE_BULK_ACTION_TASK.value())
                .type(ArchetypeType.COMPLEX_TYPE);

        AssignmentType assignment = new AssignmentType()
                .targetRef(archetypeRef);
        task.getAssignment().add(assignment);

        task.getArchetypeRef().add(archetypeRef.clone());

        task.schedule(new ScheduleType().recurrence(TaskRecurrenceType.SINGLE));

        ObjectFilter filter = context.getPrismContext().queryFor(ObjectType.class)
                .id(refs.stream().map(ObjectReferenceType::getOid).toArray(String[]::new))
                .buildFilter();
        SearchFilterType searchFilter = context.getPrismContext().getQueryConverter().createSearchFilterType(filter);

        //@formatter:off
        IterativeScriptingWorkDefinitionType iterativeScripting = new IterativeScriptingWorkDefinitionType();
        iterativeScripting
                .beginObjects()
                    .type(ObjectType.COMPLEX_TYPE)
                    .query(new QueryType().filter(searchFilter))
                .<IterativeScriptingWorkDefinitionType>end()
                .beginScriptExecutionRequest()
                    .scriptingExpression(
                            new ObjectFactory()
                                    .createAction(
                                            new ActionExpressionType()
                                                .type("recompute")));

        task
                .beginActivity()
                    .beginWork()
                        .iterativeScripting(iterativeScripting)
                .<ActivityDefinitionType>end();
        //@formatter:on

        return task.asPrismObject();
    }
}

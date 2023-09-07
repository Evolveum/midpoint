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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.impl.LogTarget;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.merger.object.ObjectMergeOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

// todo action should write XML + maybe csv? for review
public class InitialObjectsAction extends Action<InitialObjectsOptions, ActionResult<InitialObjectsResult>> {

    private static final String INITIAL_OBJECTS_RESOURCE_PATTERN = "classpath*:/initial-objects/**/*.xml";

    @Override
    public String getOperationName() {
        return "initial objects";
    }

    @Override
    public LogTarget getLogTarget() {
        return options.getOutput() != null ? LogTarget.SYSTEM_ERR : LogTarget.SYSTEM_OUT;
    }

    @Override
    public ActionResult<InitialObjectsResult> execute() throws Exception {
        InitialObjectsResult actionResult = new InitialObjectsResult();

        Writer writer = null;
        try {
            if (options.isReport()) {
                writer = NinjaUtils.createWriter(
                        options.getOutput(), context.getCharset(), options.isZip(), options.isOverwrite(), context.out);
                writer.write(NinjaUtils.XML_DELTAS_PREFIX);
            }

            OperationResult result = new OperationResult("Initial objects update");

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

            for (Resource resource : resources) {
                actionResult.incrementTotal();

                processFile(resource, result, actionResult, writer);
            }
        } finally {
            if (writer != null) {
                writer.write(NinjaUtils.XML_DELTAS_SUFFIX);

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

    private <O extends ObjectType> void processFile(
            Resource resource, OperationResult parentResult, InitialObjectsResult actionResult, Writer writer) {

        OperationResult result = parentResult.createSubresult("Process file");

        final PrismContext prismContext = context.getPrismContext();
        final RepositoryService repository = context.getRepository();

        log.debug("File: {}", resource.getFilename());

        try (InputStream is = resource.getInputStream()) {
            String xml = IOUtils.toString(is, StandardCharsets.UTF_8);
            PrismObject<O> object = prismContext.parseObject(xml);

            PrismObject<O> existing = null;
            try {
                existing = repository.getObject(object.getCompileTimeClass(), object.getOid(), null, result);
            } catch (ObjectNotFoundException ex) {
                // this is ok, object will be added, no merge needed
            }

            if (existing == null) {
                // we'll just import object, since it's new one
                addObject(object, result, actionResult, writer);
            } else {
                mergeObject(object, existing, result, actionResult, writer);
            }
        } catch (Exception ex) {
            log.error("Unexpected exception occurred processing file {}", ex, resource.getFilename());
            actionResult.incrementError();
        }
    }

    private <O extends ObjectType> void mergeObject(
            PrismObject<O> initial, PrismObject<O> existing, OperationResult result, InitialObjectsResult actionResult, Writer writer)
            throws SchemaException, ConfigurationException, IOException {

        log.debug("Merging object {}", NinjaUtils.printObjectNameOidAndType(existing));

        final PrismObject<O> merged = existing.clone();

        boolean wasMerged = mergeObject(merged, initial);
        if (!wasMerged) {
            log.warn("Couldn't merge object {}, skipping", NinjaUtils.printObjectNameOidAndType(existing));
            actionResult.incrementError();
        }

        // addTrigger(existing);
        ObjectDelta<O> delta = existing.diff(merged);
        if (delta.isEmpty()) {
            log.debug("Skipping object update, delta is empty");

            actionResult.incrementUnchanged();
            return;
        }

        reportDelta(delta, writer);

        try {
            log.debug(
                    "Updating object {} in repository {}",
                    NinjaUtils.printObjectNameOidAndType(existing), options.isDryRun() ? "(dry run)" : "");

            if (!options.isDryRun()) {
                context.getRepository().modifyObject(delta.getObjectTypeClass(), delta.getOid(), delta.getModifications(), result);
            }

            actionResult.incrementMerged();
        } catch (ObjectNotFoundException | ObjectAlreadyExistsException | SchemaException ex) {
            log.error(
                    "Couldn't modify object {} ({}, {})",
                    ex, existing.getName(), existing.getOid(), existing.toDebugType());

            actionResult.incrementError();
        }
    }

    private <O extends ObjectType> boolean mergeObject(PrismObject<O> target, PrismObject<O> source)
            throws SchemaException, ConfigurationException {

        if (target.equivalent(source)) {
            return true;
        }

        if (!ObjectMergeOperation.hasMergeOperationFor(target)) {
            return false;
        }

        ObjectMergeOperation.merge(target, source);
        return true;
    }

    /**
     * @deprecated This is just a hack to trigger recompute after midpoint is started. TODO FIXME fix this
     */
    @Deprecated
    private <O extends ObjectType> void addTrigger(PrismObject<O> object) {
        TriggerType trigger = new TriggerType()
                .timestamp(MiscUtil.asXMLGregorianCalendar(0L))
                .handlerUri(SchemaConstants.NS_MODEL + "/trigger/recompute/handler-3");
        object.asObjectable().trigger(trigger);
    }

    private <O extends ObjectType> void reportAddDelta(PrismObject<O> object, Writer writer) throws SchemaException, IOException {
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

    private <O extends ObjectType> void addObject(
            PrismObject<O> object, OperationResult result, InitialObjectsResult actionResult, Writer writer)
            throws SchemaException, IOException {

        reportAddDelta(object, writer);

        if (!options.isForceAdd()) {
            log.debug("Skipping object add, force-add options is not set, object will be correctly added during midpoint startup.");
            return;
        }

        // addTrigger(object);

        try {
            log.debug(
                    "Adding object {} to repository {}",
                    NinjaUtils.printObjectNameOidAndType(object), options.isDryRun() ? "(dry run)" : "");

            if (!options.isDryRun()) {
                context.getRepository().addObject(object, RepoAddOptions.createOverwrite(), result);
            }

            actionResult.incrementAdded();
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            log.error("Couldn't add object {} to repository", ex, NinjaUtils.printObjectNameOidAndType(object));

            actionResult.incrementError();
        }
    }
}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade.action;

import com.evolveum.midpoint.ninja.action.Action;
import com.evolveum.midpoint.ninja.action.ActionResult;
import com.evolveum.midpoint.ninja.util.ConsoleFormat;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

// todo action should write XML + maybe csv? for review
public class InitialObjectsAction extends Action<InitialObjectsOptions, ActionResult<InitialObjectsResult>> {

    private static final String INITIAL_OBJECTS_RESOURCE_PATTERN = "classpath*:/initial-objects/**/*.xml";

    @Override
    public String getOperationName() {
        return "initial objects";
    }

    @Override
    public ActionResult<InitialObjectsResult> execute() throws Exception {
        InitialObjectsResult actionResult = new InitialObjectsResult();

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

        Collections.sort(resources, Comparator.comparing(Resource::getFilename));

        for (Resource resource : resources) {
            actionResult.incrementTotal();

            processFile(resource, result, actionResult);
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
            Resource resource, OperationResult parentResult, InitialObjectsResult actionResult) {

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
                addObject(object, result, actionResult);
            } else {
                mergeObject(object, existing, result, actionResult);
            }
        } catch (Exception ex) {
            log.error("Unexpected exception occurred processing file {}", ex, resource.getFilename());
            actionResult.incrementError();
        }
    }

    private <O extends ObjectType> void mergeObject(
            PrismObject<O> initial, PrismObject<O> existing, OperationResult result, InitialObjectsResult actionResult) {

        log.debug("Merging object {}", NinjaUtils.printObjectNameOidAndType(existing));

        final PrismObject<O> merged = existing.clone();

        // todo do merge

        addTrigger(existing);
        ObjectDelta<O> delta = merged.diff(existing);
        if (delta.isEmpty()) {
            log.debug("Skipping object update, delta is empty");

            actionResult.incrementUnchanged();
            return;
        }

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

    private <O extends ObjectType> void addObject(
            PrismObject<O> object, OperationResult result, InitialObjectsResult actionResult) {

        if (!options.isForceAdd()) {
            log.debug("Skipping object add, force-add options is not set, object will be correctly added during midpoint startup.");
            return;
        }

        addTrigger(object);

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

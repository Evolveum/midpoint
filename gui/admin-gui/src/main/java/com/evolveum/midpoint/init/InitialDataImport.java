/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.core.context.SecurityContext;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Imports initial data objects as needed, ignoring already imported objects.
 * Initial objects are found on the classpath using defined pattern to match
 * resources under {@code initial-objects} directory.
 */
public class InitialDataImport extends DataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    private static final String INITIAL_OBJECTS_RESOURCE_PATTERN = "classpath*:/initial-objects/**/*.xml";

    public void init() throws SchemaException {
        init(false);
    }

    public void init(boolean overwrite) throws SchemaException {
        LOGGER.info("Starting initial object import (if necessary).");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_INIT_URI);

        Map<ImportResult, AtomicInteger> importStats = new LinkedHashMap<>();
        importStats.put(ImportResult.IMPORTED, new AtomicInteger());
        importStats.put(ImportResult.ERROR, new AtomicInteger());
        importStats.put(ImportResult.SKIPPED, new AtomicInteger());
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(INITIAL_OBJECTS_RESOURCE_PATTERN);
            Arrays.sort(resources, Comparator.comparing(Resource::getFilename));

            SecurityContext securityContext = provideFakeSecurityContext();
            for (Resource resource : resources) {
                ImportResult result = importInitialObjectsResource(
                        resource, task, mainResult, overwrite);
                importStats.get(result).incrementAndGet();
            }
            securityContext.setAuthentication(null);
        } catch (IOException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't list initial-objects resources", e);
            mainResult.recordFatalError("Couldn't list initial-objects resources", e);
        }

        mainResult.recomputeStatus("Couldn't import objects.");

        LOGGER.info("Initial object import finished ({} objects imported, {} errors, {} skipped)",
                importStats.get(ImportResult.IMPORTED),
                importStats.get(ImportResult.ERROR),
                importStats.get(ImportResult.SKIPPED));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initialization status:\n" + mainResult.debugDump());
        }
    }

    private ImportResult importInitialObjectsResource(
            Resource resource, Task task, OperationResult mainResult, boolean overwrite) {

        try {
            LOGGER.debug("Considering initial import of file {}.", resource.getFilename());
            PrismObject<? extends ObjectType> object;
            try (InputStream resourceInputStream = resource.getInputStream()) {
                String objectText = IOUtils.toString(resourceInputStream, StandardCharsets.UTF_8);
                object = prismContext.parseObject(objectText);
            }

            return importObject(object, resource.getFilename(), task, mainResult, overwrite);
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't import file {}", ex, resource.getFilename());
            mainResult.recordFatalError(
                    "Couldn't import file '" + resource.getFilename() + "'", ex);
            return ImportResult.ERROR;
        }
    }

    private ImportResult importObject(PrismObject<? extends ObjectType> object,
            String fileName, Task task, OperationResult mainResult, boolean overwrite) {
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

        Class<? extends ObjectType> type = object.getCompileTimeClass();
        if (type == null) {
            LOGGER.warn("Object without static type? Skipping: {}", object);
            return ImportResult.SKIPPED;
        }
        if (!model.isSupportedByRepository(type)) { // temporary code (until generic repo is gone)
            LOGGER.debug("Skipping {} because of unsupported object type", object);
            return ImportResult.SKIPPED;
        }

        try {
            // returns not-null or throws, we don't care about the returned object
            model.getObject(
                    type,
                    object.getOid(),
                    SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()),
                    task,
                    result);
            result.recordSuccess();
            if (!overwrite) {
                return ImportResult.SKIPPED;
            }
        } catch (ObjectNotFoundException ex) {
            // this is OK, we're going to import missing object
        } catch (Exception ex) {
            // unexpected, but we'll try to import the object and see what happens
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't get object with oid {} from model", ex, object.getOid());
            result.recordWarning(
                    "Couldn't get object with oid '" + object.getOid() + "' from model", ex);
        }

        preImportUpdate(object);

        ObjectDelta<? extends ObjectType> delta = DeltaFactory.Object.createAddDelta(object);
        try {
            LOGGER.info("Starting initial import of file {}.", fileName);
            model.executeChanges(
                    MiscUtil.createCollection(delta),
                    ModelExecuteOptions.create().setIsImport().overwrite(overwrite),
                    task,
                    result);
            result.recordSuccess();
            LOGGER.info("Created {} as part of initial import", object);
            return ImportResult.IMPORTED;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import {} from file {}: ",
                    e, object, fileName, e.getMessage());
            result.recordFatalError(e);

            LOGGER.info("\n" + result.debugDump());
            return ImportResult.ERROR;
        }
    }

    private enum ImportResult {
        SKIPPED, IMPORTED, ERROR
    }
}

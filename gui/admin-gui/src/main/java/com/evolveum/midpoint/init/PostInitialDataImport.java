/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.init;

import java.io.File;
import java.util.Arrays;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.config.ExecuteScriptConfigItem;
import com.evolveum.midpoint.schema.util.ScriptingBeansUtil;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.security.core.context.SecurityContext;

import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModelExecuteOptionsType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

/**
 * @author lazyman
 * @author skublik
 */
public class PostInitialDataImport extends DataImport {

    private static final Trace LOGGER = TraceManager.getTrace(PostInitialDataImport.class);

    private static final String OPERATION_EXECUTE_SCRIPT = DOT_CLASS + "executeScript";

    private static final String SUFFIX_FOR_IMPORTED_FILE = "done";
    private static final String XML_SUFFIX = "xml";

    private ScriptingService scripting;

    public void setScripting(ScriptingService scripting) {
        Validate.notNull(scripting, "Scripting service must not be null.");
        this.scripting = scripting;
    }

    public void init() throws SchemaException {
        LOGGER.info("Starting initial object import (if necessary).");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_INIT_URI);

        File[] files = getPostInitialImportObjects();
        LOGGER.debug("Files to be imported: {}.", Arrays.toString(files));

        SecurityContext securityContext = provideFakeSecurityContext();

        int countImportedObjects = 0;
        int countExecutedScripts = 0;

        for (File file : files) {
            String fileExtension = FilenameUtils.getExtension(file.getName());
            if (fileExtension.equals(SUFFIX_FOR_IMPORTED_FILE)) {
                continue;
            }
            if (!fileExtension.equals(XML_SUFFIX)) {
                LOGGER.warn("Post-initial import support only xml files. Actual file: " + file.getName());
                continue;
            }
            Item<?, ?> item = null;
            try {
                item = prismContext.parserFor(file).parseItem();
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse file {}", ex, file.getName());
                mainResult.recordFatalError("Couldn't parse file '" + file.getName() + "'", ex);
            }

            if (item instanceof PrismProperty && (item.getRealValue() instanceof ScriptingExpressionType || item.getRealValue() instanceof ExecuteScriptType)) {
                //noinspection unchecked
                PrismProperty<Object> expression = (PrismProperty<Object>) item;
                if (executeScript(expression, file, task, mainResult)) {
                    markAsDone(file);
                    countExecutedScripts++;
                } else {
                    break;
                }
            } else {
                if (importObject(file, task, mainResult)) {
                    markAsDone(file);
                    countImportedObjects++;
                } else {
                    break;
                }
            }
        }

        securityContext.setAuthentication(null);

        mainResult.recomputeStatus("Couldn't import objects.");

        LOGGER.info("Post-initial object import finished ({} objects imported, {} scripts executed)", countImportedObjects, countExecutedScripts);
        LOGGER.trace("Initialization status:\n{}", mainResult.debugDumpLazily());
    }

    private void markAsDone(File file) {
        if (!file.renameTo(new File(file.getPath() + "." + SUFFIX_FOR_IMPORTED_FILE))) {
            LOGGER.warn("Renaming {} to indicate 'done' status was not successful", file);
        }
    }

    /**
     * @return true if it was success, otherwise false
     */
    private boolean importObject(File file, Task task, OperationResult mainResult) {
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);
        try {
            LOGGER.info("Starting post-initial import of file {}.", file.getName());
            ImportOptionsType options = new ImportOptionsType();
            options.overwrite(true);
            options.setModelExecutionOptions(new ModelExecuteOptionsType(prismContext).raw(false));
            model.importObjectsFromFile(file, options, task, result);
            result.recordSuccess();
            return true;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import object from file {}: ", e, file.getName(), e.getMessage());
            result.recordFatalError(e);
            LOGGER.info("\n{}", result.debugDump());
            return false;
        }
    }

    /**
     * @return true if it was success, otherwise false
     */
    private boolean executeScript(PrismProperty<Object> expression, File file, Task task, OperationResult mainResult) {
        OperationResult result = mainResult.createSubresult(OPERATION_EXECUTE_SCRIPT);

        try {
            LOGGER.info("Starting post-initial execute script from file {}.", file.getName());
            ExecuteScriptType parsed =
                    ScriptingBeansUtil.asExecuteScriptCommand(
                            expression.getAnyValue().getValue());

            ScriptExecutionResult executionResult =
                    scripting.evaluateExpression(
                            ExecuteScriptConfigItem.of(
                                    parsed,
                                    // TODO or should we create some "fully trusted origin"?
                                    ConfigurationItemOrigin.external(SchemaConstants.CHANNEL_INIT_URI)),
                            VariablesMap.emptyMap(),
                            false,
                            task,
                            result);
            result.recordSuccess();
            result.addReturn("console", executionResult.getConsoleOutput());
            LOGGER.info("Executed a script in {} as part of post-initial import. Output is:\n{}",
                    file.getName(), executionResult.getConsoleOutput());
            return true;
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script from file {}", ex, file.getName());
            result.recordFatalError("Couldn't execute script from file '" + file.getName() + "'", ex);
            return false;
        }
    }

    private File[] getPostInitialImportObjects() {
        File[] files = new File[0];
        String midpointHomePath = configuration.getMidpointHome();

        if (checkDirectoryExistence(midpointHomePath)) {
            if (!midpointHomePath.endsWith("/")) {
                midpointHomePath = midpointHomePath + "/";
            }
            String postInitialObjectsPath = midpointHomePath + "post-initial-objects";
            if (checkDirectoryExistence(postInitialObjectsPath)) {
                File folder = new File(postInitialObjectsPath);
                files = listFiles(folder);
                sortFiles(files);
            } else {
                LOGGER.info("Directory " + postInitialObjectsPath + " does not exist. Creating.");
                File dir = new File(postInitialObjectsPath);
                if (!dir.exists() || !dir.isDirectory()) {
                    boolean created = dir.mkdirs();
                    if (!created) {
                        LOGGER.error("Unable to create directory " + postInitialObjectsPath + " as user " + System.getProperty("user.name"));
                    }
                }
            }
        } else {
            LOGGER.debug("Directory " + midpointHomePath + " does not exist.");
        }
        return files;
    }

    private File[] listFiles(File folder) {
        File[] files = folder.listFiles();
        File[] retFiles = new File[0];
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    retFiles = ArrayUtils.add(retFiles, file);
                    continue;
                }
                if (file.isDirectory()) {
                    retFiles = ArrayUtils.addAll(retFiles, listFiles(file));
                }
            }
        }
        return retFiles;
    }

    private boolean checkDirectoryExistence(String dir) {
        File d = new File(dir);
        if (d.isFile()) {
            LOGGER.error(dir + " is file and NOT a directory.");
            throw new SystemException(dir + " is file and NOT a directory !!!");
        }
        if (d.isDirectory()) {
            LOGGER.info("Directory " + dir + " exists. Using it.");
            return true;
        } else {
            return false;
        }
    }
}

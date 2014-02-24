/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ReportType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author lazyman
 */
public class InitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    private static final String DOT_CLASS = InitialDataImport.class.getName() + ".";
    private static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    private static final String OBJECTS_FILE = "objects.xml";

    @Autowired
    private transient PrismContext prismContext;
    private ModelService model;
    private TaskManager taskManager;

    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }

    public void setTaskManager(TaskManager taskManager) {
        Validate.notNull(taskManager, "Task manager must not be null.");
        this.taskManager = taskManager;
    }

    public void init() {
        LOGGER.info("Starting initial object import.");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        int count = 0;
        int errors = 0;
        try {
            PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();

            List<PrismObject<? extends Objectable>> objects = domProcessor.parseObjects(getResource(OBJECTS_FILE));
            for (PrismObject object : objects) {
                Boolean importObject = importObject(object, task, mainResult);
                if (importObject == null) {
                    continue;
                }
                if (importObject) {
                    count++;
                } else {
                    errors++;
                }
            }

            File[] files = getInitialImportObjects();
            for (File file : files) {
                LOGGER.trace("Initial import of file {}.", file.getName());
                PrismObject object = domProcessor.parseObject(file);
                if (ReportType.class.equals(object.getCompileTimeClass())) {
                    ReportTypeUtil.applyDefinition(object, prismContext);
                }

                Boolean importObject = importObject(object, task, mainResult);
                if (importObject == null) {
                    continue;
                }
                if (importObject) {
                    count++;
                } else {
                    errors++;
                }
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't import file {}", ex, OBJECTS_FILE);
            mainResult.recordFatalError("Couldn't import file '" + OBJECTS_FILE + "'", ex);
        }

        mainResult.recomputeStatus("Couldn't import objects.");

        LOGGER.info("Initial object import finished ({} objects imported, {} errors)", count, errors);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initialization status:\n" + mainResult.debugDump());
        }
    }

    /**
     * @param object
     * @param task
     * @param mainResult
     * @return null if nothing was imported, true if it was success, otherwise false
     */
    private Boolean importObject(PrismObject object, Task task, OperationResult mainResult) {
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

        boolean importObject = true;
        try {
            model.getObject(object.getCompileTimeClass(), object.getOid(), null, task, result);
            importObject = false;
            result.recordSuccess();
        } catch (ObjectNotFoundException ex) {
            importObject = true;
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get object with oid {} from model", ex,
                    object.getOid());
            result.recordWarning("Couldn't get object with oid '" + object.getOid() + "' from model",
                    ex);
        }

        if (!importObject) {
            return null;
        }

        ObjectDelta delta = ObjectDelta.createAddDelta(object);
        try {
            model.executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
            result.recordSuccess();
            LOGGER.info("Created {} as part of initial import", object);
            return true;
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Couldn't import {} from file {}: ", e, object, OBJECTS_FILE, e.getMessage());
            result.recordFatalError(e);
            return false;
        }
    }

    private File getResource(String name) {
        URI path;
        try {
            path = InitialDataImport.class.getClassLoader().getResource(name).toURI();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("parameter name = " + name, e);
        }
        return new File(path);
    }

    private File[] getInitialImportObjects() {
        File folder = getResource("initial-objects");
        File[] files = folder.listFiles(new FileFilter() {

            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    return false;
                }

                return true;
            }
        });
        Arrays.sort(files, new Comparator<File>() {

            @Override
            public int compare(File o1, File o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
            }
        });

        return files;
    }
}

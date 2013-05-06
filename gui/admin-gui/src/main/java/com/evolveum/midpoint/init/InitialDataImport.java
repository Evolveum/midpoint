/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.dom.PrismDomProcessor;
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBElement;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author lazyman
 */
public class InitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);
    private static final String DOT_CLASS = InitialDataImport.class.getName() + ".";
    private static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    private final String[] FILES_FOR_IMPORT = new String[]{"globalPasswordPolicy.xml", "systemConfiguration.xml", "admin.xml", "task-cleanup.xml"};
    @Autowired(required = true)
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

    @SuppressWarnings("unchecked")
    public void init() {
        LOGGER.info("Starting initial object import.");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);
        for (String file : FILES_FOR_IMPORT) {
            OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

            try {
            	File resource = getResource(file);
                PrismDomProcessor domProcessor = prismContext.getPrismDomProcessor();
                PrismObject prismObj = domProcessor.parseObject(resource);

                boolean importObject = true;
                try {
                    model.getObject(prismObj.getCompileTimeClass(), prismObj.getOid(), null, task, result);
                    importObject = false;
                    result.recordSuccess();
                } catch (ObjectNotFoundException ex) {
                    importObject = true;
                } catch (Exception ex) {
                    LoggingUtils.logException(LOGGER, "Couldn't get object with oid {} from model", ex,
                            prismObj.getOid());
                    result.recordWarning("Couldn't get object with oid '" + prismObj.getOid() + "' from model",
                            ex);
                }

                if (!importObject) {
                    continue;
                }

                ObjectDelta delta = ObjectDelta.createAddDelta(prismObj);
                model.executeChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
                result.recordSuccess();
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't import file {}", ex, file);
                result.recordFatalError("Couldn't import file '" + file + "'", ex);
            } finally {
//                if (stream != null) {
//                    IOUtils.closeQuietly(stream);
//                }
                result.computeStatus("Couldn't import objects.");
            }
        }
        mainResult.recordSuccess();
        LOGGER.info("Initial object import finished.");
        LOGGER.info("Initialization status:\n" + mainResult.dump());
    }

    private File getResource(String name) {
        URI path = null;
		try {
			path = InitialDataImport.class.getClassLoader().getResource(name).toURI();
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("parameter name = " + name, e);
		}        
        return new File(path);
    }
}

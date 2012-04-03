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
import com.evolveum.midpoint.prism.xml.PrismJaxbProcessor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.bind.JAXBElement;
import java.io.InputStream;

/**
 * @author lazyman
 */
public class InitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    private final String[] FILES_FOR_IMPORT = new String[]{"systemConfiguration.xml", "admin.xml"};
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

        OperationResult mainResult = new OperationResult("Initial Objects Import");
        Task task = taskManager.createTaskInstance();
		// TODO: task initialization
        for (String file : FILES_FOR_IMPORT) {
            OperationResult result = mainResult.createSubresult("Import Object");

            InputStream stream = null;
            try {
                stream = getResource(file);
                PrismJaxbProcessor jaxbProcessor = prismContext.getPrismJaxbProcessor();
                JAXBElement<ObjectType> element = jaxbProcessor.unmarshalElement(stream, ObjectType.class);
                ObjectType object = element.getValue();

                boolean importObject = true;
                try {
                    model.getObject(object.getClass(), object.getOid(), new PropertyReferenceListType(),
                            result);
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
                    continue;
                }

                model.addObject(object.asPrismObject(), task, result);
                result.recordSuccess();
            } catch (Exception ex) {
                LoggingUtils.logException(LOGGER, "Couldn't import file {}", ex, file);
                result.recordFatalError("Couldn't import file '" + file + "'", ex);
            } finally {
                if (stream != null) {
                    IOUtils.closeQuietly(stream);
                }
                result.computeStatus("Couldn't import objects.");
            }
        }
        mainResult.recordSuccess();
        LOGGER.info("Initial object import finished.");
        LOGGER.info("Initialization status:\n" + mainResult.dump());
    }

    private InputStream getResource(String name) {
        return InitialDataImport.class.getClassLoader().getResourceAsStream(name);
    }
}

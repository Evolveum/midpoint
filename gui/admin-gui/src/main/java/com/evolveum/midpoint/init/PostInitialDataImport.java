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

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.ScriptExecutionResult;
import com.evolveum.midpoint.model.api.ScriptingService;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InternalsConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.io.File;
import java.util.*;

/**
 * @author lazyman
 * @author skublik
 */
public class PostInitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(PostInitialDataImport.class);

    private static final String MIDPOINT_HOME_PROPERTY = "midpoint.home";
    private static final String SUFFIX_FOR_IMPORTED_FILE = "done";
    private static final String DOT_CLASS = PostInitialDataImport.class.getName() + ".";
    private static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "postInitialObjectsImport";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "postImportObject";

    @Autowired
    private transient PrismContext prismContext;
    private ModelService model;
    private TaskManager taskManager;
    private ScriptingService scripting;
    
    @Autowired
    private MidpointConfiguration configuration;
    
    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }
    
    public void setPrismContext(PrismContext prismContext) {
    	Validate.notNull(prismContext, "Prism context must not be null.");
		this.prismContext = prismContext;
	}

    public void setTaskManager(TaskManager taskManager) {
        Validate.notNull(taskManager, "Task manager must not be null.");
        this.taskManager = taskManager;
    }
    
    public void setConfiguration(MidpointConfiguration configuration) {
    	Validate.notNull(configuration, "Midpoint configuration must not be null.");
		this.configuration = configuration;
	}
    
    public void setScripting(ScriptingService scripting) {
    	Validate.notNull(scripting, "Scripting service must not be null.");
		this.scripting = scripting;
	}

    public void init() throws SchemaException {
        LOGGER.info("Starting initial object import (if necessary).");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        int countImpotredObjects = 0;
        int countExecutedScripts = 0;

        File[] files = getPostInitialImportObjects();
        LOGGER.debug("Files to be imported: {}.", Arrays.toString(files));

        // We need to provide a fake Spring security context here.
        // We have to fake it because we do not have anything in the repository yet. And to get
        // something to the repository we need a context. Chicken and egg. So we fake the egg.
        SecurityContext securityContext = SecurityContextHolder.getContext();
        UserType userAdministrator = new UserType();
        prismContext.adopt(userAdministrator);
        userAdministrator.setName(new PolyStringType(new PolyString("initAdmin", "initAdmin")));
		MidPointPrincipal principal = new MidPointPrincipal(userAdministrator);
		AuthorizationType superAutzType = new AuthorizationType();
		prismContext.adopt(superAutzType, RoleType.class, new ItemPath(RoleType.F_AUTHORIZATION));
		superAutzType.getAction().add(AuthorizationConstants.AUTZ_ALL_URL);
		Authorization superAutz = new Authorization(superAutzType);
		Collection<Authorization> authorities = principal.getAuthorities();
		authorities.add(superAutz);
        Authentication authentication = new PreAuthenticatedAuthenticationToken(principal, null);
        securityContext.setAuthentication(authentication);

        for (File file : files) {
        	if(FilenameUtils.getExtension(file.getName()).equals(SUFFIX_FOR_IMPORTED_FILE)) {
        		continue;
        	}
        	Item item = null;
			try {
				item = prismContext.parserFor(file).parseItem();
			} catch (Exception ex) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse file {}", ex, file.getName());
                mainResult.recordFatalError("Couldn't parse file '" + file.getName() + "'", ex);
			}
            if(item instanceof PrismObject) {
            	try {
            		LOGGER.debug("Considering initial import of file {}.", file.getName());
            		PrismObject object = (PrismObject)item;
            		if (ReportType.class.equals(object.getCompileTimeClass())) {
            			ReportTypeUtil.applyDefinition(object, prismContext);
            		}

            		Boolean importObject = importObject(object, file, task, mainResult);
            		if (importObject) {
            			file.renameTo(new File(file.getPath() + "." + SUFFIX_FOR_IMPORTED_FILE));
            			countImpotredObjects++;
            		} else {
            			break;
            		}
            	} catch (Exception ex) {
            		LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import file {}", ex, file.getName());
            		mainResult.recordFatalError("Couldn't import file '" + file.getName() + "'", ex);
            	}
            } else if(item instanceof PrismProperty && (((PrismProperty)item).getRealValue() instanceof ScriptingExpressionType) || ((PrismProperty)item).getRealValue() instanceof ExecuteScriptType){
            	PrismProperty<Object> expression = (PrismProperty<Object>)item;
            	Boolean executeScript = executeScript(expression, file, task, mainResult);
            	if (executeScript) {
        			file.renameTo(new File(file.getPath() + "." + SUFFIX_FOR_IMPORTED_FILE));
        			countExecutedScripts++;
        		} else {
        			break;
        		}
            } else {
            	mainResult.recordFatalError("\"Provided file" + file.getName() +" is not a bulk action object or prism object.\"");
            }
        }

        securityContext.setAuthentication(null);

        mainResult.recomputeStatus("Couldn't import objects.");

        LOGGER.info("Post-initial object import finished ({} objects imported, {} scripts executed)", countImpotredObjects, countExecutedScripts);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Initialization status:\n" + mainResult.debugDump());
        }
    }

    /**
     * @param object
     * @param task
     * @param mainResult
     * @return true if it was success, otherwise false
     */
    private <O extends ObjectType> Boolean importObject(PrismObject<O> object, File file, Task task, OperationResult mainResult) {
    	
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);
        preImportUpdate(object);

        ObjectDelta delta = ObjectDelta.createAddDelta(object);
        try {
       		LOGGER.info("Starting post-initial import of file {}.", file.getName());
       		ImportOptionsType options = new ImportOptionsType();
       		options.overwrite(true);
       		model.importObjectsFromFile(file, options, task, result);
       		result.recordSuccess();
       		LOGGER.info("Created {} as part of post-initial import", object);
       		return true;
       	} catch (Exception e) {
       		LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import {} from file {}: ", e, object,
       				file.getName(), e.getMessage());
       		result.recordFatalError(e);

       		LOGGER.info("\n" + result.debugDump());
       		return false;
       	}
    }

    private <O extends ObjectType> void preImportUpdate(PrismObject<O> object) {
		if (object.canRepresent(SystemConfigurationType.class)) {
			SystemConfigurationType systemConfigType = (SystemConfigurationType) object.asObjectable();
			InternalsConfigurationType internals = systemConfigType.getInternals();
			if (internals != null) {
				PolyStringNormalizerConfigurationType normalizerConfig = internals.getPolyStringNormalizer();
				if (normalizerConfig != null) {
					try {
						prismContext.configurePolyStringNormalizer(normalizerConfig);
						LOGGER.debug("Applied PolyString normalizer configuration {}", DebugUtil.shortDumpLazily(normalizerConfig));
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						LOGGER.error("Error applying polystring normalizer configuration: "+e.getMessage(), e);
						throw new SystemException("Error applying polystring normalizer configuration: "+e.getMessage(), e);
					}
					// PolyString normalizer configuration applied. But we need to re-normalize the imported object
					// otherwise it would be normalized in a different way than other objects.
					object.recomputeAllValues();
				}
			}
		}
		
	}
    
    /**
     * @param expression
     * @param file
     * @param task
     * @param mainResult
     * @return rue if it was success, otherwise false
     */
    private <O extends ObjectType> Boolean executeScript(PrismProperty<Object> expression, File file, Task task, OperationResult mainResult) {
    	
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

        try {
        	LOGGER.info("Starting post-initial execute script from file {}.", file.getName());
        	Object parsed = expression.getAnyValue().getValue();
        	ScriptExecutionResult executionResult =
                parsed instanceof ExecuteScriptType ?
                        scripting.evaluateExpression((ExecuteScriptType) parsed, Collections.emptyMap(),
                                false, task, result) :
                        scripting.evaluateExpression((ScriptingExpressionType) parsed, task, result);
            result.recordSuccess();
            result.addReturn("console", executionResult.getConsoleOutput());
            LOGGER.info("Executed {} as part of post-initial import with output: {}", expression, executionResult.getConsoleOutput());
            return true;
        } catch (Exception ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't execute script from file {}", ex, file.getName());
			result.recordFatalError("Couldn't execute script from file '" + file.getName() + "'", ex);
			return false;
        }
    }

    private File[] getPostInitialImportObjects() {
    	File[] files = new File[0];
    	String midpointHomePath= System.getProperty(MIDPOINT_HOME_PROPERTY);
    	
    	if (checkDirectoryExistence(midpointHomePath)) {
    	
    		if (!midpointHomePath.endsWith("/")) {
    			midpointHomePath = midpointHomePath + "/";
    		}
    		
    		String postInitialObjectsPath = midpointHomePath + "post-initial-objects";
    		if (checkDirectoryExistence(postInitialObjectsPath)) {
    			File folder = new File(postInitialObjectsPath);

    			files = listFiles(folder);

    			Arrays.sort(files, (o1, o2) -> {
    				int n1 = getNumberFromName(o1);
    				int n2 = getNumberFromName(o2);

    				return n1 - n2;
    			});

    		}
    		else {
        		LOGGER.debug("Directory " + postInitialObjectsPath + " does not exist.");
        	}
    	}
    	else {
    		LOGGER.debug("Directory " + midpointHomePath + " does not exist.");
    	}
    	return files;
    }
    
    private File[] listFiles(File folder) {
    	File[] files = folder.listFiles();
    	for(File file: files){
    	    if(file.isDirectory()){
    	    	files = (File[])ArrayUtils.removeElement(files, file);
    	    	files = (File[])ArrayUtils.addAll(files, listFiles(file));
    	    }
    	}
    	return files;
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

    private int getNumberFromName(File file) {
        String name = file.getName();
        String number = StringUtils.left(name, 3);
        if (number.matches("[\\d]+")) {
            return Integer.parseInt(number);
        }
        return 0;
    }
}

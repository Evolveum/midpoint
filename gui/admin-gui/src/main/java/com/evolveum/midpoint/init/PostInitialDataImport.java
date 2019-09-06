/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import java.io.File;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.Validate;
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
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ScriptingExpressionType;

/**
 * @author lazyman
 * @author skublik
 */
public class PostInitialDataImport extends DataImport{

    private static final Trace LOGGER = TraceManager.getTrace(PostInitialDataImport.class);

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
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        File[] files = getPostInitialImportObjects();
        LOGGER.debug("Files to be imported: {}.", Arrays.toString(files));

        SecurityContext securityContext = provideFakeSecurityContext();

        int countImpotredObjects = 0;
        int countExecutedScripts = 0;
        
        for (File file : files) {
        	if(FilenameUtils.getExtension(file.getName()).equals(SUFFIX_FOR_IMPORTED_FILE)) {
        		continue;
        	}
        	if(!FilenameUtils.getExtension(file.getName()).equals(XML_SUFFIX)) {
        		LOGGER.warn("Post-initial import support only xml files. Actual file: " + file.getName());
        		continue;
        	}
        	Item item = null;
			try {
				item = prismContext.parserFor(file).parseItem();
			} catch (Exception ex) {
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't parse file {}", ex, file.getName());
                mainResult.recordFatalError("Couldn't parse file '" + file.getName() + "'", ex);
			}
			
			if(item instanceof PrismProperty && (((PrismProperty)item).getRealValue() instanceof ScriptingExpressionType || ((PrismProperty)item).getRealValue() instanceof ExecuteScriptType)){
	        	PrismProperty<Object> expression = (PrismProperty<Object>)item;
	        	Boolean executeScript = executeScript(expression, file, task, mainResult);
	        	if (executeScript) {
	    			file.renameTo(new File(file.getPath() + "." + SUFFIX_FOR_IMPORTED_FILE));
	    			countExecutedScripts++;
	    		} else {
	    			break;
	    		}
			} else {
				try {
	        		LOGGER.debug("Considering post-initial import of file {}.", file.getName());

	        		Boolean importObject = importObject(file, task, mainResult);
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
    private <O extends ObjectType> Boolean importObject(File file, Task task, OperationResult mainResult) {
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);
        try {
       		LOGGER.info("Starting post-initial import of file {}.", file.getName());
       		ImportOptionsType options = new ImportOptionsType();
       		options.overwrite(true);
       		model.importObjectsFromFile(file, options, task, result);
       		result.recordSuccess();
       		return true;
       	} catch (Exception e) {
       		LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import object from file {}: ", e, file.getName(), e.getMessage());
       		result.recordFatalError(e);

       		LOGGER.info("\n" + result.debugDump());
       		return false;
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
                        scripting.evaluateExpression((ExecuteScriptType) parsed, VariablesMap.emptyMap(),
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
    	String midpointHomePath= configuration.getMidpointHome();
    	
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
    	}
    	else {
    		LOGGER.debug("Directory " + midpointHomePath + " does not exist.");
    	}
    	return files;
    }
    
    private File[] listFiles(File folder) {
    	File[] files = folder.listFiles();
    	File[] retFiles =new File[0];
    	for(File file: files){
    		if(file.isFile()){
    			retFiles = (File[])ArrayUtils.add(retFiles, file);
    			continue;
    		}
    	    if(file.isDirectory()){
    	    	
    	    	retFiles = (File[])ArrayUtils.addAll(retFiles, listFiles(file));
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

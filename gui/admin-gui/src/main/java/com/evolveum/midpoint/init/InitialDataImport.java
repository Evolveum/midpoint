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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
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
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * @author lazyman
 */
public class InitialDataImport {

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    private static final String DOT_CLASS = InitialDataImport.class.getName() + ".";
    private static final String OPERATION_INITIAL_OBJECTS_IMPORT = DOT_CLASS + "initialObjectsImport";
    private static final String OPERATION_IMPORT_OBJECT = DOT_CLASS + "importObject";

    @Autowired
    private transient PrismContext prismContext;
    private ModelService model;
    private TaskManager taskManager;
    @Autowired
    private MidpointConfiguration configuration;

    public void setModel(ModelService model) {
        Validate.notNull(model, "Model service must not be null.");
        this.model = model;
    }

    public void setTaskManager(TaskManager taskManager) {
        Validate.notNull(taskManager, "Task manager must not be null.");
        this.taskManager = taskManager;
    }

    public void init() throws SchemaException {
        LOGGER.info("Starting initial object import (if necessary).");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        int count = 0;
        int errors = 0;

        File[] files = getInitialImportObjects();
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
            try {
                LOGGER.debug("Considering initial import of file {}.", file.getName());
                PrismObject object = prismContext.parseObject(file);
                if (ReportType.class.equals(object.getCompileTimeClass())) {
                    ReportTypeUtil.applyDefinition(object, prismContext);
                }

                Boolean importObject = importObject(object, file, task, mainResult);
                if (importObject == null) {
                    continue;
                }
                if (importObject) {
                    count++;
                } else {
                    errors++;
                }
            } catch (Exception ex) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import file {}", ex, file.getName());
                mainResult.recordFatalError("Couldn't import file '" + file.getName() + "'", ex);
            }
        }

        securityContext.setAuthentication(null);

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
    private Boolean importObject(PrismObject object, File file, Task task, OperationResult mainResult) {
        OperationResult result = mainResult.createSubresult(OPERATION_IMPORT_OBJECT);

        boolean importObject = true;
        try {
            model.getObject(object.getCompileTimeClass(), object.getOid(), SelectorOptions.createCollection(GetOperationOptions.createAllowNotFound()), task, result);
            importObject = false;
            result.recordSuccess();
        } catch (ObjectNotFoundException ex) {
            importObject = true;
        } catch (Exception ex) {
        	if (!importObject){
	            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get object with oid {} from model", ex,
	                    object.getOid());
	            result.recordWarning("Couldn't get object with oid '" + object.getOid() + "' from model",
	                    ex);
        	}
        }

        if (!importObject) {
            return null;
        }

        ObjectDelta delta = ObjectDelta.createAddDelta(object);
        try {
            LOGGER.info("Starting initial import of file {}.", file.getName());
            model.executeChanges(WebComponentUtil.createDeltaCollection(delta), ModelExecuteOptions.createIsImport(), task, result);
            result.recordSuccess();
            LOGGER.info("Created {} as part of initial import", object);
            return true;
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't import {} from file {}: ", e, object,
                    file.getName(), e.getMessage());
            result.recordFatalError(e);

            LOGGER.info("\n" + result.debugDump());
            return false;
        }
    }

    private File getResource(String name) {
        URI path;
        try {
        	LOGGER.trace("getResource: name = {}", name);
            path = InitialDataImport.class.getClassLoader().getResource(name).toURI();
            LOGGER.trace("getResource: path = {}", path);
            //String updatedPath = path.toString().replace("zip:/", "jar:/");
            //LOGGER.trace("getResource: path updated = {}", updatedPath);
            //path = new URI(updatedPath);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("parameter name = " + name, e);
        }
        return new File(path);
    }

    private File[] getInitialImportObjects() {
        URL path = InitialDataImport.class.getClassLoader().getResource("initial-objects");
    	String resourceType = path.getProtocol();

        File[] files = null;
        File folder = null;

        if ("zip".equals(resourceType) || "jar".equals(resourceType)) {
        	try {
        		File tmpDir = new File(configuration.getMidpointHome()+"/tmp");
        		if (!tmpDir.mkdir()) {
        			LOGGER.warn("Failed to create temporary directory for initial objects {}. Maybe it already exists", configuration.getMidpointHome()+"/tmp");
        		}

        		tmpDir = new File(configuration.getMidpointHome()+"/tmp/initial-objects");
        		if (!tmpDir.mkdir()) {
        			LOGGER.warn("Failed to create temporary directory for initial objects {}. Maybe it already exists", configuration.getMidpointHome()+"/tmp/initial-objects");
        		}

        		//prerequisite: we are expecting that the files are store in the same archive as the source code that is loading it
	        	URI src = InitialDataImport.class.getProtectionDomain().getCodeSource().getLocation().toURI();
	        	LOGGER.trace("InitialDataImport code location: {}", src);
	            Map<String, String> env = new HashMap<>();
	            env.put("create", "false");
	            URI normalizedSrc = new URI(src.toString().replaceFirst("file:", "jar:file:"));
	            LOGGER.trace("InitialDataImport normalized code location: {}", normalizedSrc);
	        	try (FileSystem zipfs = FileSystems.newFileSystem(normalizedSrc, env)) {
	                Path pathInZipfile = zipfs.getPath("/initial-objects");
	                //TODO: use some well defined directory, e.g. midPoint home
	                final Path destDir = Paths.get(configuration.getMidpointHome()+"/tmp");
	                Files.walkFileTree(pathInZipfile, new SimpleFileVisitor<Path>(){
	                    @Override
	                    public FileVisitResult visitFile(Path file,
	                        BasicFileAttributes attrs) throws IOException {
	                      final Path destFile = Paths.get(destDir.toString(),
	                                                      file.toString());
	                      LOGGER.trace("Extracting file {} to {}", file, destFile);
	                      Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
	                      return FileVisitResult.CONTINUE;
	                    }

	                    @Override
	                    public FileVisitResult preVisitDirectory(Path dir,
	                        BasicFileAttributes attrs) throws IOException {
	                      final Path dirToCreate = Paths.get(destDir.toString(),
	                                                         dir.toString());
	                      if(Files.notExists(dirToCreate)){
	                        LOGGER.trace("Creating directory {}", dirToCreate);
	                        Files.createDirectory(dirToCreate);
	                      }
	                      return FileVisitResult.CONTINUE;
	                    }
	                  });


	            }
	        	folder = new File(configuration.getMidpointHome()+"/tmp/initial-objects");
        	} catch (IOException ex) {
        		throw new RuntimeException("Failed to copy initial objects file out of the archive to the temporary directory", ex);
        	} catch (URISyntaxException ex) {
        		throw new RuntimeException("Failed get URI for the source code bundled with initial objects", ex);
        	}
        }

    	if ("file".equals(resourceType)) {
	        folder = getResource("initial-objects");
    	}

        files = folder.listFiles(new FileFilter() {

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
                int n1 = getNumberFromName(o1);
                int n2 = getNumberFromName(o2);

                return n1 - n2;
            }
        });

        return files;
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

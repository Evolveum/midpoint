/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ReportTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;
import org.apache.commons.io.FileUtils;
import org.springframework.security.core.context.SecurityContext;

import java.io.File;
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
public class InitialDataImport extends DataImport{

    private static final Trace LOGGER = TraceManager.getTrace(InitialDataImport.class);

    public void init() throws SchemaException {
        LOGGER.info("Starting initial object import (if necessary).");

        OperationResult mainResult = new OperationResult(OPERATION_INITIAL_OBJECTS_IMPORT);
        Task task = taskManager.createTaskInstance(OPERATION_INITIAL_OBJECTS_IMPORT);
        task.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);

        int count = 0;
        int errors = 0;

        File[] files = getInitialImportObjects();
        LOGGER.debug("Files to be imported: {}.", Arrays.toString(files));

        SecurityContext securityContext = provideFakeSecurityContext();

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

        try {
            cleanup();
        } catch (IOException ex) {
            LOGGER.error("Couldn't cleanup tmp folder with imported files", ex);
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
    private <O extends ObjectType> Boolean importObject(PrismObject<O> object, File file, Task task, OperationResult mainResult) {
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

        preImportUpdate(object);

        ObjectDelta delta = DeltaFactory.Object.createAddDelta(object);
        try {
            LOGGER.info("Starting initial import of file {}.", file.getName());
            model.executeChanges(MiscUtil.createCollection(delta), ModelExecuteOptions.createIsImport(), task, result);
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

    private void cleanup() throws IOException {
        Path destDir = Paths.get(configuration.getMidpointHome(), "tmp/initial-objects");
        FileUtils.deleteDirectory(destDir.toFile());
    }

    /**
     * @param path jar:file:/<ABSOLUTE_PATH>/midpoint.war!/WEB-INF/classes!/initial-objects
     * @param tmpDir
     */
    private void copyInitialImportObjectsFromFatJar(URL path, File tmpDir) throws IOException, URISyntaxException {
        String warPath = path.toString().split("!/")[0];

        URI src = URI.create(warPath);

        Map<String, String> env = new HashMap<>();
        env.put("create", "false");
        try (FileSystem zipfs = FileSystems.newFileSystem(src, env)) {
            Path pathInZipfile = zipfs.getPath("/WEB-INF/classes/initial-objects");
            final Path destDir = Paths.get(configuration.getMidpointHome(), "tmp");

            Files.walkFileTree(pathInZipfile, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs) throws IOException {
                    String f = file.subpath(2, file.getNameCount()).toString();  // strip /WEB-INF/classes
                    final Path destFile = Paths.get(destDir.toString(), f);
                    LOGGER.trace("Extracting file {} to {}", file, destFile);
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                                                         BasicFileAttributes attrs) throws IOException {
                    String folder = dir.subpath(2, dir.getNameCount()).toString();  // strip /WEB-INF/classes
                    final Path dirToCreate = Paths.get(destDir.toString(), folder);
                    if (Files.notExists(dirToCreate)) {
                        LOGGER.trace("Creating directory {}", dirToCreate);
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    /**
     * file:/<ABSOLUTE_PATH>/midpoint/WEB-INF/classes/initial-objects/
     */
    private void copyInitialImportObjectsFromJar() throws IOException, URISyntaxException {
        URI src = InitialDataImport.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        LOGGER.trace("InitialDataImport code location: {}", src);
        Map<String, String> env = new HashMap<>();
        env.put("create", "false");
        URI normalizedSrc = new URI(src.toString().replaceFirst("file:", "jar:file:"));
        LOGGER.trace("InitialDataImport normalized code location: {}", normalizedSrc);
        try (FileSystem zipfs = FileSystems.newFileSystem(normalizedSrc, env)) {
            Path pathInZipfile = zipfs.getPath("/initial-objects");
            final Path destDir = Paths.get(configuration.getMidpointHome(), "tmp");
            Files.walkFileTree(pathInZipfile, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file,
                        BasicFileAttributes attrs) throws IOException {
                    final Path destFile = Paths.get(destDir.toString(), file.toString());
                    LOGGER.trace("Extracting file {} to {}", file, destFile);
                    Files.copy(file, destFile, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir,
                        BasicFileAttributes attrs) throws IOException {
                    final Path dirToCreate = Paths.get(destDir.toString(), dir.toString());
                    if (Files.notExists(dirToCreate)) {
                        LOGGER.trace("Creating directory {}", dirToCreate);
                        Files.createDirectory(dirToCreate);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private File setupInitialObjectsTmpFolder() {
        File tmpDir = new File(configuration.getMidpointHome(), "tmp");
        if (!tmpDir.mkdir()) {
            LOGGER.warn("Failed to create temporary directory for initial objects {}. Maybe it already exists",
                    configuration.getMidpointHome()+"/tmp");
        }

        tmpDir = new File(configuration.getMidpointHome()+"/tmp/initial-objects");
        if (!tmpDir.mkdir()) {
            LOGGER.warn("Failed to create temporary directory for initial objects {}. Maybe it already exists",
                    configuration.getMidpointHome()+"/tmp/initial-objects");
        }

        return tmpDir;
    }

    protected File[] getInitialImportObjects() {
        URL path = InitialDataImport.class.getClassLoader().getResource("initial-objects");
        String resourceType = path.getProtocol();

        File folder = null;

        try {
            if (path.toString().split("!/").length == 3) {
                File tmpDir = setupInitialObjectsTmpFolder();

                copyInitialImportObjectsFromFatJar(path, tmpDir);

                folder = tmpDir;
            } else if ("zip".equals(resourceType) || "jar".equals(resourceType)) {

                File tmpDir = setupInitialObjectsTmpFolder();
                copyInitialImportObjectsFromJar();

                folder = tmpDir;
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to copy initial objects file out of the archive to the temporary directory", ex);
        } catch (URISyntaxException ex) {
            throw new RuntimeException("Failed get URI for the source code bundled with initial objects", ex);
        }

        if ("file".equals(resourceType)) {
            folder = getResource("initial-objects");
        }

        File[] files = folder.listFiles(pathname -> {
            if (pathname.isDirectory()) {
                return false;
            }

            return true;
        });

        sortFiles(files);

        return files;
    }
}

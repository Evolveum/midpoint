/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.test;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

import com.google.common.io.Files;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.ClassPathUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Represents CSV resource to be used in tests.
 *
 * Manages the content (CSV file itself) and the resource definition object.
 *
 * Unlike other "test resource classes" ({@link TestResource}, {@link DummyTestResource})
 * this class tries to be active and to manage the things.
 *
 * Limitations:
 *
 * - data manipulation methods use fixed charset (system default)
 */
public class CsvResource extends AnyResource {

    private static final Trace LOGGER = TraceManager.getTrace(CsvResource.class);

    private static final String NS_RESOURCE_CSV = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/bundle/"
            + "com.evolveum.polygon.connector-csv/com.evolveum.polygon.connector.csv.CsvConnector";
    private static final ItemName FILE_PATH_NAME = new ItemName(NS_RESOURCE_CSV, "filePath");
    private static final ItemPath FILE_PATH_PATH =
            ItemPath.create(
                    ResourceType.F_CONNECTOR_CONFIGURATION,
                    SchemaConstants.ICF_CONFIGURATION_PROPERTIES,
                    FILE_PATH_NAME);

    @NotNull private final File dir;

    /** Name of the data file (without the path). It is the same for source and "live" data file. */
    @NotNull private final String dataFileName;

    /** Initial content of the file. If specified, we do not try to copy the content from the source file. */
    @Nullable private final String initialContent;

    /**
     * The "live" data file, i.e. where the actual data for CSV resource is stored. It is placed
     * in midpoint home under directory named by resource OID: `midpoint-home/resource-oid/data-file-name`.
     */
    private File dataFile;

    @NotNull private final Charset charset = Charset.defaultCharset();

    /**
     * Creates the resource. The content is taken from the specified source file (`dataFileName` in `dir`).
     */
    public CsvResource(@NotNull File dir, @NotNull String fileName, @NotNull String oid, @NotNull String dataFileName) {
        super(dir, fileName, oid);
        this.dir = dir;
        this.dataFileName = dataFileName;
        this.initialContent = null;
    }

    /**
     * Creates the resource. The content is not taken from the data file but from provided string. If line separator
     * is not present at the end of the line, it is added automatically.
     */
    public CsvResource(@NotNull File dir, @NotNull String fileName, @NotNull String oid, @NotNull String dataFileName,
            @NotNull String initialContent) {
        super(dir, fileName, oid);
        this.dir = dir;
        this.dataFileName = dataFileName;
        this.initialContent = initialContent;
    }

    @Override
    protected void customizeParsed(PrismObject<ResourceType> parsed) {
        setCsvFilePathInResourceDefinition(parsed);
    }

    /** Hacks the resource prism object by setting CSV file path. */
    private void setCsvFilePathInResourceDefinition(PrismObject<ResourceType> parsed) {
        dataFile = prepareDataFile();
        parsed.findProperty(FILE_PATH_PATH)
                .setRealValue(dataFile.getAbsolutePath());
    }

    private @NotNull File prepareDataFile() {
        try {
            File destinationDir = new File(
                    TestSpringBeans.getMidpointConfiguration().getMidpointHome(),
                    oid);
            //noinspection ResultOfMethodCallIgnored
            destinationDir.mkdir();
            File destinationFile = new File(destinationDir, dataFileName);

            if (initialContent != null) {
                LOGGER.info("Creating {} in {}", dataFileName, destinationDir);
                write(destinationFile, terminateLastLine(initialContent));
            } else {
                LOGGER.info("Start copying {} from {} to {}", dataFileName, dir, destinationDir);
                ClassPathUtil.copyFile(
                        new FileInputStream(new File(dir, dataFileName)),
                        dataFileName, destinationFile);
            }

            if (!destinationFile.exists()) {
                throw new SystemException("CSV file was not created");
            }
            return destinationFile;
        } catch (IOException e) {
            throw new SystemException("Couldn't read the data file: " + e.getMessage(), e);
        }
    }

    /** Appends line separator, if not there. */
    private String terminateLastLine(String text) {
        if (text.endsWith(System.lineSeparator())) {
            return text;
        } else {
            return text + System.lineSeparator();
        }
    }

    private void write(File file, String data) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(file, charset))) {
            pw.append(data);
        }
    }

    /**
     * Appends given data to the "live" file. The caller is responsible for including appropriate line separators.
     */
    public void append(String data) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(dataFile, charset, true))) {
            pw.append(data);
        }
    }

    /**
     * Replaces the whole file with new content. The caller is responsible for including appropriate line separators.
     */
    public void replace(String data) throws IOException {
        write(dataFile, data);
    }

    /**
     * Returns the current content of the file as a mutable list of lines.
     */
    public List<String> getContent() throws IOException {
        return Files.readLines(dataFile, charset);
    }

    /**
     * Rewrites the file with given list of lines. Lines should not contain separators.
     */
    public void setContent(List<String> lines) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(dataFile, charset))) {
            for (String line : lines) {
                checkNoSeparator(line);
                pw.println(line);
            }
        }
    }

    /**
     * Appends given line to the "live" file. The argument should NOT contain line separators.
     */
    public void appendLine(String line) throws IOException {
        checkNoSeparator(line);
        append(line + System.lineSeparator());
    }

    private void checkNoSeparator(String line) {
        argCheck(!line.contains(System.lineSeparator()), "No line separators are allowed: %s", line);
    }

    /**
     * Replaces first line that matches given regular expression with the new content. (Or deletes it if `newLine` is `null`.)
     * Throws an exception if there was no matching line.
     *
     * The new line should NOT contain line separators.
     */
    public void replaceLine(@Language("RegExp") String regex, @Nullable String newLine) throws IOException {
        if (newLine != null) {
            checkNoSeparator(newLine);
        }
        List<String> content = getContent();
        boolean found = false;
        for (int i = 0; i < content.size(); i++) {
            String existingLine = content.get(i);
            if (existingLine.matches(regex)) {
                if (newLine != null) {
                    content.set(i, newLine);
                } else {
                    content.remove(i);
                }
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("No line matched '" + regex + "'");
        }
        setContent(content);
    }

    /** A convenience variant of {@link #replaceLine(String, String)}. */
    public void deleteLine(@Language("RegExp") String regex) throws IOException {
        replaceLine(regex, null);
    }
}

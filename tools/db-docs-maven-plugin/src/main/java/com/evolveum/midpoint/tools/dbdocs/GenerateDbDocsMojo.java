/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.parser.DbSchemaParser;
import com.evolveum.midpoint.tools.dbdocs.render.DbSchemaAsciiDocRenderer;
import com.evolveum.midpoint.tools.dbdocs.render.ReleaseNotesSchemaChangesUpdater;
import com.evolveum.midpoint.tools.dbdocs.render.RenderedAsciiDocPage;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates native PostgreSQL schema documentation from SQL scripts.
 *
 * It writes the main documentation pages and updates the bounded generated
 * native PostgreSQL schema changes block in the release notes page.
 */
@Mojo(name = "generate-db-docs")
public class GenerateDbDocsMojo extends AbstractMojo {

    private static final List<String> DEFAULT_NATIVE_POSTGRESQL_SQL_FILES = List.of(
            "postgres.sql",
            "postgres-audit.sql",
            "postgres-quartz.sql",
            "postgres-upgrade.sql",
            "postgres-audit-upgrade.sql");

    @Parameter
    private List<File> sourceSqlFiles;

    @Parameter(defaultValue = "${project.basedir}/src/main/sql", required = true)
    private File nativeSqlDirectory;

    @Parameter(defaultValue = "${project.basedir}/target/sql-schema", required = true)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.basedir}/target/sql-schema-release-notes.adoc", required = true)
    private File releaseNotesFile;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<Path> sqlFiles = resolveSourceSqlFiles();
            SchemaDoc schemaDoc = new DbSchemaParser().parse(sqlFiles);
            DbSchemaAsciiDocRenderer renderer = new DbSchemaAsciiDocRenderer();

            Path outputPath = outputDirectory.toPath();
            Path scriptsPath = outputPath.resolve(DbSchemaAsciiDocRenderer.SCRIPTS_DIRECTORY);
            Files.createDirectories(scriptsPath);

            Path outputFile = outputPath.resolve(DbSchemaAsciiDocRenderer.OUTPUT_FILE_NAME);
            Files.write(outputFile, renderer.renderLandingPage(sqlFiles, schemaDoc), StandardCharsets.UTF_8);
            getLog().info("Generated DB schema documentation: " + outputFile);

            for (SqlFileDoc sourceFile : schemaDoc.sourceFiles()) {
                List<RenderedAsciiDocPage> pages = renderer.renderScriptPages(sourceFile, schemaDoc);
                cleanStaleNestedScriptPages(outputPath, pages);
                for (RenderedAsciiDocPage page : pages) {
                    Path scriptOutputFile = outputPath.resolve(page.relativePath());
                    Files.createDirectories(scriptOutputFile.getParent());
                    Files.write(scriptOutputFile, page.lines(), StandardCharsets.UTF_8);
                    getLog().info("Generated DB schema script documentation: " + scriptOutputFile);
                }
            }

            //TODO: Release notes changes are commented out for now.
            //writeReleaseNotesSchemaChanges(schemaDoc);
        } catch (IOException e) {
            throw new MojoExecutionException("Couldn't generate DB schema documentation", e);
        }
    }

    /**
     * Deletes stale split-script region directories before writing newly rendered region pages.
     */
    private void cleanStaleNestedScriptPages(Path outputPath, List<RenderedAsciiDocPage> pages) throws IOException {
        for (RenderedAsciiDocPage page : pages) {
            Path relativeParent = page.relativePath().getParent();
            if (relativeParent != null && relativeParent.getNameCount() > 1) {
                deleteDirectory(outputPath.resolve(relativeParent));
                return;
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> sortedPaths = paths
                    .sorted(Comparator.reverseOrder())
                    .toList();
            for (Path path : sortedPaths) {
                Files.delete(path);
            }
        }
    }

    /**
     * Resolves configured source SQL files or the default native PostgreSQL scripts.
     */
    List<Path> resolveSourceSqlFiles() throws MojoExecutionException {
        List<File> files = sourceSqlFiles;
        if (files == null || files.isEmpty()) {
            files = defaultSourceSqlFiles();
        }

        List<File> sortedFiles = new ArrayList<>(files);
        sortedFiles.sort(Comparator.comparingInt(this::defaultFileOrder).thenComparing(File::getName));

        List<Path> paths = new ArrayList<>();
        for (File file : sortedFiles) {
            validateSourceSqlFile(file);
            paths.add(file.toPath());
        }

        return paths;
    }

    private List<File> defaultSourceSqlFiles() {
        List<File> files = new ArrayList<>();
        for (String fileName : DEFAULT_NATIVE_POSTGRESQL_SQL_FILES) {
            files.add(new File(nativeSqlDirectory, fileName));
        }
        return files;
    }

    private int defaultFileOrder(File file) {
        int index = DEFAULT_NATIVE_POSTGRESQL_SQL_FILES.indexOf(file.getName());
        return index >= 0 ? index : DEFAULT_NATIVE_POSTGRESQL_SQL_FILES.size();
    }

    private void validateSourceSqlFile(File sourceSqlFile) throws MojoExecutionException {
        if (!sourceSqlFile.exists()) {
            throw new MojoExecutionException("Source SQL file does not exist: " + sourceSqlFile);
        }

        if (!sourceSqlFile.isFile()) {
            throw new MojoExecutionException("Source SQL path is not a file: " + sourceSqlFile);
        }

        if (!sourceSqlFile.getName().endsWith(".sql")) {
            throw new MojoExecutionException("Source file is not an SQL file: " + sourceSqlFile);
        }
    }

    /**
     * Updates the release-notes generated block with native PostgreSQL schema changes for the current release.
     */
    private void writeReleaseNotesSchemaChanges(SchemaDoc schemaDoc) throws IOException {
        if (releaseNotesFile == null || !releaseNotesFile.isFile()) {
            getLog().warn("Release notes file does not exist, skipping DB schema release-notes update: "
                    + releaseNotesFile);
            return;
        }

        ReleaseNotesSchemaChangesUpdater updater = new ReleaseNotesSchemaChangesUpdater();
        if (!updater.update(releaseNotesFile.toPath(), schemaDoc)) {
            getLog().warn("Release notes metadata or DB schema changes markers not found, skipping update: "
                    + releaseNotesFile);
            return;
        }

        getLog().info("Updated DB schema release-notes block: " + releaseNotesFile);
    }
}

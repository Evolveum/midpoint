/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc.Category;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import com.evolveum.midpoint.tools.dbdocs.model.TableDoc;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeChangeDoc;

/**
 * Provides grouped and filtered schema data for Velocity templates.
 *
 * The view keeps both the full parsed schema and the currently visible schema subset. The full schema is used for
 * cross-page links and lookups, while the visible schema is used by templates rendering one page, one script, or one
 * postgres.sql region.
 */
public class SchemaDocView {

    private static final Set<SqlObjectDoc.Kind> VIEWS = EnumSet.of(
            SqlObjectDoc.Kind.VIEW, SqlObjectDoc.Kind.MATERIALIZED_VIEW);
    private static final Set<SqlObjectDoc.Kind> ENUM_TYPES = EnumSet.of(SqlObjectDoc.Kind.ENUM_TYPE);
    private static final Set<SqlObjectDoc.Kind> ROUTINES = EnumSet.of(
            SqlObjectDoc.Kind.FUNCTION, SqlObjectDoc.Kind.PROCEDURE);
    private static final Set<SqlObjectDoc.Kind> TRIGGERS = EnumSet.of(SqlObjectDoc.Kind.TRIGGER);
    private static final Set<SqlObjectDoc.Kind> PROCEDURAL_BLOCKS = EnumSet.of(SqlObjectDoc.Kind.DO_BLOCK);
    private static final Set<SqlObjectDoc.Kind> DOCUMENTED_ALTERS = EnumSet.of(SqlObjectDoc.Kind.ALTER_TABLE);
    private static final Set<SqlObjectDoc.Kind> INFRASTRUCTURE = EnumSet.of(
            SqlObjectDoc.Kind.EXTENSION, SqlObjectDoc.Kind.SCHEMA);

    // Full schema, used for cross-page links and lookups.
    private final SchemaDoc schemaDoc;
    // Filtered schema rendered by the current page.
    private final SchemaDoc visibleSchemaDoc;
    private final List<SqlFileDoc> sourceFiles;
    private final Path currentPage;
    private final DocRegion region;
    private final PageResolver pageResolver = new PageResolver();

    public SchemaDocView(List<Path> sqlFiles, SchemaDoc schemaDoc) {
        this(schemaDoc, schemaDoc, sourceFiles(sqlFiles, schemaDoc), null, null);
    }

    private SchemaDocView(
            SchemaDoc schemaDoc,
            SchemaDoc visibleSchemaDoc,
            List<SqlFileDoc> sourceFiles,
            Path currentPage,
            DocRegion region) {
        this.schemaDoc = schemaDoc;
        this.visibleSchemaDoc = visibleSchemaDoc;
        this.sourceFiles = List.copyOf(sourceFiles);
        this.currentPage = currentPage;
        this.region = region;
    }

    public SchemaDoc schema() {
        return visibleSchemaDoc;
    }

    public SchemaDoc fullSchema() {
        return schemaDoc;
    }

    public SchemaDocView forSourceFile(SqlFileDoc sourceFile) {
        return new SchemaDocView(
                schemaDoc,
                new SchemaDoc(
                        allTablesFor(sourceFile),
                        allSqlObjectsFor(sourceFile),
                        List.of(sourceFile),
                        upgradeChangesFor(sourceFile)),
                List.of(sourceFile),
                pageResolver.scriptPagePath(sourceFile.path()),
                null);
    }

    public SchemaDocView forRegion(SqlFileDoc sourceFile, DocRegion region) {
        return new SchemaDocView(
                schemaDoc,
                new SchemaDoc(
                        tablesFor(sourceFile, region),
                        sqlObjectsFor(sourceFile, region),
                        List.of(sourceFile),
                        List.of()),
                List.of(sourceFile),
                pageResolver.postgresRegionPagePath(region),
                region);
    }

    public Path currentPage() {
        return currentPage;
    }

    /**
     * Returns true for postgres.sql when it has explicit documentation regions and should therefore be rendered as a
     * landing page plus region pages.
     */
    public boolean isSplitScript(SqlFileDoc sourceFile) {
        return sourceFile.category() == Category.INITIAL_SCHEMA
                && sourceFile.path().getFileName().toString().equals("postgres.sql")
                && hasExplicitRegions(sourceFile);
    }

    public Path postgresRegionPagePath(DocRegion region) {
        return pageResolver.postgresRegionPagePath(region);
    }

    public Path pageFor(TableDoc table) {
        return pageFor(table.sourceFile(), regionFor(table));
    }

    public Path pageFor(SqlObjectDoc object) {
        return pageFor(object.sourceFile(), regionFor(object));
    }

    public List<DocRegion> postgresRegions() {
        if (sourceFiles.isEmpty()) {
            return List.of();
        }
        return postgresRegions(sourceFiles.get(0));
    }

    private List<DocRegion> postgresRegions(SqlFileDoc sourceFile) {
        Map<String, DocRegion> regionsBySlug = new LinkedHashMap<>();
        for (TableDoc table : allTablesFor(sourceFile)) {
            addRegion(regionsBySlug, regionFor(table));
        }
        for (SqlObjectDoc object : allSqlObjectsFor(sourceFile)) {
            addRegion(regionsBySlug, regionFor(object));
        }

        return regionsBySlug.values().stream()
                .sorted(Comparator
                        .comparingInt(DocRegion::order)
                        .thenComparing(DocRegion::title))
                .toList();
    }

    public List<SqlFileDoc> initialSourceFiles() {
        return sourceFiles.stream()
                .filter(sourceFile -> sourceFile.category() == Category.INITIAL_SCHEMA)
                .toList();
    }

    public List<SqlFileDoc> upgradeSourceFiles() {
        return sourceFiles.stream()
                .filter(sourceFile -> sourceFile.category() == Category.UPGRADE)
                .toList();
    }

    public List<TableDoc> tablesFor(SqlFileDoc sourceFile) {
        return allTablesFor(sourceFile).stream()
                .filter(table -> region == null || regionFor(table).equals(region))
                .toList();
    }

    private List<TableDoc> tablesFor(SqlFileDoc sourceFile, DocRegion region) {
        return allTablesFor(sourceFile).stream()
                .filter(table -> regionFor(table).equals(region))
                .toList();
    }

    public List<SqlObjectDoc> viewsFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, VIEWS);
    }

    public List<SqlObjectDoc> enumTypesFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, ENUM_TYPES);
    }

    public List<SqlObjectDoc> routinesFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, ROUTINES);
    }

    public List<SqlObjectDoc> triggersFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, TRIGGERS);
    }

    public List<SqlObjectDoc> infrastructureObjectsFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, INFRASTRUCTURE);
    }

    public List<SqlObjectDoc> proceduralBlocksFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, PROCEDURAL_BLOCKS);
    }

    public List<SqlObjectDoc> documentedAltersFor(SqlFileDoc sourceFile) {
        return objectsFor(sourceFile, DOCUMENTED_ALTERS);
    }

    private List<UpgradeChangeDoc> upgradeChangesFor(SqlFileDoc sourceFile) {
        return schemaDoc.upgradeChanges().stream()
                .filter(change -> samePath(change.sourceFile(), sourceFile.path()))
                .sorted(Comparator.comparingInt(UpgradeChangeDoc::numericChangeNumber).reversed())
                .toList();
    }

    public List<SqlObjectDoc> views() {
        return objects(VIEWS);
    }

    public List<SqlObjectDoc> enumTypes() {
        return objects(ENUM_TYPES);
    }

    public List<SqlObjectDoc> routines() {
        return objects(ROUTINES);
    }

    public List<SqlObjectDoc> triggers() {
        return objects(TRIGGERS);
    }

    public List<SqlObjectDoc> extensions() {
        return objects(EnumSet.of(SqlObjectDoc.Kind.EXTENSION));
    }

    public List<SqlObjectDoc> schemas() {
        return objects(EnumSet.of(SqlObjectDoc.Kind.SCHEMA));
    }

    public List<SqlObjectDoc> proceduralBlocks() {
        return objects(PROCEDURAL_BLOCKS);
    }

    public List<SqlObjectDoc> documentedAlters() {
        return objects(DOCUMENTED_ALTERS);
    }

    public DocRegion regionFor(TableDoc table) {
        return table.region() != null ? table.region() : DocRegion.UNCLASSIFIED;
    }

    public DocRegion regionFor(SqlObjectDoc object) {
        return object.region() != null ? object.region() : DocRegion.UNCLASSIFIED;
    }

    private boolean hasExplicitRegions(SqlFileDoc sourceFile) {
        for (TableDoc table : allTablesFor(sourceFile)) {
            if (table.region() != null) {
                return true;
            }
        }
        for (SqlObjectDoc object : allSqlObjectsFor(sourceFile)) {
            if (object.region() != null) {
                return true;
            }
        }
        return false;
    }

    private List<TableDoc> allTablesFor(SqlFileDoc sourceFile) {
        return schemaDoc.tables().stream()
                .filter(table -> samePath(table.sourceFile(), sourceFile.path()))
                .toList();
    }

    private List<SqlObjectDoc> allSqlObjectsFor(SqlFileDoc sourceFile) {
        return schemaDoc.sqlObjects().stream()
                .filter(object -> samePath(object.sourceFile(), sourceFile.path()))
                .toList();
    }

    private List<SqlObjectDoc> sqlObjectsFor(SqlFileDoc sourceFile, DocRegion region) {
        return allSqlObjectsFor(sourceFile).stream()
                .filter(object -> regionFor(object).equals(region))
                .toList();
    }

    private Path pageFor(Path sourcePath, DocRegion region) {
        SqlFileDoc sourceFile = sourceFile(sourcePath);
        return pageResolver.pageFor(
                sourcePath,
                sourceFile != null && isSplitScript(sourceFile),
                region);
    }

    private List<SqlObjectDoc> objectsFor(SqlFileDoc sourceFile, Set<SqlObjectDoc.Kind> kinds) {
        return allSqlObjectsFor(sourceFile).stream()
                .filter(object -> region == null || regionFor(object).equals(region))
                .filter(object -> kinds.contains(object.kind()))
                .toList();
    }

    private List<SqlObjectDoc> objects(Set<SqlObjectDoc.Kind> kinds) {
        return visibleSchemaDoc.sqlObjects().stream()
                .filter(object -> kinds.contains(object.kind()))
                .toList();
    }

    private void addRegion(Map<String, DocRegion> regionsBySlug, DocRegion region) {
        regionsBySlug.putIfAbsent(region.slug(), region);
    }

    private SqlFileDoc sourceFile(Path path) {
        for (SqlFileDoc sourceFile : schemaDoc.sourceFiles()) {
            if (samePath(sourceFile.path(), path)) {
                return sourceFile;
            }
        }
        return null;
    }

    private boolean samePath(Path left, Path right) {
        return left.toAbsolutePath().normalize().equals(right.toAbsolutePath().normalize());
    }

    private static List<SqlFileDoc> sourceFiles(List<Path> sqlFiles, SchemaDoc schemaDoc) {
        if (!schemaDoc.sourceFiles().isEmpty()) {
            return schemaDoc.sourceFiles();
        }

        List<SqlFileDoc> sourceFiles = new ArrayList<>();
        for (Path sqlFile : sqlFiles) {
            sourceFiles.add(new SqlFileDoc(sqlFile, SqlFileDoc.category(sqlFile)));
        }
        return sourceFiles;
    }
}

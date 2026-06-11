/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.tools.dbdocs.model.DocRegion;
import com.evolveum.midpoint.tools.dbdocs.model.SchemaDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc.Category;

/**
 * Coordinates Velocity template rendering for generated database schema documentation.
 *
 * The renderer prepares template contexts and chooses the correct template for each generated page:
 * the main overview, per-script pages, upgrade-script pages, and region pages for large split scripts.
 * Formatting and xref creation are delegated to {@link AsciiDocFormat}, while page path decisions are
 * delegated to {@link PageResolver}.
 */
public class DbSchemaAsciiDocRenderer {

    public static final String OUTPUT_FILE_NAME = "schema.adoc";
    public static final String SCRIPTS_DIRECTORY = "scripts";

    private final VelocityTemplateRenderer templateRenderer = new VelocityTemplateRenderer();
    private final AsciiDocFormat format = new AsciiDocFormat();
    private final PageResolver pageResolver = new PageResolver();

    /**
     * Renders the main schema overview page.
     */
    public List<String> renderLandingPage(List<Path> sqlFiles, SchemaDoc schemaDoc) {
        Map<String, Object> context = baseContext(new SchemaDocView(sqlFiles, schemaDoc));
        return renderTemplate("schema-index.adoc.vm", context);
    }

    /**
     * Renders documentation for a single SQL script.
     */
    public List<String> renderScriptPage(SqlFileDoc sourceFile, SchemaDoc schemaDoc) {
        SchemaDocView view = new SchemaDocView(List.of(sourceFile.path()), schemaDoc).forSourceFile(sourceFile);
        Map<String, Object> context = scriptContext(
                view,
                sourceFile,
                "Native PostgreSQL Schema Script: " + format.fileName(sourceFile.path()),
                "../schema.adoc",
                null,
                null);

        if (sourceFile.category() == Category.UPGRADE) {
            return renderTemplate("script-upgrade.adoc.vm", context);
        }

        return renderTemplate("script-detail.adoc.vm", context);
    }

    /**
     * Renders all generated pages for a single SQL script.
     */
    public List<RenderedAsciiDocPage> renderScriptPages(SqlFileDoc sourceFile, SchemaDoc schemaDoc) {
        SchemaDocView schemaView = new SchemaDocView(List.of(sourceFile.path()), schemaDoc);
        if (sourceFile.category() == Category.UPGRADE) {
            return renderUpgradeScriptPages(sourceFile, schemaDoc);
        }

        if (!schemaView.isSplitScript(sourceFile)) {
            return List.of(new RenderedAsciiDocPage(
                    pageResolver.scriptPagePath(sourceFile.path()),
                    renderScriptPage(sourceFile, schemaDoc)));
        }

        SchemaDocView scriptView = schemaView.forSourceFile(sourceFile);
        List<RenderedAsciiDocPage> pages = new ArrayList<>();

        Map<String, Object> landingContext = scriptContext(
                scriptView,
                sourceFile,
                "Native PostgreSQL Schema Script: " + format.fileName(sourceFile.path()),
                "../schema.adoc",
                null,
                null);
        pages.add(new RenderedAsciiDocPage(
                pageResolver.scriptPagePath(sourceFile.path()),
                renderTemplate("script-postgres-index.adoc.vm", landingContext)));

        for (DocRegion region : scriptView.postgresRegions()) {
            SchemaDocView regionView = scriptView.forRegion(sourceFile, region);
            Map<String, Object> regionContext = scriptContext(
                    regionView,
                    sourceFile,
                    "Native PostgreSQL Schema Script: postgres.sql - " + region.title(),
                    "../../schema.adoc",
                    "../postgres.adoc",
                    region);
            pages.add(new RenderedAsciiDocPage(
                    regionView.postgresRegionPagePath(region),
                    renderTemplate("region.adoc.vm", regionContext)));
        }

        return pages;
    }

    /**
     * Renders the main upgrade-script page and one detail page for each midPoint version.
     */
    private List<RenderedAsciiDocPage> renderUpgradeScriptPages(SqlFileDoc sourceFile, SchemaDoc schemaDoc) {
        SchemaDocView scriptView = new SchemaDocView(List.of(sourceFile.path()), schemaDoc).forSourceFile(sourceFile);
        List<RenderedAsciiDocPage> pages = new ArrayList<>();

        pages.add(new RenderedAsciiDocPage(
                pageResolver.scriptPagePath(sourceFile.path()),
                renderScriptPage(sourceFile, schemaDoc)));

        for (SchemaDocView.UpgradeVersionChanges versionChanges : scriptView.upgradeChangesByMidpointVersion()) {
            SchemaDocView versionView = scriptView.forUpgradeVersion(sourceFile, versionChanges.midpointVersion());
            Map<String, Object> versionContext = scriptContext(
                    versionView,
                    sourceFile,
                    "Native PostgreSQL Upgrade Script: " + format.fileName(sourceFile.path())
                            + " - midPoint " + versionChanges.midpointVersion(),
                    "../../schema.adoc",
                    "../" + PageResolver.scriptOutputFileName(sourceFile.path()),
                    null);
            versionContext.put("versionChanges", versionChanges);
            pages.add(new RenderedAsciiDocPage(
                    pageResolver.upgradeVersionPagePath(sourceFile.path(), versionChanges.midpointVersion()),
                    renderTemplate("script-upgrade-version.adoc.vm", versionContext)));
        }

        return pages;
    }

    private Map<String, Object> baseContext(SchemaDocView view) {
        Map<String, Object> context = new HashMap<>();
        context.put("schema", view.schema());
        context.put("view", view);
        context.put("r", format);
        return context;
    }

    private Map<String, Object> scriptContext(
            SchemaDocView view,
            SqlFileDoc sourceFile,
            String pageTitle,
            String backToOverviewPath,
            String scriptLandingPath,
            DocRegion region) {
        Map<String, Object> context = baseContext(view);
        context.put("sourceFile", sourceFile);
        context.put("pageTitle", pageTitle);
        context.put("backToOverviewPath", backToOverviewPath);
        context.put("scriptLandingPath", scriptLandingPath);
        context.put("region", region);
        return context;
    }

    private List<String> renderTemplate(String templateName, Map<String, Object> context) {
        return templateRenderer.render(templateName, context).lines().toList();
    }

}

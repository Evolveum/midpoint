/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.tools.dbdocs.render;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.evolveum.midpoint.tools.dbdocs.model.ColumnDoc;
import com.evolveum.midpoint.tools.dbdocs.model.ForeignKeyDoc;
import com.evolveum.midpoint.tools.dbdocs.model.IndexDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlFileDoc;
import com.evolveum.midpoint.tools.dbdocs.model.SqlObjectDoc;
import com.evolveum.midpoint.tools.dbdocs.model.TableDoc;
import com.evolveum.midpoint.tools.dbdocs.model.UpgradeChangeDoc;

/**
 * Formatting helpers exposed to Velocity templates.
 *
 * This class should contain presentation-only logic: xrefs, labels, anchors,
 * fallback text, and small display formatting helpers. Schema grouping and page
 * selection should stay in SchemaDocView.
 */
public class AsciiDocFormat {

    public String scriptPageLink(Path sourceFile) {
        return DbSchemaAsciiDocRenderer.SCRIPTS_DIRECTORY + "/" + PageResolver.scriptOutputFileName(sourceFile);
    }

    public String scriptXref(SqlFileDoc sourceFile) {
        return scriptXref(sourceFile.path());
    }

    public String scriptXref(Path sourceFile) {
        return "xref:" + scriptPageLink(sourceFile) + "[`" + sourceFile.getFileName() + "`]";
    }

    public String pageXref(Path targetPage, String label, SchemaDocView view) {
        return "xref:" + relativePageLink(targetPage, view) + "[" + label + "]";
    }

    public String tableXref(TableDoc table, SchemaDocView view) {
        return xref(view.pageFor(table), table.name(), table.name(), view);
    }

    public String objectXref(String kind, SqlObjectDoc object, SchemaDocView view) {
        return xref(view.pageFor(object), anchor(kind, object.name()), object.name(), view);
    }

    public String tableReference(String tableName, SchemaDocView view) {
        TableDoc table = findTable(tableName, view);
        if (table == null) {
            return localXref(tableName, tableName);
        }
        return xref(view.pageFor(table), table.name(), tableName, view);
    }

    public String routineReference(String routineName, SchemaDocView view) {
        SqlObjectDoc routine = findObject("routine", routineName, view);
        if (routine == null) {
            return localXref(anchor("routine", routineName), routineName);
        }
        return xref(view.pageFor(routine), anchor("routine", routine.name()), routine.name(), view);
    }

    public String fileName(Path path) {
        return path.getFileName().toString();
    }

    /**
     * Returns a stable AsciiDoc anchor for the object kind and name.
     */
    public String anchor(String kind, String name) {
        String normalizedName = name.replaceAll("[^A-Za-z0-9_:-]", "_");
        if (normalizedName.startsWith(kind + "-")) {
            return normalizedName;
        }
        return kind + "_" + normalizedName;
    }

    public String description(String value) {
        return metadataValue(value, "No description available yet.");
    }

    public String metadataValue(String value) {
        return metadataValue(value, "");
    }

    public String changeText(UpgradeChangeDoc change) {
        if (change.metadata().change() != null) {
            return change.metadata().change();
        }

        return metadataValue(change.metadata().description());
    }

    public String upgradeChangeAnchor(UpgradeChangeDoc change) {
        if (change.changeNumber() != null && !change.changeNumber().isBlank() && !"-".equals(change.changeNumber())) {
            return "change-" + change.changeNumber();
        }

        String text = changeText(change);
        return "change-unnumbered-" + Integer.toHexString(text != null ? text.hashCode() : 0);
    }

    public String upgradeChangeLabel(UpgradeChangeDoc change) {
        if (change.changeNumber() != null && !change.changeNumber().isBlank() && !"-".equals(change.changeNumber())) {
            return "Change " + change.changeNumber();
        }

        return "Unnumbered change";
    }

    public String upgradeChangeNumber(UpgradeChangeDoc change) {
        return metadataValue(change.changeNumber(), "-");
    }

    public String upgradeChangeScript(UpgradeChangeDoc change) {
        return fileName(change.sourceFile());
    }

    public String constraints(ColumnDoc column) {
        if (column.constraints().isEmpty()) {
            return "";
        }

        return String.join(", ", column.constraints());
    }

    /**
     * Returns rendered index columns including method when present.
     */
    public String indexColumns(IndexDoc index) {
        if (index.method() == null || index.method().isBlank()) {
            return index.columns();
        }

        return "USING " + index.method() + " (" + index.columns() + ")";
    }

    public String indexNotes(IndexDoc index) {
        List<String> notes = new ArrayList<>();
        if (index.metadata().description() != null) {
            notes.add(trimTrailingPeriod(index.metadata().description()));
        }
        if (index.metadata().usedFor() != null) {
            notes.add("Used for: " + index.metadata().usedFor());
        }
        if (index.unique()) {
            notes.add("unique");
        }
        return String.join(". ", notes);
    }

    /**
     * Returns rendered foreign-key target xref, resolving the anchor against documented tables case-insensitively.
     */
    public String foreignKeyReference(ForeignKeyDoc foreignKey, SchemaDocView view) {
        String label = foreignKeyLabel(foreignKey);
        TableDoc table = findTable(foreignKey.referencedTable(), view);
        if (table == null) {
            return "xref:#" + foreignKey.referencedTable() + "[`" + label + "`]";
        }
        return xref(view.pageFor(table), table.name(), label, view);
    }

    public String foreignKeyNotes(ForeignKeyDoc foreignKey) {
        if (foreignKey.deleteRule() == null) {
            return "-";
        }

        return "ON DELETE " + foreignKey.deleteRule();
    }

    public boolean hasForeignKeyNotes(TableDoc table) {
        for (ForeignKeyDoc foreignKey : table.foreignKeys()) {
            if (foreignKey.deleteRule() != null) {
                return true;
            }
        }

        return false;
    }

    public String viewType(SqlObjectDoc view) {
        return view.kind() == SqlObjectDoc.Kind.MATERIALIZED_VIEW ? "materialized view" : "view";
    }

    /**
     * Returns the rendered schema object kind.
     */
    public String objectType(SqlObjectDoc object) {
        return switch (object.kind()) {
            case FUNCTION -> "function";
            case PROCEDURE -> "procedure";
            case TRIGGER -> "trigger";
            case EXTENSION -> "extension";
            case SCHEMA -> "schema";
            case ENUM_TYPE -> "enum type";
            case MATERIALIZED_VIEW -> "materialized view";
            case VIEW -> "view";
            case DO_BLOCK -> "DO block";
            case ALTER_TABLE -> "ALTER TABLE";
        };
    }

    /**
     * Returns a display path relative to the current directory when possible.
     */
    public String displayPath(Path path) {
        Path normalizedPath = path.toAbsolutePath().normalize();
        Path currentDirectory = Path.of("").toAbsolutePath().normalize();

        if (normalizedPath.startsWith(currentDirectory)) {
            return currentDirectory.relativize(normalizedPath).toString().replace('\\', '/');
        }

        return normalizedPath.toString().replace('\\', '/');
    }

    private String metadataValue(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private String localXref(String anchor, String label) {
        return "xref:#" + anchor + "[`" + label + "`]";
    }

    private String trimTrailingPeriod(String value) {
        String stripped = value.stripTrailing();
        if (stripped.endsWith(".")) {
            return stripped.substring(0, stripped.length() - 1);
        }

        return stripped;
    }

    /**
     * Builds an xref, using a local anchor when the target is on the currently rendered page.
     */
    private String xref(Path targetPage, String anchor, String label, SchemaDocView view) {
        Path currentPage = view.currentPage();
        if (currentPage != null && currentPage.equals(targetPage)) {
            return localXref(anchor, label);
        }

        return "xref:" + relativePageLink(targetPage, view) + "#" + anchor + "[`" + label + "`]";
    }

    private String relativePageLink(Path targetPage, SchemaDocView view) {
        Path currentPage = view.currentPage();
        Path link = targetPage;
        if (currentPage != null && currentPage.getParent() != null) {
            link = currentPage.getParent().relativize(targetPage);
        }
        return link.toString().replace('\\', '/');
    }

    private TableDoc findTable(String referencedTable, SchemaDocView view) {
        for (TableDoc table : view.fullSchema().tables()) {
            if (table.name().equalsIgnoreCase(referencedTable)) {
                return table;
            }
        }
        return null;
    }

    private String foreignKeyLabel(ForeignKeyDoc foreignKey) {
        return foreignKey.referencedColumn() != null
                ? foreignKey.referencedTable() + "." + foreignKey.referencedColumn()
                : foreignKey.referencedTable();
    }

    private SqlObjectDoc findObject(String kind, String name, SchemaDocView view) {
        SqlObjectDoc.Kind expectedKind = expectedKind(kind);

        for (SqlObjectDoc object : view.fullSchema().sqlObjects()) {
            if (kindMatches(object, expectedKind) && object.name().equalsIgnoreCase(name)) {
                return object;
            }
        }
        return null;
    }

    private SqlObjectDoc.Kind expectedKind(String kind) {
        return switch (kind.toLowerCase(Locale.ROOT)) {
            case "view" -> SqlObjectDoc.Kind.VIEW;
            case "enum" -> SqlObjectDoc.Kind.ENUM_TYPE;
            case "trigger" -> SqlObjectDoc.Kind.TRIGGER;
            case "extension" -> SqlObjectDoc.Kind.EXTENSION;
            case "schema" -> SqlObjectDoc.Kind.SCHEMA;
            default -> null;
        };
    }

    private boolean kindMatches(SqlObjectDoc object, SqlObjectDoc.Kind expectedKind) {
        if (expectedKind == null) {
            return object.kind() == SqlObjectDoc.Kind.FUNCTION || object.kind() == SqlObjectDoc.Kind.PROCEDURE;
        }

        return object.kind() == expectedKind
                || expectedKind == SqlObjectDoc.Kind.VIEW && object.kind() == SqlObjectDoc.Kind.MATERIALIZED_VIEW;
    }
}

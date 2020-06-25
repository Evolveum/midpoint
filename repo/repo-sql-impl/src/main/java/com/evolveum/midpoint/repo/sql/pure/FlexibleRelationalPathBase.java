package com.evolveum.midpoint.repo.sql.pure;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.dsl.SimplePath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

/**
 * Extension of {@link RelationalPathBase} that provides midPoint specific features.
 * This allows to add metadata about Prism objects in addition to pure DB metadata.
 * <p>
 * Typical migration from originally generated Querydsl class:
 * <ul>
 * <li>extend from this class instead of {@code RelationalPathBase}</li>
 * <li>rename fields as needed (missing uppercase for words), also in related bean</li>
 * <li>rename static final field to capitalized table name (this will stand out + fix checkstyle)</li>
 * <li>simplify bean to public fields and no setters/getters</li>
 * <li>add PK-based equals/hashCode to beans (not critical, but handy for grouping transformations)</li>
 * <li>TODO: add prism-related metadata?</li>
 * <li>TODO: change types as needed, e.g. date/time-related? (also in bean)</li>
 * </ul>
 *
 * @param <T> entity type - typically a pure DTO bean for the table mapped by Q-type
 */
public abstract class FlexibleRelationalPathBase<T> extends RelationalPathBase<T> {

    private static final long serialVersionUID = -3374516272567011096L;

    private Set<String> knownColumns = new HashSet<>();

    public FlexibleRelationalPathBase(
            Class<? extends T> type, PathMetadata metadata, String schema, String table) {
        super(type, metadata, schema, table);
    }

    protected void applyExtensions(ExtensionColumns extensionColumns) {
        for (Map.Entry<String, ColumnMetadata> entry : extensionColumns.extensionColumnsMetadata()) {
            SimplePath<?> path = createSimple(entry.getKey(), Object.class);
            // no need to add to knownColumns anymore
            super.addMetadata(path, entry.getValue());
        }
    }

    /**
     * Adds extension columns to the internal metadata of {@link RelationalPathBase} superclass.
     * Must be called from the subclass constructor, otherwise path fields are not registered yet.
     */
    protected void fixMetadata() {
        for (Map.Entry<String, ColumnMetadata> entry : getTableMetamodel().columnMetadataEntries()) {
            String columnName = entry.getKey();
            if (!knownColumns.contains(columnName)) {
                System.out.println("Adding extension column: " + columnName);
                SimplePath<?> simple = createSimple(columnName, Object.class);
                // no need to add to knownColumns anymore
                super.addMetadata(simple, entry.getValue());
            }
        }
        knownColumns = null;
    }

    @Override
    protected <P extends Path<?>> P addMetadata(P path, ColumnMetadata metadata) {

        return super.addMetadata(path, metadata);
    }

    protected abstract SqlTableMetamodel<?> getTableMetamodel();
}

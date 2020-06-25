/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static com.querydsl.core.types.PathMetadataFactory.forVariable;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Types;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.*;
import org.testng.annotations.Test;

import com.evolveum.midpoint.repo.sql.pure.FlexibleRelationalPathBase;
import com.evolveum.midpoint.repo.sql.pure.SqlTableMetamodel;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;

/**
 * Tests of Q-classes behavior and their support of extension columns.
 * Q-classes have two different set of metadata:
 * <ul>
 *     <li>The static one that is added by midPoint,
 *     typically accessible via static {@code METAMODEL} field.</li>
 *     <li>The non-static metadata managed by Querydsl, providing methods like
 *     {@link RelationalPath#getColumns()} or {@link RelationalPath#getMetadata(Path)}.</li>
 * </ul>
 * Both these models must be in sync.
 * <p>
 * For non-static metadata are mapped paired with {@link Path}, which normally designates property
 * on the matching bean, but for dynamic extension attributes column name is used as property name.
 * This is fine for SQLs not selecting the entity via single entity path but using the column paths.
 */
public class QSqlMetamodelTest extends AbstractUnitTest {

    // Demo entity, implemented without backing bean (has itself as type parameter)
    private static class QTestEntity extends FlexibleRelationalPathBase<QTestEntity> {

        // two fixed columns, extensions will be added later
        public static final ColumnMetadata ID =
                ColumnMetadata.named("ID").ofType(Types.BIGINT).withSize(19).notNull();
        public static final ColumnMetadata NAME =
                ColumnMetadata.named("NAME").ofType(Types.VARCHAR).withSize(255);

        // TODO consider where this metamodel should go and whether it should have all columns or just extensions (ExtensionColumns class)
        // metamodel with pre-defined columns, shared for all instances of this Q-class
        public static final SqlTableMetamodel<QTestEntity> METAMODEL =
                new SqlTableMetamodel<>("TEST_TABLE", QTestEntity.class, ID, NAME);

        public final NumberPath<Long> id = addMetadata(createNumber("id", Long.class), ID);
        public final StringPath name = addMetadata(createString("name"), NAME);

        public QTestEntity(String variable) {
            super(QTestEntity.class, forVariable(variable), "SCHEMA", "TEST_TABLE");
            fixMetadata();
        }

        @Override
        protected SqlTableMetamodel<?> getTableMetamodel() {
            return METAMODEL;
        }
    }

    @Test
    public void test100EntityWithoutExtensions() {
        when("default entity is created");
        QTestEntity te = new QTestEntity("te");

        then("only predefined fields are in metamodels");
        assertThat(te.getTableMetamodel().columnNames()).hasSize(2);
        // internal metamodel mapping paths -> columns
        assertThat(te.getColumns()).hasSize(2);
    }

    @Test
    public void test200EntityWithExtensionRegisteredBeforeEntityPathCreation() {
        when("extension is registered and default entity is created afterwards");
        QTestEntity.METAMODEL.add(ColumnMetadata.named("EXT_INFO").ofType(Types.VARCHAR));
        QTestEntity te = new QTestEntity("te");

        then("metamodels contain extension column in addition to the predefined ones");
        assertThat(te.getTableMetamodel().columnNames()).hasSize(3);
        assertThat(te.getColumns()).hasSize(3);
    }

    @Test
    public void test300() {
        QTestEntity te = new QTestEntity("te");
        SQLQuery<?> query = new SQLQueryFactory(SQLTemplates.DEFAULT, null)
                .select(te.all())
                .from(te);
        System.out.println("query = " + query);

        // TODO check it has three columns in FROM
        query.getMetadata().getProjection();
    }
}

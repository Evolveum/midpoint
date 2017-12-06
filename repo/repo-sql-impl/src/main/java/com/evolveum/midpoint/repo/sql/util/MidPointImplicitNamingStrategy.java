/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.google.common.base.CaseFormat;
import org.apache.commons.lang.StringUtils;
import org.hibernate.boot.model.naming.*;
import org.hibernate.boot.model.source.spi.AttributePath;

import java.util.Arrays;

/**
 * Created by Viliam Repan (lazyman).
 *
 * Pure magic. Clean up necessary, same for annoations.
 */
public class MidPointImplicitNamingStrategy extends ImplicitNamingStrategyLegacyHbmImpl {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointImplicitNamingStrategy.class);

    private static final int MAX_LENGTH = 30;

    @Override
    public Identifier determinePrimaryTableName(ImplicitEntityNameSource source) {
        Identifier i = super.determinePrimaryTableName(source);
        LOGGER.trace("determinePrimaryTableName {} -> {}", source.getEntityNaming(), i);
        return i;
    }

    @Override
    public Identifier determineCollectionTableName(ImplicitCollectionTableNameSource source) {
        Identifier i = super.determineCollectionTableName(source);
        LOGGER.trace("determineCollectionTableName {} {} -> {}", source.getOwningEntityNaming(), source.getOwningPhysicalTableName(), i);
        return i;
    }

    @Override
    public Identifier determineForeignKeyName(ImplicitForeignKeyNameSource source) {
        Identifier i = super.determineForeignKeyName(source);
        LOGGER.trace("determineForeignKeyName {} {} -> {}", source.getTableName(), source.getColumnNames(), i);
        return i;
    }

    @Override
    public Identifier determineUniqueKeyName(ImplicitUniqueKeyNameSource source) {
        Identifier i = super.determineUniqueKeyName(source);
        LOGGER.trace("determineUniqueKeyName {} {} -> {}", source.getTableName(), source.getColumnNames(), i);
        return i;
    }

    @Override
    public Identifier determineIndexName(ImplicitIndexNameSource source) {
        Identifier i = super.determineIndexName(source);
        LOGGER.trace("determineIndexName {} {} -> {}", source.getTableName(), source.getColumnNames(), i);
        return i;
    }

    @Override
    public Identifier determineMapKeyColumnName(ImplicitMapKeyColumnNameSource source) {
        Identifier i = super.determineMapKeyColumnName(source);
        LOGGER.trace("determineMapKeyColumnName {} -> {}", source.getPluralAttributePath(), i);
        return i;
    }

    @Override
    public Identifier determineDiscriminatorColumnName(ImplicitDiscriminatorColumnNameSource source) {
        Identifier i = super.determineDiscriminatorColumnName(source);
        LOGGER.trace("determineDiscriminatorColumnName {} -> {}", source.getEntityNaming(), i);
        return i;
    }

    @Override
    public Identifier determineAnyDiscriminatorColumnName(ImplicitAnyDiscriminatorColumnNameSource source) {
        Identifier i = super.determineAnyDiscriminatorColumnName(source);
        LOGGER.trace("determineAnyDiscriminatorColumnName {} -> {}", source.getAttributePath(), i);
        return i;
    }

    @Override
    public Identifier determineAnyKeyColumnName(ImplicitAnyKeyColumnNameSource source) {
        Identifier i = super.determineAnyKeyColumnName(source);
        LOGGER.trace("determineAnyKeyColumnName {} -> {}", source.getAttributePath(), i);
        return i;
    }

    @Override
    public Identifier determineListIndexColumnName(ImplicitIndexColumnNameSource source) {
        Identifier i = super.determineListIndexColumnName(source);
        LOGGER.trace("determineListIndexColumnName {} -> {}", source.getPluralAttributePath(), i);
        return i;
    }

    @Override
    public Identifier determineIdentifierColumnName(ImplicitIdentifierColumnNameSource source) {
        Identifier i = super.determineIdentifierColumnName(source);
        LOGGER.trace("determineIdentifierColumnName {} {} -> {}", source.getEntityNaming(), source.getIdentifierAttributePath(), i);
        return i;
    }

    @Override
    public Identifier determinePrimaryKeyJoinColumnName(ImplicitPrimaryKeyJoinColumnNameSource source) {
        Identifier i = super.determinePrimaryKeyJoinColumnName(source);
        LOGGER.trace("determinePrimaryKeyJoinColumnName {} {} -> {}", source.getReferencedTableName(), source.getReferencedPrimaryKeyColumnName(), i);

        return i;
    }

    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
        Identifier i = super.determineJoinColumnName(source);

        // RObject, creatorRef.target, oid -> m_object.creatorRef_targetOid
        // RObjectReference, owner, oid -> m_object_reference.owner_oid

        AttributePath path = source.getAttributePath();
        String property = path.getProperty();
        String columnName = source.getReferencedColumnName().getText();

        Identifier real;
        if (path.getDepth() == 1) {
            real = toIdentifier(StringUtils.join(Arrays.asList(property, columnName), "_"),
                    source.getBuildingContext());
        } else {
            // TODO fixme BRUTAL HACK -- we are not able to eliminate columns like 'ownerRefCampaign_targetOid' from the schema (even with @AttributeOverride/@AssociationOverride)
            if ("ownerRefCampaign.target".equals(path.getFullPath()) ||
                    "ownerRefDefinition.target".equals(path.getFullPath()) ||
                    "ownerRefTask.target".equals(path.getFullPath())) {
                path = AttributePath.parse("ownerRef.target");
            }

            AttributePath parent = path.getParent();
            String translatedParent = transformAttributePath(parent);

            columnName = property + StringUtils.capitalize(columnName);

            real = toIdentifier(StringUtils.join(Arrays.asList(translatedParent, columnName), "_"), source.getBuildingContext());

        }

        LOGGER.trace("determineJoinColumnName {} {} -> {}, {}", source.getReferencedTableName(), source.getReferencedColumnName(), i, real);
        return real;
    }

    @Override
    public Identifier determineJoinTableName(ImplicitJoinTableNameSource source) {
        Identifier i = super.determineJoinTableName(source);
        LOGGER.trace("determineJoinTableName {} {}", source, i);
        return i;
    }

    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        String columnName = source.getAttributePath().getProperty();
        String fullPath = source.getAttributePath().getFullPath();

        String result;
        if (fullPath.startsWith("credentials.") || fullPath.startsWith("activation.")) {
            //credentials and activation are embedded and doesn't need to be qualified

            Identifier i = super.determineBasicColumnName(source);
            LOGGER.trace("determineBasicColumnName {} {}", fullPath, i);
            return i;
        } else {
            if (fullPath.contains("&&")) {
                // it's collection
                result = columnName;
            } else {
                result = fullPath.replaceAll("\\.", "_");
            }
        }
        result = RUtil.fixDBSchemaObjectNameLength(result);

        Identifier i = toIdentifier(result, source.getBuildingContext());
        LOGGER.trace("determineBasicColumnName {} {}", fullPath, i);
        return i;
    }
}

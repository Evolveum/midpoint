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

import org.apache.commons.lang.StringUtils;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.ImplicitBasicColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitJoinColumnNameSource;
import org.hibernate.boot.model.naming.ImplicitNamingStrategyLegacyHbmImpl;
import org.hibernate.boot.model.source.spi.AttributePath;

import java.util.Arrays;

/**
 * Created by Viliam Repan (lazyman).
 * <p>
 * Pure magic. Clean up necessary, same for annoations.
 */
public class MidPointImplicitNamingStrategy extends ImplicitNamingStrategyLegacyHbmImpl {

    @Override
    public Identifier determineJoinColumnName(ImplicitJoinColumnNameSource source) {
        Identifier i = super.determineJoinColumnName(source);

        // RObject, creatorRef.target, oid -> m_object.creatorRef_targetOid
        // RObjectReference, owner, oid -> m_object_reference.owner_oid
        // RObjectReference, target, oid -> m_object_reference.targetOid

        AttributePath path = source.getAttributePath();
        String property = path.getProperty();
        String columnName = source.getReferencedColumnName().getText();

        Identifier real;
        if (path.getDepth() == 1) {
            String name;
            if (property.endsWith("target") && "oid".equals(columnName)) {
                name = property + "Oid";
            } else {
                name = StringUtils.join(Arrays.asList(property, columnName), "_");
            }

            real = toIdentifier(name, source.getBuildingContext());
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

        return real;
    }

    @Override
    public Identifier determineBasicColumnName(ImplicitBasicColumnNameSource source) {
        String columnName = source.getAttributePath().getProperty();
        String fullPath = source.getAttributePath().getFullPath();

        String result;
        if (fullPath.startsWith("credentials.") || fullPath.startsWith("activation.")) {
            //credentials and activation are embedded and doesn't need to be qualified

            return super.determineBasicColumnName(source);
        } else {
            if (fullPath.contains("&&")) {
                // it's collection
                result = columnName;
            } else {
                result = fullPath.replaceAll("\\.", "_");
            }
        }
        result = RUtil.fixDBSchemaObjectNameLength(result);

        return toIdentifier(result, source.getBuildingContext());
    }
}

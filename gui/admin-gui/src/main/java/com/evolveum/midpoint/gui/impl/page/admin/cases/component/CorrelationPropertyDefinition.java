/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.MatchingUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationPropertiesDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationPropertyDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class CorrelationPropertyDefinition implements Serializable {

    public static final String F_DISPLAY_NAME = "displayName";

    @Nullable private final CorrelationPropertyDefinitionType definitionBean;

    @NotNull private final ItemPath sourcePath;
    @NotNull private final ItemPath primaryTargetPath;
    @Nullable private final ItemPath secondaryTargetPath;

    @Nullable private final ItemDefinition<?> definition;

    private CorrelationPropertyDefinition(
            @Nullable CorrelationPropertyDefinitionType definitionBean,
            @NotNull ItemPath sourcePath,
            @NotNull ItemPath primaryTargetPath,
            @Nullable ItemPath secondaryTargetPath,
            @Nullable ItemDefinition<?> definition) {
        this.definitionBean = definitionBean;
        this.sourcePath = sourcePath;
        this.primaryTargetPath = primaryTargetPath;
        this.secondaryTargetPath = secondaryTargetPath;
        this.definition = definition;
    }

    static void fillFromConfiguration(
            @NotNull List<CorrelationPropertyDefinition> correlationProperties,
            @NotNull CorrelationPropertiesDefinitionType propertiesBean,
            @Nullable PrismObject<?> preFocus) {
        boolean mirror = !Boolean.FALSE.equals(propertiesBean.isMirrored());
        for (CorrelationPropertyDefinitionType propertyBean : propertiesBean.getProperty()) {
            ItemPath sourcePath = MiscUtil.requireNonNull(
                            propertyBean.getSource(),
                            () -> new IllegalArgumentException("No source item path"))
                    .getItemPath();

            ItemPath primaryTargetPath;
            ItemPath secondaryTargetPath = null;
            if (propertyBean.getPrimaryTarget() != null) {
                primaryTargetPath = propertyBean.getPrimaryTarget().getItemPath();
                if (propertyBean.getSecondaryTarget() != null) {
                    secondaryTargetPath = propertyBean.getSecondaryTarget().getItemPath();
                } else if (mirror) {
                    secondaryTargetPath = sourcePath;
                }
            } else {
                primaryTargetPath = sourcePath;
                if (propertyBean.getSecondaryTarget() != null) {
                    secondaryTargetPath = propertyBean.getSecondaryTarget().getItemPath();
                }
            }

            ItemDefinition<?> definition = preFocus != null ? preFocus.getDefinition().findItemDefinition(sourcePath) : null;
            correlationProperties.add(
                    new CorrelationPropertyDefinition(
                            propertyBean,
                            sourcePath,
                            primaryTargetPath,
                            secondaryTargetPath,
                            definition));
        }
    }

    static void fillFromObject(List<CorrelationPropertyDefinition> definitions, PrismObject<?> preFocus) {
        List<PrismProperty<?>> properties = MatchingUtil.getSingleValuedProperties(
                (ObjectType) preFocus.asObjectable());
        for (PrismProperty<?> property : properties) {
            ItemPath path = property.getPath().namedSegmentsOnly();
            if (!containsPath(definitions, path)) {
                definitions.add(
                        new CorrelationPropertyDefinition(
                                null,
                                path,
                                path,
                                null,
                                property.getDefinition()));
            }
        }
    }

    private static boolean containsPath(Collection<CorrelationPropertyDefinition> definitions, ItemPath path) {
        return definitions.stream()
                .anyMatch(def -> def.getSourcePath().equivalent(path));
    }

    public @Nullable CorrelationPropertyDefinitionType getDefinitionBean() {
        return definitionBean;
    }

    public @NotNull ItemPath getSourcePath() {
        return sourcePath;
    }

    public @NotNull ItemPath getPrimaryTargetPath() {
        return primaryTargetPath;
    }

    public @Nullable ItemPath getSecondaryTargetPath() {
        return secondaryTargetPath;
    }

    public @Nullable ItemDefinition<?> getDefinition() {
        return definition;
    }

    public @NotNull String getDisplayName() {
        if (definition != null) {
            if (definition.getDisplayName() != null) {
                return definition.getDisplayName();
            } else {
                return definition.getItemName().getLocalPart();
            }
        } else {
            return sourcePath.toString();
        }
    }
}

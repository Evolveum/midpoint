/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import jakarta.xml.bind.JAXBElement;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.MappingStrengthType.STRONG;

/**
 * Functionality common to all "mapping config items". In the form of a mixin, as the superclass is {@link ConfigurationItem}.
 *
 * @param <M> Type of the mapping bean.
 */
public interface AbstractMappingConfigItem<M extends AbstractMappingType> extends ConfigurationItemable<M> {

    /** See LensUtil.setMappingTarget */
    default @NotNull <CI extends ConfigurationItem<M>> CI setTargetIfMissing(@NotNull ItemPath path, Class<CI> clazz) {
        VariableBindingDefinitionType existingTarget = value().getTarget();
        M updatedBean;
        if (existingTarget == null) {
            //noinspection unchecked
            updatedBean = (M) CloneUtil.cloneIfImmutable(value())
                    .target(new VariableBindingDefinitionType().path(new ItemPathType(path)));

        } else if (existingTarget.getPath() == null) {
            //noinspection unchecked
            updatedBean = (M) CloneUtil.cloneIfImmutable(value())
                    .target(existingTarget.clone().path(new ItemPathType(path)));
        } else {
            return this.as(clazz);
        }

        return ConfigurationItem.of(updatedBean, origin()).as(clazz);
    }

    default @Nullable String getName() {
        return value().getName();
    }

    default boolean isStrong() {
        return value().getStrength() == STRONG;
    }

    default @Nullable ItemPath getTargetPath() {
        if (value().getTarget() != null && value().getTarget().getPath() != null) {
            return value().getTarget().getPath().getItemPath();
        }
        return null;
    }

    default void setDefaultStrong() {
        if (value().getStrength() == null) {
            value().setStrength(STRONG);
        }
    }

    default void setDefaultRelativityAbsolute() {
        ExpressionType expression = value().getExpression();
        if (expression == null) {
            return;
        }
        for (JAXBElement<?> evaluator : expression.getExpressionEvaluator()) {
            Object evaluatorValue = evaluator.getValue();
            if (evaluatorValue instanceof TransformExpressionEvaluatorType transform) {
                if (transform.getRelativityMode() == null) {
                    transform.setRelativityMode(TransformExpressionRelativityModeType.ABSOLUTE);
                }
            }
        }
    }

    default boolean isEnabled() {
        return BooleanUtils.isNotFalse(value().isEnabled());
    }
}

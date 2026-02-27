/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.util.HashMap;
import java.util.Map;

import org.apache.wicket.validation.IValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

@Component
public class ValidatorRegistry implements BeanPostProcessor {

    private final Map<String, ItemValidatorFactory<?>> validators = new HashMap<>();

    @Override
    public @Nullable Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (!(bean instanceof ItemValidatorFactory<?> ivf)) {
            return bean;
        }

        validators.put(ivf.getIdentifier(), ivf);

        return bean;
    }

    public IValidator<?> getValidator(String validatorId, ItemValidationContext context) {
        if (validatorId == null) {
            return null;
        }

        ItemValidatorFactory<?> factory = validators.get(validatorId);
        if (factory != null) {
            return factory.createValidatorInstance(context);
        }

        return null;
    }
}

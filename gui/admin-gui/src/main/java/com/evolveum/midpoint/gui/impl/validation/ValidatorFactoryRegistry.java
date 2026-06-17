/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.web.component.prism.InputPanel;

@Component
public class ValidatorFactoryRegistry implements BeanPostProcessor {

    private final Map<String, ItemValidatorFactory> factories = new HashMap<>();

    @Override
    public @Nullable Object postProcessAfterInitialization(@NotNull Object bean, @NotNull String beanName) throws BeansException {
        if (!(bean instanceof ItemValidatorFactory ivf)) {
            return bean;
        }

        factories.put(ivf.getIdentifier(), ivf);

        return bean;
    }

    public void attachValidators(List<String> validators, InputPanel panel, ItemValidationContext context) {
        if (validators == null) {
            return;
        }

        for (String validatorId : validators) {
            ItemValidatorFactory validator = getFactory(validatorId);
            if (validator != null) {
                validator.attachValidator(panel, context);
            }
        }
    }

    private ItemValidatorFactory getFactory(String validatorId) {
        if (validatorId == null) {
            return null;
        }

        return factories.get(validatorId);
    }
}

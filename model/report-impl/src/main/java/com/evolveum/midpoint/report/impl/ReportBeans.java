/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Objects;

@Component
public class ReportBeans {

    private static ReportBeans instance;

    @PostConstruct
    public void init() {
        instance = this;
    }

    @NotNull public static ReportBeans get() {
        return Objects.requireNonNull(instance, "no instance");
    }

    @Autowired public ReportServiceImpl reportService;
    @Autowired public ExpressionFactory expressionFactory;
}

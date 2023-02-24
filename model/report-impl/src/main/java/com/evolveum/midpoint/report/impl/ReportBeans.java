/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.report.impl;

import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
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

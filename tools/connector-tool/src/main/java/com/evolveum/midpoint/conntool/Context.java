/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.conntool;

import org.jetbrains.annotations.NotNull;
import org.springframework.context.support.GenericXmlApplicationContext;

public class Context {

    // TODO use if we'd need something from midPoint
    private static final String[] CTX_MIDPOINT = new String[] {
            "classpath:ctx-common.xml"
    };

    @NotNull private final CommonOptions commonOptions;

    private GenericXmlApplicationContext context;

    Context(@NotNull CommonOptions commonOptions) {
        this.commonOptions = commonOptions;
    }

    void init() {
        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
//        ctx.load(CTX_MIDPOINT);
        ctx.refresh();

        context = ctx;

    }

    void destroy() {
        if (context != null) {
            context.close();
        }
    }

    @NotNull CommonOptions getCommonOptions() {
        return commonOptions;
    }
}

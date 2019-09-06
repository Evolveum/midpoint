/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.tools.layout;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;

import java.io.File;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MidPointWarLayoutFactory implements LayoutFactory {

    private static final String NAME = "midpoint-war";

    private String name;

    public MidPointWarLayoutFactory() {
        this(NAME);
    }

    public MidPointWarLayoutFactory(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Layout getLayout(File source) {
        return new MidPointWarLayout(name);
    }
}

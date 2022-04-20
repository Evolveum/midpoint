/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.launcher;

import java.io.File;

import org.springframework.boot.loader.tools.Layout;
import org.springframework.boot.loader.tools.LayoutFactory;

/**
 * Layout factory used in POM repackaging the application (JAR version).
 */
@SuppressWarnings("unused")
public class MidPointJarLayoutFactory implements LayoutFactory {

    @Override
    public Layout getLayout(File source) {
        return new MidPointJarLayout();
    }
}

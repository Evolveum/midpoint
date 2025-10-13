/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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

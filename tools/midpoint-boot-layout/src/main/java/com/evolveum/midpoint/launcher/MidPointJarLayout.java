/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.launcher;

import org.springframework.boot.loader.tools.Layouts;

public class MidPointJarLayout extends Layouts.Jar implements MidPointLayoutCommon {

    @Override
    public String getLauncherClassName() {
        return MidPointLauncher.class.getName();
    }
}

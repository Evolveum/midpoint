/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action;

import java.io.File;

public interface BasicExportOptions {

    File getOutput();

    boolean isOverwrite();

    boolean isZip();
}

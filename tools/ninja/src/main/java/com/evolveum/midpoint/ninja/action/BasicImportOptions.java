/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.ninja.action;

import java.io.File;
import java.util.Set;

import com.evolveum.midpoint.schema.constants.ObjectTypes;

public interface BasicImportOptions {
    File getInput();
    boolean isOverwrite();
    boolean isZip();
    Set<ObjectTypes> getType();
}

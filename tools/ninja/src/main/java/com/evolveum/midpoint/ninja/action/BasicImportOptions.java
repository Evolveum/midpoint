/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

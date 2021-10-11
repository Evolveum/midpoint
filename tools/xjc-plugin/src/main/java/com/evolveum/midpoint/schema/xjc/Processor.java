/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.xjc;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

/**
 * @author lazyman
 */
@FunctionalInterface
public interface Processor {

    boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws Exception;
}

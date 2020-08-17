/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.sun.tools.xjc.addon.apache_cxf.midpoint;

import com.evolveum.midpoint.schema.xjc.Processor;
import com.evolveum.midpoint.schema.xjc.clone.CloneProcessor;
import com.evolveum.midpoint.schema.xjc.schema.SchemaProcessor;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @author lazyman
 */
public class MidPointPlugin extends Plugin {

    private final Processor[] processors = { new SchemaProcessor(), new CloneProcessor() };

    @Override
    public String getOptionName() {
        return "Xmidpoint";
    }

    @Override
    public String getUsage() {
        return "-" + getOptionName();
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        boolean result = true;
        try {
            for (Processor processor : processors) {
                result &= processor.run(outline, opt, errorHandler);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new SAXException(ex.getMessage(), ex);
        }

        return result;
    }
}

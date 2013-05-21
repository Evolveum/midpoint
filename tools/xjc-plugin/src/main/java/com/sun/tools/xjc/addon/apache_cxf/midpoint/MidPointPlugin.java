/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private final Processor[] processors = {new SchemaProcessor(), new CloneProcessor()};

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

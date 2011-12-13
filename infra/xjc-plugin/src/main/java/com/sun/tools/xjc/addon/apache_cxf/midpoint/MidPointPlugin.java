/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.sun.tools.xjc.addon.apache_cxf.midpoint;

import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.Plugin;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

/**
 * @author lazyman
 */
public class MidPointPlugin extends Plugin {

    private com.evolveum.midpoint.infra.xjc.MidPointPlugin plugin =
            new com.evolveum.midpoint.infra.xjc.MidPointPlugin();

    @Override
    public String getOptionName() {
        return plugin.getOptionName();
    }

    @Override
    public String getUsage() {
        return plugin.getUsage();
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws SAXException {
        return plugin.run(outline, opt, errorHandler);
    }
}

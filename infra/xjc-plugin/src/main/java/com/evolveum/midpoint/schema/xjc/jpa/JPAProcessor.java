/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.xjc.jpa;

import com.evolveum.midpoint.schema.xjc.Processor;
import com.sun.codemodel.JDefinedClass;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 */
public class JPAProcessor implements Processor {

    private static final String PACKAGE_DATA = "com.evolveum.midpoint.repo.data";

    @Override
    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws Exception {
        if (1 == 1) {
            return true;
        }

        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();

        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            JDefinedClass definedClass = classOutline.implClass;

            if (definedClass.parentContainer() instanceof JDefinedClass) {
                //inner classes
                continue;
            }

            String packageName = definedClass.getPackage().name();
            if (!packageName.startsWith("com.evolveum.midpoint.xml.ns._public")) {
                continue;
            }

            packageName = PACKAGE_DATA + packageName.replaceFirst("com.evolveum.midpoint.xml.ns._public", "");
            JDefinedClass dataClass = outline.getCodeModel()._class(packageName + "." + definedClass.name());
        }

        return true;
    }
}

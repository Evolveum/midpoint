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

package com.evolveum.midpoint.infra.xjc;

import com.evolveum.midpoint.util.DOMUtil;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;

import java.util.List;

/**
 * Simple proof of concept for our custom XJC plugin.
 *
 * @author lazyman
 */
public class MidPointPlugin {

    public String getOptionName() {
        return "Xmidpoint";
    }

    public String getUsage() {
        return "-Xmidpoint";
    }

    public boolean run(Outline outline, Options options, ErrorHandler errorHandler) throws SAXException {
        //todo remove this if, it's here just till we create real implementation
        if (1 == 1) {
            return true;
        }

        for (ClassOutline classOutline : outline.getClasses()) {
            JFieldVar field = classOutline.implClass.field(JMod.PRIVATE, DOMUtil.class, "lazyman");
            List<JMethod> methods = (List) classOutline.implClass.methods();
            for (JMethod method : methods) {
                if (!method.name().equals("getFullName")) {
                    continue;
                }

                JBlock body = method.body();
                //do stuff with method body
            }
        }

        return true;
    }
}

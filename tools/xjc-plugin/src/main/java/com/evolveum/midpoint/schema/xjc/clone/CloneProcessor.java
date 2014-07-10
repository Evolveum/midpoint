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

package com.evolveum.midpoint.schema.xjc.clone;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.evolveum.midpoint.schema.xjc.schema.SchemaProcessor;
import com.evolveum.midpoint.schema.xjc.util.ProcessorUtils;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CClassInfo;
import com.sun.tools.xjc.model.nav.NClass;
import com.sun.tools.xjc.outline.ClassOutline;
import com.sun.tools.xjc.outline.Outline;
import net.sourceforge.ccxjc.PluginImpl;
import org.xml.sax.ErrorHandler;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author lazyman
 */
public class CloneProcessor implements Processor {

    private static final String METHOD_CLONE = "clone";

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws Exception {
        PluginImpl clonePlugin = new PluginImpl();
        clonePlugin.run(outline, opt, errorHandler);

        Set<Map.Entry<NClass, CClassInfo>> set = outline.getModel().beans().entrySet();
        for (Map.Entry<NClass, CClassInfo> entry : set) {
            ClassOutline classOutline = outline.getClazz(entry.getValue());
            if (isPrism(classOutline)) {
                removeConstructors(classOutline);
                removeCloneableMethod(classOutline);

                removePrivateStaticCopyMethods(classOutline);
                createCloneMethod(classOutline);
            }
        }

        return true;
    }

    private void removePrivateStaticCopyMethods(ClassOutline classOutline) {
        JDefinedClass impl = classOutline.implClass;
        Iterator<JMethod> methods = impl.methods().iterator();
        while (methods.hasNext()) {
            JMethod method = methods.next();
            if ((method.mods().getValue() & (JMod.PRIVATE | JMod.STATIC)) == 0) {
                continue;
            }

            if (method.name().startsWith("copy")) {
                methods.remove();
            }
        }
    }

    private void removeConstructors(ClassOutline classOutline) {
        JDefinedClass impl = classOutline.implClass;
        Iterator<JMethod> constructors = impl.constructors();
        while (constructors.hasNext()) {
            JMethod constructor = constructors.next();
            if (constructor.hasSignature(new JType[]{impl})
                    /* || constructor.hasSignature(new JType[]{}) */) {             // default constructor has to be kept there!
                constructors.remove();
            }
        }
    }

    private void removeCloneableMethod(ClassOutline classOutline) {
        JDefinedClass impl = classOutline.implClass;
        Iterator<JMethod> methods = impl.methods().iterator();
        while (methods.hasNext()) {
            JMethod method = methods.next();
            if ("clone".equals(method.name()) && method.hasSignature(new JType[]{})) {
                methods.remove();
            }
        }
    }

    private void createCloneMethod(ClassOutline classOutline) {
        JDefinedClass impl = classOutline.implClass;
        JMethod cloneMethod = impl.method(JMod.PUBLIC, impl, METHOD_CLONE);
        JBlock body = cloneMethod.body();

        if (impl.isAbstract()) {
            body._return(JExpr._this());

            //don't create clone() method body on abstract prism objects
            return;
        }

        Outline outline = classOutline.parent();
        JVar object = body.decl(impl, "object", JExpr._new(impl));
        if (ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_PRISM_OBJECT)) {
            JClass type = (JClass) outline.getModel().codeModel._ref(PrismObject.class);
            JVar prism = body.decl(type, "value",
                    JExpr.invoke(SchemaProcessor.METHOD_AS_PRISM_OBJECT).invoke(METHOD_CLONE));
            JInvocation invocation = object.invoke(SchemaProcessor.METHOD_SETUP_CONTAINER);
            invocation.arg(prism);
            body.add(invocation);
        } else if (ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_PRISM_CONTAINER)) {
            JClass type = (JClass) outline.getModel().codeModel._ref(PrismContainerValue.class);
            JVar prism = body.decl(type, "value",
                    JExpr.invoke(SchemaProcessor.METHOD_AS_PRISM_CONTAINER_VALUE).invoke(METHOD_CLONE));
            JInvocation invocation = object.invoke(SchemaProcessor.METHOD_SETUP_CONTAINER_VALUE);
            invocation.arg(prism);
            body.add(invocation);
        } else if (ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_OBJECT_REFERENCE)) {
            JClass type = (JClass) outline.getModel().codeModel._ref(PrismReferenceValue.class);
            JVar prism = body.decl(type, "value",
                    JExpr.invoke(SchemaProcessor.METHOD_AS_REFERENCE_VALUE).invoke(METHOD_CLONE));
            JInvocation invocation = object.invoke(SchemaProcessor.METHOD_SETUP_REFERENCE_VALUE);
            invocation.arg(prism);
            body.add(invocation);
        }

        body._return(object);
    }

    private boolean isPrism(ClassOutline classOutline) {
        return ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_PRISM_OBJECT)
                || ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_PRISM_CONTAINER)
                || ProcessorUtils.hasParentAnnotation(classOutline, SchemaProcessor.A_OBJECT_REFERENCE);
    }
}

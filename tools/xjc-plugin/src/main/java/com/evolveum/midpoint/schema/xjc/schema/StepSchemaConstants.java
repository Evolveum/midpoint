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

package com.evolveum.midpoint.schema.xjc.schema;

import com.evolveum.midpoint.schema.xjc.PrefixMapper;
import com.evolveum.midpoint.schema.xjc.Processor;
import com.evolveum.midpoint.schema.xjc.util.ProcessorUtils;
import com.sun.codemodel.*;
import com.sun.tools.xjc.Options;
import com.sun.tools.xjc.model.CElementInfo;
import com.sun.tools.xjc.model.Model;
import com.sun.tools.xjc.outline.Outline;
import org.xml.sax.ErrorHandler;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lazyman
 */
public class StepSchemaConstants implements Processor {

    public static final String CLASS_NAME = "com.evolveum.midpoint.schema.SchemaConstantsGenerated";
    private Map<String, JFieldVar> namespaceFields = new HashMap<>();

    public Map<String, JFieldVar> getNamespaceFields() {
        return namespaceFields;
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws Exception {
        Model model = outline.getModel();
        JDefinedClass schemaConstants = model.codeModel._class(CLASS_NAME);

        //creating namespaces
        List<FieldBox<String>> namespaces = new ArrayList<>();
        for (PrefixMapper prefix : PrefixMapper.values()) {
            namespaces.add(new FieldBox("NS_" + prefix.getNamespaceName(), prefix.getNamespace()));
        }

        Collections.sort(namespaces);
        for (FieldBox<String> field : namespaces) {
            JFieldVar var = createNSFieldDefinition(outline, schemaConstants, field.getFieldName(), field.getValue());
            getNamespaceFields().put(field.getValue(), var);
        }

        //creating qnames
        List<FieldBox<QName>> fields = new ArrayList<>();
        Map<QName, CElementInfo> map = model.getElementMappings(null);
        Set<Map.Entry<QName, CElementInfo>> set = map.entrySet();
        for (Map.Entry<QName, CElementInfo> entry : set) {
            QName qname = entry.getKey();
            CElementInfo info = entry.getValue();
            String fieldName = ProcessorUtils.fieldPrefixedUnderscoredUpperCase(info.getSqueezedName(), qname);

            fields.add(new FieldBox(fieldName, qname));
        }

        //sort field by name and create qname definitions in class
        Collections.sort(fields);
        for (FieldBox<QName> field : fields) {
            JFieldVar var = namespaceFields.get(field.getValue().getNamespaceURI());
            if (var != null) {
                createQNameDefinition(outline, schemaConstants, field.getFieldName(), var, field.getValue());
            } else {
                ProcessorUtils.createPSFField(outline, schemaConstants, field.getFieldName(), field.getValue());
            }
        }

        return true;
    }

    private JFieldVar createNSFieldDefinition(Outline outline, JDefinedClass definedClass, String fieldName,
            String value) {
        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, String.class, fieldName, JExpr.lit(value));
    }

    private JFieldVar createQNameDefinition(Outline outline, JDefinedClass definedClass, String fieldName,
            JFieldVar namespaceField, QName reference) {
        JClass clazz = (JClass) outline.getModel().codeModel._ref(QName.class);

        JInvocation invocation = (JInvocation) JExpr._new(clazz);
        invocation.arg(namespaceField);
        invocation.arg(reference.getLocalPart());

        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, QName.class, fieldName, invocation);
    }
}

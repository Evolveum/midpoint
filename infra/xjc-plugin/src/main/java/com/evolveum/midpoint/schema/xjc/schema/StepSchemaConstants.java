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

    public static final String CLASS_NAME = "com.evolveum.midpoint.schema.SchemaConstants";
    private Map<String, JFieldVar> namespaceFields = new HashMap<String, JFieldVar>();

    public Map<String, JFieldVar> getNamespaceFields() {
        return namespaceFields;
    }

    @Override
    public boolean run(Outline outline, Options opt, ErrorHandler errorHandler) throws Exception {
        Model model = outline.getModel();
        JDefinedClass schemaConstants = model.codeModel._class(CLASS_NAME);

        //creating namespaces
        List<FieldBox<String>> namespaces = new ArrayList<FieldBox<String>>();
        for (PrefixMapper prefix : PrefixMapper.values()) {
            namespaces.add(new FieldBox("NS_" + prefix.getNamespaceName(), prefix.getNamespace()));
        }

        Collections.sort(namespaces);
        for (FieldBox<String> field : namespaces) {
            JFieldVar var = createNSFieldDefinition(outline, schemaConstants, field.getFieldName(), field.getValue());
            getNamespaceFields().put(field.getValue(), var);
        }

        //creating qnames
        List<FieldBox<QName>> fields = new ArrayList<FieldBox<QName>>();
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

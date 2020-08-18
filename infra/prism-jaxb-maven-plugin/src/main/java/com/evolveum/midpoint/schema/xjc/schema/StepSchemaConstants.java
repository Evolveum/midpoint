/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

import static com.evolveum.midpoint.schema.xjc.util.ProcessorUtils.createQName;

/**
 * @author lazyman
 */
public class StepSchemaConstants {

    public static final String SCHEMA_CONSTANTS_GENERATED_CLASS_NAME = "com.evolveum.midpoint.schema.SchemaConstantsGenerated";
    private Map<String, JFieldVar> namespaceFields = new HashMap<>();

    public Map<String, JFieldVar> getNamespaceFields() {
        return namespaceFields;
    }


    public boolean run(JCodeModel codeModel) throws Exception {
        JDefinedClass schemaConstants = codeModel._class(SCHEMA_CONSTANTS_GENERATED_CLASS_NAME);

        //creating namespaces
        List<FieldBox<String>> namespaces = new ArrayList<>();
        for (PrefixMapper prefix : PrefixMapper.values()) {
            namespaces.add(new FieldBox("NS_" + prefix.getNamespaceName(), prefix.getNamespace()));
        }

        Collections.sort(namespaces);
        for (FieldBox<String> field : namespaces) {
            JFieldVar var = createNSFieldDefinition(schemaConstants, field.getFieldName(), field.getValue());
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
            createQName(codeModel, schemaConstants, field.getFieldName(), field.getValue(), var, true, true);
        }

        return true;
    }

    private JFieldVar createNSFieldDefinition(JDefinedClass definedClass, String fieldName,
            String value) {
        int psf = JMod.PUBLIC | JMod.STATIC | JMod.FINAL;
        return definedClass.field(psf, String.class, fieldName, JExpr.lit(value));
    }

}

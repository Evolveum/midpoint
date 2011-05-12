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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.schema.util;

import com.evolveum.midpoint.provisioning.schema.Operation;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import static org.junit.Assert.*;
import org.w3c.dom.Document;

/**
 *
 * @author laszlohordos
 */
public class SchemaToDOMParserTest {

    /**
     * Test of getDocument method, of class SchemaToDOMParser.
     */
    @Test
    public void testGetDocument() throws Exception {
        SchemaToDOMParser instance = new SchemaToDOMParser();

        ResourceSchema resSchema = Utils.createSampleSchema();

        //write
        Document result = instance.getDomSchema(resSchema);
        URL baseDIR = this.getClass().getResource("/");
        URL targetFile = new URL(baseDIR, "SampleResourceSchema.xsd");
        System.out.println("Export file is at: " + targetFile);
        File file = new File(targetFile.toURI());
        if (file.exists()) {
            file.delete();
        }
        file = new File(targetFile.toURI());
        if (!file.exists()) {
            file.createNewFile();
        }
        OutputStream out = new FileOutputStream(file);
        writeDocument(out, result);
        writeDocument(System.out, result); //only for debugging purposes only - dom xml file after save

        //read
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        result = db.parse(file);
        result.getDocumentElement().normalize();

        ResourceSchema resSchemaNew = new DOMToSchemaParser().getSchema(result.getDocumentElement());
        writeDocument(System.out, instance.getDomSchema(resSchemaNew)); //only for debugging purposes only - dom xml file after read and save
        assertEquals(resSchema.getResourceNamespace(), resSchemaNew.getResourceNamespace());

        assertEquals(resSchema.getImportList().size(), resSchemaNew.getImportList().size());
        assertEquals(resSchema.getImportList(), resSchemaNew.getImportList());

        assertEquals(resSchema.getObjectClassesCopy().size(), resSchemaNew.getObjectClassesCopy().size());

        List<ResourceObjectDefinition> objList = resSchema.getObjectClassesCopy();
        List<ResourceObjectDefinition> objListNew = resSchemaNew.getObjectClassesCopy();
        Collections.sort(objList, new ObjDefComparator());
        Collections.sort(objListNew, new ObjDefComparator());

        for (int i = 0; i < objList.size(); i++) {
            testResourceObjectDef(objList.get(i), objListNew.get(i));
        }
    }

    private void testResourceObjectDef(ResourceObjectDefinition def1, ResourceObjectDefinition def2) {
        assertEquals(def1.getQName(), def2.getQName());
        assertEquals(def1.getName(), def2.getName());
        assertEquals(def1.getNativeObjectClass(), def2.getNativeObjectClass());
        assertEquals(def1.getSuperclasses(), def2.getSuperclasses());

        Collections.sort(def1.getOperations(), new OperationComparator());
        Collections.sort(def2.getOperations(), new OperationComparator());
        assertEquals(def1.getOperations(), def2.getOperations());

        List<ResourceAttributeDefinition> attr1 = new ArrayList(def1.getAttributesCopy());
        List<ResourceAttributeDefinition> attr2 = new ArrayList(def2.getAttributesCopy());
        assertEquals(attr1.size(), attr2.size());

        Collections.sort(attr1, new AttrDefComparator());
        Collections.sort(attr2, new AttrDefComparator());

        for (int i = 0; i < attr1.size(); i++) {
            testResourceAttributeDef(attr1.get(i), attr2.get(i));
        }
    }

    private void testResourceAttributeDef(ResourceAttributeDefinition def1, ResourceAttributeDefinition def2) {
        assertEquals(def1.getQName(), def2.getQName());
        assertEquals(def1.getHelp(), def2.getHelp());
        assertEquals(def1.getClassifiedAttributeInfo(), def2.getClassifiedAttributeInfo());
    }

    private void writeDocument(OutputStream out, Document document) {
        OutputFormat opfrmt = new OutputFormat(document, "UTF-8", true);
        opfrmt.setIndenting(true);
        opfrmt.setPreserveSpace(false);
        opfrmt.setLineWidth(500);
        XMLSerializer serial = new XMLSerializer(out, opfrmt);
        try {
            serial.setNamespaces(true);
            serial.asDOMSerializer();
            serial.serialize(document);
        } catch (java.io.IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private class OperationComparator implements Comparator<Operation> {

        @Override
        public int compare(Operation o1, Operation o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.toString(), o2.toString());
        }
    }

    private class AttrDefComparator implements Comparator<ResourceAttributeDefinition> {

        @Override
        public int compare(ResourceAttributeDefinition a1, ResourceAttributeDefinition a2) {
            return String.CASE_INSENSITIVE_ORDER.compare(a1.getQName().toString(), a2.getQName().toString());
        }
    }

    private class ObjDefComparator implements Comparator<ResourceObjectDefinition> {

        @Override
        public int compare(ResourceObjectDefinition def1, ResourceObjectDefinition def2) {
            return String.CASE_INSENSITIVE_ORDER.compare(def1.getQName().toString(), def2.getQName().toString());
        }
    }
}

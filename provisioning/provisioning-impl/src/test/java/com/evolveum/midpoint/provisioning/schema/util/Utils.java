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

import com.evolveum.midpoint.provisioning.schema.AccountObjectClassDefinition;
import com.evolveum.midpoint.provisioning.schema.AttributeFlag;
import com.evolveum.midpoint.provisioning.schema.ResourceAttributeDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceObjectDefinition;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Node;
import static javax.xml.XMLConstants.*;

/**
 *
 * @author elek
 */
public class Utils {

    private static final Map<Class, QName> types = new HashMap<Class, QName>();
    public static final String TNS = "http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";
    public static final String ICC = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd";

    static {
        types.put(String.class, new QName(W3C_XML_SCHEMA_NS_URI, "string"));
        types.put(int.class, new QName(W3C_XML_SCHEMA_NS_URI, "integer"));
        types.put(boolean.class, new QName(W3C_XML_SCHEMA_NS_URI, "boolean"));
        types.put(byte[].class, new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary"));
        //types.put(org.identityconnectors.common.security.GuardedString.class,new QName(SchemaConstants.NS_C, "PasswordType"));
    }

    public static ResourceSchema createSampleSchema() {
        //Identifier attributes
        ResourceAttributeDefinition uid = new ResourceAttributeDefinition(new QName(ICC, "__UID__")); //__UID__
        uid.setIdentifier(true);
        uid.setType(types.get(String.class));
        uid.getAttributeFlag().add(AttributeFlag.NOT_UPDATEABLE);

        ResourceAttributeDefinition name = new ResourceAttributeDefinition(new QName(ICC, "__NAME__")); //__NAME__
        name.setSecondaryIdentifier(true);
        name.setType(types.get(String.class));

        //Password
        ResourceAttributeDefinition password = new ResourceAttributeDefinition(new QName(ICC, "__PASSWORD__")); //__PASSWORD__
        password.setType(new QName(ICC, "PasswordType"));
        password.getAttributeFlag().add(AttributeFlag.PASSWORD);
        password.makeClassified(ResourceAttributeDefinition.Encryption.HASH, "password");


        // Wrong example but we need exaple for composite identifier
        ResourceAttributeDefinition givenName = new ResourceAttributeDefinition(new QName(TNS, "givenName"));
        givenName.setType(types.get(String.class));
        givenName.setCompositeIdentifier(true);
        givenName.setMinOccurs(0);

        ResourceAttributeDefinition sn = new ResourceAttributeDefinition(new QName(TNS, "sn"));
        sn.setType(types.get(String.class));
        sn.setCompositeIdentifier(true);
        sn.getAttributeFlag().add(AttributeFlag.NOT_UPDATEABLE);

        //Any custom and common attributes
        ResourceAttributeDefinition fullName = new ResourceAttributeDefinition(new QName(TNS, "fullName"));
        fullName.setType(types.get(String.class));
        fullName.setMinOccurs(0);
        fullName.setDescriptionAttribute(true);
        fullName.setDisplayName(true);
        fullName.setHelp("DISPLAY_NAME_HELP_KEY");

        ResourceAttributeDefinition description = new ResourceAttributeDefinition(new QName(TNS, "description"));
        description.setType(types.get(String.class));
        description.setMinOccurs(0);
        description.setDescriptionAttribute(true);
        description.setDisplayName(true);
        description.setHelp("DESCRIPTION_NAME_HELP_KEY");

        ResourceAttributeDefinition VAR_ORG = new ResourceAttributeDefinition(new QName(TNS, "custom"));
        VAR_ORG.setType(types.get(String.class));
        VAR_ORG.setMinOccurs(0);
        VAR_ORG.getAttributeFlag().add(AttributeFlag.IGNORE_ATTRIBUTE);
        VAR_ORG.setHelp("This attribute part of the SchemaHandling");

        //Group Object
        ResourceObjectDefinition group = new ResourceObjectDefinition(new QName(TNS, "Group"), "__GROUP__", true); //__GROUP__
        group.addAttribute(uid);
        group.addAttribute(name);
        group.addAttribute(description);

        ResourceAttributeDefinition groups = new ResourceAttributeDefinition(new QName(TNS, "groups"));
        groups.setType(types.get(String.class));
        groups.setResourceObjectReference(group);
        groups.setMinOccurs(0);
        groups.setMaxOccurs(ResourceAttributeDefinition.MAX_OCCURS_UNBOUNDED);

        //Account Object
        AccountObjectClassDefinition account = new AccountObjectClassDefinition(new QName(TNS, "Account"), "__ACCOUNT__"); //__ACCOUNT__
        account.setDefault(true);
        account.addAttribute(uid);
        account.addAttribute(name);
        account.addAttribute(password);
        account.addAttribute(givenName);
        account.addAttribute(sn);
        account.addAttribute(fullName);
        account.addAttribute(description);
        account.addAttribute(VAR_ORG);
        account.addAttribute(groups);

        ResourceSchema resSchema = new ResourceSchema(TNS);
        resSchema.getImportList().add(ICC);
        resSchema.addObjectClass(account);
        resSchema.addObjectClass(group);

        return resSchema;
    }

    public static void writeXml(Node result, FileWriter writer) throws Exception {
        TransformerFactory transFactory = TransformerFactory.newInstance();
        Transformer transformer = transFactory.newTransformer();
        StringWriter buffer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        //transformer.transform(new DOMSource(result), new StreamResult(buffer));
        String str = buffer.toString();
        System.out.println(str);
        transformer.transform(new DOMSource(result), new StreamResult(writer));
    }
}

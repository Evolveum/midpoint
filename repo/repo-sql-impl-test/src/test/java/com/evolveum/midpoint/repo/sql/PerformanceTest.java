package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.type.XMLGregorianCalendarType;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.*;
import java.util.*;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class PerformanceTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(PerformanceTest.class);

    private static final String[] GIVEN_NAMES = {"James", "Josephine", "Art", "Lenna", "Donette", "Simona", "Mitsue",
            "Leota", "Sage", "Kris", "Minna", "Abel", "Kiley", "Graciela", "Cammy", "Mattie", "Meaghan", "Gladys", "Yuki",
            "Fletcher", "Bette", "Veronika", "Willard", "Maryann", "Alisha", "Allene", "Chanel", "Ezekiel", "Willow",
            "Bernardo", "Ammie", "Francine", "Ernie", "Albina", "Alishia", "Solange", "Jose", "Rozella", "Valentine",
            "Kati", "Youlanda", "Dyan", "Roxane", "Lavera", "Erick", "Fatima", "Jina", "Kanisha", "Emerson"};
    private static final String[] FAMILY_NAMES = {"Butt", "Darakjy", "Venere", "Paprocki", "Foller", "Morasca", "Tollner",
            "Dilliard", "Wieser", "Marrier", "Amigon", "Maclead", "Caldarera", "Ruta", "Albares", "Poquette", "Garufi",
            "Rim", "Whobrey", "Flosi", "Nicka", "Inouye", "Kolmetz", "Royster", "Slusarski", "Iturbide", "Caudy", "Chui",
            "Kusko", "Figeroa", "Corrio", "Vocelka", "Stenseth", "Glick", "Sergi", "Shinko", "Stockham", "Ostrosky",
            "Gillian", "Rulapaugh", "Schemmer", "Oldroyd", "Campain", "Perin", "Ferencz", "Saylors", "Briddick",
            "Waycott", "Bowley", "Malet", "Bolognia", "Nestle", "Uyetake", "Mastella", "Klonowski", "Wenner", "Monarrez",
            "Seewald", "Ahle", "Juhas", "Pugh", "Vanausdal", "Hollack", "Lindall", "Yglesias", "Buvens", "Weight",
            "Morocco", "Eroman"};

    private static final int ORG_COUNT = 10;
    private static final int USER_PER_ORG_COUNT = 10000;
    private static final int RESOURCE_COUNT = 5;

    @Test(enabled = false)
    public void test100Parsing() throws Exception {
        long time = System.currentTimeMillis();

        int COUNT = 1000;
        for (int i = 0; i < COUNT; i++) {
            List<PrismObject<? extends Objectable>> elements = prismContext.parserFor(new File(FOLDER_BASIC, "objects.xml")).parseObjects();
            for (PrismObject obj : elements) {
                prismContext.serializeObjectToString(obj, PrismContext.LANG_XML);
            }
        }
        LOGGER.info("xxx>> time: {}", (System.currentTimeMillis() - time));
    }

    @Test(enabled = false)
    public void test200PrepareBigXml() throws Exception {
        File file = new File("./target/big-test.xml");
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), "utf-8");
        try {
            writeHeader(writer);

            OrgType root = createOrgType("University", "University org. structure", "UROOT", "Bratislava", null);
            root.getOrgType().add("functional");
            final String OID_ORG_ROOT = root.getOid();

            writeObject(root, writer);

            final List<String> RESOURCE_OIDS = new ArrayList<>();
            for (int i = 1; i <= RESOURCE_COUNT; i++) {
                ResourceType resource = createResource(i);
                RESOURCE_OIDS.add(resource.getOid());

                writeObject(resource, writer);
            }

            for (int i = 1; i <= ORG_COUNT; i++) {
                OrgType org = createOrgType("Unit " + i, "University unit " + i, "U00" + i, null, OID_ORG_ROOT);
                writeObject(org, writer);

                String orgOID = org.getOid();
                for (int j = 1; j <= USER_PER_ORG_COUNT; j++) {
                    UserType user = createUserType((i - 1) * USER_PER_ORG_COUNT + j, orgOID);
                    for (int k = 1; k <= RESOURCE_OIDS.size(); k++) {
                        ShadowType shadow = createShadow(user, RESOURCE_OIDS.get(k - 1), k);
                        writeObject(shadow, writer);

                        user.getLinkRef().add(createRef(shadow.getOid(), ShadowType.COMPLEX_TYPE));
                    }

                    writeObject(user, writer);
                }
            }

            writeFooter(writer);
        } finally {
            writer.close();
        }
    }

    @Test(enabled = false)
    public void test300Get() throws Exception {
        OperationResult result = new OperationResult("test300Get");

        PrismObject<UserType> user = prismContext.createObject(UserType.class);
        user.asObjectable().setName(PolyStringType.fromOrig("user"));
        String oid = repositoryService.addObject(user, null, result);

        long time = System.currentTimeMillis();

        int COUNT = 100;
        for (int i = 0; i < COUNT; i++) {
            LOGGER.info("Get operation {} of {}", i+1, COUNT);
            repositoryService.getObject(UserType.class, oid, null, result);
        }
        long duration = System.currentTimeMillis() - time;
        LOGGER.info("xxx>> time: {} ms, per get: {} ms", duration, (double) duration/COUNT);
    }


    private ResourceType createResource(int resourceId) throws SchemaException, IOException {
        PrismObject<ResourceType> prism = prismContext.parseObject(new File(FOLDER_BASIC, "resource-opendj.xml"));

        ResourceType resource = new ResourceType();
        resource.setupContainer(prism);
        resource.setName(createPoly("Resource " + resourceId));
        resource.setOid(UUID.randomUUID().toString());

        return resource;
    }

    private ShadowType createShadow(UserType user, String resourceOid, int resourceId) throws SchemaException {
        ShadowType shadow = new ShadowType();
        shadow.setOid(UUID.randomUUID().toString());
        final String DN = "cn=" + user.getName().getOrig() + ",ou=resource" + resourceId + ",dc=example,dc=com";
        shadow.setName(createPoly(DN));
        shadow.setResourceRef(createRef(resourceOid, ResourceType.COMPLEX_TYPE));
        shadow.setKind(ShadowKindType.ACCOUNT);
        shadow.setIntent("standardAccount");
        shadow.setObjectClass(new QName("http://midpoint.evolveum.com/xml/ns/public/resource/instance/" + resourceOid,
                "AccountObjectClass"));

        PrismObject<ShadowType> prism = shadow.asPrismObject();
        prismContext.adopt(prism);

        PrismContainer attributes = prism.findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        PrismProperty property = attributes.findOrCreateProperty(SchemaConstants.ICFS_UID);
        property.setRealValue(UUID.randomUUID().toString());

        property = attributes.findOrCreateProperty(SchemaConstants.ICFS_NAME);
        property.setRealValue(DN);

        return shadow;
    }

    private String createUserName(String given, String family, int userId) {
        StringBuilder sb = new StringBuilder();
        sb.append(given.toLowerCase().charAt(0));
        sb.append(family.toLowerCase().charAt(0));
        sb.append(StringUtils.leftPad(Integer.toString(userId), 8, "0"));

        return sb.toString();
    }

    private UserType createUserType(int userId, String orgOid) throws SchemaException {
        String given = GIVEN_NAMES[new Random().nextInt(GIVEN_NAMES.length)];
        String family = FAMILY_NAMES[new Random().nextInt(FAMILY_NAMES.length)];

        UserType user = new UserType();
        user.setName(createPoly(createUserName(given, family, userId)));
        user.setGivenName(createPoly(given));
        user.setFamilyName(createPoly(family));
        user.setFullName(createPoly(given + " " + family));
        user.setEmployeeNumber(Integer.toString(userId));
        user.getParentOrgRef().add(createRef(orgOid, OrgType.COMPLEX_TYPE));

        PrismObject<UserType> prism = user.asPrismObject();
        prismContext.adopt(prism);

        final String NS_P = "http://example.com/p";

        PrismProperty property = prism.findOrCreateProperty(
                new ItemPath(ObjectType.F_EXTENSION, new QName(NS_P, "weapon")));
        property.setRealValue("Ak-47-" + new Random().nextInt(10));

        property = prism.findOrCreateProperty(
                new ItemPath(ObjectType.F_EXTENSION, new QName(NS_P, "shipName")));
        property.setRealValue("smallBoat-" + new Random().nextInt(10));

        property = prism.findOrCreateProperty(
                new ItemPath(ObjectType.F_EXTENSION, new QName(NS_P, "loot")));
        property.setRealValue(new Random().nextInt(10000));

        property = prism.findOrCreateProperty(
                new ItemPath(ObjectType.F_EXTENSION, new QName(NS_P, "funeralDate")));
        property.setRealValue(XMLGregorianCalendarType.asXMLGregorianCalendar(new Date()));

        return user;
    }

    private OrgType createOrgType(String name, String description, String identifier, String locality,
                                  String parentRefOid) {
        OrgType org = new OrgType();
        org.setOid(UUID.randomUUID().toString());
        org.setName(createPoly(name));
        org.setDescription(description);
        org.setIdentifier(identifier);
        org.setLocality(createPoly(locality));

        if (StringUtils.isNotEmpty(parentRefOid)) {
            ObjectReferenceType ref = createRef(parentRefOid, OrgType.COMPLEX_TYPE);
            org.getParentOrgRef().add(ref);
        }

        return org;
    }

    private ObjectReferenceType createRef(String oid, QName type) {
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(oid);
        ref.setType(type);

        return ref;
    }

    private PolyStringType createPoly(String orig) {
        PolyStringType poly = new PolyStringType();
        poly.setOrig(orig);
        return poly;
    }

    private void writeObject(ObjectType obj, Writer writer) throws IOException, SchemaException {
        PrismObject prism = obj.asPrismObject();
        prismContext.adopt(prism);
        writer.write(prismContext.serializeObjectToString(prism, PrismContext.LANG_XML));
        writer.write('\n');
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        writer.write("<objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">\n");
    }

    private void writeFooter(Writer writer) throws IOException {
        writer.write("</objects>");
    }
}

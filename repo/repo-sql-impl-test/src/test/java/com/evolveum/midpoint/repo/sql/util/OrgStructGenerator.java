/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import java.io.File;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.BaseSQLRepoTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
public class OrgStructGenerator extends BaseSQLRepoTest {

    @Test(enabled = false)
    public void generateOrgStructure() throws Exception {
        List<OrgType> orgs = generateOrgStructure(0, new int[] { 1, 20, 25, 2 }, "Org", null);

        System.out.println(orgs.size());

        Collections.shuffle(orgs);

        File file = new File("./target/orgs.xml");
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        try (Writer writer = new FileWriterWithEncoding(file, StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            writer.write("<objects xmlns=\"http://midpoint.evolveum.com/xml/ns/public/common/common-3\">\n");

            for (OrgType org : orgs) {
                writer.write(PrismTestUtil.serializeObjectToString(org.asPrismObject()));
            }

            writer.write("</objects>");
        }
    }

    private List<OrgType> generateOrgStructure(int level, int[] levels, String name, ObjectReferenceType parentRef) throws Exception {
        List<OrgType> orgs = new ArrayList<>();

        if (level >= levels.length) {
            return orgs;
        }

        for (int i = 0; i < levels[level]; i++) {
            OrgType org = createOrgType(name + " " + i, parentRef != null ? parentRef.clone() : null);
            orgs.add(org);

            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setOid(org.getOid());
            ref.setType(OrgType.COMPLEX_TYPE);

            orgs.addAll(generateOrgStructure(level + 1, levels, org.getName().getOrig(), ref));
        }

        return orgs;
    }

    private OrgType createOrgType(String name, ObjectReferenceType parentRef) throws Exception {
        OrgType orgType = new OrgType();
        orgType.setOid(UUID.randomUUID().toString());
        orgType.setName(new PolyStringType(name));
        orgType.setIdentifier(UUID.randomUUID().toString());
        orgType.setDisplayName(new PolyStringType(name));

        if (parentRef != null) {
            orgType.setCostCenter(parentRef.getOid().substring(4));
            orgType.getParentOrgRef().add(parentRef);
        }

        prismContext.adopt(orgType);

        return orgType;
    }
}

/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.longtest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.LDIFImportConfig;
import org.opends.server.util.LDIFException;
import org.opends.server.util.LDIFReader;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Mix of various tests for issues that are difficult to replicate using dummy resources.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-longtest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLongTest extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
    protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    protected static final File RESOURCE_OPENDJ_FILE = new File(COMMON_DIR, "resource-opendj.xml");
    protected static final String RESOURCE_OPENDJ_NAME = "Localhost OpenDJ";
    protected static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    protected static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
    protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
    protected static final String USER_BARBOSSA_USERNAME = "barbossa";
    protected static final String USER_BARBOSSA_FULL_NAME = "Hector Barbossa";

    protected static final File USER_GUYBRUSH_FILE = new File (COMMON_DIR, "user-guybrush.xml");
    protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
    protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
    protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";

    public static final String DOT_JPG_FILENAME = "src/test/resources/common/dot.jpg";

    protected PrismObject<UserType> userAdministrator;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        provisioningService.postInit(initResult);
        modelService.postInit(initResult);

        // System Configuration
        PrismObject<SystemConfigurationType> config;
        try {
            config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        // to get profiling facilities (until better API is available)
//        LoggingConfigurationManager.configure(
//                ProfilingConfigurationManager.checkSystemProfilingConfiguration(config),
//                config.asObjectable().getVersion(), initResult);

        // administrator
        userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
        login(userAdministrator);

    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    protected Entry createLdapEntry(String uid, String name) throws IOException, LDIFException {
        StringBuilder sb = new StringBuilder();
        String dn = "uid="+uid+","+openDJController.getSuffixPeople();
        sb.append("dn: ").append(dn).append("\n");
        sb.append("objectClass: inetOrgPerson\n");
        sb.append("uid: ").append(uid).append("\n");
        sb.append("cn: ").append(name).append("\n");
        sb.append("sn: ").append(name).append("\n");
        LDIFImportConfig importConfig = new LDIFImportConfig(IOUtils.toInputStream(sb.toString(), StandardCharsets.UTF_8));
        LDIFReader ldifReader = new LDIFReader(importConfig);
        Entry ldifEntry = ldifReader.readEntry();
        return ldifEntry;
    }

    protected void loadLdapEntries(String prefix, int numEntries) throws LDIFException, IOException {
        long ldapPopStart = System.currentTimeMillis();

        for(int i=0; i < numEntries; i++) {
            String name = "user"+i;
            Entry entry = createLdapEntry(prefix+i, name);
            openDJController.addEntry(entry);
        }

        long ldapPopEnd = System.currentTimeMillis();

        display("Loaded "+numEntries+" LDAP entries in "+((ldapPopEnd-ldapPopStart)/1000)+" seconds");
    }

}

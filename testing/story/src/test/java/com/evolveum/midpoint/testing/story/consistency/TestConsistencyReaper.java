/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story.consistency;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.LinksAsserter;
import com.evolveum.midpoint.test.asserter.ShadowAsserter;
import com.evolveum.midpoint.test.asserter.ShadowReferenceAsserter;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.ActivationCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang3.StringUtils;
import org.opends.server.types.Entry;
import org.opends.server.util.EmbeddedUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.test.IntegrationTestTools.assertNoRepoThreadLocalCache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

/**
 * Almost same sa TestConsistencyMechanism, but this reaps dead shadows immediately (deadShadowRetentionPeriod=0).
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestConsistencyReaper extends TestConsistencyMechanism {

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj-reaper.xml");

    protected File getResourceFile() {
        return RESOURCE_OPENDJ_FILE;
    }

    @Override
    protected void checkTest130DeadShadow(Task task, OperationResult parentResult)  {
        // nothing to do. Dead shadows are reaped immediately.
    }

    @Override
    protected void checkTest140DeadShadow(Task task, OperationResult parentResult)  {
        // nothing to do. Dead shadows are reaped immediately.
    }

    @Override
    protected void assert800DeadShadows() throws CommonException {
        assertNoRepoShadow(ACCOUNT_DENIELS_OID);

        LinksAsserter linksAsserter = assertUser(USER_ELAINE_OID, "User after recon")
                .assertLinks(1)
                .links();

        ShadowReferenceAsserter notDeadShadow = linksAsserter.by()
                .dead(false)
                .find();

        assertModelShadow(notDeadShadow.getOid())
                .display()
                .attributes()
                .assertHasPrimaryIdentifier()
                .assertHasSecondaryIdentifier()
                .end()
                .end();

    }

}

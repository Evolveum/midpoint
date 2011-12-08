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
 */

package com.evolveum.midpoint.model.sync.action;

import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.model.sync.SynchronizationException;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBElement;
import java.io.File;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-model-unit-test.xml",
        "classpath:application-context-configuration-test-no-repo.xml",
        "classpath:application-context-task.xml"})

public class LinkAccountActionTest extends BaseActionTest {

    private static final File TEST_FOLDER = new File("./src/test/resources/sync/action/account");
    private static final Trace LOGGER = TraceManager.getTrace(LinkAccountActionTest.class);

    @BeforeMethod
    public void before() {
        Mockito.reset(provisioning, repository);
        before(new LinkAccountAction());
    }

    @Test(expectedExceptions = SynchronizationException.class)
    @SuppressWarnings("unchecked")
    public void nonExistingUser() throws Exception {
        ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
                .unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();
        OperationResult result = new OperationResult("Link Account Action Test");

        String userOid = "1";
        when(
                repository.getObject(any(Class.class), eq(userOid), any(PropertyReferenceListType.class),
                        any(OperationResult.class))).thenThrow(new ObjectNotFoundException("user not found"));

        try {
            ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
            action.executeChanges(userOid, change, SynchronizationSituationType.CONFIRMED,
                    (ResourceObjectShadowType) addition.getObject(), result);
        } finally {
            LOGGER.debug(result.dump());
        }
    }

    @Test(enabled = false)
    @SuppressWarnings("unchecked")
    public void nonAccountShadow() throws Exception {
        ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
                .unmarshal(new File(TEST_FOLDER, "group-change.xml"))).getValue();
        OperationResult result = new OperationResult("Link Account Action Test");

        String userOid = ModelTUtil.mockUser(repository, new File(TEST_FOLDER, "user.xml"), null);

        try {
            ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
            action.executeChanges(userOid, change, SynchronizationSituationType.CONFIRMED,
                    (ResourceObjectShadowType) addition.getObject(), result);
        } finally {
            LOGGER.debug(result.dump());
        }

        verify(repository, times(0)).modifyObject(any(Class.class), any(ObjectModificationType.class),
                any(OperationResult.class));
    }

    @Test(enabled = false)
    @SuppressWarnings("unchecked")
    public void correctLinkAccount() throws Exception {
        ResourceObjectShadowChangeDescriptionType change = ((JAXBElement<ResourceObjectShadowChangeDescriptionType>) JAXBUtil
                .unmarshal(new File(TEST_FOLDER, "../user/existing-user-change.xml"))).getValue();
        OperationResult result = new OperationResult("Link Account Action Test");

        final String shadowOid = change.getShadow().getOid();

        final String userOid = ModelTUtil.mockUser(repository, new File(TEST_FOLDER, "user.xml"), null);
        doNothing().doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ObjectModificationType change = (ObjectModificationType) invocation.getArguments()[0];
                assertNotNull(change);
                assertEquals(userOid, change.getOid());
                assertEquals(1, change.getPropertyModification().size());

                PropertyModificationType modification = change.getPropertyModification().get(0);
                assertNotNull(modification.getValue());
                assertEquals(1, modification.getValue().getAny().size());
                assertEquals(modification.getModificationType(), PropertyModificationTypeType.add);

                Element element = ((Element) modification.getValue().getAny().get(0));

                ObjectReferenceType accountRef = new ObjectReferenceType();
                accountRef.setOid(shadowOid);
                accountRef.setType(ObjectTypes.ACCOUNT.getTypeQName());

                XmlAsserts.assertPatch(JAXBUtil.marshalWrap(accountRef, SchemaConstants.I_ACCOUNT_REF),
                        DOMUtil.printDom(element).toString());

                return null;
            }
        }).when(repository).modifyObject(any(Class.class), any(ObjectModificationType.class), any(OperationResult.class));

        try {
            ObjectChangeAdditionType addition = (ObjectChangeAdditionType) change.getObjectChange();
            action.executeChanges(userOid, change, SynchronizationSituationType.CONFIRMED,
                    (ResourceObjectShadowType) addition.getObject(), result);
        } finally {
            LOGGER.debug(result.dump());
        }

        verify(repository, times(1)).modifyObject(any(Class.class), any(ObjectModificationType.class),
                any(OperationResult.class));
    }
}

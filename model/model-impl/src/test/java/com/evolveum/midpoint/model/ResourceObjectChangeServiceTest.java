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

package com.evolveum.midpoint.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.provisioning.objects.ResourceObject;
import com.evolveum.midpoint.provisioning.schema.ResourceSchema;
import com.evolveum.midpoint.provisioning.schema.util.ObjectValueWriter;
import com.evolveum.midpoint.provisioning.service.BaseResourceIntegration;
import com.evolveum.midpoint.provisioning.service.ResourceAccessInterface;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ExtensibleObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectContainerType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationalResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.provisioning.provisioning_1.ProvisioningPortType;
import com.evolveum.midpoint.xml.ns._public.provisioning.resource_object_change_listener_1.ResourceObjectChangeListenerPortType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;
import com.evolveum.midpoint.xml.schema.XPathSegment;
import com.evolveum.midpoint.xml.schema.XPathType;

/**
 *
 * @author sleepwalker
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:application-context-model.xml", "classpath:application-context-repository.xml", "classpath:application-context-repository-test.xml", "classpath:application-context-provisioning.xml", "classpath:application-context-model-test.xml"})
public class ResourceObjectChangeServiceTest {

    @Autowired(required = true)
    private ResourceObjectChangeListenerPortType resourceObjectChangeService;
    @Autowired(required = true)
    private RepositoryPortType repositoryService;
    @Autowired(required = true)
    private ResourceAccessInterface rai;
//    @Autowired(required = true)
//    private ProvisioningPortType provisioningService;

    public ResourceObjectChangeServiceTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private PropertyModificationType createPasswordModification(String newPassword) {
        if (null == newPassword) {
            return null;
        }
        PropertyModificationType modification = null;
        Document doc = DOMUtil.getDocument();
        modification = new PropertyModificationType();
        modification.setModificationType(PropertyModificationTypeType.replace);
        List<XPathSegment> segments = new ArrayList<XPathSegment>();
        segments.add(new XPathSegment(SchemaConstants.I_CREDENTIALS));
        XPathType xpath = new XPathType(segments);
        modification.setPath(xpath.toElement(SchemaConstants.NS_C, "path", doc));

        Element e = (Element) doc.createElementNS(SchemaConstants.NS_C, "password");
        e.setTextContent(newPassword);

        modification.setValue(new Value());
        modification.getValue().getAny().add(e);
        return modification;

    }

    private ExtensibleObjectType addObjectToRepo(String fileString) throws Exception {
        ObjectContainerType objectContainer = new ObjectContainerType();
        ExtensibleObjectType object = ((JAXBElement<ExtensibleObjectType>) JAXBUtil.unmarshal(new File(fileString))).getValue();
        objectContainer.setObject(object);
        repositoryService.addObject(objectContainer);
        return object;
    }

    private ResourceObject createSampleResourceObject(ResourceSchema schema, ResourceObjectShadowType shadow ) throws ParserConfigurationException {
        ObjectValueWriter valueWriter = ObjectValueWriter.getInstance();
        return valueWriter.buildResourceObject(shadow, schema);
    }

    /**
     * Test simulates scenario when PasswordChangeService notifies model about password change on account.
     * Origin account - account on system where the password was changed.
     * Target account - other account for the same user for which we will set the new password
     * To simplify the test both accounts are on the same object
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testResourceObjectChangeService() throws Exception {
        final String resourceOid = "aaaaaaaa-76e0-48e2-86d6-3d4f02d3e1a2";
        final String originChangeAccountOid = "acc11111-76e0-48e2-86d6-3d4f02d3e1a2";
        final String targetChangeAccountOid = "acc22222-76e0-48e2-86d6-3d4f02d3e1a2";
        final String userOid = "c0c010c0-d34d-b33f-f00d-111111111111";
        final String newPassword = "newPasswordUnitTest";

        try {
            //prepare objects used by tests in the repo
            ResourceType resource = (ResourceType) addObjectToRepo("src/test/resources/resource-password-change.xml");
            AccountShadowType accountShadowOrigin = (AccountShadowType) addObjectToRepo("src/test/resources/account-origin-password-change.xml");
//            AccountShadowType accountShadowTarget = (AccountShadowType) addObjectToRepo("src/test/resources/account-target-password-change.xml");
//            UserType user = (UserType) addObjectToRepo("src/test/resources/user-password-change.xml");

            //setup provisioning mock
            BaseResourceIntegration bri = new BaseResourceIntegration(resource);
            ResourceObject ro = createSampleResourceObject(bri.getSchema(), accountShadowOrigin);
            when(rai.get(
                    any(OperationalResultType.class),
                    any(ResourceObject.class))).thenReturn(ro);
            when(rai.getConnector()).thenReturn(bri);
            //when(rai.synchronize(any(SynchronizationState.class), any(OperationalResultType.class), any(ResourceObjectDefinition.class))).thenReturn(new SynchronizationResult());

            //prepare password change
            ObjectContainerType container = repositoryService.getObject(resourceOid, new PropertyReferenceListType());
            ResourceObjectShadowChangeDescriptionType change = new ResourceObjectShadowChangeDescriptionType();
            change.setResource((ResourceType) container.getObject());
            change.setShadow(accountShadowOrigin);
            change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_SYNC));
            ObjectChangeModificationType pwchange = new ObjectChangeModificationType();
            ObjectModificationType mod = new ObjectModificationType();
            mod.setOid(originChangeAccountOid);
            PropertyModificationType passwordChange = createPasswordModification(newPassword);
            mod.getPropertyModification().add(passwordChange);
            pwchange.setObjectModification(mod);
            change.setObjectChange(pwchange);
            //notify object change service about changed password
            resourceObjectChangeService.notifyChange(change);

            //propagate the password change to target objects
            //provisioningService.synchronize(resourceOid);

            //password has to be set on origin and target account
//            container = repositoryService.getObject(originChangeAccountOid, new PropertyReferenceListType());
//            AccountShadowType originAccountWithChangedPassword = (AccountShadowType) container.getObject();
//            assertNotNull(originAccountWithChangedPassword.getCredentials());
//            Element passwordElement = (Element) originAccountWithChangedPassword.getCredentials().getPassword().getAny();
//            assertEquals(newPassword, passwordElement.getTextContent());
            container = repositoryService.getObject(targetChangeAccountOid, new PropertyReferenceListType());
            AccountShadowType targetAccountWithChangedPassword = (AccountShadowType) container.getObject();
            assertNotNull(targetAccountWithChangedPassword.getCredentials());
            Element passwordElement = (Element) targetAccountWithChangedPassword.getCredentials().getPassword().getAny();
            assertEquals(newPassword, passwordElement.getTextContent());

        } finally {
            //cleanup repo
            repositoryService.deleteObject(originChangeAccountOid);
            repositoryService.deleteObject(targetChangeAccountOid);
            repositoryService.deleteObject(resourceOid);
            repositoryService.deleteObject(userOid);
        }
    }
}

package com.evolveum.midpoint.notifications.impl.formatters;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.util.string.Strings;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipalManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class VisualizationBasedDeltaFormatterTest extends AbstractIntegrationTest {

    private final IndentationGenerator indentationGenerator = new IndentationGenerator("|", "\t");
    @Autowired
    private Visualizer visualizer;
    @Autowired
    private MidPointPrincipalManager principalManager;

    private VisualizationBasedDeltaFormatter formatter;

    @Override
    public void initSystem() throws Exception {
        super.initSystem();
        final URL midPointHomeDir = this.getClass().getResource("/midpoint-home");
        if (midPointHomeDir == null) {
            throw new IllegalStateException("Unable to find test midpoint home directory.");
        }
        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midPointHomeDir.getPath());
        ((LocalizationServiceImpl)this.localizationService).init();
        final Task initTask = createTask();
        repoAddObjectFromFile(new File("src/test/resources/objects/role-superuser.xml"), RoleType.class,
                initTask.getResult());
        repoAddObjectFromFile(new File("src/test/resources/objects/user-administrator.xml"), UserType.class,
                initTask.getResult());
    }


    @BeforeMethod
    void login() throws SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        final MidPointPrincipal principal = this.principalManager.getPrincipal((USER_ADMINISTRATOR_USERNAME),
                UserType.class);
        final SecurityContext securityContext = SecurityContextHolder.getContext();
        final Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        securityContext.setAuthentication(authentication);
        final PropertyFormatter propertyFormatter = new PropertyFormatter(this.localizationService, " ", "\n");
        final PropertiesFormatter<VisualizationItem> propertiesFormatter = new PlainTextPropertiesFormatter(
                this.indentationGenerator, propertyFormatter);
        final PropertiesFormatter<VisualizationDeltaItem> modifiedPropertiesFormatter = new ModifiedPropertiesFormatter(
                propertyFormatter, indentationGenerator);
        final PropertiesFormatter<VisualizationDeltaItem> containerPropertiesModificationFormatter =
                new ContainerPropertiesModificationFormatter(this.localizationService, propertiesFormatter,
                        indentationGenerator, modifiedPropertiesFormatter);
        this.formatter = new VisualizationBasedDeltaFormatter(this.visualizer, propertiesFormatter,
                containerPropertiesModificationFormatter, indentationGenerator,
                this.localizationService);
    }

    @Test
    void userWithFewPropertiesIsAdded_formatObjectModificationDeltaIsCalled_propertiesShouldBeProperlyFormatted() {
        final Task testTask = getTestTask();
        final UserType user = new UserType();
        user.setName(new PolyStringType("Rudy"));
        user.setFamilyName(new PolyStringType("Moric"));
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                Add User "Rudy":
                |\tName: Rudy
                |\tFamily name: Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test(enabled = false, description = "Delete of focal objects is not handled via formatters right now.")
    void userWithFewPropertiesIsDeleted_formatObjectModificationDeltaIsCalled_propertiesShouldBeProperlyFormatted() {
        final Task testTask = getTestTask();
        final UserType user = new UserType();
        user.setName(new PolyStringType("Rudy"));
        user.setFamilyName(new PolyStringType("Moric"));
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createDeleteDelta();

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                Remove User "Rudy":
                |\tName: Rudy
                |\tFamily name: Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userPropertiesHasBeenModified_formatObjectModificationDeltaIsCalled_propertiesShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final Task testTask = getTestTask();
        final UserType modifiedUser = new UserType();
        final String userOid = UUID.randomUUID().toString();
        modifiedUser.oid(userOid);
        modifiedUser.name("Rudy");
        modifiedUser.familyName("Moric");
        modifiedUser.givenName("Ferdo");
        modifiedUser.fullName("Ferdo Moric");

        final UserType oldUser = new UserType();
        oldUser.oid(userOid);
        oldUser.name("Rudy");
        oldUser.familyName("Moric");
        oldUser.fullName("Moric");
        this.repoAddObject(oldUser.asPrismObject(), testTask.getResult());
        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                User "Moric (Rudy)" has been modified:
                |\tAdded properties:
                |\t|\tGiven name: Ferdo
                |\tModified properties:
                |\t|\tFull name: Moric -> Ferdo Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userWithActivationContainerIsAdded_formatObjectModificationDeltaIsCalled_containerShouldBeProperlyFormatted() {
        final Task testTask = getTestTask();
        final ActivationType activation = new ActivationType();
        activation.lockoutStatus(LockoutStatusType.NORMAL);
        final UserType user = new UserType();
        user.setName(new PolyStringType("Rudy"));
        user.setActivation(activation);
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                Add User "Rudy":
                |\tName: Rudy
                |\tAdd Activation:
                |\t|\tLock-out Status: %s""".formatted(StringUtils.capitalize(LockoutStatusType.NORMAL.value()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test(enabled = false, description = """
            There is several issues preventing this to pass:
            * Overview translations contains html tags
            * Additional identification properties are not implemented yet""")
    void userWithMoreContainersIsAdded_formatObjectModificationDeltaIsCalled_containersShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final Task testTask = getTestTask();
        final ActivationType userActivation = new ActivationType();
        userActivation.lockoutStatus(LockoutStatusType.NORMAL);
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role.asPrismObject(), testTask.getResult());

        final ActivationType assignmentActivation = new ActivationType();
        final XMLGregorianCalendar activationDate = TestUtil.currentTime();
        assignmentActivation.validFrom(activationDate);
        final UserType user = new UserType();
        user.setName(new PolyStringType("Rudy"));
        user.setActivation(userActivation);
        user.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(assignmentActivation)
                .end();
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                Add User "Rudy":
                |\tName: Rudy
                |\tAdd Activation:
                |\t|\tLock-out Status: %s
                |\tRole "Accounting" assigned:
                |\t|\tAdd Activation:
                |\t|\t|\tValid from: %s"""
                .formatted(StringUtils.capitalize(LockoutStatusType.NORMAL.value()), activationDate);
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userRoleIsUnassigned_formatObjectModificationDeltaIsCalled_unassignedRoleShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final Task testTask = getTestTask();
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role.asPrismObject(), testTask.getResult());

        final String userOid = UUID.randomUUID().toString();
        final UserType oldUser = new UserType();
        oldUser.oid(userOid);
        oldUser.setName(new PolyStringType("Rudy"));
        oldUser.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(new ActivationType())
                .end();
        repoAddObject(oldUser.asPrismObject(), testTask.getResult());

        final UserType modifiedUser = new UserType();
        modifiedUser.oid(userOid);
        modifiedUser.setName(new PolyStringType("Rudy"));

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tRole "Accounting" unassigned:
                |\t|\tDelete "Activation\"""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userRoleWithActivationIsUnassigned_formatObjectModificationDeltaIsCalled_unassignedRoleShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final Task testTask = getTestTask();
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role.asPrismObject(), testTask.getResult());

        final String userOid = UUID.randomUUID().toString();
        final UserType oldUser = new UserType();
        final XMLGregorianCalendar validFrom = TestUtil.currentTime();
        oldUser.oid(userOid);
        oldUser.setName(new PolyStringType("Rudy"));
        oldUser.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(new ActivationType().validFrom(validFrom))
                .end();
        repoAddObject(oldUser.asPrismObject(), testTask.getResult());

        final UserType modifiedUser = new UserType();
        modifiedUser.oid(userOid);
        modifiedUser.setName(new PolyStringType("Rudy"));

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final String formattedDelta = this.formatter.formatObjectModificationDelta(userDelta, Collections.emptyList(),
                false, testTask, testTask.getResult());

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tRole "Accounting" unassigned:
                |\t|\tDelete Activation:
                |\t|\t|\tValid from: %s""".formatted(validFrom);
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }
}
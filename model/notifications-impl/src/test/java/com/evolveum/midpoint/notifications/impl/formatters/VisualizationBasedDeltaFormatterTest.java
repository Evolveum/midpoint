package com.evolveum.midpoint.notifications.impl.formatters;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.StringUtils;
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
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.visualizer.VisualizationContext;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
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

    private final List<ObjectType> addedObjects = new ArrayList<>();
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
    }

    @BeforeMethod
    void setupFormatters() {
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

    @AfterMethod
    void cleanupRepository() {
        final Task task = getTestTask();
        for (ObjectType object : this.addedObjects) {
            try {
                this.repositoryService.deleteObject(object.getClass(), object.getOid(), task.getResult());
            } catch (ObjectNotFoundException e) {
                // FIXME figure out why this occurs.
                System.out.println("Weird thing happened " + e.getMessage());
            }
        }
    }

    @Test
    void userWithFewPropertiesIsAdded_formatVisualizationIsCalled_propertiesShouldBeProperlyFormatted() {
        final UserType user = createUserRudy();
        user.setFamilyName(new PolyStringType("Moric"));
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                Add User "Rudy":
                |\tName: Rudy
                |\tFamily name: Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test(enabled = false, description = "Delete of focal objects is not handled via formatters right now.")
    void userWithFewPropertiesIsDeleted_formatVisualizationIsCalled_propertiesShouldBeProperlyFormatted() {
        final UserType user = createUserRudy();
        user.setFamilyName(new PolyStringType("Moric"));
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createDeleteDelta();

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                Remove User "Rudy":
                |\tName: Rudy
                |\tFamily name: Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userPropertiesHasBeenModified_formatVisualizationIsCalled_propertiesShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        oldUser.familyName("Moric");
        oldUser.fullName("Moric");
        this.repoAddObject(oldUser);

        final UserType modifiedUser = oldUser.clone();
        modifiedUser.givenName("Ferdo");
        modifiedUser.fullName("Ferdo Moric");

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Moric (Rudy)" has been modified:
                |\tAdded properties:
                |\t|\tGiven name: Ferdo
                |\tModified properties:
                |\t|\tFull name: Moric -> Ferdo Moric""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userWithActivationContainerIsAdded_formatVisualizationIsCalled_containerShouldBeProperlyFormatted() {
        final ActivationType activation = new ActivationType();
        activation.lockoutStatus(LockoutStatusType.NORMAL);
        final UserType user = createUserRudy();
        user.setActivation(activation);
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                Add User "Rudy":
                |\tName: Rudy
                |\tAdd Activation:
                |\t|\tLock-out Status: %s""".formatted(StringUtils.capitalize(LockoutStatusType.NORMAL.value()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userWithMoreContainersIsAdded_formatVisualizationIsCalled_containersShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final ActivationType userActivation = new ActivationType();
        userActivation.lockoutStatus(LockoutStatusType.NORMAL);
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role);

        final ActivationType assignmentActivation = new ActivationType();
        final XMLGregorianCalendar activationDate = TestUtil.currentTime();
        assignmentActivation.validFrom(activationDate);
        final UserType user = createUserRudy();
        user.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(assignmentActivation)
                .end();
        final ObjectDelta<UserType> userDelta = user.asPrismObject().createAddDelta();

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

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
    void userRoleIsUnassigned_formatVisualizationIsCalled_unassignedRoleShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final Task testTask = getTestTask();
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role);

        final UserType oldUser = createUserRudy();
        final UserType modifiedUser = oldUser.clone();
        oldUser.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(new ActivationType())
                .end();
        repoAddObject(oldUser);

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tRole "Accounting" unassigned:
                |\t|\tDelete "Activation\"""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void userRoleWithActivationIsUnassigned_formatVisualizationIsCalled_unassignedRoleShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role);

        final UserType oldUser = createUserRudy();
        final UserType modifiedUser = oldUser.clone();
        final XMLGregorianCalendar validFrom = TestUtil.currentTime();
        oldUser.beginAssignment()
                .targetRef(role.getOid(), RoleType.COMPLEX_TYPE)
                .activation(new ActivationType().validFrom(validFrom))
                .end();
        repoAddObject(oldUser);

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject());

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tRole "Accounting" unassigned:
                |\t|\tDelete Activation:
                |\t|\t|\tValid from: %s""".formatted(validFrom);
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void hidingOperationalPropertiesIsOn_formatVisualizationIsCalled_operationalPropertiesShouldNotBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        repoAddObject(oldUser);

        final UserType modifiedUser = oldUser.clone();
        modifiedUser.activation(new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED)
                .effectiveStatus(ActivationStatusType.DISABLED)); // Effective status is operational property

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject(),
                ParameterizedEquivalenceStrategy.DEFAULT_FOR_EQUALS);

        final Visualization visualization = createVisualization(userDelta, true, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tUser was disabled:
                |\t|\tAdded properties:
                |\t|\t|\tAdministrative status: %s"""
                .formatted(StringUtils.capitalize(ActivationStatusType.DISABLED.value()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void hidingOperationalPropertiesIsOff_formatVisualizationIsCalled_operationalPropertiesShouldBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        repoAddObject(oldUser);

        final UserType modifiedUser = oldUser.clone();
        modifiedUser.activation(new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED)
                .effectiveStatus(ActivationStatusType.DISABLED)); // Effective status is operational property

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject(),
                ParameterizedEquivalenceStrategy.DEFAULT_FOR_EQUALS);

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tUser was disabled:
                |\t|\tAdded properties:
                |\t|\t|\tAdministrative status: %s
                |\t|\t|\tEffective status: %s"""
                .formatted(StringUtils.capitalize(ActivationStatusType.DISABLED.value()),
                        StringUtils.capitalize(ActivationStatusType.DISABLED.value()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void effectiveStatusAddedToHiddenPaths_formatVisualizationIsCalled_effectiveStatusShouldNotBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        repoAddObject(oldUser);

        final UserType modifiedUser = oldUser.clone();
        modifiedUser.activation(new ActivationType()
                .administrativeStatus(ActivationStatusType.DISABLED)
                .effectiveStatus(ActivationStatusType.DISABLED)); // Effective status is operational property

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject(),
                ParameterizedEquivalenceStrategy.DEFAULT_FOR_EQUALS);
        final List<ItemPath> pathsToHide = List.of(UserType.F_ACTIVATION.append(ActivationType.F_EFFECTIVE_STATUS));

        final Visualization visualization = createVisualization(userDelta, false, pathsToHide);
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tUser was disabled:
                |\t|\tAdded properties:
                |\t|\t|\tAdministrative status: %s"""
                .formatted(StringUtils.capitalize(ActivationStatusType.DISABLED.value()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test(enabled = false, description = "This is currently known limitation. Visualization will contain description "
            + "of a change also when the only changed properties were operational, regardless of the configuration of"
            + " visualizer to hide operational data.")
    void onlyOperationalPropertyChangedButOperationPropertiesAreHidden_formatVisualizationIsCalled_nothingShouldBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        repoAddObject(oldUser);

        final UserType modifiedUser = oldUser.clone();
        modifiedUser.activation(new ActivationType()
                .effectiveStatus(ActivationStatusType.DISABLED)); // Effective status is operational property

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(modifiedUser.asPrismObject(),
                ParameterizedEquivalenceStrategy.DEFAULT_FOR_EQUALS);

        final Visualization visualization = createVisualization(userDelta, true, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        Assertions.assertThat(formattedDelta).isEmpty();
    }

    @Test
    void metadataSetToBeShown_formatVisualizationIsCalled_metadataShouldBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final XMLGregorianCalendar now = TestUtil.currentTime();
        final UserType userRudy = createUserRudy();
        repoAddObject(userRudy);

        final ObjectDelta<Objectable> userDelta = this.prismContext.deltaFor(UserType.class)
        .item(UserType.F_GIVEN_NAME).add("Ferdo")
        .item(InfraItemName.METADATA, new IdItemPathSegment(0L), ValueMetadataType.F_STORAGE,
                StorageMetadataType.F_MODIFY_TIMESTAMP).add(now)
        .asObjectDelta(userRudy.getOid());

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());

        final String formattedDelta = this.formatter.formatVisualization(visualization);
        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tAdded properties:
                |\t|\tGiven name: Ferdo
                |\tStorage has been modified:
                |\t|\tModified at: %s"""
                .formatted(now.toXMLFormat());
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void metadataSetToBeHidden_formatVisualizationIsCalled_metadataShouldNotBeShown()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final XMLGregorianCalendar now = TestUtil.currentTime();
        final UserType userRudy = createUserRudy();
        repoAddObject(userRudy);

        final ObjectDelta<Objectable> userDelta = this.prismContext.deltaFor(UserType.class)
                .item(UserType.F_GIVEN_NAME).add("Ferdo")
                .item(InfraItemName.METADATA, new IdItemPathSegment(0L), ValueMetadataType.F_STORAGE,
                        StorageMetadataType.F_MODIFY_TIMESTAMP).add(now)
                .asObjectDelta(userRudy.getOid());

        final Visualization visualization = createVisualization(userDelta, true, Collections.emptyList());

        final String formattedDelta = this.formatter.formatVisualization(visualization);
        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tAdded properties:
                |\t|\tGiven name: Ferdo""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    private void repoAddObject(ObjectType object)
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        repoAddObject(object.asPrismObject(), getTestTask().getResult());
        this.addedObjects.add(object);
    }

    private static UserType createUserRudy() {
        final String userOid = UUID.randomUUID().toString();
        final UserType user = new UserType();
        return user.oid(userOid)
                .name(new PolyStringType("Rudy"))
                .activation(new ActivationType());
    }

    private Visualization createVisualization(ObjectDelta<? extends Objectable> delta,
            boolean hideOperationalAttributes, Collection<ItemPath> pathsToHide) {
        final Task task = getTestTask();
        final VisualizationContext context = new VisualizationContext();
        context.setPathsToHide(pathsToHide);
        context.setIncludeOperationalItems(!hideOperationalAttributes);
        context.setIncludeMetadata(!hideOperationalAttributes);
        try {
            return this.visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, null, context, true, task,
                    task.getResult());
        } catch (SchemaException | ExpressionEvaluationException e) {
            throw new RuntimeException(e);
        }
    }
}
package com.evolveum.midpoint.notifications.impl.formatters;

import static com.evolveum.midpoint.test.DummyDefaultScenario.Account.AttributeNames.DESCRIPTION;
import static com.evolveum.midpoint.test.DummyDefaultScenario.Account.AttributeNames.FULLNAME;
import static com.evolveum.midpoint.test.DummyDefaultScenario.Account.AttributeNames.INTERNAL_ID;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
import com.evolveum.midpoint.model.api.visualizer.Visualization;
import com.evolveum.midpoint.model.api.visualizer.VisualizationDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.VisualizationItem;
import com.evolveum.midpoint.model.impl.visualizer.VisualizationContext;
import com.evolveum.midpoint.model.impl.visualizer.Visualizer;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipalManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class VisualizationBasedDeltaFormatterTest extends AbstractIntegrationTest {

    private static final String RESOURCE_FILE_PATH = "src/test/resources/objects/resource-dummy.xml";
    private final List<ObjectType> addedObjects = new ArrayList<>();
    private final IndentationGenerator indentationGenerator = new IndentationGenerator("|", "\t");

    @Autowired
    private Visualizer visualizer;
    @Autowired
    private MidPointPrincipalManager principalManager;
    @Autowired
    private ProvisioningService provisioningService;

    private ResourceType resource;

    private VisualizationBasedDeltaFormatter formatter;

    @Override
    public void initSystem() throws Exception {
        super.initSystem();
        final Task initTask = createTask();
        this.provisioningService.postInit(initTask.getResult());
        ((LocalizationServiceImpl)this.localizationService).init();
        ((LocalizationServiceImpl)this.localizationService).setOverrideLocale(Locale.US);
        repoAddObjectFromFile(new File("src/test/resources/objects/role-superuser.xml"), RoleType.class,
                initTask.getResult());
        repoAddObjectFromFile(new File("src/test/resources/objects/user-administrator.xml"), UserType.class,
                initTask.getResult());
        final PrismObject<ResourceType> loadedResource = addResourceFromFile(new File(RESOURCE_FILE_PATH),
                IntegrationTestTools.DUMMY_CONNECTOR_TYPE, true, initTask.getResult());
        final DummyResourceContoller dummyResourceContoller = DummyResourceContoller.create(null, loadedResource);
        dummyResourceContoller.populateWithDefaultSchema();
        this.resource = this.provisioningService.getObject(ResourceType.class, loadedResource.getOid(), null, initTask,
                initTask.getResult()).asObjectable();
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
                new ContainerPropertiesModificationFormatter(propertiesFormatter, this.indentationGenerator,
                        modifiedPropertiesFormatter);
        final PropertiesFormatter<VisualizationItem> additionalIdentificationFormatter =
                new AdditionalIdentificationFormatter(propertiesFormatter, this.indentationGenerator);
        this.formatter = new VisualizationBasedDeltaFormatter(propertiesFormatter, additionalIdentificationFormatter,
                containerPropertiesModificationFormatter, indentationGenerator, this.localizationService);
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
        final RoleType role = new RoleType();
        role.name("Accounting");
        repoAddObject(role);

        final ActivationType assignmentActivation = new ActivationType();
        final XMLGregorianCalendar activationDate = TestUtil.currentTime();
        assignmentActivation.validFrom(activationDate);
        final ActivationType userActivation = new ActivationType();
        userActivation.lockoutStatus(LockoutStatusType.NORMAL);
        final UserType user = createUserRudy();
        user.activation(userActivation)
                .beginAssignment()
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
    void levelOfClassLoggerIsChanged_formatVisualizationIsCalled_changedLogLevelShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final SystemConfigurationType systemConfig = new SystemConfigurationType();
        final String loggingPackage = "com.evolveum.*";
        systemConfig.name("config")
                .logging(new LoggingConfigurationType());
        repoAddObject(systemConfig);
        final SystemConfigurationType configWithChangedLogger = systemConfig.clone();
        configWithChangedLogger.getLogging().getClassLogger().add(new ClassLoggerConfigurationType()
                ._package(loggingPackage)
                .level(LoggingLevelType.DEBUG));

        final ObjectDelta<SystemConfigurationType> configDelta = systemConfig.asPrismObject().diff(
                configWithChangedLogger.asPrismObject());

        final Visualization visualization = createVisualization(configDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                System configuration "config" has been modified:
                |\tAdd level "%s" for logger "%s":
                |\t|\tpackage: %s
                |\t|\tlevel: %s""".formatted(LoggingLevelType.DEBUG, loggingPackage, loggingPackage,
                StringUtils.capitalize(LoggingLevelType.DEBUG.value().toLowerCase()));
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void passwordIsChanged_formatVisualizationIsCalled_passwordShouldBeProperlyFormatted()
            throws SchemaException, EncryptionException, ObjectAlreadyExistsException {
        final UserType oldUser = createUserRudy();
        repoAddObject(oldUser);
        final UserType userWithPassword = oldUser.clone();
        userWithPassword.credentials(new CredentialsType()
                .password(new PasswordType()
                        .value(new ProtectedStringType().clearValue("1111"))));

        final ObjectDelta<UserType> userDelta = oldUser.asPrismObject().diff(
                userWithPassword.asPrismObject());

        final Visualization visualization = createVisualization(userDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        // There is a known bug somewhere in the `PasswordDescriptionHandler`, which causes this weird nested "Password
        // created" overviews. The first should be about added credentials.
        final String expectedDeltaFormat = """
                User "Rudy" has been modified:
                |\tPassword created:
                |\t|\tPassword created:
                |\t|\t|\tValue: (protected string)""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void shadowIsAdded_formatVisualizationIsCalled_shadowShouldBeProperlyFormatted() {
        final ShadowType accountRudy = new ShadowType()
                .kind(ShadowKindType.ACCOUNT)
                .name("Rudy")
                .intent("HR Account")
                .resourceRef(new ObjectReferenceType()
                        .oid(UUID.randomUUID().toString())
                        .targetName("HR System")
                );

        final ObjectDelta<ShadowType> accountDelta = accountRudy.asPrismObject().createAddDelta();
        final Visualization visualization = createVisualization(accountDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                Account "Rudy" (HR Account) created on "HR System":
                |\tName: Rudy
                |\tKind: Account
                |\tIntent: HR Account
                |\tResource: HR System""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    @Test
    void shadowIsModified_formatVisualizationIsCalled_shadowShouldBeProperlyFormatted()
            throws SchemaException, ObjectAlreadyExistsException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            PolicyViolationException {
        final PrismObject<ShadowType> accountRudy = Resource.of(this.resource)
                .shadow(ResourceObjectTypeIdentification.of(ShadowKindType.ACCOUNT, "HR Account"))
                .withSimpleAttribute(SchemaConstants.ICFS_NAME, "Rudy")
                .withSimpleAttribute(FULLNAME.q(), "Rudy M.")
                .withSimpleAttribute(INTERNAL_ID.q(), 1)
                .asPrismObject();
        this.provisioningService.addObject(accountRudy, null, null, getTestTask(), getTestTask().getResult());

        final PrismObject<ShadowType> modifiedAccount = accountRudy.mutableCopy();
        ShadowUtil.getAttributesContainer(modifiedAccount)
                .addSimpleAttribute(DESCRIPTION.q(), "HR");

        final ObjectDelta<ShadowType> accountDelta = accountRudy.diff(modifiedAccount);
        final Visualization visualization = createVisualization(accountDelta, false, Collections.emptyList());
        final String formattedDelta = this.formatter.formatVisualization(visualization);

        final String expectedDeltaFormat = """
                Account "Rudy" (HR Account) modified on "HR System":
                |\tAdditional identification (not modified data):
                |\t|\tResource: HR System
                |\t|\tKind: Account
                |\t|\tIntent: HR Account
                |\tattributes has been modified:
                |\t|\tAdded properties:
                |\t|\t|\tdescription: HR""";
        Assertions.assertThat(formattedDelta).isEqualTo(expectedDeltaFormat);
    }

    // Tests for hiding/showing of operational data and metadata

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
                |\t"@metadata/[0]/storage" has been modified:
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
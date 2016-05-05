import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.common.security.GuardedString;

log.info("Entering openStack schema Script");

// Declare the Account attributes
// id
accountNameAIB = new AttributeInfoBuilder("accountName",String.class);
accountNameAIB.setRequired(true);
accountNameAIB.setCreateable(true);
accountNameAIB.setMultiValued(false);
accountNameAIB.setUpdateable(true);

passwordAIB = new AttributeInfoBuilder("password", GuardedString.class);
passwordAIB.setRequired(false);
passwordAIB.setCreateable(true);
passwordAIB.setMultiValued(false);
passwordAIB.setUpdateable(true);

emailAIB = new AttributeInfoBuilder("email", String.class);
emailAIB.setRequired(false);
emailAIB.setCreateable(true);
emailAIB.setMultiValued(false);
emailAIB.setUpdateable(true);

tenantIdAIB = new AttributeInfoBuilder("tenantId", String.class);
tenantIdAIB.setRequired(false);
tenantIdAIB.setCreateable(true);
tenantIdAIB.setUpdateable(true);
tenantIdAIB.setMultiValued(true);

enabledAIB = new AttributeInfoBuilder("enabled", Boolean.class);
enabledAIB.setRequired(false);
enabledAIB.setCreateable(true);
enabledAIB.setUpdateable(true);
enabledAIB.setMultiValued(false);

usernameAIB = new AttributeInfoBuilder("username", String.class);
usernameAIB.setRequired(false);
usernameAIB.setCreateable(true);
usernameAIB.setUpdateable(false);
usernameAIB.setMultiValued(false);

// Declare the VM attributes
// id
idAIB = new AttributeInfoBuilder("id",String.class);
idAIB.setRequired(true);
idAIB.setCreateable(true);
idAIB.setMultiValued(false);
idAIB.setUpdateable(false);

// tenantName
projectNameAIB = new AttributeInfoBuilder("projectName",String.class);
projectNameAIB.setCreateable(true);
projectNameAIB.setMultiValued(false);
projectNameAIB.setUpdateable(true);

// projectId
ipAddressAIB = new AttributeInfoBuilder("ipAddress",String.class);
ipAddressAIB.setCreateable(false);
ipAddressAIB.setMultiValued(false);
ipAddressAIB.setUpdateable(false);


// projectId
projectIdAIB = new AttributeInfoBuilder("projectId",String.class);
projectIdAIB.setCreateable(false);
projectIdAIB.setMultiValued(false);
projectIdAIB.setUpdateable(false);

// serverName
serverNameAIB = new AttributeInfoBuilder("serverName",String.class);
serverNameAIB.setCreateable(true);
serverNameAIB.setMultiValued(false);
serverNameAIB.setUpdateable(true);

// imageRef
imageRefAIB = new AttributeInfoBuilder("imageRef",String.class);
imageRefAIB.setCreateable(true);
imageRefAIB.setMultiValued(false);
imageRefAIB.setUpdateable(true);

// flavorRef
flavorRefAIB = new AttributeInfoBuilder("flavorRef",String.class);
flavorRefAIB.setCreateable(true);
flavorRefAIB.setMultiValued(false);
flavorRefAIB.setUpdateable(true);

// networkId
networkIdAIB = new AttributeInfoBuilder("networkId",String.class);
networkIdAIB.setCreateable(true);
networkIdAIB.setMultiValued(true);
networkIdAIB.setUpdateable(true);


vmAttrsInfo = new HashSet<AttributeInfo>();
vmAttrsInfo.add(projectNameAIB.build());
vmAttrsInfo.add(projectIdAIB.build());
vmAttrsInfo.add(imageRefAIB.build());
vmAttrsInfo.add(flavorRefAIB.build());
vmAttrsInfo.add(networkIdAIB.build());
vmAttrsInfo.add(serverNameAIB.build());
vmAttrsInfo.add(ipAddressAIB.build());

projectAttrsInfo = new HashSet<AttributeInfo>();
projectAttrsInfo.add(projectNameAIB.build());
projectAttrsInfo.add(projectIdAIB.build());

accountAttrsInfo = new HashSet<AttributeInfo>();
accountAttrsInfo.add(accountNameAIB.build());
accountAttrsInfo.add(passwordAIB.build());
accountAttrsInfo.add(emailAIB.build());
accountAttrsInfo.add(tenantIdAIB.build());
accountAttrsInfo.add(enabledAIB.build());
accountAttrsInfo.add(usernameAIB.build());

// Create the VM Object class
final ObjectClassInfo ociVM = new ObjectClassInfoBuilder().setType("Server").addAllAttributeInfo(vmAttrsInfo).build();
builder.defineObjectClass(ociVM);

final ObjectClassInfo ociProject = new ObjectClassInfoBuilder().setType("Project").addAllAttributeInfo(projectAttrsInfo).build();
builder.defineObjectClass(ociProject);

final ObjectClassInfo ociAccount = new ObjectClassInfoBuilder().setType("Account").addAllAttributeInfo(accountAttrsInfo).build();
builder.defineObjectClass(ociAccount);

log.info("OpenStack Schema script done");


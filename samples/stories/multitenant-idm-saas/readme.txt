OpenLDAP customer management test scenario.

This scenario simulates situation where multiple organizations ("tenants")
wish to manage their own users and administrators in midPoint and in OpenLDAP.
The first administrative accounts will be created by importing (one-time or
periodic using LiveSync) from CSV file to midPoint. Passwords will be sent to the first
administrative accounts (simulated by midPoint notifications redirected to
a file). The administrators can then manage their own users.

For each organization ("tenant"), the following objects will be created in
OpenLDAP (assumed the "ou=customers,dc=example,dc=com" already exists!):
1) OU: ou=<customerName>,ou=customers,dc=example,dc=com
2) group: cn=<customerName>-admins,ou=<customerName>,ou=customers,dc=example,dc=com
3) group: cn=<customerName>-users,ou=<customerName>,ou=customers,dc=example,dc=com
4) group: cn=<customerName>-powerusers,ou=<customerName>,ou=customers,dc=example,dc=com

For each user from CSV, user with identifier <name> will be created in
midPoint, employeeType will be set to "customer-admin", which causes the
following to be automatically assigned in midPoint:
1) organization <customerName> (member)
2) organization <customerName> (manager)
3) role Customer Admin Role
4) role Customer User Role
5) role Delegated Administration Role

In OpenLDAP, the following will be automatically created:
1) account uid=<name>,ou=<customerName>,ou=customers,dc=example,dc=com
2) account added to group cn=<customerName>-admins,ou=<customerName>,ou=customers,dc=example,dc=com
3) account added to group cn=<customerName>-users,ou=<customerName>,ou=customers,dc=example,dc=com

The initial password is generated using Default Password Policy and will be
sent (simulated by mail notification redirection to
<tomcat>/logs/idm-mail-notifications.log</tomcat>) to the admin user.

Admin user can login to midPoint using http://localhost:8080/midpoint/. From
the GUI he can display the organizational structure using "Org. structure"
menu on the left side.

To modify organization settings:
- click Org. structure on the left side, then Organization tree
- click the wheel button near the organization name in the tree part of the
page and choose Edit
- click "Show empty fields" icon if necessary
- update attribute(s), e.g. Description if necessary
- change password policy which will be applied for all objects in this
organization
- save the form

Based on your changes, attributes in OpenLDAP will be changed.
Password policy if midPoint-only setting, so changing it will not provision
changes to OpenLDAP.

To create new administrator or user for his/her organization:
- click Org. structure on the left side, then Organization tree
- (do not use Users / New user to create user; there is no relationship to
organization and permission will be denied (no attributes will be displayed)

- click the organization name on the left side to expand current users
- on the right side, in the header of "Members" table, click the wheel icon
and select "Create member" and confirm "UserType" to open new user form
- fill in the following attributes:
  - name (this is midPoint login and OpenLDAP uid attribute)
  - givenName
  - familyName
  - employeeType: click to the field and choose either customer-admin or
  customer-user
  - emailAddress: to send (simulate sending) notification with initial password
  - password
- save the form

Based on the employeeType setting, user will be created either as
administrator or normal user:

1) assigned organization <customerName> (member) (for both customer-admin and
customer-user)
2) organization <customerName> (manager) (for customer-admin)
3) role Customer Admin Role (for customer-admin)
4) role Customer User Role (for both customer-admin and customer-user)
5) role Delegated Administration Role (for customer-admin)

In OpenLDAP, the following will be automatically created:
1) account uid=<name>,ou=<customerName>,ou=customers,dc=example,dc=com
2) account added to group
cn=<customerName>-admins,ou=<customerName>,ou=customers,dc=example,dc=com (for
customer-admin)
3) account added to group
cn=<customerName>-users,ou=<customerName>,ou=customers,dc=example,dc=com (for
both customer-admin and customer-user)

To modify user, certain attributes can be modified from within midPoint, such
as givenName, familyName, name, employeeType, password etc. Some attributes
will be computed by midPoint (fullName) and cannot be edited directly.

To change the permissions from customer-admin to customer-user and vice-versa,
change the employeeType attribute value in midPoint. Groups and organization
will be automatically (un)assigned based on this setting.

To assign user a different role using midPoint, click on the user and switch
to tab "Assignments":
- click on the wheel near "Assignments" header
- click Assign
- select "RoleType" in "Choose member type" select box
- select role(s). For example Customer Power User role
- click Assign and then Save

User will be updated and OpenLDAP account will be updated if role(s) related
to OpenLDAP have been (un)assigned. In the case of the "Customer Power User
Role", the membership of the following group will be updated:

1) group: cn=<customerName>-powerusers,ou=<customerName>,ou=customers,dc=example,dc=com

You can assign the Customer Admin Role to the normal user if you wish, but
this role is automatically assigned/unassigned based on employeeType attribute
value.

To rename the account, edit the user and change "name" attribute. The account
in OpenLDAP will be automatically renamed and the group membership will be
also updated.

To change the password, edit the user and change the Password value.

Disable/enable is not supported in this scenario, as OpenLDAP has no default
way of disabling users. For now custom objectClass (instead of inetOrgPerson)
would need to be used (which would also mean all target applications bound
with OpenLDAP would need to know which attribute represents the account
state).

To Delete user you need to go to Users then List users. Select the users you wish to
delete and in the wheel choose "Delete" action. User will be deleted from
midPoint and the account from OpenLDAP.

To allow normal (non-admin) users to change their own password in the
self-service GUI, users first need to have "Customer End Role" role assigned
(this is now automatically assigned by object template for both customer-user
and customer-admin users).

From now, the user can login to midPoint using http://localhost:8080/midpoint
(the same URL is used for administration) and update his/her password in
midPoint and OpenLDAP.

Setup:
- tested with Tomcat 7.x, JDK 7.x, v3.4devel-1465-g49a0f7b
- update filePath in the CSV resource before importing data! It must point to
your CSV file with customer info!

- set/ensure: System Configuration: Assignment Policy Enforcement: Relative
- Import organizational structure
- Import password policies
- Import object templates
- Import resources
- Import roles (Please do not import/use "role-meta-ldap-customer-group.xml",
it's work in progress.)
- System Configuration: Object Policies: UserType = "User Template"
- System Configuration: Object Policies: OrgType = "Organization Object Template"
- Resource "CRM Simulation" - click Content, and import any of the entries by
clicking the wheel and selecting Import. To import all, you can run Import
from resource task. To allow periodic synchronization, you can configure Live
Sync task.
- edit System Configuration using repository objects and replace
<notificationConfiguration> with the sample in misc/sysconfig-readme.txt
- Notifications will be redirected to <tomcat>/logs/idm-mail-notification.log


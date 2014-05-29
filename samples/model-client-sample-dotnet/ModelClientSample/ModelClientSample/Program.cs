using ModelClientSample.midpointModelService;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.Text;
using System.Threading.Tasks;
using System.Xml;
using System.Xml.Serialization;

namespace ModelClientSample
{
    class Program
    {
        private const string LOGIN_USERNAME = "administrator";
        private const string LOGIN_PASSWORD = "5ecr3t";

        private const string USER_LECHUCK_FILE = "..\\..\\user-lechuck.xml";

        private const string NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
        private const string NS_Q = "http://prism.evolveum.com/xml/ns/public/query-3";

        private static XmlQualifiedName USER_TYPE = new XmlQualifiedName("UserType", NS_C);
        private static XmlQualifiedName ROLE_TYPE = new XmlQualifiedName("RoleType", NS_C);
        private static XmlQualifiedName TASK_TYPE = new XmlQualifiedName("TaskType", NS_C);
        private static XmlQualifiedName RESOURCE_TYPE = new XmlQualifiedName("ResourceType", NS_C);
        private static XmlQualifiedName SYSTEM_CONFIGURATION_TYPE = new XmlQualifiedName("SystemConfigurationType", NS_C);

        private const string WS_URL = "http://localhost:8080/midpoint/model/model-3?wsdl";   // when using fiddler, change "localhost" to "localhost."

        private const string SYSTEM_CONFIGURATION_OID = "00000000-0000-0000-0000-000000000001";
        private const string ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";

        private const string ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-987987987988";
        private const string ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-987987cccccc";

        static void Main(string[] args)
        {
            try
            {
                Main1(args);
            }
            catch (Exception e)
            {
                Console.WriteLine(e.ToString());
            }
            Console.Write("Press any key...");
            Console.ReadKey();
        }

        static void Main1(string[] args)
        {
            modelPortType modelPort = openConnection();

            Console.WriteLine("Getting system configuration..."); 
            SystemConfigurationType configurationType = getConfiguration(modelPort);
            Console.WriteLine(dumpSystemConfiguration(configurationType));
            
            Console.WriteLine("========================================================="); 
            Console.WriteLine("Getting administrator user...");
            UserType userAdministrator = searchUserByName(modelPort, "administrator");
            Console.WriteLine(dumpUser(userAdministrator));

            Console.WriteLine("========================================================="); 
            Console.WriteLine("Getting Sailor role...");
            RoleType sailorRole = searchRoleByName(modelPort, "Sailor");
            if (sailorRole != null)
            {
                Console.WriteLine(dumpRole(sailorRole));
            }
            else
            {
                Console.WriteLine("No Sailor role in the system.");
            }

            Console.WriteLine("=========================================================");
            Console.WriteLine("Getting resources...");
            // please note: ObjectType is ObjectType in prism module, ObjectType1 is ObjectType in schema module (i.e. the "real" c:ObjectType)
            ObjectType1[] resources = listObjects(modelPort, RESOURCE_TYPE);
            Console.WriteLine(dump(resources));

            Console.WriteLine("=========================================================");
            Console.WriteLine("Getting users...");
            ObjectType1[] users = listObjects(modelPort, USER_TYPE);
            Console.WriteLine(dump(users));

            Console.WriteLine("=========================================================");
            Console.WriteLine("Getting users (first three, sorted by name)...");
            ObjectType1[] users2 = listObjectsRestrictedAndSorted(modelPort, USER_TYPE);
            Console.WriteLine(dump(users2));

            Console.WriteLine("=========================================================");
            Console.WriteLine("Getting tasks...");
            TaskType[] tasks = listTasks(modelPort);
            Console.WriteLine(dump(tasks));
            Console.WriteLine("Next scheduled times: ");
            foreach (TaskType taskType in tasks) {
                Console.WriteLine(" - " + getOrig(taskType.name) + ": " + taskType.nextRunStartTimestamp);
            }

            Console.WriteLine("=========================================================");
            Console.WriteLine("Creating user guybrush...");
            String userGuybrushoid = createUserGuybrush(modelPort, sailorRole);
            Console.WriteLine("Created with OID: " + userGuybrushoid);
			
            Console.WriteLine("=========================================================");
            Console.WriteLine("Creating user lechuck...");
            //String userLeChuckOid = createUserFromFile(modelPort, USER_LECHUCK_FILE);             deserializing from file doesn't work for unknown reason
            String userLeChuckOid = createUserLechuck(modelPort);
            Console.WriteLine("Created with OID: " + userLeChuckOid);
			
            Console.WriteLine("=========================================================");
            Console.WriteLine("Changing password for guybrush...");
            changeUserPassword(modelPort, userGuybrushoid, "MIGHTYpirate");
            Console.WriteLine("Done.");

            Console.WriteLine("=========================================================");
            Console.WriteLine("Changing given name for lechuck...");
            changeUserGivenName(modelPort, userLeChuckOid, "CHUCK");
            Console.WriteLine("Done.");
			
            Console.WriteLine("=========================================================");
            Console.WriteLine("Assigning roles to guybrush...");
            assignRoles(modelPort, userGuybrushoid, new string[] { ROLE_PIRATE_OID, ROLE_CAPTAIN_OID });
            Console.WriteLine("Done.");
            
            Console.WriteLine("=========================================================");
            Console.WriteLine("Unassigning a role from guybrush...");
			unAssignRoles(modelPort, userGuybrushoid, new string[] { ROLE_CAPTAIN_OID });
            Console.WriteLine("Done.");
			
            Console.WriteLine("=========================================================");
            Console.WriteLine("Getting requestable roles...");
            ObjectType1[] roles = listRequestableRoles(modelPort);
            Console.WriteLine(dump(roles));
			
            // Comment-out the following lines if you want to see what midPoint really did
            // ... because deleting the user will delete also all the traces (except logs and audit of course).
            Console.WriteLine("=========================================================");
            Console.WriteLine("Deleting users guybrush and lechuck...");
            deleteUser(modelPort, userGuybrushoid);
            deleteUser(modelPort, userLeChuckOid);
            Console.WriteLine("Done.");
        }

        private static string getOrig(PolyStringType polystring)
        {
            if (polystring == null)
            {
                return null;
            }
            else if (polystring.orig != null)
            {
                return polystring.orig;
            }
            else if (polystring.Any != null)
            {
                StringBuilder sb = new StringBuilder();
                foreach (XmlNode any in polystring.Any)
                {
                    if (any.InnerText != null)
                    {
                        sb.Append(any.InnerText.Trim());
                    }
                }
                return sb.ToString();
            }
            else
            {
                return null;
            }
        }

        private static string dump(ObjectType1[] objects) 
        {
            if (objects == null)
            {
                return "No objects.";
            }
            StringBuilder sb = new StringBuilder();
            sb.Append("Objects returned: ").Append(objects.Length).Append("\n");
            foreach (ObjectType1 obj in objects)
            {
                sb.Append(" - ").Append(getOrig(obj.name)).Append("\n");
            }
            return sb.ToString();
        }

        private static string dumpUser(UserType user)
        {
            if (user == null)
            {
                return "NO SUCH USER";
            }
            StringBuilder sb = new StringBuilder();
            sb.Append("User object: ").Append(getOrig(user.name)).Append(" (").Append(getOrig(user.fullName)).Append("); oid = ").Append(user.oid);
            return sb.ToString();
        }

        private static string dumpRole(RoleType role)
        {
            if (role == null)
            {
                return "NO SUCH ROLE";
            }
            StringBuilder sb = new StringBuilder();
            sb.Append("Role object: ").Append(getOrig(role.name));
            if (role.requestable)
            {
                sb.Append(" (REQUESTABLE)");
            }
            else
            {
                sb.Append(" (NON-REQUESTABLE)");
            }
            sb.Append("; oid = ").Append(role.oid);
            return sb.ToString();
        }

        private static string dumpSystemConfiguration(SystemConfigurationType configurationType)
        {
            StringBuilder sb = new StringBuilder();
            sb.Append("System configuration object:");
            sb.Append("\n- name = ").Append(configurationType.name);
            sb.Append("\n- oid = ").Append(configurationType.oid);
            return sb.ToString();
        }

        private static SystemConfigurationType getConfiguration(modelPortType modelPort)
        {
            getObject request = new getObject(SYSTEM_CONFIGURATION_TYPE, SYSTEM_CONFIGURATION_OID, null);
            getObjectResponse response = modelPort.getObject(request);
            return (SystemConfigurationType)response.@object;
        }

        private static ObjectType1[] listObjects(modelPortType modelPort, XmlQualifiedName type)
        {
            searchObjects request = new searchObjects(type, null, null);
            searchObjectsResponse response = modelPort.searchObjects(request);

            ObjectListType objectList = response.objectList;
            ObjectType1[] objects = objectList.@object;
            return objects;
        }

        private static ObjectType1[] listObjectsRestrictedAndSorted(modelPortType modelPort, XmlQualifiedName type)
        {
            // let's say we want to get first 3 users, sorted alphabetically by user name

            QueryType queryType = new QueryType();          // holds search query + paging options
            PagingType pagingType = new PagingType();
            pagingType.maxSize = 3;
            ItemPathType orderBy = createItemPathType("name");
            pagingType.orderBy = orderBy;
            pagingType.orderDirection = OrderDirectionType.ascending;

            queryType.paging = pagingType;

            searchObjects request = new searchObjects(type, queryType, null);
            searchObjectsResponse response = modelPort.searchObjects(request);

            ObjectListType objectList = response.objectList;
            ObjectType1[] objects = objectList.@object;
            return objects;
        }

        private static ItemPathType createItemPathType(string path)
        {
 	        ItemPathType rv = new ItemPathType();
            rv.Value = "declare default namespace '" + NS_C + "'; " + path;
            return rv;
        }

        private static TaskType[] listTasks(modelPortType modelPort) 
        {
            // Let's say we want to retrieve tasks' next scheduled time (because this may be a costly operation if
            // JDBC based quartz scheduler is used, the fetching of this attribute has to be explicitly requested)
            SelectorQualifiedGetOptionType getNextScheduledTimeOption = new SelectorQualifiedGetOptionType();

            // prepare a selector (described by path) + options (saying to retrieve that attribute)
            ObjectSelectorType selector = new ObjectSelectorType();
            selector.path = createItemPathType("nextRunStartTimestamp");
            getNextScheduledTimeOption.selector = selector;
        
            GetOperationOptionsType selectorOptions = new GetOperationOptionsType();
            selectorOptions.retrieve = RetrieveOptionType.include;
            selectorOptions.retrieveSpecified = true;
            getNextScheduledTimeOption.options = selectorOptions;

            SelectorQualifiedGetOptionType[] operationOptions = new SelectorQualifiedGetOptionType[] { getNextScheduledTimeOption };

            searchObjects request = new searchObjects(TASK_TYPE, null, operationOptions);
            searchObjectsResponse response = modelPort.searchObjects(request);

            ObjectListType objectList = response.objectList;
            List<TaskType> tasks = new List<TaskType>();
            foreach (ObjectType1 object1 in response.objectList.@object)
            {
                tasks.Add((TaskType)object1);
            }
            return tasks.ToArray();
        }

        private static XmlElement parseXml(string xml)
        {
            XmlDocument doc = new XmlDocument();
            doc.LoadXml(xml);
            return doc.DocumentElement;
        }

        private static SearchFilterType createNameFilter(String name)
        {
            PropertyComplexValueFilterClauseType clause = new PropertyComplexValueFilterClauseType();
            clause.path = createItemPathType("name");
            clause.Item = name;

            SearchFilterType filter = new SearchFilterType();
            filter.Item = clause;
            filter.ItemElementName = ItemChoiceType1.equal;
            return filter;
        }

        private static ObjectType getOneObject(searchObjectsResponse response, String name)
        {
            ObjectType[] objects = response.objectList.@object;
            if (objects == null || objects.Length == 0)
            {
                return null;
            }
            else if (objects.Length == 1)
            {
                return (ObjectType)objects[0];
            }
            else
            {
                throw new InvalidOperationException("Expected to find a object with name '" + name + "' but found " + objects.Length + " ones instead");
            }
        }

        private static UserType searchUserByName(modelPortType modelPort, String username)
        {
            QueryType query = new QueryType();
            query.filter = createNameFilter(username);

            searchObjects request = new searchObjects(USER_TYPE, query, null);
            searchObjectsResponse response = modelPort.searchObjects(request);
            return (UserType) getOneObject(response, username);
        }

   	    private static RoleType searchRoleByName(modelPortType modelPort, String roleName) 
        {
            QueryType query = new QueryType();
            query.filter = createNameFilter(roleName);

            searchObjects request = new searchObjects(ROLE_TYPE, query, null);
            searchObjectsResponse response = modelPort.searchObjects(request);
            return (RoleType) getOneObject(response, roleName);
    	}

        private static String createUserGuybrush(modelPortType modelPort, RoleType role) 
        {
		    UserType user = new UserType();
		    user.name = createPolyStringType("guybrush");
		    user.fullName = createPolyStringType("Guybrush Threepwood");
		    user.givenName = createPolyStringType("Guybrush");
		    user.familyName = createPolyStringType("Threepwood");
		    user.emailAddress = "guybrush@meleeisland.net";
            user.organization = new PolyStringType[] { createPolyStringType("Pirate Brethren International") };
            user.organizationalUnit = new PolyStringType[] { createPolyStringType("Pirate Wannabes") };
            user.credentials = createPasswordCredentials("IwannaBEaPIRATE");
		
		    if (role != null) 
            {
			    // create user with a role assignment
			    AssignmentType roleAssignment = createRoleAssignment(role.oid);
                user.assignment = new AssignmentType[] { roleAssignment };
    		}
		
	    	return createUser(modelPort, user);
	    }

        private static String createUserLechuck(modelPortType modelPort)
        {
            UserType user = new UserType();
            user.name = createPolyStringType("lechuck");
            user.fullName = createPolyStringType("Ghost Pirate LeChuck");
            user.givenName = createPolyStringType("Chuck");
            user.familyName = createPolyStringType("LeChuck");
            user.honorificPrefix = createPolyStringType("Ghost Pirate");
            user.emailAddress = "lechuck@monkeyisland.com";
            user.telephoneNumber = "555-1234";
            user.employeeNumber = "emp1234";
            user.employeeType = new string[] { "UNDEAD" };
            user.locality = createPolyStringType("Monkey Island");
            user.credentials = createPasswordCredentials("4rrrrghhhghghgh!123");
            ActivationType activationType = new ActivationType();
            activationType.administrativeStatus = ActivationStatusType.enabled;
            user.activation = activationType;

            return createUser(modelPort, user);
        }

        private static CredentialsType createPasswordCredentials(string clearValue)
        {
            PasswordType passwordType = new PasswordType();
            passwordType.value = createProtectedStringType(clearValue);

     	    CredentialsType retval = new CredentialsType();
            retval.password = passwordType;
            return retval;
        }

        private static ProtectedStringType createProtectedStringType(string clearValue)
        {
            ProtectedStringType rv = new ProtectedStringType();
            rv.clearValue = clearValue;
            return rv;
        }


        private static PolyStringType createPolyStringType(string orig)
        {
 	        PolyStringType retval = new PolyStringType();
            retval.orig = orig;
            return retval;
        }

	    private static String createUserFromFile(modelPortType modelPort, String fileName) 
        {
            StreamReader reader = new StreamReader(fileName);
            XmlSerializer xmlSerializer = new XmlSerializer(typeof(UserType));
            UserType user = (UserType) xmlSerializer.Deserialize(reader);
            
            return createUser(modelPort, user);
    	}
	
	    private static String createUser(modelPortType modelPort, UserType userType)
        {
            ObjectDeltaType deltaType = new ObjectDeltaType();
            deltaType.objectType = USER_TYPE;
            deltaType.changeType = ChangeTypeType.add;
            deltaType.objectToAdd = userType;

            executeChanges request = new executeChanges(new ObjectDeltaType[] { deltaType }, null);
            executeChangesResponse response = modelPort.executeChanges(request);

		    return getOidFromDeltaOperationList(response.deltaOperationList, deltaType);
	    }

        /**
         * Retrieves OID created by model Web Service from the returned list of ObjectDeltaOperations.
         *
         * @param operationListType result of the model web service executeChanges call
         * @param originalDelta original request used to find corresponding ObjectDeltaOperationType instance. Must be of ADD type.
         * @return OID if found
         *
         * PRELIMINARY IMPLEMENTATION. Currently the first returned ADD delta with the same object type as original delta is returned.
         */
        public static string getOidFromDeltaOperationList(ObjectDeltaOperationType[] operations, ObjectDeltaType originalDelta) {
        
            if (originalDelta.changeType != ChangeTypeType.add) 
            {
                throw new ArgumentException("Original delta is not of ADD type");
            }
            if (originalDelta.objectToAdd == null) 
            {
                throw new ArgumentException("Original delta contains no object-to-be-added");
            }
            foreach (ObjectDeltaOperationType operationType in operations) 
            {
                ObjectDeltaType objectDeltaType = operationType.objectDelta;
                if (objectDeltaType.changeType == ChangeTypeType.add &&
                    objectDeltaType.objectToAdd != null) 
                {
                    ObjectType1 objectAdded = (ObjectType1) objectDeltaType.objectToAdd;
                    if (objectAdded.GetType().Equals(originalDelta.objectToAdd.GetType())) 
                    {
                        return objectAdded.oid;
                    }
                }
            }
            return null;
        }

	    private static void changeUserPassword(modelPortType modelPort, String oid, String newPassword) 
        {
		    ItemDeltaType passwordDelta = new ItemDeltaType();
		    passwordDelta.modificationType = ModificationTypeType.replace;
		    passwordDelta.path = createItemPathType("credentials/password/value");
            passwordDelta.value = new object[] { createProtectedStringType(newPassword) };

            ObjectDeltaType deltaType = new ObjectDeltaType();
            deltaType.objectType = USER_TYPE;
            deltaType.changeType = ChangeTypeType.modify;
            deltaType.oid = oid;
            deltaType.itemDelta = new ItemDeltaType[] { passwordDelta };

            executeChanges request = new executeChanges(new ObjectDeltaType[] { deltaType }, null);
            executeChangesResponse response = modelPort.executeChanges(request);
            check(response);
    	}

        private static void check(executeChangesResponse response)
        {
            foreach (ObjectDeltaOperationType objectDeltaOperation in response.deltaOperationList) 
            {
                if (!OperationResultStatusType.success.Equals(objectDeltaOperation.executionResult.status)) 
                {
                    Console.WriteLine("*** Operation result = " + objectDeltaOperation.executionResult.status + ": " 
                        + objectDeltaOperation.executionResult.message);
                }
            }
        }

        private static void changeUserGivenName(modelPortType modelPort, String oid, String newValue) 
        {
   		    ItemDeltaType itemDelta = new ItemDeltaType();
		    itemDelta.modificationType = ModificationTypeType.replace;
		    itemDelta.path = createItemPathType("givenName");
            itemDelta.value = new object[] { createPolyStringType(newValue) };

            ObjectDeltaType objectDelta = new ObjectDeltaType();
            objectDelta.objectType = USER_TYPE;
            objectDelta.changeType = ChangeTypeType.modify;
            objectDelta.oid = oid;
            objectDelta.itemDelta = new ItemDeltaType[] { itemDelta };

            executeChanges request = new executeChanges(new ObjectDeltaType[] { objectDelta }, null);
            executeChangesResponse response = modelPort.executeChanges(request);
            check(response);
        }

    	private static void assignRoles(modelPortType modelPort, String userOid, String[] roleOids)
        {
		    modifyRoleAssignment(modelPort, userOid, true, roleOids);
    	}
	
	    private static void unAssignRoles(modelPortType modelPort, String userOid, String[] roleOids) 
        {
		    modifyRoleAssignment(modelPort, userOid, false, roleOids);
	    }
	
	    private static void modifyRoleAssignment(modelPortType modelPort, String userOid, bool isAdd, String[] roleOids) 
        {
		    ItemDeltaType assignmentDelta = new ItemDeltaType();
		    if (isAdd) 
            {
			    assignmentDelta.modificationType = ModificationTypeType.add;
		    } 
            else 
            {
			    assignmentDelta.modificationType = ModificationTypeType.delete;
		    }
            assignmentDelta.path = createItemPathType("assignment");
            List<object> assignments = new List<object>();
		    foreach (String roleOid in roleOids) 
            {
                assignments.Add(createRoleAssignment(roleOid));
		    }
            assignmentDelta.value = assignments.ToArray();

            ObjectDeltaType objectDelta = new ObjectDeltaType();
            objectDelta.objectType = USER_TYPE;
            objectDelta.changeType = ChangeTypeType.modify;
            objectDelta.oid = userOid;
            objectDelta.itemDelta = new ItemDeltaType[] { assignmentDelta };

            executeChanges request = new executeChanges(new ObjectDeltaType[] { objectDelta }, null);
            executeChangesResponse response = modelPort.executeChanges(request);
            check(response);
    	}

	    private static AssignmentType createRoleAssignment(String roleOid) 
        {
		    AssignmentType roleAssignment = new AssignmentType();
		    ObjectReferenceType roleRef = new ObjectReferenceType();
		    roleRef.oid = roleOid;
		    roleRef.type = ROLE_TYPE;
		    roleAssignment.Item = roleRef;
		    return roleAssignment;
	    }

        private static ObjectType1[] listRequestableRoles(modelPortType modelPort)
        {
            SearchFilterType filter = createRequestableFilter();
            QueryType query = new QueryType();
            query.filter = filter;
            
            searchObjects request = new searchObjects(ROLE_TYPE, query, null);
            searchObjectsResponse response = modelPort.searchObjects(request);

            ObjectListType objectList = response.objectList;
            ObjectType1[] objects = objectList.@object;
            return objects;
        }

        private static SearchFilterType createRequestableFilter()
        {
            PropertyComplexValueFilterClauseType clause = new PropertyComplexValueFilterClauseType();
            clause.path = createItemPathType("requestable");
            clause.Item = true;

            SearchFilterType filter = new SearchFilterType();
            filter.Item = clause;
            filter.ItemElementName = ItemChoiceType1.equal;
            return filter;
        }

        private static void deleteUser(modelPortType modelPort, String oid)
        {
            ObjectDeltaType objectDelta = new ObjectDeltaType();
            objectDelta.objectType = USER_TYPE;
            objectDelta.changeType = ChangeTypeType.delete;
            objectDelta.oid = oid;

            executeChanges request = new executeChanges(new ObjectDeltaType[] { objectDelta }, null);
            executeChangesResponse response = modelPort.executeChanges(request);
            check(response);
        }
    
        private static modelPortType openConnection()
        {
            //WebRequest.DefaultWebProxy = new WebProxy("127.0.0.1", 8888);         // uncomment this line if you want to use fiddler

            modelPortTypeClient service = new modelPortTypeClient();

            service.ClientCredentials.UserName.UserName = LOGIN_USERNAME;
            service.ClientCredentials.UserName.Password = LOGIN_PASSWORD;

            service.Endpoint.Behaviors.Add(new InspectorBehavior(new ClientInspector(new SecurityHeader(LOGIN_USERNAME, LOGIN_PASSWORD))));
            try
            {
                return service.ChannelFactory.CreateChannel(new EndpointAddress(WS_URL));
            }
            catch (Exception e)
            {
                throw e;
            }
        }

    }

}

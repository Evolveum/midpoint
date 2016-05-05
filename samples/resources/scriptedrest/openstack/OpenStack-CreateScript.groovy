log.info("Entering "+action+" Script");

def keystoneTokenUrl = 'http://192.168.56.12:5000'
def keystoneUrl = 'http://192.168.56.12:35357'
def novaUrl = 'http://192.168.56.12:8774'
def adminId = '856d2846c4b24c1b923a224b1e9d007d'
def memberRoleId = '19d2a98348364d2a98c26a3160eef739'

def engine = new groovy.text.SimpleTemplateEngine();

def projectTemplate = '''
{
"tenant": 
	{
		"enabled": true, 
		"name": "$projectName", 
		"description": "Created by midPoint"
	}
}
'''

def vmTemplate = '''
{
"server": { 
	"name": "$serverName", 
	"imageRef":"$imageRef", 
	"flavorRef":"$flavorRef" 
	}
}
'''

def floatingTemplate = '''
{"addFloatingIp": {"address": "$address"}}
'''

def userTemplate = '''
{
    "user": {
        "email": "$email",
        "password": "$password",
        "enabled": true,
        "name": "$accountName"
    }
}

'''

def securityRuleTemplate = '''
{"security_group_rule": {"from_port": $fromPort, "ip_protocol": "$protocol", "to_port": $toPort, "parent_group_id": $parent_group_id, "cidr": "0.0.0.0/0", "group_id": null}}
'''

def login(def prName, def keystoneTokenUrl) {
	def loginBinding = [
        	projectName : prName
        ]
	
	def engine = new groovy.text.SimpleTemplateEngine();
	def loginTemplate = '''
		{
		"auth": {
        		"tenantName": "$projectName",
		        "passwordCredentials": {
                			"username": "admin",
	                		"password": "blekota"
                		}
        		}
		}
	'''


        request = engine.createTemplate(loginTemplate).make(loginBinding).toString()
        System.out.println("request: " + request);
        connection.setUri(keystoneTokenUrl)
	
        def resp = connection.post( path : "/v2.0/tokens", headers: ['Content-type': 'application/json'], body: request);
        
	System.out.println("response: " + resp.getData());
	System.out.println(resp.data.access.token.id);	

	return resp.data;
}

def randomNum() {
	Random random = new Random()
	return random.nextInt(10000)
}

def getServerInformation(def novaUrl, def token, def projectId, def serverId) {
        connection.setUri(novaUrl)
        requestPath = '/v2.1/' + token.access.token.tenant.id + '/servers/'+serverId;
        System.out.println("Path: "+ requestPath)

        def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);

        json = resp2.getData();
        System.out.println("Response: " + json)
	return json
}

def pollState(def novaUrl, def token, def projectId, def serverId) {
	json = getServerInformation(novaUrl, token, projectId, serverId)
	return json.server.status;
}

uid = ""
switch ( objectClass ) {
        case "Server":
                log.info("in the script")
                System.out.println("Attributes: "+attributes);

                
		projectId = (attributes.get("projectName") == null )? "project-name" : attributes.get("projectName").get(0)
		def token = login(projectId, keystoneTokenUrl)

                // invoke server start operation
                def vmBinding = [
                        serverName : (attributes.get("serverName") == null )? "random-name-"+randomNum() : attributes.get("serverName").get(0),
                        imageRef : (attributes.get("imageRef") == null)? "" : attributes.get("imageRef").get(0),
                        flavorRef : (attributes.get("flavorRef") == null)? "" : attributes.get("flavorRef").get(0),
                        //networkId : [attributes.get("networkId") == null] ? "" : attributes.get("networkId").get(0)
                ]

                connection.setUri(novaUrl)
                requestPath = '/v2.1/' + token.access.token.tenant.id + '/servers';
                System.out.println("Path: "+ requestPath)

                request = engine.createTemplate(vmTemplate).make(vmBinding).toString()
                System.out.println("request: " + request);
                def resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);

                System.out.println("response: " + resp2.getData());
                uid = resp2.data.server.id
                def projectId = resp2.data.server.tenant_id

                //give it some time
                sleep(5000);

                //poll server status and wait till it is started or it fails to start
                //def state = pollState(novaUrl, token, projectId, uid);
                //while (state=='BUILD') {
                //        state = pollState(novaUrl, token, projectId, uid);
                //        sleep(2000);
                //}

		//if (state != 'ACTIVE') {
	//		def serverJson = getServerInformation(novaUrl, token, projectId, uid)
			//TODO: improve error reporting
	//		throw new Exception("Failed to start "+attributes.get("serverName")+" Server/VM  with uid "+uid+" and final state "+state+". It failed with error: "+serverJson.server.fault?.message);
//		}
		
		//get floating IPs list and choose a free one
	        connection.setUri(novaUrl)
                requestPath = '/v2.1/' + token.access.token.tenant.id + '/os-floating-ips';
                println(requestPath)
		def resp3 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
		println(resp3.data);
		def freeIP = null
		for (def floatingIP in resp3.data.floating_ips) {
			if (floatingIP.instance_id == null) {
				freeIP = floatingIP.ip
				break;
			}
		}

		if (freeIP == null){
			throw new Exception("Failed to lookup free Floating IP");
		}
		
		//associate floating IP
                connection.setUri(novaUrl)
                requestPath = '/v2.1/' + token.access.token.tenant.id + '/servers/'+uid+'/action';
		println(requestPath)
                def floatingBinding = [
                        address : freeIP
                ]
		def request3 = engine.createTemplate(floatingTemplate).make(floatingBinding).toString()
		resp3 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request3);
		println(resp3.data);
		return attributes.get("projectName").get(0) + ":" +uid

//poll server status and wait till it is started or it fails to start
                def state = pollState(novaUrl, token, projectId, uid);
                while (state=='BUILD') {
                        state = pollState(novaUrl, token, projectId, uid);
                        sleep(2000);
                }

                if (state != 'ACTIVE') {
                        def serverJson = getServerInformation(novaUrl, token, projectId, uid)
                        //TODO: improve error reporting
                        throw new Exception("Failed to start "+attributes.get("serverName")+" Server/VM  with uid "+uid+" and final state "+state+". It failed with error: "+serverJson.server.fault?.message);
                }


                //return uid;
                break;

        case "Project":
               log.info("In the create script to create Project")
                System.out.println("Attributes: "+attributes);

                def token = login("admin", keystoneTokenUrl)

                def projectBinding = [
                        projectName : (attributes.get("projectName") == null )? "project-name" : attributes.get("projectName").get(0),
                ]

                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants';
                System.out.println("Path: "+ requestPath)

                request = engine.createTemplate(projectTemplate).make(projectBinding).toString()
		println(request)
                def resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);

                System.out.println("response: " + resp2.getData());
                uid = resp2.data.tenant.id;
                println("uid: " + uid);
	

		//associate admin user with our created project through member role
                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants/'+uid+'/users/'+adminId+'/roles/OS-KSADM/'+memberRoleId;
                System.out.println("Path: "+ requestPath)
                resp3 = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
                System.out.println("response: " + resp3.getData());

		 //relogin required
                token = login(resp2.data.tenant.name, keystoneTokenUrl)

		  //add firewall rules to project's default security group
                //get project's default group id
                connection.setUri(novaUrl)
                requestPath = '/v2.1/'+uid+'/os-security-groups';
                //we are expecting one security group for freshly created project under our control, the default one
                println("Path: "+ requestPath)
                resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
                System.out.println("response: " + resp2.getData());
                System.out.println("resp2.data.security_groups[0]=" + resp2.data.security_groups[0])
		 System.out.println("resp2.data.security_groups[0].id=" + resp2.data.security_groups[0].id)
		
		def defaultGroupId = resp2.data.security_groups[0].id
		System.out.println(defaultGroupId)
                //RESP BODY: {"security_groups": [{"rules": [], "tenant_id": "f45bc8514ca34109a4d9ddedf7141c1a", "id": 3, "name": "default", "description": "default"}]}


                connection.setUri(novaUrl)
                requestPath = '/v2.1/'+uid+'/os-security-group-rules';
                System.out.println("Path: "+ requestPath)
                def securityRuleBinding = [
                        toPort: "22",
			fromPort: "22",
                        protocol: "tcp",
                        parent_group_id: defaultGroupId,
                ]
                request = engine.createTemplate(securityRuleTemplate).make(securityRuleBinding).toString()
                println(request)
                resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);
                System.out.println("response: " + resp2.getData());

                securityRuleBinding = [
                        toPort: "389",
			fromPort: "389",
                        protocol: "tcp",
                        parent_group_id: defaultGroupId,
                ]
                request = engine.createTemplate(securityRuleTemplate).make(securityRuleBinding).toString()
                println(request)
                resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);
                System.out.println("response: " + resp2.getData());

                securityRuleBinding = [
                        protocol: "icmp",
			fromPort: "-1",
			toPort: "-1",
                        parent_group_id: defaultGroupId,
                ]
                request = engine.createTemplate(securityRuleTemplate).make(securityRuleBinding).toString()
                println(request)
                resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);
                System.out.println("response: " + resp2.getData());

		 //allocate Floating IP to project
                connection.setUri(novaUrl)
                requestPath = '/v2.1/'+uid+'/os-floating-ips';
                System.out.println("Path: "+ requestPath)
                println(request)
                resp2 = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: '{"pool": null}');
                System.out.println("response: " + resp2.getData());

                return uid;
	case "Account":
		log.info("In the create script to create User account")
		println("Attributes: "+attributes)
		
	        def token = login("admin", keystoneTokenUrl)

                def accountBinding = [
                        accountName : (attributes.get("accountName") == null )? "" : attributes.get("accountName").get(0),
                        password : password,
                        email : (attributes.get("email") == null )? "" : attributes.get("email").get(0)
                ]

                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/users';
                System.out.println("Users path: "+ requestPath)

                request = engine.createTemplate(userTemplate).make(accountBinding).toString()
                println(request)
                def resp = connection.post( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);
		println(resp)
                System.out.println("response: " + resp.getData());
                uid = resp.data.user.id;
                println("uid: " + uid);

		tenants = attributes.get("tenantId")
		println("tenants: " + tenants)
		if (tenants != null){
			for (tenant in tenants){
				connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants/'+tenant+'/users/'+uid+'/roles/OS-KSADM/'+memberRoleId;
                System.out.println("Path: "+ requestPath)
                resp3 = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
                System.out.println("response: " + resp3.getData());	
			}
		}
                return uid;
}

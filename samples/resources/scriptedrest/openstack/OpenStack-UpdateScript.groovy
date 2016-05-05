log.info("Entering "+action+" Script");

def keystoneTokenUrl = 'http://192.168.56.12:5000'
def keystoneUrl = 'http://192.168.56.12:35357'
def novaUrl = 'http://192.168.56.12:8774'
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

def accountTemplate = '''
{
    "user": {
        "email": "$email",
        "enabled": true,
        "name": "$accountName"
    }
}

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

//uid = ""
switch ( objectClass ) {
        case "Server":
                log.info("in the script")
//                System.out.println("Attributes: "+attributes);

 //               def token = login("admin", keystoneTokenUrl)

                // invoke server start operation
   //             def vmBinding = [
     //                   serverName : (attributes.get("serverName") == null )? "random-name-"+randomNum() : attributes.get("serverName").get(0),
//			imageRef : attributes.get("imageRef")?.get(0),
//			flavorRef : attributes.get("flavorRef")?.get(0)
  //              ]

    //            connection.setUri(novaUrl)
      //          requestPath = '/v2.1/' + token.access.token.tenant.id + '/servers/'+uid;
      //          System.out.println("Path: "+ requestPath)

        //        request = engine.createTemplate(vmTemplate).make(vmBinding).toString()
         //       System.out.println("request: " + request);
        //        def resp2 = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);

          //      System.out.println("response: " + resp2.getData());
        //        uid = resp2.data.server.id
         //       def projectId = resp2.data.server.tenant_id

	//	return attributes.get("projectName").get(0) + ":" +uid
                return uid;
                break;

        case "Project":
               log.info("In the create script to create Project")
                System.out.println("Attributes: "+attributes);

                def token = login("admin", keystoneTokenUrl)

                def projectBinding = [
                        projectName : (attributes.get("projectName") == null )? "project-name" : attributes.get("projectName").get(0),
                ]

                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants/'+attributes.get("projectId");
                System.out.println("Path: "+ requestPath)

                request = engine.createTemplate(projectTemplate).make(projectBinding).toString()
		println(request)
                def resp2 = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);

                System.out.println("response: " + resp2.getData());
                uid = resp2.data.tenant.id;
                println("uid: " + uid);
                return uid;

        case "Account":
                log.info("In the create script to create User account")
                println("Attributes: "+attributes)

                def token = login("admin", keystoneTokenUrl)

		
		switch(action){
		
			case "ADD_ATTRIBUTE_VALUES" : 
			println("In add attribute values for account, attributes: " + attributes)			
	if (attributes.get("tenantId") != null){
		for (tenant in attributes.get("tenantId")){
					//associate admin user with our created project through member role
                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants/'+tenant+'/users/'+uid+'/roles/OS-KSADM/'+memberRoleId;
                System.out.println("Path: "+ requestPath)
                resp3 = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
                System.out.println("response: " + resp3.getData());
}
					break;
				}
			case "REMOVE_ATTRIBUTE_VALUES" :
				println("In delete attribute values for account, attributes: " + attributes)
				if (attributes.get("tenantId") != null){
                for (tenant in attributes.get("tenantId")){
                                        //associate admin user with our created project through member role
                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/tenants/'+tenant+'/users/'+uid+'/roles/OS-KSADM/'+memberRoleId;
                System.out.println("Path: "+ requestPath)
                resp3 = connection.delete( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
                System.out.println("response: " + resp3.getData());
                                        break;
                                }
		}
			default:
		println("Before email")
		def email = attributes.get("email")?.get(0);
		def usern = attributes.get("accountName")?.get(0)
		println("email: " + attributes.get("email")?.get(0));
				 //TODO: adapt to relative changes

		if (email!=null && usern!=null){ 
                def accountBinding = [
                        accountName : (attributes.get("accountName") == null )? "" : attributes.get("accountName").get(0),
                        email : (attributes.get("email") == null )? "" : attributes.get("email")?.get(0),
                ]

                connection.setUri(keystoneUrl)
                requestPath = '/v2.0/users/'+uid;
                System.out.println("Path: "+ requestPath)

                request = engine.createTemplate(accountTemplate).make(accountBinding).toString()
                println(request)
                def resp = connection.put( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: request);

                System.out.println("response: " + resp.getData());
                uid = resp.data.user.id;
		}
                println("uid: " + uid);

		}
                return uid;
}

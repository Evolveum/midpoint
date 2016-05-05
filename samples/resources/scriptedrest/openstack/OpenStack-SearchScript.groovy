
log.info("Entering "+action+" Script");

def engine = new groovy.text.SimpleTemplateEngine();

def keystoneTokenUrl = 'http://192.168.56.12:5000'
def keystoneUrl = 'http://192.168.56.12:35357'
def novaUrl = 'http://192.168.56.12:8774'

def login(def prName, def keystoneTokenUrl) {
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


	def loginBinding = [
        	projectName : prName
        ]
	
	def engine = new groovy.text.SimpleTemplateEngine();
        request = engine.createTemplate(loginTemplate).make(loginBinding).toString()
        System.out.println("request: " + request);
        connection.setUri(keystoneTokenUrl)
	
        def resp = connection.post( path : "/v2.0/tokens", headers: ['Content-type': 'application/json'], body: engine.createTemplate(loginTemplate).make(loginBinding).toString());
        
	System.out.println("response: " + resp.getData());
	System.out.println(resp.data.access.token.id);	

	return resp.data;
}

def getServerInformation(def novaUrl, token, def serverId) {
        System.out.println("getServerInformation start")
	connection.setUri(novaUrl)
	System.out.println("after connection")

        requestPath = '/v2.1/' + token.access.token.tenant.id + '/servers/'+serverId;
        System.out.println("Path: "+ requestPath)

        def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);

        json = resp2.getData();
        System.out.println("Response: " + json)
	return json
}

def getTenant(def keystoneUrl, def token, def projectId) {
        connection.setUri(keystoneUrl)
        requestPath = '/v2.0/tenants/' + projectId;
        System.out.println("Path: "+ requestPath)

        def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);

        json = resp2.getData();
        System.out.println("Response: " + json)
        return json
}

def getUser(def keystoneUrl, def token, def userId) {
        connection.setUri(keystoneUrl)
        requestPath = '/v2.0/users/' + userId;
        System.out.println("Users Get Path: "+ requestPath)

        def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);

        json = resp2.getData();
        System.out.println("Response: " + json)
        return json
}


def result = []

if (query != null){

switch ( objectClass ) {
        case "Server":
                if (query.get("left").equalsIgnoreCase('__UID__')) {

                        projectServerId = query.get('right')
                        serverId = projectServerId.tokenize(':')[1]
                        projectName = projectServerId.tokenize(':')[0]

                        System.out.println("projectName : " + projectServerId.tokenize(':')[0])
			System.out.println("before login")
                        def token = login(projectName, keystoneTokenUrl)
			System.out.println("Login successful");
                        def json = getServerInformation(novaUrl, token, serverId)
			
			System.out.println("JSON: " + json)
			System.out.println("IP Address: " + json.server.addresses.private[1].addr);
                        result.add([__UID__ : projectServerId.tokenize(':')[0] + ':' +json.server.id, __NAME__ : json.server.name, ipAddress : json.server.addresses.private[1].addr]);
                } 
		break;
	case "Project":
		def uid = query.get('right')
		System.out.println("uid: "+ uid);
		//login for admin tenant scope
		def token = login("admin", keystoneTokenUrl)
		try {
			def json = getTenant(keystoneUrl, token, uid)
		} catch(groovyx.net.http.HttpResponseException ex) {
			if ("Not Found".equals(ex.getMessage())) {
				throw new Exception("Project does not exist");
			}
			throw ex;
		}
		result.add([__UID__: json.tenant.id, __NAME__: json.tenant.name, projectId: json.tenant.id]);		
		break;


        case "Account":
                def uid = query.get('right')
                System.out.println("uid: "+ uid);
                //login for admin tenant scope
                def token = login("admin", keystoneTokenUrl)
                def json = getUser(keystoneUrl, token, uid)

		connection.setUri(keystoneUrl)
		def requestPath = "/v3/users/"+uid+"/projects"		

		def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);
		println(resp2.getData())
		
                result.add([__UID__: json.user.id, __NAME__: json.user.name, tenantId: resp2.data.projects.id, email:json.user.email, enabled: json.user.enabled, username: json.user.username, password: json.user.password]);
                break;
	}
    }
return result;

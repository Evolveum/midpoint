
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


def getObject(def url, def token, def requestPath){
	connection.setUri(url)
	System.out.println("Get Object Path: "+ requestPath)

	def resp2 = connection.get( path : requestPath, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id], body: null);

	json = resp2.getData();
	System.out.println("Response: " + json)
	return json
}

def getProjects(def uid, def keystoneTokenUrl, def keystoneUrl) {
			//login for admin tenant scope
			def foundProjects = [];
			def token = login("admin", keystoneTokenUrl)
			try {
						requestPath = '/v2.0/tenants/'
						if (uid != null) {
							 requestPath = requestPath + uid
						}
						def projects = getObject(keystoneUrl, token, requestPath)
						if (uid != null) {
							foundProjects.add([__UID__: projects.tenant.id, __NAME__: projects.tenant.name, projectId: projects.tenant.id]);
						} else {
								for (project in projects.tenants) {
										foundProjects.add([__UID__: project.id, __NAME__: project.name, projectId: project.id]);
								}
						}
			} catch(groovyx.net.http.HttpResponseException ex) {
						if ("Not Found".equals(ex.getMessage())) {
								throw new Exception("Project does not exist");
						}
						throw ex;
			}
			return foundProjects;
}

def getAccounts(def uid, def keystoneTokenUrl, def keystoneUrl) {
				//login for admin tenant scope
				def accounts = []
				def token = login("admin", keystoneTokenUrl)
				if (uid != null) {
							def json = getObject(keystoneUrl, token, '/v2.0/users/' + uid)
							connection.setUri(keystoneUrl)
							def resp2 = getObject(keystoneUrl, token, "/v3/users/"+uid+"/projects")
							accounts.add([__UID__: json.user.id, __NAME__: json.user.name, tenantId: resp2.data.projects.id, email:json.user.email, enabled: json.user.enabled, username: json.user.username, password: json.user.password]);
				} else {
							def users = getObject(keystoneUrl, token, '/v2.0/users/')
							for (json in users.users) {
									connection.setUri(keystoneUrl)
									def projects = getObject(keystoneUrl, token, "/v3/users/"+json.id+"/projects")
									accounts.add([__UID__: json.id, __NAME__: json.name, tenantId: projects.projects.id, email:json.email, enabled: json.enabled, username: json.username, password: json.password]);
							}
				}
				return accounts;
}

def getServerInformation(def uid, def keystoneTokenUrl, def novaUrl) {

				serverId = uid.tokenize(':')[1]
				projectName = uid.tokenize(':')[0]
				System.out.println("projectName : " + projectName)
				System.out.println("before login")
				def token = login(projectName, keystoneTokenUrl)
				System.out.println("Login successful");
				def json = getObject(novaUrl, token, '/v2.1/' + token.access.token.tenant.id + '/servers/'+serverId)
				System.out.println("JSON: " + json)
				System.out.println("IP Address: " + json.server.addresses.private[1].addr);
				def server
				server.add([__UID__ : uid.tokenize(':')[0] + ':' +json.server.id, __NAME__ : json.server.name, ipAddress : json.server.addresses.private[1].addr]);
				return server
}

def result = []
def uid
if (query != null) {
		uid = query.get('right')
		System.out.println("uid: "+ uid);
	}
		switch ( objectClass ) {
        case "Server":
								if (uid != null) {
                		result = getServerInformation(uid, keystoneTokenUrl, novaUrl)
								}
								break;
				case "Project":
								result = getProjects(uid, keystoneTokenUrl, keystoneUrl)
								break;
        case "Account":
								result = getAccounts(uid, keystoneTokenUrl, keystoneUrl)
                break;
			}

return result;

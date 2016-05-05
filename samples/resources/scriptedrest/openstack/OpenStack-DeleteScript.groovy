log.info("Entering "+action+" Script");

engine = new groovy.text.SimpleTemplateEngine();

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



switch ( objectClass ) {
        case "Server":
                        serverId = uid.tokenize(':')[1]
                        System.out.println("projectName : " + uid.tokenize(':')[0])
			def token = login(uid.tokenize(':')[0], keystoneTokenUrl)

                        connection.setUri(novaUrl)
                        response = connection.delete( path : '/v2/'+ token.access.token.tenant.id + '/servers/' + serverId, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id]);

                        json = response.getData();
                        System.out.println("Response: " + json)
                       
			return serverId
	case "Project":
		def token = login("admin", keystoneTokenUrl);
		
		connection.setUri(keystoneUrl)
		response = connection.delete( path : '/v2.0/tenants/'+uid, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id]);

		json = response.getData();
		println("Response: "+ json);
		
		return uid
        case "Account":
                def token = login("admin", keystoneTokenUrl);

                connection.setUri(keystoneUrl)
                response = connection.delete( path : '/v2.0/users/'+uid, headers: ['Content-type': 'application/json', 'X-Auth-Token': token.access.token.id]);

                json = response.getData();
                println("Response: "+ json);

                return uid

}

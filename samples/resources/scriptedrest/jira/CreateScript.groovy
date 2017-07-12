import org.identityconnectors.framework.common.exceptions.AlreadyExistsException

/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Parameters:
// The connector sends us the following:
// connection : handler to the REST Client
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// id: The entry identifier (OpenICF "Name" atribute. (most often matches the uid) - IF action = CREATE
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map
//
// Returns: Create must return UID.

log.info("Entering "+action+" Script, attributes: "+attributes);

// detect if user already exists
resp = connection.get(path: "https://jira.evolveum.com/rest/api/latest/user",
        query: ['username' : id]);
json = resp.getData();
log.ok("JSON create search response:\n" + json);

//userName = json."name";
if (json && json."name" && id.equals(json."name")) {
    throw new AlreadyExistsException("User "+id+" already exists");
}


throw new UnsupportedOperationException("not supported operation, only update/delete avatar is implemented");

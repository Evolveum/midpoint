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

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException
import org.identityconnectors.framework.common.objects.Uid

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

def DEFAULT_ENDPOINT = "https://api.thingspeak.com/channels"

log.info("Entering " + action + " script, attributes: " + attributes)

def endPoint = configuration.endPoint != null ? configuration.endPoint : DEFAULT_ENDPOINT
def api_key = null
configuration.password.access(new org.identityconnectors.common.security.GuardedString.Accessor() {
    def void access(char[] clearChars) {
        api_key = new String(clearChars)
    }
})

// TODO check if channel does already exist

resp = connection.post(path: endPoint, query: [
        'api_key': api_key,
        'name': id,
        'description': attributes.description,
        'field1': attributes.field1,
        'field2': attributes.field2,
        'field3': attributes.field3,
        'field4': attributes.field4,
        'field5': attributes.field5,
        'field6': attributes.field6,
        'field7': attributes.field7,
        'field8': attributes.field8
])
json = resp.getData();
log.info("JSON create response:\n" + json);
uid = String.valueOf(json.id) //new Uid(String.valueOf(json.id))
log.info("UID: $uid")
return uid

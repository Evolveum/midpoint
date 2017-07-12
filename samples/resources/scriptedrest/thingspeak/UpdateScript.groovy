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
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// uid: a String representing the entry uid
// attributes: an Attribute Map, containing the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text (only for UPDATE)
// options: a handler to the OperationOptions Map

log.info("Entering $action script, uid: $uid, attributes: $attributes")

if (objectClass != "channel") {
    return uid
}

def DEFAULT_ENDPOINT = "https://api.thingspeak.com/channels"

def endPoint = configuration.endPoint != null ? configuration.endPoint : DEFAULT_ENDPOINT
def api_key = null

configuration.password.access(new org.identityconnectors.common.security.GuardedString.Accessor() {
    def void access(char[] clearChars) {
        api_key = new String(clearChars)
    }
})

query = ['api_key': api_key]
for (Map.Entry entry : attributes) {
    def paramName = entry.key == '__NAME__' ? 'name' : entry.key;
    query.put(paramName, entry.value)           // TODO deal with multiple values?
}

log.info("Query parameters: $query")

resp = connection.put(path: endPoint + "/" + uid, query: query)
log.info("Response: $resp")

return uid

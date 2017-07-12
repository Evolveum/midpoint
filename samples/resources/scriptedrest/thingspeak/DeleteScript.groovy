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
// The connector sends the following:
// connection: handler to the REST Client 
// (see: http://groovy.codehaus.org/modules/http-builder/apidocs/groovyx/net/http/RESTClient.html)
// configuration : handler to the connector's configuration object
// action: a string describing the action ("DELETE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// options: a handler to the OperationOptions Map
// uid: String for the unique id that specifies the object to delete

def DEFAULT_ENDPOINT = "https://api.thingspeak.com/channels"

log.info("Entering $action script, uid: $uid")

def endPoint = configuration.endPoint != null ? configuration.endPoint : DEFAULT_ENDPOINT
def api_key = null
configuration.password.access(new org.identityconnectors.common.security.GuardedString.Accessor() {
    def void access(char[] clearChars) {
        api_key = new String(clearChars)
    }
})

resp = connection.delete(path: endPoint + '/' + uid , query: [ 'api_key': api_key ])
json = resp.getData();
log.info("DELETE response:\n $json")

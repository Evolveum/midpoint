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
// objectClass: a String describing the Object class ("channel" in this case)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used.
//
// query = [ operation: "CONTAINS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "ENDSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "STARTSWITH", left: attribute, right: "value", not: true/false ]
// query = [ operation: "EQUALS", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "GREATERTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHAN", left: attribute, right: "value", not: true/false ]
// query = [ operation: "LESSTHANOREQUAL", left: attribute, right: "value", not: true/false ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

def DEFAULT_ENDPOINT = "https://api.thingspeak.com/channels"

log.info("Entering " + action + " script, query: " + query)

def endPoint = configuration.endPoint != null ? configuration.endPoint : DEFAULT_ENDPOINT
def api_key = null

configuration.password.access(new org.identityconnectors.common.security.GuardedString.Accessor() {
    def void access(char[] clearChars) {
        api_key = new String(clearChars)
    }
})

def result = []

if (objectClass != "channel") {
    return result
}

response = connection.get(path: endPoint, query: ['api_key': api_key])
data = response.getData()
log.info("JSON response size {0}, data:\n{1}", data?.size, data)

data?.each {
    res = parseChannel(it)
    result.add(res)
}

log.info("result:\n{0}", result)
return result

def parseChannel(json) {
    //log.info("Parsing channel: " + json)
    id = json.id
    readKey = null
    writeKey = null
    for (api_key_entry in json.api_keys) {
        if (api_key_entry.write_flag) {
            writeKey = api_key_entry.api_key
        } else {
            readKey = api_key_entry.api_key
        }
    }

    return [__UID__         : json.id,
            __NAME__        : json.name,
            description     : json.description,
            readKey         : readKey,
            writeKey        : writeKey
    ];
}

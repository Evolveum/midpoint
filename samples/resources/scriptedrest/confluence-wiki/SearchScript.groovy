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
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
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

log.info("Entering "+action+" Script, query: "+query);

def result = [];

switch ( objectClass ) {
case "__ACCOUNT__":

    if (query!=null && ("__UID__".equalsIgnoreCase(query.get("left")) || "__NAME__".equalsIgnoreCase(query.get("left")))
            && ("CONTAINS".equalsIgnoreCase(query.get("operation")) || ("EQUALS".equalsIgnoreCase(query.get("operation")))) ) {
        log.ok("Exact query on: " + query.get("right"))
        def body = '[ "'+query.get("right")+'" ]';
        resp = connection.post(path: "https://wiki.evolveum.com/rpc/json-rpc/confluenceservice-v2/getUser",
                headers: ['Accept': '*/*', 'Content-Type': 'application/json'],
                body: body)
        json = resp.getData();
        log.ok("JSON response:\n" + json);

        if (json.name) { // exists
            // need to replace configured endpoint to other endpoint
            restEndpoint = "https://wiki.evolveum.com/rest/prototype/1";
            path = restEndpoint+"/search/user";
            connection.setUri(restEndpoint)
            log.ok("JSON GET detail url: {0}", path);
            respDetail = connection.get(path: path,
                    headers: ['Content-Type': 'application/json'],
                    query: ['query': json.name])
            jsonDetail = respDetail.getData();
            log.ok("JSON response detail:\n {0}", jsonDetail);
            thumbnail = "";
            if (jsonDetail && jsonDetail.result && jsonDetail.result[0].thumbnailLink && jsonDetail.result[0].thumbnailLink.href) {
                thumbnail = jsonDetail.result[0].thumbnailLink.href;
            }

            byte[] avatar = null; // send only not default profile pictures
            if (thumbnail != null && !thumbnail.contains("/profilepics/")) {
                thumbnailPrefix = thumbnail.split("\\?");
                connection.setUri(thumbnailPrefix[0]);
                log.ok("JSON GET profile picture url: {0}", thumbnailPrefix[0]);
                respImage = connection.get(path: thumbnailPrefix[0],
                        headers: ['Content-Type': 'image/png'],
                        contentType: 'application/octet-stream',
                    )
                ByteArrayInputStream bais = respImage.getData();
                avatar = new byte[bais.available()];
                bais.read(avatar);
            }

            user = [__UID__ : json.name,
                    __NAME__: json.name,
                    thumbnailLink : thumbnail,
                    avatar : avatar
            ];

            result.add(user);
        }
    }
    else {
        log.ok("Find all active users");

        def viewAll = '[ "false" ]';
        resp = connection.post(path: "https://wiki.evolveum.com/rpc/json-rpc/confluenceservice-v2/getActiveUsers",
                headers: ['Accept': '*/*', 'Content-Type': 'application/json'],
                body: viewAll)

        data = resp.getData();
        log.ok("JSON response:\n {0}", data);
        data.each {
            log.ok("JSON LINE:\n {0}", it);

            user = [__UID__ : it,
                    __NAME__: it
            ];

            result.add(user);
        }
    }
    break

}

log.ok("result: \n" + result);
return result;

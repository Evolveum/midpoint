/*
 * Copyright (c) 2010-2013 Evolveum
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
import groovy.sql.Sql;
import groovy.sql.DataSet;

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
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

log.info("Entering "+action+" Script");

def sql = new Sql(connection);
def result = []
def where = "";

if (query != null){
    //Need to handle the __UID__ in queries
    if (query.get("left").equalsIgnoreCase("__UID__") && objectClass.equalsIgnoreCase("__ACCOUNT__")) query.put("left","id");
    if (query.get("left").equalsIgnoreCase("__UID__") && objectClass.equalsIgnoreCase("Group")) query.put("left","id");
    if (query.get("left").equalsIgnoreCase("__UID__") && objectClass.equalsIgnoreCase("Organization")) query.put("left","id")

    // We can use Groovy template engine to generate our custom SQL queries
    def engine = new groovy.text.SimpleTemplateEngine();

    def whereTemplates = [
        CONTAINS:' WHERE $left ${not ? "NOT " : ""}LIKE "%$right%"',
        ENDSWITH:' WHERE $left ${not ? "NOT " : ""}LIKE "%$right"',
        STARTSWITH:' WHERE $left ${not ? "NOT " : ""}LIKE "$right%"',
        EQUALS:' WHERE $left ${not ? "<>" : "="} \'$right\'',
        GREATERTHAN:' WHERE $left ${not ? "<=" : ">"} "$right"',
        GREATERTHANOREQUAL:' WHERE $left ${not ? "<" : ">="} "$right"',
        LESSTHAN:' WHERE $left ${not ? ">=" : "<"} "$right"',
        LESSTHANOREQUAL:' WHERE $left ${not ? ">" : "<="} "$right"'
    ]

    def wt = whereTemplates.get(query.get("operation"));
    def binding = [left:query.get("left"),right:query.get("right"),not:query.get("not")];
    def template = engine.createTemplate(wt).make(binding);
    where = template.toString();
    log.ok("Search WHERE clause is: "+ where)
}

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.eachRow("SELECT * FROM Users" + where, {result.add([__UID__:it.id, __NAME__:it.login, __ENABLE__:!it.disabled, fullname:it.fullname, firstname:it.firstname, lastname:it.lastname, email:it.email, organization:it.organization])} );
    break

    case "Group":
    sql.eachRow("SELECT * FROM Groups" + where, {result.add([__UID__:it.id, __NAME__:it.name, ,description:it.description])} );
    break

    case "Organization":
    sql.eachRow("SELECT * FROM Organizations" + where, {result.add([__UID__:it.id, __NAME__:it.name, description:it.description])} );
    break

    default:
    result;
}

return result;

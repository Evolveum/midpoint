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
// The connector sends us the following:
// connection : SQL connection
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// id: The entry identifier (OpenICF "Name" atribute. (most often matches the uid)
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");

def sql = new Sql(connection);
//Create must return UID. Let's return the name for now.

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.execute("INSERT INTO Users (uid, firstname,lastname,fullname,email,organization) values (?,?,?,?,?,?)",
        [
            id,
            attributes.get("firstname") ? attributes.get("firstname").get(0) : "",
            attributes.get("lastname")  ? attributes.get("lastname").get(0) : "",
            attributes.get("fullname")  ? attributes.get("fullname").get(0) : "",
            attributes.get("email")     ? attributes.get("email").get(0) : "",
            attributes.get("organization") ? attributes.get("organization").get(0) : ""
        ])
	sql.commit();
    break

    case "__GROUP__":
    sql.execute("INSERT INTO Groups (gid,name,description) values (?,?,?)",
        [
            attributes.get("gid") ? attributes.get("gid").get(0) : "",
            id,
            attributes.get("description") ? attributes.get("description").get(0) : ""
        ])
	sql.commit();
    break

    case "organization":
    sql.execute("INSERT INTO Organizations (name,description) values (?,?)",
        [
            id,
            attributes.get("description") ? attributes.get("description").get(0) : ""
        ])
	sql.commit();
    break

    default:
    id;
}

return id;

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
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");
def sql = new Sql(connection);
def doCommit = false;

switch ( action ) {
    case "UPDATE":
    switch ( objectClass ) {
        case "__ACCOUNT__":
        if (attributes.get("fullname")) {
		sql.executeUpdate("UPDATE Users set fullname = ? where uid = ?", [attributes.get("fullname").get(0), uid]);
		doCommit = true;
	}
        if (attributes.get("firstname")) {
	        sql.executeUpdate("UPDATE Users set firstname = ? where uid = ?", [attributes.get("firstname").get(0), uid]);
		doCommit = true;
	}
        if (attributes.get("lastname")) {
        	sql.executeUpdate("UPDATE Users set lastname = ? where uid = ?", [attributes.get("lastname").get(0), uid]);
		doCommit = true;
	}
        if (attributes.get("email")) {
	        sql.executeUpdate("UPDATE Users set email = ? where uid = ?", [attributes.get("email").get(0), uid]);
		doCommit = true;
	}
        if (attributes.get("organization")) {
        	sql.executeUpdate("UPDATE Users set organization = ? where uid = ?", [attributes.get("organization").get(0), uid]);
		doCommit = true;
	}
        if (doCommit) {
		sql.commit();
	}
        break

        case "__GROUP__":
        sql.executeUpdate("UPDATE Groups set description = ? where name = ?", [attributes.get("description").get(0), uid]);
        sql.executeUpdate("UPDATE Groups set gid = ? where name = ?", [attributes.get("gid").get(0), uid]);
        sql.commit();
        break

        case "organization":
        sql.executeUpdate("UPDATE Organizations set description = ? where name = ?", [attributes.get("description").get(0), uid]);
        sql.commit();
        break

        default:
        uid;
    }
    break

    case "ADD_ATTRIBUTE_VALUES":
    break

    case "ADD_ATTRIBUTE_VALUES":
    break


    default:
    uid
}

return uid;

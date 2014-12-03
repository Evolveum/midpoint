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
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.SecurityUtil;

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
// password: GuardedString type
//
// options: a handler to the OperationOptions Map

log.info("Entering "+action+" Script");
def sql = new Sql(connection);
def doCommit = false;
def preparedStatementPrefixAccounts = "UPDATE Users SET ";
def preparedStatementPrefixGroups = "UPDATE Groups SET ";
def preparedStatementPrefixOrganizations = "UPDATE Organizations SET ";
def preparedStatementAttributes = "";
def preparedStatementAttributesList = [];
def preparedStatementColumns = [];
def accountAttrNames = ["__NAME__", "firstname", "lastname", "fullname", "email", "organization", "__PASSWORD__", "__ENABLE__" ];
def groupAttrNames = ["__NAME__", "description" ];
def orgAttrNames = ["__NAME__", "description" ];

switch ( action ) {
    case "UPDATE":
    switch ( objectClass ) {
        case "__ACCOUNT__":
// All our attributes are single valued, but can be null on update (except for
// __NAME__).
// http://stackoverflow.com/questions/4839834/get-the-first-element-of-a-list-idiomatically-in-groovy


        for (attr in accountAttrNames) {
            if (attributes.get(attr) != null) {
//log.info("XXXX Processing attribute: " + attr + "=" + attributes.get(attr));
                switch (attr) {
                    case "__NAME__":
                        // __NAME__ corresponds to "login" column
                        preparedStatementAttributesList.add("login" + " = ?");
                        preparedStatementColumns.add(attributes.get(attr)?.find { true });
                        break;
                    case "__ENABLE__":
                        // __ENABLE__ corresponds to (negated) "disabled" column
                        preparedStatementAttributesList.add("disabled" + " = ?");
                        // negate the attribute value
                        preparedStatementColumns.add( !(attributes.get(attr)?.find { true }) as Boolean);
                        break;
                    case "__PASSWORD__":
                        // __PASSWORD__ corresponds to (clear text) "password" column
                        preparedStatementAttributesList.add("password" + " = ?");
                        // decrypt password
                        preparedStatementColumns.add(SecurityUtil.decrypt(attributes.get(attr)?.find { true } as GuardedString));
                        break;
                    default:
                        // all other attributes
                        preparedStatementAttributesList.add(attr + " = ?");
                        preparedStatementColumns.add(attributes.get(attr)?.find { true });
                }
            }
        }
        preparedStatementAttributes = preparedStatementAttributesList.join(',');

	if (preparedStatementAttributes != "") {
	    preparedStatementColumns.add(uid as Integer);
	//    log.info("XXXXXXXXXXXX" + preparedStatementPrefixAccounts + preparedStatementAttributes + " WHERE id = ?", preparedStatementColumns);
	    sql.executeUpdate(preparedStatementPrefixAccounts + preparedStatementAttributes + " WHERE id = ?", preparedStatementColumns);
	    doCommit = true;
	}

        //if (doCommit) {
	//	sql.commit();
	//}
        break

        case "Group":

        for (attr in groupAttrNames) {
            if (attributes.get(attr) != null) {
                preparedStatementAttributesList.add((attr == '__NAME__' ? "name" : attr) + " = ?");
                preparedStatementColumns.add(attributes.get(attr)?.find { true });
            }
        }
        preparedStatementAttributes = preparedStatementAttributesList.join(',');

        if (preparedStatementAttributes != "") {
            preparedStatementColumns.add(uid as Integer);
            sql.executeUpdate(preparedStatementPrefixGroups + preparedStatementAttributes + " WHERE id = ?", preparedStatementColumns);
            doCommit = true;
        }
        //sql.commit();
        break

        case "Organization":

        for (attr in orgAttrNames) {
            if (attributes.get(attr) != null) {
                preparedStatementAttributesList.add((attr == '__NAME__' ? "name" : attr) + " = ?");
                preparedStatementColumns.add(attributes.get(attr)?.find { true });
            }
        }
        preparedStatementAttributes = preparedStatementAttributesList.join(',');

        if (preparedStatementAttributes != "") {
            preparedStatementColumns.add(uid as Integer);
            sql.executeUpdate(preparedStatementPrefixOrganizations + preparedStatementAttributes + " WHERE id = ?", preparedStatementColumns);
            doCommit = true;
        }
        //sql.commit();
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

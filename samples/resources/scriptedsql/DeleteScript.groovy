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
import org.identityconnectors.framework.common.exceptions.UnknownUidException

// Parameters:
// The connector sends the following:
// connection: handler to the SQL connection
// action: a string describing the action ("DELETE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// options: a handler to the OperationOptions Map
// uid: String for the unique id that specifies the object to delete

log.info("Entering "+action+" Script");
def sql = new Sql(connection);

assert uid != null

switch ( objectClass ) {
    case "__ACCOUNT__":
    sql.execute("DELETE FROM Users where id= ?",[uid as Integer])
    count = sql.updateCount;
    //sql.commit();
    break

    case "Groups":
    sql.execute("DELETE FROM Groups where id= ?",[uid as Integer])
    count = sql.updateCount;
    //sql.commit();
    break

    case "Organization":
    sql.execute("DELETE FROM Organizations where id= ?",[uid as Integer])
    count = sql.updateCount;
    //sql.commit();
    break

    default:
    uid;
}

if (count != 1) {
    throw new UnknownUidException("Couldn't found and delete object $objectClass with uid $uid")
}


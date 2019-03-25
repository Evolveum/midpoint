import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.objects.Attribute
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions

import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def attributes = attributes as Set<Attribute>
def connection = connection as Connection
def id = id as String
def configuration = configuration as ScriptedSQLConfiguration

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

//return new Uid(_some_unique_identifier)
return null

/*
// todo fix sample
switch (objectClass) {
    case ObjectClass.ACCOUNT:
        return handleAccount(sql)
    case BaseScript.GROUP:
        return handleGroup(sql)
    case BaseScript.ORGANIZATION:
        return handleOrganization(sql)
    default:
        throw new ConnectorException("Unknown object class " + objectClass)
}

Uid handleAccount(Sql sql) {
    def keys = sql.executeInsert("INSERT INTO Users (login, firstname,lastname,fullname,email,organization,password,disabled) values (?,?,?,?,?,?,?,?)",
            [
                    id,
                    AttributeUtil.getSingleValue("firstname"),
                    AttributeUtil.getSingleValue("lastname"),
                    AttributeUtil.getSingleValue("fullname"),
                    AttributeUtil.getSingleValue("email"),
                    AttributeUtil.getSingleValue("organization"),
                    // decrypt password
                    SecurityUtil.decrypt(AttributeUtil.getPasswordValue(attributes)),
                    // negate __ENABLE__ attribute
                    !(AttributeUtil.getSingleValue("__ENABLE__") as Boolean)

                    //attributes.get("firstname") ? attributes.get("firstname").get(0) : "",
                    //attributes.get("lastname")  ? attributes.get("lastname").get(0) : "",
                    //attributes.get("fullname")  ? attributes.get("fullname").get(0) : "",
                    //attributes.get("email")     ? attributes.get("email").get(0) : "",
                    //attributes.get("organization") ? attributes.get("organization").get(0) : ""
            ])

    return new Uid(keys[0][0])
}

Uid handleGroup(Sql sql) {
    def keys = sql.executeInsert("INSERT INTO Groups (name,description) values (?,?)",
            [
                    id,
                    AttributeUtil.getSingleValue("description")
            ])

    return new Uid(keys[0][0])
}

Uid handleOrganization(Sql sql) {
    def keys = sql.executeInsert("INSERT INTO Organizations (name,description) values (?,?)",
            [
                    id,
                    AttributeUtil.getSingleValue("description")
            ])

    return new Uid(keys[0][0])
}
*/
import groovy.sql.Sql
import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.ICFObjectBuilder
import org.forgerock.openicf.misc.scriptedcommon.MapFilterVisitor
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log
import org.identityconnectors.framework.common.exceptions.ConnectorException
import org.identityconnectors.framework.common.objects.ObjectClass
import org.identityconnectors.framework.common.objects.OperationOptions
import org.identityconnectors.framework.common.objects.ResultsHandler
import org.identityconnectors.framework.common.objects.SearchResult
import org.identityconnectors.framework.common.objects.filter.Filter

import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def options = options as OperationOptions
def objectClass = objectClass as ObjectClass
def configuration = configuration as ScriptedSQLConfiguration
def filter = filter as Filter
def connection = connection as Connection
def query = query as Closure
def handler = handler as ResultsHandler

log.info("Entering " + operation + " Script")

def sql = new Sql(connection)

switch (objectClass) {
    case ObjectClass.ACCOUNT:
        handleAccount(sql)
        break
    case BaseScript.GROUP:
        handleGroup(sql)
        break
    case BaseScript.ORGANIZATION:
        handleOrganization(sql)
        break
    default:
        throw new ConnectorException("Unknown object class " + objectClass)
}

return new SearchResult()

// =================================================================================

void handleAccount(Sql sql) {
    Closure closure = { row ->
        ICFObjectBuilder.co {
            uid row.id as String
            id row.login
            attribute '__ENABLE__', !row.disabled
            attribute 'fullname', row.fullname
            attribute 'firstname', row.firstname
            attribute 'lastname', row.lastname
            attribute 'email', row.email
            attribute 'organization', row.organization
        }
    }

    Map params = [:]
    String where = buildWhereClause(filter, params, 'id', 'login')

    sql.eachRow(params, "SELECT * FROM Users " + where, closure);
}


void handleGroup(Sql sql) {
    Closure closure = { row ->
        ICFObjectBuilder.co {
            uid row.id as String
            id row.name
            attribute 'description', row.description
        }
    }

    Map params = [:]
    String where = buildWhereClause(filter, params, 'id', 'name')

    sql.eachRow(params, "SELECT * FROM Groups " + where, closure)
}

void handleOrganization(Sql sql) {
    Closure closure = { row ->
        ICFObjectBuilder.co {
            uid row.id as String
            id row.name
            attribute 'description', row.description
        }
    }

    Map params = [:]
    String where = buildWhereClause(filter, params, 'id', 'name')

    sql.eachRow(params, "SELECT * FROM Organizations " + where, closure)
}

static String buildWhereClause(Filter filter, Map sqlParams, String uidColumn, String nameColumn) {
    if (filter == null) {
        log.info("Returning empty where clause")
        return ""
    }

    Map query = filter.accept(MapFilterVisitor.INSTANCE, null)

    log.info("Building where clause, query {0}, uidcolumn {1}, nameColumn {2}", query, uidColumn, nameColumn)

    String columnName = uidColumn.replaceFirst("[\\w]+\\.", "")

    String left = query.get("left")
    if (Uid.NAME.equals(left)) {
        left = uidColumn
    } else if (Name.NAME.equals(left)) {
        left = nameColumn
    }

    String right = query.get("right")

    String operation = query.get("operation")
    switch (operation) {
        case "CONTAINS":
            right = '%' + right + '%'
            break;
        case "ENDSWITH":
            right = '%' + right
            break;
        case "STARTSWITH":
            right = right + '%'
            break;
    }

    sqlParams.put(columnName, right)
    right = ":" + columnName

    def engine = new groovy.text.SimpleTemplateEngine()

    def whereTemplates = [
            CONTAINS          : ' $left ${not ? "not " : ""}like $right',
            ENDSWITH          : ' $left ${not ? "not " : ""}like $right',
            STARTSWITH        : ' $left ${not ? "not " : ""}like $right',
            EQUALS            : ' $left ${not ? "<>" : "="} $right',
            GREATERTHAN       : ' $left ${not ? "<=" : ">"} $right',
            GREATERTHANOREQUAL: ' $left ${not ? "<" : ">="} $right',
            LESSTHAN          : ' $left ${not ? ">=" : "<"} $right',
            LESSTHANOREQUAL   : ' $left ${not ? ">" : "<="} $right'
    ]

    def wt = whereTemplates.get(operation)
    def binding = [left: left, right: right, not: query.get("not")]
    def template = engine.createTemplate(wt).make(binding)
    def where = template.toString()

    log.info("Where clause: {0}, with parameters {1}", where, sqlParams)

    return where
}


/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.forgerock.openicf.connectors.scriptedsql.ScriptedSQLConfiguration
import org.forgerock.openicf.misc.scriptedcommon.OperationType
import org.identityconnectors.common.logging.Log

import java.sql.Connection

def log = log as Log
def operation = operation as OperationType
def connection = connection as Connection
def configuration = configuration as ScriptedSQLConfiguration

log.info("Entering " + operation + " Script")

log.info("Using driver: {0} version: {1}",
        connection.getMetaData().getDriverName(),
        connection.getMetaData().getDriverVersion())

Sql sql = new Sql(connection)

sql.eachRow("SELECT 1", {})



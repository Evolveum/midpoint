#!/usr/bin/env bash
#
# Copyright (C) 2010-2021 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# If TGZFILE is set in environment, the file with existing results will be imported.
# Git commit, branch and build ID will be taken from the file name.
# Example of old files import:
# for i in ~/perf-out/*.gz; do TGZFILE=$i bash perf-tst-process.sh ; done

set -eu

# can be overridden to psql.exe on Windows to avoid tty problems
: "${PSQL:="psql"}"
# the rest is normally set by Jenkins, BRANCH is used instead of GIT_BRANCH that contains slashes
: "${BUILD_NUMBER:="dev"}"
: "${BRANCH:=$(git rev-parse --abbrev-ref HEAD)}"
: "${GIT_COMMIT:=$(git show -s --format=%H)}"

# backup
mkdir -p "${HOME}/perf-out/"
if [ -z "${TGZFILE:-}" ]; then
  # if branch contains / we replace it with _ which we generally don't use in branch names
  TARFILE="${HOME}/perf-out/mp-perf-${BRANCH/\//_}-${BUILD_NUMBER}-${GIT_COMMIT}.tar"
  rm -f "${TARFILE}" "${TARFILE}.gz"
  find -wholename '*target/PERF-*' -exec tar --transform 's/.*\///g' -rvf "${TARFILE}" {} \;
  gzip "${TARFILE}"
  TGZFILE="${TARFILE}.gz"
  echo "Performance reports backed up to ${TGZFILE}"
else
  echo "Importing ${TGZFILE} to DB"
  BRANCH=$(basename $TGZFILE | cut -d- -f3)
  # _ was likely / originally, see above
  BRANCH="${BRANCH/_/\/}"
  BUILD_NUMBER=$(basename $TGZFILE | cut -d- -f4)
  GIT_COMMIT=$(basename $TGZFILE | cut -d- -f5 | cut -d. -f1)
fi

COMMIT_DATE=$(git show -s --format=%cI "${GIT_COMMIT}")

# load to DB
BUILD_ID=$(psql -tc "select id from mst_build where commit_hash='${GIT_COMMIT}'")
if [ -n "${BUILD_ID}" ]; then
  echo "Results for commit ${GIT_COMMIT} already processed, no action needed."
  exit
fi

# create new build entry
BUILD_ID=$(
  "${PSQL}" -qtAX -c "insert into mst_build (build, branch, commit_hash, date) values ('${BUILD_NUMBER}', '${BRANCH}', '${GIT_COMMIT}', '${COMMIT_DATE}') returning id"
)

echo "BUILD_ID = $BUILD_ID"

# pre-process CSVs for each metric type
mkdir -p perf-tmp
cd perf-tmp
rm -f PERF-*
tar xzf "${TGZFILE}"

# stopwatch note is not imported yet due to quoting/escaping problems
echo "build_id,test,monitor,count,total_us,avg_us,min_us,max_us" >stopwatch.csv
echo "build_id,test,operation,count,total_ms,min_ms,max_ms,avg_ms" >glob_perf_info.csv
echo "build_id,test,metric,count" >query.csv

for FILE in PERF-*; do
  # TODO: if quoting is changed, cut removing note may be removed
  sed -e '1,/\[stopwatch]/d;/^test,/d;/^$/,$d' -e 's/^/'${BUILD_ID}',/g' "${FILE}" | cut -d, -f 1-8 >>stopwatch.csv
  sed -e '1,/\[globalPerformanceInformation]/d;/^test,/d;/^$/,$d' -e 's/^/'${BUILD_ID}',/g' "${FILE}" >>glob_perf_info.csv
  sed -e '1,/\[query]/d;/^test,/d;/^$/,$d' -e 's/^/'${BUILD_ID}',/g' "${FILE}" >>query.csv
done

# import into DB
# set PGHOST(ADDR), PGPORT, PGDATABASE, PGUSER... appropriately for psql
# TODO: if quoting is changed, fix QUOTE character and perhaps add ESCAPE
"${PSQL}" -c "\copy mst_stopwatch FROM 'stopwatch.csv' WITH CSV HEADER DELIMITER ',' QUOTE E'\b';"
"${PSQL}" -c "\copy mst_glob_perf_info FROM 'glob_perf_info.csv' WITH CSV HEADER DELIMITER ',' QUOTE E'\b';"
"${PSQL}" -c "\copy mst_query FROM 'query.csv' WITH CSV HEADER DELIMITER ',' QUOTE E'\b';"

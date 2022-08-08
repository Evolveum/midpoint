# The account*.csv files contain some data columns plus some auxiliary ones:
#
# - The `uid` has to be a pure integer. The accounts are processed in the order of their `uid`.
# - The `test` column describes the expected result of the correlation:
#   - `_none` means that no matching
#   - `_uncertain` means that the correlator couldn't decide
#   - a name is a name of a specific user
#
# The automated tests processes these accounts and check if the correlator or matcher acts accordingly.

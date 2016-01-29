#!/bin/sh

#/*
# * Copyright (c) 2010-2013 Evolveum
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# *     http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# */

#// Parameters:
#// The connector sends the following:
#// __NAME__ (value of icfs:name)
#// attributeName (value of attribute)
#// ...

## TODO: connector does not bother with exit values...
if [ ! -w /tmp/homedirs ]; then
	echo "Directory not writable"
	exit 1
else exit 0
fi


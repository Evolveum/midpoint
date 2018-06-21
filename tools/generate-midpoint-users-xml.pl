#!/usr/bin/perl
#
# Copyright (c) 2014-2016 Evolveum
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Authors: Ivan Noris, Radovan Semancik
#
# Perl midpoint user XML generator
#
# Generates import XML files with a number of user objects.
#
# Create a file with a user template like this:
#
#  <user>
#    <name>u%%ID%%</name>
#    <givenName>%%GIVEN_NAME%%</givenName>
#    <familyName>%%FAMILY_NAME%%</familyName>
#    <fullName>%%GIVEN_NAME%% %%FAMILY_NAME%% (%%ID%%)</fullName>
#    ... other properties here ...
#  </user>
#
#  Run as:
#
# generate-midpoint-users.pl <template.xml> [ <start> ] [ <count> ]
#

use strict;

my $user_template_filename;
my $line;
my $content = '';
my $cnt_text;
my $start_users = 1;
my $max_users = 1;
my @consonants = (qw(b c d f g h j k l m n p r s t v w x z));
my @vowels = (qw(a e i o u));


die("Usage: $0 user_template_file [count]\n") unless defined($ARGV[0]);
if (defined($ARGV[1])) {
	$start_users = $ARGV[1];
}

if (defined($ARGV[2])) {
	$max_users = $ARGV[2];
}

$user_template_filename = $ARGV[0];
open(FH, "<$user_template_filename") or die("Can't open '$user_template_filename' $!");
while ($line = <FH>)
{
#	chop $line;
	$content .= $line;
}
print << 'ENDHEADER';
<?xml version="1.0" encoding="UTF-8"?>
<objects xmlns="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
	xmlns:c="http://midpoint.evolveum.com/xml/ns/public/common/common-3"
	xmlns:org="http://midpoint.evolveum.com/xml/ns/public/common/org-3"
	xmlns:icfs="http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3"
	xmlns:t="http://prism.evolveum.com/xml/ns/public/types-3"
	xmlns:q="http://prism.evolveum.com/xml/ns/public/query-3"
	xmlns:ri="http://midpoint.evolveum.com/xml/ns/public/resource/instance-3">

ENDHEADER

foreach (my $counter = $start_users + 1; $counter <= $max_users; $counter++)
{
	my $loop_content = $content;
	$cnt_text = sprintf("%05d", $counter);
	$loop_content =~ s/%%ID%%/$cnt_text/g;

	my $givenName = randomName();
	$loop_content =~ s/%%GIVEN_NAME%%/$givenName/g;

	my $familyName = randomName();
	$loop_content =~ s/%%FAMILY_NAME%%/$familyName/g;

	my $givenName = randomName();

	print $loop_content;
}

print "</objects>\n";
close(FH);


sub randomName {
  my $numsyl = int(rand(2)) + 2;
  my $out = "";
  for (1..$numsyl) {
    $out .= $consonants[rand(@consonants)];
    $out .= $vowels[rand(@vowels)];
    if (rand(2) > 1) {
      $out .= $consonants[rand(@consonants)];
    }
  }
  return ucfirst($out);
}

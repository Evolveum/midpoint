#!/usr/bin/perl
#
# Copyright (c) 2010-2020 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
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

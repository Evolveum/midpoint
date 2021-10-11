#!/usr/bin/perl
#
# Copyright (c) 2010-2020 Evolveum and contributors
#
# This work is dual-licensed under the Apache License 2.0
# and European Union Public License. See LICENSE file for details.
#
# Hack to set copyright headers
# TODO: maybe it needs fix... or removal?
#
# Author: Radovan Semancik
#

use strict;
use warnings;

use Getopt::Long qw(:config bundling no_auto_abbrev pass_through);
use Pod::Usage;
use File::Basename;
use Data::Dumper;

my ($verbose,$optHelp);
my ($recursive,$modify);
my $regexp = undef;
my $debug = 0;

$SIG{__DIE__} = sub { Carp::confess(@_) };

my @excludes = qw(target .*\.versionsBackup .*~ .*\.iml \.project);
my $commentChar = "#";
my $licencePattern = "Licensed under the Apache License";

my $license = <<EOT;

 Copyright (c) 2010-2020 Evolveum and contributors

This work is dual-licensed under the Apache License 2.0
and European Union Public License. See LICENSE file for details.
EOT

my $dir;
if (defined $ARGV[0] && $ARGV[0] !~ /^-/) {
  $dir = shift;
}

GetOptions (
  "regexp|e=s" => \$regexp,
  "recursive|r" => \$recursive,
  "modify|m" => \$modify,
  "verbose|v" => \$verbose,
  "debug|d" => \$debug,
  "help" => \$optHelp,
  ) or usage();
usage() if $optHelp;

if (!defined($dir)) {
  $dir = shift;
}

if (!$dir) { usage(); }
if ($dir eq "--help") { usage(); }

print "dir: $dir, recursive=$recursive, regexp=$regexp\n";

if (-f $dir) {
  header($dir);
} elsif (-d $dir) {
  processDir($dir);
} else {
  die("Eh?");
}

sub processDir {
  my ($currDir) = @_;

  opendir(my $dh, $currDir) || die ("Cannot open $currDir: $!\n");
  my @subs = readdir $dh;
  closedir $dh;
  foreach my $sub (@subs) {
    if ($sub =~ /^\./) {
      next;
    }
    if (grep {$sub =~ /^$_$/} @excludes) {
      next;
    }
    my $subpath = "$currDir/$sub";
    if (-f $subpath) {
      if ($regexp && $sub !~ /$regexp/) {
        next;
      }
      header($subpath);
    } elsif ($recursive && -d $subpath) {
      processDir($subpath);
    }
  }
}

sub header {
  my ($path) = @_;

  open(my $fh, $path) or die("Cannot open $path: $!\n");
  my (@lines) = <$fh>;
  close $fh;

  my $hasLicense = 0;
  my $nonComment = 0;
  my @linesToKeep;
  foreach my $line (@lines) {
    if ($line =~ /^\s*$commentChar/) {
      if ($line =~ /$licencePattern/) {
        $hasLicense = 1;
        last;
      }
    } else {
      $nonComment = 1;
    }
    if ($nonComment) {
      push @linesToKeep,$line;
    }
  }

  if (!$hasLicense) {
    print "$path: $hasLicense ".scalar(@lines)."/".scalar(@linesToKeep)." lines\n";

    if ($modify) {
      open($fh, ">$path") or die("Cannot write to $path: $!\n");
      foreach my $lline (split("\n",$license)) {
        print $fh $commentChar." ".$lline."\n";
      }
      print $fh "\n";
      foreach my $fline (@linesToKeep) {
        print $fh $fline;
      }
      close($fh);
    }
  }

}


### USAGE and DOCUMENTATION

sub usage {
  pod2usage(-verbose => 2);
  exit(1);
}

sub man {
  pod2usage(-verbose => 3);
  exit(0);
}

__END__

=head1 NAME

copyheader - hack to set copyright headers

=head1 SYNOPSIS

copyheader [options] dir

=cut

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

my $verbose = 0;
my $optHelp = 0;
my $recursive = 0;
my $modify = 0;
my $fileregexp = "";
my $debug = 0;

$SIG{__DIE__} = sub { Carp::confess(@_) };

my @excludes = qw(target .*\.versionsBackup .*~ .*\.iml \.project);
my $licencePattern = 'Licensed under the EUPL-1.2 or later';
my $copyrightPattern = "Copyright \\([Cc]\\)";

# Taken from "European Union Public Licence EUPL Guidelines July 2021"
my $licenseText = 'Licensed under the EUPL-1.2 or later.';
my $copyrightText = 'Copyright (c) 2010-2025 Evolveum and contributors';

my $fileconfig = {
    'java' => {
        'cstart' => '/*',
        'cstartp' => '/\\*',
        'cbody' => ' *',
        'cbodyp' => '\\*',
        'cend' => ' */',
        'cendp' => '\\*/',
    },
    'css' => {
        'cstart' => '/*',
        'cstartp' => '/\\*',
        'cbody' => ' *',
        'cbodyp' => '\\*',
        'cend' => ' */',
        'cendp' => '\\*/',
    },
    'xml' => {
        'cstart' => '<!--',
        'cstartp' => '<\\!--',
        'cbody' => '  ~',
        'cbodyp' => '\\~',
        'cend' => '  -->',
        'cendp' => '-->',
        'prologp' => '\\<\\?xml',
    },
    'xsd' => {
        'cstart' => '<!--',
        'cstartp' => '<\\!--',
        'cbody' => '  ~',
        'cbodyp' => '\\~',
        'cend' => '  -->',
        'cendp' => '-->',
        'prologp' => '<\\?xml',
    },
};

my $dir;
if (defined $ARGV[0] && $ARGV[0] !~ /^-/) {
  $dir = shift;
}

GetOptions (
  "regexp|e=s" => \$fileregexp,
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

print "dir: $dir, recursive=$recursive, regexp=$fileregexp\n";

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
      if ($fileregexp && $sub !~ /$fileregexp/) {
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

  my ($filename, $dirs, $suffix) = fileparse($path, qr/\.[^.]*/);
  $suffix =~ s/^\.//;
  my $suffixconfig = $fileconfig->{$suffix};
  if (!$suffixconfig) {
    warn("$path: UNKNOWN suffix $suffix, skipping");
    return;
  }
  my $cstart = $suffixconfig->{'cstart'};
  my $cend = $suffixconfig->{'cend'};
  my $cbody = $suffixconfig->{'cbody'};
  my $cstartp = $suffixconfig->{'cstartp'};
  my $cendp = $suffixconfig->{'cendp'};
  my $cbodyp = $suffixconfig->{'cbodyp'};
  my $prologp = $suffixconfig->{'prologp'};

  open(my $fh, $path) or die("Cannot open $path: $!\n");
  my (@lines) = <$fh>;
  close $fh;

print ("PPP: prologp $prologp\n");

  my $hasLicense = 0;
  my $copyrightLine = undef;
  my $prologLine = undef;
  my $reachedFileBody = 0;
  my @linesToKeep;
  foreach my $line (@lines) {
    if ($reachedFileBody) {
      push @linesToKeep, $line;
    } else {
        if ($line =~ /^\s*$/) { # blank line
            next;
        } elsif ($prologp && $line =~ /^\s*$prologp/) {
            print("->P ".$line) if $debug;
            $prologLine = $line;
        } elsif (isComment($line, $cstartp, $cbodyp, $cendp)) {
          print("->C ".$line) if $debug;
          if ($line =~ /$licencePattern/) {
            $hasLicense = 1;
          }
          if ($line =~ /$copyrightPattern/) {
            $copyrightLine = $line;
          }
        } else {
          print("->X ".$line) if $debug;
          $reachedFileBody = 1;
        }
    }
  }

  if ($hasLicense && $copyrightLine) {
    print "$path: OK\n";
  } else {
    print "$path: FIX ".scalar(@lines)."/".scalar(@linesToKeep)." lines\n";

    if ($modify) {

      open($fh, ">$path") or die("Cannot write to $path: $!\n");

      print $fh $prologLine if $prologLine;

      print $fh $cstart."\n" if $cstart;

      if ($copyrightLine) {
        print $fh $copyrightLine;
      } else {
        print $fh $cbody." ".$copyrightText."\n";
      }

      print $fh $cbody."\n";

      foreach my $lline (split("\n",$licenseText)) {
        print $fh $cbody." ".$lline."\n";
      }

      print $fh $cend."\n" if $cend;
      print $fh "\n";

      foreach my $fline (@linesToKeep) {
        print $fh $fline;
      }

      close($fh);

    }
  }

}


sub isComment {
    my ($line, $cstart, $cbody, $cend) = @_;
    if ( $cstart &&  $line =~ /^\s*$cstart/ ) { return 1 }
    if ( $cend &&  $line =~ /^\s*$cend/ ) { return 1 }
    if ( $cbody &&  $line =~ /^\s*$cbody/ ) { return 1 }
    return 0;
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

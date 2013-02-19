#!/usr/bin/perl
# (c) 2013 Radovan Semancik
use strict;
use warnings;
#use Data::Dumper;

sub run ($) {
  my ($command) = @_;
  open(OUT,$command."|") or die("Cannot execute: $command: $!\n");
  my $out;
  while (<OUT>) {
    $out .= $_;
  }
  close(OUT);
  if ($? == 0) {
    # success
    return undef;
  }
  if ($out) {
    return $out;
  }
  return "";
}

sub usage() {
  print STDERR "Gotcha will run the command in a loop until it fails\n";
  print STDERR "$0 <command>\n";
  exit(-1);
}

sub divider() {
  print("+---------+------------+---------------------------+------------+\n");
}

sub header() {
  divider();
  print("| Result  | Attempt    | Start                     | Duration   |\n");
  divider();
}

sub line($$$$) {
  my ($status,$attempt,$startTime,$duration) = @_;
    printf("| %7s | %10d | %25s | %10d | \n",$status,$attempt,scalar(localtime($startTime)),$duration);
}

my $maxAttempts = undef;
my $command = join(' ',@ARGV) || usage();

header();

my $attempt = 0;
while(1) {
  $attempt++;
  my $startTime = time();
  my $out = run($command);
  my $endTime = time();
  if (defined $out) {
    line("FAILURE",$attempt,$startTime,$endTime-$startTime);
    divider();
    print ("\n\n".("="x80)."\n");
    print "$out\n";
    print (("="x80)."\n");
    last;
  } else {
    line("SUCCESS",$attempt,$startTime,$endTime-$startTime);
  }
  if (defined $maxAttempts && $attempt >= $maxAttempts) {
    divider();
    last;
  }
}

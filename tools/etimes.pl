#!/usr/bin/perl
use strict;

my %ops;

my $line = <>;
while ($line) {
  chomp $line;
  if ($line =~ /^(\S+)\s+(\S+)\s+\[([^]]+)\]\s+(\w+)/) {
    my %op = ();
    $op{date} = $1;
    $op{time} = $2;
    $op{thread} = $3;
    $op{level} = $4;
    my $halfline = $';
    if ($halfline =~ /####\s+Entry:\s*(\d+)\s+(\S+)/) {
      my $rest = $';
      my $opkey = $1;
      my $op = $ops{$opkey};
      $op->{op} = $2;
      $ops{$opkey} = $op;
      my @args;
      $op->{args} = \@args;
      $line = <>;
      while ($line =~ /###### args:/) {
        push @args,$';
        $line = <>;
      }
      next;
    }
    if ($halfline =~ /####\s+Exit:\s*(\d+)\s+(\S+)/) {
      my $rest = $';
      my $opkey = $1;
      my $op = $ops{$opkey};
      $op->{op} = $2;
      $ops{$opkey} = $op;
      if ($rest =~ /etime:\s*([\d\.]+)/) {
        $op->{etime} = $1;
      }
    }
  }
  $line = <>;
}

foreach my $op (sort { $a->{etime} <=> $b->{etime} } values %ops) {
  print $op->{etime}.": ".$op->{op}."\n";
  foreach my $args (@{$op->{args}}) {
    print "  ".$args."\n";
  }
}
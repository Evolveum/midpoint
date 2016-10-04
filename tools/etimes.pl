#!/usr/bin/perl
use strict;
#use Data::Dumper;

my %ops;
my %cumulative;
my %subsystems;
my $line;
my %threads;
my $rootStackFrame = { op => "ROOT" };
my %packageToSubsystem = (
  repo => "REPOSITORY",
  provisioning => "PROVISIONING",
  web => "WEB",
  mode => "MODEL",
);

parse();
displayIndividualTimes();
print "\n";
print ("="x80);
print "\n\n";
displayCumulativeTimes();
print "\n";
print ("="x80);
print "\n\n";
displaySubsystemTimes();

sub parse {
  $line = <>;
  while ($line) {
    chomp $line;
    if ($line =~ /^(\S+)\s+(\S+)\s+\[([^]]+)\]\s+(\w+)/) {
      my %logdata = ();
      $logdata{date} = $1;
      $logdata{time} = $2;
      $logdata{thread} = $3;
      $logdata{level} = $4;
      my $halfline = $';
      processHalfline($halfline,\%logdata);
    }
    if ($line =~ /^(\S+)\s+(\S+)\s+(\w+):/) {
      my %logdata = ();
      $logdata{date} = $1;
      $logdata{time} = $2;
      $logdata{level} = $3;
      my $halfline = $';
      processHalfline($halfline,\%logdata);
    }
    if ($line =~ /^(\S+)\s+(\S+)\s+\[([^]]+)\]\s+\[([^]]+)\]\s+(\w+)/) {
      my %logdata = ();
      $logdata{date} = $1;
      $logdata{time} = $2;
      $logdata{subsystem} = $3;
      $logdata{thread} = $4;
      $logdata{level} = $5;
      my $halfline = $';
      processHalfline($halfline,\%logdata);
    }
    $line = <>;
  }
}

sub processHalfline {
  my ($halfline,$logdata) = @_;
  
  my $thread = $logdata->{thread};
  
  if ($halfline =~ /####\s+Entry:\s*(\d+)\s+(\S+)/) {
    my $rest = $';
    my $opkey = $1;
    my $op = $ops{$opkey};
    $op->{key} = $1;
    $op->{op} = $2;
    if ($logdata->{subsystem}) {
      $op->{subsystem} = $logdata->{subsystem};
    } else {
      $op->{subsystem} = packageToSubsystem($op->{op});
    }
    $ops{$opkey} = $op;
    push @{$threads{$thread}},$op;
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
    my $opname = $2;
    my $rest = $';
    my $opkey = $1;
    my $op = $ops{$opkey};
    $op->{op} = $opname;
    $ops{$opkey} = $op;
    if ($rest =~ /etime:\s*([\d\.]+)/) {
      $op->{etime} = $1;
    }
    my $stackop = pop @{$threads{$thread}};
    if ($stackop->{op} ne $opname) {
      print "!!! stack mismatch: ".$stackop->{op}." - ".$opname."\n";
    }
    my $parentop = $threads{$thread}->[$#{$threads{$thread}}] || $rootStackFrame;
    count($op,$parentop);
  }
  
}

sub packageToSubsystem {
  my ($op) = @_;
  
  foreach my $key (keys %packageToSubsystem) {
    if ($op =~ $key) {
      return $packageToSubsystem{$key};
    }
  }
  return undef;
}

sub count {
  my ($op,$parentop) = @_;
  
  if ($parentop->{owntime} > 0 || $op->{owntime} > 0) {
    warn "Possibly inconsistent data, is the log from two executions?\n";
#    print Dumper($op,$parentop,\%threads);
  }
  
  my $etime = $op->{etime};
  
  $parentop->{owntime} -= $etime;
  $op->{owntime} += $etime;
  
  my $opname = $op->{op};
  $cumulative{$opname}->{op} = $opname;
  $cumulative{$opname}->{invocations}++;
  $cumulative{$opname}->{totalTime} += $op->{etime};
  $cumulative{$opname}->{ownTime} += $op->{owntime};
  
  my $subsystem = $op->{subsystem};
  $subsystems{$subsystem}->{subsystem} = $subsystem;
  $subsystems{$subsystem}->{invocations}++;
  $subsystems{$subsystem}->{totalTime} += $op->{etime};
  $subsystems{$subsystem}->{ownTime} += $op->{owntime};
}

sub displayIndividualTimes {
  foreach my $op (sort { $a->{etime} <=> $b->{etime} } values %ops) {
    printf "%40s(%4s): total: %-15f own: %-15f : ",$op->{op},$op->{key},$op->{etime},$op->{ownTime};
#    print $op->{etime}.": ".$op->{key}." ".$op->{op}."\n";
    foreach my $args (@{$op->{args}}) {
      print "  ".$args."\n";
    }
  }
}

sub displayCumulativeTimes {
  foreach my $op (sort { $a->{ownTime} <=> $b->{ownTime} } values %cumulative) {
    printf "%25s: total: %-15f own: %-15f (%d)\n",$op->{op},$op->{totalTime},$op->{ownTime},$op->{invocations};
#    print $op->{totalTime}.": ".$op->{op}." (".$op->{invocations}.")\n";
  }
}

sub displaySubsystemTimes {
  my $totalOwnTime = 0;
  map { $totalOwnTime += $_->{ownTime} } values %subsystems;
  foreach my $op (sort { $a->{ownTime} <=> $b->{ownTime} } values %subsystems) {
    my $subsystem = $op->{subsystem};
    printf "%15s:  total:%-15f  own:%-15f %3d%%  (%d)\n",$op->{subsystem},$op->{totalTime},$op->{ownTime},($op->{ownTime}*100/$totalOwnTime),$op->{invocations};
#    print $op->{totalTime}."(total): ".$op->{subsystem}.": total:".$op->{totalTime}." own:".ownTime($subsystem)." (".$op->{invocations}.")\n";
  }
  
  printf "%15s:  total:%-15f  own:%-15f\n","ROOT",$rootStackFrame->{totalTime},$rootStackFrame->{ownTime};
  
}


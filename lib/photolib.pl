# Photo library routines
# Copyright(c) 1997-1998  Dustin Sallings

use CGI;
use Postgres;

# Global stuffs
$cgidir="/cgi-bin/dustin/photo";
$imagedir="/~dustin/images/";
$uriroot="/~dustin/photo";
$Itop="$uriroot/album";
$includes="/usr/people/dustin/public_html/photo/inc";
$adminc="/usr/people/dustin/public_html/photo/admin/inc";

# Here we store the persistent database handler.
local($dbh)=0;

sub openDB
{
    $dbh=db_connect("photo") || die("Cannot connect to database\n");
}

sub doQuery
{
    my($query)=@_;
    my($s);

    # Open the database if it's not already.
    openDB() unless($dbh);

    if(!($s=$dbh->execute($query)))
    {
        die "Datbase error: $Postgres::error\n<!--\n$query\n-->\n";
    }
    return($s);
}

sub addTail
{
    my(%p, @a, @vars);

    @vars=qw(FILE_DEV FILE_INO FILE_MODE FILE_NLINK FILE_UID FILE_GID
             FILE_RDEV FILE_SIZE FILE_ATIME FILE_MTIME FILE_TIME
             FILE_BLKSIZE FILE_BLOCKS);

    @a=stat($ENV{'SCRIPT_FILENAME'});
    for(0..$#a)
    {
        $p{$vars[$_]}=$a[$_];
    }

    $p{'LAST_MODIFIED'}=localtime($p{FILE_MTIME});

    showTemplate("$includes/tail.inc", %p);
}
sub showTemplate
{
    my($fn, %p, $q)=@_;

    $q=CGI->new;
    map { $p{uc($_)}=$q->param($_) if(!defined($p{$_}))} $q->param;
    map { $p{$_}=$ENV{$_} if(!defined($p{$_})) } keys(%ENV);

    $p{'URIROOT'}=$uriroot;
    $p{'CGIDIR'}=$cgidir;
    $p{'IMAGEDIR'}=$imagedir;
    $p{'ITOP'}=$Itop;

    $p{'ALL_VARS'}=join("\n", sort(keys(%p)));

    open(IN, $fn) || die("Can't open $fn:  $!\n");
    while(<IN>)
    {
        s/%([A-Z0-9_]+)%/$p{$1}/g;
        print;
    }

    close(IN);
}

1;

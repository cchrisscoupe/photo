# Photo library routines
# Copyright(c) 1997-1998  Dustin Sallings
#
# $Id: Photo.pm,v 1.67 1999/01/30 22:20:36 dustin Exp $

package Photo;

use CGI;
use DBI;
use DCache;
use MIME::Base64();
use strict;

sub new
{
	my $self = {};
	bless($self);
	$self->configure;
	return($self);
}

sub configure
{
	my($self)=@_;
	my(%h, $tmp);
	local(*_CONF);

	# defaults
	%h=(
		'cgidir' => "/perl/dustin/photo",
		'uriroot' => "/~dustin/photo",
		'includes' => "/usr/people/dustin/public_html/photo/inc",
		'dbname' => 'photo',
		'dbhost' => 'bleu',
		'dbuser' => 'nobody',
		'dbpass' => '',
	);

	# may want to search through more photo.confs in the future
	for( qw(/usr/local/etc/photo.conf) ) {
		$tmp=open(_CONF, $_);
		last if($tmp);
	}

	if($tmp) {
		while(<_CONF>) {
			my(@a);
			chomp;
			@a=split(/\s+=\s+/);
			$h{$a[0]}=$a[1];
		}
		close(_CONF);
	}

	$self->{'config'}=\%h;
}

sub openDB
{
	my($self)=shift;
	my($dbname, $dbhost, $dbuser, $dbpass)=(
		$self->{'config'}{'dbname'},
		$self->{'config'}{'dbhost'},
		$self->{'config'}{'dbuser'},
		$self->{'config'}{'dbpass'}
	);

	$self->{'dbh'}=DBI->connect("dbi:Pg:dbname=$dbname;host=$dbhost",
		$dbuser,$dbpass) || die("Cannot connect to database\n");
}

sub DESTROY
{
	my($self)=@_;

		if($self->{'dbh'}) {
			$self->{'dbh'}->disconnect();
			undef($self->{'dbh'});
		}
}

sub doQuery
{
	my $self=shift;
	my($query)=@_;
	my($s, $rv);

	$self->openDB unless($self->{'dbh'});

	$s=$self->{'dbh'}->prepare($query);

	if(defined($s)) {
		$rv=$s->execute;
	}

	if(!defined($rv)) {
		die("Database Error:  $DBI::errstr\n<!--\n$query\n-->\n");
	}

	return($s);
}

sub setAuto
{
	my $self=shift;
		my($value)=@_;

	$self->openDB unless($self->{'dbh'});

	$self->{'dbh'}->{AutoCommit}=$value;
}

sub commit
{
	my $self=shift;
	$self->openDB unless($self->{'dbh'});
	$self->{'dbh'}->commit;
}

sub rollback
{
	my $self=shift;
	$self->openDB unless($self->{'dbh'});
	$self->{'dbh'}->rollback;
}

sub getuid
{
	my $self=shift;
	my($name)=@_;
	my($id, $r, $s);

	if(!defined($name)) {
		$name=$ENV{'REMOTE_USER'};
	}

	$name=$self->myquote($name);

	$s=$self->doQuery("select getwwwuser($name)");
	$r=$s->fetch;
	if($r) {
		$id=$r->[0];
	} else {
		$id=-1; # -1 because this can easily be plugged into ACL code
	}

	return($id);
}

sub buildQuery
{
	my($self, $q)=@_;
	my(%h, $query, $needao, $sub, @tmp, $tmp, $snao, $ln, $order, $id);
	my(@a);

	for($q->param) {
		$h{$_}=$q->param($_);
		$h{$_}=~s/\'/\\\'/g;
	}

	$id=$self->getuid();

	$query ="select a.keywords,a.descr,b.name,\n";
	$query.="	a.size,a.taken,a.ts,a.id,a.cat,a.addedby,b.id\n";
	$query.="	from album a, cat b\n	where a.cat=b.id\n";
	$query.="		and a.cat in (select cat from wwwacl\n";
	$query.="			   where userid=$id)";

	$needao=0;
	$sub="";

	@tmp=$q->param('cat');

	if(@tmp) {
		$sub.=" and" if($needao++>0);
		$tmp="";

		map {
			$tmp.=" or" if($snao++>0);
			$tmp.="\n		  a.cat=$_";
		} @tmp;

		if(@tmp>1) {
			$sub.="\n	  ($tmp\n	  )";
		} else {
			$sub.="\n	$tmp";
		}
	}

	if($h{what} ne "") {
		my($a, $b, $c);
		$c=0;

		$sub.=" $h{fieldjoin}" if($needao++>0);

		@a=split(/\s+/, $h{what});
		$b=($h{keyjoin} eq "and")?"and":"or";

		if(@a>1) {
			$sub.="\n	  (";
			map {
				$sub.=" $b" if($c++>0);
				$sub.="\n\t$h{field} ~* '$_'";
			} @a;
			$sub.="\n	  )";
		} else {
			$sub.="\n	  $h{field} ~* '$h{what}'";
		}
	}

	if($h{tstart}) {
		$sub.=" $h{tstartjoin}" if($needao++>0);
		$sub.="\n	  a.taken>='$h{tstart}'";
	}

	if($h{tend}) {
		$sub.=" $h{tendjoin}" if($needao++>0);
		$sub.="\n	  a.taken<='$h{tend}'";
	}

	if($h{start}) {
		$sub.=" $h{startjoin}" if($needao++>0);
		$sub.="\n	  a.ts>='$h{start}'";
	}

	if($h{end}) {
		$sub.=" $h{endjoin}" if($needao++>0);
		$sub.="\n	  a.ts<='$h{end}'";
	}

	if(length($sub)>0) {
		$query.=" and\n	($sub\n	)";
	}

	if(defined($h{order}) && $h{order}=~/[A-z0-9]/) {
		$order=$h{order};
	} else {
		$order="a.taken";
	}

	$h{sdirection}|="";
	$query.="\n	order by $order $h{sdirection};\n";

	return($query);
}

sub setStyle
{
	my($self, $q)=@_;
	my($style, $cookie, %in, %p);

	%in=map { $_, $q->param($_) } $q->param;

	if(!defined($in{'font'}) || !defined($in{'h_transform'})) {
		print $q->header;
		print "Damnit, use the form.\n";
		exit;
	}

	# The style for the cookie.
	$style=<<EOF;
body,td {font-family: $in{'font'}, Arial; background-color:
$in{'bgcolor'};}
EOF

	# Apply the blockquote transform if we've got one.
	if($in{'d_transform'} ne "none") {
    	$style.="blockquote {text-transform: $in{'d_transform'};};";
	};

	# apply the header transform if we've got one.
	if($in{'h_transform'} ne "none") {
    	$style.="h1,h2,h3,h4,h5 {text-transform: $in{'h_transform'};}";
	};

	$cookie=$q->cookie(-name=>'photo_style',
		-path=>$self->{'config'}{'cgidir'},
		-value=>$style,
		-expires=>'+30d');
	print $q->header(-cookie => $cookie);
	$self->start_html($q, 'Set Style');

	%p=();
	$p{'STYLE'}=$style;
    $self->showTemplate("setstyle.inc", %p);
}

sub saveSearch
{
	my($self, $q)=@_;
	my($query, $name, %p);

	# don't allow anyone who doesn't have the canadd flag set.
	if($self->canadd($ENV{'REMOTE_USER'})!=1) {
		$self->showTemplate("savesearch_deny.inc", ());
		return;
	}

	$self->start_html($q, 'Saving your Search');

	$name=$q->param('name');

	$query ="insert into searches (name, addedby, search) ";
	$query.="values(\n\t'$name', '$ENV{REMOTE_USER}',\n'",
	$query.=$q->param('search');
	$query.="');\n";

	$self->doQuery($query);

	%p=('QUERY', $query);
	$self->showTemplate("savedsearch.inc", %p);
}

sub cacheImage
{
	my($self, $key, $img, $type)=@_;
	my($query, $r, $s, $c, $out, $done);

	$c=DCache->new;

	$self->setAuto(0);

	$out="";
	$query ="declare c cursor for select * from image_store where id=$img\n";
	$query.="	order by line\n";
	$self->doQuery($query);

	$done=0;
	while($done==0) {
			$s=$self->doQuery("fetch 20 in c;\n");
				$done=20;
				while($r=$s->fetch) {
						$done--;
			$out.=MIME::Base64::decode($r->[2]);
				}
	}
	$s->finish;
	# Only cache if we got something, maybe this will help with some of the
	# problems...
	if(length($out)>0) {
	    $c->cache($key, $type, $out);
	}
	undef($out);
	$self->setAuto(1);
}

sub makeThumbnail
{
	my($self, $img, $type)=@_;
	my($query, $r, $s, $c, $key, $tmp, $out, $i);

	$c=DCache->new;

	$key="photo-image: tn/$img";
	if(!$c->checkcache($key)) {
	   $self->cacheImage($key, $img, $type);
	}

		# Try real hard to generate the thumbnail.
		for($i=0; $i<3; $i++) {
		open(OUT, ">/tmp/photoout.$img.$$");
		$c->printcache_only($key, \*OUT);
		close(OUT);

				if(-s "/tmp/photoout.$img.$$") {
			system('/usr/local/bin/convert', '-size', '100x100',
				   "/tmp/photoout.$img.$$", "/tmp/photoout.tn.$img.$$");

			unlink("/tmp/photoout.$img.$$");
						last;
				}
		}

	$c=DCache->new;
	$key="photo-image: tn/$img";

	$out="";
	open(IN, "/tmp/photoout.tn.$img.$$");
	while( read(IN, $tmp, 1024) ) {
		$out.=$tmp;
	}
	close(IN);
	unlink("/tmp/photoout.tn.$img.$$");

	if(length($out)>0) {
		$c->cache($key, $type, $out);
	}
}

sub displayImage
{
	my($self, $img)=@_;
	my($query, $r, $s, $c, $key, $tmp, $q, $type, $tn);

	$q=CGI->new;

	# Probably need to handle multiple types someday
	$type="image/jpg";

	print $q->header(-type=>$type, -expires=>'+90d');

	$key="photo-image: $img";
	$tn=0;
	if($img=~/^tn./o) {
		$tn=1;
		$img=~s/^tn.//o;
	}

	$c=DCache->new;

	if(!$c->checkcache($key)) {

		if($tn) {
			$self->makeThumbnail($img, $type);
		} else {
			$self->cacheImage($key, $img, $type);
		}
	}

	$c->printcache_only($key);
}

sub showSaved
{
	my($self, $q)=@_;
	my($query, $name, $param, $s, $r, $cgi, %p, $out);

	$query="select * from searches order by name,addedby\n";

	$s=$self->doQuery($query);

	# $r is id, name, addedby, search

	$cgi =$self->{'config'}{'cgidir'};
	$cgi.="/photo.cgi";

	while($r=$s->fetch) {
		$out.= "	<li><a href=\"$cgi?" . MIME::Base64::decode($r->[3])
			   . "\">$r->[1]</a></li>\n";
	}
	$s->finish;

	return($out);
}

sub doFind
{
	my($self, $q)=@_;
	my($query, $s, $i, $start, $max, %p, $n, $nn, $r);

	$self->start_html($q, 'Find results');

	$query=$self->buildQuery($q);

	print "<!--\n$query\n-->\n";

	$s=$self->doQuery($query);

	$n=$s->rows;
	$i=0;

	$start=$q->param('qstart');  # Find the desired starting point
	$start+=0;				   # make it a number
	$q->delete('qstart');		# Delete it so we can add it later

	if($start==0) { # is this your first time?
		my($selfurl, %stuff);
		$selfurl=MIME::Base64::encode($q->query_string);

		%stuff=("SEARCH" => $selfurl);
		$self->showTemplate("savesearch.inc", %stuff);
	}

	$max=$q->param('maxret');	# Find the desired max return "
	$max+=0;					 # make it a number

	print "<h2>Found $n match".(($n==1)?"":"es")."</h2>\n";
	print "Displaying ".($start+1)."-".
		  (($start+$max>$n)?$n:$start+$max). ":<br>\n";
	print "<table><tr>\n";

	while($r=$s->fetch) {
		($p{KEYWORDS}, $p{DESCR}, $p{CAT}, $p{SIZE}, $p{TAKEN},
			$p{TS}, $p{IMAGE}, $p{CATNUM}, $p{ADDEDBY})=@{$r};
		next if($i++<$start);

		last if( $max>0 && $i-$start>$max);

		if($self->ns_version()>=4.5) {
			$p{'IMAGEDATA'}="data:image/jpeg;base64,".
				$self->base64image("tn/$p{'IMAGE'}");
		} else {
			$p{'IMAGEDATA'}=$self->{'config'}{'cgidir'}.
				"/img.cgi/tn/$p{'IMAGE'}";
		}

		# Two columns.
		print "</tr>\n<tr>\n" if(($i+1)%2==0);

		print "<td>\n";
		$self->showTemplate("findmatch.inc", %p);
		print "</td>\n";  # "
	}
	$s->finish;

	print "</tr></table>\n";

	# Add a link to the next matches.
	if( (($start+$max) < $n) && $max>0) {
		if(($n-($start+$max))<$max) {
			$nn=($n-($start+$max));
			$nn=($nn==1)?"match":"$nn matches";
		} else {
			$nn="$max matches";
		}

		print $q->startform(-method=>'POST',-action=>$q->url);
		print $q->hidden(-name=>'qstart', -value=>$start+$max);

		print map { $q->hidden(-name=>$_, -value=>$q->param($_)) . "\n" }
		$q->param;

		print $q->submit(-value=>"Next $nn");

		print $q->endform;
	}
}

sub doDisplay
{
	my($self, $q)=@_;
	my($query, $s, @r, @mapme, %p, $id);
	%p=();

	$id=$self->getuid();

	$query ="select a.id,a.keywords,a.descr,\n";
	$query.="	a.size,a.taken,a.ts,b.name,a.cat,a.addedby,b.id\n";
	$query.="	from album a, cat b\n";
	$query.="	where a.cat=b.id and a.id=" . $q->param('id');
	$query.="\n	and a.cat in (select cat from wwwacl where ";
	$query.="userid=$id);\n";

	print "<!-- Query:\n$query\n-->\n";

	$s=$self->doQuery($query);

	if($s->rows<1) {
		print "ACL ERROR!!!  We don't want your type here.\n";
	} else {
		@r=@{$s->fetch};
		@mapme=qw(IMAGE KEYWORDS INFO SIZE TAKEN TIMESTAMP CAT CATNUM
				  ADDEDBY);
		map { $p{$mapme[$_]}=$r[$_] } (0..$#r);
		if($self->ns_version()>=4.5) {
			$p{'IMAGEDATA'}="data:image/jpeg;base64,".
				$self->base64image($p{'IMAGE'});
		} else {
			$p{'IMAGEDATA'}=$self->{'config'}{'cgidir'}
				."/img.cgi/$p{'IMAGE'}";
		}
		$self->showTemplate("display.inc", %p);
	}
	$s->finish;
}

sub doCatView
{
	my($self, $q)=@_;
	my($r, $query, $s, $t, $id);

	$self->start_html($q, 'View Images by Category');

	$id=$self->getuid();

	print "<h2>Category List</h2>\n";

	$query ="select name,id,catsum(id) as cs from cat\n";
	$query.="where id in\n";
	$query.="  (select cat from wwwacl where\n";
	$query.="   userid=$id)\n";
	$query.=" order by cs desc;";
	print "<!--\n$query\n-->\n";
	$s=$self->doQuery($query);

	print "<ul>\n";

	while($r=$s->fetch) {
		next if($r->[2]==0);

		$t=($r->[2]==1?"image":"images");

		print "<li>$r->[0]:  <a href=\"" . $self->{'config'}{'cgidir'}
			."/photo.cgi?func=search&".
		"searchtype=advanced&cat=$r->[1]&maxret=5\">$r->[2] $t</a></li>\n";
	}
	$s->finish;

	print "</ul>\n" . $q->end_html . "\n";
}

sub myquote
{
	my($self, $str)=@_;
	$self->openDB unless($self->{'dbh'});
	return($self->{'dbh'}->quote($str));
}

sub canadd {
	my($self, $user)=@_;
	my($r, $s, $query, $ret);

	$user=$self->myquote($user);

	$query="select canadd from wwwusers where username=$user\n";
	$s=$self->doQuery($query);
	$r=$s->fetch;
	$s->finish;
	$ret=$r?$r->[0]:0;
	return($ret);
}

sub addImage
{
	my($self, $q)=@_;
	my(@elements, %in, %tmp, $query, $ext, $f, $s, $r, $dbh, $size, $id);

	@elements=qw(category keywords picture info taken);
	%tmp=map{$_,1}@elements;
	%in=map{
	  $_,defined($tmp{$_})?$self->myquote($q->param($_)):$q->param($_)
	}$q->param;

	$self->start_html($q, 'Adding image');

	if($self->canadd($ENV{'REMOTE_USER'})!=1) {
		$self->showTemplate("add_denied.inc", ());
		return;
	}

	if($in{'picture'}=~/jpg.$/i) {
		$ext="jpg";
	} elsif($in{'picture'}=~/gif.$/i) {
		$ext="gif";
	} else {
		%tmp=('FILENAME',$in{'picture'});
		$self->showTemplate("add_badfiletype.inc", %tmp);
		return;
	}

	$f=$q->param('picture');

	eval {
		my($i, $premime, $tmp);
		$self->setAuto(0);
		$query ="insert into album(keywords,descr,cat,taken,addedby)\n";
		$query.="    values($in{'keywords'},\n\t$in{'info'},\n";
		$query.="    \t$in{'category'},\n\t$in{'taken'},\n";
		$query.="    '$ENV{'REMOTE_USER'}')\n";
		$self->doQuery($query);
		$query ="select currval('album_id_seq')\n";
		$s=$self->doQuery($query);
		$r=$s->fetch;
		$id=$r->[0];
		$s->finish;

		# do encoding
		$i=0; $size=0; $premime="";
		while( $tmp=read($f, $premime, 60*57)) {
			$size+=$tmp;
			map {
				$query="insert into image_store values($id, $i, '$_');\n";
				$self->doQuery($query);
				$i++;
			} split(/\n/, MIME::Base64::encode($premime));
		}

		# set the image size, now that we know it...
		$query ="update album set size=$size where id=$id\n";
		$self->doQuery($query);
	};

	close(OUT);

	if($@) {
		%tmp=('QUERY', $query, 'ERRSTR', $DBI::errstr);
		$self->showTemplate("add_dbfailure.inc", %tmp);
		$self->rollback;
	} else {
		%tmp=(
			'ID' => $id,
			'QUERY' => $query
		);
		$self->showTemplate("add_success.inc", %tmp);
		$self->commit;
	}
	if($s) {
		$s->finish;
	}
	$self->setAuto(1);
}

sub addTail
{
	my $self=shift;
	my(%p, @a, @vars);

	@vars=qw(FILE_DEV FILE_INO FILE_MODE FILE_NLINK FILE_UID FILE_GID
			 FILE_RDEV FILE_SIZE FILE_ATIME FILE_MTIME FILE_TIME
			 FILE_BLKSIZE FILE_BLOCKS);

	@a=stat($ENV{'SCRIPT_FILENAME'});
	for(0..$#a) {
		$p{$vars[$_]}=$a[$_];
	}

	$p{'LAST_MODIFIED'}=localtime($p{FILE_MTIME});

	$self->showTemplate("tail.inc", %p);
}

sub myself
{
	my $self=shift;
	my($s);
	$s=$ENV{REQUEST_URI};
	$s=~s/(.*?)\?.*/$1/;
	return($s);
}

sub showTemplate
{
	my $self=shift;
	my($fn, %p)=@_;
	my($q);

	$q=CGI->new;
	map { $p{uc($_)}=$q->param($_) unless(defined($p{uc($_)}))} $q->param;
	map { $p{$_}=$ENV{$_} unless(defined($p{uc($_)})) } keys(%ENV);

	$p{'URIROOT'}=$self->{'config'}{'uriroot'};
	$p{'CGIDIR'}=$self->{'config'}{'cgidir'};
	$p{'SELF_URI'}=&myself;

	$p{'ALL_VARS'}=join("\n", sort(keys(%p)));
	$p{'STYLESHEET'}="<link rel=\"stylesheet\"href=\"".
		$self->{'config'}{'cgidir'} ."/style.cgi\">";

	open(IN, $self->{'config'}{'includes'} . $fn)
		|| die("Can't open $fn:  $!\n");
	while(<IN>) {
		s/%([A-Z0-9_]+)%/$p{$1}/g;
		print;
	}

	close(IN);
}

sub deleteImage
{
	my($self, $id)=@_;
	my($query, $s, $r, %p);

	$query ="select keywords,descr,id\n";
	$query.="	from album\n";
	$query.="	where id=$id\n";

	$s=$self->doQuery($query);

	if($r=$s->fetch) {
		($p{KEYWORDS}, $p{DESCR}, $p{ID})=@{$r};
	}

	eval {
		$query="delete from album where id=$id\n";
		$self->doQuery($query);
	};

	$self->showTemplate("admin/killimage.inc", %p);
}

sub start_html
{
	my($self, $cgi, $title)=@_;

	print "<html><head><title>$title</title>\n<head>\n".
		  "<link rel=\"stylesheet\" href=\"".
		  	$self->{'config'}{'cgidir'} ."/style.cgi\">".
		  "</head><body bgcolor=\"#fFfFfF\">";
}

sub ns_version
{
	my($self)=@_;
	my(@a);

	@a=split(/[\s\/]+/, $ENV{'HTTP_USER_AGENT'});
	if($a[0] ne "Mozilla") {
		return(0);
	}

	return($a[1]);
}

sub base64image
{
	my($self, $img)=@_;
	my($c, $key, $type, $tn);

	$type="image/jpg";

	$key="photo-image: $img";

	$tn=0;
	if($img=~/^tn./o) {
		$tn=1;
		$img=~s/^tn.//o;
	}

	$c=DCache->new;

	if(!$c->checkcache($key)) {
		if($tn) {
			$self->makeThumbnail($img, $type);
		} else {
			$self->cacheImage($key, $img, $type);
		}
	}
	return(MIME::Base64::encode($c->getcache_only($key)));
}

1;

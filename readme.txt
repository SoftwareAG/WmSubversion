This package provides support for adding version control management to
webMethods Integration Server.

Requirements:

    IS 6.5 SP2 or later
    Subversion 1.4, 1.6, and 1.7 (svn)

To set an IS, one must first do the initial import in Subversion. This must be
done prior to running the WmSubversion package. To import IS into Subversion,
run the following commands. (Note that path names and comments are more brief
than in most environments.)

    % cd /opt/webm/is65sp2/packages
    % svn import -m "Initial import." . file:///opt/svn/ispkgs

Note that because by default Subversion will import all files, one might first
want to remove files that are generated, such as .bak, .class and node.ndf
files.

    % cd ..
    % mv packages /opt/archive
    % svn checkout file:///opt/svn/ispkgs packages

Edit the server.cnf file (in the server/config directory), and set (or add) the
parameter:

    watt.server.ns.lockingMode=vcs

Edit (or add) the configuration file WmVCS/config/vcs.cnf to include:

    watt.vcs.type=svn
    watt.vcs.svn.executable=svn

The WmSubversion package relies on the program svn, which uses the environment
for its configuration parameters.

Start IS and check startup messages and errors from the WmVCS and WmSubversion
packages. To diagnose problems, one can set logging for VCS (code 132) to level
8 or 9, at which a high amount of output is produced.

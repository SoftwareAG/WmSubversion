package com.webmethods.vcs.svn;

import com.webmethods.vcs.VCSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SVNRevertCommand extends SubversionCommand {
    public SVNRevertCommand(String fileName) throws VCSException {
        this(new ArrayList(Arrays.asList(new Object[] { fileName })));
    }

    public SVNRevertCommand(Collection fileNames) throws VCSException {
        super("revert", fileNames);
    }

    //svn 1.4 does not permit username/password for add command. So override
    public boolean hasUserName() {
        return false;
    }
}

package com.webmethods.vcs.svn;

import com.webmethods.vcs.VCSException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class SVNAddCommand extends SubversionCommand {
    public SVNAddCommand(String fileName) throws VCSException {
        this(new ArrayList(Arrays.asList(new Object[] { fileName })));
    }

    public SVNAddCommand(Collection<String> fileNames) throws VCSException {
        super("add", fileNames);
        getArguments().add("--non-recursive");
    }
    
    public SVNAddCommand() {
    	super("add", flatten(new Object[] { "--non-recursive" }));
    }
    
    //svn 1.4 does not permit username/password for add command. So override
    public boolean hasUserName() {
        return false;
    }    
}

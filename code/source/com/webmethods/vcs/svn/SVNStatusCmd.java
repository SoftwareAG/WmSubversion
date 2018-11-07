package com.webmethods.vcs.svn;

import com.webmethods.vcs.VCSException;
import com.webmethods.vcs.VCSLog;
import java.util.Collection;
import java.util.List;

public class SVNStatusCmd extends SubversionCommand {    
    private StatusInfo _statusInfo;
    
    public SVNStatusCmd(String fileName){
        super("status", flatten(new Object[] { "--show-updates", "--non-recursive", fileName }));        
        run();
    }
    
    public SVNStatusCmd(Collection<String> fileNames){
        super("status", flatten(new Object[] { "--show-updates", "--non-recursive", fileNames }));
        run();
    }
    
    public SVNStatusCmd() {
        super("status", flatten(new Object[] { "--show-updates", "--non-recursive"}));
    }
    
    private void run() {
        try {
            exec();
        }
        catch (VCSException vcse) {
            // this is ok, since this might just mean that the file(s)
            // isn't/aren't in version control yet.
        }
        
        List<String> errorList = getError();
        List<String> outList = getOutput();
        _statusInfo = new StatusInfo(errorList, outList);
    }
    
    public void run(List<String> files) throws VCSException {
        try {
            super.exec(files);
        }
        catch (VCSException vcse) {
            // this is ok, since this might just mean that the file(s)
            // isn't/aren't in version control yet.
            if (vcse.getMsgId().endsWith(VCSLog.EXC_NO_USER_MAPPING)){
                throw vcse;
            }
        }
        List<String> errorList = getError();
        List<String> outList = getOutput();
        _statusInfo = new StatusInfo(errorList, outList);
    }    

    public StatusInfo getStatusInfo() {
        return _statusInfo;
    }

    /**
     * StatusCmd should capture the status info which this.run() does. Hence,
     * overriding super#exec to always initialize status info
     */    
    @Override
    public List<String> exec(List<String> files) throws VCSException {
        run(files);
        return getOutput();
    }    
}

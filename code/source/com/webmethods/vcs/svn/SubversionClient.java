package com.webmethods.vcs.svn;

import com.webmethods.vcs.AbstractClient;
import com.webmethods.vcs.VCSException;
import com.webmethods.vcs.VCSLog;
import com.wm.app.b2b.server.Server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * VCS support for Subversion. Essentially just a wrapper to the "svn" executable.
 */
public class SubversionClient extends AbstractClient {
    // see http://www.w3.org/TR/NOTE-datetime
    protected final static String SVN_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    protected final static String CLEANUP_COMMAND = "cleanup";
    protected final static String MAKE_DIR = "mkdir";
    protected final static String CHECKOUT_COMMAND = "checkout";
    
    public final static String PACKAGES_FOLDER = "packages";
    public final static String INTEGRATION_SERVER_FOLDER = "IntegrationServer";

    public SubversionClient(String name) {
    }

    /**
     * Returns "svn info"
     */
    //@Override
    public List getInfo() throws VCSException {
        return (new SubversionCommand("info")).exec();
    }

    /**
     * "svn checkout (edit)". If a file is edited but is not in Subversion, it will
     * be added.
     */
    //@Override
    public void checkout(List fileNames) throws VCSException {
        if (fileNames != null && fileNames.size() > 0) {
            VCSLog.log(VCSLog.DEBUG8, VCSLog.VCS_CHECKOUT_FROM_VCS);
    
            Collection<String> uniqFileNames = toUniqueList(fileNames);
            Collection<String> toAdd = new LinkedHashSet<String>();
            Collection<String> toCommit = new LinkedHashSet<String>();
            Collection<String> isAlreadyLocked = new LinkedHashSet<String>();
    
            SVNStatusCmd statusCmd = new SVNStatusCmd();
            statusCmd.run(new ArrayList<String>(uniqFileNames));
            StatusInfo statusInfo = statusCmd.getStatusInfo();
            for (String fileName : uniqFileNames) {
                if (statusInfo.isOutOfDate(fileName)) {
                    throw new VCSException(VCSLog.EXC_NODE_OUT_OF_DATE);
                }
                else if (statusInfo.isNotInVersionControl(fileName)) {
                    toAdd.addAll(getFilesNotInSubversion(fileName));
                }
                else if (statusInfo.isAddedOrModified(fileName)) {
                    toCommit.add(fileName);
                }
                else if (statusInfo.isLocked(fileName) || statusInfo.isUserLocked(fileName)) {
                    isAlreadyLocked.add(fileName);
                }
            }
            
            VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "entries to add: " + toAdd);
    
            Collection<String> existing = new LinkedHashSet<String>();
            for (String fileName : toAdd) {
                File file = new File(fileName);
                if (file.exists()) {
                    existing.add(fileName);
                }
            }

            toAdd = existing;
            
            if (toAdd.size() > 0) {
                toAdd = getWithSubArtifacts(toAdd);
                new SVNAddCommand().exec(new ArrayList<String>(toAdd));
            }

            toCommit.addAll(toAdd);
            
            if (toCommit.size() > 0) {
                commit(toCommit, "added");
            }
            
            // When the file exists only lock the file
            // This is because the framework passes the 
            // deleted file also in case of deleting a folder
            // having some artifact
            Collection<String> filesPresent = Util.getFilesThatExist(uniqFileNames, isAlreadyLocked);

            VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "existing files to lock: " + uniqFileNames);
            
            if (filesPresent.size() > 0) {
                new SubversionCommand("lock").exec(new ArrayList<String>(selectFiles(filesPresent)));
            }
        }
        else {
            VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "no files to check out");
        }
    }

    /**
     * Returns a list of the names that are files (i.e., are not directories).
     */
    private Collection<String> selectFiles(Collection<String> names) {
        Collection<String> files = new ArrayList<String>();
        for (String name : names) {
            if (new File(name).isFile()) {
                files.add(name);
            }
        }
        return files;
    }

    /**
     * Returns "svn filelog"
     */
    //@Override
    public List getLog(String fileName) throws VCSException {
        SubversionCommand cmd = new SubversionCommand("log", fileName);
        return cmd.exec();
    }

    /**
     * Checks the given files in, with a description.
     */
    //@Override
    public void checkin(List fileNames, String description) throws VCSException {
        commit(fileNames, description);
    }

    /**
     * Commits a list of files to Subversion.
     */
    private void commit(Collection<String> fileNames, String description) throws VCSException {
        if (fileNames != null && fileNames.size() > 0) {
            Set<String> uniqFileNames = new LinkedHashSet<String>(fileNames);

            Collection<String> toCommit = new LinkedHashSet<String>();
            Collection<String> toAdd    = new LinkedHashSet<String>();
            Collection<String> toUnlock = new LinkedHashSet<String>();

            SVNStatusCmd statusCmd = new SVNStatusCmd();
            statusCmd.run(new ArrayList<String>(uniqFileNames));
            StatusInfo statusInfo = statusCmd.getStatusInfo(); 
            for (String fileName : uniqFileNames) {
                if (statusInfo.isNotInVersionControl(fileName)) {
                    toAdd.addAll(getFilesNotInSubversion(fileName));
                }
                else if ((new File(fileName)).exists()) {
                    toCommit.add(fileName);
                }
            }

            if (toAdd.size() > 0) {
                toAdd = getWithSubArtifacts(toAdd);
                new SVNAddCommand().exec(new ArrayList<String>(toAdd));
            }
            
            toCommit.addAll(toAdd);
            
            if (toCommit.size() > 0) {
                List<String> args = new ArrayList<String>();
                // Temp file is created to save comment in various format(multiple line, special characters).
                description =  description.replaceAll("\r\n", "\n").replaceAll("\n", "\r\n");
                // replacing all \n to \r\n; if you don't do it, svn server gives you error 
                // as "svn: Inconsistent line ending style"
                File tempFile = null;
                if (description.contains("\"")) {
                    tempFile = getTempFileForComments(description);
                }
                if (tempFile == null) {
                    args.add("-m");
                    args.add(description);
                }
                else {
                    args.add("-F");
                    args.add(tempFile.getAbsolutePath());
                }

                new SubversionCommand("commit", args).exec(new ArrayList<String>(toCommit));
                if (tempFile != null) {
                    tempFile.delete();
                }
                VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_SUBMIT);

                // apparently SVN separates commit from unlocking files:

                // SVN will unlock modified files when they are committed, but
                // will not unlock unmodified files.

                // The status command is not "live"; the previous one is the
                // *old* status, and we need the post-commit status for each
                // file:

                Collection<String> t = Util.getFilesThatExist(toCommit);
                statusCmd = new SVNStatusCmd();
                statusCmd.run(new ArrayList<String>(t));
                StatusInfo statusInfoNew = statusCmd.getStatusInfo();
                for(String fileName: t){
                    if (statusInfoNew.isUserLocked(fileName)) {
                        toUnlock.add(fileName);
                    }
                }

                VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "existing files to unlock: " + toUnlock);

                if (toUnlock.size() > 0) {
                    new SubversionCommand("unlock").exec(new ArrayList<String>(toUnlock));
                }
            }
        }
        else {
            VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "no files to commit");
        }
    }
    
    private void addSubPaths(File dir, Collection<String> c) {
        File[] dirs = dir.listFiles();
        for (File subDir: dirs) {
            String absPath = subDir.getAbsolutePath(); 
            if (!(absPath.endsWith(".bak") || absPath.endsWith(".svn"))) {
                c.add(absPath);
                if (subDir.isDirectory()) {
                    addSubPaths(subDir, c);
                }
            }
        }
    }
    
    private Collection<String> getWithSubArtifacts(Collection<String> fileNames) {
        Collection<String> allFiles = new LinkedHashSet<String>();
        for (String fileName : fileNames) {
            File f = new File(fileName);
            if (!(fileName.endsWith(".bak") || fileName.endsWith(".svn"))) {
                allFiles.add(fileName);
                if (f.isDirectory()) {
                    addSubPaths(f, allFiles);
                }
            }
        }
        return allFiles;
    }

    /**
     * Loads/syncs the directories.
     *
     * @param dirNames The directory names.
     * @param spec The spec, if any.
     */
    private void doLoad(List dirNames, List fileNames, String spec) throws VCSException {
        VCSLog.log(VCSLog.DEBUG8, VCSLog.VCS_LOAD_DIRECTORIES, dirNames.toString(), spec);

        List args = toUniqueList(dirNames);
        
        args.addAll(toUniqueList(fileNames));
        
        if (spec != null) {
            args.add("-r");
            args.add(spec);
        }
        
        List<String> results = (new SubversionCommand("update", args)).exec();
        deleteAllDeletedDirs(results);
    }

    private void deleteAllDeletedDirs(List<String> results) {
        for (String result : results) {
            if (result.charAt(0) == 'D') {
                String filePath = result.substring(result.lastIndexOf(" ") + 1);
                File file = new File(filePath);
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
            }
        }
    }
    private boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for(int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }

    /**
     * Loads the files by date.
     */
    //@Override
    public void load(List dirNames, List fileNames, Date date) throws VCSException {
        String daterev = "{" + (new SimpleDateFormat(SVN_DATE_FORMAT)).format(date) + "}";
        doLoad(dirNames, fileNames, daterev);
    }

    //@Override
    public void load(List dirNames, List fileNames, String revision, boolean isInterface) throws VCSException {
        doLoad(dirNames, fileNames, revision);
    }

    /**
     * Loads the files by label (a string)
     *
     * @param dirNames The file names.
     * @param label The Subversion label.
     */
    //@Override
    public void loadByLabel(List dirNames, List fileNames, String label) throws VCSException {
        List<String> dirNames1 = dirNames;
        for (String dir : dirNames1) {
            List<String> dirTemp = new ArrayList<String>();
            dirTemp.add(dir) ;
            if (Util.isPackageIncluded(dirTemp)) {
                try {
                    int revision = Integer.parseInt(label.trim());
                    int baseRevision = getBaseRevision(dirTemp);
                    if (revision < baseRevision) {
                        throw new VCSException(VCSLog.EXC_INVALID_REVISION, new Object[]{revision});
                    }
                } 
                catch (NumberFormatException e) {
                    //Revision is not number so it can't be compared. 
                }
                break;
            }
        }
        doLoad(dirNames, fileNames, label);
    }

    private int getBaseRevision(List<String> fileName) throws VCSException {
        fileName.add("--limit");
        fileName.add("1");
        fileName.add("--revision");
        fileName.add("1:BASE");
        SubversionCommand cmd = new SubversionCommand("log", fileName);
        List<String> list = cmd.exec();
        String revision = list.get(1).substring(1, list.get(1).indexOf('|')).trim();
        return Integer.parseInt(revision);
    }

    /**
     * Loads the current version of the given files.
     */
    //@Override
    public void load(List dirNames, List fileNames) throws VCSException {
        String spec = null;
        doLoad(dirNames, fileNames, spec);
    }

    private static boolean directoryWillBeEmpty(File dir, Collection<String> filesBeingDeleted) {
        File[] contents = dir.listFiles();
        for (int i = 0; contents != null && i < contents.length; ++i) {
            File fd = contents[i];
            if (!fd.getName().equals(".svn") && 
                !fd.getName().equals("flow.xml.bak") && 
                !filesBeingDeleted.contains(fd.getPath())) {
                return false;
            }
        }

        return true;
    }
    
    // Argument for force delete
    private static final String forceDelArg = "--force";

    /**
     * Deletes the given files and commits the change.
     */
    private void del(List fileNames, String comment) throws VCSException {
        List<String> possibles = toUniqueList(fileNames);
        List<String> toDelete  = new ArrayList<String>();

        toDelete.addAll(possibles);

        // Creating parent directory set
        Set<File> dirs = new LinkedHashSet<File>();
        for (String fileName : toDelete) {
            dirs.add((new File(fileName)).getParentFile());
        }

        for (File dir : dirs) {
            File[] dirContents = dir.listFiles();

            if (directoryWillBeEmpty(dir, toDelete)) {
                for (int ci = 0; dirContents != null && ci < dirContents.length; ++ci) {
                    File fd = dirContents[ci];
                    if (fd.getName().equals("flow.xml.bak")) {
                        if (!fd.delete()) {
                            throw new VCSException(null,
                                                   "Could not delete: " + fd.getAbsolutePath(),
                                                   new Object[] { },
                                                   new ArrayList());
                        }
                    }
                }

                toDelete.add(dir.getPath());
            }
        }

        Collection<String> finalToDelete = Util.getFilesThatExist(toDelete);
        if (finalToDelete.size() > 0) {
            // Update the files before delete:
            // http://subversion.tigris.org/faq.html#wc-out-of-date
            // Especially the 2nd point mentioned here.
            new SubversionCommand("update", finalToDelete).exec();
            finalToDelete.add(forceDelArg);
            new SubversionCommand("delete", finalToDelete).exec();
            finalToDelete.remove(forceDelArg);
            List<String> args = new ArrayList<String>();
            args.addAll(finalToDelete);
            args.add("-m");
            args.add(comment);
            new SubversionCommand("commit", args).exec();
        }
    }
    
    /**
     * Deletes the given files and commits the change.
     */
    //@Override
    public void delete(boolean isPredelete, List fileNames, String comment) throws VCSException {
        List<String> validFileNames = Util.getNonSVNList(fileNames);
        final boolean isPackageIncluded = Util.isPackageIncluded(validFileNames);
        if (isPredelete) {
            if (!isPackageIncluded){ // packages should not be pre-deleted
                del(validFileNames, comment);
            }
        }
        else {
            if (isPackageIncluded) {
                // bring back the files deleted by framework:
                new SubversionCommand("update", validFileNames).exec();
                del(validFileNames, comment);
            }
        }
    }

    /**
     * Reverts the given files.
     */
    //@Override
    public void revert(List fileNames) throws VCSException {
        List uniqFileNames = toUniqueList(fileNames);
        VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "reverting: " + uniqFileNames);

        (new SVNRevertCommand(uniqFileNames)).exec();

        VCSLog.log(VCSLog.DEBUG9, VCSLog.VCS_GENERAL_MESSAGE, "unlocking: " + uniqFileNames);
        (new SubversionCommand("unlock", uniqFileNames)).exec();
    }

    /**
     * Returns whether the given file is "checked out", which here means it is
     * not open (that is, pending commit). Of course, a file that is not checked
     * out might not be checked in, either, so beware.
     */
    //@Override
    public boolean isCheckedOut(String fileName) throws VCSException {
        StatusInfo statusInfo = new SVNStatusCmd(fileName).getStatusInfo();
        return statusInfo.isLocked(fileName);
    }

    /**
     * Returns whether this client must delete existing files before loading an
     * entire directory.
     */
    //@Override
    public boolean mustDeleteBeforeLoad() throws VCSException {
        return false;
    }

    /**
     * Returns a list of the hierarchy not in Subversion. Order is top-down
     * (e.g., / before /opt before /opt/webm before /opt/webm/is65).
     */
    private List<String> getFilesNotInSubversion(String name) throws VCSException {
        List<String> files = new ArrayList<String>();

        while (name != null && name.length() > 0) {
            ArrayList<String> filenames = new ArrayList<String>();
            filenames.add(name);
            SVNStatusCmd  statusCmd = new SVNStatusCmd();
            statusCmd.run(filenames);
            StatusInfo statusInfo = statusCmd.getStatusInfo();
            if (statusInfo.isNotInVersionControl(name)) {
                files.add(0, name);
            }
            else{
                break;
            }
            name = Util.getParentName(name);
        }
        
        return files;
    }
    
    private File getTempFileForComments(String comment) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("comment", null);
            FileWriter writer = new FileWriter(tmpFile);
            writer.write(comment);
            writer.flush();
            writer.close();
        }
        catch (IOException ioe) {
            tmpFile = null;
        }
        return tmpFile;
    }
    
    /**
     * This method runs subversion clean up command on the path. 
     * @param path - path for which clean up would run. 
     * @throws VCSException
     */
    public void cleanup(String path) throws VCSException {
        List<String> pathList = new ArrayList<String>();
        pathList.add(path);
        new SubversionCommand(CLEANUP_COMMAND, pathList).exec();
    }
    
    /**
     * Create a directory subversion repository. 
     * @param comments - comments passed when creating the directory in the repository.
     * @param path - path of the directory to be created. 
     * @throws VCSException
     */
    public void makedir(String comments,String path) throws VCSException {
        List<String> pathList = new ArrayList<String>();
        pathList.add("-m");
        pathList.add(comments);
        pathList.add(path);
        new SubversionCommand(MAKE_DIR, pathList).exec();
    }
    
    /**
     * This method does initial check out the the subversion repository location in the 'packages' 
     * directory of Integration Server. It will create 'packages' directory in Integration Server 
     * installation as the working directory for subversion. The check out is not recursive.  
     * @param repoURL - is subversion repository location URL. 
     * @throws VCSException
     */
    public void initialCheckout(String repoURL) throws VCSException {
        List<String> pathList = new ArrayList<String>();
        pathList.add("-N");
        pathList.add("-r");
        pathList.add("HEAD");
        pathList.add(repoURL);
        String packagesPath = Server.getResources().getPackagesDir().getAbsolutePath();
        pathList.add(packagesPath);
        new SubversionCommand("checkout", pathList).exec();
    }
}

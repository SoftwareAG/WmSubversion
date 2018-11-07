package com.webmethods.vcs.svn;

import com.wm.app.b2b.server.Server;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class Util {
    /**
     * @param folder Folder name to check if it is a package
     * @return true if the folder is a package
     */
    private static boolean isPackage(String folder) {
        boolean isPackage;
        final File f = new File(folder);
        final File parent = f.getParentFile();
        final File packagesDir = Server.getResources().getPackagesDir();
        isPackage = parent.equals(packagesDir);
        return isPackage;
    }
    
    public static boolean isPackageIncluded(List<String> files) {
        boolean isPackage = false;
        for (String file : files) {
            return isPackage(file);
        }
        return isPackage;
    }
    
    private static final String SVN_FOLDER = File.separatorChar + ".svn";

    public static List<String> getNonSVNList(List<String> inList){
        List<String> outList = new ArrayList<String>();
        for (String file: inList){
            if (file.indexOf(SVN_FOLDER) == -1) { // file/folder is NOT a .svn special file
                outList.add(file);
            }
        }
        return outList;
    }
    
    public static Collection<String> getFilesThatExist(Collection<String> files) {
        return getFilesThatExist(files, null);
    }
    
    public static Collection<String> getFilesThatExist(Collection<String> files, Collection<String> excludes) {
        Collection<String> out = new java.util.ArrayList<String>();
        for (String file: files) {
            if (new java.io.File(file).exists()) {
                if (excludes == null) {
                    out.add(file);
                }
                else if (!excludes.contains(file)) {
                    out.add(file);
                }
            }
        }
        return out;
    }
    
    /**
     * Returns the parent as a VSS entry (folder, directory, subproject ... all
     * of these mean the same thing in VSS).
     */
    public static String getParentName(String fileName){
        int sepPos = fileName.lastIndexOf(File.separatorChar);
        if (sepPos < 0) {
            return null;
        }
        else {
            String parentName = fileName.substring(0, sepPos);
            return parentName;
        }
    }
}

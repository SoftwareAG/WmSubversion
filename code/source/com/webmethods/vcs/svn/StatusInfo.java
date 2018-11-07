package com.webmethods.vcs.svn;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusInfo {
    private static boolean isCharMatch(String status, int index, char ch){
        boolean match = status != null && index < status.length() && status.charAt(index) == ch;
        return match;
    }
    
    private Map<String, String> _statuses = new LinkedHashMap<String, String>();
    
    private static final Pattern PATTERN_OUT = Pattern.compile("^(.{8}) .*? (\\S+)$", Pattern.COMMENTS);
    private static final Pattern PATTERN_ERR = Pattern.compile("^svn:\\s*warning:\\s*(?:W155007:\\s*)?'([^']*)'\\s+is\\s+not\\s+a\\s+working\\s+copy.*$");
    
    public StatusInfo(List<String> errList, List<String> outList){
        // Process the STD ERR
        for(String str: errList){
            Matcher m = PATTERN_ERR.matcher(str);
            if (m.matches()) {
                String fileName = m.group(1);
                _statuses.put(fileName, "?       ");
            }
        }
        
        // Process the STD OUT
        for(String str: outList){
            // Check if this is the last line:
            if(str.startsWith("Status against revision:")){
                continue;
            }
            Matcher m = PATTERN_OUT.matcher(str);
            if (m.matches()) {
                String status = m.group(1);
                String fileName = m.group(2);
                _statuses.put(fileName, status);
            }
        }
    }
    
    public boolean isNotInVersionControl(final String fileName){
        String status = _statuses.get(fileName);
        if(status == null){
            return false;
        }
        else{
            return isCharMatch(status, 0, '?');
        }
    }
    
    /**
     * Checks if the file needs a commit
     * @param fileName
     * @return
     */
    public boolean isAddedOrModified(String fileName){
        String status = _statuses.get(fileName);
        if(status == null){
            return false;
        }
        else{
            return isCharMatch(status, 0, 'A') || isCharMatch(status, 0, 'M');
        }
    }
    
    public boolean isAdded(String fileName){
        String status = _statuses.get(fileName);
        return isCharMatch(status, 0, 'A');
    }
    
    public boolean isOutOfDate(final String fileName){
        String status = _statuses.get(fileName);
        if(status == null){
            return false;
        }
        else{
            return isCharMatch(status, 7, '*');
        }
    }
    
    public boolean isUserLocked(final String fileName){
        String status = _statuses.get(fileName);
        if(status == null){
            return false;
        }
        else{
            return isCharMatch(status, 5, 'K');
        }
    }
    
    public boolean isLocked(final String fileName){
        String status = _statuses.get(fileName);
        if(status == null){
            return false;
        }
        else{
            return !isCharMatch(status, 5, ' ');
        }
    }
    
    @Override
    public String toString(){
        StringBuffer sb = new StringBuffer();
        for(String fileName: _statuses.keySet()){
            sb.append(fileName).append(" : ").append(_statuses.get(fileName)).append("\n");
            sb.append("\t").append("isNotInVersionControl(): ").append(isNotInVersionControl(fileName)).append("\n");
            sb.append("\t").append("isAddedOrModified(): ").append(isAddedOrModified(fileName)).append("\n");
            sb.append("\t").append("isAdded(): ").append(isAdded(fileName)).append("\n");
            sb.append("\t").append("isOutOfDate(): ").append(isOutOfDate(fileName)).append("\n");
            sb.append("\t").append("isUserLocked(): ").append(isUserLocked(fileName)).append("\n");
            sb.append("\t").append("isLocked(): ").append(isLocked(fileName)).append("\n");
        }
        return sb.toString();
    }
}

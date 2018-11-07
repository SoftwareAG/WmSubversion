package wm.server.svn;

// -----( B2B Java Code Template v1.2
// -----( CREATED: Mon Apr 29 20:02:30 GMT+02:00 2002
// -----( ON-HOST: sal.east.webmethods.com 

// --- <<B2B-START-IMPORTS>> ---
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.webmethods.vcs.*;
import com.webmethods.vcs.svn.*;
import com.wm.app.b2b.server.Server;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.*;

// --- <<B2B-END-IMPORTS>> ---

public final class svnservices
{
	// ---( internal utility methods )---

	final static svnservices _instance = new svnservices();

	static svnservices _newInstance()
	{
		return new svnservices();
	}

	static svnservices _cast(Object o)
	{
		return (svnservices)o;
	}

	// ---( server methods )---

	public static final void configureSVN(IData pipeline) throws ServiceException
	{
		// --- <<IS-START(configureSVN)>> ---
		// @subtype unknown
		// @sigtype java 3.5
		// [i] field:0:required svnRepositoryLocation
		// pipeline

		IDataCursor pipelineCursor = pipeline.getCursor();
		String svnRepositoryLocation = IDataUtil.getString(pipelineCursor, "svnRepositoryLocation");
		String reconnectingRepository = IDataUtil.getString(pipelineCursor, "reconnectingRepository");
		VCSClient vcsclient = VCSManager.getInstance().getClient();
		SubversionClient svnClient = (SubversionClient)vcsclient;
		String svnPackagesLocation =svnRepositoryLocation + "/" + "packages";
		boolean dirCreatedInRepo = false;
		try {
			if(!Boolean.TRUE.toString().equalsIgnoreCase(reconnectingRepository)){
				svnClient.makedir("Initial Import", svnPackagesLocation);
				dirCreatedInRepo = true;
			}	
			svnClient.initialCheckout(svnPackagesLocation);
		} catch (VCSException e) {
			if(dirCreatedInRepo){
				List<String> deleteArg = new ArrayList<String>();
				deleteArg.add(svnPackagesLocation);
				try {
					svnClient.delete(false,deleteArg , "Error while configuring subversion");
				} catch (Exception e1) {
					//to nothing as exception would be thrown in next step, which would be the actual error. 
				}
			}
			throw new ServiceException(e);
		}
		String svnConfigured = Boolean.TRUE.toString();
		
		try {
			
			Map<String, String> svnParams = new HashMap<String, String>();
			svnParams.put("watt.vcs.svn.svnRepositoryLocation", svnRepositoryLocation);
			svnParams.put("watt.vcs.svn.configured", svnConfigured);
			Config.getInstance().writeProperties("WmSubversion", "subversion.cnf", svnParams);
			pipelineCursor.insertAfter("svnConfigured", svnConfigured);
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		pipelineCursor.destroy();
		
		// --- <<B2B-END>> ---
	}

	public static final void cleanup(IData pipeline) throws ServiceException
	{
		// --- <<IS-START(cleanup)>> ---
		// @subtype unknown
		// @sigtype java 3.5
		// pipeline

		VCSClient vcsclient = VCSManager.getInstance().getClient();
		SubversionClient svnClient = (SubversionClient)vcsclient;
    	String packagesPath = Server.getResources().getPackagesDir().getAbsolutePath();
		try {
			svnClient.cleanup(packagesPath);
		} catch (VCSException e) {
			throw new ServiceException(e);
		}
		// --- <<B2B-END>> ---
	}
	

	public static final void disconnect(IData pipeline) throws ServiceException
	{
		// --- <<IS-START(disconnect)>> ---
		// @subtype unknown
		// @sigtype java 3.5
		// pipeline

		IDataCursor pipelineCursor = pipeline.getCursor();
		String svnConfigured = Boolean.FALSE.toString();
		String reconnectingRepository = IDataUtil.getString(pipelineCursor, "deleteSVNFiles");
		if(Boolean.TRUE.toString().equalsIgnoreCase(reconnectingRepository)){
			File packagesDirPath = Server.getResources().getPackagesDir();
			browsePackageDir(packagesDirPath);
		}
		try {
			
			Map<String, String> svnParams = new HashMap<String, String>();
			svnParams.put("watt.vcs.svn.svnRepositoryLocation", "");
			svnParams.put("watt.vcs.svn.configured", svnConfigured);
			Config.getInstance().writeProperties("WmSubversion", "subversion.cnf", svnParams);
			pipelineCursor.insertAfter("deleteSVNFiles", reconnectingRepository);
			pipelineCursor.insertAfter("watt.vcs.svn.svnRepositoryLocation", "");
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		pipelineCursor.destroy();
		// --- <<B2B-END>> ---
	}
	
	
	// --- <<B2B-START-SHARED>> ---	

	// --- <<B2B-END-SHARED>> ---
	private static void deepDelete(File resource) {
		if (resource.isDirectory()) {
			File[] fileList = resource.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				deepDelete(fileList[i]);
			}
		}
		resource.delete();
	}
	
	private static void browsePackageDir(File file){
		if (file.isDirectory()) {
			if(file.getName().startsWith(".svn")){
				deepDelete(file);
				return;
			}
			File[] fileList = file.listFiles();
			for (int i = 0; i < fileList.length; i++) {
				browsePackageDir(fileList[i]);
			}
		}
	}

}


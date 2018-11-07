package wm.server.svn;

// -----( B2B Java Code Template v1.2
// -----( CREATED: Mon Apr 29 20:02:30 GMT+02:00 2002
// -----( ON-HOST: sal.east.webmethods.com

// --- <<B2B-START-IMPORTS>> ---
import com.webmethods.vcs.*;
import com.webmethods.vcs.svn.*;
import com.wm.app.b2b.server.ServiceException;
import com.wm.data.*;
import wm.server.vcsadmin;
// --- <<B2B-END-IMPORTS>> ---

public final class admin
{
	// ---( internal utility methods )---

	final static admin _instance = new admin();

	static admin _newInstance()
	{
		return new admin();
	}

	static admin _cast(Object o)
	{
		return (admin)o;
	}

	// ---( server methods )---

	public static final void startup(IData pipeline) throws ServiceException
	{
		// --- <<B2B-START(startup)>> ---
		// @sigtype java 3.5

        vcsadmin.clientStartup("WmSubversion",
                               new String[] { "subversion", "svn" }, 
                               new String[] { "svn.cnf",    "subversion.cnf" }, 
                               SubversionClient.class, 
                               ADMIN_SERVICES, 
                               null);

		// --- <<B2B-END>> ---
	}

	public static final void shutdown(IData pipeline) throws ServiceException
	{
		// --- <<B2B-START(shutdown)>> ---
		// @sigtype java 3.5

        vcsadmin.setEnabled(false);
            
		// --- <<B2B-END>> ---
	}

	// --- <<B2B-START-SHARED>> ---

	public static final String ADMIN_SERVICES = "wm.server.svn.admin";

	// --- <<B2B-END-SHARED>> ---

}


package com.webmethods.vcs.svn;

import com.webmethods.vcs.Command;
import com.webmethods.vcs.Config;
import com.webmethods.vcs.VCSException;
import com.webmethods.vcs.VCSLog;
import com.webmethods.vcs.util.Users;
import com.wm.app.b2b.server.InvokeState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SubversionCommand extends Command
{
    public final static String EXECUTABLE_PROPERTY_KEY = "watt.vcs.svn.executable";

    public final static List<String> NO_ARGUMENTS = null;
    
    public final static List<String> NO_INPUT = null;

    private boolean _hasMappedUser; //Defaults to false 
    
    /**
     * The executable.
     */
    private String _exec;

    /**
     * The subcommand, such as "lock", "diff", "move".
     */
    private String _subcommand;

    /**
     * Arguments to the subcommand.
     */
    private Collection<String> _args;

    /**
     * The user name.
     */
    private String _username;

    /**
     * The user's password.
     */
    private String _password;

    /**
     * The command to be logged (that is, without passwords).
     */
    private List<String> _logCmd;

    public static List flatten(Object[] objs)
    {
        List<Object> list = new ArrayList<Object>();
        for (int oi = 0; oi < objs.length; ++oi) {
            Object obj = objs[oi];
            if (obj.getClass().isArray()) {
                Object[] ary = (Object[])obj;
                list.addAll(flatten(ary));
            }
            else if (obj instanceof Collection) {
                list.addAll((Collection<Object>)obj);
            }
            else {
                list.add(obj);
            }
        }
        return list;
    }

    public SubversionCommand(String subcommand, Collection<String> args, List<String> input)
    {
        super(input);

        _subcommand = subcommand;
        _args       = args;
        _exec       = Config.getInstance().getProperty(EXECUTABLE_PROPERTY_KEY);

        if (_exec == null) {
            _exec = "svn";
        }

        InvokeState state   = InvokeState.getCurrentState();
        String      devUser = state.getUser().getName();

        if (Users.getInstance().hasUser(devUser)) {
            _username = Users.getInstance().getUser(devUser);
            _password = Users.getInstance().getPassword(devUser);
            this._hasMappedUser = true;
        }
        else {
            _username = null;
            _password = null;
            this._hasMappedUser = false;
        }
    }

    public SubversionCommand(String subcommand)
    {
        this(subcommand, NO_ARGUMENTS, NO_INPUT);
    }

    public SubversionCommand(String subcommand, Collection<String> args)
    {
        this(subcommand, args, NO_INPUT);
    }

    public SubversionCommand(String subcommand, String arg)
    {
        this(subcommand, new ArrayList<String>(), NO_INPUT);

        _args.add(arg);
    }

    public SubversionCommand(String subcommand, String arg0, String arg1)
    {
        this(subcommand, new ArrayList<String>(), NO_INPUT);

        _args.add(arg0);
        _args.add(arg1);
    }

    public Collection<String> getArguments()
    {
        return _args;
    }

    public List<String> exec() throws VCSException
    {
        List<String> cmd = getCommand();
        return exec(cmd, _logCmd, getInput(), getOutput(), getError());
    }

    public List<String> exec(List<String> files) throws VCSException {
        // Check if the IS user has a mapped vcs user before exec'ing svn comms 
        if (hasMappedUser()) {
            List<String> cmd = getCommand();
            return exec(cmd,files, _logCmd, getInput(), getOutput(), getError());
        }
        else {
            throw new VCSException(String.valueOf(VCSLog.EXC_NO_USER_MAPPING), new Object[] { InvokeState.getCurrentState().getUser().getName()});
        }
    }

    public int execute() throws VCSException
    {
        List<String> cmd = getCommand();
        return execute(cmd, _logCmd, getInput(), getOutput(), getError());
    }

    private void buildCommandList(List<String> cmdList, List<String> logCmdList, String arg, boolean isMasked)
    {
        cmdList.add(arg);
        if (logCmdList != cmdList) {
            if (isMasked) {
                logCmdList.add("********");
            }
            else {
                logCmdList.add(arg);
            }
        }
    }
    
    @Override
    public List<String> getCommand()
    {
        List<String> cmd = new ArrayList<String>();

        _logCmd = new ArrayList<String>();
            
        // the executable:
        buildCommandList(cmd, _logCmd, _exec, false);

        // svn doesn't have any options/arguments between the command and the
        // subcommand.

        // the subcommand
        buildCommandList(cmd, _logCmd, _subcommand, false);

        if (hasUserName()) {
            if (_username != null) {
                buildCommandList(cmd, _logCmd, "--username", false);
                buildCommandList(cmd, _logCmd, _username,    false);
            }
            
            if (_password != null) {
                buildCommandList(cmd, _logCmd, "--password", false);
                buildCommandList(cmd, _logCmd, _password,    true);
            }
        }

        // the subcommand arguments:
        if (_args != null) {
            for (String arg : _args) {
                buildCommandList(cmd, _logCmd, arg, false);
            }
        }

        return cmd;
    }

    /**
     * Returns whether this command takes a username and password.
     */
    public boolean hasUserName() {
        return true;
    }

    public boolean hasMappedUser(){
        return this._hasMappedUser;
    }
}

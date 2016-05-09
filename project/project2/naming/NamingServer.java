package naming;

//TODO(lmlaaron):
//1.locking mechanism refactoring: multiple shared lock using semafore? thread safe?
//2. replication management using command interface: where to implement the counter
//3. replication consistency management
//4. start/stop machinism 
//5. ".", ".." consideration

import java.io.*;
import java.net.*;
import java.util.*;

import rmi.*;
import common.*;
import storage.*;
import java.util.ArrayList;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event;
import java.io.File;
 
/**
 * This application that requires the following additional files:
 *   TreeDemoHelp.html
 *    arnold.html
 *    bloch.html
 *    chan.html
 *    jls.html
 *    swingtutorial.html
 *    tutorial.html
 *    tutorialcont.html
 *    vm.html
 */
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
 
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;



/** Naming server.

    <p>
    Each instance of the filesystem is centered on a single naming server. The
    naming server maintains the filesystem directory tree. It does not store any
    file data - this is done by separate storage servers. The primary purpose of
    the naming server is to map each file name (path) to the storage server
    which hosts the file's contents.

    <p>
    The naming server provides two interfaces, <code>Service</code> and
    <code>Registration</code>, which are accessible through RMI. Storage servers
    use the <code>Registration</code> interface to inform the naming server of
    their existence. Clients use the <code>Service</code> interface to perform
    most filesystem operations. The documentation accompanying these interfaces
    provides details on the methods supported.

    <p>
    Stubs for accessing the naming server must typically be created by directly
    specifying the remote network address. To make this possible, the client and
    registration interfaces are available at well-known ports defined in
    <code>NamingStubs</code>.
 */

enum FileType {
    FILE, DIRECTORY
}

enum LockType {
    EXCLUSIVE, SHARED, UNLOCKED
}

class StorageMachine {
    public Command command_stub;
    public Storage client_stub;

    StorageMachine(Command command_stub_, Storage client_stub_) {
      this.command_stub = command_stub_;
      this.client_stub = client_stub_;
    }
    boolean equal(StorageMachine another) {
    	return (this.command_stub == another.command_stub) && (this.client_stub == another.client_stub);
    }
}

class PathMachinePair {
    public Path path; 
    // TODO(lmlaaron):modify the machine to be an array
    public StorageMachine machine;	
    public FileType file_type;
    public LockType lock_type; 

    PathMachinePair(Path path_, FileType file_type_, StorageMachine machine_) {
	this.path = path_;
	this.machine = machine_;
	this.file_type = file_type_;
    	this.lock_type = UNLOCKED;
    }
}

public class NamingServer implements Service, Registration
{
    // register all the storage server
   private ArrayList<StorageMachine> storage_machines; 

   // a tree structure to remember all the <file, storageMachine>
   // the structure of the tree resembles the file system tree (at least at the top level)
   //private TreeNode<PathMachinePair> root;
   private JTree tree;
   private DefaultMutableTreeNode root;
   //private DefaultMutableTreeNode<PathMachinePair> root;
   
   

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
  	//throw new UnsupportedOperationException("not implemented");
        storage_machines = new ArrayList<StorageMachine>();

	//TODO(lmlaaron): replace null with "/" in path object
	root = new DefaultMutableTreeNode(new PathMachinePair(Path("/"), DIRECTORY,null))
	tree = new Jtree(root);
    }

    // need to write a function given a Path, return the <file, storageMachine> node on the tree
    private DefaultMutableTreeNode get(Path file) {
        Iterator itr = file.iterator();
        String cpath = "";
        //DefaultMutableTreeNode<PathMachinePair> ctr = this.root;
        DefaultMutableTreeNode ctr = this.root;
        
        // traverse the tree from root to the node representing the file, return the respective machine stub
        while (itr.hasNext()) {
            cpath = cpath + itr.next();	    
            for ( int i = 0; i < ctr.getChildCount(); i++ ) {
            	if (ctr.getChildAt(i).path == Path(cpath)) {
        	    break;
        	}
            }
            if ( i == ctr.getChildCount() ) {
            	//throw filenotfoundexception
                return null;
            } else {
            	ctr = ctr.getChildAt(i);
            }
        }
        return ctr;
    }

    /** Starts the naming server.

        <p>
        After this method is called, it is possible to access the client and
        registration interfaces of the naming server remotely.

        @throws RMIException If either of the two skeletons, for the client or
                             registration server interfaces, could not be
                             started. The user should not attempt to start the
                             server again if an exception occurs.
     */
    public synchronized void start() throws RMIException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Stops the naming server.

        <p>
        This method commands both the client and registration interface
        skeletons to stop. It attempts to interrupt as many of the threads that
        are executing naming server code as possible. After this method is
        called, the naming server is no longer accessible remotely. The naming
        server should not be restarted.
     */
    public void stop()
    {
        throw new UnsupportedOperationException("not implemented");
    }

    /** Indicates that the server has completely shut down.

        <p>
        This method should be overridden for error reporting and application
        exit purposes. The default implementation does nothing.

        @param cause The cause for the shutdown, or <code>null</code> if the
                     shutdown was by explicit user request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following public methods are documented in Service.java.
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException
    {
	// the semantics requires all teh files to be locked
	DefaultMutableTreeNode pm = this.get(path);
	if ( pm.LockType == UNLOCKED ) {
	    if ( exclusive == true) {
		pm.LockType == EXCLUSIVE;
	    } else if ( exclusive == false) {
	        pm.LockType == SHARED;
	    }
	} else if ( pm.LockType == SHARED) {
	    if ( exclusive == true) {
	        throw new IllegalStateException("the file is locked!");
	    } else if ( exclusive == false) {
	        pm.LockType == SHARED;
	    }
	} else if ( pm.LockType == EXCLUSIVE){
		throw new IllegalStateException("the file is locked!");
	}
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
       	// the semantics requires all the files alone the path to be locked
	DefaultMutableTreeNode pm = this.get(path);
	if ( pm.LockType == UNLOCKED ) {
	    throw new IllegalStateException("file is not locked!");
	} else if ( pm.LockType == SHARED) {
	    if (exclusive == true) {
	    	throw new IllegalStateException("file is not locked for shared access!");
	    } else if (exclusive == false) {
		pm.LockType = UNLOCKED;
	    }   
	} else if ( pm.LockType == EXCLUSIVE){
	    if (exclusive == false) {
	    	throw new IllegalStateException("file is not locked for exclusive access!");
	    } else if (exclusive == true) {
		pm.LockType = UNLOCKED;
	    }
	}
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        PathMachinePair pmp = (PathMachinePair) this.get(file).getUserObject;
	if ( pmp.FileType == DIRECTORY ) {
	    return true;
	} else {
	    return false;
	}
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
	PathMachinePair pm = this.get(directory);
	String[] ret = new String[pm.getChildCount()];
	for ( int i = 0; i < pm.getChildCount(); i++ ) {
	    PathMachinePair pmp = (PathMachinePair) pm.getChildAt(i).getUserObject();
	    ret[i] = pmp.path.toString();
	}
	return ret;
	// check if directory is valid, if not return FileNotFoundException
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
	// traverse the tree from root to the node representing the file, get the respective storage stub, use this stub to create the file on that storage server, close the stub,and return, add the node onto the tree

    	DefaultMutableTreeNode pm = this.get(file.parent());
	// public void add(MutableTreeNode newChil)

	//TODO(lmlaaron): currently use the first machine, need a policy	
	pm.add(new DefaultMutableTreeNode(new PathMachinePair(file,FILE,storage_machines[0])))
	return true;
    }



    /** Creates the given directory, if it does not exist.
        <p>
        The parent directory should be locked for exclusive access before this
        operation is performed.
        @param directory Path at which the directory is to be created.
        @return <code>true</code> if the directory is created successfully,
                <code>false</code> otherwise. The directory is not created if
                a file or directory with the given name already exists.
        @throws FileNotFoundException If the parent directory does not exist.
        @throws RMIException If the call cannot be completed due to a network
                                 error.
     */
    @Override
    public boolean createDirectory(Path directory) throws RMIException, FileNotFoundException
    {
        // traverse the tree from root to the node representing the file, get the respective storage stub, use this stub to create the file on that storage server, close the stub, adding the node to the tree
    	DefaultMutableTreeNode pm = this.get(file.parent());

	//TODO(lmlaaron): currently use the first machine, need a policy	
	pm.add(new DefaultMutableTreeNode(new PathMachinePair(file,DIRECTORY,null)))
	return true;
    }

    /** Deletes a file or directory.
    <p>
    The parent directory should be locked for exclusive access before this
    operation is performed.
    @param path Path to the file or directory to be deleted.
    @return <code>true</code> if the file or directory is deleted;
            <code>false</code> otherwise. The root directory cannot be
            deleted.
    @throws FileNotFoundException If the object or parent directory does not
                                  exist.
    @throws RMIException If the call cannot be completed due to a network
                         error.
    */
    @Override
    public boolean delete(Path path) throws RMIException, FileNotFoundException
    {
	DefaultMutableTreeNode pm = this.get(path);
	if ( pm == null ) {
	    throw new FileNotFoundException("file not found!");
	}
	PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	if (pmp.FileType == FILE ) {
	    try {
	    	this.get(path).removeFromParent();
	         // TODO(lmlaaron): for multiple replications, remove all
	         //pmp.command_stub.delete(Path path);	    
	    } catch (Throwable t) {
	         return false;
	    }
	} else if ( pmp.FileType == DIRECTORY ) {
	    for (int i = 0; i < pm.getChildCount(); i++ ) {
	        boolean isNormal = false;
	        try {
	 	  isNormal = this.delete(pm.getChildAt(i).getUserObject().Path);
	        } catch (Throwable t) {
	        	  return false;
	        }
	        if (!isNormal) {
	           return false;
	        }
	    }
	    return true; 
	}
    }


    /** Returns a stub for the storage server hosting a file.
        <p>
        If the client intends to perform calls only to <code>read</code> or
        <code>size</code> after obtaining the storage server stub, it should
        lock the file for shared access before making this call. If it intends
        to perform calls to <code>write</code>, it should lock the file for
        exclusive access.
        @param file Path to the file.
        @return A stub for communicating with the storage server.
        @throws FileNotFoundException If the file does not exist.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public Storage getStorage(Path file) throws RMIException, FileNotFoundException
    {
	DefaultMutableTreeNode pm = this.get(file);
	if ( pm == null ) {
	    throw new FileNotFoundException("file not found");
	}
	PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	return pmp.machine.client_stub;
    }

    // The method register is documented in Registration.java.
    /*
        Registration requries the naming server to lock the root directory for
        exclusive access. Therefore, it is best done when there is not heavy
        usage of the filesystem.

       @param client_stub Storage server client service stub. This will be
                          given to clients when operations need to be performed
                          on a file on the storage server.
       @param command_stub Storage server command service stub. This will be
                           used by the naming server to issue commands that
                           modify the directory tree on the storage server.
       @param files The list of files stored on the storage server. This list
                    is merged with the directory tree already present on the
                    naming server. Duplicate filenames are dropped.
       @return A list of duplicate files to delete on the local storage of the
               registering storage server.
       @throws IllegalStateException If the storage server is already
                                     registered.
       @throws NullPointerException If any of the arguments is
                                    <code>null</code>.
       @throws RMIException If the call cannot be completed due to a network
                            error.
    */
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files) throws NullPointerException, IllegalStateException, RMIException
    {
	// lock the root directory
	try {
	    this.lock(new Path("/"), true);    
	} catch (Throwable t) {
	    throw new ApplicationFailure("cannot lock root!");
	}

	
	ArrayList<String> ret = new ArrayList<>(); 
	try {
	    if ( client_stub == null || command_stub == null || files == null ) {
	        throw new NullPointerException("null pointer");
	    }
	    if ( storage_machines.contains(new StorageMachine(client_stub,command_stub))){
 	        throw new IllegalStateException("the storage server is registered!");
	    }	
            
	    // scan and compare the storage directory tree and naming server directory tree
	    this.storage_machines.add(new StorageMachine(client_stub,command_stub));
	    for ( int i = 0; i < files.length; i++ ) {
	       if( this.get(file[i]) != null ) {
                   ret.add(file[i]);
	       } else {
	           this.createFile(file[i]);
	       } 
	    }
	} catch (Throwable t) {
	    throw t;
	} finally {
	    // unlock the root directory
	    try {
	        this.lock(new Path("/"), true);    
	    } catch (Throwable t) {
	        throw new ApplicationFailure("cannot unlock root!");
	    }
            return ret;
	}
    }
}

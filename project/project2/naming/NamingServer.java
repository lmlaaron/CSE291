package naming;

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




class StorageMachine {
    public Command command_stub;
    public Storage client_stub;

    StorageMachine(Command command_stub_, Storage client_stub_) {
      this.command_stub = command_stub_;
      this.client_stub = client_stub_;
    }
}

class PathMachinePair {
    public Path path; 
    public StorageMachine machine;	

    PathMachinePair(Path path_, StorageMachine machine_) {
	this.path = path_;
	this.machine = machine_;
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
	root = new DefaultMutableTreeNode(new PathMachinePair(Path("/"),null))
	tree = new Jtree(root);
	// top.add(new DefaultMutableTreeNode)
	//root = new TreeNode<PathMachinePair>();
    }

    // need to write a function given a Path, return the <file, storageMachine> node on the tree
    private DefaultMutableTreeNode get(Path file) {
        Iterator itr = file.iterator();
        String cpath = "";
        //DefaultMutableTreeNode<PathMachinePair> ctr = this.root;
        DefaultMutableTreeNode ctr = this.root;
        
        // traverse the tree from root to the node representing the file, return the respective machine stub
        while (itr.hasNext()) {
            cpath = current_path + itr.next();	    
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
        //throw new UnsupportedOperationException("not implemented");
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
	// save for later
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public void unlock(Path path, boolean exclusive)
    {
	// save for later
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public String[] list(Path directory) throws FileNotFoundException
    {
	// check if directory is valid, if not return FileNotFoundException
	 

        //throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
	// traverse the tree from root to the node representing the file, get the respective storage stub, use this stub to create the file on that storage server, close the stub,and return, add the node onto the tree

        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean createDirectory(Path directory) throws FileNotFoundException
    {
        // traverse the tree from root to the node representing the file, get the respective storage stub, use this stub to create the file on that storage server, close the stub, adding the node to the tree
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean delete(Path path) throws FileNotFoundException
    {
	//PathMachinePair pmp = (PathMachinePair) this.get(path).getUserObject;

	// traverse the tree from the root to the node representing the file, get the respective storage machine id, asking the naming server to delete that file on behalf of the client, and remove the node on the tree
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public Storage getStorage(Path file) throws FileNotFoundException
    {
	PathMachinePair pmp = (PathMachinePair) this.get(file).getUserObject;
	return pmp.machine.client_stub;
        //throw new UnsupportedOperationException("not implemented");
    }

    // The method register is documented in Registration.java.
    @Override
    public Path[] register(Storage client_stub, Command command_stub,
                           Path[] files)
    {
        this.storage_machines.add(StorageMachine(client_stub,command_stub))
	
	//TODO(lmlaaron):
	//1. scan all the files on this storage machine
	//2. compare the files to be deleted
	//3. register the files on the naming server to be added
	//4. return the file list to be deleted
	//throw new UnsupportedOperationException("not implemented");
    }
}

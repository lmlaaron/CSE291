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
import java.io.File;
import java.util.Random;
import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
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
import java.lang.Object;
import java.awt.Component;
import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.JTree; 
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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

//enum LockType {
//    EXCLUSIVE, SHARED, UNLOCKED
//}

class StorageMachine {
    public Command command_stub;
    public Storage client_stub;

    StorageMachine(Command command_stub_, Storage client_stub_) {
      this.command_stub = command_stub_;
      this.client_stub = client_stub_;
    }
    boolean equals(StorageMachine another) {
    	return (this.command_stub == another.command_stub) && (this.client_stub == another.client_stub);
    }
}

class PathMachinePair {
    public Path path; 
    public ArrayList<StorageMachine> machine;	
    public FileType file_type;
    public FileLock file_lock;
    PathMachinePair(Path path_, FileType file_type_, StorageMachine machine_) {
	this.path = path_;
	this.machine = new ArrayList<StorageMachine>(); 
	this.machine.add(machine_);
	this.file_type = file_type_;
    	this.file_lock = new FileLock();
    }
}

class FileLock {
    // -1 means exclusive locked, +n neans shared by n locks, 0 means unlocked
    private AtomicInteger shared_lockers;
    private AtomicInteger access_counter;
    public boolean isExclusiveLocked() {
    	return (shared_lockers.intValue() == -1 );
    }
    public boolean isSharedLocked() {
    	return (shared_lockers.intValue() > 0 );
    }
    //TODO(lmlaaron): this lock needs to be changed, implementing queueing mechanism, in this signature, needs to always return true, otherwise block
    public synchronized boolean lock(boolean exclusive) {
	if (exclusive) {
	    if ( this.isSharedLocked() || this.isExclusiveLocked() ) {
	        return false;
	    } else {
	        this.shared_lockers.set(-1);
		return true;
	    }
	} else {
	    if (this.isExclusiveLocked()) {
	        return false;
	    } else {
	        this.access_counter.incrementAndGet();
		this.shared_lockers.incrementAndGet(); 
	    	return true;
	    }
	}
    }
    //TODO(lmlaaron): this unlock needs to be changed, implementing queueing mechanism
    public synchronized boolean unlock(boolean exclusive) {
	if (exclusive) {
	    if ( this.isExclusiveLocked()) {
	        this.shared_lockers.set(0);
		return true;
	    } else if ( this.isSharedLocked()){
	    	return false;
	    } else {
		 return false;
	    }
	} else {
	    if ( this.isExclusiveLocked() ) {
	    	return false;
	    } else if ( this.isSharedLocked() ) {
	    	this.shared_lockers.decrementAndGet();
	    	return true;
	    } else {
		return false;
	    }
	}
    }
    public synchronized void replicate(Path path, Storage server, Command command_stub) throws RMIException {
        if ( access_counter.intValue() == 20 ) {
	    access_counter.set(0);
	    try {
	        command_stub.copy(path, server);
	    } catch (Throwable t) {
	        throw new RMIException("cannot copy");
	    }
	}
    }
    FileLock() {
	shared_lockers = new AtomicInteger(0);
	access_counter = new AtomicInteger(0);
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
   private boolean stopped;

   private int servicePort;
   private int registrationPort;
   private Skeleton<Service> serviceSkeleton;
   private Skeleton<Registration> registrationSkeleton;

    /** Creates the naming server object.

        <p>
        The naming server is not started.
     */
    public NamingServer()
    {
        storage_machines = new ArrayList<StorageMachine>();
	root = new DefaultMutableTreeNode(new PathMachinePair(new Path("/"), FileType.DIRECTORY,null));
	tree = new JTree(root);
	servicePort = 6000;
	registrationPort = 6001;
    }

    // need to write a function given a Path, return the <file, storageMachine> node on the tree
    private DefaultMutableTreeNode get(Path file) {
        //System.out.println("HAHAHA");
        //System.out.println(file);
        //System.out.println("HAHA");
        Iterator itr = file.iterator();
        String cpath = "";
        //DefaultMutableTreeNode<PathMachinePair> ctr = this.root;
        DefaultMutableTreeNode ctr = this.root;
        
        // traverse the tree from root to the node representing the file, return the respective machine stub
        while (itr.hasNext()) {
            cpath = cpath + "/" + itr.next();
            //System.out.println(cpath);
            int i;
	    for ( i = 0; i < ctr.getChildCount(); i++ ) {
		DefaultMutableTreeNode pm_child = (DefaultMutableTreeNode) ctr.getChildAt(i);
            	PathMachinePair child = (PathMachinePair) pm_child.getUserObject();
                //System.out.println("HAHA");
                //System.out.println(child.path);
                //System.out.println("HAHAHA");
                Path cpath_obj = new Path(cpath);
		if (child.path.equals(cpath_obj)) {
        	    break;
        	}
            }
            if ( i == ctr.getChildCount() ) {
            	//throw filenotfoundexception
                return null;
            } else {
            	ctr = (DefaultMutableTreeNode) ctr.getChildAt(i);
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
	try {
	    //InetSocketAddress serviceAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), this.servicePort);
            //InetSocketAddress registrationAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), this.registrationPort);
            InetSocketAddress serviceAddress = new InetSocketAddress("localhost", this.servicePort);
            InetSocketAddress registrationAddress = new InetSocketAddress("localhost", this.registrationPort);
            
	    serviceSkeleton = new Skeleton<>(Service.class, this, serviceAddress);
            registrationSkeleton = new Skeleton<>(Registration.class, this, registrationAddress);

            serviceSkeleton.start();
            registrationSkeleton.start();
	    //throw new UnsupportedOperationException("not implemented");
	} catch (Throwable t) {
	    throw new RMIException("cannot start server!");
	}
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
        serviceSkeleton.stop();
	registrationSkeleton.stop();
	stopped(null);
	//throw new UnsupportedOperationException("not implemented");
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

    public boolean isClosed() {
    	//TODO(lmlaaron):implement the close function
    	return false;
    }

    // The following public methods are documented in Service.java.
    /**
        @param path The file or directory to be locked.
        @param exclusive If <code>true</code>, the object is to be locked for
                         exclusive access. Otherwise, it is to be locked for
                         shared access.
        @throws FileNotFoundException If the object specified by
                                      <code>path</code> cannot be found.
        @throws IllegalStateException If the object is a file, the file is
                                      being locked for write access, and a stale
                                      copy cannot be deleted from a storage
                                      server for any reason, or if the naming
                                      server has shut down and the lock attempt
                                      has been interrupted.
        @throws RMIException If the call cannot be completed due to a network
                             error. This includes server shutdown while a client
                             is waiting to obtain the lock.
     */ 
    @Override
    public void lock(Path path, boolean exclusive) throws FileNotFoundException, RMIException
    {
	// change the file read(shared)/write(exclusive) counter
   	// the semantics requires all the files to be locked
	// lock the upward object for SHARED access
	// lock the downward objects for EXCLUSIVE acccess
	DefaultMutableTreeNode pm = this.get(path);
	if ( pm == null ) {
	    throw new FileNotFoundException("file not found!");
	}
	
	if ( this.isClosed()) {
	    throw new IllegalStateException("file is locked");
	}
	
	try {
	    PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	    if (!pmp.file_lock.lock(exclusive)) {
	        //return;
		throw new Error("error");
	    }
	
	    if ( pmp.file_type == FileType.FILE && exclusive ) {
	        for ( int i = pmp.machine.size()-1; i > 0; i-- ) {
	            if (!pmp.machine.get(i).command_stub.delete(path)) {
	    	        throw new IllegalStateException("replication cannot be deleted");
	    	    }
		    //TODO(lmlaaron): check this operation only unlink the reference, not 
		    // destruct the object
		    pmp.machine.remove(i);
	        }
	        //throw new IllegalStateException("file is locked!");
	    }

	    //replication policy here
	    Random randomGenerator = new Random();
	    int random_int = randomGenerator.nextInt()%(storage_machines.size());
	    while (pmp.machine.contains(storage_machines.get(random_int)) ) {
	    	random_int = randomGenerator.nextInt();
	    }
	    pmp.file_lock.replicate(path,  pmp.machine.get(0).client_stub, this.storage_machines.get(random_int).command_stub); 	
	    ArrayList<DefaultMutableTreeNode> parents = new ArrayList<DefaultMutableTreeNode>();
	    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) pm.getParent();
	    boolean success = true;
	    while (parent != null ) {
		PathMachinePair parent_pmp = (PathMachinePair) parent.getUserObject();
	    	if ( !parent_pmp.file_lock.lock(false)) {
	        	success = false;
	    		break; 
	        }
	        parents.add(parent);
	        parent = (DefaultMutableTreeNode) parent.getParent();
	    }

	    // lock from current object upward, if failed at some point undo all the lock
	    if ( success == false ) {
	        for ( int i = parents.size() -1; i >= 0; i-- ) {
		    DefaultMutableTreeNode parent_pm = parents.get(i);
	            PathMachinePair parent_pmp = (PathMachinePair) parent_pm.getUserObject();
		    parent_pmp.file_lock.unlock(false);
	        }
	        //return;
	    	throw new Error("error");
	    }
	    
	    // for exclusive access, lock the downstream objects for exclusive access    
	    // this BFS algorithm may have performance issues, consider modifying to multithread (need to watch out for thread safety)
	    if (exclusive) {
	        ArrayList<DefaultMutableTreeNode> nodes_to_visit = new ArrayList<DefaultMutableTreeNode>();
		nodes_to_visit.add(pm);
		int cursor = 0;	
		while (cursor < nodes_to_visit.size()) {
		    DefaultMutableTreeNode currentnode = nodes_to_visit.get(cursor);
		    PathMachinePair current_pm = (PathMachinePair) currentnode.getUserObject();
	    	    if (!current_pm.file_lock.lock(true)) {
		        success = false;
			break;
		    }
		    for ( int i = 0; i < currentnode.getChildCount(); i++ ) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) currentnode.getChildAt(i);
		    	PathMachinePair pmp_child = (PathMachinePair) child.getUserObject();
			nodes_to_visit.add(child);
		    }
		    cursor++;
		}
		
		if (!success ) {
	            for ( int i = cursor - 1; i >= 0; i-- ) {
	                PathMachinePair pmp_parent = (PathMachinePair) parents.get(i).getUserObject();
			pmp_parent.file_lock.unlock(true);
	            }
	            //return;
		    throw new Error("error");
		}
	    }
	} catch (Throwable t) {
	    throw t;
	}
    }

    /** Unlocks a file or directory.
        @param path The file or directory to be unlocked.
        @param exclusive Must be <code>true</code> if the object was locked for
                         exclusive access, and <code>false</code> if it was
                         locked for shared access.
        @throws IllegalArgumentException If the object specified by
                                         <code>path</code> cannot be found. This
                                         is a client programming error, as the
                                         path must have previously been locked,
                                         and cannot be removed while it is
                                         locked.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public void unlock(Path path, boolean exclusive)
    {
       	// the semantics requires all the files alone the path to be locked
	// TODO(lmlaaron): locker queue?
	DefaultMutableTreeNode pm = this.get(path);
	if ( pm == null ) {
	    throw new IllegalStateException("illegal state");
	}

	try {
	    PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	    if (!pmp.file_lock.unlock(exclusive)) {
	        //return;
		throw new Error("error");
	    }
	    ArrayList<DefaultMutableTreeNode> parents = new ArrayList<DefaultMutableTreeNode>();
	    DefaultMutableTreeNode parent = (DefaultMutableTreeNode) pm.getParent();
	    boolean success = true;
	    while (parent != null ) {
	    	PathMachinePair parent_pmp = (PathMachinePair) parent.getUserObject();
		if ( !parent_pmp.file_lock.unlock(false)) {
	        	success = false;
	    	break; 
	        }
	        parents.add(parent);
		parent = (DefaultMutableTreeNode) parent.getParent();
	    }

	    // lock from current object upward, if failed at some point undo all the lock
	    if ( success == false ) {
	        for ( int i = parents.size() -1; i >= 0; i-- ) {
		    DefaultMutableTreeNode parent_pm = parents.get(i);
	            PathMachinePair parent_pmp =(PathMachinePair) parent_pm.getUserObject();
		    parent_pmp.file_lock.lock(false);
	        }
	        //return;
	    	throw new Error("error");
	    }
	    
	    // for exclusive access, lock the downstream objects for exclusive access    
	    //TODO(lmlaaron): need to improve the BFS algorithm for performance
	    if (exclusive) {
	        ArrayList<DefaultMutableTreeNode> nodes_to_visit = new ArrayList<DefaultMutableTreeNode>();
		nodes_to_visit.add(pm);
		int cursor = 0;	
		
		DefaultMutableTreeNode currentnode = nodes_to_visit.get(cursor);
		while (cursor < nodes_to_visit.size()) {
		    PathMachinePair current_pm = (PathMachinePair) currentnode.getUserObject();
		    if (!current_pm.file_lock.unlock(true)) {
		        success = false;
			break;
		    }
	
		    for ( int i = 0; i < currentnode.getChildCount(); i++ ) {
		    	nodes_to_visit.add((DefaultMutableTreeNode) currentnode.getChildAt(i));
		    }
		    cursor++;
		}
		if (!success ) {
	            for ( int i = cursor - 1; i >= 0; i-- ) {
	                DefaultMutableTreeNode n2v_pm = nodes_to_visit.get(i);
			PathMachinePair n2v_pmp = (PathMachinePair) n2v_pm.getUserObject();
			n2v_pmp.file_lock.lock(true);
	            }
	            //return;
		    throw new Error("error");
		}
	    }
	} catch (Throwable t) {
	    throw new IllegalStateException("the lock attempt being interrupted");
	}
    }

    /** Determines whether a path refers to a directory.
        <p>
        The parent directory should be locked for shared access before this
        operation is performed. This is to prevent the object in question from
        being deleted or re-created while this call is in progress.
        @param path The object to be checked.
        @return <code>true</code> if the object is a directory,
                <code>false</code> if it is a file.
        @throws FileNotFoundException If the object specified by
                                      <code>path</code> cannot be found.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public boolean isDirectory(Path path) throws FileNotFoundException
    {
        DefaultMutableTreeNode pm = this.get(path);
	if (pm == null ) {
	    throw new FileNotFoundException("file not found!");
	}
	PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	if ( pmp.file_type == FileType.DIRECTORY ) {
	    return true;
	} else {
	    return false;
	}
    }

    /** Lists the contents of a directory.
        <p>
        The directory should be locked for shared access before this operation
        is performed, because this operation reads the directory's child list.
        @param directory The directory to be listed.
        @return An array of the directory entries. The entries are not
                guaranteed to be in any particular order.
        @throws FileNotFoundException If the given path does not refer to a
                                      directory.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public String[] list(Path directory) throws RMIException, FileNotFoundException
    {
	// list ".", ".."
	DefaultMutableTreeNode pm = this.get(directory);
	if ( pm == null ) {
	    throw new FileNotFoundException("no such directory");
	}
	PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	
	if ( pmp.file_type != FileType.DIRECTORY ) {
	    throw new FileNotFoundException("no such directory");
	}

	//String[] ret = new String[pm.getChildCount()+2];
	String[] ret = new String[pm.getChildCount()];
	for ( int i = 0; i < pm.getChildCount(); i++ ) {
	    DefaultMutableTreeNode pm_child = (DefaultMutableTreeNode) pm.getChildAt(i);	
	    PathMachinePair pmp_child = (PathMachinePair) pm_child.getUserObject();
	    ret[i] = pmp_child.path.toString().substring(pmp_child.path.toString().lastIndexOf("/")+1, pmp_child.path.toString().length());
	}
	//ret[pm.getChildCount()] = ".";
	//ret[pm.getChildCount()+1] = "..";
        //for ( String list : ret )
        //    System.out.println(list);
	return ret;
    }

    /** Creates the given file, if it does not exist.
        <p>
        The parent directory should be locked for exclusive access before this
        operation is performed.
        @param file Path at which the file is to be created.
        @return <code>true</code> if the file is created successfully,
                <code>false</code> otherwise. The file is not created if a file
                or directory with the given name already exists.
        @throws FileNotFoundException If the parent directory does not exist.
        @throws IllegalStateException If no storage servers are connected to the
                                      naming server.
        @throws RMIException If the call cannot be completed due to a network
                             error.
     */
    @Override
    public boolean createFile(Path file)
        throws RMIException, FileNotFoundException
    {
    	DefaultMutableTreeNode pm = this.get(file);
	if ( pm != null ) {
	    return false;
	}

        String[] cpath = file.toString().split("/");
        String cpath_cat = "";
        for (int i = 0; i < cpath.length-1; i++) { // exclude the directory to be created
            cpath_cat = cpath_cat + "/" + cpath[i];
	    pm = this.get(new Path(cpath_cat));
            if ( pm == null || !this.isDirectory(new Path(cpath_cat)) ) {
	        throw new FileNotFoundException("file not found!");
	    }
        }

	//pm = this.get(file.parent());
	//if ( pm == null) {
	//    throw new FileNotFoundException("file not found!");
	//}
	if (storage_machines.size() == 0 ) {
	    throw new IllegalStateException("no available storage server");
	}
	
	//TODO(lmlaaron):Need a policy to select the storage server to save the file, currently random select	
	try {
            // when called by register, where is the file?
	    Random randomGenerator = new Random();
	    int random_int = randomGenerator.nextInt()%(this.storage_machines.size());
	    pm.add(new DefaultMutableTreeNode(new PathMachinePair(file, FileType.FILE, this.storage_machines.get(random_int) )));
            this.storage_machines.get(random_int).command_stub.create(file); 
	} catch (Throwable t) {
	    return false;
	}
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
	DefaultMutableTreeNode pm = this.get(directory);
	if ( pm != null ) {
	    return false;
	}
        String[] cpath = directory.toString().split("/");
        String cpath_cat = "";
        for (int i = 0; i < cpath.length-1; i++) { // exclude the directory to be created
            cpath_cat = cpath_cat + "/" + cpath[i];
	    pm = this.get(new Path(cpath_cat));
            if ( pm == null || !this.isDirectory(new Path(cpath_cat)) ) {
	        throw new FileNotFoundException("file not found!");
	    }
        }
	
	//for directory the storage server is null because they do not need to be saved on storage server.	
	try {
	    pm.add(new DefaultMutableTreeNode(new PathMachinePair(directory, FileType.DIRECTORY, null)));
	} catch (Throwable t) {
	    return false;
	}
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
	if (pmp.file_type == FileType.FILE ) {
	    try {
	    	this.get(path).removeFromParent();
	         // only to remove from the first server, because when acquiring exlusive locks, the replica has already been removed
	         pmp.machine.get(0).command_stub.delete(path);
		 //pmp.command_stub.delete(Path path);	    
	    } catch (Throwable t) {
	         return false;
	    }
	    return true;
	} else if ( pmp.file_type == FileType.DIRECTORY ) {
	    for (int i = 0; i < pm.getChildCount(); i++ ) {
	        boolean isNormal = false;
	        try {
		  DefaultMutableTreeNode pm_child = (DefaultMutableTreeNode) pm.getChildAt(i);
		  PathMachinePair pmp_child = (PathMachinePair) pm_child.getUserObject(); 
	 	  isNormal = this.delete(pmp_child.path);
	        } catch (Throwable t) {
	        	  return false;
	        }
	        if (!isNormal) {
	           return false;
	        }
	    }
	    return true; 
	}
	return false;
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
	if ( pm == null || this.isDirectory(file)) {
	    throw new FileNotFoundException("file not found");
	}
	PathMachinePair pmp = (PathMachinePair) pm.getUserObject();
	return pmp.machine.get(0).client_stub;
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
	if ( client_stub == null || command_stub == null || files == null ) {
	    throw new NullPointerException("null pointer!");
	}	
	    
	    
	// lock the root directory
	try {
	    //this.lock(new Path("/"), true);    
	} catch (Throwable t) {
	    //throw new ApplicationFailure("cannot lock root!");
	    //throw t;
	    //TODO(lmlaaron): according to description, the lock exception is not allowed, there should be a locking queue, thus should not throw exception
	    throw new IllegalStateException("!");
	}
	
	ArrayList<Path> ret = new ArrayList<>(); 
	try {
	    if ( client_stub == null || command_stub == null || files == null ) {
	        throw new NullPointerException("null pointer");
	    }
	    for ( StorageMachine sm : this.storage_machines) {
		// TODO(lmlaaron):some issues with the equals method, working around using hashcode
		//if ( sm.command_stub == command_stub && sm.client_stub == client_stub ) 
	    	if ( sm.command_stub.hashCode() == command_stub.hashCode() && sm.client_stub.hashCode() == client_stub.hashCode() ) {
		    //System.out.println("shot");
		    throw new IllegalStateException("the storage server is registered!");
		}
	    }

	} catch (Throwable t) {
	    throw t;
	}

	try {
    		// scan and compare the storage directory tree and naming server directory tree
	    this.storage_machines.add(new StorageMachine(command_stub, client_stub));
 		//System.out.println(files.length);
	    //for ( int i = 0; i < files.length; i++ ) {
	//	System.out.println(files[i]);
		
	   	//System.out.println(files[i].parent());
		//System.out.println(this.get(files[i].parent()));
	  //  }
    /*
    	    for ( int i = 0; i < files.length; i++ ) {
		//System.out.println(files[i]);
		
	   	System.out.println(files[i].parent());
		System.out.println(this.get(files[i].parent()));
	    }
    */

	    for ( int i = 0; i < files.length; i++ ) {
		//if( this.get(files[i]) != null && pm.file_type == FileType.FILE) {
		if( this.get(files[i]) != null && !files[i].isRoot()) {
                    //System.out.println(files[i]);
                    ret.add(files[i]);
	        } else {
                    //System.out.println("***************");
		    //System.out.println(files[i]);
                    //System.out.println("***************");
	            //for deep path, e.g., need to create the directory before create the files
		    ArrayList<Path> parents = new ArrayList<Path>();
		    Path parent = files[i].parent();
                    //System.out.println(parent);
		    while (this.get(parent) == null && !parent.isRoot()) {
            //        System.out.println(parent);
		        parents.add(parent);	
//                    System.out.println("HAHA");
		        parent = parent.parent();
		    }
		    for ( int j = parents.size() - 1; j >= 0; j-- ) {
                        //System.out.println(parents.get(j));
		        this.createDirectory(parents.get(j));		
		        //System.out.println("Directory " + parents.get(j).toString() + " created.");
		    }
		    //this.createFile(files[i]);
		    //migrate
    	            DefaultMutableTreeNode pm = this.get(files[i]);
	            if ( pm != null ) {
			continue;
	            }

                    String[] cpath = files[i].toString().split("/");
                    String cpath_cat = "";
                    for (int j = 0; j < cpath.length-1; j++) { // exclude the directory to be created
                        cpath_cat = cpath_cat + "/" + cpath[j];
	                pm = this.get(new Path(cpath_cat));
                        if ( pm == null || !this.isDirectory(new Path(cpath_cat)) ) {
	                    throw new FileNotFoundException("file not found!");
	                }
                    }

                    if (storage_machines.size() == 0 ) {
	                throw new IllegalStateException("no available storage server");
	            }
	
            //this.storage_machines.get(random_int).command_stub.create(file); 
                   //command_stub.delete(files[i]);
		   //System.out.println("File " + files[i].toString() + " created.");
                   //System.out.println("================");
	            pm.add(new DefaultMutableTreeNode(new PathMachinePair(files[i], FileType.FILE, new StorageMachine(command_stub, client_stub))));
	        }
            }
	} catch (Throwable t) {
            //System.out.println(t.getClass().getName());
	    throw t;
	} finally {
	     //unlock the root directory
	    try {
	        //this.unlock(new Path("/"), true);    
	    } catch (Throwable t) {
	        //throw new ApplicationFailure("cannot unlock root!");
	    	//TODO(lmlaaron): according to description, the lock exception is not allowed, there should be a locking queue, thus should not throw exception
	    	throw new IllegalStateException("!");
	        //throw t;
	    }
	    Path[] r = new Path[ret.size()];
            r = ret.toArray(r);
            //for (Path p : r)
            //    System.out.println(p);
	    return r;
	}
    }
}

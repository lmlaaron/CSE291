package rmi;

import java.net.*;
import java.lang.reflect.*;
import java.lang.Object;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.io.*;
import java.io.Serializable;
import java.net.InetSocketAddress;
//import java.lang.reflect.Proxy.ProxyFactory.newProxyInstance; 
import java.lang.reflect.Proxy;

/** RMI skeleton

    <p>
    A skeleton encapsulates a multithreaded TCP server. The server's clients are
    intended to be RMI stubs created using the <code>Stub</code> class.

    <p>
    The skeleton class is parametrized by a type variable. This type variable
    should be instantiated with an interface. The skeleton will accept from the
    stub requests for calls to the methods of this interface. It will then
    forward those requests to an object. The object is specified when the
    skeleton is constructed, and must implement the remote interface. Each
    method in the interface should be marked as throwing
    <code>RMIException</code>, in addition to any other exceptions that the user
    desires.

    <p>
    Exceptions may occur at the top level in the listening and service threads.
    The skeleton's response to these exceptions can be customized by deriving
    a class from <code>Skeleton</code> and overriding <code>listen_error</code>
    or <code>service_error</code>.
*/
public class Skeleton<T>
{
    private Class<T> c;
    private T server;
    private String hostname;
    private int port;
    private boolean isStopped;
    public ServerSocket serverSocket;
    private Thread listen_thread;
    private Listener listener;
    public String hostname() {
    	return hostname;
    }
    public int port() {
        return port;
    }
    public boolean isStopped() {
    	return isStopped;
    }
    public ServerSocket serverSocket() {
    	return serverSocket;
    }
    public T server() {
    	return server;
    }
    /** Creates a <code>Skeleton</code> with no initial server address. The
        address will be determined by the system when <code>start</code> is
        called. Equivalent to using <code>Skeleton(null)</code>.

        <p>
        This constructor is for skeletons that will not be used for
        bootstrapping RMI - those that therefore do not require a well-known
        port.

        @param c An object representing tghe class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server)
    {
       	if (c!= null) {
	    if (!c.isInterface()) {
	     	throw new Error("not interface type!");
	    }
    	} else {
	   throw new NullPointerException("null");	
	}

	// for all methods in the interface, check whether they throw RMIException
	// If one of them does not throw RMIException, throw an Error
	Method[] allmethods = c.getDeclaredMethods();
	int rmi_ex = 0;
	for ( int i = 0; i < allmethods.length; i++ ) {
	    Class<?>[] all_ex = allmethods[i].getExceptionTypes();
	    rmi_ex = 0;
	    for ( int j = 0; j < all_ex.length; j++ ) {
	        if ( all_ex[j] == RMIException.class ) {
	    	rmi_ex = 1;
	    	break;	
	        }
	    }
	    if ( rmi_ex == 0 ) {
	        break;
	    }
	}
	if ( rmi_ex == 0 ) {
	    throw new Error("Error!");
	}
	    
	if ( c == null || server == null ) {
	    throw new NullPointerException("NullPointerException");
	}
	    
	this.c = c;
	this.server = server;
	this.isStopped = true;
	this.hostname = "wildcard";
   }

    /** Creates a <code>Skeleton</code> with the given initial server address.

        <p>
        This constructor should be used when the port number is significant.

        @param c An object representing the class of the interface for which the
                 skeleton server is to handle method call requests.
        @param server An object implementing said interface. Requests for method
                      calls are forwarded by the skeleton to this object.
        @param address The address at which the skeleton is to run. If
                       <code>null</code>, the address will be chosen by the
                       system when <code>start</code> is called.
        @throws Error If <code>c</code> does not represent a remote interface -
                      an interface whose methods are all marked as throwing
                      <code>RMIException</code>.
        @throws NullPointerException If either of <code>c</code> or
                                     <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address)
    {
	if ( c!= null ) {	
	    if (!c.isInterface() ) {
	     	throw new Error("not interface type!");
	    }    
	} else {
	    throw new NullPointerException("null");
	}
	Method[] allmethods = c.getDeclaredMethods();
	int rmi_ex = 0;
	for ( int i = 0; i < allmethods.length; i++ ) {
	    Class<?>[] all_ex = allmethods[i].getExceptionTypes();
	    rmi_ex = 0;
	    for ( int j = 0; j < all_ex.length; j++ ) {
	        if ( all_ex[j] == RMIException.class ) {
	            rmi_ex = 1;
	    	    break;	
	        }
	    }
	    if ( rmi_ex == 0 ) {
	        break;
	    }
	}
	if ( allmethods.length != 0 && rmi_ex == 0 ) {
	    throw new Error("Error!");
	}
	    
	if ( c == null || server == null ) {
	    throw new NullPointerException("NullPointerException");
	}
	
	if (!c.isInterface() ) {
	    throw new Error("not interface type!");
	}    
	this.c = c;
	this.server = server;
	this.isStopped = true;
	if ( address != null ) {
	    this.hostname = address.getHostName();
	    this.port = address.getPort();
	}
    }

    /** Called when the listening thread exits.

        <p>
        The listening thread may exit due to a top-level exception, or due to a
        call to <code>stop</code>.

        <p>
        When this method is called, the calling thread owns the lock on the
        <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
        calling <code>start</code> or <code>stop</code> from different threads
        during this call.

        <p>
        The default implementation does nothing.

        @param cause The exception that stopped the skeleton, or
                     <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause)
    {
        //this.isStopped = true;	    
    }

    /** Called when an exception occurs at the top level in the listening
        thread.

        <p>
        The intent of this method is to allow the user to report exceptions in
        the listening thread to another thread, by a mechanism of the user's
        choosing. The user may also ignore the exceptions. The default
        implementation simply stops the server. The user should not use this
        method to stop the skeleton. The exception will again be provided as the
        argument to <code>stopped</code>, which will be called later.

        @param exception The exception that occurred.
        @return <code>true</code> if the server is to resume accepting
                connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception)
    {
        return false;
    }

    /** Called when an exception occurs at the top level in a service thread.

        <p>
        The default implementation does nothing.

        @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception)
    {
    }

    /** Starts the skeleton server.

        <p>
        A thread is created to listen for connection requests, and the method
        returns immediately. Additional threads are created when connections are
        accepted. The network address used for the server is determined by which
        constructor was used to create the <code>Skeleton</code> object.

        @throws RMIException When the listening socket cannot be created or
                             bound, when the listening thread cannot be created,
                             or when the server has already been started and has
                             not since stopped.
     */
    public synchronized void start() throws RMIException
    {
	if ( this.hostname == "wildcard" ) {
	    try {
	    	this.hostname =InetAddress.getLocalHost().getHostAddress();
	    } catch (UnknownHostException e) {
	    	throw new RMIException("cannot resolve host");
	    }
	};


    if (!this.isStopped() ) {
	    throw new RMIException("already started!");
	}

	this.isStopped = false;	
	try {
  	    this.serverSocket = new ServerSocket(this.port);
		if (this.port == 0)
		{
			this.port = this.serverSocket.getLocalPort();
		}
	    this.listener = new Listener(this, this.serverSocket, this.c);
            this.listen_thread = new Thread(this.listener);
	    this.listen_thread.start();
	    return;        
	} catch (IOException e ) {
	    throw new RMIException("listening port cannot be created!");	
	}
       
   }
    /** Stops the skeleton server, if it is already running.

        <p>
        The listening thread terminates. Threads created to service connections
        may continue running until their invocations of the <code>service</code>
        method return. The server stops at some later time; the method
        <code>stopped</code> is called at that point. The server may then be
        restarted.
     */
    public synchronized void stop()
    {	
	if ( !this.isStopped() ) {	
	}
	if ( this.listen_thread != null && this.listen_thread.isAlive() ) {
	    this.listen_thread.stop();
            try {
	        this.serverSocket.close();
            } catch ( Exception e ) {
            }
	}
	this.isStopped = true;
	this.stopped(null);
	return;
    }
}


// the listener thread listens the socket requests
class Listener<T> implements Runnable {
    private Skeleton<T> skeleton;
    private ServerSocket serverSocket;
    private Class<?> c;
	    
    Listener(Skeleton<T> skeleton, ServerSocket serverSocket, Class<?> c) {
    	this.skeleton = skeleton;
	this.serverSocket = serverSocket;
	this.c = c;
    }

    public void run() {
        while (! this.skeleton.isStopped() ) {
 	    try {
 	        if ( ! this.skeleton.isStopped() ) {
 	    	    Socket socket = this.serverSocket.accept();
		    new Thread( new ClientWorker(socket, this.skeleton.server(), this.skeleton, this.c)).start();
	    	} else {
		    return;
		}
	     } catch ( Exception e) {
 	        if(this.skeleton.isStopped()) {
                     return;
                 }
 	    }
	}
	return;
    }
    public void stop() {
    	if ( this.serverSocket != null ) {
	    try {
	        this.serverSocket.close();
	    } catch (IOException e) {
	    }
	}
    }
}


// The thread is trigged if a socket request is accepted
// The thread receives the signature of the called method
class ClientWorker<T> implements Runnable {
    private Socket socket;
    private T server;
    private Skeleton<T> skeleton; 
    private Class<?> c;
    
    ClientWorker(Socket socket, T server, Skeleton<T> skeleton, Class<?> c) {
        this.socket = socket;
        this.server = server;
        this.skeleton = skeleton;
        this.c = c;
    }

    public void run() {
        try {
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Class<?> return_class;
            String method_name;
            Integer method_argc;
            try { 
                method_name = (String) in.readObject();
           	method_argc = (Integer) in.readObject();
            } catch ( ClassNotFoundException e ) {
       	        throw e;
            }
            Class<?>[] classes = new Class<?>[method_argc];
            Object[] args = new Object[method_argc];
            try { 
		classes = (Class<?>[])in.readObject();		
		args = (Object[])in.readObject();
            } catch ( ClassNotFoundException e ) {
                  throw e;
            }
            Method method;
            int exceptionNum = -1;
	    // Take care that this.server.class and this.c can be different
	    // this.server implements a superset of c
            try {
                method = this.c.getMethod(method_name, classes);
            } catch ( NoSuchMethodException e ) {
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); 
                out.writeObject(-2);
                out.flush();
                socket.close();
        	throw e;
	    }
	    if(!method.isAccessible()) {
  	        method.setAccessible(true);
	     }

            Class<?>[] exceptions = method.getExceptionTypes();
            Object return_obj;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); 
            try {
                return_obj = method.invoke(this.server, args);
                out.writeObject(-1);
                out.writeObject(return_obj);
            } catch ( InvocationTargetException e ) {
		Throwable ex = e.getCause();
                out.writeObject(1);
                out.writeObject(ex);
	    }
            out.flush();
            socket.close();
        } catch (IOException e ) {
	    RMIException ex_rmi = new RMIException("Service Error!");
	    this.skeleton.service_error(ex_rmi);
        } catch (ClassNotFoundException e) {
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
	}
    }    
}

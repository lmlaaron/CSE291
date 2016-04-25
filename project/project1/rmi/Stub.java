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

class MyInvocationHandler  implements InvocationHandler{
    private InetSocketAddress address;
    //private String hostname;
    //public String getHostName() {
    //	return this.hostname;
    //}
    public InetSocketAddress getInetSocketAddress() {
        return this.address;
    }
    private Class<?> remoteInterface;
    public Class<?> getInterface() {
    	return remoteInterface;
    }
    public MyInvocationHandler(InetSocketAddress address, Class<?> remoteInterface) {
    	this.address = address;
	this.remoteInterface = remoteInterface;
    } 
    //public MyInvocationHandler(String hostname) {
    //	this.hostname = hostname;
    //} 

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Object return_obj;
        try{
		String method_name = m.getName();
		Class<?>[] classes = m.getParameterTypes();
		Class<?>[] exceptions = m.getExceptionTypes();

		// handle equals separately
		if ( m.getName() == "equals" ) {
		    if ( args.length != 1 ) {
		        throw new Error("equal signature mismatch");
		    }
		    if (this == null && args[0] == null ) {
		    	return true;
		    } else if ( this == null || args[0] == null ) {
		    	return false;
		    } else {
		        MyInvocationHandler mih = this;
		        InetSocketAddress maddr = mih.getInetSocketAddress();
		        MyInvocationHandler oih = (MyInvocationHandler) Proxy.getInvocationHandler(args[0]);
		        InetSocketAddress oaddr = oih.getInetSocketAddress();
		        return ((mih.getInterface() == oih.getInterface()) && (maddr.equals(oaddr)));
		    }
		} else if ( m.getName() =="hashCode") {
		    MyInvocationHandler mih = this;
		    InetSocketAddress addr = mih.getInetSocketAddress();
	            final int prime = 31;
	            int ret = 0;
	            Class<?> c = mih.getInterface();
	            String ret_str = ( addr.toString() + c.toString() );
	            return ret = ret_str.hashCode();
		} else if ( m.getName() == "toString") {
	            MyInvocationHandler mih = this;
		    InetSocketAddress addr = mih.getInetSocketAddress();
		    String ret = "Name of RemoteInterface: " + mih.getInterface() + " remote address: " + addr.getHostName() + "; " + addr.getPort(); 
	            return ret;
		}
		int method_argc = classes.length;
		int method_exc = exceptions.length; 
		Class<?> return_class = m.getReturnType();
		Constructor<?> cons = return_class.getConstructor();
		return_obj = cons.newInstance();
		//args
		
		Socket socket = new Socket(address.getHostName(), address.getPort());
		// Connect Exception 
		
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
		out.writeObject(method_name);
		out.writeObject(method_argc);
		
		for ( int i = 0; i < method_argc; i++ ) {
		    out.writeObject(classes[i]);
                    out.writeObject(args[i]);
		}

		//for ( int i = 0; i < classes.length; i++ ) {
		//    out.writeObject(classes[i]);
		//}

		// need to serialize the argument
	        //for ( int i = 0; i < args.length; i++ ) {
		//    out.writeObject(args[i]);
		//}

		//out.writeObject(method_exc);
		//for ( int i =0; i < exceptions.length; i++ ) {
		//    out.writeObject(exceptions[i]);
		//}

		out.writeObject(return_class);
		
		out.flush();
		//out.close();
		
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		
		int exceptionNum = (int) in.readObject(); 
		if ( exceptionNum == -1 ) {
		    return_obj = return_class.cast(in.readObject());
		    //return_obj = in.readObject();
		} else {
		    return_obj = exceptions[exceptionNum].cast(in.readObject());
		    //return_obj = in.readObject();
		}
		//in.close();

	    } catch (InvocationTargetException e) {
	        throw e;
	    } catch (IOException e) {
	        throw new RMIException("Fail to invoke a remote call");
	    } catch (Exception e) {
		throw e;
	    } 
        return return_obj;
    }
}


/** RMI stub factory.

    <p>
    RMI stubs hide network communication with the remote server and provide a
    simple object-like interface to their users. This class provides methods for
    creating stub objects dynamically, when given pre-defined interfaces.

    <p>
    The network address of the remote server is set when a stub is created, and
    may not be modified afterwards. Two stubs are equal if they implement the
    same interface and carry the same remote server address - and would
    therefore connect to the same skeleton. Stubs are serializable.
 */





public abstract class Stub
{
    /** Creates a stub, given a skeleton with an assigned adress.

        <p>
        The stub is assigned the address of the skeleton. The skeleton must
        either have been created with a fixed address, or else it must have
        already been started.

        <p>
        This method should be used when the stub is created together with the
        skeleton. The stub may then be transmitted over the network to enable
        communication with the skeleton.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose network address is to be used.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned an
                                      address by the user and has not yet been
                                      started.
        @throws UnknownHostException When the skeleton address is a wildcard and
                                     a port is assigned, but no address can be
                                     found for the local host.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
        throws UnknownHostException
    {
	final int TIMEOUT_MILLIS = 10000;
	
 	//throw new UnsupportedOperationException("not implemented");
	if ( c == null || skeleton == null ) {
	    throw new NullPointerException("null pointer!");
	}
	if ( skeleton.hostname() == "wildcard" || skeleton.port() == 0 ) {
 	    throw new IllegalStateException("IllegalStateException!");
	}    
	
	// currently assume if the server cannot be connected, then it is not started 
	try {
	    Socket soc = new Socket();
	    soc.connect( new InetSocketAddress(skeleton.hostname(), skeleton.port()), TIMEOUT_MILLIS);
	    soc.close();
	} catch ( IOException e ) {
		throw new IllegalStateException("IllegalStateException!");
	}
		    
	if ( skeleton.hostname() == null && skeleton.port() != 0 ) {
	    try {
	        Socket soc = new Socket();
		soc.connect( new InetSocketAddress("localhost", skeleton.port()), TIMEOUT_MILLIS);
   		soc.close();
	    } catch ( IOException e ) {
		throw new UnknownHostException("UnknownHostException!");
	    }
   	}

	MyInvocationHandler h = new MyInvocationHandler(new InetSocketAddress( skeleton.hostname(), skeleton.port() ), c);
	ClassLoader cl = c.getClassLoader();
	T stub = (T) Proxy.newProxyInstance( cl, new java.lang.Class[] { c }, h);

	Method[] allmethods = stub.getClass().getDeclaredMethods();
	int rmi_ex = 0;
	int i = 0;
	//String em ="";
	//em = em + allmethods.length;
	//em = em + RMIException.class.getName();
	for ( i = 0; i < allmethods.length; i++ ) {
	    Class<?>[] all_ex = allmethods[i].getExceptionTypes();
	    rmi_ex = 0;

	    if ( allmethods[i].getName() == "equals" ||  allmethods[i].getName() == "toString" ||  allmethods[i].getName() == "hashCode") {
	    	rmi_ex = 1;
		continue;
	    } 
	    //em = em + all_ex.length;
	    for ( int j = 0; j < all_ex.length; j++ ) {
		//em = em +" " + all_ex[j].getName() + " " ;
	        if ( all_ex[j] == RMIException.class ) {
	    	    rmi_ex = 1;
	    	    break;	
	        }
	    }
	    if ( rmi_ex == 0 ) {
	        break;
		//throw new Error("Error!"+allmethods[i].getName());
	    }
	}
	if ( rmi_ex == 0 ) {
	    //System.out.println(i);
	    throw new Error("Error!" + allmethods.length+" "+allmethods[1].getParameterTypes().length +" "+allmethods[1].getName() +" "+allmethods[2].getName() +" "+allmethods[3].getName() +" "+allmethods[4].getName() + " " +RMIException.class.getName());
	    // throw new Error("Error!" + allmethods.length+" "+allmethods[0].getExceptionTypes().length +" "+allmethods[1].getExceptionTypes().length +" "+allmethods[2].getExceptionTypes().length +" "+allmethods[3].getExceptionTypes().length +" "+allmethods[4].getExceptionTypes().length + " " +RMIException.class.getName());
	    
	    //throw new Error("Error!" + em);
	
	}

	return stub;
    }

    /** Creates a stub, given a skeleton with an assigned address and a hostname
        which overrides the skeleton's hostname.

        <p>
        The stub is assigned the port of the skeleton and the given hostname.
        The skeleton must either have been started with a fixed port, or else
        it must have been started to receive a system-assigned port, for this
        method to succeed.

        <p>
        This method should be used when the stub is created together with the
        skeleton, but firewalls or private networks prevent the system from
        automatically assigning a valid externally-routable address to the
        skeleton. In this case, the creator of the stub has the option of
        obtaining an externally-routable address by other means, and specifying
        this hostname to this method.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param skeleton The skeleton whose port is to be used.
        @param hostname The hostname with which the stub will be created.
        @return The stub created.
        @throws IllegalStateException If the skeleton has not been assigned a
                                      port.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname)
    {
        //throw new UnsupportedOperationException("not implemented");
	
	if ( c == null || skeleton == null || hostname == null ) {
	    throw new NullPointerException("null pointer!");
	}
	if ( skeleton.port() == 0 ) {
 	    throw new IllegalStateException("IllegalStateException!");
	}
	
	MyInvocationHandler h = new MyInvocationHandler(new InetSocketAddress( hostname, skeleton.port()), c);
	ClassLoader cl = c.getClassLoader();
	T stub = (T) Proxy.newProxyInstance( cl, new java.lang.Class[] { c }, h);
	
	Method[] allmethods = stub.getClass().getDeclaredMethods();
	
	int rmi_ex = 0;
	for ( int i = 0; i < allmethods.length; i++ ) {
	    rmi_ex = 0;
	    if ( allmethods[i].getName() == "equals" ||  allmethods[i].getName() == "toString" ||  allmethods[i].getName() == "hashCode") {
	    	rmi_ex = 1;
		continue;
	    }
	    Class<?>[] all_ex = allmethods[i].getExceptionTypes();
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

	return stub;
    }

    /** Creates a stub, given the address of a remote server.

        <p>
        This method should be used primarily when bootstrapping RMI. In this
        case, the server is already running on a remote host but there is
        not necessarily a direct way to obtain an associated stub.

        @param c A <code>Class</code> object representing the interface
                 implemented by the remote object.
        @param address The network address of the remote skeleton.
        @return The stub created.
        @throws NullPointerException If any argument is <code>null</code>.
        @throws Error If <code>c</code> does not represent a remote interface
                      - an interface in which each method is marked as throwing
                      <code>RMIException</code>, or if an object implementing
                      this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address)
    {
	if ( c == null || address == null ) {
		throw new NullPointerException("null pointer!");
	}
	
	MyInvocationHandler h = new MyInvocationHandler(address, c);
	ClassLoader cl = c.getClassLoader();
	try {
	    T stub = (T) Proxy.newProxyInstance( cl, new java.lang.Class[] { c }, h);
	    Method[] allmethods = stub.getClass().getDeclaredMethods();
	    
	    int rmi_ex = 0;
	    for ( int i = 0; i < allmethods.length; i++ ) {
	    	Class<?>[] all_ex = allmethods[i].getExceptionTypes();
	        rmi_ex = 0;
	        
		if ( allmethods[i].getName() == "equals" ||  allmethods[i].getName() == "toString" || allmethods[i].getName() == "hashCode") {
	    	    rmi_ex = 1;
		    continue;
	        }
		
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
	    return stub;
	} catch ( Error err) {
	    throw err;
	} catch ( Exception e) {
	    throw new Error("error");
    	    
	}
    }
   

    // the bottom code is misplaced, should be in the involcationhandler
    /*
    public boolean equals(Object other) {
	MyInvocationHandler mih = (MyInvocationHandler) Proxy.getInvocationHandler(this);
	InetSocketAddress maddr = mih.getInetSocketAddress();
	MyInvocationHandler oih = (MyInvocationHandler) Proxy.getInvocationHandler(other);
	InetSocketAddress oaddr = oih.getInetSocketAddress();

	return ((mih.getInterface() == oih.getInterface()) && maddr == oaddr );
    }
    
    public int hashCode() {
	final int prime = 31;
	int ret = 0;
	MyInvocationHandler mih = (MyInvocationHandler) Proxy.getInvocationHandler(this);
	InetSocketAddress addr = mih.getInetSocketAddress();
	Class<?> c = mih.getInterface();
	String ret_str = ( addr.toString() + c.toString() );
	return ret = ret_str.hashCode();
    }

    public String toString() {
	MyInvocationHandler mih = (MyInvocationHandler) Proxy.getInvocationHandler(this);
	InetSocketAddress addr = mih.getInetSocketAddress();

	String ret = "Name of RemoteInterface: " + mih.getInterface() + " remote address: " + addr.getHostName() + "; " + addr.getPort(); 
	return ret;
    }
    */
}

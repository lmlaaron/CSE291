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
    public MyInvocationHandler(InetSocketAddress address) {
    	this.address = address;
    } 

    @Override
    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        Object return_obj;
        try{
		String method_name = m.getName();
		Class<?>[] classes = m.getParameterTypes();
		Class<?> return_class = m.getReturnType();
		Constructor<?> cons = return_class.getConstructor();
		return_obj = cons.newInstance();
		//args
		
		Socket socket = new Socket(address.getHostName(), address.getPort());
		// Connect Exception 
		
		ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); 

		out.writeObject(method_name);
		for ( int i = 0; i < classes.length; i++ ) {
		    out.writeObject(classes[i]);
		}

		// need to serialize the argument
	        for ( int i = 0; i < args.length; i++ ) {
		    out.writeObject(args[i]);
		}
		out.writeObject(return_class);
		out.flush();
		out.close();
		ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
		//return_obj = (Object) in.readObject();
		return_obj = return_class.cast(in.readObject());
		in.close();

		//Class<return_class> ret =Class<retrun_class> in.readObject();	

		//return return_obj;
		//args
		// 1.serilize
		//m.getParameterTypes
		//m.getReturnType
		// 2.tcp to server
		//==============
		// server deserialize to get m
		// m.invoke
		// server get the return value
		//==============
		
		// 3.tcp returned from server
		// 4. return the object 

		// doing nothing but m.invoke(obj, args);

        	//result = m.invoke(obj, args);
	    } catch (InvocationTargetException e) {
	        throw e;
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
        throw new UnsupportedOperationException("not implemented");
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
        throw new UnsupportedOperationException("not implemented");
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
	// throw new RMIException
	// throw new RMIException
	
	MyInvocationHandler h = new MyInvocationHandler(address);
	ClassLoader cl = c.getClassLoader();
	T stub = (T) Proxy.newProxyInstance( cl, new java.lang.Class[] { c }, h);
	return stub;
    }
}

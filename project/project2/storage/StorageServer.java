package storage;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import common.*;
import rmi.*;
import naming.*;

import static javafx.scene.input.KeyCode.T;

/** Storage server.

    <p>
    Storage servers respond to client file access requests. The files accessible
    through a storage server are those accessible under a given directory of the
    local filesystem.
 */
public class StorageServer implements Storage, Command
{
    ExecutorService executor;
    private File root;
    private int clientPort;
    private int commandPort;
    private Skeleton<Storage> clientSkeleton;
    private Skeleton<Command> commandSkeleton;

    /** Creates a storage server, given a directory on the local filesystem, and
        ports to use for the client and command interfaces.

        <p>
        The ports may have to be specified if the storage server is running
        behind a firewall, and specific ports are open.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @param client_port Port to use for the client interface, or zero if the
                           system should decide the port.
        @param command_port Port to use for the command interface, or zero if
                            the system should decide the port.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
    */
    public StorageServer(File root, int client_port, int command_port)
    {
        this.root = root;
        this.clientPort = client_port;
        this.commandPort = command_port;
    }

    /** Creats a storage server, given a directory on the local filesystem.

        <p>
        This constructor is equivalent to
        <code>StorageServer(root, 0, 0)</code>. The system picks the ports on
        which the interfaces are made available.

        @param root Directory on the local filesystem. The contents of this
                    directory will be accessible through the storage server.
        @throws NullPointerException If <code>root</code> is <code>null</code>.
     */
    public StorageServer(File root)
    {
        this(root, 0, 0);
    }

    /** Starts the storage server and registers it with the given naming
        server.

        @param hostname The externally-routable hostname of the local host on
                        which the storage server is running. This is used to
                        ensure that the stub which is provided to the naming
                        server by the <code>start</code> method carries the
                        externally visible hostname or address of this storage
                        server.
        @param naming_server Remote interface for the naming server with which
                             the storage server is to register.
        @throws UnknownHostException If a stub cannot be created for the storage
                                     server because a valid address has not been
                                     assigned.
        @throws FileNotFoundException If the directory with which the server was
                                      created does not exist or is in fact a
                                      file.
        @throws RMIException If the storage server cannot be started, or if it
                             cannot be registered.
     */
    public synchronized void start(String hostname, Registration naming_server)
        throws RMIException, UnknownHostException, FileNotFoundException
    {
        InetSocketAddress clientAddress = new InetSocketAddress(hostname, clientPort);
        InetSocketAddress commandAddress = new InetSocketAddress(hostname, commandPort);
        clientSkeleton = new Skeleton<>(Storage.class, this, clientAddress);
        commandSkeleton = new Skeleton<>(Command.class, this, clientAddress);

        clientSkeleton.start();
        commandSkeleton.start();

        Path[] toDelete = naming_server.register(Stub.create(Storage.class, clientSkeleton, hostname),
                Stub.create(Command.class, commandSkeleton, hostname),
                Path.list(root));

        registrationCleanup(toDelete);
    }

    private synchronized void registrationCleanup(Path[] toDelete) throws FileNotFoundException
    {
        Path[] beforePrune = Path.list(root);
        for (Path file : toDelete)
        {
            delete(file);
        }
        List<Path> afterPrune = Arrays.asList(Path.list(root));

        for (Path toPrune : beforePrune)
        {
            if (afterPrune.contains(toPrune))
            {
                continue;
            }
            File directory = toPrune.toFile(root).getParentFile();
            while (directory.listFiles().length == 0)
            {
                directory.delete();
                directory = directory.getParentFile();
            }
        }
    }

    /** Stops the storage server.

        <p>
        The server should not be restarted.
     */
    public void stop()
    {
        clientSkeleton.stop();
        commandSkeleton.stop();
    }

    /** Called when the storage server has shut down.

        @param cause The cause for the shutdown, if any, or <code>null</code> if
                     the server was shut down by the user's request.
     */
    protected void stopped(Throwable cause)
    {
    }

    // The following methods are documented in Storage.java.
    @Override
    public synchronized long size(Path file) throws FileNotFoundException
    {
        File f = file.toFile(root);
        isValidFile(f);
        return f.length();
    }

    @Override
    public synchronized byte[] read(Path file, long offset, int length)
        throws FileNotFoundException, IOException
    {
        if (offset < 0)
        {
            throw new IndexOutOfBoundsException("Reading from negative offset.");
        }
        if (length < 0)
        {
            throw new IndexOutOfBoundsException("Trying to read negative length.");
        }
        File f = file.toFile(root);
        isValidFile(f);
        if (offset + length > f.length())
        {
            throw new IndexOutOfBoundsException("Reading past file size.");
        }
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        byte[] readBuffer = new byte[length];
        /* TODO kevin: support long offset. */
        raf.read(readBuffer, (int) offset, length);
        return readBuffer;
    }

    @Override
    public synchronized void write(Path file, long offset, byte[] data)
        throws FileNotFoundException, IOException
    {
        if (offset < 0)
        {
            throw new IndexOutOfBoundsException("Writing to negative offset.");
        }
        File f = file.toFile(root);
        isValidFile(f);
        RandomAccessFile raf = new RandomAccessFile(f, "rw");
        raf.seek(offset);
        raf.write(data);
    }

    private boolean isValidFile(File file) throws FileNotFoundException
    {
        if (!file.exists() || file.isDirectory())
        {
            throw new FileNotFoundException(file + "does not exist.");
        }
        return true;
    }

    // The following methods are documented in Command.java.
    @Override
    public synchronized boolean create(Path file)
    {
        File f = file.toFile(root);
        File parent = f.getParentFile();
        if(!parent.exists())
        {
            boolean success =  parent.mkdirs();
            if(!success)
            {
                return false;
            }
        }
        try
        {
            return f.createNewFile();
        }
        catch (IOException e)
        {
            return false;
        }
    }

    @Override
    public synchronized boolean delete(Path file)
    {
        if (file.isRoot())
        {
            return false;
        }
        File f = file.toFile(root);
        if(f.isDirectory())
        {
            return deleteDirectory(f);
        }
        else
        {
            return f.delete();
        }
    }

    /** Empties directory
     *
     * @param directory
     * @return success
     */
    private boolean deleteDirectory(File directory)
    {
        boolean success = true;
        for (File file : directory.listFiles())
        {
            if(file.isDirectory())
            {
                success = deleteDirectory(file);
            }
            else
            {
                success = file.delete();
            }
            if(!success)
            {
                return false;
            }
        }
        return directory.delete();
    }

    @Override
    public synchronized boolean copy(Path file, Storage server)
        throws RMIException, FileNotFoundException, IOException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}

package me.griffinbeck.server;


import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

class ServerThread extends Thread {
    private final int port;
    private ThreadManager threadManager;
    private String ip;

    public ServerThread(ThreadManager threadManager) {
        this.threadManager = threadManager;
        this.ip = Settings.IP.getSetting();
        this.port = Integer.parseInt(Settings.PORT.getSetting());
    }

    @Override
    public void run() {
        while (allowConnections()) {
            System.out.println("Connection Open");
            Socket socket = acceptConnection();
            if (socket != null) {
                if (threadManager.isRunServerConnection())
                    threadManager.awaitConnection(new ConnectionThread(socket, threadManager));
            }
        }
        threadManager.exitServerThread(this);
    }

    private Socket acceptConnection() {
        try {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ip, port));
            serverSocket.setReuseAddress(true);
            Socket socket1 = serverSocket.accept();
            socket1.setTcpNoDelay(true);
            socket1.setSoTimeout(1000);
            socket1.setKeepAlive(true);
            serverSocket.close();//MUST BE RAN TO ALLOW OTHER CONNECTIONS TO BE ACCEPTED
            System.out.println("Recieved Connection");
            return socket1;
        } catch (BindException e) {
            System.err.println("Failed to Bind Socket to Address");
            System.err.println("Is your ip and port settings valid and available?");
            e.printStackTrace();
            System.exit(-5);
        } catch (IOException e) {
            System.err.println("IO Exception occurred with network connection");
            e.printStackTrace();
            System.exit(-5);
        } catch (Exception e) {
            System.err.println("Error with Network Connection");
            e.printStackTrace();
            System.exit(-5);
        }
        return null;
    }

    public /*synchronized*/ boolean allowConnections() {
        //Check if more connections are allowed
        /*if(!threadManager.isRunServerConnection()) {
            return false;
        }
        if(Server.getDataManager().availableConnectionTypes() > 0) {
            return true;
        }*/
        return ( threadManager.isRunServerConnection() && Server.getDataManager().availableConnectionTypes() > 0 );
    }
}

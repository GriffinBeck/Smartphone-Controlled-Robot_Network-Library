package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

import java.io.IOException;
import java.net.Socket;

public class ClientSocketConnector {
    private final boolean isRobotClient;
    private String ip;
    private int port;
    private ClientConnectionManager clientConnectionManager;

    public ClientSocketConnector(String ip, int port, boolean isRobotClient) {
        this.ip = ip;
        this.port = port;
        this.isRobotClient = isRobotClient;
    }

    public Socket connectToServer() {
        return connectToServer(this.ip, this.port, this.isRobotClient);
    }

    private Socket connectToServer(String ip, int port, boolean isRobotClient) {
        try {
            Socket socket = new Socket(ip, port);
            if (clientConnectionManager != null) {
                clientConnectionManager.setSocket(socket);
            } else
                clientConnectionManager = new ClientConnectionManager(socket, this);
            if (!holdConnection()) {
                try {
                    socket.close();
                    socket = null;
                } catch (Exception e) {

                }
                return null;
            } else {
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(1000);
                socket.setKeepAlive(true);
                return socket;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @return true when socket is properly connected and setup for use
     */
    private boolean handlePacket(CommandPacket packet) throws IOException {
        String cmd = packet.getCmd();
        System.out.println("in handle packet: " + cmd);
        if (cmd.equalsIgnoreCase(Commands.REQUEST.toString())) {
            System.out.println("packet args" + packet.getArgs()[0]);
            if (packet.getArgs()[0].equalsIgnoreCase(CommandArguments.REQUEST_PROGRAMCONFIG.toString())) {
                clientConnectionManager.sendPacket(new CommandPacket(Commands.RESPONSE, CommandArguments.REQUEST_PROGRAMCONFIG.toString(), ( ( isRobotClient ) ? ( "ROBOT" ) : ( "CONTROLLER" ) )));
                System.out.println("sent Response");
                return false;
            }

        } else if (cmd.equalsIgnoreCase(Commands.CONNECTIONESABLISHED.toString())) {
            System.out.println("received Connection Established");
            return true;
        } else if (cmd.equalsIgnoreCase(Commands.EXIT.toString())) {
            throw new IOException("Server Connection Exited");
        } else {
            System.out.println("could not find cmd");
        }

        return false;//TODO
    }

    /**
     * @return true when socket is properly connected and setup for use
     * @Precondition Socket is Connected
     */
    private boolean holdConnection() throws IOException {
        boolean doRun = true;
        CommandPacket packet;
        while (doRun) {
            packet = clientConnectionManager.getPacket(true);//TODO
            System.out.println("calling handlePacket");
            if (handlePacket(packet)) {
                return true;
            }
        }
        return false;
    }

    public void setClientConnectionManager(ClientConnectionManager manager) {
        if (manager != null) {
            this.clientConnectionManager = manager;
        }
    }
}

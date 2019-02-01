package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class ConnectionThread extends Thread {
    private final int readTimeOut;
    private Socket socket;
    private ThreadManager threadManager;
    private DataOutputStream socketOut;
    private DataInputStream socketIn;
    private BlockingQueue<CommandPacket> sendingQueue;
    private BlockingQueue<CommandPacket> receivingQueue;
    private ClientConnectionManager clientConnectionManager;
    private int streamTimeOut;
    private String currentInput;
    private boolean isRobot;
    private boolean exit;
    private long timeSinceLastMessage;

    public ConnectionThread(Socket socket, ThreadManager threadManager) {
        this.socket = socket;
        this.threadManager = threadManager;
        timeSinceLastMessage = 0;
        DataManager dataManager = Server.getDataManager();
        clientConnectionManager = new ClientConnectionManager(socket);
        readTimeOut = dataManager.getReadTimeOut();
        streamTimeOut = dataManager.getStreamTimeOut();
        try {
            socketOut = new DataOutputStream(socket.getOutputStream());
            socketIn = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            if (authenticate()) {
                System.out.println("calling negotiation");
                if (negotiateConnectionProperties()) {
                    clientConnectionManager.sendPacket(new CommandPacket(Commands.CONNECTIONESABLISHED));
                    System.out.println("calling hold");
                    holdConnection();
                } else {
                    System.out.println("negotiation failed");
                    removeConnection(CommandArguments.EXIT_AUTHENTICATIONFAILED);
                }
                //TODO migrate removal of connection out?
            } else {
                Server.getThreadManager().waitingConnectionFailed(this);
                removeConnection(CommandArguments.EXIT_AUTHENTICATIONFAILED);
                clientConnectionManager.closeConnection();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Removing Connection isRobot: " + isRobot);
        threadManager.removeConnection(this);
    }

    //TODO rename to more descriptive name
    private void holdConnection() {
        exit = false;
        CommandPacket packetToSend = null;
        CommandPacket packetReceived = null;
        //Initialize connections
        System.out.println("HoldingConnection");
        if (isRobot) {
            //System.out.println("robot?: " + Server.getDataManager().isRobotConnected());
            //System.out.println("controller?: " + Server.getDataManager().isControllerConnected());
            sendingQueue = Server.getDataManager().getControllerQueue();
            receivingQueue = Server.getDataManager().getRobotQueue();
            if (Server.getDataManager().isControllerConnected()) {
                exit = true;
                receivingQueue.clear();
                sendingQueue.clear();
                try {
                    sendingQueue.add(CommandPacket.PAUSE_UNPAUSE_PACKET);
                    clientConnectionManager.sendPacket(new CommandPacket(Commands.LINK, CommandArguments.LINK_PREPARE));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    clientConnectionManager.sendPacket(CommandPacket.PAUSE_PAUSE_PACKET);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //System.out.println("controller?: " + Server.getDataManager().isRobotConnected());
            sendingQueue = Server.getDataManager().getRobotQueue();
            receivingQueue = Server.getDataManager().getControllerQueue();
            if (Server.getDataManager().isRobotConnected()) {
                exit = true;
                receivingQueue.clear();
                sendingQueue.clear();
                try {
                    sendingQueue.add(CommandPacket.PAUSE_UNPAUSE_PACKET);
                    clientConnectionManager.sendPacket(new CommandPacket(Commands.LINK, CommandArguments.LINK_PREPARE));
                    System.out.println("Link prepare sent to controller");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    clientConnectionManager.sendPacket(CommandPacket.PAUSE_PAUSE_PACKET);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        while (!exit) {
            if (isRobot) {
                //System.out.println("controller?: " + Server.getDataManager().isControllerConnected());
                if (Server.getDataManager().isControllerConnected()) {
                    exit = true;
                    receivingQueue.clear();
                    sendingQueue.clear();
                    try {
                        clientConnectionManager.sendPacket(new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_UNPAUSECONNECTION, CommandArguments.PAUSE_ESTABLISHLINK));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //System.out.println("controller?: " + Server.getDataManager().isRobotConnected());
                if (Server.getDataManager().isRobotConnected()) {
                    exit = true;
                    receivingQueue.clear();
                    sendingQueue.clear();
                    try {
                        clientConnectionManager.sendPacket(new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_UNPAUSECONNECTION, CommandArguments.PAUSE_ESTABLISHLINK));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!exit) {
                try {
                    packetReceived = clientConnectionManager.getPacket(false);
                    if (packetReceived != null) {
                        if (Commands.HEARTBEAT.equalTo(packetReceived.getCmd())) {
                            timeSinceLastMessage = System.currentTimeMillis();
                            if (CommandArguments.HEARTBEAT_REQUEST.equals(packetReceived.getArg(0))) {
                                clientConnectionManager.sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_RESPONSE));
                            }
                        } else {
                            System.out.println("received: " + packetReceived.getPacket());
                            handlePacketCommandFromSocket(packetReceived);
                            timeSinceLastMessage = System.currentTimeMillis();
                        }
                    } else if (System.currentTimeMillis() - timeSinceLastMessage > 1000) {
                        clientConnectionManager.sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
                    }
                } catch (SocketTimeoutException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                }
            }
        }
        exit = false;
        System.out.println("Both Connections Connected, starting Thread isRobot: " + isRobot);
        //Holding Connection
        packetReceived = null;
        packetToSend = null;
        timeSinceLastMessage = System.currentTimeMillis() + 10000;
        while (!exit) {
            try {
                try {
                    packetReceived = clientConnectionManager.getPacket(false);
                    if (packetReceived != null) {
                        timeSinceLastMessage = System.currentTimeMillis();
                        if (Commands.HEARTBEAT.equalTo(packetReceived.getCmd())) {
                            if (CommandArguments.HEARTBEAT_REQUEST.equals(packetReceived.getArg(0))) {
                                clientConnectionManager.sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_RESPONSE));
                            }
                        } else {
                            System.out.println("received: " + packetReceived.getPacket());
                            timeSinceLastMessage = System.currentTimeMillis();
                            handlePacketCommandFromSocket(packetReceived);
                        }
                    } /*else if (System.currentTimeMillis() - timeSinceLastMessage > 1000) {
                        clientConnectionManager.sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
                    }*/
                } catch (SocketTimeoutException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                    connectionLost();
                }
                if (receivingQueue.remainingCapacity() > 0) {
                    try {
                        packetToSend = receivingQueue.poll(1000, TimeUnit.MICROSECONDS);
                    /*if(!receivingQueue.isEmpty()) {
                        packetReceived = receivingQueue.take();
                    }*/
                        if (packetToSend != null) {
                            System.out.println("sending: " + packetToSend.getPacket() + "  //////////");
                            clientConnectionManager.sendPacket(packetToSend);
                            if (Commands.PAUSE.equalTo(packetToSend.getCmd())) {
                                timeSinceLastMessage = System.currentTimeMillis() + 10000;
                            }
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (SocketTimeoutException e) {

                    } catch (IOException e) {
                        e.printStackTrace();
                        connectionLost();
                    }
                }
                /*if (System.currentTimeMillis() - timeSinceLastMessage > 2000) {
                    packetReceived = null;
                    System.out.println("Sending Keep alive, isRobot: " + isRobot);
                    packetReceived = sendKeepAlive();
                    if (packetReceived == null) {
                        throw new IOException("Socket Connection lost");
                    } else if (Commands.HEARTBEAT.equalTo(packetReceived.getCmd())) {
                        timeSinceLastMessage = System.currentTimeMillis();
                    } else {
                        handlePacketCommandFromSocket(packetReceived);
                        timeSinceLastMessage = System.currentTimeMillis();
                    }
                }
            } catch (SocketTimeoutException timeoutExcept) {*/

            } catch (Exception el) {
                el.printStackTrace();
                connectionLost();

            }
            packetReceived = null;
            packetToSend = null;
        }
        System.out.println("exited loop");
    }

    private void connectionLost() {
        exit = true;
        try {
            socket.close();
        } catch (Exception e) {}
        //sendingQueue.add(new CommandPacket(Commands.EXIT.toString(), CommandArguments.EXIT_CONNECTIONLOST.toString()));
        System.out.println("Lost Connection: isRobot: " + isRobot + ", now removing");
        removeConnection(CommandArguments.EXIT_CONNECTIONLOST);
        clientConnectionManager.closeConnection();
        //threadManager.removeConnection(this);
    }

    private void handlePacketCommandFromSocket(CommandPacket packet) {
        if (packet.getCmd().equalsIgnoreCase(Commands.EXIT.toString())) {
            sendingQueue.add(packet);
            removeConnection(CommandArguments.EXIT_CONNECTIONTERMINATED);
            clientConnectionManager.closeConnection();
            threadManager.removeConnection(this);
        }
        /*if(packet.getCmd().equalsIgnoreCase(Commands.PAUSE.toString())) {
            sendPacket(packet);
            handlePause();
        }*/
        else {
            sendingQueue.add(packet);
        }
    }

    private void handlePacketCommandFromRemote(CommandPacket packet) throws IOException {
        if (packet.getCmd().equalsIgnoreCase(Commands.EXIT.toString())) {
            clientConnectionManager.sendPacket(packet);
        }
        if (packet.getCmd().equalsIgnoreCase(Commands.PAUSE.toString())) {
            clientConnectionManager.sendPacket(packet);
            handlePause();
        } else {
            clientConnectionManager.sendPacket(packet);
        }
    }

    private void handlePause() {
        while (threadManager.isPauseConnectionThread()) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (socket.isConnected()) {
                try {
                    clientConnectionManager.sendPacket(new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_PAUSECONNECTION));
                } catch (IOException e) {
                    exit = true;
                    sendingQueue.add(new CommandPacket(Commands.EXIT.toString(), CommandArguments.EXIT_CONNECTIONLOST.toString()));
                    removeConnection(CommandArguments.EXIT_CONNECTIONLOST);
                    clientConnectionManager.closeConnection();
                    threadManager.removeConnection(this);
                }
            } else {
                exit = true;
                sendingQueue.add(new CommandPacket(Commands.EXIT.toString(), CommandArguments.EXIT_CONNECTIONLOST.toString()));
                removeConnection(CommandArguments.EXIT_CONNECTIONLOST);
                clientConnectionManager.closeConnection();
                threadManager.removeConnection(this);
            }
        }
    }


    /**
     * Authenticates a connection
     *
     * @return If connection is properly authenticated
     */
    //TODO authenticate
    private boolean authenticate() {
        return true;
    }

    /**
     * @return returns false if failed to negotiate
     * @Precondition Socket is Connected
     */
    private boolean negotiateConnectionProperties() {
        try {
            clientConnectionManager.sendPacket(new CommandPacket(Commands.REQUEST, CommandArguments.REQUEST_PROGRAMCONFIG));
            CommandPacket response = getResponse(false);
            System.out.println(response.getPacket());
            if (CommandArguments.REQUEST_PROGRAMCONFIG.equals(response.getArg(0))) {
                String hold = response.getArg(1);
                System.out.println("Checking the request");
                if (hold.equals("ROBOT")) {
                    if (!Server.getThreadManager().assignRobot(this)) {
                        System.out.println("exiting");
                        removeConnection(CommandArguments.EXIT_CONNECTIONFILLED);
                        clientConnectionManager.closeConnection();
                        return false;
                    }
                    System.out.println("Setting is robot to true");
                    isRobot = true;
                } else if (hold.equals("CONTROLLER")) {
                    if (!Server.getThreadManager().assignController(this)) {
                        System.out.println("exiting");
                        removeConnection(CommandArguments.EXIT_CONNECTIONFILLED);
                        clientConnectionManager.closeConnection();
                        return false;
                    }
                    System.out.println("setting is robot to false");
                    isRobot = false;
                } else {
                    System.out.println("exiting");
                    removeConnection(CommandArguments.EXIT_CONNECTIONFILLED);
                    clientConnectionManager.closeConnection();
                    return false;
                }
            } else if (CommandArguments.RESPONSE_AUTORECCONNECT.equals(response.getArg(0))) {

            } else {
                System.out.println("request failed" + response.getArgs()[0]);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /*private void sendPacket(Commands cmd, String... data) throws IOException {
        String packet = "<" + cmd.toString();
        for (String word : data) {
            packet += "," + word;
        }
        packet += ">";
        //socketOut.writeChars(packet);
        socketOut.writeUTF(packet);
        socketOut.flush();
        socket.getOutputStream().flush(); //Re-add if packets are not sent
    }*/

    /*private void sendPacket(CommandPacket commandPacket) throws IOException {
        String packet = commandPacket.getPacket();
        //socketOut.writeChars(packet);
        socketOut.writeUTF(packet);
        socketOut.flush();
        socket.getOutputStream().flush(); //Re-add if packets are not sent
    }*/

    private CommandPacket sendKeepAlive() throws IOException {
        clientConnectionManager.sendPacket(new CommandPacket(Commands.HEARTBEAT, CommandArguments.HEARTBEAT_REQUEST));
        return clientConnectionManager.getPacket(3000000000L);
    }


    /*private void removeConnection(String reason) {
        try {
            OutputStream stream = socket.getOutputStream();
            sendPacket(Commands.EXIT, reason);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    private void removeConnection(CommandArguments commandArguments) {
        if (commandArguments.getLinkedCommand() == Commands.EXIT) {
            try {
                //OutputStream stream = socket.getOutputStream();
                clientConnectionManager.sendPacket(Commands.EXIT, commandArguments.toString());
                //socket.close();
            } catch (IOException e) {
                //socket = null;//TODO verify that nullifying all pointers will drop the connection in said case
            }
        }
    }

    /*private CommandPacket getPacket(boolean waitForMessage) throws IOException {
        boolean found = false;
        if (!waitForMessage) {
            long time = System.nanoTime();
            byte[] arr;
            while (!found && System.nanoTime() - time < readTimeOut) {
                //arr = new byte[socketIn.available()];
                //socketIn.readFully(arr);
                //currentInput += new String(arr,"UTF-8");
                if (socketIn.available() > 0) {
                    currentInput += socketIn.readUTF();
                    //System.out.println("currentInput: " + currentInput);
                }
                if (containsPacket())
                    found = true;
            }
        } else {
            byte[] arr;
            while (!found) {
                //arr = new byte[socketIn.available()];
                //socketIn.readFully(arr);
                //currentInput += new String(arr,"UTF-8");
                currentInput += socketIn.readUTF();
                if (containsPacket())
                    found = true;
            }
        }
        if (found) {
            int start = currentInput.indexOf("<");
            int end = currentInput.indexOf(">");
            CommandPacket toReturn = CommandPacket.fromPacket(currentInput.substring(start, end + 1));
            currentInput = currentInput.substring(end + 1);
            return toReturn;
        }
        return null;
    }*/

    /*private CommandPacket getPacket(long waitForMessage) throws IOException {
        boolean found = false;
        long time = System.nanoTime();
        byte[] arr;
        while (!found && System.nanoTime() - time < waitForMessage) {
            //arr = new byte[socketIn.available()];
            //socketIn.readFully(arr);
            //currentInput += new String(arr,"UTF-8");
            if (socketIn.available() > 0) {
                currentInput += socketIn.readUTF();
                //System.out.println("currentInput: " + currentInput);
            }
            if (containsPacket())
                found = true;
        }
        if (found) {
            int start = currentInput.indexOf("<");
            int end = currentInput.indexOf(">");
            CommandPacket toReturn = CommandPacket.fromPacket(currentInput.substring(start, end + 1));
            currentInput = currentInput.substring(end + 1);
            return toReturn;
        }
        return null;
    }*/

    private CommandPacket getResponse(boolean commandPassThrough) throws IOException {
        //CommandPacket packet = getPacket(true);
        return clientConnectionManager.getPacket(true);
        //TODO make this not dumb
    }

    /*private boolean containsPacket() {
        int i = currentInput.indexOf("<");
        int x = currentInput.indexOf(">");
        if (i > -1 && x > -1) {
            if (i < x)
                return true;
            else {
                cleanExcessPacket();
            }
        }
        return false;
    }*/

    private void cleanExcessPacket() {
        int i = currentInput.indexOf("<");
        int x = currentInput.indexOf(">");
        if (i > -1 && x > -1) {
            if (i > x) {
                currentInput = currentInput.substring(i + 1);
            }
        }
    }
}

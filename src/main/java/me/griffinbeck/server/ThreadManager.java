package me.griffinbeck.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadManager {
    private ConnectionThread robotConnection;
    private int robotConnectionID;
    private ConnectionThread controllerConnection;
    private int controllerConnectionID;
    private ConnectionThread waitingConnection;
    private ServerThread serverThread;
    private ExecutorService executor;
    private boolean runServerConnection;
    private boolean pauseConnectionThread;

    public ThreadManager() {
        executor = Executors.newCachedThreadPool();
        runServerConnection = true;
        pauseConnectionThread = false;
    }

    public void startServer() {
        serverThread = new ServerThread(this);
        executor.execute(serverThread);
    }

    public void prepareForForceShutdown() {
        executor.shutdownNow();
    }

    public void prepareForExit() {
        executor.shutdown();
    }

    public boolean assignRobot(ConnectionThread thread) {
        if (!Server.getDataManager().isRobotConnected()) {
            System.out.println("configuring robot");
            robotConnection = thread;
            Server.getDataManager().setIsRobotConnected(true);
            if (areConnectionsSaturated()) {
                runServerConnection = false;
                pauseConnectionThread = false;
            }
            if (thread.equals(waitingConnection)) {
                waitingConnection = null;
            }
            return true;
        }
        return false;
    }

    public boolean assignController(ConnectionThread thread) {
        if (!Server.getDataManager().isControllerConnected()) {
            controllerConnection = thread;
            Server.getDataManager().setIsControllerConnected(true);
            if (areConnectionsSaturated()) {
                runServerConnection = false;
                pauseConnectionThread = false;
            }
            if (thread.equals(waitingConnection)) {
                waitingConnection = null;
            }
            return true;
        }
        return false;
    }

    public int getRobotConnectionID() {
        if (robotConnectionID == 0) {
            if (Server.getDataManager().isRobotConnected()) {
                robotConnectionID = (int) ( Math.random() * 9998 + 1 );
                return robotConnectionID;
            } else {
                return 0;
            }
        } else {
            return robotConnectionID;
        }
    }

    public int getControllerConnectionID() {
        if (controllerConnectionID == 0) {
            if (Server.getDataManager().isControllerConnected()) {
                controllerConnectionID = (int) ( Math.random() * 9998 + 1 );
                return controllerConnectionID;
            } else {
                return 0;
            }
        } else {
            return controllerConnectionID;
        }
    }

    public void awaitConnection(ConnectionThread thread) {
        waitingConnection = thread;
        System.out.println("Begining Connection negotiation");
        executor.submit(waitingConnection);
    }

    public void waitingConnectionFailed(ConnectionThread thread) {
        if (thread.equals(waitingConnection)) {
            waitingConnection = null;
            serverThread = new ServerThread(this);
            executor.submit(serverThread);
        }
    }

    public boolean isAwaitingConnection() {
        return waitingConnection != null;
    }

    public boolean isPauseConnectionThread() {
        return pauseConnectionThread;
    }

    private boolean areConnectionsSaturated() {
        return ( robotConnection != null && robotConnection.isAlive() ) && ( controllerConnection != null && controllerConnection.isAlive() );
    }

    public void removeConnection(ConnectionThread connectionThread) {
        if (connectionThread.equals(robotConnection)) {
            robotConnection = null;
            Server.getDataManager().setIsRobotConnected(false);
            removeConnection(connectionThread, true);
            return;
        }

        if (connectionThread.equals(controllerConnection)) {
            controllerConnection = null;
            Server.getDataManager().setIsControllerConnected(false);
            removeConnection(connectionThread, false);
        }

    }

    private void removeConnection(ConnectionThread connectionThread, boolean isRobot) {
        if (!areConnectionsSaturated()) {
            if (serverThread == null) {
                runServerConnection = true;
                startServer();
                System.out.println("Allowing connections ");
            }
        }
        if (robotConnection != null) {
            Server.getDataManager().getRobotQueue().clear();
            Server.getDataManager().getRobotQueue().add(CommandPacket.PAUSE_PAUSE_PACKET);
            pauseConnectionThread = true;
            System.out.println("Controller Connection Removed");
        } else if (controllerConnection != null) {
            Server.getDataManager().getControllerQueue().clear();
            Server.getDataManager().getControllerQueue().add(CommandPacket.PAUSE_PAUSE_PACKET);
            pauseConnectionThread = true;
            System.out.println("Robot Connection Removed");
        }
        /*if(isRobot) {
            robotConnection = null;
            Server.getDataManager().setIsRobotConnected(false);
        } else {
            controllerConnection = null;
            Server.getDataManager().setIsControllerConnected(false);
        }*/
    }

    public void exitServerThread(ServerThread serverThread) {
        if (serverThread.equals(this.serverThread)) {
            this.serverThread = null;
            runServerConnection = false;
        }
    }

    public boolean isRunServerConnection() {
        return runServerConnection;
    }
}

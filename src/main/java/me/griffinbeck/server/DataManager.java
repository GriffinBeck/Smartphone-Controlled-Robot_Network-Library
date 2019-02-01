package me.griffinbeck.server;

import me.griffinbeck.server.cmdresponses.CommandArguments;
import me.griffinbeck.server.cmdresponses.Commands;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataManager {
    private BlockingQueue<CommandPacket> toController;
    private BlockingQueue<CommandPacket> toRobot;
    private boolean isRobotConnected;
    private boolean isControllerConnected;

    public DataManager() {
        isRobotConnected = false;
        isControllerConnected = false;
        toController = new LinkedBlockingQueue<>();
        toRobot = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<CommandPacket> getControllerQueue() {
        return toController;
    }

    public BlockingQueue<CommandPacket> getRobotQueue() {
        return toRobot;
    }

    public boolean controllerIsQueued() {
        return !toController.isEmpty();
    }

    public boolean robotIsQueued() {
        return !toRobot.isEmpty();
    }

    public int getPort() {
        return Integer.parseInt(Settings.PORT.getSetting());
    }

    public String getIP() {
        return Settings.IP.getSetting();
    }

    public boolean isRobotConnected() {
        return isRobotConnected;
    }

    public boolean isControllerConnected() {
        return isControllerConnected;
    }

    public void setIsRobotConnected(boolean isRobotConnected) {
        this.isRobotConnected = isRobotConnected;
        if (!isRobotConnected) {
            toController.clear();
            toController.add(new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_PAUSECONNECTION));
        }

    }

    public void setIsControllerConnected(boolean isControllerConnected) {
        this.isControllerConnected = isControllerConnected;
        if (!isControllerConnected) {
            toRobot.clear();
            toRobot.add(new CommandPacket(Commands.PAUSE, CommandArguments.PAUSE_PAUSECONNECTION));
        }
    }

    public int getReadTimeOut() {
        return Integer.parseInt(Settings.READTIMEOUT.getSetting());
    }

    public int getStreamTimeOut() {
        return Integer.parseInt(Settings.STREAMTIMEOUT.getSetting());
    }

    /**
     * @return number of available connections from 0 to 2 (will count connections that have not been authenticated or properly negotiated yet)
     */
    public int availableConnectionTypes() {
        int response = 0;
        if (!isRobotConnected())
            response++;
        if (!isControllerConnected())
            response++;
        if (Server.getThreadManager().isAwaitingConnection())
            response--;
        return response;
    }
}

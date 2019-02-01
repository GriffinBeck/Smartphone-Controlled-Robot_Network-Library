package me.griffinbeck.server;

public class Server {
    private static DataManager dataManager;
    private static ThreadManager threadManager;

    public static void main(String[] args) {
        init();
        dataManager = new DataManager();
        threadManager = new ThreadManager();
        threadManager.startServer();
    }

    private static void init() {
        Settings.init();
        Settings.loadSettings();
    }

    public static DataManager getDataManager() {
        return dataManager;
    }

    public static ThreadManager getThreadManager() {
        return threadManager;
    }
}

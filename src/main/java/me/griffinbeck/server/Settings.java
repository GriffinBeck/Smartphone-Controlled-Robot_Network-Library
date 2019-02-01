package me.griffinbeck.server;

import java.io.*;
import java.util.Properties;

public enum Settings {
    IP("ip"),
    PORT("port"),
    READTIMEOUT("readtimeout"),
    STREAMTIMEOUT("streamtimeout");
    private static Properties properties;
    final String key;

    Settings(String key) {
        this.key = key;
    }

    public static void init() {
        Properties defaultProp = new Properties();
        try {
            InputStream stream = ClassLoader.getSystemResourceAsStream("settings.properties");
            defaultProp.load(stream);
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-3);
        }
        Settings.properties = new Properties(defaultProp);
    }

    public static void loadSettings() {
        final File propsFile = new File("settings.properties");
        if (!propsFile.exists()) {
            try {
                propsFile.createNewFile();
                ClassLoader.getSystemResourceAsStream("settings.properties");
                OutputStream fileStream = new FileOutputStream(propsFile);
                //Properties prop = new Properties();
                DataInputStream stream = new DataInputStream(ClassLoader.getSystemResourceAsStream("settings.properties"));
                //InputStream stream = Settings.class.getResourceAsStream("settings.properties");//TODO why u not get file
                //prop.load(ClassLoader.getSystemResourceAsStream("settings.properties"));
                byte[] buffer = new byte[stream.available()];
                for (int i = 0; i < buffer.length; i++) {
                    buffer[i] = stream.readByte();
                }
                fileStream.write(buffer);
                fileStream.flush();
                fileStream.close();
                System.out.println("Program settings were not available, please configure the settings");
                System.out.println("in the settings.properties file. Before starting again");
                System.exit(-3);
            } catch (Exception e) {
                System.err.println("An Error occurred while creating the settings.properties file");
                e.printStackTrace();
                System.exit(-3);
            }
        } else {
            try {
                InputStream in = new FileInputStream(propsFile);
                properties.load(in);
            } catch (Exception e) {
                System.err.println("Could Not Load Settings");
                e.printStackTrace();
                System.exit(-3);
            }
        }
    }

    public String getSetting() {
        return properties.getProperty(this.key);
    }
}

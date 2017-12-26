package com.sandvoxel.firestormbot.utility;

import java.io.*;
import java.util.Properties;

public class ConfigFile {
    public void setConfig(){
        Properties prop = new Properties();
        OutputStream output = null;

        try {

            output = new FileOutputStream("config.properties");

            // set the properties value
                prop.setProperty("CommandPrefix", "&");
                prop.setProperty("token","");


            // save properties to project root folder
            prop.store(output, null);

        } catch (IOException io) {
            io.printStackTrace();
        } finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }
    public String getConfig(String conf) {
        String output = "";
        Properties prop = new Properties();
        InputStream input = null;

        try {

            input = new FileInputStream("config.properties");

            // load a properties file
            prop.load(input);

            // get the property value and print it out
            output = prop.getProperty(conf);


        } catch (IOException ex) {
            setConfig();
            if(conf.equals("CommandPrefix")){
                output = "&";
            }else {
                System.out.println("Place token in config file");
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return output;
    }
}

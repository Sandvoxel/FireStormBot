package com.sandvoxel.firestormbot;

import com.sandvoxel.firestormbot.audio.PlayerControl;
import com.sandvoxel.firestormbot.commands.TestCommand;
import com.sandvoxel.firestormbot.utility.ConfigFile;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

import java.io.IOException;

public class FireStormBot extends ListenerAdapter {

    public static String CommandPrefix = "";
    public static String token = "";


    public static void main(String[] args) throws LoginException, RateLimitedException, InterruptedException, IOException {

        ConfigFile configFile = new ConfigFile();
        CommandPrefix = configFile.getConfig("CommandPrefix");
        token = configFile.getConfig("token");
        JDA jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
        jda.addEventListener(new PlayerControl());
        jda.addEventListener(new TestCommand());
    }

}

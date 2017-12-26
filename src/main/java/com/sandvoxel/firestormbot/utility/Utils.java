package com.sandvoxel.firestormbot.utility;

import com.sandvoxel.firestormbot.FireStormBot;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

public class Utils {


    public static boolean isCommand(Message message){
        return message.getContent().toLowerCase().startsWith(FireStormBot.CommandPrefix);
    }

    public static boolean role(Member msg, Permission permission){
        return msg.hasPermission(permission);
    }

    public static String rdm(String[] string) {
        return string[(int)(Math.random() * string.length)];
    }
}

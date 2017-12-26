package com.sandvoxel.firestormbot.commands;

import com.sandvoxel.firestormbot.FireStormBot;
import com.sandvoxel.firestormbot.Refrence;
import com.sandvoxel.firestormbot.utility.ConfigFile;
import com.sandvoxel.firestormbot.utility.Utils;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;

public class TestCommand extends ListenerAdapter {
    Utils utils = new Utils();
    ConfigFile configFile = new ConfigFile();


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(utils.isCommand(event.getMessage())&& !event.getMember().getUser().isBot()){
            String[] strings = event.getMessage().getContent().split(" ");


            switch (strings[0].replace(FireStormBot.CommandPrefix,"")){
                case "help":
                    PrivateChannel meme =event.getMember().getUser().openPrivateChannel().complete();
                    meme.sendMessage("```ini\nFun = General commands" +
                            "\nascii                  # posts random ascii art" +
                            "\nlenny                  # adds a nice lenny ( ͡° ͜ʖ ͡°)" +
                            "\nroast                  # roasts you as bad as never before seen" +
                            "\npengu                  # posts a cute pengu! 🐧" +
                            "\ncoin                   # flips a *totally* fair coin" +
                            "\nmeme                   # posts a random meme" +
                            "\ncancer                 # infects the channel with toxic, cringy pictures and videos" +
                            "\nnuke [place]           # nukes the bad guys! (Use '&nuke help' for a list of places)" +
                            "\nscp [number]           # posts an SCP entry! (001-4000)" +
                            "\nroll [number]          # rolls a fair 6-sided-die (or with more sides if you wish)" +
                            "\nask [question]         # asks me a yes/no question" +
                            "\navatar [@user]         # posts the avatar of that user, or yourself if no user is mentioned" +
                            "\nbtext [message]        # changes all instances of `b` to the 🅱 emote" +
                            "\ninfo [pagename]        # displays info from that YTP Wiki page" +
                            "\neflag [country]        # displays the flag of a country with emojis" +
                            "\nvapor [message]        # makes your text into a ｖａｐｏｒｔｅｘｔ" +
                            "\nbattle [army1] [army2] # simulates a battle between two armies```").queue();
                    meme.sendMessage("```ini\nAdmin = You need the correct role to use them" +
                            "\nlang  [de/en]          # sets the language, currently only for German or English" +
                            "\nclear [number]         # deletes messages" +
                            "\nprefix [prefix]        # changes the bot prefix" +
                            "\nwarn  [@user] [reason] # warns that user, sending both you and them a message" +
                            "\nban   [@user] [reason] # unleashes a banhammer upon that user, sending both you and them a message" +
                            "\nkick  [@user] [reason] # kicks the mentoned user, but he can return" +
                            "\nrepeat [number] [text] # reapeats a message a certain amount of times```").queue();
                    meme.sendMessage("```ini\nNSFW = You must be in a 'NSFW' channel to use them" +
                            "\necchi  # posts an ecchi picture from imgur" +
                            "\nass    # results in a ass pic" +
                            "\nboobs  # posts a random boobs picture```").queue();
                    meme.sendMessage("```ini\nUtility = The very useful commands, without which you cannot live" +
                            "\nbotinfo          # displays infos about the bot" +
                            "\nsupport          # links to ways to get bot Support" +
                            "\ntest             # tests whether the bot is active" +
                            "\ndate             # posts the date and time" +
                            "\ninvite           # gets you the bot invite link" +
                            "\ndonate           # posts a link to the Patreon page" +
                            "\ndefine [word]    # searches up the definition of a word using The Urban Dictonary" +
                            "\nsay [message]    # gets bot to say something" +
                            "\npoll [question]  # creates a poll with two options (Yes or No)" +
                            "\ncalc [operation] # calculates using numbers and operations 🔢```" +
                            "\nNow pick a command, or else you will die!").queue();
                    event.getTextChannel().sendMessage("The list of commands was sent to you privately via DMs! ✅").queue();
                    break;

                case "ecchi":
                    if(event.getTextChannel().isNSFW()){
                        event.getTextChannel().sendMessage(Utils.rdm(Refrence.Ecchi)).queue();
                    }
                    break;
                case "boobs":
                    if(event.getTextChannel().isNSFW()){
                        event.getTextChannel().sendMessage(Utils.rdm(Refrence.Boobs)).queue();
                    }
                    break;
                case "ass":
                    if(event.getTextChannel().isNSFW()){
                        event.getTextChannel().sendMessage(Utils.rdm(Refrence.ass)).queue();
                    }
                    break;
                case "say":
                    String temp = event.getMessage().getContent().replace("say ","");
                    event.getMessage().delete().queue();
                    event.getTextChannel().sendMessage(temp).queue();
                    break;
                case "reload":
                    FireStormBot.CommandPrefix = configFile.getConfig("CommandPrefix");
                    event.getChannel().sendMessage("reloaded config file Prefix is now "+ FireStormBot.CommandPrefix).queue();
                    break;
                default:
                    System.out.println(event.getMessage().getContent());
            }


        }
    }
}

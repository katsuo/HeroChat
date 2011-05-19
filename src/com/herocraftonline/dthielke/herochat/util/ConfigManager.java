/**
 * Copyright (C) 2011 DThielke <dave.thielke@gmail.com>
 * 
 * This work is licensed under the Creative Commons Attribution-NonCommercial-NoDerivs 3.0 Unported License.
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/3.0/ or send a letter to
 * Creative Commons, 171 Second Street, Suite 300, San Francisco, California, 94105, USA.
 **/

package com.herocraftonline.dthielke.herochat.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import com.herocraftonline.dthielke.herochat.HeroChat;
import com.herocraftonline.dthielke.herochat.channels.Channel;
import com.herocraftonline.dthielke.herochat.channels.ChannelManager;
import com.herocraftonline.dthielke.herochat.chatters.Chatter;

public class ConfigManager {
    private final HeroChat plugin;
    private final File primaryConfigFile;
    private final File usersConfigFolder;

    public ConfigManager(HeroChat plugin) {
        this.plugin = plugin;
        this.primaryConfigFile = new File(plugin.getDataFolder(), "config.yml");
        this.usersConfigFolder = new File(plugin.getDataFolder(), "users/");
        usersConfigFolder.mkdirs();
    }

    public void reload() {
        load();
    }

    public void load() {
        checkConfig();

        Configuration config = new Configuration(primaryConfigFile);
        config.load();
        loadChannels(config);
        loadGlobals(config);
        loadPlayers();
    }

    private void checkConfig() {
        if (!primaryConfigFile.exists()) {
            try {
                primaryConfigFile.getParentFile().mkdir();
                primaryConfigFile.createNewFile();
                OutputStream output = new FileOutputStream(primaryConfigFile, false);
                InputStream input = ConfigManager.class.getResourceAsStream("config.yml");
                byte[] buf = new byte[8192];
                while (true) {
                    int length = input.read(buf);
                    if (length < 0) {
                        break;
                    }
                    output.write(buf, 0, length);
                }
                input.close();
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadGlobals(Configuration config) {
        ChannelManager channelManager = plugin.getChannelManager();
        String defChannelName = config.getString("globals.default-channel");

        Channel defChannel = channelManager.getChannel(defChannelName);
        if (defChannel == null) {
            defChannel = channelManager.getChannels()[0];
        }
        channelManager.setDefaultChannel(defChannel);
    }

    private void loadChannels(Configuration config) {
        ChannelManager channelManager = plugin.getChannelManager();
        for (String name : config.getKeys("channels")) {
            Channel channel = Channel.load(plugin, config, "channels." + name);
            channelManager.addChannel(channel);
        }
    }

    public void loadPlayer(Player player) {
        File userConfigFile = new File(usersConfigFolder, player.getName() + ".yml");
        Configuration config = new Configuration(userConfigFile);
        if (userConfigFile.exists()) {
            config.load();
        }
        Chatter chatter = Chatter.load(plugin, config, player);
        chatter.initialize(userConfigFile.exists());
        plugin.getChatterManager().addChatter(chatter);
    }
    
    public void loadPlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            loadPlayer(player);
        }
    }

    public void savePlayer(Player player) {
        File userConfigFile = new File(usersConfigFolder, player.getName() + ".yml");
        Configuration config = new Configuration(userConfigFile);
        Chatter chatter = plugin.getChatterManager().getChatter(player);
        chatter.save(config);
        config.save();
    }
    
    public void savePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            savePlayer(player);
        }
    }

    public void save() {
        Configuration config = new Configuration(primaryConfigFile);
        saveGlobals(config);
        saveChannels(config);
        savePlayers();
        config.save();
    }

    private void saveGlobals(Configuration config) {
        config.setProperty("globals.default-channel", plugin.getChannelManager().getDefaultChannel().getName());
    }

    private void saveChannels(Configuration config) {
        ConfigurationNode channelsNode = Configuration.getEmptyNode();
        Channel[] channels = plugin.getChannelManager().getChannels();
        for (Channel channel : channels) {
            ConfigurationNode channelNode = Configuration.getEmptyNode();
            channel.save(channelNode);
            channelsNode.setProperty("channels." + channel.getName(), channelNode);
        }
        config.setProperty("channels", channelsNode);
    }

}

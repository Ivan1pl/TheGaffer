/*  This file is part of TheGaffer.
 * 
 *  TheGaffer is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  TheGaffer is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with TheGaffer.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.thegaffer;

import com.mcmiddleearth.thegaffer.TeamSpeak.TSfetcher;
import com.mcmiddleearth.thegaffer.commands.AdminCommands.JobAdminConversation;
import com.mcmiddleearth.thegaffer.commands.JobCommand;
import com.mcmiddleearth.thegaffer.commands.JobCreationConversation;
import com.mcmiddleearth.thegaffer.ext.ExternalProtectionHandler;
import com.mcmiddleearth.thegaffer.listeners.CraftingListener;
import com.mcmiddleearth.thegaffer.listeners.JobEventListener;
import com.mcmiddleearth.thegaffer.listeners.PlayerListener;
import com.mcmiddleearth.thegaffer.listeners.ProtectionListener;
import com.mcmiddleearth.thegaffer.servlet.GafferServer;
import com.mcmiddleearth.thegaffer.storage.Job;
import com.mcmiddleearth.thegaffer.storage.JobDatabase;
import com.mcmiddleearth.thegaffer.utilities.CleanupUtil;
import com.mcmiddleearth.thegaffer.utilities.Util;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

public class TheGaffer extends JavaPlugin {

    @Getter
    static Server serverInstance;
    @Getter
    static TheGaffer pluginInstance;
    @Getter
    static File pluginDataFolder;
    @Getter
    static String fileSeperator = System.getProperty("file.separator");
    @Getter
    static ObjectMapper jsonMapper;
    @Getter
    static String fileExtension = ".job";
    @Getter
    static boolean debug = false;
    @Getter
    static boolean TS;
    @Getter
    static Configuration pluginConfig;
    @Getter
    static boolean TSenabled;
    @Getter
    static int servletPort;
    GafferServer server;
    @Getter
    static List<String> unprotectedWorlds = new ArrayList();
    @Getter
    static ArrayList<Player> listening = new ArrayList();
    @Getter
    static List<ExternalProtectionHandler> externalProtectionAllowHandlers = new ArrayList();
    @Getter
    static List<ExternalProtectionHandler> externalProtectionDenyHandlers = new ArrayList();

    @Override
    public synchronized void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        debug = getConfig().getBoolean("general.debug");
        jsonMapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, false);
        if(this.getConfig().contains("TS")){
            TSenabled = this.getConfig().getBoolean("TS");
        }else{
            TSenabled = false;
        }
        new TSfetcher().runTaskTimer(this, 20, 1200);
        setupConfig();
        try {
            int jobsLoaded = JobDatabase.loadJobs();
            Util.info("Loaded " + jobsLoaded + " jobs.");
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        getCommand("createjob").setExecutor(new JobCreationConversation());
        getCommand("job").setExecutor(new JobCommand());
        getCommand("jobadmin").setExecutor(new JobAdminConversation());
        serverInstance.getPluginManager().registerEvents(new PlayerListener(), this);
        serverInstance.getPluginManager().registerEvents(new ProtectionListener(), this);
        serverInstance.getPluginManager().registerEvents(new JobEventListener(), this);
        serverInstance.getPluginManager().registerEvents(new CraftingListener(), this);
        serverInstance.getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Util.debug("Starting running job cleanup.");
                CleanupUtil.scheduledCleanup();
                CleanupUtil.scheduledAbandonersCleanup();
            }
        }, 0, (5 * 60) * 20);
        server = new GafferServer(servletPort);
        try {
            server.startServer();
        } catch (Exception ex) {
            Util.severe(ex.toString());
        }
    }

    @Override
    public synchronized void onDisable() {
        try {
            server.stopServer();
        } catch (Exception ex) {
            Logger.getLogger(TheGaffer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setupConfig() {
        pluginConfig = getConfig();
        debug = pluginConfig.getBoolean("general.debug");
        servletPort = pluginConfig.getInt("servlet.port");
        unprotectedWorlds = pluginConfig.getStringList("unprotectedworlds");
        if(pluginConfig.contains("externalProtectionHandlers")) {
            ConfigurationSection section = pluginConfig.getConfigurationSection("externalProtectionHandlers");
            Set<String> handlers = section.getKeys(false);
            for(String pname : handlers) {
                ConfigurationSection pluginSection = section.getConfigurationSection(pname);
                if(pluginSection.contains("allow")) {
                    externalProtectionAllowHandlers.add(new ExternalProtectionHandler(pname, pluginSection.getString("allow")));
                }
                if(pluginSection.contains("deny")) {
                    externalProtectionDenyHandlers.add(new ExternalProtectionHandler(pname, pluginSection.getString("deny")));
                }
            }
        }
        saveDefaultConfig();
    }

    public static void scheduleOwnerTimeout(Job job) {
        Long time = System.currentTimeMillis();
        CleanupUtil.getWaiting().put(job, time);
    }
}

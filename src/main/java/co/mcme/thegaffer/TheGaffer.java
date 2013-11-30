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
package co.mcme.thegaffer;

import co.mcme.jobs.Cleanup;
import co.mcme.jobs.listeners.PlayerListener;
import co.mcme.thegaffer.commands.JobCreationConversation;
import co.mcme.thegaffer.storage.Job;
import co.mcme.thegaffer.utilities.Util;
import co.mcme.thegaffer.storage.JobDatabase;
import co.mcme.thegaffer.utilities.CleanupUtil;
import java.io.File;
import java.io.IOException;
import lombok.Getter;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
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
    static ObjectMapper jsonMapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    @Getter
    static String fileExtension = ".job";
    @Getter
    static boolean debug = false;

    @Override
    public void onEnable() {
        serverInstance = getServer();
        pluginInstance = this;
        pluginDataFolder = pluginInstance.getDataFolder();
        debug = getConfig().getBoolean("general.debug");
        try {
            JobDatabase.loadJobs();
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
        getCommand("createjob").setExecutor(new JobCreationConversation());
        serverInstance.getPluginManager().registerEvents(new PlayerListener(), this);
        serverInstance.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                Util.debug("Starting running job cleanup.");
                CleanupUtil.scheduledCleanup();
            }
        }, 0, (5 * 60) * 20);
    }

    @Override
    public void onDisable() {
        try {
            JobDatabase.saveJobs();
        } catch (IOException ex) {
            Util.severe(ex.getMessage());
        }
    }
    
    public static void scheduleOwnerTimeout(Job job) {
        Long time = System.currentTimeMillis();
        CleanupUtil.getWaiting().put(job, time);
    }
}

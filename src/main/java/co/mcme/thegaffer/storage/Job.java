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
package co.mcme.thegaffer.storage;

import co.mcme.thegaffer.GafferResponses.BanWorkerResponse;
import co.mcme.thegaffer.GafferResponses.HelperResponse;
import co.mcme.thegaffer.GafferResponses.InviteResponse;
import co.mcme.thegaffer.GafferResponses.WorkerResponse;
import co.mcme.thegaffer.TheGaffer;
import co.mcme.thegaffer.utilities.PermissionsUtil;
import java.awt.Polygon;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.codehaus.jackson.annotate.JsonIgnore;

public class Job implements Listener {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String owner;
    @Getter
    @Setter
    private boolean running;
    @Getter
    @Setter
    private JobWarp warp;
    @Getter
    @Setter
    private ArrayList<String> helpers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> workers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> bannedWorkers = new ArrayList();
    @Getter
    @Setter
    private ArrayList<String> invitedWorkers = new ArrayList();
    @Getter
    @Setter
    private Long startTime;
    @Getter
    @Setter
    private String world;
    @Getter
    @Setter
    private boolean Private;
    @Getter
    @Setter
    @JsonIgnore
    private Polygon area;
    @Getter
    @Setter
    @JsonIgnore
    private Rectangle2D bounds;
    @Getter
    @Setter
    @JsonIgnore
    private boolean dirty;

    public Job(String name, String owner, boolean running, JobWarp warp, String world, boolean Private) {
        this.name = name;
        this.owner = owner;
        this.running = running;
        this.warp = warp;
        this.world = world;
        this.Private = Private;
        this.startTime = System.currentTimeMillis();
        Location bukkitLoc = warp.toBukkitLocation();
        int zbounds[] = {bukkitLoc.getBlockZ() - 250, bukkitLoc.getBlockZ() + 250};
        int xbounds[] = {bukkitLoc.getBlockX() - 250, bukkitLoc.getBlockX() + 250};
        this.area = new Polygon(xbounds, zbounds, xbounds.length);
        this.bounds = area.getBounds2D();
    }

    public Job() {

    }

    @JsonIgnore
    public OfflinePlayer getOwnerAsOfflinePlayer() {
        return TheGaffer.getServerInstance().getOfflinePlayer(owner);
    }

    @JsonIgnore
    public boolean isPlayerHelper(OfflinePlayer p) {
        return helpers.contains(p.getName());
    }

    @JsonIgnore
    public boolean isPlayerWorking(OfflinePlayer p) {
        return workers.contains(p.getName());
    }

    @JsonIgnore
    public World getBukkitWorld() {
        return TheGaffer.getServerInstance().getWorld(world);
    }
    
    @JsonIgnore
    public Player[] getWorkersAsPlayers() {
        ArrayList<Player> players = new ArrayList();
        for (String pName : workers) {
            OfflinePlayer p = TheGaffer.getServerInstance().getOfflinePlayer(pName);
            if (p.isOnline()) {
                players.add(p.getPlayer());
            }
        }
        return players.toArray(new Player[players.size()]);
    }

    public HelperResponse addHelper(OfflinePlayer p) {
        if (helpers.contains(p.getName())) {
            return HelperResponse.ALREADY_HELPER;
        }
        if (!p.isOnline()) {
            return HelperResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getCreatePermission())) {
            return HelperResponse.NO_PERMISSIONS;
        }
        helpers.add(p.getName());
        return HelperResponse.ADD_SUCCESS;
    }

    public HelperResponse removeHelper(OfflinePlayer p) {
        if (!helpers.contains(p.getName())) {
            return HelperResponse.NOT_HELPER;
        }
        helpers.remove(p.getName());
        return HelperResponse.REMOVE_SUCCESS;
    }

    public WorkerResponse addWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            return WorkerResponse.ALREADY_WORKER;
        }
        if (!p.isOnline()) {
            return WorkerResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return WorkerResponse.NO_PERMISSIONS;
        }
        if (Private && !(invitedWorkers.contains(p.getName()))) {
            return WorkerResponse.NOT_INVITED;
        }
        workers.add(p.getName());
        return WorkerResponse.ADD_SUCCESS;
    }

    public WorkerResponse removeWorker(OfflinePlayer p) {
        if (!workers.contains(p.getName())) {
            return WorkerResponse.NOT_WORKER;
        }
        workers.remove(p.getName());
        return WorkerResponse.REMOVE_SUCCESS;
    }

    public InviteResponse inviteWorker(OfflinePlayer p) {
        if (invitedWorkers.contains(p.getName())) {
            return InviteResponse.ALREADY_INVITED;
        }
        if (!p.isOnline()) {
            return InviteResponse.NOT_ONLINE;
        }
        if (!p.getPlayer().hasPermission(PermissionsUtil.getJoinPermission())) {
            return InviteResponse.NO_PERMISSIONS;
        }
        invitedWorkers.add(p.getName());
        return InviteResponse.ADD_SUCCESS;
    }

    public InviteResponse uninviteWorker(OfflinePlayer p) {
        if (!invitedWorkers.contains(p.getName())) {
            return InviteResponse.NOT_INVITED;
        }
        if (workers.contains(p.getName())) {
            workers.remove(p.getName());
        }
        invitedWorkers.remove(p.getName());
        return InviteResponse.REMOVE_SUCCESS;
    }

    public BanWorkerResponse banWorker(OfflinePlayer p) {
        if (workers.contains(p.getName())) {
            workers.remove(p.getName());
        }
        if (bannedWorkers.contains(p.getName())) {
            return BanWorkerResponse.ALREADY_BANNED;
        }
        bannedWorkers.add(p.getName());
        return BanWorkerResponse.BAN_SUCCESS;
    }

    public BanWorkerResponse unbanWorker(OfflinePlayer p) {
        if (bannedWorkers.contains(p.getName())) {
            return BanWorkerResponse.ALREADY_UNBANNED;
        }
        bannedWorkers.remove(p.getName());
        return BanWorkerResponse.UNBAN_SUCCESS;
    }

    public int sendToHelpers(String message) {
        int count = 0;
        for (String hName : helpers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(hName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(hName).getPlayer().sendMessage(message);
                count++;
            }
        }
        return count;
    }

    public int sendToWorkers(String message) {
        int count = 0;
        for (String wName : workers) {
            if (TheGaffer.getServerInstance().getOfflinePlayer(wName).isOnline()) {
                TheGaffer.getServerInstance().getOfflinePlayer(wName).getPlayer().sendMessage(message);
                count++;
            }
        }
        return count;
    }

    public int sendToAll(String message) {
        return sendToHelpers(message) + sendToWorkers(message);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeave(PlayerQuitEvent event) {
        if (event.getPlayer().getName().equals(owner)) {
            TheGaffer.scheduleOwnerTimeout(this);
        }
    }
}

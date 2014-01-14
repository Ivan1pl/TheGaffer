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

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

public class JobKit {

    @Getter
    @Setter
    private JobItem[] contents;
    @Getter
    @Setter
    private JobItem helmet;
    @Getter
    @Setter
    private JobItem chestplate;
    @Getter
    @Setter
    private JobItem pants;
    @Getter
    @Setter
    private JobItem boots;

    public void replaceInventory(Player p) {
        p.getInventory().clear();
        for (JobItem i : contents) {
            p.getInventory().addItem(i.toBukkitItem());
        }
        p.getInventory().setHelmet(helmet.toBukkitItem());
        p.getInventory().setChestplate(chestplate.toBukkitItem());
        p.getInventory().setLeggings(pants.toBukkitItem());
        p.getInventory().setBoots(boots.toBukkitItem());
        p.updateInventory();
    }
}

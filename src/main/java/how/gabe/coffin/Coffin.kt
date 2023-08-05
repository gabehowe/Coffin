package how.gabe.coffin

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.block.TileState
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin


class Coffin : JavaPlugin(), Listener {
    val key = NamespacedKey(this, "coffin")
    val coffins = mutableListOf<Block>()
    val contents = mutableMapOf<Block, Array<out ItemStack?>>()
    override fun onEnable() {
        // Plugin startup logic
        server.pluginManager.registerEvents(this, this)
        saveDefaultConfig()

    }

    override fun onDisable() {
        for (i in this.coffins) {
            i.type = Material.AIR
        }
    }

    @EventHandler
    fun onPlayerDeathEvent(event: PlayerDeathEvent) {
        var block = event.entity.location.block
        val noBurn = mutableListOf(
            Material.ANCIENT_DEBRIS
        )
        for (i in Material.entries) {
            if (i.name.lowercase().contains("netherite")) {
                noBurn += i
            }
        }

        var items = event.drops.toMutableList()
        var newDrops = items.toMutableList()
        if (block.type == Material.VOID_AIR) {
            return
        }
        if (block.type == Material.LAVA) {
            for (i in items) {
                if (i.type !in noBurn) {
                    newDrops.remove(i)
                }
            }
        }
        if (block.type != Material.AIR && block.type != Material.CAVE_AIR) {
            for (i in 0..64) {
                var testBlock =
                    Location(block.world, block.x.toDouble(), (block.y + i).toDouble(), block.z.toDouble()).block
                if (testBlock.type == Material.AIR || testBlock.type == Material.WATER) {
                    block = testBlock
                    break
                }

                testBlock = Location(block.world, block.x.toDouble(), (block.y - i).toDouble(), block.z.toDouble()).block

                if (testBlock.type == Material.AIR || testBlock.type == Material.WATER) {
                    block = testBlock
                    break
                }

            }
        }
        event.drops.clear()
        block.type = Material.WAXED_OXIDIZED_CUT_COPPER
        contents[block] = newDrops.toTypedArray()
        this.coffins += block
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, {
            if ((block.state as Barrel).persistentDataContainer.has(key)) {
                block.type = Material.AIR
            }
        }, config.getLong("despawn-timer") * 20L)
    }

    @EventHandler
    fun onCoffinExplodedByEntityEvent(event: EntityExplodeEvent) {
        for (i in ArrayList(event.blockList())) {
            if (i !in this.coffins) continue
            event.blockList().remove(i)
        }
    }

    @EventHandler
    fun onCoffinBreakEvent(event: BlockBreakEvent) {
        if (!dropInventory(event.block)) return
        event.isDropItems = false
    }

    @EventHandler
    fun onCoffinWhackEvent(event: BlockDamageEvent) {
        if (!dropInventory(event.block)) return
        event.block.type = Material.AIR
    }

    private fun dropInventory(block: Block): Boolean {
        if (block !in this.coffins) return false
        val inv = contents[block]
        if (inv != null) {
            for (i in inv) {
                if (i == null) continue
                block.world.dropItem(block.location + Location(block.world, 0.5, 0.0, 0.5), i)
            }
        }
        contents[block] = mutableListOf<ItemStack>().toTypedArray()
        return true
    }


}

private operator fun Location.plus(location: Location): Location {
    return Location(this.world, this.x + location.x, this.y + location.y, this.z + location.z)
}

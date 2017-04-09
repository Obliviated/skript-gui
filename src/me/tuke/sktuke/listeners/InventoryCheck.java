package me.tuke.sktuke.listeners;

import me.tuke.sktuke.util.InventoryUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import me.tuke.sktuke.TuSKe;
import me.tuke.sktuke.manager.gui.GUI;
import me.tuke.sktuke.manager.gui.GUIActionEvent;
import me.tuke.sktuke.manager.gui.GUIManager;
import me.tuke.sktuke.util.ReflectionUtils;

//TODO Separate all this events into parts and only active them when is used in scripts.
public class InventoryCheck implements Listener{
	private TuSKe instance;
	private GUIManager gm = TuSKe.getGUIManager();
	public InventoryCheck(TuSKe tuske){
		instance = tuske;
	}

	@EventHandler
	public void InventoryEvent(final InventoryClickEvent e) {
		if (getClickedInventory(e) != null && gm.isAllowedType(e.getClick()) && (gm.hasGUI(getClickedInventory(e)) || gm.hasGUI(e.getInventory()))){
			final Inventory click = getClickedInventory(e);
			Inventory inv = click;
			Integer slot = e.getSlot();
			if ((e.getAction().equals(InventoryAction.MOVE_TO_OTHER_INVENTORY) && !inv.getType().equals(e.getInventory().getType())) || e.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)){
				inv = e.getInventory();
				ItemStack i = (e.getAction().equals(InventoryAction.COLLECT_TO_CURSOR)) ? e.getCursor() : click.getItem(e.getSlot()) ; 
				slot = InventoryUtils.getSlotTo(inv, i);
			}
			if (gm.isGUI(inv, slot)){
				e.setCancelled(true);
				final GUI gui = gm.getGUI(click, e.getSlot(), e.getClick());
				if (gui != null && e.getInventory().getItem(e.getSlot()) != null && gui.runOnlyWith(e.getCursor())){
					if (gui.toCallEvent()){
						GUIActionEvent guie = new GUIActionEvent(e);
						Bukkit.getPluginManager().callEvent(guie);
						e.setCancelled(!guie.isCancelled());
					} else if(gui.toClose())
						Bukkit.getScheduler().scheduleSyncDelayedTask(instance, new Runnable(){
	
							@Override
							public void run() {
								//gm.removeAll(click);
								if (gui.getInventory() != null)
									e.getWhoClicked().openInventory(gui.getInventory());
								else
									e.getWhoClicked().closeInventory();
								if (gui.toRun())
									gui.getRunnable().run();
							}}, 0L);
					else if (gui.toRun())
						gui.getRunnable().run();
				}
			}
			
		}
	}
	
	@EventHandler
	public void InventoryClose(final InventoryCloseEvent e){
		if (TuSKe.getGUIManager().hasGUI(e.getInventory())){
			TuSKe.getGUIManager().removeAll(e.getInventory());
			Bukkit.getScheduler().runTaskLater(instance, new Runnable(){
	
				@Override
				public void run() {
					((Player)e.getPlayer()).updateInventory();				
				}}, 0L);
		}
	}
	@EventHandler
	public void InventoryDrag(InventoryDragEvent e){
		if (e.getInventory() != null && e.getInventorySlots() != null && e.getInventorySlots().size() > 0 && e.getWhoClicked() instanceof Player)
			for (Integer slot : e.getInventorySlots())
				if (TuSKe.getGUIManager().isGUI(e.getInventory(), slot)){
					e.setCancelled(true);
					return;
				}
				
		
	}

	@SuppressWarnings("unused")
	private boolean isAllowedTo(Inventory inv, ItemStack i, int slot){
		
		switch (inv.getType()){
		case ANVIL: return slot < 2;
		case BEACON: return i.getType().equals(Material.DIAMOND) || i.getType().equals(Material.IRON_INGOT) || i.getType().equals(Material.GOLD_INGOT) || i.getType().equals(Material.EMERALD);
		case BREWING: return i.getItemMeta() instanceof PotionMeta;
		default:
			break;
		}
		return true;
	}
	private Inventory getClickedInventory(InventoryClickEvent e){
		if (ReflectionUtils.hasMethod(InventoryClickEvent.class, "getClickedInventory"))
			return e.getClickedInventory();
		else if (e.getRawSlot() < 0)
			return null;
		else if ((e.getView().getTopInventory() != null) && (e.getRawSlot() < e.getView().getTopInventory().getSize()))
			return e.getView().getTopInventory();
		else 
			return e.getView().getBottomInventory();
	}
}

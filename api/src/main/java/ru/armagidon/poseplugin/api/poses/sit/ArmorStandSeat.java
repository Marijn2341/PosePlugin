package ru.armagidon.poseplugin.api.poses.sit;

import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spigotmc.event.entity.EntityDismountEvent;
import ru.armagidon.poseplugin.api.PosePluginAPI;
import ru.armagidon.poseplugin.api.ticking.Tickable;

import java.lang.reflect.Field;
import java.util.function.BiConsumer;

public class ArmorStandSeat implements Listener, Tickable
{

    private final BiConsumer<EntityDismountEvent, ArmorStandSeat> execute;
    private @Setter BiConsumer<EntityDismountEvent, ArmorStand> teleport = (event, armorStandSeat) -> {};
    private ArmorStand seat;
    private final Player sitter;

    private boolean handledTeleport = false;


    public ArmorStandSeat(Player sitter, BiConsumer<EntityDismountEvent, ArmorStandSeat> onDismount) {
        Bukkit.getPluginManager().registerEvents(this, PosePluginAPI.getAPI().getPlugin());
        this.sitter = sitter;
        this.execute = onDismount;
    }

    public void takeASeat() {
        Location location = sitter.getLocation().clone();
        seat = sitter.getWorld().spawn(location.clone().subtract(0, 0.2D, 0), ArmorStand.class, (armorStand -> {
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setSmall(true);
            armorStand.setVisible(false);
            armorStand.setCollidable(false);
            armorStand.addPassenger(sitter);
        }));
        PosePluginAPI.getAPI().getTickingBundle().addToTickingBundle(ArmorStandSeat.class, this);
    }

    public void standUp() {
        HandlerList.unregisterAll(this);
        sitter.eject();
        seat.addScoreboardTag("PPGC");
        seat.remove();
        sitter.teleport(seat.getLocation().clone().add(0, 0.2D,0).setDirection(sitter.getLocation().getDirection()));
        if( PosePluginAPI.getAPI().getPlugin().isEnabled() ) {
            Bukkit.getScheduler().runTaskLater(PosePluginAPI.getAPI().getPlugin(), () ->
                    sitter.teleport(seat.getLocation().clone().add(0, 0.2D,0).setDirection(sitter.getLocation().getDirection())), 1);
        }
        PosePluginAPI.getAPI().getTickingBundle().removeFromTickingBundle(ArmorStandSeat.class, this);
    }

    public void pushBack(){
        Bukkit.getScheduler().runTaskLater(PosePluginAPI.getAPI().getPlugin(), () -> seat.addPassenger(sitter), 3L);
    }

    @EventHandler
    private void armorManipulate(PlayerArmorStandManipulateEvent event){
        if(event.getRightClicked().equals(seat)){
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void playerStoodUpEvent(EntityDismountEvent event){
        if( event.getEntity().getType().equals(EntityType.PLAYER) && event.getDismounted().getType().equals(EntityType.ARMOR_STAND) ){
            ArmorStand stand = (ArmorStand) event.getDismounted();
            Player player = (Player) event.getEntity();
            if( player.getUniqueId().equals(sitter.getUniqueId()) && stand.equals(seat) ){
                //If dismounting was caused by teleport, don't fire event
                if (handledTeleport) {
                    Bukkit.getScheduler().runTaskLater(PosePluginAPI.getAPI().getPlugin(), () -> {
                        seat.teleport(sitter.getLocation().clone().subtract(0, 0.2D, 0));
                        pushBack();
                        teleport.accept(event, seat);
                    }, 1);
                    handledTeleport = false;
                    return;
                }
                //If player dismounted from seat, do stuff
                execute.accept(event, this);
            }
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getPlayer().equals(sitter) && event.getPlayer().getVehicle() != null && event.getPlayer().getVehicle().equals(seat)) {
            handledTeleport = true;
        }
    }

    private void rotate(){
        if (seat == null) return;
        try {
            Object vanillaStand = seat.getClass().getMethod("getHandle").invoke(seat);
            Field yawF = vanillaStand.getClass().getField("yaw");
            yawF.set(vanillaStand, sitter.getLocation().getYaw());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick(){
        rotate();
        if( !seat.isDead() && !seat.getPassengers().contains(sitter) && seat.getScoreboardTags().contains("PPGC")){
            seat.remove();
        }
    }
}

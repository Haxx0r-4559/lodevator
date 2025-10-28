package me.haxx0r.lodevator;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

public class Lodevator extends JavaPlugin implements Listener {
  private Config cfg;
  private PlayerPrefs prefs;

  private Map<UUID, List<Location>> locations;

  @Override
  public void onEnable() {
    // Load config and data
    cfg = new Config(this);
    prefs = new PlayerPrefs(this, cfg);
    // Setup interaction rate-limiter
    locations = new TreeMap<>();
    Bukkit.getScheduler().runTaskTimer(this, () -> locations.clear(), 1L, 1L);
    // Register command and events
    registerCommand();
    getServer().getPluginManager().registerEvents(this, this);
  }

  private void registerCommand() {
    LiteralCommandNode<CommandSourceStack> root = Commands.literal("lodevator")
        .executes(ctx -> {
          CommandSender sender = ctx.getSource().getSender();
          Entity executor = ctx.getSource().getExecutor();

          if (!(executor instanceof Player player)) {
            sender.sendPlainMessage("You are unable to use a lodevator block!");
            return Command.SINGLE_SUCCESS;
          }

          if (sender == executor) {
            sendMessage(player, player, cfg.getCheckMsg());
            return Command.SINGLE_SUCCESS;
          }

          sendMessage(sender, player, cfg.getCheckOtherMsg());
          return Command.SINGLE_SUCCESS;
        })
        .then(Commands.literal("toggle")
            .executes(ctx -> {
              CommandSender sender = ctx.getSource().getSender();
              Entity executor = ctx.getSource().getExecutor();

              if (!(executor instanceof Player player)) {
                sender.sendPlainMessage("You are unable to use a lodevator block!");
                return Command.SINGLE_SUCCESS;
              }

              prefs.toggle(player.getUniqueId());
              sendMessage(player, player, cfg.getToggleMsg());

              if (sender != executor)
                sendMessage(sender, player, cfg.getToggleOtherMsg());

              return Command.SINGLE_SUCCESS;
            }))
        .then(Commands.literal("reload")
            .requires(sender -> sender.getSender().hasPermission("lodevator.reload"))
            .executes(ctx -> {
              cfg.reload();
              prefs.reload();

              ctx.getSource().getSender()
                  .sendPlainMessage("Lodevator configuration & data reloaded.");
              return Command.SINGLE_SUCCESS;
            }))
        .build();

    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
      commands.registrar().register(root);
    });
  }

  private void sendMessage(CommandSender target, Player p, String msg) {
    Trigger curr = prefs.get(p.getUniqueId());

    target.sendRichMessage(msg,
        Placeholder.parsed("block", "<lang:" + cfg.getMaterial().getBlockTranslationKey() + ">"),
        Placeholder.parsed("curr_up", curr.up()), Placeholder.parsed("curr_down", curr.down()),
        Placeholder.parsed("next_up", curr == Trigger.CLICK ? Trigger.MOVE.up() : Trigger.CLICK.up()),
        Placeholder.parsed("next_down", curr == Trigger.CLICK ? Trigger.MOVE.down() : Trigger.CLICK.down()),
        Placeholder.unparsed("playername", p.getName()));
  }

  private boolean hasInteracted(Player player, Block block) {
    List<Location> existingOnes = locations.computeIfAbsent(player.getUniqueId(), u -> new LinkedList<>());
    if (existingOnes.contains(block.getLocation()))
      return true;

    existingOnes.add(block.getLocation());
    return false;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void lodevatorSneak(PlayerToggleSneakEvent event) {
    if (!event.isSneaking())
      return;

    Player p = event.getPlayer();
    if (prefs.get(p.getUniqueId()) != Trigger.MOVE)
      return;

    Block below = Utils.getBelow(p);
    if (below.getType() != cfg.getMaterial())
      return;

    attemptDown(p, below);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void lodevatorJump(PlayerJumpEvent event) {
    Player p = event.getPlayer();
    if (prefs.get(p.getUniqueId()) != Trigger.MOVE)
      return;

    Block below = Utils.getBelow(p);
    if (below.getType() != cfg.getMaterial())
      return;

    attemptUp(p, below);
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void lodevatorInteract(PlayerInteractEvent event) {
    Player p = event.getPlayer();

    if (prefs.get(p.getUniqueId()) != Trigger.CLICK)
      return;

    Action action = event.getAction();
    Block b = event.getClickedBlock();

    if (action == Action.RIGHT_CLICK_BLOCK) {
      if (hasInteracted(p, b))
        return; // Already processed during this tick
    } else if (action != Action.LEFT_CLICK_BLOCK)
      return; // Not a left or right click

    Block below = Utils.getBelow(p);
    if (below.getType() != cfg.getMaterial() || !b.equals(below))
      return; // Player did not click a lodevator block underneath them

    if (action == Action.LEFT_CLICK_BLOCK)
      attemptDown(p, below);
    else
      attemptUp(p, below);
  }

  private void attemptUp(Player p, Block below) {
    for (int y = below.getY() + 1; y <= below.getWorld().getMaxHeight(); y++)
      if (doTeleport(below, p, y))
        return;

    sendMessage(p, p, cfg.getUpFailMsg());
  }

  private void attemptDown(Player p, Block below) {
    for (int y = below.getY() - 1; y > below.getWorld().getMinHeight(); y--)
      if (doTeleport(below, p, y))
        return;

    sendMessage(p, p, cfg.getDownFailMsg());
  }

  private boolean doTeleport(Block source, Player player, int y) {
    World world = source.getWorld();
    Block target = world.getBlockAt(source.getX(), y, source.getZ());
    if (target.getType() != cfg.getMaterial() || Utils.isSolid(target.getRelative(BlockFace.UP).getLocation()))
      return false; // Not a valid destination

    // Teleport the player
    Location oldLoc = player.getLocation();
    Location newLoc = target.getLocation().clone().add(0.5, 1.02, 0.5);
    newLoc.setYaw(oldLoc.getYaw());
    newLoc.setPitch(oldLoc.getPitch());

    world.playSound(cfg.getTriggerSound(), oldLoc.x(), oldLoc.y(), oldLoc.z());
    world.playSound(cfg.getTriggerSound(), newLoc.x(), newLoc.y(), newLoc.z());

    player.teleport(newLoc);
    return true;
  }
}

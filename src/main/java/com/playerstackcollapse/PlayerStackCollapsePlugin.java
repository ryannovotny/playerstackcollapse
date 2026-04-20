package com.playerstackcollapse;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Player;
import net.runelite.api.Varbits;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
        name = "Player Stack Collapse",
        description = "Collapses right-click menus for multiple players into sub-menus.",
        tags = {"menu", "player", "stack", "submenu", "clutter"}
)
public class PlayerStackCollapsePlugin extends Plugin
{
    // Dummy ID to Tag Custom Menu to Identify Later On
    private static final int CUSTOM_PARENT_IDENTIFIER = 990099;

    // High-Traffic Areas
    private static final int REGION_GE = 12598;
    private static final Set<Integer> REGIONS_WINTERTODT = Set.of(6461, 6462);
    private static final int REGION_GOTR = 14484;
    private static final Set<Integer> REGIONS_PEST_CONTROL = Set.of(10536, 10537, 10538, 10539);

    // Get Game Client and Configuration File
    @Inject
    private Client client;

    @Inject
    private PlayerStackCollapseConfig config;

    @Provides
    PlayerStackCollapseConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(PlayerStackCollapseConfig.class);
    }

    // Ensure Other Plug-Ins Load First
    @Subscribe(priority = -1)
    public void onMenuOpened(MenuOpened event)
    {
        // Skip Plug-In If Shift is Held and Shift Bypass Enabled
        if (config.shiftBypass() && client.isKeyPressed(KeyCode.KC_SHIFT))
        {
            return;
        }

        // Skip Plug-In if in Wilderness / PVP
        if (config.disableInPvp() && client.getVarbitValue(Varbits.IN_WILDERNESS) == 1)
        {
            return;
        }

        // Check if Location Filtering Enabled
        if (config.locationFilterEnabled())
        {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null)
            {
                return;
            }

            // Get Current Player Coordinate
            int regionId = localPlayer.getWorldLocation().getRegionID();
            boolean validLocation = false;

            // Check if In Setting Enabled Region
            if (config.locGe() && regionId == REGION_GE) validLocation = true;
            else if (config.locWintertodt() && REGIONS_WINTERTODT.contains(regionId)) validLocation = true;
            else if (config.locGotr() && regionId == REGION_GOTR) validLocation = true;
            else if (config.locPestControl() && REGIONS_PEST_CONTROL.contains(regionId)) validLocation = true;

            // Exit if Not In Zone
            if (!validLocation)
            {
                return;
            }
        }

        // Grab Current Menu Entries
        MenuEntry[] entries = client.getMenu().getMenuEntries();

        // Preserve Native Game Order of Players
        Map<Player, List<CachedMenuEntry>> playerEntriesMap = new LinkedHashMap<>();

        // Split Non-Player Entries into Junk (bottom) and Actionable (top)
        List<MenuEntry> lowPriorityEntries = new ArrayList<>();
        List<CachedMenuEntry> actionableEntries = new ArrayList<>();
        CachedMenuEntry walkHereEntry = null;

        for (MenuEntry entry : entries)
        {
            String option = entry.getOption();

            // Isolate "Walk Here" Entry
            if (entry.getType() == MenuAction.WALK)
            {
                walkHereEntry = new CachedMenuEntry(entry);
            }
            // Check if Menu Updated Dynamically so Entries aren't Incorrectly Placed in Sub Menus
            else if (entry.getType() == MenuAction.RUNELITE && entry.getIdentifier() == CUSTOM_PARENT_IDENTIFIER)
            {
                continue;
            }
            // Group Entries Belonging to Specific Players
            else if (entry.getPlayer() != null)
            {
                playerEntriesMap.computeIfAbsent(entry.getPlayer(), k -> new ArrayList<>()).add(new CachedMenuEntry(entry));
            }
            // Push 'Cancel' and 'Examine' to Bottom of List
            else if (entry.getType() == MenuAction.CANCEL || (option != null && option.equalsIgnoreCase("Examine")))
            {
                lowPriorityEntries.add(entry);
            }
            // Actions (Bank, Fish, Take, etc.) Pushed to Top of List
            else
            {
                actionableEntries.add(new CachedMenuEntry(entry));
            }
        }

        // Check if Enough Players to Activate Plug-In
        if (playerEntriesMap.size() < config.minimumPlayers())
        {
            return;
        }

        // ---- BUILD MENU ----
        // 1. BASE: Cancel & Examine
        client.getMenu().setMenuEntries(lowPriorityEntries.toArray(new MenuEntry[0]));

        // Check if 'Hide Player: Text' Enabled
        String parentOption = config.hidePlayerPrefix() ? "" : "Player:";

        // 2. MIDDLE: Players
        for (Map.Entry<Player, List<CachedMenuEntry>> mapEntry : playerEntriesMap.entrySet())
        {
            List<CachedMenuEntry> pEntries = mapEntry.getValue();

            // Get HTML Color String for Player Levels
            String coloredTarget = pEntries.get(0).target;
            if (coloredTarget == null)
            {
                coloredTarget = "Unknown";
            }

            // Append Player Entry to End of Array (Top)
            MenuEntry parentEntry = client.getMenu().createMenuEntry(-1)
                    .setOption(parentOption)
                    .setTarget(coloredTarget)
                    .setType(MenuAction.RUNELITE)
                    .setIdentifier(CUSTOM_PARENT_IDENTIFIER); // Dummy ID

            Menu submenu = parentEntry.createSubMenu();

            // Populate Sub Menu with Cached Actions
            for (CachedMenuEntry cached : pEntries)
            {
                MenuEntry subEntry = submenu.createMenuEntry(-1)
                        .setOption(cached.option)
                        .setTarget("") // Remove Username / Level from String for Clean Sub Menu
                        .setType(cached.type)
                        .setIdentifier(cached.identifier)
                        .setParam0(cached.param0)
                        .setParam1(cached.param1)
                        .setForceLeftClick(cached.forceLeftClick)
                        .setDeprioritized(cached.deprioritized);

                // Re-Attach Logic from Other Plug-Ins
                if (cached.onClick != null)
                {
                    subEntry.onClick(cached.onClick);
                }
            }
        }

        // 3. NEAR-TOP: Walk here
        if (walkHereEntry != null)
        {
            MenuEntry newWalkEntry = client.getMenu().createMenuEntry(-1)
                    .setOption(walkHereEntry.option)
                    .setTarget("") // Remove Username / Level from Walk Here String
                    .setType(walkHereEntry.type)
                    .setIdentifier(walkHereEntry.identifier)
                    .setParam0(walkHereEntry.param0)
                    .setParam1(walkHereEntry.param1)
                    .setForceLeftClick(walkHereEntry.forceLeftClick)
                    .setDeprioritized(walkHereEntry.deprioritized);

            if (walkHereEntry.onClick != null)
            {
                newWalkEntry.onClick(walkHereEntry.onClick);
            }
        }

        // 4. TOP: Actionable Items
        for (CachedMenuEntry cached : actionableEntries)
        {
            MenuEntry newActionEntry = client.getMenu().createMenuEntry(-1)
                    .setOption(cached.option)
                    .setTarget(cached.target)
                    .setType(cached.type)
                    .setIdentifier(cached.identifier)
                    .setParam0(cached.param0)
                    .setParam1(cached.param1)
                    .setForceLeftClick(cached.forceLeftClick)
                    .setDeprioritized(cached.deprioritized);

            if (cached.onClick != null)
            {
                newActionEntry.onClick(cached.onClick);
            }
        }
    }

    // Cached Menu Decouples Data from Engine to Ensure No Straggling Entries
    private static class CachedMenuEntry
    {
        public final String option;
        public final String target;
        public final MenuAction type;
        public final int identifier;
        public final int param0;
        public final int param1;
        public final boolean forceLeftClick;
        public final boolean deprioritized;
        public final Consumer<MenuEntry> onClick;

        public CachedMenuEntry(MenuEntry entry)
        {
            this.option = entry.getOption();
            this.target = entry.getTarget();
            this.type = entry.getType();
            this.identifier = entry.getIdentifier();
            this.param0 = entry.getParam0();
            this.param1 = entry.getParam1();
            this.forceLeftClick = entry.isForceLeftClick();
            this.deprioritized = entry.isDeprioritized();
            this.onClick = entry.onClick();
        }
    }
}

package com.playerstackcollapse;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("playerstackcollapse")
public interface PlayerStackCollapseConfig extends Config
{
    @ConfigItem(
            keyName = "hidePlayerPrefix",
            name = "Hide 'Player:' Prefix",
            description = "Removes the 'Player:' text from the main menu, leaving only the username.",
            position = 1
    )
    default boolean hidePlayerPrefix()
    {
        return false; // Default False
    }

    @ConfigItem(
            keyName = "minimumPlayers",
            name = "Minimum Stack Size",
            description = "How many players must be on the same tile before the menu collapses.",
            position = 2
    )
    default int minimumPlayers()
    {
        return 2; // Default 2
    }

    @ConfigItem(
            keyName = "shiftBypass",
            name = "Shift-Click Bypass",
            description = "Holding Shift while right-clicking will temporarily disable the plugin.",
            position = 3
    )
    default boolean shiftBypass()
    {
        return true; // Default True
    }

    @ConfigItem(
            keyName = "disableInPvp",
            name = "Disable in PvP Areas",
            description = "Automatically disables the plugin when you are in the Wilderness or a PvP world.",
            position = 4
    )
    default boolean disableInPvp()
    {
        return true; // Default True
    }

    @ConfigSection(
            name = "Location Filters",
            description = "Restrict the plugin to only collapse menus in specific busy areas.",
            position = 4
    )
    String locationSection = "locationSection";

    @ConfigItem(
            keyName = "locationFilterEnabled",
            name = "Enable Location Filter",
            description = "If ON, the plugin will ONLY work in the locations selected below. If OFF, it works everywhere.",
            position = 5,
            section = locationSection
    )
    default boolean locationFilterEnabled()
    {
        return false; // Default False
    }

    @ConfigItem(
            keyName = "locGe",
            name = "Grand Exchange",
            description = "Enable collapsing at the Grand Exchange.",
            position = 6,
            section = locationSection
    )
    default boolean locGe()
    {
        return true; // Default True
    }

    @ConfigItem(
            keyName = "locWintertodt",
            name = "Wintertodt",
            description = "Enable collapsing at the Wintertodt camp and boss room.",
            position = 7,
            section = locationSection
    )
    default boolean locWintertodt()
    {
        return true; // Default True
    }

    @ConfigItem(
            keyName = "locGotr",
            name = "Guardians of the Rift",
            description = "Enable collapsing inside the GotR minigame.",
            position = 8,
            section = locationSection
    )
    default boolean locGotr()
    {
        return true; // Default True
    }

    @ConfigItem(
            keyName = "locPestControl",
            name = "Pest Control",
            description = "Enable collapsing at the Pest Control outpost and landings.",
            position = 9,
            section = locationSection
    )
    default boolean locPestControl()
    {
        return true; // Default True
    }
}
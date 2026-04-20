package com.playerstackcollapse;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PlayerStackCollapsePluginTest
{
    public static void main(String[] args) throws Exception
    {
        // This tells the development client to load your specific plugin
        ExternalPluginManager.loadBuiltin(PlayerStackCollapsePlugin.class);

        // This launches the RuneLite client
        RuneLite.main(args);
    }
}
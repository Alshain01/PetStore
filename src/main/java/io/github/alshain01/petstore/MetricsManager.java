package io.github.alshain01.petstore;

import org.bukkit.plugin.Plugin;

import io.github.alshain01.petstore.Metrics.Graph;
import java.io.IOException;

public class MetricsManager {
    public static void StartMetrics(final Plugin plugin) {
        try {
            final Metrics metrics = new Metrics(plugin);

            final Graph transGraph = metrics.createGraph("Animal Transactions");
            transGraph.addPlotter(new Metrics.Plotter("For Sale") {
                @Override
                public int getValue() {
                    return ((PetStore)plugin).sales.getCount();
                }
            });

            transGraph.addPlotter(new Metrics.Plotter("Give Away") {
                @Override
                public int getValue() {
                    return ((PetStore)plugin).give.getCount();
                }
            });
        } catch (final IOException e) {
            plugin.getLogger().info(e.getMessage());
        }
    }
}

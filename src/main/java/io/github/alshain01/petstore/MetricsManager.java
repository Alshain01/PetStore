package io.github.alshain01.petstore;

import org.bukkit.plugin.Plugin;

import io.github.alshain01.petstore.Metrics.Graph;
import java.io.IOException;

public class MetricsManager {
    static void StartMetrics(final Plugin plugin) {
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

            /*
			 * Auto Update settings
			 */
            final Graph updateGraph = metrics.createGraph("Update Configuration");
            if (!plugin.getConfig().getBoolean("flags.Update.Check")) {
                updateGraph.addPlotter(new Metrics.Plotter("No Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else if (!plugin.getConfig().getBoolean("flags.Update.Download")) {
                updateGraph.addPlotter(new Metrics.Plotter("Check for Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else {
                updateGraph.addPlotter(new Metrics.Plotter("Download Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }

            /*
			 * Economy Graph
			 */
            final Graph econGraph = metrics.createGraph("Economy Enabled");
            econGraph.addPlotter(new Metrics.Plotter(PetStore.isEconomy() ? "Yes" : "No") {
                @Override
                public int getValue() {
                    return 1;
                }
            });

            metrics.start();
        } catch (final IOException e) {
            plugin.getLogger().info(e.getMessage());
        }
    }
}

package io.github.alshain01.petstore.metrics;

import io.github.alshain01.petstore.PetStore;

import io.github.alshain01.petstore.metrics.Metrics.Graph;
import java.io.IOException;

public class MetricsManager {
    public static void StartMetrics(final PetStore plugin) {
        try {
            final Metrics metrics = new Metrics(plugin);

            final Graph transGraph = metrics.createGraph("Animal Transactions");

            if(PetStore.isEconomy()) {
                transGraph.addPlotter(new Metrics.Plotter("For Sale") {
                    @Override
                    public int getValue() {
                        return plugin.getSalesCount();
                    }
                });
            }

            transGraph.addPlotter(new Metrics.Plotter("Give Away") {
                @Override
                public int getValue() {
                    return plugin.getGiveCount();
                }
            });

            /*
			 * Auto Update settings
			 */
            final Graph updateGraph = metrics.createGraph("Update Configuration");
            if (!plugin.getConfig().getBoolean("PetStore.Update.Check")) {
                updateGraph.addPlotter(new Metrics.Plotter("No Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else if (!plugin.getConfig().getBoolean("PetStore.Update.Download")) {
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
            metrics.createGraph("Economy Enabled").addPlotter(new Metrics.Plotter(PetStore.isEconomy() ? "Yes" : "No") {
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

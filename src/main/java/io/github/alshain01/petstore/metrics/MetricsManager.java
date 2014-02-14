package io.github.alshain01.petstore.metrics;

import io.github.alshain01.petstore.PetStore;

import io.github.alshain01.petstore.metrics.Metrics.Graph;
import java.io.IOException;

public class MetricsManager {
    public static void StartMetrics(final PetStore plugin) {
        try {
            final Metrics metrics = new Metrics(plugin);

            Graph graph = metrics.createGraph("Animal Transactions");

            if(PetStore.isEconomy()) {
                graph.addPlotter(new Metrics.Plotter("For Sale") {
                    @Override
                    public int getValue() {
                        return plugin.getSalesCount();
                    }
                });
            }

            graph.addPlotter(new Metrics.Plotter("Give Away") {
                @Override
                public int getValue() {
                    return plugin.getGiveCount();
                }
            });

            /*
			 * Auto Update settings
			 */
            graph = metrics.createGraph("Update Configuration");
            if (!plugin.getConfig().getBoolean("Update.Check")) {
                graph.addPlotter(new Metrics.Plotter("No Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else if (!plugin.getConfig().getBoolean("Update.Download")) {
                graph.addPlotter(new Metrics.Plotter("Check for Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            } else {
                graph.addPlotter(new Metrics.Plotter("Download Updates") {
                    @Override
                    public int getValue() {
                        return 1;
                    }
                });
            }

            /*
			 * Economy Graph
			 */
            graph = metrics.createGraph("Economy Enabled");
            graph.addPlotter(new Metrics.Plotter(PetStore.isEconomy() ? "Yes" : "No") {
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

package com.jtprince.coordinateoffset;

import com.jtprince.coordinateoffset.provider.ConstantOffsetProvider;
import com.jtprince.coordinateoffset.provider.RandomOffsetProvider;
import com.jtprince.coordinateoffset.provider.ZeroAtLocationOffsetProvider;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;

import java.util.HashMap;
import java.util.Map;

public class MetricsWrapper {
    // https://bstats.org/plugin/bukkit/CoordinateOffset/19988
    private static final int BSTATS_PLUGIN_METRICS_ID = 19988;

    public static void reportMetrics(CoordinateOffset plugin) {
        Metrics metrics = new Metrics(plugin, BSTATS_PLUGIN_METRICS_ID);

        metrics.addCustomChart(new DrilldownPie("default_offset_provider", () -> {
            Map<String, Map<String, Integer>> result = new HashMap<>();
            OffsetProvider defaultProvider = plugin.getOffsetProviderManager().getDefaultProvider();
            if (defaultProvider instanceof ConstantOffsetProvider) {
                result.put("ConstantOffsetProvider", Map.of("ConstantOffsetProvider", 1));
            } else if (defaultProvider instanceof RandomOffsetProvider randomOffsetProvider) {
                Map<String, Integer> inner = new HashMap<>();
                if (randomOffsetProvider.isPersistent()) {
                    inner.put("Persistent", 1);
                } else {
                    inner.put("Not Persistent", 1);
                }
                if (randomOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.DEATH_RESPAWN)) {
                    inner.put("Reset on Death", 1);
                }
                if (randomOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.WORLD_CHANGE)) {
                    inner.put("Reset on World Change", 1);
                }
                if (randomOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.DISTANT_TELEPORT)) {
                    inner.put("Reset on Teleport", 1);
                }
                result.put("RandomOffsetProvider", inner);
            } else if (defaultProvider instanceof ZeroAtLocationOffsetProvider zeroAtLocationOffsetProvider) {
                Map<String, Integer> inner = new HashMap<>();
                if (zeroAtLocationOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.DEATH_RESPAWN)) {
                    inner.put("Zero on Death", 1);
                }
                if (zeroAtLocationOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.WORLD_CHANGE)) {
                    inner.put("Zero on World Change", 1);
                }
                if (zeroAtLocationOffsetProvider.getResetConfig().resetOn(OffsetProviderContext.ProvideReason.DISTANT_TELEPORT)) {
                    inner.put("Zero on Teleport", 1);
                }
                result.put("ZeroAtLocationOffsetProvider", inner);
            } else {
                // Intentionally obfuscate the name of any extensions made to CoordinateOffset.
                result.put("Custom Provider", Map.of("Unknown Offset Provider", 1));
            }
            return result;
        }));

        metrics.addCustomChart(new SimplePie("world_border_obfuscation", () -> {
            if (plugin.getWorldBorderObfuscator().enableObfuscation()) {
                return "enabled";
            } else {
                return "disabled";
            }
        }));
    }
}

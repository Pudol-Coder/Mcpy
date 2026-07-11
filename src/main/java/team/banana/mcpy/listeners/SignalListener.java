package team.banana.mcpy.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import team.banana.mcpy.events.McpySignalEvent;

/**
 * 파이썬 -> 서버 신호를 처리하는 리스너.
 * "linkrequest:" 같은 세부 로직은 Skript(.sk)의 "on mcpy signal received"에서 처리하므로,
 * 여기서는 가장 기본적인 "broadcast:" 신호만 다룬다.
 */
public class SignalListener implements Listener {

    @EventHandler
    public void onSignal(McpySignalEvent event) {
        String signal = event.getSignal();
        String source = event.getSourceScript();

        Bukkit.getLogger().info("[Mcpy] '" + source + "'로부터 신호 수신: " + signal);

        if (signal.startsWith("broadcast:")) {
            String message = signal.substring("broadcast:".length());
            Bukkit.broadcastMessage("§b[Mcpy] " + message);
        }
    }
}

package team.banana.mcpy.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import team.banana.mcpy.events.McpySignalEvent;

/**
 * 파이썬 -> 서버 신호를 실제로 처리하는 예시 리스너.
 * 신호 내용(content)을 어떻게 파싱하고 어떤 동작을 할지는 자유롭게 커스터마이징하면 된다.
 * 예: "broadcast:서버 재시작 5분 전" 같은 형태로 규칙을 정해서 파싱
 */
public class SignalListener implements Listener {

    @EventHandler
    public void onSignal(McpySignalEvent event) {
        String signal = event.getSignal();
        String source = event.getSourceScript();

        Bukkit.getLogger().info("[Mcpy] '" + source + "'로부터 신호 수신: " + signal);

        // 예시: "broadcast:메시지" 형식이면 서버 전체에 방송
        if (signal.startsWith("broadcast:")) {
            String message = signal.substring("broadcast:".length());
            Bukkit.broadcastMessage("§b[Mcpy] " + message);
        }
    }
}

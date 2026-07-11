package team.banana.mcpy.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import team.banana.mcpy.Mcpy;

/**
 * 플레이어 채팅을 감지해서 파이썬 쪽(to-python)으로 신호를 보낸다.
 * 형식: "chat:플레이어이름:메시지내용"
 * 디스코드 봇 스크립트가 이 신호를 받아서 디스코드 채널로 전달하면 된다.
 */
public class ChatBridgeListener implements Listener {

    private final Mcpy plugin;

    public ChatBridgeListener(Mcpy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // ':' 이 메시지에 들어있으면 파싱이 꼬일 수 있으니 그대로 두되,
        // 파이썬 쪽에서 split(":", 2)로 앞 두 개만 자르면 문제 없음
        plugin.getSignalManager().sendSignal("chat:" + playerName + ":" + message);
    }
}

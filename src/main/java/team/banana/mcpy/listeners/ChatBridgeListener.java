package team.banana.mcpy.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import team.banana.mcpy.Mcpy;

/**
 * 플레이어 채팅을 감지해서 파이썬 쪽(to-python)으로 신호를 보낸다.
 * 형식: "chat:실제유저네임:표시이름(닉네임):메시지내용"
 *
 * 실제 유저네임은 아바타(스킨) 이미지를 가져오는 용도로,
 * 표시이름은 닉네임 플러그인 등으로 바뀐 이름을 디스코드에 그대로 보여주는 용도로 쓴다.
 */
public class ChatBridgeListener implements Listener {

    private final Mcpy plugin;

    public ChatBridgeListener(Mcpy plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String playerName = event.getPlayer().getName();

        // getDisplayName()은 닉네임 플러그인이 바꿔둔 이름을 그대로 반영한다.
        // 색상 코드(§)가 섞여 있을 수 있어 디스코드에 보내기 전에 제거한다.
        String displayName = ChatColor.stripColor(event.getPlayer().getDisplayName());
        if (displayName == null || displayName.isBlank()) {
            displayName = playerName;
        }

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // ':' 이 메시지에 들어있으면 파싱이 꼬일 수 있으니, 파이썬 쪽에서
        // "chat:" 을 뗀 나머지를 split(":", 2)로 앞 두 개(유저네임, 표시이름)만 자르고
        // 나머지 전부를 메시지로 취급하면 안전하다.
        plugin.getSignalManager().sendSignal("chat:" + playerName + ":" + displayName + ":" + message);
    }
}

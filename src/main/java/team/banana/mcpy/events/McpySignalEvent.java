package team.banana.mcpy.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * 파이썬 스크립트 -> 마인크래프트 서버로 신호가 도착했을 때 발생하는 이벤트.
 * 다른 플러그인/리스너에서 이 이벤트를 구독해서 신호 내용에 따라 동작을 처리할 수 있다.
 */
public class McpySignalEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String sourceScript;
    private final String signal;

    public McpySignalEvent(String sourceScript, String signal) {
        this.sourceScript = sourceScript;
        this.signal = signal;
    }

    public String getSourceScript() {
        return sourceScript;
    }

    public String getSignal() {
        return signal;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

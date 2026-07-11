package team.banana.mcpy.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.bukkit.event.Event;
import team.banana.mcpy.events.McpySignalEvent;

/**
 * Skript 문법: on mcpy signal received:
 * 파이썬 -> 서버 신호(McpySignalEvent)가 도착하면 실행된다.
 * 스크립트 안에서는 "event-string" 표현식으로 신호 내용을 꺼낼 수 있다.
 *
 * 예시 (.sk):
 * on mcpy signal received:
 *     broadcast "받은 신호: %event-string%"
 */
public class EvtMcpySignal extends SkriptEvent {

    static {
        Skript.registerEvent("Mcpy Signal Received", EvtMcpySignal.class, McpySignalEvent.class,
                "[mcpy] signal received");
    }

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return true;
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "mcpy signal received";
    }
}

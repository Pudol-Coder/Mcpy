package team.banana.mcpy.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.util.Kleenean;
import org.bukkit.event.Event;
import team.banana.mcpy.Mcpy;

/**
 * Skript 문법: send mcpy signal "내용"
 * 파이썬 스크립트(to-python 폴더 감시 중인 쪽)로 신호를 보낸다.
 */
@Name("Send Mcpy Signal")
@Description("파이썬 스크립트로 신호를 보낸다. Mcpy 플러그인이 필요하다.")
@Examples({"send mcpy signal \"broadcast:안녕!\"", "send mcpy signal \"chat:%player%:%message%\""})
@Since("1.2")
public class EffSendMcpySignal extends Effect {

    static {
        Skript.registerEffect(EffSendMcpySignal.class, "send mcpy signal %string%");
    }

    private Expression<String> signal;

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        signal = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String value = signal.getSingle(event);
        if (value != null) {
            Mcpy.getInstance().getSignalManager().sendSignal(value);
        }
    }

    @Override
    public String toString(Event event, boolean debug) {
        return "send mcpy signal " + signal.toString(event, debug);
    }
}

package team.banana.mcpy.listeners;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import team.banana.mcpy.Mcpy;

import java.util.ArrayList;
import java.util.List;

/**
 * /mcpy 명령어에 탭 자동완성을 제공한다.
 *
 * /mcpy <탭>                 -> start, stop, list, reload, update
 * /mcpy start <탭>           -> scripts 폴더에 있는 .py 파일 목록
 * /mcpy stop <탭>            -> 현재 실행 중인 스크립트 목록
 * /mcpy update <탭>          -> download
 */
public class McpyTabCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("start", "stop", "list", "reload", "update");
    private static final List<String> UPDATE_ARGS = List.of("download");

    private final Mcpy plugin;

    public McpyTabCompleter(Mcpy plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("mcpy")) {
            return List.of();
        }

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "start" -> filter(plugin.getScriptManager().getAvailableScriptNames(), args[1]);
                case "stop" -> filter(plugin.getScriptManager().getRunningScriptNames(), args[1]);
                case "update" -> filter(UPDATE_ARGS, args[1]);
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> filter(List<String> options, String typed) {
        String lower = typed.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}

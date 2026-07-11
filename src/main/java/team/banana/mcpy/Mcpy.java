package team.banana.mcpy;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import team.banana.mcpy.listeners.ChatBridgeListener;
import team.banana.mcpy.listeners.McpyTabCompleter;
import team.banana.mcpy.listeners.SignalListener;
import team.banana.mcpy.managers.ScriptManager;
import team.banana.mcpy.managers.SignalManager;
import team.banana.mcpy.skript.SkriptHook;
import team.banana.mcpy.utils.UpdateChecker;

public final class Mcpy extends JavaPlugin {

    private static Mcpy instance;

    private ScriptManager scriptManager;
    private SignalManager signalManager;
    private UpdateChecker updateChecker;

    // TODO: 저장소 이름이 다르면 여기만 바꾸면 됨
    private static final String GITHUB_OWNER = "Pudol-Coder";
    private static final String GITHUB_REPO = "Mcpy";

    @Override
    public void onEnable() {
        instance = this;

        this.scriptManager = new ScriptManager(this);
        this.signalManager = new SignalManager(this);
        this.updateChecker = new UpdateChecker(this, GITHUB_OWNER, GITHUB_REPO);

        signalManager.start();
        updateChecker.checkAsync();

        getServer().getPluginManager().registerEvents(new SignalListener(), this);
        getServer().getPluginManager().registerEvents(new ChatBridgeListener(this), this);

        if (getCommand("mcpy") != null) {
            getCommand("mcpy").setTabCompleter(new McpyTabCompleter(this));
        }

        SkriptHook.hook(this);

        getLogger().info("Mcpy가 활성화되었습니다.");
    }

    @Override
    public void onDisable() {
        if (scriptManager != null) {
            scriptManager.stopAll();
        }
        if (signalManager != null) {
            signalManager.stop();
        }
        getLogger().info("Mcpy가 비활성화되었습니다.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("mcpy")) {
            return false;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "start" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c[Mcpy] 사용법: /mcpy start <파일명>");
                    return true;
                }
                scriptManager.startScript(args[1], sender);
            }
            case "stop" -> {
                if (args.length < 2) {
                    sender.sendMessage("§c[Mcpy] 사용법: /mcpy stop <파일명>");
                    return true;
                }
                scriptManager.stopScript(args[1], sender);
            }
            case "list" -> scriptManager.listScripts(sender);
            case "reload" -> {
                reloadConfig();
                sender.sendMessage("§a[Mcpy] 설정을 리로드했습니다.");
            }
            case "update" -> {
                if (args.length >= 2 && args[1].equalsIgnoreCase("download")) {
                    updateChecker.downloadAndReplace(sender);
                } else {
                    updateChecker.checkAndNotify(sender);
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§b[ Mcpy 도움말 ]");
        sender.sendMessage("§7/mcpy start <파일명> §f- 파이썬 스크립트 실행");
        sender.sendMessage("§7/mcpy stop <파일명> §f- 실행 중인 스크립트 종료");
        sender.sendMessage("§7/mcpy list §f- 실행 중인 스크립트 목록");
        sender.sendMessage("§7/mcpy reload §f- 설정 리로드");
        sender.sendMessage("§7/mcpy update §f- 새 버전 확인");
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public SignalManager getSignalManager() {
        return signalManager;
    }

    public static Mcpy getInstance() {
        return instance;
    }
}

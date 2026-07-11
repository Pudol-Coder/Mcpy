package team.banana.mcpy.managers;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import team.banana.mcpy.Mcpy;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 파이썬 스크립트 프로세스를 시작/종료/추적하는 매니저.
 */
public class ScriptManager {

    private final Mcpy plugin;
    private final File scriptFolder;
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    // 서버 환경에 맞는 파이썬 실행 파일 경로로 변경 가능 (예: "python3", "/usr/bin/python3")
    private String pythonExecutable = "python";

    public ScriptManager(Mcpy plugin) {
        this.plugin = plugin;
        this.scriptFolder = new File(plugin.getDataFolder(), "scripts");
        if (!scriptFolder.exists()) {
            scriptFolder.mkdirs();
        }
    }

    public void startScript(String fileName, CommandSender sender) {
        String key = normalize(fileName);

        if (runningProcesses.containsKey(key)) {
            sender.sendMessage("§c[Mcpy] '" + fileName + "'은(는) 이미 실행 중입니다.");
            return;
        }

        File scriptFile = new File(scriptFolder, key);
        if (!scriptFile.exists()) {
            sender.sendMessage("§c[Mcpy] scripts 폴더에서 '" + key + "' 파일을 찾을 수 없습니다.");
            return;
        }

        try {
            ProcessBuilder builder = new ProcessBuilder(pythonExecutable, scriptFile.getAbsolutePath());
            builder.directory(scriptFolder);
            builder.redirectErrorStream(true);
            builder.inheritIO();

            Process process = builder.start();
            runningProcesses.put(key, process);

            sender.sendMessage("§a[Mcpy] '" + key + "' 실행을 시작했습니다.");

            process.onExit().thenRun(() -> {
                runningProcesses.remove(key);
                Bukkit.getScheduler().runTask(plugin, () ->
                        plugin.getLogger().info("[Mcpy] '" + key + "' 프로세스가 종료되었습니다."));
            });
        } catch (IOException e) {
            sender.sendMessage("§c[Mcpy] 실행 중 오류가 발생했습니다: " + e.getMessage());
            plugin.getLogger().warning("스크립트 실행 실패 (" + key + "): " + e.getMessage());
        }
    }

    public void stopScript(String fileName, CommandSender sender) {
        String key = normalize(fileName);
        Process process = runningProcesses.get(key);

        if (process == null) {
            sender.sendMessage("§c[Mcpy] '" + fileName + "'은(는) 실행 중이 아닙니다.");
            return;
        }

        process.destroy();
        runningProcesses.remove(key);
        sender.sendMessage("§a[Mcpy] '" + key + "'을(를) 종료했습니다.");
    }

    public void listScripts(CommandSender sender) {
        if (runningProcesses.isEmpty()) {
            sender.sendMessage("§7[Mcpy] 현재 실행 중인 스크립트가 없습니다.");
            return;
        }

        sender.sendMessage("§b[Mcpy] 실행 중인 스크립트 (" + runningProcesses.size() + "개):");
        for (String name : runningProcesses.keySet()) {
            sender.sendMessage("§7- " + name);
        }
    }

    public void stopAll() {
        for (Map.Entry<String, Process> entry : runningProcesses.entrySet()) {
            entry.getValue().destroy();
        }
        runningProcesses.clear();
    }

    private String normalize(String fileName) {
        return fileName.endsWith(".py") ? fileName : fileName + ".py";
    }

    public void setPythonExecutable(String pythonExecutable) {
        this.pythonExecutable = pythonExecutable;
    }
}

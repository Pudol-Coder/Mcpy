package team.banana.mcpy.managers;

import org.bukkit.Bukkit;
import team.banana.mcpy.Mcpy;
import team.banana.mcpy.events.McpySignalEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 마인크래프트 <-> 파이썬 양방향 신호를 파일 기반으로 주고받는 매니저.
 *
 * signals/to-python/  : 마인크래프트 -> 파이썬 (기존 방식)
 * signals/to-server/  : 파이썬 -> 마인크래프트 (신규)
 *
 * 두 폴더 모두 파일이 생성되면 감지 -> 내용을 읽고 처리 -> 파일 삭제 하는 방식으로 동작한다.
 * 파이썬 스크립트 쪽에서는 signals/to-python 폴더를 감시(polling 또는 watchdog 라이브러리)해서
 * 새 파일이 생기면 읽고 처리 후 삭제하면 된다.
 */
public class SignalManager {

    private final Mcpy plugin;
    private final File toPythonDir;
    private final File toServerDir;

    private Thread watcherThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public SignalManager(Mcpy plugin) {
        this.plugin = plugin;
        File signalsRoot = new File(plugin.getDataFolder(), "signals");
        this.toPythonDir = new File(signalsRoot, "to-python");
        this.toServerDir = new File(signalsRoot, "to-server");

        toPythonDir.mkdirs();
        toServerDir.mkdirs();
    }

    /** 파이썬 -> 서버 방향 신호 감시를 시작한다. */
    public void start() {
        if (running.get()) return;
        running.set(true);

        watcherThread = new Thread(this::watchLoop, "Mcpy-SignalWatcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public void stop() {
        running.set(false);
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
    }

    private void watchLoop() {
        try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
            toServerDir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

            while (running.get()) {
                WatchKey key;
                try {
                    key = watchService.poll(1, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (key == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) continue;

                    Path fileName = (Path) event.context();
                    File signalFile = new File(toServerDir, fileName.toString());
                    handleIncomingSignal(signalFile);
                }

                if (!key.reset()) break;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("[Mcpy] 신호 감시 중 오류: " + e.getMessage());
        }
    }

    private void handleIncomingSignal(File signalFile) {
        // 파일이 완전히 쓰여질 때까지 약간의 여유를 둔다.
        try {
            Thread.sleep(50);
            List<String> lines = Files.readAllLines(signalFile.toPath(), StandardCharsets.UTF_8);
            String content = String.join("\n", lines);
            String sourceScript = signalFile.getName();

            Files.deleteIfExists(signalFile.toPath());

            // Bukkit 이벤트는 반드시 메인 스레드에서 호출해야 한다.
            Bukkit.getScheduler().runTask(plugin, () -> {
                McpySignalEvent event = new McpySignalEvent(sourceScript, content);
                Bukkit.getPluginManager().callEvent(event);
            });
        } catch (IOException | InterruptedException e) {
            plugin.getLogger().warning("[Mcpy] 신호 파일 처리 실패 (" + signalFile.getName() + "): " + e.getMessage());
        }
    }

    /** 서버 -> 파이썬 방향으로 신호를 보낸다. */
    public void sendSignal(String content) {
        String fileName = "signal-" + UUID.randomUUID() + ".txt";
        File outFile = new File(toPythonDir, fileName);

        try {
            Files.writeString(outFile.toPath(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            plugin.getLogger().warning("[Mcpy] 신호 전송 실패: " + e.getMessage());
        }
    }
}

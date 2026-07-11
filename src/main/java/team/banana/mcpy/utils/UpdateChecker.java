package team.banana.mcpy.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import team.banana.mcpy.Mcpy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * GitHub Releases API를 사용해 최신 버전을 확인하고, 새 jar를 내려받아
 * 현재 실행 중인 플러그인 jar를 덮어쓰는 유틸리티.
 *
 * 주의: jar 파일을 덮어써도 이미 로드된 클래스는 그대로 메모리에 남아있기 때문에
 * 실제 코드 변경 사항은 "다음 서버 재시작" 시점에 적용된다. 즉 이 기능은
 * 재시작 전에 미리 최신 jar를 받아두는 용도로 쓰는 게 안전하다.
 */
public class UpdateChecker {

    private final Mcpy plugin;
    private final String owner;
    private final String repo;
    private final HttpClient httpClient;

    private static final Pattern TAG_PATTERN = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    // "browser_download_url": "https://.../Mcpy.jar" 형태에서 .jar로 끝나는 자산 URL 추출
    private static final Pattern ASSET_PATTERN = Pattern.compile("\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

    private volatile String latestVersion;
    private volatile String latestJarUrl;

    public UpdateChecker(Mcpy plugin, String owner, String repo) {
        this.plugin = plugin;
        this.owner = owner;
        this.repo = repo;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** 서버 시작 시 비동기로 조용히 확인, 새 버전이 있으면 콘솔에만 로그를 남긴다. */
    public void checkAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fetchLatestRelease();
            if (latestVersion == null) return;

            String current = plugin.getDescription().getVersion();
            if (!isUpToDate(current)) {
                plugin.getLogger().warning("[Mcpy] 새 버전이 있습니다: " + latestVersion + " (현재: " + current + ")");
                plugin.getLogger().warning("[Mcpy] '/mcpy update download' 로 미리 받아두면 다음 재시작 때 적용됩니다.");
            }
        });
    }

    /** 명령어(/mcpy update)로 즉시 확인할 때 사용. */
    public void checkAndNotify(CommandSender sender) {
        sender.sendMessage("§7[Mcpy] 최신 버전을 확인하는 중...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            fetchLatestRelease();
            String current = plugin.getDescription().getVersion();

            if (latestVersion == null) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage("§c[Mcpy] 버전 정보를 가져오지 못했습니다."));
                return;
            }

            boolean upToDate = isUpToDate(current);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (upToDate) {
                    sender.sendMessage("§a[Mcpy] 최신 버전을 사용 중입니다. (" + current + ")");
                } else {
                    sender.sendMessage("§e[Mcpy] 새 버전이 있습니다: " + latestVersion + " (현재: " + current + ")");
                    sender.sendMessage("§7/mcpy update download §f로 받아두면 다음 재시작 때 적용됩니다.");
                }
            });
        });
    }

    /** 최신 jar를 내려받아 현재 실행 중인 jar 파일 위에 덮어쓴다. (적용은 재시작 후) */
    public void downloadAndReplace(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (latestJarUrl == null) {
                fetchLatestRelease();
            }
            if (latestJarUrl == null) {
                notify(sender, "§c[Mcpy] 릴리즈에서 jar 파일을 찾지 못했습니다. (Release에 .jar 첨부 확인)");
                return;
            }

            try {
                Path runningJar = getRunningJarPath();
                Path tempFile = Files.createTempFile("mcpy-update-", ".jar");

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(latestJarUrl))
                        .timeout(Duration.ofSeconds(30))
                        .GET()
                        .build();

                HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
                if (response.statusCode() != 200) {
                    notify(sender, "§c[Mcpy] 다운로드 실패 (HTTP " + response.statusCode() + ")");
                    return;
                }

                // 리눅스에서는 실행 중인 파일이어도 교체(rename)가 안전하게 동작한다.
                Files.move(tempFile, runningJar, StandardCopyOption.REPLACE_EXISTING);

                notify(sender, "§a[Mcpy] 새 jar(" + latestVersion + ")를 받아왔습니다. 서버를 재시작하면 적용됩니다.");
                plugin.getLogger().warning("[Mcpy] jar가 교체되었습니다 (" + latestVersion + "). 재시작 전까지는 기존 코드로 계속 동작합니다.");
            } catch (IOException | InterruptedException e) {
                notify(sender, "§c[Mcpy] jar 교체 중 오류: " + e.getMessage());
            }
        });
    }

    private void notify(CommandSender sender, String message) {
        if (sender == null) {
            plugin.getLogger().info(message.replaceAll("§.", ""));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(message));
    }

    /** 현재 로드되어 실행 중인 plugin jar의 실제 경로를 알아낸다. */
    private Path getRunningJarPath() throws IOException {
        try {
            CodeSource codeSource = plugin.getClass().getProtectionDomain().getCodeSource();
            return Path.of(codeSource.getLocation().toURI());
        } catch (Exception e) {
            throw new IOException("실행 중인 jar 경로를 확인할 수 없습니다.", e);
        }
    }

    private boolean isUpToDate(String current) {
        return latestVersion.equalsIgnoreCase(current) || latestVersion.equalsIgnoreCase("v" + current);
    }

    private void fetchLatestRelease() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().warning("[Mcpy] GitHub API 응답 코드: " + response.statusCode());
                return;
            }

            String body = response.body();

            Matcher tagMatcher = TAG_PATTERN.matcher(body);
            if (tagMatcher.find()) {
                latestVersion = tagMatcher.group(1);
            }

            Matcher assetMatcher = ASSET_PATTERN.matcher(body);
            if (assetMatcher.find()) {
                latestJarUrl = assetMatcher.group(1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[Mcpy] 버전 확인 실패: " + e.getMessage());
        }
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}


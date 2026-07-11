# Mcpy

Minecraft(Paper) 서버와 파이썬 스크립트를 연결하는 플러그인.
HostBot의 후속작으로, 파이썬 스크립트 실행 관리 + **양방향 신호 통신**을 지원한다.

## 기능

- `/mcpy start <파일명>` — `plugins/Mcpy/scripts/` 안의 파이썬 스크립트 실행
- `/mcpy stop <파일명>` — 실행 중인 스크립트 종료
- `/mcpy list` — 실행 중인 스크립트 목록
- `/mcpy update` — GitHub 릴리즈 기준 최신 버전 확인
- 파이썬 ↔ 서버 양방향 신호 (파일 기반, `plugins/Mcpy/signals/`)

## 신호 통신 방식

```
plugins/Mcpy/signals/
  to-python/   서버 -> 파이썬 (SignalManager#sendSignal 로 파일 생성)
  to-server/   파이썬 -> 서버 (파이썬이 파일 생성 -> McpySignalEvent 발생)
```

파이썬 쪽 예시 (신호 보내기):

```python
import pathlib, uuid

signal_dir = pathlib.Path("plugins/Mcpy/signals/to-server")
signal_dir.mkdir(parents=True, exist_ok=True)
(signal_dir / f"signal-{uuid.uuid4()}.txt").write_text("broadcast:안녕 서버!")
```

파이썬 쪽 예시 (신호 받기, polling):

```python
import time, pathlib

watch_dir = pathlib.Path("plugins/Mcpy/signals/to-python")
seen = set()

while True:
    for f in watch_dir.glob("*.txt"):
        if f.name not in seen:
            seen.add(f.name)
            print("서버로부터:", f.read_text())
            f.unlink()
    time.sleep(1)
```

## 버전 호환성 (1.21.11 / 26.1.x)

Mojang이 26.1부터 버전 체계를 바꾸면서(Java 25 요구, 서버 jar 비난독화 등)
구버전(1.21.11)과 신버전(26.1.x) 사이에 몇 가지 차이가 생겼다.

- **Java**: `pom.xml`의 `maven.compiler.release`는 21로 유지한다. Java 25 런타임(26.1.2 서버)은
  21로 컴파일된 클래스를 문제없이 실행하지만, 반대로 25로 올려버리면 1.21.11처럼
  구버전 Java만 있는 서버에서는 실행이 안 된다.
- **`plugin.yml`의 `api-version`**: `1.21`처럼 낮게 유지한다. 너무 높게(`26.1` 등) 잡으면
  1.21.11 서버가 로드를 거부할 수 있다.
- **reobf 방식 금지**: 26.1부터는 Spigot 매핑으로 재난독화한 플러그인이 아예 동작하지 않는다.
  Mcpy는 일반 Paper API 기반이라 해당 없음 — reobfJar 같은 빌드 단계를 추가하지 말 것.
- **API 100% 호환은 보장 안 됨**: 일부 최신 이벤트/클래스가 26.1 브랜치에 아직 없는 경우가
  실제로 보고된 적 있다. Mcpy는 기본 Bukkit 이벤트/명령어/파일 IO만 쓰기 때문에 위험이
  낮지만, 새 기능을 추가할 때 Paper 최신 전용 API를 쓰면 26.1에서만 되고 1.21.11에서
  깨질 수 있으니 주의.
- 두 버전 서버 모두에 실제로 올려서 `/mcpy` 명령어와 신호 송수신이 잘 되는지 테스트하는 게 제일 확실하다.

## 빌드

```
mvn clean package
```

`target/Mcpy.jar`를 서버 `plugins/` 폴더에 넣으면 된다.

## GitHub 자동 업데이트 확인

`Mcpy.java`의 `GITHUB_OWNER`, `GITHUB_REPO` 상수를 실제 저장소로 바꾸면
서버 시작 시 자동으로, 또는 `/mcpy update` 명령어로 최신 릴리즈를 확인한다.

`/mcpy update download`를 실행하면 GitHub Release에 첨부된 `.jar` 자산을 받아서
현재 실행 중인 jar 파일을 덮어쓴다. 리눅스에서는 실행 중인 파일도 안전하게
교체할 수 있지만(이미 로드된 클래스는 메모리에 남아있음), **실제 코드 변경은
다음 서버 재시작 시점에 적용된다.** 즉 "재시작 전에 최신 버전을 미리 받아두는"
용도로 쓰면 된다. GitHub Release를 만들 때 jar 파일을 Release Asset으로
첨부해야 이 기능이 동작한다.

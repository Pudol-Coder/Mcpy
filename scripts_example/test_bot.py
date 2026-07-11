"""
Mcpy 테스트용 간단한 봇.

- 10초마다 서버로 신호를 보내서 채팅에 방송되는지 확인 (파이썬 -> 서버)
- 서버가 보낸 신호가 오면 콘솔에 출력 (서버 -> 파이썬)

사용법:
1. 이 파일을 plugins/Mcpy/scripts/test_bot.py 로 저장
2. 마인크래프트 서버에서 /mcpy start test_bot 실행
3. 서버 콘솔/채팅에 "[Mcpy] 테스트 신호 N번째!" 메시지가 10초마다 뜨는지 확인
4. 종료하려면 서버에서 /mcpy stop test_bot
"""

import pathlib
import time
import uuid

# 이 스크립트는 서버가 scripts 폴더 안에서 실행시키기 때문에,
# plugins/Mcpy 폴더를 기준으로 상대 경로를 잡는다.
BASE_DIR = pathlib.Path(__file__).resolve().parent.parent  # plugins/Mcpy
TO_SERVER_DIR = BASE_DIR / "signals" / "to-server"   # 파이썬 -> 서버
TO_PYTHON_DIR = BASE_DIR / "signals" / "to-python"   # 서버 -> 파이썬

TO_SERVER_DIR.mkdir(parents=True, exist_ok=True)
TO_PYTHON_DIR.mkdir(parents=True, exist_ok=True)

seen_files = set()


def send_signal(content: str) -> None:
    """서버로 신호를 보낸다. broadcast: 로 시작하면 서버가 채팅에 방송한다."""
    file_path = TO_SERVER_DIR / f"signal-{uuid.uuid4()}.txt"
    file_path.write_text(content, encoding="utf-8")
    print(f"[봇] 신호 전송: {content}")


def check_incoming_signals() -> None:
    """서버가 보낸 신호가 있는지 확인하고 있으면 읽고 삭제한다."""
    for file_path in TO_PYTHON_DIR.glob("*.txt"):
        if file_path.name in seen_files:
            continue
        seen_files.add(file_path.name)

        content = file_path.read_text(encoding="utf-8")
        print(f"[봇] 서버로부터 신호 수신: {content}")

        file_path.unlink(missing_ok=True)


def main() -> None:
    print("[봇] 테스트 봇 시작! 10초마다 신호를 보냅니다. (Ctrl+C 또는 /mcpy stop 으로 종료)")
    counter = 0

    try:
        while True:
            counter += 1
            send_signal(f"broadcast:[Mcpy] 테스트 신호 {counter}번째!")

            # 10초를 기다리는 동안 1초 간격으로 수신 신호도 같이 확인
            for _ in range(10):
                check_incoming_signals()
                time.sleep(1)
    except KeyboardInterrupt:
        print("[봇] 종료합니다.")


if __name__ == "__main__":
    main()

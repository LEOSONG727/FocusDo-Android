# FocusDo Android — CLAUDE.md

## 프로젝트 개요
To-do-list-with-Pomodoro 웹앱의 네이티브 Android 재구현체.
웹앱의 기능 원리는 동일하게 유지하되, 모바일 UX에 맞게 디자인을 새로 작성.

## 빌드 환경
- Android Studio Iguana (2023.2.1) 이상 권장
- AGP 8.3.2, Kotlin 1.9.22
- Min SDK: 26 (Android 8.0), Target SDK: 34

## 아키텍처
- **패턴**: MVVM (ViewModel + StateFlow + Room)
- **패키지**: `com.focusdo.app`
- **단일 Activity**: `MainActivity` + DialogFragment/BottomSheetDialogFragment

## 핵심 파일
```
app/src/main/java/com/focusdo/app/
├── FocusDoApp.kt          — Application 클래스 (DynamicColors 초기화)
├── data/
│   ├── Task.kt            — Room Entity
│   ├── TaskDao.kt         — DAO (Flow 반환)
│   └── AppDatabase.kt     — Room Database singleton
├── util/
│   └── DateUtils.kt       — 날짜 포맷/연산 유틸리티
└── ui/
    ├── MainViewModel.kt           — 전체 상태 관리 (StateFlow)
    ├── MainActivity.kt            — 메인 화면
    ├── TaskAdapter.kt             — 할일 목록 RecyclerView 어댑터
    ├── CalendarAdapter.kt         — 달력 스트립 어댑터
    ├── BuddyView.kt               — Canvas로 그린 치이카와 버디 캐릭터
    ├── TimerRingView.kt           — Canvas 원형 타이머 링
    ├── PomodoroDialog.kt          — 전체화면 뽀모도로 타이머
    ├── TaskEditBottomSheet.kt     — 할일 추가/수정 BottomSheet
    ├── FocusSelectBottomSheet.kt  — 집중할 작업 선택 BottomSheet
    └── StatsBottomSheet.kt        — 집중 통계 BottomSheet
```

## 웹앱 → Android 변환 매핑
| 웹앱                       | Android                          |
|---------------------------|----------------------------------|
| localStorage               | Room Database                    |
| CSS overlay (focus modal)  | PomodoroDialog (full-screen)     |
| CSS overlay (modals)       | BottomSheetDialogFragment        |
| Sidebar (stats/settings)   | BottomSheet + AlertDialog        |
| CSS Chiikawa 캐릭터         | BuddyView (Canvas)               |
| SVG timer ring             | TimerRingView (Canvas + drawArc) |
| setInterval                | Coroutine + delay(1000L)         |
| Toast (web)                | Snackbar (Android)               |

## 전략/비즈니스 로직
MainViewModel.kt에 모두 집중:
- `visibleTasks` = selectedDate + filter 조합 StateFlow
- `achievement` = 해당 날짜 완료율 (%)
- `startFocus(id)` → Coroutine 타이머 시작
- `togglePause()` → paused 플래그 토글 (interval은 계속 실행, tick 스킵)
- `stopFocus()` → DB에 focusedTime 누적 저장 후 state 초기화
- 뽀모도로 사이클 완료 시 tomatoes++ 및 DB 업데이트

## 빌드 명령어
```bash
./gradlew assembleDebug   # 디버그 APK 빌드
./gradlew installDebug    # 연결된 기기에 설치
```

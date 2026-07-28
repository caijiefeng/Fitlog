# FitLog UI Flow

## Screen Map

```
App Launch
    │
    ▼
┌─────────────────────────────────────────────────┐
│  Scaffold (Main)                                │
│  ┌───────────────────────────────────────────┐  │
│  │  TopAppBar (per-tab title)                │  │
│  ├───────────────────────────────────────────┤  │
│  │                                           │  │
│  │  NavHost (5 top-level tabs)               │  │
│  │  ┌──────┬──────┬──────┬──────┬──────┐    │  │
│  │  │ 今日 │ 计划  │ 记录  │ 进度  │ 我的  │    │  │
│  │  └──────┴──────┴──────┴──────┴──────┘    │  │
│  │                                           │  │
│  └───────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────┐  │
│  │  NavigationBar (5 tabs)                   │  │
│  └───────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘

Full-Screen Destinations (bottom nav HIDDEN):
┌─────────────────────────────────────────────────┐
│  Workout Execution                              │
│  ┌───────────────────────────────────────────┐  │
│  │  Exercise list + set logger + rest timer  │  │
│  └───────────────────────────────────────────┘  │
│  (no bottom nav — prevents accidental exit)     │
└─────────────────────────────────────────────────┘
```

## Navigation Graph

```
Top-Level Tabs (bottom nav visible):
  today ──→ plan ──→ record ──→ progress ──→ profile

Secondary Routes (bottom nav HIDDEN):
  today ──→ workout-execution/{sessionId}
  today ──→ workout-summary/{sessionId}
  plan ──→ template-list
  plan ──→ template-detail/{templateId}
  plan ──→ template-edit/{templateId?}
  plan ──→ exercise-list
  plan ──→ exercise-detail/{exerciseId}
  plan ──→ exercise-edit/{exerciseId?}
  record ──→ session-detail/{sessionId}
  record ──→ measurement-entry
  record ──→ meal-entry
  progress ──→ chart-detail/{chartType}
  profile ──→ profile-edit
  profile ──→ settings-*

Top-level tab switch uses:
  - popUpTo(startDestination) { saveState = true }
  - launchSingleTop = true
  - restoreState = true
```

## V0 — Tab Details

### 1. 今日 (Today)

Shows today's scheduled workout, quick-start option, and daily summary.

```
┌─────────────────────────────────┐
│ 今日                            │
├─────────────────────────────────┤
│ 下午好                          │
│ 准备开始今天的训练了吗？          │
│                                 │
│ 今日训练                        │
│ ┌─────────────────────────────┐ │
│ │ (scheduled workout or       │ │
│ │  EmptyState with CTA to     │ │
│ │  "创建训练计划")             │ │
│ └─────────────────────────────┘ │
│                                 │
│ 快速操作                        │
│ ┌─────────────────────────────┐ │
│ │ 快速开始训练                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ 今日摘要 (future)               │
│ 热量: — 蛋白质: — 碳水: —      │
└─────────────────────────────────┘
```

### 2. 计划 (Plan)

Weekly schedule overview + access to templates and exercises.

```
┌─────────────────────────────────┐
│ 计划                            │
├─────────────────────────────────┤
│ 每周计划                        │
│ ┌──┬──┬──┬──┬──┬──┬──┐        │
│ │一│二│三│四│五│六│日│        │
│ └──┴──┴──┴──┴──┴──┴──┘        │
│                                 │
│ 训练模板                        │
│ 管理训练模板和大纲                │
│                                 │
│ 动作库                          │
│ 浏览和管理训练动作                │
└─────────────────────────────────┘
```

### 3. 记录 (Record)

Recent training sessions and body data entry.

```
┌─────────────────────────────────┐
│ 记录                            │
├─────────────────────────────────┤
│ 最近训练                        │
│ (session history list)          │
│                                 │
│ 身体数据                        │
│ 记录体重、体脂、围度              │
│                                 │
│ 饮食记录 (future)               │
│ 记录每日饮食和营养素              │
└─────────────────────────────────┘
```

### 4. 进度 (Progress)

Training statistics and body change charts.

```
┌─────────────────────────────────┐
│ 进度                            │
├─────────────────────────────────┤
│ 训练统计                        │
│ 训练次数 · 连续天数 · 总组数     │
│                                 │
│ 身体变化                        │
│ 体重趋势 · 围度变化 · PR 记录    │
└─────────────────────────────────┘
```

### 5. 我的 (Profile)

User profile and app settings.

```
┌─────────────────────────────────┐
│ 我的                            │
├─────────────────────────────────┤
│ ┌─────────────────────────────┐ │
│ │ 个人资料卡片                 │ │
│ └─────────────────────────────┘ │
│                                 │
│ 设置                            │
│ ┌─────────────────────────────┐ │
│ │ 个人资料                     │ │
│ │ 训练偏好                     │ │
│ │ 外观                         │ │
│ │ 提醒设置 (future)            │ │
│ │ 数据管理                     │ │
│ │ 关于                         │ │
│ └─────────────────────────────┘ │
└─────────────────────────────────┘
```

## Future Secondary Routes

### Workout Execution (V2) — Full Screen, No Bottom Nav

```
┌─────────────────────────────────┐
│ ← 退出训练                      │  ← Close button (confirmation dialog)
├─────────────────────────────────┤
│ 训练名称 · 已进行 00:25:30      │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 1. 杠铃卧推         胸      │ │
│ │ ┌─────┬─────┬─────┬─────┐ │ │
│ │ │ 80  │  8  │ 8.0 │  1  │ │ │
│ │ │ kg  │ reps│ RPE │ RIR │ │ │
│ │ └─────┴─────┴─────┴─────┘ │ │
│ │ 休息计时器: 00:45            │ │
│ └─────────────────────────────┘ │
│                                 │
│ ┌─────────────────────────────┐ │
│ │ 2. 哑铃飞鸟         胸      │ │
│ │ [not started]               │ │
│ └─────────────────────────────┘ │
│                                 │
│ [+ 添加动作]                    │
│                                 │
│ [完成训练]                      │
└─────────────────────────────────┘
```

### Template List (V1) / Template Edit (V1)

### Session Detail (V2) / Workout Summary (V2)

### Exercise List (V1) / Exercise Edit (V1)

### Measurement Entry (V4)

### Meal Entry (V5)

## Workout Execution State Recovery (V2)

When a workout is `IN_PROGRESS` and the process dies:
1. On app restart, check Room for any session with `status = IN_PROGRESS`.
2. If found and < 24 hours old: show "恢复训练?" dialog on Today tab.
3. User can resume or discard.
4. If > 24 hours old: auto-cancel and mark as `CANCELLED`.

The execution screen uses `SavedStateHandle` as a secondary recovery mechanism for partial UI state (currently expanded exercise, timer state), but the authoritative session data is always in Room.

## Design System

### Components

| Component | Purpose |
|---|---|
| `FitLogTopAppBar` | Screen title bar, white text on dark surface |
| `FitLogCard` | Rounded 12dp card, `#1A1A1A` background, optional onClick |
| `EmptyState` | Icon + title + subtitle for empty lists |
| `PageContainer` | Scrollable column with consistent 16dp horizontal padding |
| `SectionHeader` | Uppercase gray label with optional "See all" action |

### Color Palette (Dark-Only, Forced)

| Token | Hex | Usage |
|---|---|---|
| Background | `#0D0D0D` | Screen backgrounds |
| Surface | `#0D0D0D` | TopAppBar, BottomBar |
| Card | `#1A1A1A` | Card surfaces |
| Text Primary | `#F2F2F2` | Primary content |
| Text Secondary | `#999999` | Labels, descriptions |
| Text Tertiary | `#666666` | Disabled/hint |
| Accent | `#4CAF9B` | Selected tab, primary actions only |
| Divider | `#2A2A2A` | Separators |

The accent color is used sparingly — only for selected navigation state and primary call-to-action elements. It is never used as decorative color. Status indicators (error, success) will use their own semantic colors.

### V0: No Light Theme

V0 does not provide a light theme toggle. The Profile "外观" setting shows only system font scaling, not a light/dark switch.

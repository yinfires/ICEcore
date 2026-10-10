# 时间系统与时间跳过

本系统只控制主世界的 `dayTime`，配置保存于每个世界的 `data/icecore_time.json`。所有管理命令要求权限等级 2，修改后立即校验、原子保存并同步在线客户端。

## 时间与日历

- `dayDurationMultiplier` 范围为 `0.01`～`100`；`1` 为原版速度，`2` 表示一天耗时为原版两倍。
- 系统改变一天的时间长度，并同步补偿随机刻以保持作物生长、耕地干旱等机制正常工作；不改变 game time、服务器 TPS、配方、熔炉、方块实体或实体更新。0.01 倍率按统一数学公式补足到 100 倍随机刻密度，补偿同时覆盖方块与流体随机刻，每个区块 tick 上限 10000 次。
- 随机刻补偿：当 `dayDurationMultiplier != 1` 时，主世界会在原版随机刻之后额外执行补偿性随机刻。时间减慢（`multiplier > 1`）时额外执行 `ceil((multiplier - 1) * randomTickSpeed)` 次；时间加速（`multiplier < 1`，如 0.01）时额外执行 `ceil((1/multiplier - 1) * randomTickSpeed)` 次，0.01 倍率合计 100 倍随机刻密度。每个区块 tick 的补偿上限为 10000 次，并同时处理方块和流体随机刻；其他维度和 `doDaylightCycle=false` 时不应用补偿。
- `doDaylightCycle=false` 会停止自然流逝；床过场与原版 `/time set`、`/time add` 仍可显式改时。
- `dayTime=0` 显示为第 1 天、星期一、06:00；每天按 06:00 到次日 06:00 计算。
- 其它维度不应用倍率、床投票或 Dew Drop 农田时间兼容，HUD 显示主世界时间。
- 安装已核验的 Dew Drop Farmland Growth 9.0 时，ICEcore 按配置的日内时间段检测跨越，并让已加载、正常收到 scheduled tick 的该模组农田、花盆、耕作沙及 Vinery 兼容作物每天处理一次；加速、减速、床快进和 `/time add` 跨窗均不会漏处理或重复处理。默认区间为 `[23990, 24000)`，即一天结束前 10 个 `dayTime`；倒退时间不触发，单次跨过多天只处理一次。处理日写入世界 SavedData，窗口内重启不会重复。同 Dew Drop 上游规则一致，`doDaylightCycle=false` 时每日处理暂停，重新开启后从后续有效区间继续。其它版本保持上游行为，避免上游已原生兼容后重复接管。
- 主世界自然流逝跨过 06:00 日界时，所有在线玩家只播放共享的日结打字机动画，不切换摄像机、不黑屏、不锁定输入；手动 `/time` 跨日仍会结算收入，但不播放该自然换日动画。
- HUD 时间固定为 `HH:MM` 五个字符槽；分钟变化时只滚动实际变化的数字，未变化数字与冒号保持静止。太阳与月亮暂时使用参考模组相同的字体符号（`☀`/`☾`），与时间数字共用字体基线和字号；后续替换为图标时，图标高度不得超过时间数字高度。

## JSON

`triggerBlockList` 引用 `icecore_building.json` 中已有方块列表，但只复用其 `entries`，不使用该列表的区域条件。`regions` 引用已有区域；重叠时选体积最小的区域，体积相同按名称排序。摄像机保存眼睛位置和 yaw/pitch。

`compatibility.dewDropFarmlandGrowth` 控制 Dew Drop Farmland Growth 9.0 的时间兼容：`enabled` 为总开关，`dailyWindowStart` 允许 `0..23999`，`dailyWindowEnd` 必须大于起点且不超过 `24000`。这是半开区间 `[start, end)`；修改后执行 `/icecore time reload`。关闭兼容后完全保留 Dew Drop 自己的 `dailyTimeMin` 行为。

```json
{
  "formatVersion": 1,
  "dayDurationMultiplier": 1.0,
  "hudEnabled": true,
  "naturalDaySummaryEnabled": true,
  "triggerBlockList": "beds",
  "regions": ["hotel"],
  "cameras": {
    "hotel": {"x": 10.5, "y": 72.0, "z": 20.5, "yaw": 180.0, "pitch": 10.0}
  },
  "compatibility": {
    "dewDropFarmlandGrowth": {
      "enabled": true,
      "dailyWindowStart": 23990,
      "dailyWindowEnd": 24000
    }
  },
  "timings": {
    "fadeToCameraTicks": 10,
    "revealCameraTicks": 10,
    "fastForwardTicks": 20,
    "typewriterTicksPerCharacter": 2,
    "summaryHoldTicks": 40,
    "summaryFadeTicks": 10,
    "fadeToPlayerTicks": 10,
    "revealPlayerTicks": 10
  }
}
```

## 命令

| 命令 | 作用 |
| --- | --- |
| `/icecore time reload` | 重载时间 JSON。 |
| `/icecore time speed get/set <值>` | 查询或设置昼夜耗时倍率。 |
| `/icecore time hud get/set <true/false>` | 查询或设置全服时间 HUD。 |
| `/icecore time naturalSummary get/set <true/false>` | 查询或设置自然换日时是否播放结算动画；床跳时的结算动画不受此开关影响。 |
| `/icecore time trigger get/set <列表>` | 查询或设置触发方块列表。 |
| `/icecore time region add/remove <区域>` | 增删有效时间跳过区域。 |
| `/icecore time camera get/set/remove <区域>` | 查询、记录或删除区域摄像机；记录时须手持普通木棍。 |
| `/icecore time timing get` | 查询全部过场时长。 |
| `/icecore time timing set <阶段> <ticks>` | 设置一个过场阶段；`fastForward` 表示每个游戏内小时使用的动画 tick，所有阶段允许 1～1200 tick。 |

## 投票与过场

冒险模式玩家在主世界有效区域内右键触发方块时确认，再次右键取消；区域外保留原交互。确认者离开自己的确认区域会取消。分母为主世界在线玩家并随加入、离线和换维度实时变化；全员确认后每位玩家使用自己区域的摄像机。

过场依次执行黑幕、切换摄像机、恢复画面、快进到严格下一天 06:00、打字机结算、结算文字淡出、黑幕、恢复原视角。默认快进速度为游戏内 1 小时/现实 1 秒，并依据本轮实际时间跨度动态计算；默认打字机每字符 2 tick，全文完成后停留 40 tick，再用 10 tick 淡出。区域摄像机启用期间隐藏普通 HUD、Jade 信息和第一人称手臂，只保留过场黑幕与结算文本；摄像机位置使用记录时的玩家眼睛坐标，视角固定为区域配置且不响应玩家鼠标转向。参与者在流程中锁定世界移动与交互并免疫伤害，但仍可打开 ESC 界面和聊天栏并正常操作界面；单人游戏进入暂停界面时，过场阶段、天空快进和黑幕按同一个暂停感知客户端时钟冻结，多人游戏打开界面时仍随未暂停的服务器继续。服务端不逐 tick 传送玩家。恢复时先把玩家上一帧变换同步到当前变换，再原子切回玩家相机；第一人称手臂保持隐藏到过场完全结束，床投票文字等待服务端清票快照后才重新显示，避免恢复首帧的相机插值、手臂抖动和旧文字闪回。最终淡出完成后客户端发送 ACK 并立即清除本轮输入、相机和过场状态，迟到旧包不会重新开启；异常与超时统一恢复原位置、完整旋转、视角和伤害状态。原版 `/time` 在过场中执行会中止过场并以指令时间为准。

共享货币按主世界 06:00 日界统计净变化。任何日期变化都会把当前桶结算后重开；床过场与启用后的自然换日动画复用同一套总结文本、打字机排版和时长，并显示刚结束游戏日的净收入。自然换日动画只是客户端视觉事件，不要求 ACK，也不改变玩家状态。

## 实现与验证

入口位于 `time` 包、`ServerLevelTimeMixin`、`ServerLevelRandomTickMixin`、`ClientLevelTimeMixin` 与 `TimeCommandMixin`。自然流逝使用小数余量累积，服务端在确认收入桶完成结算后才广播一次自然日结事件；网络仅在配置、投票、阶段变化、自然日结和每秒时间校正时发送。床过场与自然日结共用 `DaySummaryRenderer`，HUD 和动画使用逐帧时间、partial-frame 等价插值及缓动；时钟文本预缓存并只为变化槽绘制滚动双层，避免每帧格式化与无关字符动画；世界投票牌只绘制准星当前目标。

`ServerLevelRandomTickMixin` 注入 `ServerLevel.tickChunk()` 方法尾部，在原版随机刻执行后根据 `dayDurationMultiplier` 补偿性地额外执行随机刻；只对主世界且 `doDaylightCycle=true` 时应用，避免影响其他维度和暂停状态。补偿公式：时间减慢时（`multiplier > 1`）额外执行 `ceil((multiplier - 1) * randomTickSpeed)` 次，时间加速时（`multiplier < 1`）额外执行 `ceil((1/multiplier - 1) * randomTickSpeed)` 次，上限 1000 - randomTickSpeed。这确保了无论时间加速还是减速，每单位游戏时间内的随机刻密度都与时间流速匹配，从而修复了种植系统与时间倍率变化的兼容问题。

验证包括日历边界、时钟槽位变化与暂停、按实际跨度计算的快进时长、旧配置迁移、过场世界输入锁与界面输入放行/单人暂停时钟/相机眼高与恢复插值/HUD/手臂/投票文字快照/服务端位置锁安全门禁和 Mixin 生产选择器 JUnit 测试、随机刻缩放计算测试、`compileJava`、资源处理、Forge 47.4.10 `clean build`、生产 JAR Mixin/网络/语言检查和 Git 边界检查。已用 Forge 47.4.10 事件类核对 `InputEvent.Key` 不可取消、鼠标按钮、GUI overlay、手臂渲染事件可取消以及视口角度修改 API；测试固定要求过场源码不使用系统墙钟、不拦截 `PauseScreen`，并以 `Minecraft#isPaused` 门控客户端 tick、以 partial tick 平滑渲染。客户端视觉与多人流程需由用户执行或在明确授权后实机验收。

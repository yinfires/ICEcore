# SmartKeyPrompts 的 KubeJS 自定义提示

用途：后续编写客户端情境按键提示时的接口、事件时序与验证参考。2026-10-05 核对。

## 适用版本与证据

- Minecraft 1.20.1 Forge；SmartKeyPrompts 1.1.3-hotfix、KubeJS 2001.6.5-build.26、Rhino 2001.2.3-build.10。
- 来源：客户端 mods 下实际 JAR；`SmartKeyPromptsJSPlugin.registerBindings`、`PromptUtils`、`KeyPrompt.toGroup`、`HUD.tick`、KubeJS `BuiltinKubeJSForgePlugin` 与 `BuiltinKubeJSClientPlugin` 字节码。
- 目标 Rhino 已验证脚本加载、重复加载与模拟回调；实际游戏 HUD 仍需运行日志与画面验收，不将解释器模拟视为实机通过。升级任一模组后重新核对。

## 客户端接口

`SKP$PromptUtils`、`SKP$KeyUtils`、`SKP$PlayerUtils`、`SKP$NBTUtils`、`SKP$CommonUtils` 仅绑定到 client_scripts；内置 `Client` 是 Minecraft 客户端对象。

```js
var prompt = SKP$PromptUtils.addDesc('yourmod.prompt.use')
  .forKey(String(Client.options.keyUse.getKey().getName()))
  .withCustom(true)
prompt.toGroup('yourmod_use')
```

- addDesc 参数是说明翻译键；在客户端 `kubejs/assets/<namespace>/lang/zh_cn.json` 和 `en_us.json` 定义，不在文字里重复写按键。
- forKey 参数是实际按键 ID；读取 keyUse/keyDrop 的绑定，不能固定右键或 Q。
- withCustom(true) 用于说明翻译键与原版按键描述不同的自定义提示。
- toGroup 会加入 HUD 缓存，不是永久注册。条件满足时每 tick 提交；不满足时停止提交，下一次 HUD START 消费时移除。
- 分组受 SmartKeyPrompts 的 blacklist 控制；位置和缩放由其配置处理。
- 缓存提示对象，绑定变化时创建新对象；不能修改仍被外部集合引用的旧对象。每 tick 固定条件判断，避免重复创建、反射、I/O 或日志。KeyUtils.getKeyByDesc 内部遍历按键，能直接读取 Client.options 时优先直接读取。

## Forge END 与客户端回调

KubeJS 此版本只在 startup_scripts 提供 ForgeEvents，且仅首次启动允许注册。由启动事件调用客户端建立的 global 回调；两类脚本共享该映射。需要完整重启，不用 /reload 注册启动事件。

启动脚本模板：

```js
if (Platform.isClientEnvironment() && Platform.isLoaded('smartkeyprompts')) {
  ForgeEvents.onEvent('net.minecraftforge.event.TickEvent$ClientTickEvent', function (event) {
    if (String(event.phase) === 'END' && typeof global.yourUsePrompt === 'function') {
      global.yourUsePrompt()
    }
  })
}
```

客户端脚本模板：

```js
;(function () {
  global.yourUsePrompt = null
  if (!Platform.isLoaded('smartkeyprompts')) return
  var prompt = null
  var lastKey = null
  global.yourUsePrompt = function () {
    if (Client.player == null || Client.screen != null || Client.player.getMainHandItem().isEmpty()) return
    var key = String(Client.options.keyUse.getKey().getName())
    if (prompt == null || lastKey !== key) {
      lastKey = key
      prompt = SKP$PromptUtils.addDesc('yourmod.prompt.use').forKey(key).withCustom(true)
    }
    prompt.toGroup('yourmod_use')
  }
})()
```

END 提交供下一 START 消费，避免依赖同阶段监听顺序。停止条件允许最长一个 tick 的消费延迟。仅显示提示不代表物品具备右键用途；真实玩法条件必须单独定义。

## 限制与验收

- 不通过 Java.loadClass 加载 FMLLoader/ModList：`net.minecraftforge.fml` 被类过滤禁止。环境与模组检测用 Platform 内置接口，不放宽过滤。
- 当前 Rhino 曾对条件块 const 报 redeclaration；模板采用 IIFE + var，避免块级声明与共享脚本作用域冲突。
- node --check 只检查语法；用客户端实际 Rhino JAR验证加载/重复加载、START/END 转发、改键、对象复用、空手、界面、无玩家和依赖缺失；Java绑定/API额外用实际 JAR 核对。
- 实机验收查看 startup.log 与 client.log 的具体 ERROR 和脚本加载行，不能仅依赖末尾错误计数；验证提示、改键、黑名单、退出世界与关闭界面的清理。
- 临时测试已于 2026-10-05 按用户要求移除：两个 smartkeyprompts_use_test 脚本及 icecore.test_prompt.use 中英文翻译均不再部署。模板只保存在本文，ICEcore 的正式给予提示 Java 兼容保留。

## 工作方块正式提示（2026-10-18）

- 指定客户端startup_scripts与client_scripts均为icecore_workstation_prompts.js；双语操作键在客户端assets/icecore/lang，行为矩阵见content-implementation.md“工作方块情境按键提示”。组合键用实际潜行键ID + '+' + 实际使用键ID，混合碗同样为组合键搅拌。
- 1.1.3-hotfix原生KeyUtils.translateKey会把组合键翻译为带+字符串，KeyRenderer.drawKeyBoardKey将整串绘成单个长键帽；仅脚本不能实现独立键帽。ICEcore SmartKeyPromptsComboMixin复用原生逐键绘制及+文字并修正HUD宽度，不复制键帽贴图/背景。原生缓存HashSet不保证提交顺序，兼容只在列表重算时保证垃圾桶投入在取出前。
- 目标Rhino真实解释器测试通过；启动事件须完整重启，实际client/startup日志及HUD、改键、缩放/对齐、黑名单、退出清理仍需实机验收，不自动启动客户端。
- 2026-10-18后续外观要求：ICEcore组合键兼容中的单键/组合键两条原生绘制分支统一传入pressed=false；所有SmartKeyPrompts提示保持未按下外观，不改变按键绑定、实际输入、提示条件或原生键帽样式。SmartKeyPromptsComboContractTest直接检查编译字节码两处调用前均为ICONST_0，防止某分支残留按压变化。

- 2026-10-18蒸笼改为lid_combo，使用潜行键+使用键显示开关盖，不再提交拾取提示；已用目标Rhino复测更新规则。蒸笼叠加与取下由模组交互限制，脚本仅显示操作说明。

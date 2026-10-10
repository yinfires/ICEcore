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

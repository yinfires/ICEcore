# 固定 NPC 系统

用途：记录 ICEcore 玩家模型固定 NPC 的定义格式、服务端行为、ChatBox 接入和验证边界。

## 定义与资源

定义位于 `data/<namespace>/icecore/npcs/<path>.json`，资源 ID 为 `<namespace>:<path>`。初版字段如下：

```json
{
  "name": "npc.icecore.fire",
  "texture": "icecore:textures/entity/npc/fire.png",
  "model": "slim",
  "dialogue": {
    "resource": "icecore:npc/fire",
    "group": "greeting"
  }
}
```

`model` 只允许 `wide` 或 `slim`。名称、贴图、模型类型和对话入口在创建时写入实体同步数据与 NBT，因此已有实体不会因区块卸载或存档重启丢失显示信息。资源重载后未知或非法定义不能用于新建；已有实体保留，并使用定义 ID 和缺失贴图回退。缺失警告按定义 ID 去重，单次加载周期最多记录 128 项。

内置 `icecore:fire` 使用用户提供的 64×64 Alex 细臂皮肤，皮肤 SHA-256 为 `DEE1B05D95088F08D98FD5F6A546AD780701A781BE2AE0B22C252BEA73C1029D`。

## 实体与转向

`npc.ModEntities` 注册 `icecore:npc`，`npc.NpcEntity` 保存定义 ID、锚点坐标和 45°吸附朝向。实体保留玩家尺寸选择框并可被准星选中，但不参与实体物理碰撞、推动、伤害、燃烧、重力、寻路、自然消失和维度切换；服务端每 tick 清零速度并在位置偏离时恢复锚点。

生成朝向由 `NpcRotation.snapToEightDirections` 归一化并四舍五入到 45°倍数。正向恰处于半区间时向更高的 45°方向取整；负向使用 Java `Math.round` 规则，例如 `-22.5°` 归为 `0°`。

服务端每 10 tick 只在实体周围 8 格 AABB 查询存活且非旁观的最近玩家；选中的目标在其余 AI tick 持续续期给原版 `Mob` 的 `LookControl`。这是必要的，因为原版控制器的目标缓存会在数 tick 后过期；只每 10 tick 设置一次会使实体反复回到空闲方向。头部限制、身体跟随、平滑转动和目标失效后的稳定行为均沿用村民等原版生物，不再由 ICEcore 自建旋转计时器或在 `tick` 中覆盖实体角度。右键交互将玩家设置为 LookControl 目标；实体上一帧/当前帧旋转由原版渲染器按 partial tick 插值，不在渲染路径扫描实体或分配模型。

## 命令与对话

- `/icecore npc summon <definition> [x y z]`：OP 2，新建定义对应 NPC；省略坐标使用命令源位置，朝向始终来自命令源，控制台采用南向 `0°`。
- `/icecore npc remove`：OP 2 且仅限玩家；服务端重新射线确认 8 格内、未被方块遮挡的 ICEcore NPC 后移除。

命令提示经 `feedback.PlayerFeedback` 发送到底部信息栏。右键实体由原版服务端交互包处理，复核玩家、距离和当前定义后调用 ChatBox 1.1.4 的 `ChatBoxCommandUtil.serverSkipDialogues`。ChatBox 是双端强制依赖和 `compileOnly` 本地依赖，不进入 ICEcore JAR。

内置对话位于 `data/icecore/chatbox/dialogues/npc/fire.json`，主题位于 `data/icecore/chatbox/theme/npc.json`。`greeting` 的两个节点互相用客户端 `JUMP` 跳转，“再见”跳到越界索引 2 关闭；不存在客户端上报的命令或玩法状态动作，因此不同玩家各自使用 ChatBox 客户端会话。

## 验证

`NpcRotationTest` 覆盖八方向、负角度、超过 360°和半区间规则；`NpcResourcesTest` 覆盖定义解析、双语键一致、对话 0→1→0 循环和越界关闭结构。提交前执行 Forge 47.4.10 的 `compileJava`、`test`、资源处理和 `clean build`，检查成品 JAR 含实体、皮肤、定义、主题、对话与双语资源且不含 ChatBox JAR。未经明确授权不启动客户端；八方向生成、持久化、碰撞、名称、转向、双手交互与多人并行仍需实机验收。

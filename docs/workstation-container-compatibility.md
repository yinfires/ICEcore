# 工作方块容器兼容

本文记录 ICEcore 厨具和其它工作方块共用的容器识别、返还、取回、提示及声音规则。新增工作方块需要处理桶、瓶、碗或其它带容器内容物时，先读取本文，不得在方块类中另写一套物品 ID 判断。

## 共用入口

2026-10-18：垃圾桶是整组取放例外。主手持物左键投入，普通/潜行右键取回最高非空槽位整组；生存与创造主手空手左键仍挖掘。保留原生三槽合并和满桶淘汰最早槽内容。碗、瓶、桶以STORED_AS_ITEM解析，按实际投入数返空容器、按手持匹配容器数及容量批量取回；剩余留桶，不跳过顶端容器化内容。批量交换在36格副本中模拟消耗和正常插入后提交，不用丢弃实体处理溢出。槽位移动/淘汰同步ICEcoreSlots精确容器元数据；不改变其它工作方块逐件取放。

2026-10-17：已放置茶与空茶杯每次空手领取一件进入正常背包，茶底取回同样按一件处理；蒸笼原生取出范围保留，剩余容量不足内容留槽。空蒸笼取下也收入背包，主手必须空手；持物投料失败不能回退取物或取下。茶杯倒茶、添杯、同类叠放及蒸笼堆叠/开合保留。

2026-10-17：通用空手取回每次一件收入正常背包，不直接替换手持物，不排除当前快捷栏格；先确认接收再扣减内容，背包满静默保留。冰柜/发酵桶/汤锅/炒锅共用服务与烤炉/混合碗同类入口采用WorkstationInventoryTransfer。带容器交换不变；砧板仍按专用批量取出规则。

2026-10-17：农夫乐事砧板通过 `CuttingBoardInteraction` 复用上述容器服务，无流体槽，采用 `STORED_AS_ITEM`。输入仅一件；配方全部结果独立留存。普通内容空手批量收入背包，容量不足保留剩余；遇到后进先出顶端容器化内容时停止并提示所需容器，不跳过；容器化原料及成品用匹配空容器逐件领取。仅容器要求有底部文本，其余失败静默。原料与成品分别持久化精确空容器元数据，旧超量原料不丢失，成品取完后才能继续加工。

2026-10-16：外部工作方块通过 `WorkstationAdapter`、`WorkstationInteractionService`、`ExternalWorkstationAdapters` 接入。冰柜、发酵桶、汤锅和炒锅共用逐件取放；料理台只在原生工具动作之后统一单件投入，空手及成品取出仍走上游入口。外部槽位顺序与精确空容器栈由 `WorkstationSlotState` 保存在 `ICEcoreSlots`，旧数据在首次交互恢复；原生配方结算替换物品时重新推导容器要求。

汤锅普通与灵活配方省略容器字段时从结果物品可靠元数据推导；显式 `carrier` 保留，`empty_carrier=true` 强制空手。成品每次领取一件；无容器成品采用物品显示，有容器成品保留汤底显示。真实汤底仍由上游独立入口处理。无匹配配方产生的迷之炖菜沿用上游DEFAULT_CARRIER，必须手持碗领取；该分支优先于结果元数据推导。

Jade物品只使用标准ItemStorage容器内容；非标准槽通过只读ViewGroup适配，禁止另加物品名称/数量行。真实流体使用标准FluidStorage/FluidView，排列为物品、流体、状态。茶壶成品替换茶底和展示流体槽；其它独立真实流体保留。成功取出物品、成品或流体不显示底部提示，声音保留；容器要求及其它失败通过PlayerFeedback，茶壶烹煮中持桶取液禁止且静默。

`workstation.WorkstationContainerCompat` 是统一解析入口：

- `describe` 将已装物品解析为 `FLUID` 或 `INGREDIENT`。
- `fillContainer` 使用空容器从工作方块流体槽生成完整的已装容器。
- `emptyMatches` 判断玩家手持容器是否满足槽位保存的取回要求。
- `isKnownEmptyContainer` 与 `isFluidContainer` 用于交互优先级和失败拦截。
- `fillSound` 与 `emptySound` 统一映射桶、瓶的成功声音。

无流体槽工作方块（当前为烤炉）必须使用 `WorkstationContainerInteraction.FluidContainerMode.STORED_AS_ITEM`：满牛奶桶等流体容器按完整普通 `ItemStack` 保存并参与 Ingredient 匹配，同槽保存空容器元数据；不得创建或读取工作方块 FluidTank。只有明确拥有真实流体槽的工作方块才使用 `REAL_FLUID_STORAGE` 分支。

输入解析顺序固定为数据包显式条目、Forge `IFluidHandlerItem`、通用空容器解析，最后才视为普通物品。通用空容器解析顺序为：物品栈级 crafting remainder、Forge `IFluidHandlerItem` 返回的空容器、已加载可选模组的容器接口、原版 `BowlFoodItem`、明确的碗/玻璃瓶/桶容器标签、药水瓶特例。显式带 NBT 条目优先于仅匹配物品 ID 的条目，物品 ID 条目优先于标签条目；不按注册名中的 `bowl`、`plate`、`pot` 等词猜测。

当前可选接口适配为森罗物语厨房 `IHasContainer#getContainerItem()`；只有 `kaleidoscope_cookery` 已加载时才进入该适配类。标签同时识别 `icecore:{bowl_container,glass_bottle_container,bucket_container,ingredient_container}` 与森罗物语厨房同名标签。其它模组可通过这些标签或数据包条目接入，不需要 ICEcore 写死物品 ID。

## 数据包格式

资源目录为 `data/<命名空间>/icecore/container_compat/*.json`。同一资源 ID 按数据包正常覆盖规则生效。

`empty` 接受物品 ID 字符串或完整 ItemStack 对象；对象可使用 `item`、`count`、`nbt`。`filled` 除上述格式外，还可使用标准 Ingredient 标签对象 `{ "tag": "命名空间:标签" }`。运行时固定按单件处理，标签只能决定哪些已装物品命中，仍必须提供一个精确 `empty` ItemStack。`behavior` 只能为 `fluid` 或 `ingredient`。

流体容器示例：

```json
{
  "filled": {
    "item": "minecraft:potion",
    "nbt": "{Potion:\"minecraft:water\"}"
  },
  "empty": "minecraft:glass_bottle",
  "category": "bottle",
  "behavior": "fluid",
  "fluid": "minecraft:water",
  "amount": 250
}
```

普通容器化食材示例：

```json
{
  "filled": "minecraft:honey_bottle",
  "empty": "minecraft:glass_bottle",
  "category": "bottle",
  "behavior": "ingredient"
}
```

标签批量映射示例：

```json
{
  "filled": { "tag": "examplemod:foods/on_iron_plate" },
  "empty": "examplemod:iron_plate",
  "category": "container",
  "behavior": "ingredient"
}
```

`fluid` 条目必须提供已注册流体和正数 `amount`。`ingredient` 不声明流体；投入时返还 `empty`，工作方块内部仍保存 `filled` 作为配方输入。

内置默认条目为：奶桶→桶和1000 mB奶，水瓶→玻璃瓶和250 mB水，蜂蜜瓶→玻璃瓶和普通食材。其它碗装、瓶装或桶装食材优先使用其物品栈级 crafting remainder 自动推导。

## 工作方块交互契约

| 当前内容 | 手持物 | 行为 |
| --- | --- | --- |
| 流体 | 能完整盛装的匹配空容器 | 优先取出流体并交换为已装容器 |
| 流体 | 容量不足、流体不足或流体不兼容的流体容器 | 静默阻止，不作为普通输入 |
| 容器化食材位于后进先出顶端 | 匹配槽位要求的空容器 | 取出顶端逻辑食材并消耗一个空容器 |
| 容器化食材位于后进先出顶端 | 空手 | 不取出，使用本地化物品名提示所需容器；不得跳过该项取更早输入 |
| 顶端物品不匹配 | 错误的已知空容器 | 不继续搜索较早物品，沿用流体容器失败拦截或默认投料 |
| 无匹配内容 | 空碗、空瓶等 | 可按配方作为普通输入 |
| 普通食材位于后进先出顶端 | 空手 | 取出顶端物品；持容器不能越过它 |
| 输出有有效 `carrier` | 匹配容器 | 消耗容器并领取输出 |

同一个空玻璃瓶既可能取流体，也可能取蜂蜜瓶食材，因此交互优先级必须为：尝试流体取出、按后进先出检查顶端容器化食材、失败的流体容器静默拦截、普通物品路径。真实流体槽取出不受物品输入顺序限制；没有流体槽的工作方块必须严格遵守后进先出。

配方输入通过实际 Ingredient 候选物品自动调用共用解析器。单独写入配方的空碗或空瓶没有已装内容或 remainder，仍是正常食材。

混合碗输出的有效盛装要求按“显式优先、缺省推导”解析：结果声明 `carrier` 时完全采用该 Ingredient；未声明时仅从结果物品本身的显式映射、crafting remainder、可选接口、原版食物类型或兼容标签推导一个精确空容器。没有可靠元数据时保持空手领取；不从配方输入、显示名称或注册名猜测器皿。需要接受多个替代器皿时必须继续显式声明多物品或标签 `carrier`。

## 状态、同步和复用要求

每个保存逻辑物品的槽位必须同时保存对应的空容器 ItemStack；不能只保存容器类别或物品 ID，否则带 NBT 容器无法精确匹配。槽位清空、移动、结算、强制拆除、网络快照和 NBT 都必须同步处理这份元数据。

旧存档缺少输入容器元数据时，加载阶段用当前兼容表和保存的逻辑物品重新推导 `INGREDIENT` 空容器；旧混合碗输出缺少 carrier 时，按保存的结果物品重新推导有效 carrier。加载过程不得触发内容变化回包。

Forge物品和流体能力若只为 Jade 展示，必须返回副本并拒绝 `insert`、`extract`、`fill` 和 `drain`，避免自动化绕过工作方块交互规则。

## 声音

- 普通物品及普通容器化食材投入/取出：`ITEM_FRAME_ADD_ITEM` / `ITEM_FRAME_REMOVE_ITEM`，音量 `0.8`、音高 `1.1`。
- 桶装流体投入/取出：`BUCKET_EMPTY` / `BUCKET_FILL`，音量和音高 `1.0`。
- 瓶装流体投入/取出：`BOTTLE_EMPTY` / `BOTTLE_FILL`，音量和音高 `1.0`。
- 只有成功改变内容时播放；容量不足、错误容器、流体冲突和其它失败路径不播放成功声音。
- 工作方块的持续运转声必须使用由实际工作状态驱动的客户端可控实例，停止、卸载或切换世界时能够主动结束，不能把不可终止的长音频直接广播后留在客户端。是否允许重复实例按具体工作方块定义：混合碗每次成功搅拌播放一个 Farm & Charm 原搅拌声实例，连续追加允许叠音且不打断已有实例；离开搅拌阶段时所有尚存实例立即停止。

## 验证

至少覆盖：四个水瓶累计1000 mB并逐瓶取回、蜂蜜瓶返空瓶且必须用空瓶取回、碗装食材、错误容器提示、无匹配空容器作为普通输入、NBT精确条目优先级、物品条目高于标签条目、Forge流体回退、crafting remainder回退、`BowlFoodItem`、可选 `IHasContainer`、碗/瓶/桶标签、显式输出 carrier 覆盖自动推导、旧存档元数据推导、缺少可选模组时正常加载和只读Jade能力。

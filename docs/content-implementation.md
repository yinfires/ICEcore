# ICEcore 内容实现索引

Last updated: 2026-10-05

## 使用与维护

- 2026-10-10 Git 文档边界：`.gitignore` 移除 `/docs/` 强制忽略并明确放行 `/AGENTS.md` 与 `/docs/`，允许工作区规则与全部文档被提交、推送；`tmp/`、`tool/`、`libs/`、`wiki/`、缓存和构建产物继续忽略。验证使用 `git check-ignore` 与 `git status --short --untracked-files=all`，不执行提交或推送，无运行时开销。
- 工作区规则独立维护于根目录 `AGENTS.md` 与 [开发与调试规则](workspace-development-rules.md)，阅读路由在 `INDEX.md`。2026-10-10 已移除外部规则阅读入口与旧规则快照，必要的开发、同步、资源、构建和调试要求已直接写入本地；Java 17、中文更新日志和独立 Wiki 约定保持为项目规则。验证方式：严格 UTF-8 解码、规则依赖扫描、本地 Markdown 文件链接解析与 Git 忽略边界检查；仅文档变化，不涉及代码、资源、运行时性能或玩家行为。
- 每次实现或修改后，必须更新对应条目；新增较大领域时才拆分专题文档，并从本文件链接。
- 条目只记录可验证事实：玩家行为、代码入口、资源位置、关键实现链路和验证方法。
- 只有新增或改变长期使用说明时，才同步维护项目根目录 `wiki/` 独立仓库中的 Wiki 页面；长期使用说明包括操作步骤、命令、权限、配置字段、文件位置、生效方式和必要限制。纯视觉表现、渲染方式、内部实现、性能修复和开发期 bug 修正不写入 Wiki。Wiki 使用 GitHub 原生 Pages 导航，首页文件为 `Home.md`，不添加 `_Sidebar.md`；页面名称由 GitHub 显示，正文不重复同名一级标题，不增加“功能”等总板块标题，说明文字后直接按功能列出长期有效的简短说明，各项内容使用 `###` 级别标题，不使用更新日志的版本/日期结构，也不重复实现细节。
- Wiki 详细页面只写正常游玩与管理配置的操作方式，并用分节明确区分两者；不写“面向玩家/开发者”等受众句式，不写实机中明显可见的表现、渲染触发条件或内容工作原理。页面标题使用稳定的内容名词或功能名称，不使用“使用前准备”等过程性标题。Wiki 改动使用 `git -C wiki` 单独提交和推送，不能作为主仓库文件提交。
- 删除实现时同步删除或改写条目，不保留已经失效的方案与占位说明。

## 当前状态

### 物品提示与客户端描述覆盖

- 2026-10-18 默认描述约定：后续新增或修改物品描述，在配置中关闭原始描述后，统一通过下述客户端 `config/icecore/item_descriptions.json` 与现有 `ItemDescriptions` 覆盖链路提供，避免给物品添加原生描述；用户明确要求原生描述时才例外。规则承载于根目录 `AGENTS.md`；本次仅记录约定，未修改物品实现或客户端配置。验证方式：UTF-8 回读规则与本条目、检查 Git 差异。
- `icecore-client.toml`：`tooltip.hideOriginalDetails=true`、`tooltip.hideFoodEffects=true`，Forge 标准配置入口双语说明。原始提示过滤在 `ItemStack.m_41651_` 调用 `ForgeEventFactory.onItemTooltip` 之前执行，保留名称组件原对象；不清空最终 GatherComponents，因此 JEI 烤炉批量/时间、咖啡馆价格和其它事件附加信息保留。效果开关独立过滤物品效果段，不改变玩法。
- `tooltip.ItemDescriptions` 加载客户端 `config/icecore/item_descriptions.json` 的 `items` 映射；完整物品 ID → 字符串/文本组件数组。存在条目替换原正文，空数组只保留名称，删除恢复开关行为。`ItemTooltipEvents.Registration` 在启动/F3+T 重载，缺文件清空，错误保留上一快照；无模组内置示例。外部文件在指定客户端配置目录，不要求服务端同步。
- 客户端可选 Mixin：`GrillingFoodTooltipMixin` 阻止烟火 FoodTooltip 两个效果入口；`FarmersFoodTooltipMixin` 阻止农夫乐事 TextUtils 效果入口；国味真实注册复用 Cookery `BowlFoodOnlyItem`→`FoodWithEffectsItem`，`CookeryFoodTooltipMixin` 只拦截其 PotionUtils 效果构建调用。依据烟火 1.1.1、农夫乐事 1.3.4、国味 1.1.14、厨房 1.4.1 实际 JAR 核验。结构兜底递归识别 effect/potion 翻译组件及关联属性段，不按语言或颜色猜测。
- `PonderItemTooltipMixin` 阻止 Create 6.0.8 内嵌 Ponder 1.0.91 的 addToTooltip；`CreateItemDescriptionTooltipMixin` 阻止 Create ClientEvents.addToItemTooltip 的 Shift/Ctrl 概要；`LibItemDescriptionTooltipMixin` 阻止 CozyCafe 1.10 内嵌 LibTooltips 2.3 的 modifyItemTooltip（常驻介绍、Shift 提示与展开正文），不影响另外添加的价格。`JeiItemModNameTooltipMixin` 拦截 JEI 15.49 的两种添加模组名入口和 getModNameForTooltip，覆盖物品、流体等全部 ingredient 类型，保留其余原列表。所有外部目标以 class 资源门禁，均在 client 列表，无可选目标提前类加载。
- 2026-10-15 用户截图回归：实际食品效果的图标、时长、概率由 Quality Food 2.4.3 `events.ForgeEvents.addEffectTooltip(GatherComponents)` 追加 `core.EffectComponent`，不属于文本列表。`QualityFoodTooltipMixin` 在该方法 HEAD 按 hideFoodEffects 取消，覆盖静态效果、动态食物属性与品质修改效果，不取消其另一个 addTooltip 品质说明入口，不改变食用效果。厨房现用 1.5.1 已额外核验原 Cookery 拦截目标不变。
- 锦致装饰 Supplementaries 3.1.43：`SupplementariesPlaceableTooltipMixin` 定向替换 ClientEvents.onItemTooltip 第二次 Supplier.get（PLACEABLE_TOOLTIP），hideOriginalDetails开启时返回false，仅隐藏额外可放置说明，覆盖所有 SuppAdditionalPlacement 物品；关闭后读取原配置。其它提示、配方、价格与自定义描述保持；client注册及class资源门禁，ASM测试锁定字段与ordinal。
- 性能：仅悬停构建提示时 O(当前提示组件数)；覆盖按物品 ID 哈希查询。无 tick、注册表遍历、反射、网络或文件轮询。文件只在资源重载读取；不可变快照整体替换，内存随当前配置规模有界，复制描述组件防止外部修改污染缓存。
- 验证：TooltipRulesTest 覆盖开关组合、附加配方/价格保留、名称样式、效果结构、多行/翻译/空数组、坏配置原子保留及删除恢复；ItemTooltipMixinTest 核对真实 Forge 调用及可选客户端门禁。客户端三款食品、JEI/咖啡馆和思索显示仍待授权实机验收；未启动客户端。
- 2026-10-15 验证结果：Forge 47.4.10 独立 clean 后完整 build 通过（151 项测试），Forge 官方 promotions 当前最新 47.4.28 完整 build 同样通过，并以 dependencies 输出确认实际坐标为 1.20.1-47.4.28_mapped_official_1.20.1；最终恢复基线构建。生产 JAR 已核对原始提示 SRG 选择器、Forge 调用、Cookery PotionUtils 调用及不打包外部类；双语键一致，CodeGraph 已更新，Git 无生成文件。已在指定客户端创建空 item_descriptions.json，未部署 JAR。
- 残留修正验证：实际 Create 6.0.8、LibTooltips 2.3、Quality Food 2.4.3 JAR 的 ASM 回归验证真实方法描述符、Shift/Ctrl 字段及图形 EffectComponent 分配；模组名回归禁止 ItemStack 类型限制，确保流体覆盖且不清空其它提示。Forge 47.4.10 独立 clean 后 build 通过（154 项），47.4.28 完整 build 通过，最终恢复基线构建。新增三处拦截仅在原有悬停入口 O(1) 读取配置，提前省去文本/效果组件分配，无 tick、I/O、网络或日志。未启动客户端，四张截图对应场景仍待用户复测。

### Dew Drop 灌溉与 Bakery 水槽兼容

- 目标：Farmland Growth 9.0、Watering Cans 1.0.2、Bots Lib 4.1.1、Bakery 2.0.6。厨房水槽真实类为 Farm & Charm 1.0.14 的 SinkBlock。
- 高级洒水壶 Shift 右键改为普通单格浇水，保留上游容量、扣水、冷却、补水及事件。种子袋作物所有部分经 `SeedBagCrops.rootOf` 定位根部耕地，肥料也以根部耕地为 `UseOnContext` 调用原物品逻辑；失败终止本次手部重试，避免收获或重复操作。
- 关闭 Dew Drop 瓶桶右键与喷溅水瓶灌溉入口，不取消其它正常用途。雨水、耕地正上方水、保湿肥料行为保留。原版附近水检查水平 ±4、Y 同层/上一层，Forge 另有 FarmlandWaterManager ticket；现有两个存档 noRandomTick=true，自定义耕地不执行原版随机刻，附近水不能作为必然有效方式。
- 洒水器成功放置后处理已加载区域同层/下一层耕地，半径按等级 1/2/3/4，遵循 checkSprinklers，最大 162 个位置；无新增 tick 扫描、强制加载、反射或周期日志。
- 仅 bakery:kitchen_sink：点击任一半检查完整结构、底部 filled=true、壶未满且无冷却；服务端清空两半，发布原 WateringCanFillEvent 补满并播放声音、设置冷却。上游以底部 filled 为权威，不要求上半满水。空槽/满壶不消耗槽水。
- 入口：compat.dewdrop 的 DewDropIrrigation、DewDropIrrigationEvents、DewDropSprinklers，三款 DewDropIrrigation*Mixin 用类资源检查门控。事件仅在两个 Dew Drop 模组均存在时注册；无新命令、配置或网络协议。
- 冒险开垦：SeedBagFarmingEvents.tillInAdventure 两端调用真实 ItemStack.useOn，finally 恢复 mayBuild 原值；HoeItem 客户端只预测声音/动作，服务端改土并广播给其它玩家。
- 验证：DewDropIrrigationContractTest 覆盖生产描述符、加载时序、根部入口、水槽保护和扫描边界；实机声音、作物交互、主副手及多人同步仍待用户授权。
- 2026-10-10 验证结果：Forge 47.4.10 单独 clean 后完整 build、后续事务修正后的 build 成功，122 项测试通过。生产 JAR 已核对选择器、SRG 调用、兼容辅助类型在 Mixin 包之外且未打包第三方依赖；CodeGraph 322 文件索引正常。官方最新 47.4.26 已确认，追加构建实际解析 net.minecraftforge:forge:1.20.1-47.4.26:userdev，但依赖下载失败，尚未获得该版本构建通过结论；gradle.properties 已恢复 47.4.10。

### 种子袋种植系统

| 字段 | 内容 |
| --- | --- |
| 作物范围 | 19 种：小麦、胡萝卜、马铃薯、甜菜根（原版）；大豆、红豆、黄瓜、茶树（youkaisfeasts）；草莓、玉米（farm_and_charm）；红薯、折耳根（kaleidoscope_grilling）；番茄、辣椒（kaleidoscope_cookery）；茄子（kaleidoscope_chinesefood）；洋葱、卷心菜、稻米（farmersdelight）；青柠（collectorsreap:lime_bush）。规则只作用于这些"种子袋作物"；以后加入 `ModItems.SEED_BAG_DEFINITIONS` 的作物自动套用全部规则。 |
| 种植 | 种子袋右键任意 `FarmBlock`（原版/dew 变种/沃土耕地/施肥耕地/dew 花盆）且上方为空气时种植，冒险模式可用（`RightClickBlock` 在原版冒险检查前处理）。放置状态先走作物自身 `getStateForPlacement`（保留折耳根红变种、黄瓜 rooted 等），为空则用默认状态，再以 `canSurvive` 校验。任何"放置种子袋作物方块的 BlockItem"（原种子、马铃薯/洋葱/红薯/折耳根/红豆/大豆等本体、FD 稻米、黄瓜种子、青柠种子）在 `RightClickBlock` 中 `useItem=DENY`，食用等非方块用途不受影响。 |
| 收获 | 去除原模组空手右键收获：作物成熟时对其 `useBlock=DENY`（未成熟交互如黄瓜绑绳保留）。锄头（`ToolActions.HOE_DIG`）右键成熟作物：1 个成品经 `ItemHandlerHelper.giveItemToPlayer` 进背包，根方块 age 归 0（稻米同时 supporting=false），双格/分体上半与稻穗静默移除；黄瓜按被点藤段单独成熟/重置。锄头左键：客户端 `InputEvent.InteractionKeyMappingTriggered` 拦截攻击并发 `ServerBoundHoeBreakCropPacket`（同目标 5 tick 去重），服务端校验距离 6 格、主手锄头、目标为种子袋作物并发布 `BlockEvent.BreakEvent`（尊重建造系统受管方块），整株静默移除，1 个种子袋 + 任一段成熟时 1 个成品进背包；所有模式一致。 |
| 成品与掉落 | 成品固定 1 个；辣椒红/青各 50%。非锄头破坏走覆盖的战利品表：根方块固定掉 1 个种子袋（茶树/青柠仅 half=lower，黄瓜仅 rooted=true），成熟另掉 1 个成品（茶树 age≥6，青柠 age=4，稻米由 `rice_panicles` age=3 掉 `farmersdelight:rice_panicle`，锄头收获同为稻穗）；茄子上游重写 `getDrops`，由 `SeedBagCompatEggplantMixin` 恢复走战利品表。 |
| 土壤与特例 | 红豆、茶树：`BushBlockSeedBagFarmlandMixin` 在 `BushBlock.canSurvive` 头部对下方为 `FarmBlock` 的根方块直接放行（光照检查仍由 CropBlock 先执行）。稻米：`SeedBagCompatRiceMixin` 让 `RiceBlock.canSurvive` 在下方为 `FarmBlock` 时成立，并令 `getFluidState` 恒为空（存档内原水中稻米的水源会消失，已确认接受）。茶树：`SeedBagCompatTeaMixin` 把 `TeaCropBlock.getMaxAge` 压到 6（`ModItems.TEA_HARVEST_AGE`），随机刻、骨粉、dew 每日生长止于叶片阶段，Jade `CropProgressProvider` 以 `CropBlock.isMaxAge` 显示成熟；`SeedBagCompatDewGrowthMixin` 接管种子袋作物每日推进，茶树使用上游 `setGrowth` 保证上半格同步。 |
| 干涸消失 | `BlockStateSoilRemovedMixin` 注入 `BlockStateBase.onRemove`（仅服务端调用）：任一 `FarmBlock` 变为非 `FarmBlock` 时（dew 每日干涸、原版 `turnToDirt`、沃土/施肥耕地退化、铲子、活塞、以后的其它替换方块），上方根方块属于种子袋作物则自顶向下以 `UPDATE_CLIENTS|UPDATE_KNOWN_SHAPE` 移除上部、`UPDATE_ALL` 移除根，全程不掉落。判定只依赖"是否仍为 FarmBlock"，不依赖替换成什么方块。 |
| 未开垦的泥土 | `icecore:untilled_dirt`（`farming.ModFarming`/`UntilledDirtBlock`）：属性复制原版泥土，方块/物品模型直接引用 `minecraft:block/dirt`，掉落自身，加入 `minecraft:mineable/shovel`，不加入 `minecraft:dirt`。`getToolModifiedState(HOE_TILL)` 在上方为空气时返回注册表中的 `minecraft:farmland`（即 dew 覆盖实现）。冒险模式锄头右键时临时开放 `mayBuild` 调用 `ItemStack#useOn`。dew `CustomFarmland.tick` 每日干涸的 `setBlock(DIRT)` 由 `SeedBagCompatDewFarmlandMixin` 改为本方块，原版 `FarmBlock.turnToDirt` 由 `FarmBlockUntilledDirtMixin` 改为本方块；沃土/施肥耕地仍退化为各自土壤。 |
| Jade 图标 | `compat.jade.ICECoreJadePlugin`（`@WailaPlugin`，未装 Jade 不加载）以 `registerBlockIcon` 对 `BushBlock` 注册图标提供器，优先级 10000 排在上游之后；对 `SeedBagCrops.find` 命中的任意部分（茶树/青柠上半、稻穗、黄瓜藤段）返回对应种子袋 `ItemStack` 图标，其它方块原样返回。编译依赖 `libs/Jade-1.20.1-Forge-11.13.3.jar`（compileOnly，取自客户端 mods）。提供器 `isRequired=true`，配置键 `config.jade.plugin_icecore.seed_bag_icon` 已备翻译。 |
| 每日生长 | 安装 Dew Drop 时，`SeedBagCompatDewRandomTickMixin` 在 `BlockStateBase.m_222972_` 头部按 `SeedBagCrops.find` 的 O(1) 身份映射阻止全部种子袋作物随机刻（含稻穗、上下半与藤段），同时覆盖时间倍率补偿；未安装 Dew 时不加载此钩子。`SeedBagCompatDewGrowthMixin` 在上游每日土壤/季节判断后的 `handleCropGrowth` 接入 `compat.dewdrop.SeedBagDailyGrowth`，保留 not_daily、肥料与 Forge 生长事件。单格保留原状态属性；茶树调用真实 setGrowth 同步上半，青柠走自身 performBonemeal（含光照/空间检查），不再依赖随机刻。黄瓜保留 rooted/ropelogged，按旧藤段有界遍历（最多 64），每段调用 postGrowth，新增藤段下次结算才推进。两种原方法通过包外 DailyDoubleCrop/DailyClimbingCrop 接口和可选 Invoker 接入，不直接依赖可选目标类。 |
| 稻米阶段与提示 | Dew 9.0 ModGrowthCompat.growFarmersDelightRice 的 withAge 参数字节码实际为 age + age * getFertilizerIncrease，age=0 恒为 0；由每日适配替代该分支。RiceGrowth 统一 0..7：根 age 0→3，空位生成稻穗 age=0，再到稻穗 age=3；普通土壤共 7 次有效结算，初始肥料增量限根年龄 3，上方阻塞停在抽穗前，成熟停止，收获使用原重置逻辑。Jade RiceProgressProvider 在根与稻穗上均归一根坐标计算整株进度，复用 Jade 11.13.3 CropProgressProvider 的 tooltip.jade.crop_growth / tooltip.jade.crop_mature 翻译键、%.0f%% 四舍五入和 IThemeHelper info/success 样式，统一显示“生长状态：百分比／成熟”；不显示自定义整株文案或剩余次数。 |
| 代码入口 | `item.SeedBagDefinition`（`Shape`: SINGLE/DOUBLE/SPLIT/CLIMBING）、`item.ModItems`（定义与注册）、`item.SeedBagItem`、`farming.SeedBagCrops`（结构/成熟/收获/移除/种植，注册表解析后按 Block 身份映射）、`farming.SeedBagFarmingEvents`、`farming.SeedBagClientEvents`、`farming.SeedBagLoot`、`network.ServerBoundHoeBreakCropPacket`（协议版本 10，id 22）。可选模组 mixin 以 `SeedBagCompat` 命名并由 `ICECoreMixinPlugin` 按目标类存在与否加载。 |
| 资源与渲染 | 19 个种子袋均为独立 `minecraft:item/generated` 模型，单层引用 `icecore:item/<作物ID>_seed_bag`，PNG 位于 `assets/icecore/textures/item/`。2026-10-08 原样接入用户提供的 `D:/下载/ZZB[种子包].zip` 中 19 张透明 16×16 成品图；“柠檬”对应现有青柠 `lime`，“黄豆”对应大豆 `soybean`，“折耳根”对应 `houttuynia`，“青红辣椒”对应 `chili`，空袋素材不注册或打包。源素材与最近邻预览在 `D:/Minecraft/Mod Develop/模型/种子袋/`。保留物品 ID 和现有中英文语言键，中文名称统一为“折耳根种子袋”。Jade 返回相同种子袋 ItemStack，因此复用正式物品模型；已核对 Jade 11.13.3 优先级升序及后续图标覆盖逻辑。不使用运行时叠加渲染。 |
| 性能 | 全部事件驱动：右键/左键事件与包按玩家操作触发；`onRemove` 钩子常态只有一次 `instanceof`；`BushBlock.canSurvive` 钩子为一次 IdentityHashMap 查找；无 tick 扫描、无反射、无日志。 |
| 验证方法 | 2026-10-15 新增 `RiceGrowthTest`（7 次生命周期、抽穗、肥料与收获重置）和 `SeedBagGrowthContractTest`（随机刻/补偿同入口、生产描述符、可选门控、各结构与提示契约）；编译、窄测试及 Forge 47.4.10 单独 clean 后完整 build 通过，全量 135 项测试通过；成品 JAR 的生产 SRG 调用、Mixin 入口和包外桥接已核对，未打包第三方依赖。这里是纯逻辑/源码契约与构建验证，不是游戏环境行为测试；随机刻拦截、所有作物每日推进、Jade 画面与可选模组缺失的运行验收仍待授权。`SeedBagFarmingContractTest`（定义↔模型↔语言↔战利品表、辣椒等分、无原种子掉落、mixin 注册与可选门控、未开垦泥土资源）、`SeedBagRenderingContractTest`；2026-10-08 两项种子袋窄测试及 Forge 47.4.10 `gradlew.bat clean build` 全量测试通过，成品 JAR 的 19 个模型、19 张 PNG 与正式源资源逐张 SHA-256 一致，中文名称已核对；源图接触表为最近邻静态预览，非游戏截图，Jade 实际画面待用户授权验证。mixin 目标签名均以客户端 mods 目录 JAR 的 javap 与 Forge 47.4.10 映射核对。实机待用户授权验证：各作物种植/收获/左键破坏、dew 每日干涸后作物消失与未开垦泥土生成、茶树上半格与 Jade 成熟、稻米无水显示、青柠长满。 |

### 标准配置保存与运行时刷新

- `config.ICECoreClientConfigEvents` 监听 Forge `ScreenEvent.Closing`，通过客户端任务队列在 `Screen.removed()` 保存完成后刷新本模组 COMMON/CLIENT 规格缓存；只处理五个布尔配置值，不加载文件、不扫描注册表、不依赖编辑器类名，无 tick 或渲染轮询。客户端提示开关下一次悬停即读取新值。
- 单人游戏通过 `getSingleplayerServer()` 调度 `ICECoreConfigEvents.synchronize`；远程客户端仅刷新本地配置，不覆盖服务器权威快照。COMMON 重载事件与界面关闭共用服务端刷新入口，所有缓存读取、玩家列表访问和发包在服务端线程执行；只在禁食/兼容值变化时广播两个布尔值，重复保存去重，旧服务器已退出时丢弃任务。每次界面关闭固定 O(5)，变化时 O(在线玩家数)，快照内存 O(1)。
- 事实来源：Forge 47.4.10 `ForgeConfigSpec.save/afterReload`、`ConfigFileTypeHandler.ConfigWatcher.run` 和 `ModConfigEvent.Reloading`；原监听直接在任意事件线程遍历玩家，保存兜底此前缺失。普通文件热重载由 Forge watcher 负责，本补丁不重复读盘或保存。
- 验证：`ConfigReloadTest` 真实内存配置覆盖底层值更新后的旧缓存、双向切换、客户端提示缓存、服务端队列、重复广播去重和停服丢弃；源码门禁固定关闭后排队及单人服务器边界。Forge 47.4.10 独立 clean 后完整 build 通过（158 项测试），官方最新 47.4.28 完整 build 通过，dependencies 确认实际解析坐标为 1.20.1-47.4.28_mapped_official_1.20.1；最终恢复基线构建。实机配置编辑器保存、暂停菜单及专服同步仍待用户授权验证。2026-10-16。

### 全局禁止食物食用配置

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 管理员可通过全局配置禁止所有玩家食用原版及模组食品、牛奶、药水和水瓶；开启后外部交互先处理右键，只有物品真正进入持续使用阶段时才取消食品使用。客户端记录本次被禁食取消的操作，在 LocalPlayer.startUsingItem 返回后清理无效使用标志，并跳过该次 ItemInHandRenderer.itemUsed 的手持高度归零；服务端同时保留开始使用事件的权威取消。方块放置、容器交互、实体交互和同一物品已经成功处理的其它右键用途均放行。默认关闭。 |
| 配置位置 | `config/icecore-common.toml` 的 `food.disableConsumption`；Better ModList 20.1.1 通过 Forge 标准配置屏幕入口显示带中英文翻译的开关。本地或单人游戏保存后立即生效；专用服务器使用服务器自身配置，远程连接时开关只读并提示由服务器控制，不能代替服主修改服务器文件。 |
| 代码入口 | `config.ICECoreConfig`、`config.ICECoreClientConfig`、`food.FoodConsumptionClientInputEvents`、`food.FoodConsumptionEvents`、`food.FoodConsumptionRules`、`mixin.FoodUseFeedbackMixin`、`mixin.FoodUseLocalPlayerMixin`、`food.BlockedFoodUse`。Better ModList 直接读取 Forge 标准配置规格，不再注册自定义配置屏幕。 |
| 实现链路 | 模组初始化注册 `ModConfig.Type.COMMON` 与 `ModConfig.Type.CLIENT` → 玩家登录时服务端发送禁食与模组兼容开关快照，服务端配置热重载时向在线玩家广播 → 客户端不再按食品类别提前取消输入或 `MultiPlayerGameMode.useItem`，先让方块、实体、串制、投料和容器等外部交互处理右键 → 只有物品真正进入持续使用阶段时，客户端/服务端 `LivingEntityUseItemEvent.Start` 才按配置取消；客户端通过 `BlockedFoodUse` 记录同次点击的玩家和物品引用；`FoodUseLocalPlayerMixin` 在 RETURN 且 useItem 为空时清理被 LocalPlayer 重写的使用标志；`FoodUseFeedbackMixin` 只跳过同次被拒绝物品的 itemUsed 高度重置，保留交互返回值及其它物品反馈 → `FoodConsumptionRules` 先检查动态 `ItemStack#getFoodProperties(player)`，再按森罗物语真实 `raw_skewers`/`grilled_skewers` 标签及饮品/茶杯/果汁桶物品类判定；不硬编码示例物品 ID。Better ModList 直接按 Forge 配置规格生成默认界面，不需要对 Better ModList 建立依赖。 |
| 识别边界 | 自动覆盖遵循原版/Forge `FoodProperties` 契约的静态与动态食品；饮料与烤串按目标模组标签或相关物品类逻辑识别，不按单个物品 ID 维护名单。完全绕过原版持续使用与食物属性、在自定义 `use` 中直接扣物品和修改饥饿值的外部内容需要按其真实 API 单独兼容。 |
| 性能与生命周期 | 无 tick、注册表扫描或文件轮询；只在右键最终回退到物品直接使用及服务端开始持续使用时各读取一次布尔状态与动态食物属性，复杂度和额外分配均为 O(1)。每名玩家登录发送一次小型配置包，配置热重载时向每名在线玩家发送一次，断线即清除客户端快照。 |
| 验证方法 | `FoodConsumptionRulesTest` 固定动态 `getFoodProperties` 判定、玩家范围、`LivingEntityUseItemEvent.Start` 取消点、登录同步与客户端权威快照，并禁止前置食品取消；反馈契约检查 Start 标记、RETURN 清理、itemUsed 条件抑制及正常反馈放行；执行 `compileJava`、完整测试、资源处理和 Forge 47.4.10 `clean build`，检查语言 JSON、网络协议与生产 JAR。实机需分别验证单机/专服、食物、饮料型食品、药水、牛奶、工具及双用途物品。 |
| 最后修改 | 2026-10-01，禁食改为只在 `LivingEntityUseItemEvent.Start` 取消真正开始的食品使用；根据 Forge 47.4.10 字节码修复 itemUsed 高度归零和 LocalPlayer 后置标志覆盖，移除事件内无效 stopUsingItem；实机动画验收待用户确认。移除客户端输入和 `MultiPlayerGameMode.useItem` 的通用食品拦截，使串制、投料和容器交互不再依赖专用白名单。 |

### Collectors Reap 派（Pie）模型兼容修复

- 2026-09-26：核对客户端 `collectorsreap-1.20.1-1.5.5.jar` 后确认 `lime_pie` 与 `portobello_quiche` 的 blockstate、完整模型、3 个切片模型及对应方块纹理原本都存在；紫黑块原因是其 8 个放置模型引用了不存在的 `farmersdelight:block/pie` 与 `pie_slice1..3` 父模型，而当前 Farmers Delight 1.3.4 实际提供 `template_pie` 与 `template_pie_slice1..3`。
- ICEcore 覆盖资源：`assets/collectorsreap/models/block/{lime_pie,lime_pie_slice1..3,portobello_quiche,portobello_quiche_slice1..3}.json`，仅修正父模型名称，继续复用 Collectors Reap 原有纹理、UV 与切片几何；不增加运行时逻辑、tick、网络或对象分配。
- 验证：从实际客户端 JAR 解压核对 blockstate/模型/纹理条目和 Farmers Delight 父模型条目；执行 `gradlew.bat processResources --rerun-tasks` 与 `gradlew.bat clean build`，检查生产 JAR 覆盖资源及 JSON 引用。

### 厨具统一方案静态核对（未实现）

- 2026-09-09：核对森罗酒馆雪克杯配方、投料和倒出路径，以及幻想乡料理台的寿司、拼盘、团子串迁移边界。雪克杯普通饮料需要输出兼容；料理台建议先扩展装盘/穿串阶段。依据、三例原配方及待验证项见 [厨具统一静态核对](kitchen-unification-audit.md)。未安装测试配方、未修改运行代码、未启动客户端；无新增运行时性能开销。

### 物品 ID 驱动建模流程与参考目录

- 2026-09-08 三款独立模型已输出：egg_fried_rice（40元素/205面）、scramble_egg_with_tomatoes_rice_bowl（50/255）、braised_beef（31/156）。两碗复用已确认五元素干净碗，牛肉复用 DD food_plate 原几何/贴图；来源坐标、UV 与像素等效已自动验证。均为3张128×128纹理，静态无运行工作；独立bbmodel、JSON、PNG、试用包与五视图位于指定模型目录。详见 [三款食物模型](three-food-models.md)。未接入模组，不需要 Gradle 构建；未做游戏实测。

- 2026-09-08：建立 [能力尝试流程](item-modeling-pilot.md) 和 [模型参考源目录](model-reference-library.md)，固化一格以内尺寸、配方与色块证据分级、三视图一致性及建模门禁。
- 首例 `kaleidoscope_cookery:scramble_egg_with_tomatoes` 已核对默认物品模型、16×16 原图、两套默认配方与中英文名；原图和最近邻预览放在指定模型目录。Cookery 1.4.1 与 Display Delight 1.7.0 已归档到 libs 并记录 SHA256 和具体参考模型路径。
- 已制作盘装初版：用户指定 DD food_plate 木盘，40 元素/201 面，14×4.426×14，3 张内嵌纹理；JSON、PNG、本地试用 ZIP 和编辑器五视图均在模型目录。UV/依赖/边界静态检查与 bbmodel、JSON 空工程重导入完成；UV 编辑器界面检查和游戏实测未完成。详见能力尝试流程。未修改运行时实现，不需要 Gradle 构建。

### 暖木奶油背包与快捷栏资源包

- 2026-09-08 v2 修订：用户指出选中框外扩、底边短线与参考落实不足。新增强制视觉门禁并升级 ERR-20260908-02；制作入口 `D:/Minecraft/Mod Develop/模型/cozy-inventory-v2/scripts/build_pack.py`，导出 `export/Cozy-Inventory-1.20.1-v2.zip`，同目录 `preview/alignment.png` 和 `preview/overview.png` 保存实际输出合成。
- 参考特征映射：蜂蜜木质层次→参考顶部木框裁片用于上下木边；奶油纸面→参考空白纸面裁片用于背包面板；浅暖凹槽→参考格内裁片按每格局部坐标使用；细金色高亮→重绘贴合格槽的 2 纹理像素边线。布局以游戏消费端为准，未照搬参考比例。
- v2 验证：按 `Gui.renderHotbar` 实际 (-1,-1) 逻辑偏移验证九格高亮范围，2 倍图集实际 alpha bounds (6,6,42,42) 平移后与对应槽边 (4,4,40,40) 对齐；所有内容中心透明，底边连续无短线。保留原版保护区、alpha、格式和 ZIP 检查。静态检查不等于游戏实测或用户审美认可。
- 2026-09-08：用户确认覆盖生存背包与底部快捷栏；先通过当前 API 的 bundled imagegen CLI 请求 `gpt-image-2` 生成参考，再按原版图集制作正式像素资源。提示词方向：正视 MC 像素 GUI、奶油纸面、蜂蜜橡木边、暖棕凹槽、柔和金色选中框，无物品、文字或场景。
- 文件：`D:/Minecraft/Mod Develop/模型/cozy-inventory-v1/`；参考 `reference/concept.png`，制作入口 `scripts/build_pack.py`，导出 `export/Cozy-Inventory-1.20.1-v1.zip`，预览 `preview/overview.png`。独立资源包，未接入 ICEcore 或自动安装。
- 依据与范围：本机 1.20.1 `client.jar` 的资源格式 15 及真实 inventory/widgets 图集；Forge 47.4.10 `InventoryScreen.renderBg` 使用 UV 0,0，`AbstractContainerScreen` 默认尺寸 176×166。widgets 沿用已核验的快捷栏 182×22、选中框 24×22、副手 29×24。仅改变生存背包基础面板与快捷栏相关区域，创造物品栏、箱子及配方书未换肤。
- 成果与性能：两张 512×512 RGBA 图集，采用原版 2 倍像素尺寸；基础纹理内存约 2 MiB，不增加 tick、网络、动画或绘制调用。保留其它图集区域原版像素的最近邻等效内容。
- 验证：脚本验证原 JAR 资源包格式、物品条目、图集保护区域逐像素一致、背包 alpha 完全一致、选中框中心透明、ZIP 条目/完整性和 PNG 解码；已检查带原版物品叠加的静态预览。未启动游戏，角色、界面文字与配方书由游戏实时绘制，运行时显示待实测。
- 使用：将 ZIP 放入目标 Java 1.20.1 实例 `resourcepacks`，在资源包设置中启用并放在其它 GUI 包上方；相同 inventory.png/widgets.png 为整图覆盖，不能与其它包按图集局部自动合并。可直接停用恢复原显示。

### 贴图与模型制作流程

- 2026-09-08：建立 [贴图与模型制作流程](texture-and-model-workflow.md)，从工作区 `AGENTS.md` 和 `docs/INDEX.md` 路由。后续新 MC 贴图及模型任务可先生成参考图，再做像素规格、语义面与 UV、正式资源制作、多视角重导入验收及按需接入；不强制对已有明确参考或精确局部修正重复生成图。
- 全局调用入口：`C:/Users/yinfire/.codex/skills/gpt-image-2-api/SKILL.md`，由全局 `AGENTS.md` 引用；复用 bundled imagegen CLI、当前 provider 和本地 API 密钥，文档不保存密钥。
- 已验证参考产物：`D:/Minecraft/Mod Develop/模型/diamond-sword-sketch/diamond-sword-sketch.png`，2026-09-08 当前网关接受 `gpt-image-2` Images API 请求并返回素描图，约 31 秒；请求 1024×1024，实际 PNG 为 1122×1402 RGB，后续必须独立核对返回尺寸；属于参考图，未接入游戏。
- 本次范围与验证：仅固化调用记忆及制作规范；检查技能格式、文档路由及 Git 忽略边界，不新增运行时代码、纹理或网络开销，不触发 Java 构建或游戏启动。

### 空碗与脏碗独立模型

- 2026-09-08 用户指定后续模型工作目录为 `D:/Minecraft/Mod Develop/模型/`，范围见 AGENTS.md“模型与 UV 制作验收”；以下桌面与 tool 路径保留为历史产物位置，不作为后续输出默认值。

- 最新 UV 修正：内外分为两张 32×16 纹理；bowl_exterior.png 供所有 25 个保护面共享，bowl_interior.png / dirty_bowl_interior.png 各含 5 个互斥内部 UV 岛。旧单图 PNG 不再被模型引用。生成/修正入口 tool/repair_bowl_uv_mcp.py、tool/finalize_bowl_uv.py；验收 tool/verify_bowl_uv.py，21 个脏污差异像素均在内面，外纹理零差异。最终每模型 5 元素、2 内嵌纹理；32×16×2 RGBA 约 4 KiB，无 tick/网络变化。已通过 Blockbench MCP 重导入并核验俯视、底面截图。

- 2026-09-08：通过 Blockbench MCP 1.6.1 的 create_project/place_cube/paint_with_brush/risky_eval/export_model 制作，保留用户 mushroom_stew.json 的五个碗体几何与原侧壁 UV；从本机 displaydelight-1.7.0.jar 获取 salad_bowl.png。补齐底部上表面，UV 为 [0,0,6,6]，使用原黄褐色低对比木纹与接触暗边；脏污直接绘于内底与内壁，无额外汤面。
- 交付：C:/Users/yinfire/Desktop/BowlModels/，各含 bowl/dirty_bowl 的 bbmodel（内嵌 PNG）、Java JSON、PNG 与 Blockbench 截图。推荐用 bbmodel 编辑；JSON 的纹理引用需随游戏资源路径配置。
- 验证：实际 MCP 握手、工具列表、建模、绘制、导出与重新导入；每份 5 元素、1 内嵌纹理、逐面 UV 有效；人工检查截图。独立美术资产，未接入模组，无客户端运行或 Java 构建。静态 16×16 纹理和固定 5 元素，无 tick/网络工作。

### 独立暖木快捷栏资源包（本地美术试作）

- 2026-09-08：用户要求制作温馨暖色 MC 快捷栏，并明确禁用 imagegen；使用 Python/Pillow 逐像素绘制，独立于 ICEcore 模组，不写入模组资源树。
- 生成入口：`tool/create_cozy_hotbar.py`。交付目录：`C:/Users/yinfire/.codex/visualizations/2026/09/08/01a07e70-b951-7232-9be5-0e21e3a71fc8/cozy-hotbar/`；包含 `Cozy-Timber-Hotbar-1.20.1.zip`、`preview.png`、`widgets.png`、`hotbar-transparent.png`、`pack.png` 和使用说明。
- 来源与链路：本机 Gradle 缓存 Minecraft 1.20.1 `client.jar` 的 `version.json` 确认资源包格式 15；`widgets.png` 由 256×256 最近邻扩为 512×512，仅修改快捷栏、选中框与左右副手区域，其余像素和原版控件保持等效。Forge 47.4.10 `Gui` 字节码确认逻辑 UV：快捷栏 `(0,0,182,22)`、选中框 `(0,22,24,22)`、左右副手 `(24,22,29,24)` / `(53,22,29,24)`。
- 行为：蜂蜜木框、奶油选中边框和半透明暖棕格底；生命、饥饿、经验与物品材质不改。ZIP 放入目标实例 `resourcepacks` 并启用；与其它替换 `widgets.png` 的包存在整图优先级冲突。预览为静态合成，不是游戏截图。
- 验证：生成脚本验证资源存在、格式 15、ZIP 完整性和条目、RGBA 尺寸、选中框 24×22 逻辑尺寸与中间透明、非目标区域逐像素一致；完成放大预览检查。未启动客户端；独立美术资源包不触发模组 Java 构建或版本日志/Wiki 更新。
- 性能：512×512 静态图集，RGBA 未压缩约 1 MiB；不引入 tick、事件、网络、动画、额外绘制调用或运行时脚本。辅助脚本仍在本地 Git 忽略范围，实现文档自 2026-10-10 起允许 Git 跟踪。

ICEcore 当前提供 Forge 加载骨架、固定 NPC、全服共享虚拟货币、CozyCafe 菜单编辑、电脑识别范围、顾客进出区域与酒馆展板扩展、冒险模式建造系统、冒险模式禁丢弃与玩家给予，以及主世界时间、日期 HUD 和床投票跳时系统。固定 NPC 见 [固定 NPC 系统](npc-system.md)，建造系统见 [冒险模式建造系统](adventure-building-system.md)，时间功能见 [时间系统与时间跳过](time-system.md)。

### 玩家反馈显示约定

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 临时玩家提示的统一显示通道 |
| 玩家行为 | ICEcore 需要向玩家即时反馈的成功、失败、状态和调整操作信息显示在原版屏幕底部，不写入聊天栏；1.20.1 原版通道持续 60 tick，前 40 tick 保持不透明，后 20 tick 淡出。 |
| 代码入口 | `feedback.PlayerFeedback` |
| 实现链路 | 服务端调用 `PlayerFeedback.show`、`showFailure` 或 `showSuccess` → 玩家来源调用 `ServerPlayer.displayClientMessage(message, true)` → Forge/Minecraft 发送 `ClientboundSystemChatPacket(overlay=true)` → 客户端 `ChatListener` 调用 `Gui.setOverlayMessage`；控制台等非玩家来源保留 `CommandSourceStack` 的原生命令输出。 |
| 适用范围 | 所有 ICEcore 临时玩家反馈，包括建造系统、调整模式、CozyCafe 范围调整和货币查询/HUD 命令；管理员广播仍保留，但在线管理员反馈同样使用底部信息。聊天栏仅用于明确属于聊天或服务器管理日志的内容。 |
| 页面反馈例外 | CozyCafe电脑页面操作及开店失败必须使用电脑自身提示包和setErrorMessage区域；不得使用PlayerFeedback、原版小标题、底部信息栏或聊天栏。代码5为无有效菜单座位，代码6为营业时间限制，显示6秒、最后1秒淡出；使用原生ClientTooltipPositioner按Open按钮实际坐标居中提示框，render HEAD以System.nanoTime保证容器tick暂停时到期清理。原生原因码0..4与扩展5/6共用行为，布局仅消息/宽度改变时缓存重算。 |
| 性能与生命周期 | 只在实际事件或命令反馈时发送一个原版消息包，不增加 tick 监听、渲染遍历、定时器、客户端缓存或同步 I/O；显示和淡出生命周期完全由原版 HUD 管理。 |
| 验证方法 | 用 Forge 47.4.10 的 `javap` 核对 `ServerPlayer.displayClientMessage(Component, boolean)`、`ClientboundSystemChatPacket` 和 `Gui.setOverlayMessage`；执行 `gradlew.bat compileJava`、资源处理和 `clean build`；扫描玩家反馈路径不存在直接 `sendSystemMessage`、`sendFailure` 或 `sendSuccess`。 |
| 最后修改 | 2026-08-25，新增统一底部信息入口并将 ICEcore 现有玩家提示迁移到该入口。 |

### 外部 KubeJS 脚本迁移

| 字段 | 内容 |
| --- | --- |
| 功能领域 | KubeJS `handid` 服务端指令迁移 |
| 玩家行为 | 在“咖啡馆”实例中执行 `/handid` 时，显示主手物品注册名，并可点击复制；非玩家命令源收到英文提示。 |
| 文件位置 | `D:/Minecraft/PCL启动器/.minecraft/versions/咖啡馆/kubejs/server_scripts/handid.js` |
| 实现链路 | 目标实例 KubeJS `2001.6.5-build.26`（MC 1.20.1 Forge）通过 `ServerEvents.commandRegistry` 注册 `Commands.literal('handid')` → 从 `ctx.source.player` 获取玩家 → 使用 `mainHandItem.id` 读取物品注册名 → 构造 `Text.of(...).green().clickCopy(...).hover(...)` 组件并直接传给 `source.sendSuccess(message, false)` 返回消息。 |
| 版本差异 | “More Mods Bingo” 使用 KubeJS `2101.7.2-build.368`（MC 1.21 NeoForge），其 `ServerEvents.basicCommand` 事件不能直接用于“咖啡馆”的 KubeJS 6/Forge。 |
| 验证方法 | 从目标 KubeJS JAR、目标实例日志和 KubeJS `2001` 源码核对事件与方法；使用 `node --check` 检查脚本语法；重启目标实例或执行 `/reload` 后确认 `logs/kubejs/server.log` 无脚本错误，并执行 `/handid` 验证返回和复制行为。 |
| 最后修改 | 2026-08-21，将 KubeJS 7/NeoForge 的 `basicCommand` 写法改为 KubeJS 6/Forge 的 `commandRegistry` 写法，并修正 `sendSuccess` 直接接收组件的调用方式。 |

### GitHub Wiki 文档与独立推送

| 字段 | 内容 |
| --- | --- |
| 功能领域 | Wiki 页面维护与发布 |
| 文件位置 | 项目根目录 `wiki/`；独立仓库远程为 `git@github.com:yinfires/ICEcore.wiki.git` |
| 页面入口 | `wiki/Home.md`、`wiki/内容.md`、`wiki/冒险模式建造系统.md` |
| 维护方式 | 在项目根目录执行 `git -C wiki status`、`git -C wiki add`、`git -C wiki commit` 和 `git -C wiki push`；主仓库 `.gitignore` 忽略整个 `wiki/`。 |
| 内容规则 | `内容.md` 写 ICEcore 已实现的内容；详细页面按“正常游玩”“管理与配置”等场景写操作、命令、权限和配置，不写实现链路、渲染原理或过程性版本日志。 |

## 实现条目

### 农夫乐事砧板单件输入与成品留存

- 2026-10-17水平朝向再纠正：用户实测左右反向，保留正面朝上的XN平放，仅将水平改为YP。真实砧板原生renderer使用YP与负toYRot，炒锅朝向不等于砧板纹理朝向。四向测试同时检查正面法线和左右基向量；砧板仍批量取出，正常背包插入不排除选中快捷栏格。
- 2026-10-17方向纠正：用户实测输入物品反面朝上；SRG映射与真实炒锅render确认平放是Axis.XN而非XP，朝向是Axis.YN而非YP。砧板原料/成品共用展示改为YN→XN，保留位置、缩放、模型替换。CuttingBoardContractTest读取真实炒锅轴字段，并用四向四元数检查物品正面法线朝上；不影响汤锅或其它工作方块。
- 验证完成：方向修正后4项CuttingBoardContractTest通过，Forge47.4.10完整build及生产映射校验通过；本轮完整测试203项无失败。此前独立clean后的47.4.10完整构建与官方当前最新47.4.28完整兼容构建通过，最终恢复47.4.10。方向修正为客户端变换，未重新扩大兼容构建；JAR未部署、客户端未启动，实机方向仍需用户复测。
- Farmers Delight 1.20.1-1.3.4：`WorkstationCuttingBoardEntityMixin` 保留原生加工与发射器调用，重定向唯一 `rollResults` 后保存全部结果并返回空列表，原生落地循环不再执行；耐久、时运、粒子、声音、统计、进度和单件原料消耗仍走原生。成品列表非空时拒绝再加工与新增投入。
- `CuttingBoardInputHandler` 为原生单槽能力限制新增一件，已有内容时禁止追加；旧存档超量完整加载，每次加工一件。加载禁止内容变化回包，辅助类型在Mixin包外。`CuttingBoardInteraction` 使用共用STORED_AS_ITEM解析、容器返还、NBT精确取回和后进先出；空手批量取普通内容，容器化原料/成品需匹配空容器逐件领取。仅缺容器时调用PlayerFeedback；原生工具/配方/数量/清空文本通过真实调用点静默。
- 背包转移使用Forge `PlayerMainInvWrapper` + `ItemHandlerHelper.insertItemStacked`，以返回剩余数扣减内容；不使用会在创造模式直接清空剩余栈的 `Inventory.add` 计算接收量。容器交换先模拟容量，无位置时静默保留全部；取放保留声音。仅主手认领玩家右键，副手不能重复提交。
- 原料与成品使用独立WorkstationSlotState，分别保存ICEcoreCuttingInput和ICEcoreSlots，成品列表为ICEcoreCuttingProducts。上游save/load、getUpdateTag与onDataPacket链路同步全部数据；空成品条目保留原位置直到整批取完。拆除使用原生原料掉落加一次成品掉落，clearContent同步清理成品与容器数据。
- `CuttingBoardItems` 复用炒锅中心(0.5,0.1,0.5)、朝向、90°平放、0.5缩放、按固定索引的种子旋转和0.025层偏移；砧板实体顶面1/16低于展示面0.1。每个结果项绘制一个模型，Jade标准ItemStorage展示完整数量；厨房模型替换通过独立可选桥接调用ItemRenderReplacer及pot模型列表，厨房缺失时回退原版模型；原生插板工具展示保留。
- Jade仅按请求投影砧板当前原料或成品，沿用唯一标准容器内容提供器；没有流体槽、额外文字层、世界tick或轮询网络。加工匹配沿用原生缓存；容器解析在内容变化/交互发生时执行，背包扫描固定36格，渲染按结果项数量遍历、不按物品数量重复绘制，种子源复用避免逐帧创建随机对象。
- 来源：客户端实际FarmersDelight-1.20.1-1.3.4.jar复制到libs作为验证参考，不加入运行依赖；真实厨房1.4.1/1.5.1及既有汤锅实现。`CuttingBoardContractTest`与`WorkstationMixinContractTest`覆盖真实JAR选择器/Shadow/调用目标、原生加工副作用、容器与部分转移边界、静默提示、可选模型和实体顶面。验证结果见本条后续记录；不启动客户端，四向/多产物展示、容器领取、背包满和多人同步待实机验收。

### 一体式双层冰箱

- `icecore:fridge`，中文“冰箱”、英文“Fridge”；仅加入 ICEcore 创造物品栏，不提供合成配方。Cooking for Blockheads 16.0.15 与 Balm 7.3.44 为双端强制前置，Gradle flatDir 反混淆依赖来自 `libs/` 对应真实 JAR，不将上游类或贴图打包进 ICEcore。
- `fridge.ModFridge` 注册方块与物品，在 common setup 工作队列中通过包外 `FridgeBlockTypeAccess` 扩展原生 `ModBlockEntities.fridge` 的 validBlocks。`FridgeBlockEntityTypeMixin` 使用映射核验的生产字段 `f_58915_`，保留其它允许方块。库存、网络、Balm能力、54格菜单、FridgeScreen、FridgeRenderer、DoorAnimator、声音、厨房供料和升级均保留原生完整入口。
- `IntegratedFridgeBlock` 继承原生 FridgeBlock，上下固定 LARGE_LOWER/LARGE_UPPER；`FridgePairingMixin` 在邻居和基座查找入口限定同种方块、相反半边和一致朝向/铰链，原生冰箱排除新冰箱。无水平搜索或跨台库存合并。菜单默认名称使用本地翻译键；原生命名、染色、常开、制冰与保鲜接口保留。
- `IntegratedFridgeItem` 沿用 BlockItem 放置流程，预检上下空间、碰撞、世界高度/边界与交互权限；两次写入由 ForgeHooks 的 MultiPlace 快照捕获，取消时统一回滚，不消耗物品。第二次写入失败时恢复第一格快照。快照恢复期间不触发清理或掉落；BlockStateTag 不能把已放下半变成单层。支持原生悬空放置，禁止活塞移动。
- 任意半边替换时只掉落该半的27格库存和实际持有的升级，再无方块掉落地清理正确配对半边；另一半仅结算自己的内容。保鲜和制冰以各实体原始标志检查，避免上半getter指向基座而返还两次。方块本体由发起拆除的原版战利品入口产生一次；创造模式不额外掉落本体，爆炸沿用survives_explosion，内容/升级受doTileDrops约束。事件驱动的一次延后tick清理命令生成的缺损结构，不持续轮询。
- 扳手接入 `BuildingStructure` 的显式大冰箱半边解析，点击上下均归一到下半；上下状态参与空间预览与区域限制。`ContainerContentAccess` 提供每半自身库存及升级指纹，现有结构循环检查上下各27格；任意一边非空或带升级都要求两次Shift确认，任意库存/升级变化使确认失效。`BuildingServerActions` 在移除任何结构块前先移除所有对应实体；强拆清空两半内容与升级且只返还一个新冰箱，不产生上半库存的世界掉落。原有区域/列表规则继续决定是否允许拆放，不自动添加管理员配置。
- 世界模型直接引用原生 fridge_large_lower/upper，方向变换与原生一致；`FridgeClient` 使用原生同款颜色处理和cutoutMipped层，复用原生方块实体渲染器。物品模型由原生上下柜体与闭合上下门25个元素组合，上半仅平移16；原生实体几何实际Y=0..29，保留纹理绑定和UV，删除物品面tintindex/cullface并按整体中心缩放。资产生成/核对脚本、独立导出和正面/侧面/背面/斜视合成位于 `D:/Minecraft/Mod Develop/模型/一体式冰箱/`；未生成新贴图。可见特征映射为原生白色柜体→原柜体四壁，贯通门把手→原生上下门把手，后部黑色孔→原生背面贴图，29/16高度→完整双层物品轮廓。
- `FridgeContractTest` 覆盖双格放置检查、生产字段/描述符、包外桥接、无配方、两半资源和扳手事务；`FridgeUpstreamContractTest` 通过ASM核对真实生产JAR注入方法、原生库存/渲染入口，并逐元素比较模型坐标、UV与纹理引用。已有建造回归同批执行。构建与实机验证状态见本条末尾追加记录；不把源码契约或合成图当作实机功能验收。
- 2026-10-17验证：定向冰箱/建造回归通过；独立clean成功后Forge47.4.10完整build通过，Forge47.4.28（实际Maven最新47.x）带有限联网超时offline完整build通过，最终恢复47.4.10完整build与verifyProductionJar通过。183项测试零失败，生产包包含新方块/物品模型/战利品和包外桥接，不含net/blay09上游类、测试配方或旧Accessor。最终 `build/libs/icecore-0.1.1.jar` SHA-256为 `bdcf4395075916a7f9814bb1ee57f5f276fd5dfcffd96a190f013b292583dbad`。UTF-8、双语键集合、版本一致性、现存本地Markdown规则引用与Git忽略边界通过；CodeGraph已收录新增符号。实机未启动/未部署，仍需验证两半交互、厨房/升级/自动化、多玩家、重载、两半库存/确认变化/区域边界下扳手拆放、爆炸/取消放置与最终物品和门动画画面。

### 工作方块情境按键提示（2026-10-18）

- 方块规则部署于指定客户端`kubejs/startup_scripts/icecore_workstation_prompts.js`与`client_scripts/icecore_workstation_prompts.js`。启动脚本经Platform客户端/依赖检查注册Forge END，客户端绑定SKP$PromptUtils按条件每tick提交；IIFE/var避免目标Rhino共享作用域冲突。完整重启加载启动注册。
- 精确ID：kaleidoscope_tavern:barrel仅layer=ceiling且index非4外围八格显示右键开关盖；kaleidoscope_world_liquor:freezer、kaleidoscope_fragrantorchard:juicer、youkaisfeasts:fermentation_tank、kaleidoscope_cookery:stockpot显示潜行+使用开关盖；icecore:mixing_bowl显示潜行+使用搅拌；kaleidoscope_cookery:trash_can显示攻击投入/使用取出；kaleidoscope_cookery:steamer显示潜行+使用开关盖，teapot显示使用拾取。除酒桶几何范围外对准即显示，不以持物/加工/遮挡过滤。
- 从Client.options.keyShift/keyUse/keyAttack读取真实绑定；改键重建对象不修改HUD集合引用，退出世界清缓存，界面/非方块目标停止提交。客户端assets/icecore/lang双语icecore.workstation_prompt.{lid,put,take,stir,pickup}，不覆盖已有NPC翻译。
- SmartKeyPromptsComboMixin仅client列表、资源存在门禁；实际1.1.3-hotfix HUD.renderKeyPrompts逐行建立上下文，5处Font.m_92895_宽度调用按新增键帽宽度修正，3处drawKeyBoardKey继续原生绘制每个键帽，drawText绘制独立+。保留原生缩放、位置、描述和黑名单；按用户2026-10-18追加要求，所有SmartKeyPrompts单键与组合键原生绘制均固定pressed=false，按下后键帽保持未按下外观，游戏实际按键处理不变；updateCachedPrompts只交换倒置的垃圾桶投入/取出顺序，解决原生HashSet消费无序。单键保持原生入口。
- ComboKeyLayout测量键帽（每键文本宽+12）、间隔（+文本宽+4）、整体宽度。脚本直接读取BlockState与KubeJS Block.id，缓存酒桶layer/index属性定义，不调用会创建Map并遍历属性的BlockContainerJS.getProperties；每行查询O(1)，布局字符串最多缓存128项，字体对象变化/原生clearCache失效，按键变化使用新key；稳定渲染无字符串拆分或布局分配。顺序检查仅在原生提示列表变化时O(当前提示数)，交换时一次有界复制。无网络、反射、IO、注册表遍历或持续日志。
- 验证：ComboKeyLayoutTest覆盖双键帽、长绑定、多键、单键/字面+、字体和左右/居中宽度；SmartKeyPromptsComboContractTest读取真实JAR方法描述符及1/5/3注入位置、原生绘制与可选客户端门禁。tmp/WorkstationRhinoCheck.java用目标Rhino2001.2.3-build.10（Gson/commons-lang3完整运行依赖）实际解释验证九类方块、酒桶九索引/三层、START/END、改键缓存、界面/断线、重复加载、依赖缺失及专用服务端不注册。模拟绑定解释通过不代表真实游戏HUD验收。

- 最终验证：新增5项定向测试及目标Rhino解释通过；独立clean后Forge47.4.10完整build、47.4.28完整build（dependencyInsight核对实际1.20.1-47.4.28_mapped_official_1.20.1）、恢复47.4.10最终build均通过，全部221项无失败，verifyProductionJar通过。生产包核对SRG宽度调用、1/5/3精确注入计数及原生KeyRenderer入口；CodeGraph同步、双语、Git忽略与旧版本日志无误改检查通过。确认无Minecraft进程后部署客户端mods/icecore-0.1.1.jar，SHA256=73FF7DEAB3317D462F82F141AB8E0075CBAA313D89FFBA53A3E95156B45F8559，原包备份tmp/instance-backups/icecore-before-workstation-prompts-20261018.jar。未自动启动游戏，真实HUD和脚本绑定/黑名单/改键/缩放/三种对齐仍待完整重启实机验收。

- 未按下外观追加验证：6项定向通过；独立clean后Forge47.4.10完整build与verifyProductionJar通过，全部223项无失败。此次仅原生键帽调用boolean固定false，无新Forge API或兼容范围变更；单键与组合键两处编译字节码均由测试锁定ICONST_0，无新增高频开销。客户端在构建期间自行退出，部署前重新确认无游戏进程后替换mods/icecore-0.1.1.jar，SHA256=3B41CAC07C54666A5373886EBAD993DFE9254E06A86CBF6FB7FBA62C405055A1，原包备份tmp/instance-backups/icecore-before-unpressed-prompts-20261018.jar。未自动启动游戏，实际按压外观待下次完整启动复验。

- 2026-10-18蒸笼操作更新：客户端steamer规则改为lid_combo，提示潜行+右键开关盖。WorkstationSteamerBlockMixin在原生use HEAD认领持SteamerItem点击，禁止跳转SteamerItem.place；getStateForPlacement HEAD同时拒绝目标位置或下方为同类蒸笼，覆盖潜行绕过Block.use、半层补全及副手物品回退。普通非蒸笼承托上的首个半层仍走原生放置。WorkstationSteamerMixin删除空内容giveOne/减层/移除分支，保留4/8槽食物接收、满背包留槽、原生开盖和顶层遮挡限制；已有堆叠不迁移。两处放置检查O(1)只在点击时执行，无新tick/网络/缓存；取食物原有有界8槽×36背包检查保持。TeaSteamerPickupContractTest覆盖两版JAR真实use/placement/food入口及禁止拆层；目标Rhino用更新提示规则复测通过。此前2026-10-17允许右键堆叠/取下的记录为历史行为，由本条取代。

- 蒸笼限制最终验证：目标Rhino解释及定向测试通过，独立clean后Forge47.4.10完整build、verifyProductionJar通过，全部224项无失败；两版厨房真实JAR入口契约通过，生产包包含use HEAD及m_5573_放置拦截。客户端退出后再次确认无游戏进程并部署mods/icecore-0.1.1.jar，SHA256=4235CAF0B1F9DC266ADF68F201FE92137BB3D777635F01EE2503CC3DD17A62C4；旧包备份tmp/instance-backups/icecore-before-steamer-no-stack-20261018.jar。Wiki内容页单独修改未提交/推送，主仓库忽略Wiki；代码/文档UTF-8、Git差异和CodeGraph同步已核对。未自动启动游戏，半层/整层、持物/副手/潜行放置、原生开关盖、食物取放及旧堆叠仍待实机复验。

### 工作方块统一交互与 Jade（2026-10-16）

- 2026-10-18垃圾桶兼容：`kaleidoscope_cookery:trash_can`主手持物左键整组投入、右键按原生倒序取回一个非空槽位整组；生存/创造主手空手左键放行原生挖掘，右键持普通物品不回退投入，潜行同样适用。扳手、区域调整棒优先；冒险交互黑白名单与Forge保护事件仍校验。
- 入口为`TrashCanClientEvents`、`ServerBoundTrashPutPacket`、`TrashCanLeftClickEvents`、`TrashCanInteraction`与`WorkstationTrashCanMixin`；左键用独立请求避免冒险模式不发送挖掘包，客户端和服务端单调时钟限制每250ms一次，服务端校验存活、观察者、加载、36平方格距离、mayInteract、持物、目标与LeftClickBlock保护事件，再整组投入。保护事件不进入真实挖掘路径；协议升至12，包编号24。
- 保留两版厨房原生`putItem`三槽合并、移动/淘汰、投入扣减和`withdrawItem`粒子、动画、refresh链路。取回先接收背包再提交剩余槽栈，背包满静默保留；只处理索引最高的非空槽，不越过容器化内容。共用容器解析以STORED_AS_ITEM处理碗/瓶/桶，精确空容器逐件等量交换。普通物品用WorkstationInventoryTransfer.insert，容器批量交换在36格副本模拟后提交，允许耗尽手持容器释放选中槽；容量或容器不足只处理能接收部分，创造不扣手持。声音替换原生单次播放入口，普通物品使用共用物品声，瓶/桶流体使用共用倒入/盛取声，不叠加播放。
- 槽位精确容器与观察栈复用WorkstationSlotState/ICEcoreSlots持久化；移动/淘汰在原生refresh前同步元数据，合并与部分领取保留精确容器，旧数据首次交互有界恢复。实体吸收/红石/Jade及原生展示入口不替换。计算仅点击触发，库存3格、背包36格，容器容量二分搜索至多log2(手持数量)次36格副本，缓存玩家弱引用无tick轮询或持续日志。
- 验证入口：TrashCanInteractionTest覆盖容量、满背包、释放手持槽、按键与权限、接收后减槽和容器元数据契约；TrashCanUpstreamContractTest直接检查厨房1.4.1/1.5.1整组与效果调用，WorkstationMixinContractTest核对整批生产选择器与Shadow。客户端实机仍需覆盖生存/创造空手挖掘、冒险左键投入、左右键/副手、部分容器、旧存档、多人和原生动画；未授权自动启动客户端。
- 本轮定向10项与完整211项测试无失败；独立clean后的Forge47.4.10完整build、verifyProductionJar通过，47.4.28完整build通过且dependencyInsight确认`1.20.1-47.4.28_mapped_official_1.20.1`。最终恢复47.4.10，未部署或启动客户端，生产包只含ICEcore桥接而不含上游或测试类；CodeGraph、UTF-8、主仓库与独立Wiki边界已核对。
- 2026-10-18左键实测反馈后修正：客户端新增`WorkstationTrashAttackMixin`在Minecraft.startAttack/continueAttack生产入口HEAD统一认领持物垃圾桶交互，继续向Forge原生onClickInput咨询取消，但不进入被取消事件仍播放敲击粒子的分支；开始/持续路径都不执行挖掘。目标识别直接用真实注册ID，不再将WorkstationAccess存在作为客户端识别前置。整组投入主手变空后，本次按压持续认领直到松开，避免下一帧空手挖掘；松开、换世界、打开界面清理认领，重新空手左键放行原生生存/创造挖掘。服务器仍验证具体实体和WorkstationAccess后才投入。新增真实Minecraft字节码/SRG与按压生命周期契约；构建不能替代整合包实机验证。
- 同日再次定位确定根因：Dist.CLIENT注册的TrashCanClientEvents.preventMining收到集成服务端PutPermission并取消，ServerBoundTrashPutPacket在post(permission)后直接返回，永不到putItem；已将PutPermission排除放在客户端监听器首判断，并要求客户端世界isClientSide，服务端监听器原有排除保留。新增直接执行两个真实监听器的无世界PutPermission早退回归，验证不访问世界、不抛异常、不取消；非ICEcore保护事件继续可以拒绝投入。
- 确定根因修正验证：11项定向测试通过，Forge47.4.10完整build与verifyProductionJar通过，最终全部214项无失败；生产包SHA256为D85A3D759E61517CCFD692BA4DC6C97603BBFEF1BA63E38739CF5C279AED0DC9。未热替换当前客户端、未自行启动实机，实机复验仍待用户执行。
- 2026-10-18取放动画同步修正：上游putItem/withdrawItem仅启动所在侧AnimationState，saveAdditional只有Storage，BaseBlockEntity更新包不包含动画；服务端权威交互取消客户端原生use后，客户端PUT/WITHDRAW状态不启动。新增WorkstationTrashAnimationMixin在两方法成功refresh前记录序号、起始gameTime与类型，随原生saveAdditional/getUpdateTag/数据包发送；客户端load按新序号和未过期时间启动原生putState/withdrawState，重播前停止另一取放状态，重复/乱序包不重播，旧存档与过期块载入不播放。Forge IForgeBlockEntity.onDataPacket/handleUpdateTag均调用load，原生更新入口完整保留。
- 原生PUT时长0.5833秒、WITHDRAW时长0.5秒，以12/10tick为清理上限；renderer沿用gameTime+partialTick与原生关键帧，无自绘/复制动画。TrashCanAnimationTiming纯逻辑验证延迟、重复、乱序、过期与无序号；单调时钟截止由clientTick和WorkstationTrashAnimationRenderMixin原生render HEAD共同检查，暂停tick后也清理。桥接接口TrashCanAnimationAccess在workstation包，失败交互不会到refresh故不发动画，无新增网络包、轮询库存或日志，每次render仅常量时间截止比较。实机左右键、旁观玩家、重复取放与存档重载仍待用户复验。
- 动画修正定向13项通过，独立clean后Forge47.4.10完整build与verifyProductionJar通过，全部216项无失败；生产包包含两新增Mixin及包外接口，SHA256为ACD639A8250CEE6C0B8820CDEE8FD31087A48D5B08E91AD3C9344C7892B0A7CF。未部署、未启动客户端；原生动画可见表现仍需实机复验。
- 2026-10-18关闭垃圾桶跳入藏身：WorkstationTrashEnterMixin在TrashCanBlock.fallOn的HorizontalDirectionalBlock原生落地调用之后取消玩家分支，正常摔落处理先执行；禁止生成SitEntity、强制骑乘、32格Mob目标清除和enterState启动。非玩家落地、红石实体吸收、左右键取放与PUT/WITHDRAW同步不变；无新增事件扫描、tick、配置或网络包。两版真实JAR核对fallOn完整SRG/描述符与父类调用；TrashCanEntryTest及WorkstationMixinContractTest固定AFTER/player条件和真实注入入口。已在桶内的历史骑乘不主动批量迁移，玩家仍可正常下马。
- 关闭跳入验证：垃圾桶及WorkstationMixinContractTest定向验证通过；独立clean后Forge47.4.10完整build与verifyProductionJar通过，当前工作树全部223项测试无失败。生产包包含WorkstationTrashEnterMixin及配置注册，SHA256为D24894C89E0876852EC6B8FACE89F6185614234222AC4B378F806AC0657B0412；CodeGraph、Git/Wiki空白及忽略边界检查通过。未部署或启动客户端，玩家落地不骑乘及取放动画保留待实机复验。

- 茶/茶杯/蒸笼背包拾取验证完成：2项新契约与现有真实JAR契约通过，Forge47.4.10完整build与verifyProductionJar通过，全部207项测试无失败，CodeGraph及Git/Wiki边界检查通过；未部署、未启动客户端，杯数、满背包、双手回退与半层/整层蒸笼取下仍需实机验收。
- 2026-10-17茶与蒸笼背包取回：WorkstationCupPickupMixin对茶杯/空杯仅主手空手拾取接入giveOne，优先空杯、每次一件，接收后更新杯数与茶数，容量不足不改变方块；倒茶/添杯/同类叠放保留原生入口。WorkstationTeapotIngredientPickupMixin把status=0的茶底取回改为单件背包接收，成功后扣减并refresh。蒸笼takeFood仍取原生4/8槽范围，按实际背包接收量扣减、空槽才清计时；开盖/上方承托遮挡/顶层结构限制保留。空内容时接收一个蒸笼后才减层/移除；主手非空拒绝takeFood，SteamerBlock在投料后takeFood入口同时限制主手空手，避免双手回退取下。合法蒸笼堆叠与开合不变。扫描只在点击时执行，最多8槽×36背包格，无新tick/轮询/提示。TeaSteamerPickupContractTest及真实JAR选择器/调用目标契约覆盖；实机杯数/满背包/双手/层数待用户验收。
- 本轮水平朝向与通用背包转移验证：砧板4项、背包转移2项及工作方块/烤炉/混合碗定向测试通过，Forge47.4.10完整build、生产映射校验通过；全部205项测试无失败，CodeGraph已更新，Git与Wiki差异检查通过。未部署或启动客户端，左右朝向与背包实际槽位/满背包行为待实机确认。
- 2026-10-17通用空手取出保持每次一件，改为正常背包插入，不直接setItemInHand，不排除选中快捷栏格。WorkstationInventoryTransfer使用PlayerMainInvWrapper与insertItemStacked，先模拟容量再接收，成功后才扣减原料/成品；背包满静默保留。冰柜/发酵桶/汤锅/炒锅共用服务及烤炉/混合碗同类入口接入，带容器交换不变；砧板保留批量取出。背包扫描有界36格，无tick/轮询/新增提示；WorkstationInventoryTransferTest检查单件、正常插入、接收后扣减及全部入口取消指定手持替换。
- 目标 ID 来源为 recipe-manager `public/data/recipe-manager.json`。冰柜、发酵桶、汤锅、炒锅接入 `workstation.WorkstationInteractionService`；料理台仅由 `WorkstationCuisineMixin` 在工具处理之后接入单件投入，原生取出保持。容器识别、库存交换与后进先出复用现有服务；槽位元数据保存为 `ICEcoreSlots`，加载不发包，首次操作有界恢复。
- 发酵桶空主手 Shift 开合，普通点击不再开合或倾倒；仅开盖取放，开盖清零进度。2026-10-18：`WorkstationEvents.click` 在服务端主手成功切换盖状态后，于方块位置广播冰柜同款原版 `BARREL_OPEN` / `BARREL_CLOSE`，分类 BLOCKS、音量/音调均为1.0；真实世界名酒1.1.12 `FreezerBlock` 字节码与1.20.1映射已核对，不新增音频资源、tick任务或客户端重复播放，单次切换仅常量时间调用。炒锅取消 `tickPutIngredient` 自动 startCooking，保留 onShovelHit；成品逐件领取，无容器空手可取且不烫伤。烧焦结算保留 1–3 个木炭并停止继续烧焦。汤锅省略 carrier 时从结果推导，显式 carrier/empty_carrier 保留；无需容器时不显示汤底，使用炒锅同款中心、90°平放和0.5缩放，高度按汤锅真实内底调整为3/16格。
- 发酵桶开合音效验证：Forge47.4.10 `compileJava`、完整 `build`（含全量test、processResources、reobfJar及verifyProductionJar）通过；生产包声音字段核对为原版桶声。未部署或启动客户端；空主手Shift右键开/关各一次及旁观玩家听声仍待实机确认。
- 蒸笼 `getCookTick` 统一600 tick，开盖仅存放，关盖开始；同层同批统一进度与时长，完成槽标记-1并停止再次匹配。无热源暂停，开盖清零未完成槽，已完成成品保留。堆叠盖判定缓存按世界tick清空，同一连续堆叠只扫描一次，最多4096坐标，世界卸载可回收；点击失效缓存并清理覆盖层进度。
- 茶壶普通右键放置保留BlockItem流程；茶杯禁止不合法持物点击落入掉落实体分支，合法倒茶、添空杯、同类叠放保留。料理台与发酵桶原专用框通过实际 overlay render 入口按目标取消，其它方块提示框不变。
- `compat.jade.WorkstationJadeProvider` 在 Jade 请求时生成只读快照，不增加世界tick扫描；酒桶所有部件解析原点，显示4000mB和逐槽投入物；茶壶将原单份流体状态映射1000mB槽，完成时替换原料为成品。料理台、冰柜、汤锅、炒锅显示成品。六种工作状态使用剩余tick/20格式化MM:SS，竹匾不增加时间；容器要求等失败提示统一PlayerFeedback，成功取出不再显示底部提示，状态迁至Jade。
- 2026-10-16 容器显示修正：`WorkstationItemStorage` / `WorkstationFluidStorage` 使用 Jade 11.13.3 标准 ViewGroup、ItemView 与 FluidView，`WorkstationJadeStorageMixin` 在按需 putData 入口为目标方块选择唯一存储内容来源；请求内标记避免物品/流体入口重复投影。已有容器读取槽位副本，未标准化的成品字段为只读展示，不注册自动化能力。物品优先级1000，流体1100，酒桶状态1200，工作倒计时10000；旧自绘物品/流体及三项专用翻译键删除。茶壶完成时原料与1000mB展示流体被剩余成品替换；其它真实槽保持。
- `WorkstationCookeryOverlayMixin` 取消茶壶与炒锅实际客户端指向文字；成功取出提示从共享交互删除，原生反馈仅保留容器要求及明确失败，茶壶 take_tea_fluid.blocked 静默禁止。`WorkstationTeapotStartMixin` 在成功投入及条件首次满足时复用原生tick的配方/谜之茶初始化，跳过200tick准备等待与23tick启动轮询；同世界tick不重复扣时，原热源暂停保持。
- 汤锅不可见根因（2026-10-16）：真实厨房1.4.1/1.5.1的stockpot.json实体底板为[2,0,2]→[14,2,14]，原0.1格展示面低于2/16格而被锅底遮挡；客户端实际包确认包含0.1。成品中心升到3/16格（比内底高1/16格），保留炒锅平放、0.5缩放及模型替换。WorkstationMixinContractTest从两版真实JAR几何读取中心底板高度，锁定展示面在实体外。无匹配配方的EMPTY_ID分支优先返回上游DEFAULT_CARRIER（真实Items.BOWL），取出、服务端分类及汤底表现使用同一规则，不影响明确empty_carrier配方。本轮定向测试、独立clean后Forge47.4.10完整build及167项测试通过，生产映射校验通过；未部署或启动客户端。
- 汤锅 `WorkstationStockpotProductSyncMixin` 随上游原有保存/更新标签写入 `ICEcoreProductCarrier`，客户端不依赖尚未就绪的配方判断；旧标签缺失时按配方与结果可靠元数据回退。世界成品采用炒锅 ItemRenderReplacer 的pot模型列表、中心高度及变换，单成品固定第0项，无额外随机转向；需要容器仍显示汤底。新增契约验证标准存储与失败反馈边界，客户端布局和实际结果模型仍需实机确认。
- 来源：libs 厨房1.4.1/1.5.1、youkaisfeasts0.5.0、烟火1.1.1、酒馆1.2.0、世界名酒1.1.12；世界名酒参考JAR从指定客户端复制至libs，不作为构建依赖。可选Mixin以class资源探测，桥接与辅助类型位于Mixin包之外。
- 验证：166 项测试通过；新增 `WorkstationMixinContractTest` 使用真实JAR ASM逐项检查生产选择器和Shadow，现有顺序纯逻辑测试保留。独立执行 clean 后 Forge 47.4.10 完整 build 通过；47.4.28 兼容构建及实际依赖坐标检查通过，恢复47.4.10后最终 build 通过。生产 `build/libs/icecore-0.1.1.jar` 的Mixin、双语键及外部类打包边界检查通过；UTF-8、Git边界与CodeGraph状态检查通过。未部署或启动客户端；实机须覆盖Jade空满槽、汤锅摆放、原生工具及成品领取、旧存档、蒸笼层盖与多人双手交互。

### 时间加速下种植随机刻补偿

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 0.01 倍率下原版作物生长与耕地湿度随机刻按 100 倍时间密度运行，不再因随机刻抽样不足而长期不生长或不干涸。 |
| 代码入口 | `mixin.ServerLevelRandomTickMixin`、`time.RandomTickScaling`。 |
| 实现链路 | `ServerLevel.tickChunk` 尾部按 `ceil((multiplier - 1) * base)` 或 `ceil((1 / multiplier - 1) * base)` 计算额外抽样；补偿同时调用方块和流体 `randomTick`，每区块 tick 最多 10000 次，不增加控制台日志。 |
| 验证方法 | `RandomTickScalingTest` 覆盖 0.01、0.5、1、10 倍率、零输入和上限；Forge 47.4.10 `compileJava` 与窄测试。 |

### 共享九格烤炉与批量处理

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | `icecore:oven` 使用一个共享九格容器，输入和成品共同占用槽位；成品存在时仍可继续投入物品。每格输入和输出均严格按单件保存且不合并，内容稳定 100 tick 且下方方块属于 `farmersdelight:heat_sources` 后按 1–9 项 Ingredient 无序配方烤制，默认 400 tick，可自定义时长；`processing.mode=batch` 时才重复处理完整批次。烤制期间任何取放都会重置。烤炉没有流体槽，牛奶桶等流体容器通过共用交互层作为普通容器化物品保存，并记录空容器元数据；取出时统一按后进先出，不能越过顶端容器化物品。 |
| 代码入口 | `oven.OvenBlock`、`oven.OvenBlockEntity`、`oven.OvenRecipe`、`workstation.BatchProcessingService`、`oven.client.OvenJeiPlugin`、`oven.OvenRenderer`。 |
| JEI | 使用 ICEcore 自有透明画布、JEI 原生标准输入/输出槽位和原生处理箭头，左侧 3×3 九个输入槽、右侧 3×3 九个输出槽，不显示上方槽、流体槽或返还槽；仅批量 X 参考 Fragrant Orchard Juicer：相对输入槽左上角绘制五个 1×1 白色像素并使用相同前景层级，普通配方不绘制；不复用 Juicer 的背景、槽位布局、箭头或制作时间。淡灰色批量提示保留。布局仅用于展示，不代表方块拥有十八格。 |
| KubeJS 配方 | 目标实例 `D:/Minecraft/PCL启动器/.minecraft/versions/异次元餐厅/kubejs/server_scripts/icecore_oven_test.js` 仅用于测试；模组源码、资源和生产 JAR 均不包含该测试配方。公开的烤炉与混合碗 KubeJS 字段、批量模式、流体和容器输出写法见 Wiki《烤炉与混合碗配方》。该外部脚本不作为仓库单元测试输入。 |
| 资源与验证 | 方块模型复用 `extradelight:block/oven`；JEI 背景复制混合碗使用的发酵罐式 UI 并改为烤炉 3×3 输入/输出布局；批量 X 严格采用 Juicer 的五个 1×1 白色像素和 Z=200；不显示制作时间。已执行 `gradlew.bat test processResources --rerun-tasks` 与 `gradlew.bat build`，并检查生产 JAR 无 KubeJS/测试配方条目。客户端热源粒子与模型视觉仍需实机复测。 |

### 九格混合碗与通用容器兼容

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | `icecore:mixing_bowl` 最多保存九格输入或九项成品；手持普通物品右键优先投料，空手右键才取回最后投入的普通食材，空手 Shift+右键每次搅拌 10 tick，累计 50 tick 后才结算配方。无配方时保留内容并复位。成品阶段不再接受食材；配方显式声明 `carrier` 时以其为准，未声明时从成品自己的容器元数据安全推导，能够自动要求碗、玻璃瓶、桶或其它模组明确声明的盘/锅等空容器；无法可靠推导时仍为空手领取。真实单流体槽容量1000 mB，只要非空就占一格输入；匹配的空桶或空瓶仍可直接盛取流体。普通容器化食材投入后返还空容器，槽位保存取回要求；空手面对蜂蜜瓶等容器化食材时显示动态容器提示，手持准确匹配的空容器才执行盛取，其它手持物继续按投料处理。 |
| 配方格式 | `icecore:mixing_bowl` 的 `ingredients` 保存 0–9 项物品，专用可选 `fluid` 保存 `fluid` 注册名、精确 `amount`（1–1000 mB）和 JEI 用 `display` 单件物品；物品数加流体位必须为 1–9。`results` 为 1–9 项，每项固定一件并可带 `carrier` Ingredient；显式 `carrier` 优先，省略时从结果物品自动推导。物品输入无序精确匹配，流体种类与 mB 数也必须精确一致。`ingredients` 禁止再声明可识别的流体容器。客户端 KubeJS 测试配方位于实例 `kubejs/server_scripts/icecore_mixing_bowl_test.js`，物品输入小麦、糖和鸡蛋，流体输入为 1000 mB `minecraft:milk` 且 JEI 显示奶桶，以碗盛取一个 Bakery 蛋糕面团。 |
| 容器兼容 | 共用入口为 `workstation.WorkstationContainerCompat`，完整格式、优先级、交互矩阵和复用要求见 [工作方块容器兼容](workstation-container-compatibility.md)。输入解析为显式数据包→Forge 流体能力→通用空容器解析；空容器解析为 crafting remainder→Forge 流体能力返回空容器→可选模组接口→原版 `BowlFoodItem`→明确碗/瓶/桶标签→药水瓶特例。数据包 `filled` 支持精确 ItemStack、物品 ID 或标签，NBT 精确项优先；已适配森罗物语厨房 `IHasContainer` 与其容器标签，缺少该模组时不加载适配类。不使用注册名启发式。解析结果按物品+NBT使用有界缓存，并在数据包重载时整体失效。 |
| 显示与动画 | `MixingBowlRenderer` 分别渲染 Farm & Charm 1.0.14 `CraftingBowlModel` 的碗体和搅拌杆，始终使用普通碗实体贴图。普通输入和输出固定按槽号 `index×40°` 环形平放于约 `3/16` 高度，沿用发酵桶70°倾角，并按两种容器内腔宽度0.8比例使用0.30缩放与0.10环形偏移；取出后不压缩槽位，因此其它物品不跳位。液体使用 `FluidRenderer.renderFluidBox`，液面从 `3/16` 按实际mB升至满槽约 `7.2/16`。搅拌杆用 partial tick 连续转圈；每次成功搅拌都会播放一个可控的 Farm & Charm 原搅拌声实例，连续追加允许已有实例自然叠音且不会打断，服务端同步离开搅拌阶段时所有尚存实例立即停止。JEI仍使用发酵罐144×54背景及三组槽位，不增加流体槽或自定义说明文字。 |
| 状态与同步 | `MixingBowlBlockEntity` 使用 `INPUT`、`STIRRING`、`OUTPUT` 三阶段，服务端权威保存输入及每槽空容器要求、1000 mB `FluidTank`、输出、结算后的有效 carrier 和搅拌进度。旧存档缺少输入容器元数据或输出 carrier 时按当前共用解析器重新推导。内容变化发送完整更新，NBT加载先清空旧数据。Jade能力只读，漏斗或管道不能修改内容。搅拌期间不逐tick发送完整方块实体包，配方只在达到50 tick时查询一次。 |
| 交互音效 | 普通物品投入/取出分别使用 Fragrant Orchard 榨汁机同款 `ITEM_FRAME_ADD_ITEM`/`ITEM_FRAME_REMOVE_ITEM`（0.8、1.1）；桶装流体投入/取出使用 `BUCKET_EMPTY`/`BUCKET_FILL`，瓶装流体投入/取出使用 `BOTTLE_EMPTY`/`BOTTLE_FILL`（1.0、1.0）。仅成功操作播放。 |
| 代码与资源 | `mixing.ModMixing`、`MixingBowlBlock`、`MixingBowlBlockEntity`、`MixingBowlRecipe`、`workstation.WorkstationContainerCompat`、`MixingBowlRenderer`、`mixing.client.MixingBowlJeiPlugin`；资源位于 `assets/icecore/{blockstates,models,lang}`、`data/icecore/container_compat` 和 loot table。Farm & Charm 1.0.14 与 Youkais' Feasts 0.5.0 是强制前置。 |
| 验证方法 | `MixingBowlImplementationTest` 固化九格/50 tick、世界流体渲染、显式 carrier 优先、缺省自动推导、可选接口加载守卫、标签数据格式、JEI 有效容器显示以及强拆确认关键约束；执行 `gradlew.bat test`、`processResources --rerun-tasks`、Forge 47.4.10 `clean build` 并检查 JAR。实机仍需核对模型层、九项摆放、奶液面、音效，以及原版碗/瓶/桶和第三方盘/锅的完整交互。 |

### 岛屿传送/实质化系统（prototype）

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 把一片自定义区域捕获为“岛”，可真实隐藏（=移除，零光照泄露）与真实显现（=放回）。显现可选带入场镜头过场：黑屏过渡 + 定点相机 + 屏幕特效（震动加剧 → 轻微向心扭曲 → 白屏）；服务端**在客户端全白之后**才跨 tick 分批生成方块（放置全程藏在白屏下），放置完成后白屏 2 秒淡出**揭示成品岛**，观感与放置时长解耦。当前为验证语义与光照正确性的 prototype，不含千格性能优化。已废弃早期“客户端假模型渲染”方案（光照 FULL_BRIGHT 不匹配、双渲染源闪烁两处硬伤）。 |
| 交互与命令 | 权限等级 2。框选复用现有 `building region`（不自造岛专属选择模式）。`/icecore island capture <岛名> <区域名>` 读该 `RegionDefinition` 的 pos1/pos2/维度捕获；`/icecore island recapture <岛名>` 按原绑定区域（`min`+快照尺寸）重扫刷新方块/生物快照，保留名称/坐标/状态/入场镜头，HIDDEN 态或放置中拒绝；`/icecore island camera set <岛名>` 记录执行玩家眼位+朝向为入场镜头，`camera clear <岛名>` 清除；`/icecore island hide <岛名>` 真实清空为空气；`/icecore island show <岛名>` 有入场镜头则广播入场过场包并延迟落地，无则近瞬时落地；`/icecore island remove <岛名>` 取消设置为岛（删注册表+NBT 文件，放置中或 HIDDEN 态拒绝以防方块永久丢失）。同时只允许一个岛操作在跑（`IslandPlacer.idle()` 守卫）。 |
| 数据位置 | 每岛一份 NBT：`data/icecore_islands/<name>.nbt`（`NbtIo` 压缩）。含尺寸、方块 palette+索引、方块实体 NBT、维度、最小角坐标、状态（CAPTURED/HIDDEN/SHOWN）、可选 `entrance` 子标签（入场相机眼位 x/y/z + yaw/pitch，5 基元）。prototype 单文件整读整写，chunk 分片流式存储留到阶段 A。 |
| 代码入口 | `island.IslandData`（快照+序列化）、`IslandCaptureService`（区域→快照，含流体/含水/BE）、`IslandPlacer`（跨 tick 分批放置/清空队列，`FLAGS=UPDATE_CLIENTS\|UPDATE_KNOWN_SHAPE\|UPDATE_SUPPRESS_DROPS`，show/hide 均 flat cursor 底向上波前，各 8192 方块/tick）、`IslandManager`（注册表+持久化+生命周期，`recapture`/`setEntrance`/`clearEntrance`）、`IslandCommands`（`attach` 到 `icecore` 根）、`IslandEvents`（`ServerStarting/Stopping` 挂载、`ServerTickEvent.END` 泵 `IslandPlacer.tick()`）。入场过场：`ClientBoundIslandEntrancePacket`（维度+相机 5 基元+buildup+placeTicks，无方块数据，网络 id=17；`buildup`=收包→全屏白的总时长，`placeTicks` 保留字段客户端暂未用）、`ClientBoundIslandEntranceDonePacket`（维度，放置完成信号，网络 id=18）、客户端 `IslandEntranceClient`（`CutsceneCamera`/`CutsceneFade` 复用 + `island_warp` post 特效 + 完成包触发白幕淡出揭示）。屏幕特效资源见 `assets/icecore/shaders/{post,program}/island_warp.*`（`.fsh` 为全屏震动+收尾向心扭曲+白屏，uniform `Strength`/`White`/`Age`）；uniform 逐帧写入靠 `mixin/PostChainAccessor`（SRG `f_110009_` 取 `passes`）。 |
| 实现链路 | capture/recapture 逐格读 chunk-cached `BlockState` 建 palette+索引、`hasBlockEntity()` 存 `saveWithFullMetadata()` → 存 NBT 文件；recapture 以 `entry.min`+快照尺寸算回原区域重扫写回 `data`。show 有入场镜头时：服务端广播入场包并 `IslandPlacer.enqueueShow(delay=buildup+余量)`——落地延迟推到客户端全白之后，保证方块**只在白屏背后**生成；`onComplete` 里置 SHOWN 并广播完成包。客户端 `IslandEntranceClient.begin` 先 `CutsceneCamera.engage()` 锁输入/手，黑屏淡入满帧时 `takeCamera`+`loadEffect(island_warp)`，露出后 `Strength`（`k²` 加速）驱动全屏震动逐渐加剧，峰值（`buildup-WHITE_TICKS`）起 `White` 收尾向心扭曲 + 白屏铺满至 `whiteFull=buildup`；全白后关 post、GUI 白幕接管保持，`finish()` 置 `placementDone` 且白屏保持满 `MIN_WHITE_HOLD` 后起白幕 2 秒淡出（`WHITE_FALL`）露成品岛、停留 `HOLD` → 尾段黑屏满帧 `dropCamera` → 露玩家视角 `end`；完成包丢失有 `PLACE_TIMEOUT_TICKS` 本地兜底。post 特效加载失败自动降级（仅镜头+黑屏+白屏+揭示）。抑制邻居更新避免水流/掉落/红石连锁，保留 client+light 更新保证光照正确。 |
| 性能与生命周期 | show 与 hide 均跨多 tick 分批、单 tick 各 8192 方块预算上限，避免上千格单 tick 卡服（此前 show 为单 tick 全量落地，已改分批）；capture 用 `getChunkAt` 缓存 chunk 避免逐格 level 哈希。入场过场镜头/输入/HUD/手统一走 `CutsceneCamera` 两相位（engaged/cameraActive），黑屏与缓动走 `CutsceneFade`，覆盖暂停/登出/乱序清理。prototype 仍用逐格 `setBlock`+逐格光照，千格阶段（阶段 B）改底层 section 直写+批量 relight+整块重发，需与 `mixin/Embeddium*` 的 section 管理核对。 |
| 详细说明 | 镜头框架见下方“可复用镜头过场框架”条目（原设计交接文档已删除，事实以本条目为准）。 |
| 验证方法 | Forge 47.4.10 `compileJava`/`compileTestJava` 通过、`test`（含 `TimeCutsceneSafetyTest`）通过。capture/recapture 一致性、hide 期零光照泄露、大岛分批生成无卡顿、入场镜头→黑屏→变形→峰值落地→露岛无缝、Embeddium/Sodium 下 post 特效可用性与降级、着色器 `Strength` 是否真正生效等实机项待用户授权后验证。 |
| 最后修改 | 2026-09-04，阶段 C 重构：废弃假模型渲染，改入场镜头（黑屏过渡+定点相机+`island_warp` 屏幕特效）+ 服务端分批生成；新增 `recapture`、`camera set/clear` 命令与入场相机持久化；show 由单 tick 全量落地改为分批生成。入场特效多次重做后定为**震动加剧 → 轻微向心扭曲 → 白屏**（uniform `Strength`/`White`/`Age`）；关键修订：服务端落地延迟推到**客户端全白之后**（`buildup+余量`），`buildup` 语义改为“收包→全屏白”总时长，保证岛只在白屏背后生成；全白后 GUI 白幕保持等放置完成，再 2 秒淡出揭示成品岛。原设计交接文档 `island-transport-system.md` 已删除（计划类文档不入 docs/）。 |

### 可复用镜头过场框架

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 客户端可复用镜头过场基建，供任意脚本化镜头动画（时间跳过、岛屿入场……）共用。只抽取跨过场一致的视图侧关注点：定点相机接管、朝向锁定、玩家输入抑制与内容遮罩（HUD/Jade/手），以及全屏黑屏/闪白过渡与缓动。时序、内容、网络、是否用黑屏由各消费者自定；黑屏过渡为可选，需要时调用、明确不要时不调用。 |
| 代码入口 | `client.cutscene.CameraPose`（眼位 x/y/z + yaw/pitch record）、`client.cutscene.CutsceneCamera`（相机接管/朝向锁/输入/HUD/手抑制，两相位 engaged/cameraActive）、`client.cutscene.CutsceneFade`（`ease` 平滑缓动 + `envelope` 梯形淡入淡出包络 + `fill` 全屏填色，`BLACK`/`WHITE` 常量）。消费者：`time.TimeCutsceneClient`（时间跳过，黑屏走 `CutsceneFade`）、`island.IslandEntranceClient`（岛屿入场，黑屏+白闪+变形均走框架）。 |
| 实现链路 | `CutsceneCamera` 用不可见 `ArmorStand` 作相机实体，`setCameraEntity`；`ComputeCameraAngles` 锁 yaw/pitch/roll，每 tick 重锁位姿 + `setOldPosAndRot` 防插值；`engaged` 期 `KeyMapping.releaseAll()`+清 input impulse、取消 `MouseButton.Pre`、取消 `RenderHandEvent`，`cameraActive` 期取消 `RenderGuiOverlayEvent.Pre`（连带 Jade）；`logout` 兜底 `end()`。相机眼位经 `cameraBaseY(recordedEyeY, standEyeHeight)` 下沉对齐。`CutsceneFade.envelope` 给出单段梯形覆盖率，消费者叠多段取 max 表达完整黑屏序列。 |
| 性能与生命周期 | 纯客户端主/渲染线程，单过场同一时刻只一个，静态字段即可。`CutsceneFade` 为无状态纯函数；`fill` 对 <~1.5% alpha 帧直接跳过零开销。事件驱动无 tick 轮询。 |
| 详细说明 | 岛屿入场用法见上方“岛屿传送/实质化系统”条目；时间跳过用法见 [时间系统](time-system.md)。 |
| 验证方法 | `compileJava`/`compileTestJava` 通过，`TimeCutsceneSafetyTest` 以源码文本断言镜头/输入/HUD/手/防插值/黑屏缓动等安全不变式指向 `CutsceneCamera`，`test` 通过。 |
| 最后修改 | 2026-09-04，自时间跳过抽取镜头/输入/内容遮罩为 `CutsceneCamera`+`CameraPose`；后续抽取黑屏过渡与缓动为 `CutsceneFade`，时间跳过与岛屿入场共用。 |

### SDM Shop 多商店与命令管理

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 安装 SDM Shop Rework 6.11-pre 时，服务器可维护多个带唯一 ID 和显示名称的商店；管理员通过 `/icecore sdmshop` 管理和打开指定商店，`edit` 打开后可在 SDM 原界面创建分类、增改商品，改动写回对应商店快照。SDM 默认 O 键和背包侧边栏商店按钮被移除。 |
| 代码入口 | `compat.sdmshop.SDMShopManager`、`SDMShopCommands`、`SDMShopCompat`、`compat.sdmshop.client.SDMShopSidebarHider`、`mixin.SDMShopSaveMixin`、`BuildingCommands` |
| 数据位置 | 世界 SavedData `icecore_sdmshops`，保存 ID、UUID、显示名和 SDM NBT 快照。首次加载将 SDM 固定默认 UUID 迁移为 `default`。 |
| 命令 | `/icecore sdmshop list|create <id> [display_name]|rename <id> <display_name>|delete <id>|open <id> [players]|edit <id> [player]|migrate`，权限等级 2；ID 仅允许小写字母、数字、`_`、`-`。 |
| 兼容实现 | 通过反射调用 SDM `ShopBase`、`SDMShopR` 和 `SendOpenShopScreenS2C`，SDM 未加载时所有入口安全跳过；独立快照避免多商店覆盖同一默认文件。**打开商店时把 `ShopBase.shopUUID` 强制设为 SDM 客户端单例固定的 `SHOP_UUID` 并注册进 `MultiShop.SHOP_MAP`/`SHOP_UUIDS`**——否则客户端标签刷新 `SendGetTabsC2S` 用固定 UUID 查 `MultiShop` 查空即返回，界面无标签、编辑无反应。侧边栏按钮改由客户端 `SidebarButtonCreatedEvent` 监听命中 `sdmshoprework:shop` 后 `addVisibilityCondition(()->false)` 隐藏（FTB `SidebarButtonManager` 用 `getResourceStack` 合并所有 pack，空 JSON 覆盖无效，已删除）。编辑改动经 `ShopBase.saveShopToFile`（所有创建/编辑标签与商品包的唯一汇聚点）由 `SDMShopSaveMixin` HEAD 注入回写到 `SDMShopManager` 当前 entry 快照。**打开时不调用 `ShopBase.syncShop`（其 `SendClearTabsS2C` 会清空客户端并异步按 UUID 重取），改为在开屏包前同步发送 `SyncShopS2C(shop.serializeNBT())` 全量灌入 `ShopBase.CLIENT`**——否则界面构造时 `AbstractShopScreen.onConstruct` 读到空 `CLIENT`、`selectedTab` 恒为 null，而 `ModernShopScreen.setSelectedTab` 的 `selectedTab==null` 早退分支使首个标签永远点不中（SDM 自带 `/sdmshop open` 因客户端已由登录同步填充故无此问题）。 |
| 性能与验证 | 仅在命令、世界加载、打开商店和编辑保存时读写/同步，不增加 tick 轮询；侧边栏隐藏仅在 `FMLClientSetupEvent` 注册一次、按钮创建时一次字符串比较。执行 `gradlew.bat compileJava`、`test`、资源处理与 `clean build`，并检查目标 JAR 位于 `libs/` 且未打入成品。回归测试固定校验 `SDMShopSaveMixin` 的 `saveShopToFile()V` 生产选择器、`openShop` 对 `MultiShop`/`SHOP_UUID` 的引用，以及空 `sidebar_buttons.json` 已移除。 |

### 固定玩家模型 NPC 与“火”对话

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | OP 可按数据包定义生成固定 NPC；NPC 可穿过、不可推动或伤害，8 格内平滑注视最近玩家，右键时转向玩家并打开 ChatBox 对话。名称只在本地准星命中时显示。具体 NPC 定义、皮肤和对话不内置在模组，改由客户端 `咖啡馆` 数据包提供（见下「客户端内容位置」）。 |
| 代码入口 | `npc.ModEntities`、`NpcEntity`、`NpcDefinitionManager`、`NpcCommands`、`npc.client.NpcRenderer` |
| 资源位置 | 模组不再内置具体 NPC 资源。`icecore:fire` 及其对话、主题、皮肤和语言键已迁至客户端实例 `D:\Minecraft\PCL启动器\.minecraft\versions\咖啡馆\kubejs\data\icecore\...` 与 `\kubejs\assets\icecore\...`（见下「客户端内容位置」）。模组仅保留 NPC 系统代码与 `entity.icecore.npc`、npc 命令语言键。 |
| 实现链路 | 服务端数据重载解析定义 → OP 命令把定义字段、锚点和 45°吸附朝向写入同步数据/NBT → 原版实体同步驱动客户端玩家模型渲染 → 服务端每 10 tick 在 8 格 AABB 选择最近有效玩家，并在每个 AI tick 续期给原版 `Mob`/`LookControl` → 右键复核后调用 `ChatBoxCommandUtil.serverSkipDialogues`。ChatBox 是双端强制 `compileOnly` 依赖，运行时接受 1.1.4 及更高版本，当前以 1.1.5 编译验证，不被打包。 |
| 性能与生命周期 | 无全世界或渲染期实体扫描；附近玩家查询每 NPC 每 10 tick 一次且限定 8 格；wide/slim 两个玩家模型在渲染器构造时缓存；实体位置恢复和原版 LookControl 更新为 O(1)；缺失定义警告按加载周期去重并限制为 128 个 ID。 |
| 详细说明 | [固定 NPC 系统](npc-system.md) |
| 验证方法 | `NpcRotationTest` 与 `NpcResourcesTest`（后者现只校验模组语言键跨语言一致，fire 专项校验随迁出移除）；Forge 47.4.10 `compileJava`、`test`、资源处理、`clean build` 和成品 JAR 内容/外部依赖排除检查（确认 JAR 内已无 fire.json/fire.png）。客户端行为待明确授权后实机验证。 |
| 最后修改 | 2026-09-03，将内置 `icecore:fire` 及其对话/主题/皮肤/语言键迁出模组至 咖啡馆 客户端数据包，模组只保留通用 NPC 系统。 |

### 任务与教程系统

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 数据包驱动的任务与教程界面、追踪 HUD、进度持久化，以及与固定 NPC、ChatBox 对话和货币的联动 |
| 玩家行为 | 按 J 打开任务界面、按 I 打开教程界面（`KeyMapping`，可在原版按键设置改键）。两界面左侧为可折叠分类树、右侧为详情，详情标题放大加粗并以横线与正文分隔；正文下方是与描述明确分开的「目标」区块（每目标一行标签 + `x/y` + 进度条）与可选内嵌图片。**默认不选中任何条目**，右侧显示「未选中任务/教程」占位。半透明黑色简洁风格，内容过长时用仅在滚动/悬停时淡入的极简滚动条（单滑块、无轨道）滚动或拖动。任务与教程默认隐藏，解锁后才显示；定义省略 scope 时默认 global，任务接取/进度/完成全服共享，教程全服解锁、每人独立已读；离线及新玩家登录同步当前状态。全局任务接取时所有在线玩家自动追踪，登录无有效追踪时按 order 与 ID 选择进行中的全局任务；已完成任务从任务界面移除；教程解锁带“新”角标、**点开条目即在客户端本地即时去角标**（再由服务端同步确认）。任务同时只能标记一个，获取新任务自动改标记；被标记任务显示在右上角带边框追踪栏（含目标进度条），位于时间 HUD 之下并随其显隐上下移动。追踪栏由 tick 驱动的状态机控制：**每次进入正常视角弹出、离开时收回**（离开进入不透明界面时收回被界面遮挡不可见）；**任务完成时目标进度条补满并显示 ✔、停留后再收回**，不直接消失。有未读新任务/教程时，追踪栏下方显示更小字号的提示行「NEW 有新任务/教程，按 J/I 查看」。 |
| 编辑方式 | 任务、教程、分类均由数据包 JSON 定义，模组不内置游戏内编辑器；`/reload` 生效。奖励目前不写死类型，一律用 `on_complete` 命令列表表达。 |
| 数据位置 | `data/<ns>/icecore/quests/*.json`、`icecore/tutorials/*.json`、`icecore/journal_categories/*.json`。进度：世界 SavedData `icecore_quest_players`（每玩家进度+单标记）、`icecore_quest_global`（GLOBAL 作用域进度）、`icecore_tutorial_players`（个人模式解锁/已读、全局已读代次）、`icecore_tutorial_global`（共享解锁与代次，锁定条目保留代次与迁移标记）。文本（含目标 `desc`）由 `JournalText` 解析：**默认字面量**（裸串直接显示，无需前缀），`translate:` 前缀走翻译键，`literal:` 前缀兼容保留（剥离后字面）。任务与教程均支持 `images` 字段（字符串或 `{texture,width,height}`）在详情内嵌图片。 |
| 命令 | 权限等级 2：`/icecore quest acquire\|complete\|mark\|unmark\|progress\|reset <id> [targets]`；`/icecore tutorial unlock\|read\|reset <id> [targets]`。 |
| 代码入口 | `journal.CategoryManager`、`JournalText`、`JournalType`、`CategoryDefinition`、`JournalEvents`、`ChatBoxJournalEvents`、`journal.client.*`（`JournalScreen`、`JournalKeybinds`、`JournalClientEvents`、`Scrollbar`、`JournalTheme`、`TopRightHudStack`、`EntryView`）；`quest.*`（`QuestDefinition(Manager)`、`QuestScope`、`QuestState`、`QuestProgress`、`QuestPlayerData`、`QuestGlobalData`、`QuestService`、`QuestSnapshot`、`QuestNetworking`、`QuestCommands`、`objective.*`、`client.QuestClientState`、`client.QuestScreen`、`client.QuestTrackerHud`）；`tutorial.*` 对应类；网络 `ClientBoundQuestSyncPacket`/`ClientBoundTutorialSyncPacket`/`ServerBoundSetMarkPacket`/`ServerBoundMarkTutorialReadPacket`（`ICECoreNetwork` discriminator 12–15，协议版本升至 8）。 |
| 实现链路 | 三个 `SimpleJsonResourceReloadListener` 经 `AddReloadListenerEvent` 加载定义并 `revision` 热重载 → `QuestService`/`TutorialService` 在 `ServerStartingEvent` 取 SavedData，按 `scope` 路由每玩家/全局进度，标记始终每玩家 → 变更后经同步 Hook 发增量包，客户端静态 `QuestClientState`/`TutorialClientState` 缓存并 `LoggingOut` 重置 → J/I 打开 `Screen` 子类渲染分类树与详情；奖励/`on_accept`/`on_complete` 用玩家命令源以权限 2 执行。目标类型经 `ObjectiveRegistry` 可插拔，内置 `talk_to_npc`（`NpcEntity.mobInteract` 成功后 `QuestService.notifyObjectiveEvent` 推进，仅遍历该玩家 active 任务）、`command`（命令推进）与 `open_journal`（字段 `journal` 为 `quest`/`tutorial`，`JournalScreen.removed()` 关闭时发 `ServerBoundJournalClosedPacket`，服务端据 `journal` 推进——用于「打开某界面并关闭即完成」）；ChatBox `registerComponentEvent` 注册 `icecore_quest`/`icecore_tutorial` 事件，对话可 `accept:/complete:/mark:` 或 `unlock:/read:`，服务端复核 id 不信任 C2S。ChatBox 为强制 `compileOnly` 依赖，不打包。 |
| 性能与生命周期 | 定义加载/热重载为按需；进度变更事件驱动、按玩家单包同步；`notifyObjectiveEvent` 仅遍历单个玩家 active 任务，NPC 交互为低频事件；HUD 每帧仅格式化单张卡片、动画基于 `nanoTime`、淡出量化 alpha 夹到 ≥4 避免字体强制不透明；滚动条自隐藏基于 `nanoTime` 时间。 |
| 详细说明 | 已全部落地，事实以本条目为准（原交接文档 `quest-tutorial-system.md` 已删除）。 |
| 验证方法 | Forge 47.4.10 `compileJava`、`processResources --rerun-tasks`、`clean build`（含测试）通过；生产 JAR 含全部 `journal/quest/tutorial` 类且不含 `com.zhenshiz`（ChatBox 未打包）；ChatBox `registerComponentEvent`/`EventExecutor.registerEvent`（`EXECUTORS` 静态 `final Map` put，构造期安全）签名以 JAR `javap` 核对。键位、界面渲染、追踪 HUD 动画与避让、对话接任务等客户端行为待用户授权后实机验证。 |
| 全局兼容与生命周期 | 2026-10-04：任务解析默认 global；教程定义新增 scope 默认 global。启动与 OnDatapackSyncEvent 全量重载分支在服务端线程执行 reconcile，再同步在线玩家；登录 prepareLogin + 快照覆盖离线/新玩家。任务按 ID 记录 Migrated：已有全局记录优先，无记录时 COMPLETE > ACTIVE > HIDDEN、目标计数逐项取最大并夹到目标值；迁移后删除个人副本，事件推进还按作用域过滤，避免重复计数。教程以 Entries 中持久化条目为迁移标记；任一旧解锁即全局解锁，旧已读迁移到代次 1，重置保留代次、再解锁 +1。迁移与登录不运行回调。全局命令选择多个玩家只执行首次操作，个人追踪/已读仍逐玩家。全局变更广播在线玩家，读状态与手动追踪只同步当事人。无 tick、文件轮询或离线玩家文件 I/O；迁移仅启动/重载按旧记录扫描，低频变更同步 O(在线玩家 × 快照条目数)。 |
| 本次验证 | JournalGlobalDataTest / JournalGlobalServiceTest 覆盖解析默认值、显式个人模式、NBT 往返、旧记录合并/优先级/幂等、离线与新玩家状态、独立已读、重解锁代次、在线广播与回调次数；基线 Forge 47.4.10 窄测试通过，独立 clean 后 build（全部测试）通过；JAR 含 TutorialGlobalData/JournalEvents/QuestService，未打入 Mockito；Git 忽略边界与 CodeGraph 状态正常。Minecraft 未启动，多人登录/界面/动画待实机验收。 |
| 最后修改 | 2026-09-03，首次实现任务与教程系统骨架、界面、追踪 HUD 与 NPC/对话/货币联动；新增 `open_journal` 目标类型与 `ServerBoundJournalClosedPacket`。同日细化：界面默认不选中并显示占位、教程点开本地即时去角标、目标区块与描述分离并带 `x/y` 进度条、任务完成时进度条补满+✔ 再收回、追踪栏改为 tick 状态机（进入正常视角弹出/离开收回）、未读提示行、任务与教程支持内嵌图片。再次细化：任务列表 NEW 红点改为按条「未开过」判定、点开该条即去红点、未读提示行以「是否还有 NEW」判定；目标区块去掉「目标」标题、描述改小号细体、完成时数字位置变 ✔、`count<=1` 不显示数字；修复重置后再次完成不触发完成动画（离开正常视角时冻结而非清空 `shownId`/`cachedSnapshot`）；`JournalText` 默认改为字面量（裸串直显），`translate:` 走翻译键，`literal:` 兼容保留。 |

### 客户端内容位置（咖啡馆实例）

| 字段 | 内容 |
| --- | --- |
| 记录用途 | ICEcore 的可自定义内容（具体 NPC、任务、教程、分类、对话）不写死在模组，创作时写入客户端实例，模组只提供机制。记录当前 咖啡馆 实例中已放置的 ICEcore 内容及其位置。 |
| 落点机制 | 该实例装有 KubeJS；`kubejs/data/<ns>/` 自动作为数据包、`kubejs/assets/<ns>/` 自动作为资源包加载，无需 `/datapack enable` 或勾选资源包。根目录 `D:\Minecraft\PCL启动器\.minecraft\versions\咖啡馆\kubejs`。 |
| NPC「火」 | `data/icecore/icecore/npcs/fire.json`（细臂皮肤，对话 `icecore:npc/fire` 组 `greeting`）、`data/icecore/chatbox/dialogues/npc/fire.json`、`data/icecore/chatbox/theme/npc.json`、`assets/icecore/textures/entity/npc/fire.png`、`assets/icecore/lang/{zh_cn,en_us}.json`（fire 名称/对话/选项键）。 |
| 对话剧情 | 「火」右键问候后，选「你好」进入询问节点「要尝试接取新的任务吗…」；选「要」进入发放节点，其 `command` 执行 `icecore quest acquire icecore:open_tutorial; icecore tutorial unlock icecore:tutorials_and_quests` 并回「你的任务已经发放，请查收。」，选「好的」关闭；选「不要」或首节点「再见」直接关闭（`next` 指向越界索引 `3`）。 |
| 任务/教程 | 分类 `journal_categories/mainline.json`（`主线`）；任务 `quests/open_tutorial.json`（标题「打开教程」，目标 `icecore:open_journal` journal=tutorial count=1，打开并关闭教程界面即完成）；教程 `tutorials/tutorials_and_quests.json`（标题「教程与任务」，正文「按 J 打开任务，按 I 打开教程。」）。任务/教程/分类正文用 `literal:` 直写中文。 |
| 生效方式 | 数据包侧（NPC/对话/任务/教程/分类）`/reload` 生效；资源侧（贴图/语言）F3+T 重载资源或重启客户端。 |
| 验证方法 | 以 Node 校验全部客户端 JSON 语法通过、`zh_cn`/`en_us` 键集一致；模组 `clean build` 后成品 JAR 内已无 fire.json/fire.png。实机（生成 NPC、对话接任务、按 I 开关教程完成任务、右上追踪 HUD 弹出）待用户授权后验证。 |
| 最后修改 | 2026-09-03，首次将「火」迁入并新增「打开教程」任务与「教程与任务」教程及火的接任务对话分支。 |

### 水产乐事炸鱼卷料理台配方兼容

| 属性 | 值 |
|---|---|
| 功能领域 | 外部模组配方修改 - 水产乐事炸鱼卷改为幻想乡料理台配方 |
| 实现方式 | 客户端 KubeJS 脚本 + 资源包覆盖（非 ICEcore 模组内容） |
| 配方修改 | **移除**：`aquaculturedelight:fried_perch_roll` 的原始 `minecraft:crafting_shapeless` 配方（2炸鲈鱼片+1熟米饭→2炸鱼卷）。**新增**：`youkaisfeasts:cuisine_ordered` 料理台配方（基底 `youkaisfeasts:sushi` + 输入 `aquaculturedelight:crispy_fried_perch` → 2个 `aquaculturedelight:fried_perch_roll`）。参考 `youkaisfeasts:otoro_nigiri` 的配方结构和成品数量。 |
| 模型修改 | 从 `displaydelight:block/acd_small_plated_fried_perch_roll` 移除盘子元素（第二个元素 from `[4,0,4]` to `[12,1,12]`），只保留食物本体（from `[7,1,5.5]` to `[9,3,10.5]`），补全底部 UV 为 `[0,0,2,5]`（与顶部一致），贴图使用 `displaydelight:block/acd_fried_perch_roll`。通过 KubeJS 资源包覆盖原模型文件。 |
| 代码入口 | **客户端 KubeJS 脚本**：`kubejs/server_scripts/remove_recipes.js`（移除原配方）、`kubejs/server_scripts/add_cuisine_recipes.js`（添加料理台配方）。**客户端资源包**：`kubejs/assets/displaydelight/models/block/acd_small_plated_fried_perch_roll.json`（覆盖模型）。 |
| 资源位置 | **模型工作目录**：`D:/Minecraft/Mod Develop/模型/fried_perch_roll_cuisine/`（包含修改记录 README.md、修改后模型 JSON 和原始贴图）。**客户端部署**：`D:\Minecraft\PCL启动器\.minecraft\versions\异次元餐厅\kubejs\` |
| 涉及模组 | youkaisfeasts 0.5.0（提供料理台和配方类型）、aquaculturedelight（提供炸鱼卷物品）、displaydelight 1.7.0（提供原始展示模型）。模组 JAR 已归档到 `libs/`。 |
| 验证方法 | 1. 启动游戏，JEI 查看炸鱼卷配方，确认合成台配方已移除，只显示料理台配方。2. 在料理台放置寿司基底 + 炸鲈鱼片，确认能制作2个炸鱼卷。3. 观察料理台上的展示模型，确认为无盘子的食物本体。4. 检查模型底部是否有完整贴图（无透明/缺失）。 |
| 性能与影响 | 配方替换不增加运行时开销；模型覆盖仅替换一个已存在的方块模型文件，不增加贴图数量或渲染调用。纯客户端实现，不影响服务端。 |
| 最后修改 | 2026-09-22，首次实现水产乐事炸鱼卷的料理台配方兼容，参考幻想乡大鳍金枪鱼寿司的配方结构。 |

### Supplementaries flag 配方条件兼容

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 修复同时安装 Supplementaries + Moonlight 时，进入/创建世界偶发崩溃 `Unknown condition type: supplementaries:flag` |
| 触发场景 | Supplementaries 经 Moonlight `RegHelper.registerSimpleRecipeCondition` 注册 `supplementaries:flag`，但其 Moonlight 动态生成配方（如 `supplementaries:recipes/sign_post_oak.json`）在世界加载时于异步资源生成线程（`ServerDynamicResourcesGenerator`，`pool-*-thread-*`）被解析，调用 Forge `CraftingHelper.getCondition`；该条件此刻不在 `CraftingHelper.conditions` map 中即抛 `JsonSyntaxException` 崩溃。崩溃报告 `错误报告-2026-9-3_11.38.09.zip` 的失败帧为 `CraftingHelper.getCondition`；Supplementaries 于 11:36:35 `Finished mod setup`，异步生成于 11:37:29 仍报 unknown。属 Supplementaries/Moonlight 内部时序问题，与 icecore 内容无因果。 |
| 代码入口 | `compat.supplementaries.SupplementariesFlagConditionCompat`（`@Mod.EventBusSubscriber` MOD 总线自注册） |
| 实现链路 | 仅当 `supplementaries` 已加载时，在 `FMLLoadCompleteEvent`（晚于全部 mod setup、早于世界加载资源生成）`enqueueWork` → 反射读取 Forge `CraftingHelper` 私有 `conditions`（`Map<ResourceLocation,IConditionSerializer<?>>`）→ **仅当 `supplementaries:flag` 缺失**时 `putIfAbsent` 一个 fallback 序列化器，其 `ICondition.test` 恒返回 `true`（sign_post 等特性默认启用语义）。上游若已注册则原样保留，既不双重注册也不覆盖上游行为；反射失败仅告警、不影响加载。 |
| 性能与生命周期 | 一次性、加载完成阶段执行一次反射查/补；不进入 tick、渲染或每次重载路径。 |
| 验证方法 | Forge 47.4.10 `javap` 核对 `CraftingHelper.conditions` 字段类型与 `ICondition.test(IContext)`/`IConditionSerializer.read/write/getID` 签名；`compileJava`、`clean build`（含测试）通过；成品 JAR 含 `SupplementariesFlagConditionCompat` 及内部类。实机需在装 Supplementaries+Moonlight 的客户端反复创建/进入世界确认崩溃消除，且未破坏 sign_post 等配方（待用户授权实机验证）。 |
| 最后修改 | 2026-09-03，首次实现 `supplementaries:flag` 缺失时的 fallback 条件注册兼容。 |

### ChatBox 1.1.4/1.1.5 参考模组与对话事件链路

| 字段 | 内容 |
| --- | --- |
| 记录用途 | 记录“对话框”模组 ChatBox 1.1.4 与 1.1.5（Minecraft 1.20.1 Forge）的本地参考包，以及数据包对话如何显示、推进并触发服务端命令或 KubeJS 事件；ICEcore 固定 NPC 已把它作为双端强制依赖并调用其公开入口，但不会把参考 JAR 打入 ICEcore 成品。运行依赖范围为 `[1.1.4,)`，避免把兼容补丁版本锁死。 |
| 参考 JAR | 旧参考 `libs/chatbox-1.1.4+1.20.1-forge.jar`：1,503,647 字节，SHA-256 `BF7D5D273AFA3C821ACF2F2804F3BC0165AFA07C8C2ADDC8014AAED252AD43E5`；当前编译参考 `libs/chatbox-1.1.5+1.20.1-forge.jar`：1,515,611 字节，SHA-256 `AC0A147764C25FA2E87ED3289A0837E98C494C65A4F392CAABF717BA5B4BDA24`，来自目标“异次元餐厅”实例。1.1.5 元数据声明 `modId=chatbox`、版本 `1.1.5`、JavaFML/Forge `[47,)`、Minecraft `[1.20.1,)`、双端加载，并强制依赖 `cloth_config`；ICEcore 使用的 `serverSkipDialogues` 与 `registerComponentEvent` 签名和 1.1.4 一致。两份 JAR 仅供本地核对，不进入 ICEcore 构建产物。 |
| 对话资源 | 数据包路径为 `data/<命名空间>/chatbox/dialogues/<路径>.json`；同一命名空间还可在 `chatbox/theme/` 提供主题。JAR 自带 `data/test/chatbox/dialogues/intro.json` 与 `intro_gal.json`，资源 ID 分别为 `test:intro`、`test:intro_gal`。根对象以 `dialogues` 将组名映射到有序语句数组；每句至少可含 `dialogBox.name/text`，并可含 `portrait`、`options`、`sound`、`bgm`、`stayTick`、`command`、背景、视频和渲染事件。 |
| 打开方式 | 根命令整体要求权限等级 2。`/chatbox skip <对话资源ID> <组名> [从 0 开始的索引] [目标实体参数]` 由服务端调用 `ChatBoxCommandUtil.serverSkipDialogues`，保存本次目标实体并向玩家同步实体 NBT，再发送 `skip_chat_s2c`；客户端 `ChatBoxUtil.skipDialogues` 从已同步的数据包 JSON 取对应语句，组装 `ChatBoxScreen`、文本框、立绘、选项、音效和主题。`/chatbox open` 只重新打开客户端当前已记录的资源、组和索引，不负责首次选择对话。Java/KubeJS 服务端也可直接调用绑定类 `ChatBoxUtil.serverSkipDialogues(player, resourceLocation, group[, index/targets])`。 |
| 自动触发方式 | 对话根对象可设置原版进度条件格式的 `criteria`。`SimpleCriterionTriggerMixin` 在任意 `SimpleCriterionTrigger.trigger` 后调用 `ChatBoxDialoguesLoader.triggerDialog`；加载器遍历已解析对话条件，匹配后检查并递减该玩家、该资源的持久化最大触发次数，再从该资源第一个组的第 0 句开始对话。触发次数保存在主世界 SavedData `chatbox_trigger_count`，可由 `/chatbox maxTriggerCount` 和 `/chatbox reset` 管理。此入口在所有原版 criterion 触发后遍历全部配置条件，规模为 O(对话资源×条件数)，不适合放入大量无边界对话条件。 |
| 进入语句时的事件链 | 客户端组装当前语句后先在客户端 Forge 总线发布 `SkipChatEvent(player, resourceLocation, group, index, targets)`，随后发送 `skip_chat_c2s`；服务端据资源、组、索引重新读取权威数据，先执行该句顶层 `command`（按 `;` 拆分），再解析并回传动态占位符文本，最后在服务端再次发布同结构的 `SkipChatEvent`。KubeJS 插件把它暴露为 `ChatBoxEvents.skipChat`，事件提供 `player`、`resourceLocation`、`group`、`index`、`targets`；客户端脚本与服务端脚本会各收到对应一侧事件，需要改变玩法状态时只在 `server_scripts` 处理，避免双侧重复触发。关闭时同样发布索引 `-1` 的事件。 |
| 用对话触发命令 | 在某句对象加入 `"command": "say 第一条; give @s minecraft:apple"`，玩家进入该句并完成客户端回传后，由服务端 `ComponentEvent.executeCommands` 逐条执行。命令源是当前玩家、权限等级强制提升到 2、反馈静默；`@s` 指该玩家，目标占位符会先按本次对话目标解析。顶层 `command` 是“进入这一句”触发，不需要玩家选择选项。 |
| 用选项触发事件 | 选项结构可写 `{"text":"example.option","next":"this","click":{"type":"command","value":"give @s minecraft:apple"}}`。数据加载时它被转换为 `ChatOption` 的 `ON_CLICK` 组件事件；点击未隐藏且未锁定的选项后，`EventExecutor` 先执行客户端部分，并在事件类型要求服务端执行时发送 `SendClickEvent(type,value)`。内置 `COMMAND` 的服务端处理器按 `;` 拆分执行命令；`next` 另行控制下一句、指定索引、指定组，`this` 表示停留当前句。内置客户端事件类型还包括 `MVEL`、`JUMP`、`GOTO_NEXT`、`PLAY_VOICE`、`PLAY_SOUND`、`STOP_SOUND`、`SHOW`、`HIDE`、`REPLACE`、`LOCK`、`UNLOCK`、`SET_NORMAL`、`SET_AUTOPLAY`、`SCALE`、`RESTART_ANIMATION`、`PLAY_ANIMATION`。 |
| KubeJS 扩展 | `kubejs.plugins.txt` 注册 `ChatBoxJSPlugin`；它将 `ChatBoxCommandUtil` 绑定为全局 `ChatBoxUtil`，并注册 `ChatBoxEvents.renderPre`、`renderPost`、`skipChat`。外部脚本还能调用 `ChatBoxUtil.registerComponentEvent(type, clientHandler, executeOnServer, serverHandler)` 添加自定义选项/组件事件类型，或用 `addMvelMethod`、`addMvelProperty`、`addPlaceholderResolver` 扩展表达式和占位符。 |
| 安全边界 | 1.1.4 的服务端 `skip_chat_c2s` 处理器直接按客户端上报的资源 ID、组和索引取表，`SendClickEvent.handleOnServer` 也直接按客户端上报的事件类型和值调用执行器；两条路径未在目标字节码中看到“该玩家当前确实处于该句/该选项”的会话令牌、当前索引校验或命令白名单。由于内置 `COMMAND` 最终以权限等级 2 执行，不应把不可信玩家可连接的生产服务器安全边界建立在 ChatBox 1.1.4 的对话 JSON 隐藏/锁定条件上；若 ICEcore 后续兼容它，必须由 ICEcore 服务端重新校验允许的资源、状态、选项和动作，不能直接信任 C2S 字段。 |
| 性能与生命周期 | 数据包 JSON 在资源重载时解析并缓存；正常显示和选项点击按事件执行，不新增逐 tick 文件 I/O。客户端打开期间每隔至少 5 个客户端世界 tick 请求一次目标实体同步；自动 criterion 入口会在所有 `SimpleCriterionTrigger` 触发时扫描全部对话条件，服务端脚本监听需保持 O(1) 或小范围按需处理。 |
| 验证方法 | 以 UTF-8 直接读取 JAR 的 `mods.toml`、内置中英文语言和两个示例对话；使用 `jar tf` 与 `javap -p -c -s` 核对 `ChatBoxCommand`、`ChatBoxCommandUtil`、`ChatBoxUtil`、`ChatBoxDialoguesLoader`、`SimpleCriterionTriggerMixin`、`SimplePayload.onPlayerSkipChat`、`ComponentEvent`、`EventExecutor`、`ChatOption`、`SendClickEvent`、`ChatBoxEventsJS` 与 `CommonEventsPostJS`。未启动 Minecraft 客户端，界面和脚本实机行为仍需在用户明确授权后验证。 |
| 最后修改 | 2026-09-24，加入 ChatBox 1.1.5 编译参考并核对 ICEcore 所用 API 与 1.1.4 签名一致；运行依赖由精确 1.1.4 放宽为 1.1.4 及更高版本。原有对话显示、进度条件触发、进入语句命令、选项事件、Forge/KubeJS 事件与网络安全边界结论来自 1.1.4 字节码核对。 |

### Yuushya 入世声明与 CozyCafe 营业时间

- Yuushya 2.3.0：`YuushyaJoinMessageMixin` 在真实 `lambda$init$4(ServerPlayer)` HEAD 取消声明生成与发送，不过滤普通聊天。
- CozyCafe 1.10：所有维度电脑使用主世界时间，日内 `[12000,24000)`（18:00–次日06:00）禁止营业。`CozyCafeHoursMixin` 在电脑服务端 tick HEAD 使用 `setOpen(false,false)` 停止新客，已有顾客保留原订单、用餐、结算及正常离场。06:00仅恢复手动开店资格，原菜单/座位和每日限制保持。首次夜间加载立即关闭；前进跨关店点（包括整个夜间、多天）也关闭；倒退不触发跨越，夜间状态仍限制。手动白天开店重新记录观察时间。
- 开店资格、setOpen和分配入口均受服务端限制，页面开店失败使用 `ClientBoundCafeCannotOpenPacket((byte)6)` → `CozyCafeManagerScreenErrorMixin` → 电脑原生 errorMessage 状态（120tick兼容计数、6秒单调时钟兜底、末1秒淡出）及Open按钮上方居中提示及 `icecore.cozycafe.closed_hours` 双语翻译。不新增命令或配置文件。
- `CozyCafePendingSpawnMixin` 在异步皮肤查询前捕获电脑实例、时间与营业代次，finalizeSpawn HEAD 复验电脑移除/卸载、当前营业状态、跨关店点和旧代次。仅拒绝 MENU 新客，SIGN 离场保持；取消只释放同UUID菜单预留、重置旅行计时并清空临时电脑引用。现有validate仅对尚未入世的pending_spawn在电脑关闭时释放预留；walking/seated顾客保持。`CozyCafeAdmissionAccess` / `CozyCafePendingAdmission` 为包外桥接。
- 性能：每个已加载电脑每tick O(1)，无新增世界/注册表扫描、I/O、网络轮询或日志。仅开闭变化沿用原生方块/招牌和现有展板同步；每个待生成顾客只保留一份电脑引用及两个long，入世或取消时清除，原异步任务生命周期保持。
- 验证：`CozyCafeHoursTest` 覆盖06:00、18:00、午夜、加速、跨夜、多日、倒退与负数；`CafeHoursCompatibilityTest` 用 libs 当前真实JAR ASM核对入口描述符及可选门禁。未启动客户端；顾客完成订单、多人状态与异步延迟仍待授权实机验收。

- 2026-10-17最终验证：Forge47.4.10独立clean后完整build通过，173项测试零失败；Forge官方当前最新47.4.28完整build通过，minecraft依赖报告确认1.20.1-47.4.28_mapped_official_1.20.1，最终恢复47.4.10完整构建。verifyProductionJar通过，成品Mixin注册、双语键与外部类排除已核对，SHA-256为a6af68bc07f1fdbb79dfeb46fa914d88fc44c96a944b4b9c27643a54a6752ec9。工作区Markdown引用、Git边界、CodeGraph新增符号已核验；未部署客户端JAR或启动客户端。

- 2026-10-17电脑页面提示当前实现：保留errorMessage与原生/扩展原因映射、120tick及6秒单调时钟生命周期；保留原生提示内容包装，仅替换位置参数，调用上游wrapErrorText并缓存原生组件/visualOrderText，完整调用GuiGraphics.renderTooltip带ClientTooltipPositioner，进入Forge RenderTooltipEvent.Pre与Modern UI3.12.0.1 UIManagerForge接管，所有提示框内容样式均由原生流程绘制。不使用TooltipRenderUtil、固定背景色或手绘文字。末1秒以按需透明TextureTarget捕获原生结果并alpha合成；每屏一个目标，尺寸变化resize，过期/关闭destroyBuffers，恢复主目标和GL状态。仅显示期间O(行数)原生提示工作，末秒一份GPU目标和一次屏幕合成，无世界tick、同步I/O、网络轮询。CafeHoursCompatibilityTest读取Modern UI真实JAR核对Pre与positioner，禁止低层自绘并要求目标清理。实机截图场景待用户复测。

### CozyCafe 顾客区域与酒馆展板

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 1.9 电脑、ICEcore 命名区域和 Kaleidoscope Tavern 1.2.0 展板联动 |
| 玩家行为 | 手持酒馆黑板或任意 `kaleidoscope_tavern:sandwich_board` 展板，Shift 右键咖啡馆电脑可绑定整叠；放置后立即显示咖啡馆名称，并在名称换行后空一行显示绿色营业中或红色停业中。电脑改名、开业和停业会事件式同步多个展板。拆除展板清空链接与文字，非冒险模式手动提交文字会解绑；冒险模式不能与任何支持展板互动。顾客进店与离场使用同一门逻辑，可打开关闭的木门和栅栏门并保持开启，不打开铁门。 |
| 配置与命令 | OP 2 手持普通木棍并对准电脑执行 `/icecore cozycafe spawn-region set <区域名>` 或 `clear`；数据位于世界 `data/icecore_cozycafe_ranges.json`，格式版本 2 可读取版本 1。命名区域只负责进出点，不改变菜单识别范围。 |
| 实现链路 | `compat.cozycafe.spawn.CozyCafeSpawnRegionService` 在区域最低 Y 层缓存最多 65536 个均匀采样的可站立候选点；管理器生成和菜单离场分别随机取点，有效区域仅替代 CozyCafe 的标志校验与位置，菜单、声望、每日限制和顾客数量仍走原流程。`compat.cozycafe.board` 使用物品 NBT、方块实体 ForgeData 和 O(1) 电脑反向索引管理绑定；放置、区块加载及电脑状态变化时校验和同步。客户端 Tavern 渲染 Mixin 复用原宽度、行高、对齐与发光规则，名称布局使用 256 项 LRU 缓存并单独绘制翻译后的状态色。 |
| 可选兼容 | 两个真实参考 JAR 仅作为 `libs/` 本地 compileOnly 依赖，不打包；Mixin 插件按模组存在性筛选目标，展板事件仅在 CozyCafe 与 Kaleidoscope Tavern 同时存在时注册。 |
| 性能与生命周期 | 无世界 tick 扫描；进出区域只在首次使用或区域修订后重建有界缓存，使用前最多复验 8 个候选点；顾客每 5 tick 仅检查脚边 3×2×3 方块；展板同步只遍历该电脑的已知绑定并不强制加载区块。停服清空运行时缓存。 |
| 验证方法 | 数据 v1→v2 迁移和生产选择器 JUnit；双语 JSON 解析；Forge 47.4.10 `test`、`clean build`、生产 JAR 内容与外部依赖排除检查。客户端视觉、门路径与多人完整流程待明确授权后实机验证。 |
| 最后修改 | 2026-09-02，首次实现；修复电脑改名后客户端渲染仍读取绑定 NBT 旧名称的问题，名称改为读取酒馆方块实体原生同步文本，绑定 NBT 仅承担定位与状态元数据；补齐 CozyCafe 按 Enter/Esc 结束编辑时只修改客户端副本、未向服务端提交名称的上游缺口；营业时同步隐藏咖啡馆清空按钮、禁用命中并移除 Tooltip，停业后恢复。 |

### CozyCafe 真实顾客与菜单座位

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 1.9 菜单接待资格、真实顾客巡路、乘坐与离场，以及家具模组座椅兼容 |
| 玩家行为 | CozyCafe 的 `cafe_menu` 统一称为“菜单”。菜单仅在其朝向正前一格的下方一格放置有效且面向菜单的座椅时接客；无方向凳子不限制朝向。开店时没有任何有效“菜单与座位”组合会保持停业并提示“无有效的菜单与座位”。顾客从入口以真实实体走到座椅旁、坐下并始终面向菜单；需求物品仍显示在菜单上方。订单等待时长不变，只在顾客坐下且需求生成后开始；坐下后的点单准备阶段不会提前消耗等待时间。 |
| 座椅兼容 | 内置按真实方块类型适配 Another Furniture、Let's Do Furniture、Handcrafted、MrCrayfish's Furniture: Refurbished、Paladin's Furniture 和 Valhelsia Furniture；不按注册名猜测。整合包可通过 `#icecore:cozycafe_seats` 方块标签增加座椅，无方向属性按凳子处理，标准水平 `facing` 必须面向菜单。 |
| 实现链路 | 原版开店条件通过后，管理器只按实际识别范围建立菜单—座椅结构快照；结构组合为零时复用 CozyCafe 管理界面 HUD 原因码 5。结构有效即正常开业，入口到接待点的双向路线随后在服务端主线程按每 tick 最多一次 A* 预热；某菜单第一条路线就绪后才允许生成顾客。每个菜单固定一个紧邻座椅的正交或斜对角接待点，自定义进出区域最多抽取 16 个代表入口、每菜单最多保留 4 条路线。顾客只提交一次到该接待点的路径，到达判定命中的同一 tick 停止导航并入座，不存在独立菜单旁回退或二段停顿；只有导航提前结束或持续无位移时才向同一终点执行最多 3 次局部恢复。 |
| 冲突、失效与存档 | 预留座位会安全移除原家具座位实体或驱离乘客，并在预留期间拒绝再次右键占座。顾客沿用 ICEcore 固定 NPC 的通行语义：不可推动、无实体碰撞但仍可被准星选中，因此入场与离场顾客可在狭窄通道互相穿过；开门只请求一次局部恢复，不再固定频率重算。菜单保存顾客 UUID、座椅坐标、单一接待点和阶段，区块重载后重建预留；离场先回到保存的接待点再向出口寻路。旧版仅有菜单模型而没有 UUID 的状态直接清理，不迁移或结算。 |
| 座位姿态 | 锚点保存并直接应用顾客最终脚部世界坐标，不再混用家具座位实体 Y 与乘客相对偏移。Another Furniture、Handcrafted、Refurbished、Valhelsia 和 Let's Do 分别按目标实例 JAR 的座位实体公式换算；开放标签使用稳定默认值且不反射任意 `height` 字段。 |
| 性能与生命周期 | 菜单方块实体按区块加载/卸载和放置/拆除维护维度索引；开店通常只筛选索引，不扫描整个自定义长方体。路线预热每 tick 最多调用一次寻路器，不存在菜单×入口×接近点在同一 tick 的 A* 矩阵。营业中仅进行 O(1) 绑定检查和必要的局部路径恢复；关店或服务器停止时清除快照、预热任务和运行时预留。 |
| 代码与资源入口 | `compat.cozycafe.seating.CozyCafeSeatingService`、`CozyCafeSeatAnchorEntity`、`CozyCafeSeatingEvents`，以及 `CozyCafeManagerSeatingMixin`、`CozyCafeMenuSeatingMixin`、`CozyCafeCustomerSeatingMixin`、`CozyCafeNavigateToMenuMixin`、`CozyCafeMenuRendererSeatingMixin`；标签为 `data/icecore/tags/blocks/cozycafe_seats.json`。 |
| 验证方法 | 使用 CozyCafe 1.9 与目标实例六个家具模组 JAR 的 `mods.toml`、类签名和字节码核对菜单、顾客导航、到达、关闭、座位类型与朝向入口；源码回归测试同时禁止在顾客 AI 高频路径中对 Java 17 `Stream.toList()` 的不可变结果执行原地排序。运行 `compileJava`、`test`、`processResources --rerun-tasks`、Forge 47.4.10 `clean build`，并核对最终 JAR 的实体、Mixin、标签和双语翻译。客户端仍需逐模组验证正确/错误朝向、入口到座位净空、玩家抢座、多顾客、拆椅、关业和重载。 |
| 最后修改 | 2026-09-06，重写为结构通过即开业、路线分 tick 预热、单一接待点同 tick 入座和无碰撞顾客通行；座位锚点改用最终世界坐标，消除家具高度语义叠加；修复顾客 AI 对不可变候选列表原地排序造成的服务端 tick 崩溃。 |

### 主世界时间、HUD 与床投票跳时

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 主世界昼夜耗时倍率、日期时间 HUD、共享货币日结算、冒险模式床投票和摄像机过场 |
| 玩家行为 | HUD 右上角显示从 06:00 换日的天数、星期和时间；自然流逝跨到下一日 06:00 后，所有在线玩家仅播放昨日净收入打字机提示，不黑屏、不换镜头、不锁输入；冒险玩家在配置区域右键触发方块确认，全员确认后依次播放黑幕、区域摄像机、快速流逝、同一套结算提示和视角恢复；区域摄像机启用到玩家视角恢复期间隐藏普通 HUD。 |
| 配置与命令 | 世界 `data/icecore_time.json`；OP 2 `/icecore time` 命令覆盖倍率、HUD、自然换日结算动画开关、方块列表、区域、摄像机及全部过场时长。`compatibility.dewDropFarmlandGrowth` 可开关 Dew Drop 9.0 时间兼容并配置每日半开处理区间，默认 `[23990, 24000)`。 |
| 代码入口 | `time.TimeService`、`TimeVoteManager`、`TimeCutsceneClient`、`TimeHud`、`TimeCommands`；时间自增与原版命令兼容入口为三个时间 Mixin；Dew Drop 9.0 兼容入口为 `compat.dewdrop.DewDropFarmlandTimeCompat`、`time.DailyTimeWindow` 和时间 JSON 的 `TimeCompatibilityConfig`。 |
| 实现链路 | 主世界自然昼夜自增按倍率小数累积 → 每秒快照校正客户端天空和 HUD；服务端用本 tick 自然推进标记区分倍率自然流逝与外部 `/time`，收入桶结算完成后才向全部在线玩家发送带序号的自然日结包，床快进运行时不重复发送；Dew Drop 9.0 兼容在每次正向 `setDayTime` 时用 O(1) 日界计算判断是否进入或跨过配置区间，并在下一个主世界 tick 开启完整 10 tick 脉冲，Level tick 内临时令上游 `dailyTimeMin` 命中或禁用并在结束时恢复，从而复用其农田、花盆、耕作沙及 Vinery 的完整生长、枯萎、喷灌、温室和概率逻辑；已处理绝对日写入 `icecore_time_state` SavedData，重启和同日配置切换不重复，大幅跨日回拨时开启新时间纪元；`doDaylightCycle=false` 沿用上游规则暂停登记与脉冲消耗；HUD 时钟固定为 `HH:MM` 五个字符槽，分钟真正变化后仅让受影响数字在有界短时长内滚动，完成后保持静止，日/月暂时使用参考模组的 `☀`/`☾` 字体符号，与时间数字共用基线和字号（后续图标高度不得超过数字）；自然日结与床过场共用 `DaySummaryRenderer` 的文本、慢速打字机、2 秒停留和平滑淡出，前者只监听 GUI 后置绘制且无相机、输入锁或 ACK；右键按建造方块列表与区域匹配 → 服务端共享投票 → 全票启动带序号的过场，并按 `(目标刻-当前刻)/1000×fastForward` 动态计算快进总时长，默认游戏内 1 小时/现实 1 秒 → 黑幕后建立客户端摄像机，将临时实体基准 Y 减去其实际眼高以让摄像机眼睛精确落在记录坐标，每帧同步当前/上一帧变换并锁定配置 yaw/pitch，同时取消普通/Jade GUI overlay 与第一人称手臂；过场时间由未暂停的客户端 tick 累积，渲染叠加 partial tick，不读取系统墙钟，`Minecraft#isPaused` 时阶段与天空都不推进 → 世界内鼠标和移动保持锁定，任意 Screen 打开后停止释放键鼠映射，ESC、聊天输入和界面点击正常工作 → 全黑时先把玩家上一帧位置、旋转与身体朝向折叠到当前变换，再切回玩家摄像机；手臂继续隐藏到淡出完全结束，投票文字则从过场开始保持抑制，直到 ACK 后服务端清票快照到达才恢复 → 最终淡出完成后客户端发送 ACK 并立即清除过场、输入与相机状态，以独立最后序号拒绝迟到旧包 → 服务端单次恢复原位置、完整旋转与伤害状态并清票。 |
| 性能与生命周期 | 不扫描世界方块；只处理准星目标和在线玩家，投票变化才同步，每秒发送小型时间快照；Dew Drop 兼容每次时间写入和主世界 tick 仅做常数次整数比较与 `VarHandle` 字段读写，反射句柄只在服务端启动时解析一次，无集合、对象分配、文件 I/O、网络包或日志循环；过场每帧只覆盖相机角度和固定变换，不创建新对象、不发送网络包，服务端只清零速度而不逐 tick 传送；状态有 ACK、超时、中止、断线、换维度和停服清理。 |
| 验证方法 | 日历边界、Dew Drop 窗口默认值/非法配置/自然进入/整窗跨越/区间内加载/倒退/跨多日、处理日 SavedData 旧档迁移/往返/清除、时钟字符槽、过场输入/相机/HUD/非逐 tick 传送安全门禁与时间 Mixin 生产选择器 JUnit 测试、`compileJava`、资源处理、Forge 47.4.10 `clean build`、生产 JAR Mixin/数据包/语言检查；Dew Drop 9.0 的 `Config.dailyTimeMin:I`、三个方块 scheduled tick 与两个 Vinery Mixin 的 10 tick 调度、`doDaylightCycle` 门禁和 `[dailyTimeMin,dailyTimeMin+10)` 比较均以目标 JAR 字节码核对；使用 Forge 47.4.22 客户端 JAR、1.20.1 SRG JAR 和 Forge 47.4.10 事件类核对时间入口、原版 `/time` 入口、`InputEvent.Key` 不可取消、鼠标按钮与 GUI overlay 可取消及视口角度 API；实机视觉、启动与多人验收待用户执行或明确授权。 |
| 最后修改 | 2026-09-07，新增 Dew Drop Farmland Growth 9.0 时间兼容：可配置每日处理区间，默认 `[23990,24000)`，并用跨越检测与有界脉冲适配时间加速、减速、床快进和命令跳时；原时间系统于 2026-08-26 首次实现并完成 HUD、自然日结、过场、暂停时钟及单人提示等修正。 |

### 全服共享虚拟货币

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 世界级共享余额、HUD、命令、数据包接口，以及 CozyCafe 与 SDM Shop Rework 付款兼容 |
| 玩家行为 | 世界初始余额为 `$0`，所有玩家共享同一份非负 64 位余额；HUD 默认在左上角显示英文千分位余额。真实增减时数字约 0.6 秒平滑过渡，右侧显示偏深绿的 `+金额$` 或偏深红的 `-金额$`，出现时短暂放大并在 3 秒内淡出；后续变化覆盖前一提示。淡出透明度进入 Minecraft 字体的 Alpha 截止区间（0～3）时直接停止提交文字，避免字体将其强制恢复为完全不透明而在末尾闪回。成功增减和失败操作当前均不播放音效；失败也不触发余额动画。F1 隐藏 GUI 时不绘制，打开普通菜单时继续显示。 |
| 存档与命令 | 余额与 HUD 总开关使用主世界 `SavedData` 保存到 `data/icecore_currency.dat`。权限等级 2 的命令为 `/icecore currency get`、`set <金额>`、`add <正金额>`、`remove <正金额>`，以及 `/icecore currency hud get/on/off/toggle`。余额范围为 `0..Long.MAX_VALUE`；余额不足或增加溢出时整次失败且不修改余额。`set/add/remove` 的成功与失败均不发送聊天反馈，只用 Brigadier 返回值 `1/0` 表达结果；查询余额和 HUD 管理命令保留反馈。 |
| 数据包接口 | 命令成功返回 `1`、失败返回 `0`；完整 64 位数据写入命令存储 `icecore:currency`。根字段为 `balance`、`hud_enabled`，最近变更为 `change.previous/current/delta/requested/operation/source/name/dimension/x/y/z`，实体来源另有 `change.uuid`。每次真实变化排入有界队列，在服务端 tick 最多执行 64 次 `#icecore:currency_changed` 函数标签；执行前恢复该笔变化的存储上下文，函数继承来源实体、维度与位置并以权限 2 执行。回调内的新变化继续入队，不递归执行函数栈。 |
| CozyCafe 兼容 | `CozyCafePaymentMixin` 只在 CozyCafe 1.8 存在时作用于 `CafeMenuBlockEntity#handlePayment`：保留等待奖励、品质、属性倍率与向下取整后的最终价格，禁用 Numismatics Utils 存款分支，并把物理货币列表调用重定向到全服余额后返回空列表。因此无论 Numismatics Utils 是否安装或原配置是否开启，顾客付款都只进入 ICEcore 余额，不掉币、不存入外部账户，也不显示 CozyCafe 原存款操作栏消息；顾客、声望、菜单、餐盘和离场流程不变。 |
| SDM Shop Rework 兼容 | 目标版本 `sdmshoprework` 1.20.1-6.11-pre 的 `MoneySellerType` 通过 `EconomyManager.economy` 执行余额读取、绝对值写入和主动同步；可选 `SDMShopEconomyMixin` 在该模块初始化结束后安装 ICEcore 回调。购买与出售均使用全服共享余额，商店主动查询、玩家登录和任意 ICEcore 余额变化会把当前值同步到 SDM Economy 的客户端 `basic_money` 缓存。`SDMShopMoneyFormatMixin` 覆盖两个 `moneyString` 重载，把商店余额、价格与编辑界面的卢布前缀统一改为 ICEcore HUD 使用的 `$` 和英文千分位；SDM 的 PNG 钱币资源也覆盖为相同的白色 `$` 像素图标。可选 Mixin 仅通过类路径 `.class` 资源判断目标模组是否存在，不使用 `Class.forName` 提前定义目标类，保证金额格式 Mixin 能在准备阶段正常应用。 |
| 代码入口 | `currency.CurrencyService`、`CurrencySavedData`、`CurrencyCommands`、`CurrencyEvents`、`ClientBoundCurrencyPacket`、`CurrencyClientState`、`CurrencyHud`、`CurrencySoundHooks`、`compat.sdmshop.SDMShopCompat`、`mixin.CozyCafePaymentMixin`、`mixin.SDMShopEconomyMixin`、`mixin.SDMShopMoneyFormatMixin`。 |
| 资源位置 | `assets/icecore/lang/zh_cn.json`、`en_us.json`、`data/icecore/tags/functions/currency_changed.json`，以及覆盖外部命名空间的 `assets/sdmshoprework/textures/icons/money.png`。成功增加/减少的统一音效钩子当前为空，等待专用资源；失败音效及其注册、字幕和 OGG 已移除。 |
| 性能与生命周期 | 余额变更为事件驱动：每笔成功写入执行 O(在线玩家数) 的 ICEcore 单包广播；安装 SDM Shop Rework 时再对在线玩家逐一更新其客户端余额缓存。失败不广播状态或音效。空闲时无货币网络包、集合扫描或文件 I/O。存档只标脏并沿用原版保存周期。HUD 每帧仅格式化一个 64 位整数并最多绘制两段短文本；回调每 tick 硬限制 64 笔，剩余队列延后处理。登录快照只初始化客户端，不播放动画。 |
| 验证方法 | Forge 47.4.10 执行 `compileJava`、`test`、`processResources --rerun-tasks` 与 `clean build`；回归测试固定禁止 `ICECoreMixinPlugin` 使用 `Class.forName` 探测可选 Mixin 目标；核对生产 JAR 内 SavedData、命令、客户端包/HUD、Mixin、函数标签、双语语言和 32×32 透明 `$` 图标。使用 CozyCafe JAR 的 `javap` 核对 `handlePayment` 与 `PaymentUtils.getPaymentItems(I)`；使用 SDM Shop Rework 1.20.1-6.11-pre JAR 的 `mods.toml` 与 `javap` 核对模组版本、`EconomyManager.init()V`、`EconomyModule(Consumer,BiConsumer,Function)`、`MoneySellerType#getCount/buy`、`SendGetMoneyC2S#handle`、两个 `moneyString` 描述符及 `sdmshoprework:textures/icons/money.png` 引用。实机仍需验证客户端能够越过 Mixin 准备阶段、多玩家登录、存档重启、F1/菜单/HUD 开关、连续动画覆盖、失败静默、SDM 商店买入/卖出/重新打开后的余额与 `$` 显示，以及无 CozyCafe/仅 CozyCafe/附带 Numismatics Utils 三种组合。 |
| 最后修改 | 2026-09-02，新增 SDM Shop Rework 1.20.1-6.11-pre 联动：商店钱币读写、主动同步与 ICEcore 全服共享余额统一，并将 SDM 钱币图标替换为 ICEcore 的 `$` 标识；修复可选 Mixin 探测提前加载 `SDMShopRework`、导致客户端在 Mixin PREPARE 阶段崩溃。 |

### IDEA 与 Gradle 本地构建

- 2026-10-16 生产包验证：`build.gradle` 的 `verifyProductionJar` 依赖 `reobfJar`，并接入 `check/build`。从实际 `createMcpToSrg/output.tsrg` 建立Minecraft需重命名方法表，逐类读取最终JAR常量池的方法引用，发现开发名即拒绝构建；可用 `-PproductionJar=<待检JAR>` 检查其它成品。仅构建期扫描，无运行时开销。19:28用户报告证实客户端mods中的包仍调用ResourceLocation.getPath，而当前生产包已调用m_135815_；两个SHA-256不同。正向验证当前包通过，反向验证出错包被拒绝；独立clean后Forge47.4.10完整build和166项测试通过，生产映射校验随build执行通过。不改种子袋注册语义，不自动部署或启动客户端。

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 开发者构建流程 |
| 使用行为 | IDEA 的 Gradle 构建任务和 `gradlew.bat` 共用项目内的 Gradle 8.8 分发缓存；已有完整缓存时，手动构建 JAR 不需要访问 Gradle 分发下载地址。 |
| 配置入口 | `gradle/wrapper/gradle-wrapper.properties` |
| 实现链路 | Gradle Wrapper 使用 `PROJECT` 作为分发与压缩包基目录 → 从 `.gradle/wrapper/dists` 查找 Gradle 8.8 → 启动本地分发执行项目任务；`.gradle/` 仍由 `.gitignore` 忽略，不将本机缓存提交到 Git。 |
| 资源位置 | `.gradle/wrapper/dists/gradle-8.8-bin/`（本地构建缓存） |
| 验证方法 | 使用 `gradlew.bat --offline --version` 确认 Wrapper 可离线启动，再执行 `gradlew.bat clean build`；核对 Git 状态不包含 `.gradle/`。 |
| 最后修改 | 2026-08-19，修正 IDEA 手动构建时因用户目录残缺缓存而重复下载 Gradle。 |

### 模组加载骨架

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 模组启动与元数据 |
| 玩家行为 | 无。模组仅在 Forge 模组列表中显示为 `ICEcore` |
| 代码入口 | `com.yinfires.icecore.ICECore` |
| 资源位置 | `src/main/resources/META-INF/mods.toml`、`src/main/resources/pack.mcmeta` |
| 实现链路 | Forge 读取 `mods.toml` → 根据 `icecore` 定位 `@Mod` → 构造无副作用的 `ICECore` 实例 |
| 验证方法 | 用 Java 17 执行 `gradlew.bat clean build`；检查 JAR 中入口类、展开后的元数据和 `LICENSE_icecore` |
| 最后修改 | 2026-08-18，建立 0.1.0 最小骨架 |

### GeckoLib 动画关键帧格式兼容

- 2026-09-09：报告 `D:/下载/错误报告-2026-9-9_13.25.41.zip` 在首次资源重载时抛出 `Expected post to be a JsonArray, was an object`；实际加载 GeckoLib 4.2、Forge 47.4.22。`libs/geklib-1.20-4.2.jar` 从咖啡馆实例复制，仅供核对，SHA-256 `65ef8cfd33b981a85c8e8c57fd7ec342df9c7cbc4d1f7cff500906d31b17ef9a`。元数据 Minecraft 范围 `[1.20,)`，不能根据文件名判为版本不兼容。
- 实际资源来源：`elegant_countryside-2025-forge-1.20.1.jar` 的 `assets/elegant_countryside/animations/` 下 `cupcoffe.animation.json`、`feng_ren_ji__-_converted.animation.json`；`textiles_and_trinkets-1.1.3-forge-1.20.1.jar` 的 `assets/textiles_and_trinkets/animations/` 下 `imp_tail`、`lesser_spirit_cosmetic`、`lush_golem_entity`、`tail_animated`、`vigil_descender`（均为 `.animation.json`）。均含 `pre/post: {vector: [...]}`。报告未给出首个失败资源路径，不能断言其中某一个是唯一来源。
- 2026-09-09 第二次报告 `错误报告-2026-9-9_13.53.27.zip` 明确显示旧重定向已加载，但 `post` 含 `easing: easeInBack`，被单字段限制排除。已替换旧重定向：在 `getTripletObj(JsonElement):List` HEAD 仅对包含包装向量的通道返回兼容展开结果；保持时间轴顺序、pre 原时间和 post 的 `1e-7` 偏移，完整保留 vector/easing/easingArgs 对象，交给上游 `buildKeyframeStack`。
- 字节码证据：`buildKeyframeStack` 对对象取 `vector`，再读取同一对象的 `easing` 与 `easingArgs`；因此不能仅放宽限制并返回数组，否则会丢失缓动。普通数组/数字/原生通道仍走原实现，不吞异常、不删除动画、不修改共享 JSON。插件仅查 `.class` 资源，Mixin 只在客户端启用，无 GeckoLib 时跳过。
- 性能：只在资源加载时扫描单个动画通道，O(关键帧数)；命中兼容时创建与原解析器相同用途的有界结果列表和 Pair，不复制向量/表达式/缓动对象。不增加 tick、渲染、网络、缓存或日志。列表生命周期仍归资源烘焙过程。
- 验证：`src/test/resources/geckolib/wrapped-keyframes.json` 为目标实例两个 JAR、7 个动画文件的全部 205 个受影响关键帧，保留来源路径，共 214 个包装 pre/post，其中 1 个附带 easeInBack。`GeckoKeyframeCompatTest` 逐项验证能展开、时间键正确、对象引用/表达式/缓动和输入不变，并覆盖混合数组通道与 easingArgs、原生回退、生产选择器与加载时序。该测试数据仅属 test resources，不进入模组成品。此次补丁仍须实机启动与 F3+T 验证，不将构建通过当作整包启动成功。
- 本次最终验证：Forge 47.4.10 `clean build` 成功，71 项测试通过；成品包含新的 HEAD 注入与辅助类、JarJar 依赖和展开后的元数据，不含测试样本或测试类；旧 readKeyframeVector 重定向已不存在。CodeGraph sync 正常，Git 差异与忽略边界检查通过。没有再次修改 Forge API，未重复上轮 47.4.23 的兼容矩阵构建。

### Paladin's Furniture 纹理图集加载兼容

| 字段 | 内容 |
| --- | --- |
| 功能领域 | Paladin's Furniture 客户端资源加载兼容 |
| 玩家行为 | 同时安装 Paladin's Furniture 1.5.0 时，客户端初始资源重载不再偶发因调色板纹理源图被提前释放而启动失败。 |
| 代码入口 | `com.yinfires.icecore.mixin.PaladinFurnitureSpriteLoaderMixin` |
| 实现链路 | Paladin's Furniture 的图集 Mixin 会在原版异步加载前预先调用全部 `SpriteSupplier`；ICEcore 在 `SpriteResourceLoader.m_260886_` 已收集完 supplier、尚未复制进最终列表时，仅在 `pfm` 已加载的客户端将原版 `PalettedPermutations$PalettedSpriteSupplier` 替换为本次图集加载内有界的记忆化包装 → PFM 预扫描与原版加载复用同一 `SpriteContents` → 共享 `LazyLoadedImage` 的引用计数只消费一次，避免并行任务读取已关闭的 `NativeImage`。 |
| 性能边界 | 仅初始/手动资源重载时遍历本次图集 supplier 映射；只为调色板排列 supplier 分配包装对象，不进入 tick、渲染帧或服务端路径，缓存随本次图集加载结果释放。 |
| 验证方法 | 核对 PFM 1.5.0 `PFMAtlasLoaderMixin.pfm$AdditionalSources`、原版 1.20.1 `SpriteResourceLoader.list`、`PalettedSpriteSupplier.get` 与 `LazyLoadedImage.release` 字节码；执行 `compileJava`、资源处理、基线 `clean build`，并检查最终 JAR 的 Mixin 配置。实际客户端连续启动复测需用户授权后执行。 |
| 最后修改 | 2026-08-23，改用目标生产客户端验证的 SRG 方法名，并在 supplier 复制进最终列表前完成包装，兼容 PFM 预扫描。 |

### CozyCafe、捕星者与 Tide 启动兼容

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 1.9 与捕星者 Starcatcher 3.1.4.1 的 Forge Jar-in-Jar 依赖选择，以及捕星者内置 Tide 数据包格式兼容 |
| 玩家行为 | 同时安装 CozyCafe 1.9、捕星者 3.1.4.1 与 ICEcore 时，启动阶段统一使用 LibTooltips 2.3，不再因 CozyCafe 内置的 2.2 缺少 `Tooltips$ItemTooltip` 而加载失败；同时安装 Tide 时，修正流星海星条目的鱼饵格式，使世界注册表可以正常加载。 |
| 代码与资源入口 | LibTooltips：`build.gradle` 的 `ICEcoreLibTooltipsCompat` Ivy 仓库、`jarJar` 依赖和 `verifyLibtooltipsCompatSource`，版本与校验值位于 `gradle.properties`。Tide 数据：`compat.starcatcher.StarcatcherTideDataPackCompat` 与 `resourcepacks/starcatcher_tide_compat/`。 |
| 实现链路 | Forge 在任何 `@Mod` 构造前解析嵌套依赖；CozyCafe 以 `curse.maven:libtooltips-1370245` 内置文件版本 8332581（实际模组版本 2.2），捕星者则以另一 Modrinth 坐标内置 2.3。ICEcore 从 Modrinth Maven 获取原版 2.3 二进制，并以 CozyCafe 的依赖坐标和更高的合成选择版本 9000000 打入成品 JAR，使 JarSelector 在该坐标下淘汰 2.2；随后即使捕星者自己的 2.3 候选被去重，保留下来的实现仍为 2.3。捕星者 3.1.4.1 的 `tide_compat` 把流星海星 `starcatcher:bait` 限制写成对象，但同版本 `BaitRestriction` 编解码器及其它内置条目使用 `[{id, extra_chance}]` 数组；ICEcore 仅在捕星者精确版本 3.1.4.1 与 Tide 同时存在时注册 SERVER_DATA 数据包，并放到资源包列表底部，使它在捕星者内置 `tide_compat` 之后覆盖同一资源路径的 JSON。 |
| 安全与性能边界 | LibTooltips 处理仅影响启动时一次性的嵌套依赖发现；数据包处理仅在资源包发现时做常数次模组版本判断，运行期直接使用静态 JSON。两者均不进入 tick、渲染、网络或存档路径。构建前固定校验上游 LibTooltips JAR 的 SHA-256、模组版本和 `ItemTooltip` 类。ICEcore 单独与 CozyCafe 共存时也会把其内置 LibTooltips 提升到兼容的 2.3；Tide JSON 修正则严格限制捕星者 3.1.4.1，避免覆盖未来上游已修复版本。 |
| 验证方法 | 执行 `verifyLibtooltipsCompatSource`、`test` 与 Forge 47.4.10 `clean build`，并额外用报告实例的 Forge 47.4.22 构建。检查标准成品 `icecore-<版本>.jar`：JarJar 元数据使用 `curse.maven:libtooltips-1370245`、选择版本 9000000，嵌套 JAR SHA-256 为 `81718c32ff76cd2dddc4b891e12bb94f0e3e5f9e69e89649e366ff7d7ea47dd6` 且包含 `Tooltips$ItemTooltip.class`；修正包包含 `shooting_starfish.json`，其 `baits` 为单项数组。无嵌套依赖的开发用包明确标为 `-slim.jar`，不得部署到整合包。不自行启动客户端，整合包实机启动留待用户安装新 JAR 后验证。 |
| 最后修改 | 2026-09-08，新增 LibTooltips 2.3 依赖选择兼容，并根据实机报告将捕星者内置 Tide 流星海星修正包调整为资源列表底部覆盖。 |

### CozyCafe 一键清空与旧存档恢复保护

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 1.9 咖啡馆清空、关业与区块实体持久化兼容 |
| 玩家行为 | 咖啡馆营业期间点击电脑“一键清空”按钮不会产生任何反馈，也不会发送清空请求、关闭界面或改变状态；必须先正常关业后才能清空。由旧版错误流程留下的占用菜单会在再次结束服务时自动清理，不再因缺失出口招牌导致存档崩溃或反复无法进入。 |
| 启用条件 | CozyCafe 1.9 安装时由可选 Mixin 自动启用；未安装时 ICEcore 不加载该兼容逻辑。 |
| 代码入口 | `mixin.CozyCafeClearButtonMixin`、`mixin.CozyCafeManagerMenuStateMixin`、`mixin.CozyCafeClearCafeMixin`、`mixin.CozyCafeOrphanedMenuMixin`；桥接接口为 `compat.cozycafe.CozyCafeMenuStateAccess`、`compat.cozycafe.CozyCafeManagerStateAccess`。 |
| 实现链路 | CozyCafe 1.9 原生 `clearCafeData()` 会直接将 `open=false`、`linkedSign=null`、`menu=null`，却不执行正常 `setOpen(false, true)` 的菜单关闭广播；菜单之后调用 `closeMenu` 时会无条件执行 `getCafeManager(...).getLinkedSign().below()`，因此最后顾客结束流程时空指针。ICEcore 读取 CozyCafe 管理菜单已同步的营业状态，在界面初始化及逐帧更新时把清空按钮设为不可用，使点击不会进入原版音效、发包或关闭界面链；服务端清空入口再次检查原生营业状态并取消伪造请求。若旧存档已存在 `hasCustomer=true` 且管理器或关联招牌缺失，菜单关闭入口只清理该孤立状态、餐盘显示和请求物品，并取消上游空指针路径。 |
| 服务端与性能规则 | 仅在玩家执行一键清空或菜单实际结束服务时触发；营业检查为 O(1) 字段读取，旧档恢复只增加一次 O(1) 的关联管理器方块实体查询，不新增 tick 扫描、反射、同步 I/O、网络包或日志。旧存档自愈由服务端完成并标记菜单区块实体已修改。 |
| 资源位置 | 无新增玩家文本或资源。 |
| 验证方法 | 使用 CozyCafe 1.9 JAR 的 `mods.toml` 和 `javap` 核对 `clearCafeData()V`、`sendCloseCommandToMenus(...)V`、`CafeMenuBlockEntity.closeMenu(Z)V`、`linkedSign`、`cafeManager` 与菜单状态字段；执行 `gradlew.bat compileJava test --no-daemon`、Forge 47.4.10 基线 `clean build`，检查最终 JAR 的 Mixin 配置和注入选择器。实机仍需在备份存档中复测开业清空、最后顾客离场与旧坏档首次载入。 |
| 最后修改 | 2026-09-02，禁止营业中一键清空，并增加旧残留菜单自愈。 |

### CozyCafe 菜单编辑 JEI 兼容

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 菜单编辑与 JEI 物品列表兼容 |
| 玩家行为 | 打开 CozyCafe 电脑的菜单编辑界面后，可将 JEI 物品列表中的物品拖到原有输入格、直接拿起物品点击输入格，或对背包物品使用 Shift+左键/右键快捷写入菜单；连续写入成功或失败时，每次都会播放对应音效；悬浮 JEI 列表中的可售食物时，会显示与背包物品相同的价格、分类，以及碗装食物或瓶装饮料提示。 |
| 启用条件 | CozyCafe 1.9 与 JEI 15.49 同时安装时自动启用；缺少任一模组时 ICEcore 不加载其兼容逻辑。 |
| 代码入口 | `com.yinfires.icecore.compat.cozycafe.jei.CozyCafeJeiPlugin`、`CozyCafeGhostIngredientHandler`、`CozyCafeJeiTooltipHandler`、`com.yinfires.icecore.compat.cozycafe.client.CozyCafeClientEvents`；网络入口为 `com.yinfires.icecore.network.ICECoreNetwork`。 |
| 实现链路 | JEI 幽灵物品处理器识别 `MenuSelectorScreen` 的第 0 个输入格；客户端鼠标事件识别同一输入格且光标已拿起物品，或识别输入格以外的有物品槽位在 Shift+左键/右键下的快捷写入；直接鼠标写入在按下事件中记录当前界面和按钮，并在对应的释放事件中取消原版后续容器处理，避免同一次鼠标操作再次发送原生写入；三条路径均只发送单个物品请求 → 服务端确认仍打开有效的 CozyCafe 菜单后调用其 `CafeManagerBlockEntity.addToMenu` → 无论成功或失败都回传带结果的客户端包；客户端同步菜单列表/状态并按结果直接播放 CozyCafe 原有成功或失败音效，从而不依赖状态值变化来触发音效。提示事件仅在该界面中、且鼠标位于 JEI 物品列表时查询 CozyCafe 的 `CafeMenuItemRegistry` 并附加其既有价格、`category.cozycafe.*` 分类和容器翻译信息。 |
| 服务端规则 | 不信任客户端物品请求。可售食物、重复项目、25 项上限与方块实体持久化均由 CozyCafe 原始 `addToMenu` 决定。 |
| 资源位置 | 无新增玩家文本或资源；复用 CozyCafe 的 `gui.cozycafe.menu_selector.*` 翻译键。 |
| 验证方法 | 使用 Java 17 执行 `gradlew.bat clean build`（Forge 47.4.10）与临时 Forge 47.4.22 覆盖构建；使用 `javap` 核对 CozyCafe 1.8 原生菜单写入只更新状态值、失败不发原生结果包，并确认 `quickMoveStack` 的 Shift 快捷入口及 Forge 鼠标按下/释放事件的取消链路；在同时安装 CozyCafe、JEI 的客户端手测直接物品点击、Shift+左键、Shift+右键和 JEI 拖入的连续成功、连续失败、成功与失败交替、每次打开菜单后的第一个直接写入、无效物品、重复项目、菜单满 25 项，以及 JEI 与背包 tooltip 的显示差异，并确认每次输入只执行一次服务端写入和一次结果反馈。 |
| 最后修改 | 2026-08-21，补齐直接鼠标写入的释放事件拦截，避免首个成功写入后被原版释放路径再次判定为失败。 |

### CozyCafe 电脑识别范围覆盖

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 1.9 咖啡馆电脑识别菜单范围扩展 |
| 玩家行为 | 权限等级 2 的玩家手持普通木棍、对准 CozyCafe 咖啡馆电脑并执行 `/icecore cozycafe range set` 后进入该电脑的范围修改模式；左键选择第一个角点并开始一轮新选区，右键选择第二个角点，操作不会破坏或使用被点击方块；两个坐标完整后显示青色无深度遮挡线框。完成后的闭区间范围覆盖该电脑原本按店铺声望星级计算的菜单识别范围，之后不随声望或等级变化。 |
| 配置入口 | 世界 `data/icecore_cozycafe_ranges.json`；管理员命令 `/icecore cozycafe range set`，权限等级 2。每台电脑按维度与方块坐标独立保存；同一位置重新放置电脑时继续沿用该记录。 |
| 启用条件 | CozyCafe 1.9 安装时加载数据和覆盖逻辑；未安装时命令返回不可用提示，ICEcore 其余内容仍可加载。 |
| 代码入口 | `compat.cozycafe.range.CozyCafeRangeCommands`、`CozyCafeRangeAdjustmentManager`、`CozyCafeRangeDataManager`、`CozyCafeRangeEvents`、`CozyCafeRangeClientEvents`、`mixin.CozyCafeManagerRangeMixin`。 |
| 实现链路 | 指令在服务端按实际方块交互距离重新射线检测 `cozycafe:cafe_manager` → 将对应维度和电脑坐标写入带标记的临时木棍并保存空/现有定义 → 服务端最高与最低优先级事件拦截左右键，只有实际持调整木棍的右键手写入第二点，客户端保留必要数据包同时禁止本地方块使用/持续挖掘 → 每次坐标写入复制、校验并原子替换独立 JSON，再向在线客户端发送有版本号的快照用于线框显示 → 可选 Mixin 在 CozyCafe `CafeManagerBlockEntity#getFirstPos/getSecondPos` 入口执行 O(1) 的“维度+电脑坐标”哈希查询，仅当两个坐标完整时返回覆盖角点；顾客菜单搜索和 CozyCafe 原生范围粒子显示都复用这两个入口，因此保持同一范围且不再读取声望星级。 |
| 服务端与性能规则 | 服务端决定目标电脑、坐标和持久化结果；客户端数据只用于显示。每台电脑范围最多包含 65536 个方块，世界最多保存 1024 台电脑。ICEcore 日常额外开销仅为 CozyCafe 请求范围角点时的两次 O(1) 查询，无新增 tick 全世界扫描、同步 I/O 或无界日志；范围体积带来的常态开销来自 CozyCafe 1.8 本身：开启咖啡馆后，它会按顾客生成周期在主线程把整个闭区间复制成 `BlockPos` 列表并逐格检查，时间与临时内存均为 O(范围体积)，多台开启的大范围电脑会叠加，因此仍保留硬上限并建议只覆盖实际需要识别菜单的空间。关闭咖啡馆时的菜单关闭扫描和主动显示原生范围也会各执行一次同规模遍历。 |
| 资源位置 | `assets/icecore/lang/zh_cn.json`、`assets/icecore/lang/en_us.json`；世界数据 `data/icecore_cozycafe_ranges.json`。 |
| 验证方法 | 使用 CozyCafe 1.9 JAR 的 `javap` 核对 `cozycafe:cafe_manager` 注册名、`getFirstPos/getSecondPos` 描述符以及 `assignCustomersInArea`、`sendCloseCommandToMenus`、`showCafeArea` 的共用调用和 O(范围体积) 遍历；Forge 47.4.22 与基线 47.4.10 均执行 `clean build`，最终再用基线构建并核对生产 JAR 的 Mixin 配置、目标描述符、65536 常量、双语语言键和新增类；实机需验证主手/副手木棍、创造/生存/冒险模式、左右键不破坏/不使用方块、跨维度/切换物品恢复木棍、范围粒子与菜单识别一致、声望变化不改变已覆盖范围。 |
| 最后修改 | 2026-08-23，新增按电脑坐标持久化的识别范围覆盖、管理员调整木棍、客户端线框预览、范围/数量硬上限和 CozyCafe 私有范围方法兼容 Mixin；根据 CozyCafe 1.8 的实际扫描链路将单台电脑上限调整为 65536 个方块，并记录开启状态下的 O(范围体积) 性能边界。 |

### CozyCafe 顾客座位与生成诊断

后续客流改造设计见 [咖啡馆客流量计算方案](cafe-customer-flow-design.md)（2026-10-08，尚未实现）。该文档保存当日额度、开店快照与多人生成节奏设计；晚开允许加密至剩余完整营业时间的四分之三，之后间隔与同行分布不再因晚开加密。不替代下表当前实现事实。

| 字段 | 内容 |
| --- | --- |
| 功能领域 | CozyCafe 真实顾客入座、座位兼容和生成节奏 |
| 实现事实 | 开店时只把菜单与座位结构作为“无有效菜单与座位”判定依据；入口可达性由开店后的分 tick 路线预热决定，路线尚未完成不会否决结构有效的咖啡馆。路线预热按菜单轮询，某菜单首条双向路线提交后立即进入可接客状态，备用路线后台补齐。座位锚点直接使用适配器计算的顾客最终世界坐标，Valhelsia 的 `getRidingOffset()` 只表示座位实体世界 Y，并在最终坐标中只应用一次玩家乘客偏移。 |
| 上游事实来源 | 固定以 `libs/cozycafe-1.9-all.jar` 为当前目标事实来源；相关入口为 `CafeManagerBlockEntity#tick`、`#assignCustomersInArea`、`#getMaxCustomers`、`#getStarsFromReputation` 与 `CozyConfig`。解译副本位于 `tmp/decompile/cozy-src/io/github/chakyl/cozycafe/`，仅供诊断，最终结论仍以 JAR 的 `javap` 字节码为准。 |
| 声望与星级 | `getStarsFromReputation()` 的实际公式为 `clamp(floor(reputation / 1000), 0, 5)`：声望 `0–999` 为 0 星、`1000–1999` 为 1 星、`2000–2999` 为 2 星、`3000–3999` 为 3 星、`4000–4999` 为 4 星、`5000` 为 5 星；正常增减入口把声望限制在 `0–5000`。 |
| 每日顾客数量 | `getMaxCustomers()` 每次调用计算 `base + floor(randomFloat(0, 5))`，0–4 星的 `base=(stars+1)*5`，5 星的 `base=40`，因此判停阈值依次为：0 星 `5–9`、1 星 `10–14`、2 星 `15–19`、3 星 `20–24`、4 星 `25–29`、5 星 `40–44`。随机项会在每秒判停时重新抽取，并非开店时固定的每日额度；单批最多 4 人，故成功生成的一批还可能令计数越过当次阈值最多 3 人。仅 `dailyLimit=true` 时成功生成才增加 `attemptedCustomers` 并启用这套每日计数。 |
| 生成节奏 | CozyCafe 1.9 `tick` 的实际周期公式是 `20 * customerSpawnInterval * (1 - max(0, reputation - 1) / 5000)` tick，其中除法为 Java 整数除法。默认 `customerSpawnInterval=40`；正常 `0–5000` 声望下被除数最多为 `4999`，整数商恒为 0，所以所有星级实际均为每 `800 tick`（40 秒）尝试生成一批。`CozyConfig` 注释所述“每星加快 20%”与实际字节码不符，不能按注释推算。ICEcore 不修改该计时公式；路线池为空、预留失败或导航启动失败不得增加顾客计数。 |
| 每批顾客数量 | 上游循环在每生成 1 人后，以 `random < groupCustomerChance` 为停止条件，或达到 4 人时强制停止；配置名和注释容易让人误读为“继续概率”。默认 `groupCustomerChance=0.5` 时，单批 1/2/3/4 人的概率分别为 `50%/25%/12.5%/12.5%`，期望为 `1.875` 人；一般概率记停止概率为 `p` 时，分布为 `p`、`(1-p)p`、`(1-p)^2p`、`(1-p)^3`。可用菜单或有效路线不足时，实际生成量还会更少。 |
| 性能边界 | 开店检测不创建导航路径；路线预热每 tick 最多执行一次 A*，营业期间不重复扫描范围或反射识别家具。 |
| 验证方法 | 对 `libs/cozycafe-1.9-all.jar` 执行 `javap -c -p`，核对上述五个入口中的整数运算、随机范围、循环退出条件与默认配置；代码改动仍执行 `gradlew.bat compileJava test --offline --no-daemon`、资源处理、`clean build` 和 `git diff --check`。实机重点观察有效座位可开店、Valhelsia 坐姿高度及各星级均约 40 秒一次生成尝试。 |
| 最后修改 | 2026-09-06，修复结构检测误依赖路线预热及座位锚点二次高度偏移；同日依据 CozyCafe 1.9 目标 JAR 字节码纠正生成间隔记录，并固化各声望段每日顾客阈值与单批生成概率。 |

### 奇妙商店物语 1.3.1 餐厅客流静态核对（未接入）

- 来源：用户指定 `D:/Minecraft/PCL启动器/.minecraft/versions/奇妙商店物语1.3.1/mods/wonder_shop_story_core-1.3.1.jar`；元数据版本 1.3.1，Minecraft `[1.21.1,1.22)` / NeoForge `[21,)`，不作为 ICEcore 1.20.1 Forge 的直接依赖。2026-10-08 复制为 ZIP 后解包，以 CFR 定位、`javap -c -p` 核对公式与调用端；诊断副本在 `tmp/wonder-shop-inspect/`，未启动客户端、未修改玩法。
- 菜单计数链：`MenuVariety.count → RestaurantShopType.variety → RestaurantRecipes.restaurantPotential`，对已解锁可构成潜在配方且未下架的菜品计数；`seen.add(p.food)` 按结果物品去重。该指标不是当下库存中立即可制作的菜数，实际接待另走 `RestaurantService.menu/diningMenu/admitRestaurant`。
- `CustomerLoyalty.menuVariety(n)`：令 `e=max(0,n-3)`，倍率 `V=1+0.05*min(e,10)+0.02*max(0,e-10)`；3/8/13/25 道对应 1/1.25/1.5/1.74。`varietyPopularity(n)` 在 n≤10 时为 `round((max(1,n)-10)*30/9)`，否则为 `(n-10)*3`；1/3/10/20 道对应 -30/-23/0/+30。
- `CityResidents.effectivePopularity` 合并顾客忠诚、道具、特殊、奖杯、节气、经营方案和菜单人气，减去每条未付款离场记录 5 点后最低为 0。吸引半径 `12*sqrt(人气)`，方案 23/26 分别再加 60/100；街头人口来源基础半径 160，部分有目的抽样扩大为 `160+吸引半径`。
- `CityResidents.wordOfMouth` 按 1–40 顾客类型中的粉丝类型数 F 计算 `W=1+0.05*clamp(F,0,40)`。`CustomerLoyalty.History` 保留最近最多 6 条评分，至少 3 条≥60 且平均≥60 才为粉丝；正常评分≥60 声望+1，<40 声望-1，类型声望限 0–5。
- 街头链：`CityResidents.tick` 每 5 tick 更新、每 20 tick 尝试客流；餐厅理论每秒流量 `人口/600*1.5*W*V`，游戏时间 00:00–05:00 再乘 0.2。`CustomerLoyalty.arrivals` 取整数部分加按小数概率补 1；调用端 `StreetCrowdBudget.arrivals` 再限每来源每秒最多 1 人。默认背景行人全服 144、水平 96 格内 36、全服每秒新增 6，受抑制请求不累积；这不是全部餐厅顾客的统一上限。
- 定向来客链：`regularGuests → scheduleGuest → CustomerLoyalty.visitDelay`；普通基准 500–900 tick、粉丝 300–500 tick，延迟 `max(20,floor(基准*粉丝间隔系数/(2.25*W*V*(boosted?2:1))))` tick。每秒检查接待资格/在途容量，先邀请附近合格行人，没有时按合格类型人口权重选人；来客计时并非必然成交。
- 转化与接待：正常街客需处于需求时间窗，第二需求窗以 30% 身份散列条件启用；同一需求已服务或尝试两店即停止。基础进店概率 `clamp(0.65*吸引设施倍率+(美观*(方案22?2:1)+氛围)/3000,0.35,0.95)`，再叠加方案/节气，有目的顾客通过概率关仍需需求及有效订单。新客允许尝试已知菜品；非试吃客的菜品预测评分需≥40。营业、在线、区块、岗位、菜单、座位/候位、路径等也会限制实际接待。
- 验证边界：上述为目标 JAR 静态字节码事实及默认值，未读取存档实际配置，未执行游戏观察；ICEcore 当前 CozyCafe 声望、客流与菜单规则未改变。

### 森罗物语烟火烤串放置与烟火气移除

2026-10-15 Jade 烧烤架标题精简：`GrillingJadeGrillMixin` 仅在客户端重定向烟火 1.1.1 的 `GrillProvider.appendTooltip` 中 `JadeElements.appendItems(ITooltip,Iterable)` 调用为空操作。该方法用 `ITooltip.append` 将逐槽小图标追加到名称行；下方状态文本仍由原方法添加，Jade 自身物品汇总不变。目标与 Jade API 均通过 class 资源检测，不提前加载可选类。每次提示省去图标复制、元素创建和布局，无新增遍历、缓存、网络或日志。`GrillingSkewerRulesTest` 核对上游唯一图标调用、完整描述符、状态入口保留、客户端注册和可选门禁；Forge 47.4.10 编译、定向测试、独立 clean 后完整 build 通过（139 项测试），生产 JAR 描述符与未打包上游类检查通过，CodeGraph 已更新。实际空/满架、混合烤串和各烤制状态的 HUD 待实机验收；未启动客户端。纯展示精简不写更新日志或 Wiki。

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 单根烤串 Shift 右键不再放置；独立烤串盘、已有摆盘、双手拆串、串制、烧烤和调味保持上游入口。新旧烤串均不显示烟火气及倒计时，不享受热食饱和度或效果时长加成；非烤串食品不变。 |
| 核对来源 | `libs/kaleidoscope_grilling-1.1.1-forge1.20.1.jar`；`SkewerPlatePlacement.onRightClickBlock` 只有一处单串 `place` 调用，副手分支为拆串；独立盘子调用 `placePacked`。`SkewerPlateItem.isSkewer` 统一识别 SkewerItem、秘制串、生串和 grilled_skewers 标签，覆盖脚本及自定义烤串。 |
| 代码入口 | `mixin.GrillingSkewerPlacementMixin`、`mixin.GrillingSkewerHeatMixin`、`ICECoreMixinPlugin`；配置位于 `icecore.mixins.json` 通用侧列表。 |
| 实现链路 | 仅重定向事件中的单串 place 调用为 PASS，不取消整个右键事件。FoodState.setHot 对烤串提前返回，isHot 返回 false、remainingHeat 返回 0；已有 HotUntil 数据保留但失效，仍由上游过期清理。HotFoodHandler 唯一直接读取 HotUntil 的提示分支位于 isHot 门禁之后；GUI 徽标和食用结算也使用该门禁。无新增资源、配置、tick 扫描、背包遍历、网络或日志；热度路径每次仅执行上游有界类型/标签判断。 |
| 验证方法 | `GrillingSkewerRulesTest` 直接用 ASM 读取目标 JAR 核对描述符、唯一放置调用、两手拆串与独立盘子入口；源码契约覆盖烤串条件、旧热度查询、非烤串落回原方法、可选缺失门禁和禁止目标提前类加载。Forge 47.4.10 定向编译及测试通过，独立 clean 后完整 build 通过；生产 JAR 检查包含两个通用侧 Mixin、完整目标描述符及上游 isSkewer 调用，未打包上游类。CodeGraph 索引和 Git 忽略边界已核对。测试及构建不代替缺模组启动或真实交互验证。 |
| 实机待验证 | 主副手与连续 Shift 右键、独立盘子放置及已有摆盘取回；固定/自定义/脚本烤串烧烤完成及带旧热度存档；非烤串热食显示与加成、串制/调味/拆串、缺烟火模组启动。未部署或启动客户端。 |
| 最后修改 | 2026-10-15。 |

### 森罗物语烟火菜籽油副产物移除

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 制作菜籽油不再生成油渣，压榨输入、进度、产油量与容器规则保持上游行为；已有油渣不删除。 |
| 核对来源 | 客户端 `mods/kaleidoscope_grilling-1.1.1-forge1.20.1.jar`，mod ID `kaleidoscope_grilling`，Forge `[47.3.0,48)` / Minecraft `[1.20.1,1.20.2)`；字节码 `oil.OilPressBlockEntity.tryFinish()Z` 成功插入 4 桶油后构造 4 个 `ModItems.OIL_RESIDUE`，优先向 Create 输送带插入，剩余调用 `ejectResidue(int)`。 |
| 代码入口 | `mixin.GrillingOilPressMixin`；`ICECoreMixinPlugin` 只通过 class 资源探测可选目标，不加载目标类。 |
| 实现链路 | 精确重定向 `tryFinish()Z` 内唯一的 `ItemStack(ItemLike,int)` 构造为 `ItemStack.EMPTY`，在输送带插入和掉落实体之前阻止副产物生成；Create 空栈路径立即返回，无新增 tick 扫描、对象分配、网络或日志。外部方法未混淆，`remap=false`；默认 require=1，避免上游变化后静默失效。 |
| 资源位置 | `assets/kaleidoscope_grilling/lang/{zh_cn,en_us}.json` 覆盖真实 `hud.kaleidoscope_grilling.press.output` 键，只显示菜籽油产量；JEI 原类别不含油渣槽，无需修改。 |
| 验证方法 | `GrillingOilPressMixinTest` 检查完整生产选择器、空栈输出、配置注册及不提前定义目标类；基线 Forge 47.4.10 窄测试通过，单独 `clean` 后完整 `build` 通过，生产 JAR 注解描述符、SRG 空栈字段及语言资源检查通过，无打包上游类。ForgeGradle 6.0.54 连通性检查停在 TLS 握手，离线验证命令临时使用 `-Dnet.minecraftforge.gradle.check.certs=false --offline --no-daemon`，不修改持久设置。未启动客户端，手动/铁砧/Create 压榨及 HUD 实机回归待验证。 |
| 最后修改 | 2026-10-11。 |

### 森罗物语厨房汤锅盖直接切换与禁用盾牌

- 2026-10-16：主手为空时 Shift 右键汤锅直接切换有盖/无盖，允许副手持物；操作独占此次点击，不同时取内容。`kaleidoscope_cookery:stockpot_lid` 不再举盾、格挡或用于盖锅，开盖和破坏汤锅不生成锅盖物品。现有物品与配方保留，无配置或命令。
- 来源：`libs/` 中厨房 1.4.1 与客户端同版 1.5.1 的 `StockpotLidItem` 字节码均直接继承 `ShieldItem`，不覆写盾牌使用方法。Forge 47.4.10 映射及字节码确认 `use` 启动使用，`getUseAnimation` 返回 BLOCK，`getUseDuration` 返回 72000，`canPerformAction` 查询盾牌动作集合。
- 入口：通用侧 `mixin.CookeryStockpotLidMixin` 以继承覆写替换上述四个方法：use 返回 PASS、不启动使用；动画 NONE、时长 0、工具动作 false。构建重混淆生成 SRG 方法名；不在外部目标上注入不存在的方法。`ICECoreMixinPlugin` 通过 class 资源检测跳过缺失模组，不提前加载目标类。
- 方块入口：`compat.kaleidoscopecookery.StockpotInteractions` 在 Cookery 存在时注册 LOWEST RightClickBlock，尊重取消、DENY、旁观者与 mayInteract。主手空+蹲下命中 StockpotBlock 时两侧消费事件，副手重试不修改状态；仅服务端主手翻转 HAS_LID 并以 flags=3 同步，成功后播放上游 LANTERN_PLACE/BREAK 音效。普通右键进入上游汤底/食材/成品逻辑。
- 物品与旧数据：`CookeryStockpotEntityMixin` 在真实 `onLitClick(Level,LivingEntity,ItemStack)Z` HEAD 返回 false，阻止原装盖消耗及取盖发物品；生产 `m_142466_(CompoundTag)V` TAIL 清空 lidItem，不修改 HAS_LID。`CookeryStockpotDropsMixin` 重定向真实 getDrops 内唯一 HAS_LID getValue 查询为 false，精确跳过锅盖追加，保留汤锅与食材掉落。两版目标 StockpotBlock/Entity 字节码完全一致。
- 资源：`assets/kaleidoscope_cookery/lang/{zh_cn,en_us}.json` 覆盖真实 stockpot 和 stockpot_lid 提示键，说明空主手 Shift 开合及物品用途取消。长期操作说明维护于独立 Wiki《内容》。
- 性能：仅物品使用或能力查询时 O(1)，无 tick、背包遍历、反射、I/O、缓存、网络或日志；使用结果采用原版 PASS 包装，无持续动画状态。
- 验证：`CookeryStockpotLidMixinTest` 与 `CookeryStockpotRulesTest` 共 5 项，ASM 核对两版真实继承、交互/加载完整描述符、唯一盖子掉落查询及食材入口；检查双侧消费/主手服务端修改边界、生产选择器、旧数据清空、缺失门禁及 Forge RightClickBlock 的真实 Cancelable 注解。窄测试通过；最终构建及成品验证记录见下方。主副手、普通盾牌、普通右键取物、旧存档、破坏掉落与多人同步仍需实机复测，不启动或部署客户端。
- 最终验证：Forge 47.4.10 独立 clean 后完整 build 通过（163 项测试）；Forge 官方 promotions 最新 47.4.28 完整 build 通过，dependencies 确认解析 `1.20.1-47.4.28_mapped_official_1.20.1`，随后恢复 47.4.10 构建。生产 JAR 核对 `m_7203_`/`m_6164_`/`m_8105_` 覆写、Forge canPerformAction、两个新 Mixin 的完整选择器、HEAD/TAIL 与 lidItem Shadow；通用侧注册、双语键、展开元数据、无测试/上游类及 Git 忽略边界检查通过。CodeGraph watcher 已收录新增类。未部署、启动客户端或提交推送。

### 森罗物语厨房菜籽油汤底兼容

| 字段 | 内容 |
| --- | --- |
| 功能领域 | `kaleidoscope_grilling` 菜籽油桶与 `kaleidoscope_cookery` 汤锅底料兼容 |
| 玩家行为 | 同时安装两个森罗物语模组时，可将 `kaleidoscope_grilling:canola_oil_bucket` 作为汤锅底料投入；汤底注册名为 `icecore:canola_oil`。 |
| 代码入口 | `compat.kaleidoscopecookery.KaleidoscopeCookeryCompat`、`ICECore` 构造器公共初始化监听 |
| 实现链路 | ICEcore 在两个外部模组均加载时监听 `FMLCommonSetupEvent`，按 Forge 物品注册表查找菜籽油桶并确认其为 `BucketItem`，注册自定义 `FluidSoupBase` 汤底；厨房渲染器从桶对应流体的 `IClientFluidTypeExtensions` 读取 still 贴图和 tint，因此复用烧烤模组配置的原版水流体贴图与 `0xFFC08A24` 金黄色 tint，不复制外部资源。倒入和取出时先查询菜籽油 `FluidType` 的 `BUCKET_EMPTY/BUCKET_FILL` 声音；当前烧烤模组未提供，因此回退播放原版 `SoundEvents.BUCKET_EMPTY`/`BUCKET_FILL`，避免静音，上游未来补充声音后会自动优先使用。任一模组缺失或桶注册异常时跳过兼容。 |
| 验证方法 | Forge 47.4.10 执行 `gradlew.bat compileJava test --offline --no-daemon` 与 `gradlew.bat clean build --offline --no-daemon`；检查生产 JAR 仅包含兼容类，不包含厨房/烧烤类与流体贴图资源；未启动客户端，液面表现需实机验证。 |
| 最后修改 | 2026-09-25，新增菜籽油汤底兼容并确认复用烧烤流体 still/flowing 纹理与 tint；补充倒入和取出时的原版桶音效回退。 |

### 森罗物语油壶拾取与菜籽油装桶

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 放置的 `kaleidoscope_cookery:oil_pot`：蹲下、主手空手右键拾取整个油壶，空壶也可取回；普通空手取油及投油沿用上游。主手空桶右键菜籽油壶，每次消耗 8 点油与 1 个桶，获得 `kaleidoscope_grilling:canola_oil_bucket`；不足 8 点无变化，创造模式同样扣除。其它油种不扩展装桶。 |
| 核对来源 | Cookery 1.4.1 的 `OilPotBlock.getCloneItemStack` 保存油量，Grilling 1.1.1 的同入口 RETURN 注入保存油种；`TypedOilPotAccess` 提供油种/油量读写，菜籽油标识为 `canola`。`grilling$setOilCount` 调用上游 setOilCount 更新 HAS_OIL 并同步方块实体。参考烟火 JAR 位于 libs，SHA256 为 `8F13C33B0982E6D61344888B663E3FF7C949C0B7AF3BC63AF9257220F23A0D38`。 |
| 代码入口 | `compat.kaleidoscopecookery.OilPotInteractions`、`GrillingOilPotBuckets`，由 ICECore 在厨房存在时注册；烟火装桶类仅在烟火存在且持空桶时调用。编译依赖为 libs 中烟火 1.1.1 JAR，仅 compileOnly，不打包上游。 |
| 实现链路 | RightClickBlock LOWEST、不接收已取消事件，同时尊重 useBlock/useItem DENY、主手、旁观者和 mayInteract 限制。拾取在服务端先复制完整上游 ItemStack，再无掉落替换为空气/水，成功后交到主手；装桶在服务端检查油种/余量/注册物品，扣油、扣桶、交付成品；堆叠空桶的成品优先入背包，满背包则掉落。不足 8 点的菜籽油点击也被消费，防止副手重试。 |
| 性能 | 仅右键事件触发，非目标快速返回；油种与余量检查 O(1)，无 tick 扫描、反射、同步 I/O 或日志。仅成功拾取创建油壶栈，成功装桶创建成品栈；入背包最多扫描固定玩家槽位，网络沿用方块和背包同步，无新增数据包。 |
| 验证方法 | `OilPotInteractionsTest` 的四项源码契约测试通过，覆盖可取消事件、权限/单手门禁、服务端结算、含水恢复、先移除再交付、扣除及满背包回退、可选依赖隔离。Forge 47.4.10 单独 clean 后完整 build 通过，128 项测试无失败；生产 JAR 包含交互类、setBlock 为 SRG `m_7731_`，未打包厨房/烟火上游类。当前最新 Forge 47.4.28 compileJava 通过，日志确认实际解析该版本的映射依赖，随后逐字节恢复 gradle.properties 的 47.4.10 基线并重新 build。CodeGraph 已更新，UTF-8 与 Git 忽略边界检查通过。源码测试不构造未引导的 Minecraft 注册表，不代替真实交互测试。 |
| 实机待验证 | 空壶、普通油与各烟火油取回再放置；菜籽油 0/7/8/9/16 点、单桶/堆叠/满背包/创造模式、副手与连续点击、多人竞争；普通取油/投油和权限拦截；缺厨房、仅厨房、厨房加烟火的启动。未启动或部署客户端。 |
| 最后修改 | 2026-10-14；稳定使用说明同步 Wiki 内容页。 |

### 冒险模式建造系统

- 承托依赖保护验证结果：8项定向测试通过；Forge 47.4.10 独立clean后完整build成功，verifyProductionJar通过，成品JAR的BuildingRules/BuildingServerActions与中英文提示已核对，UTF-8和Git忽略边界通过，CodeGraph已索引两个hasDependents入口。clean首轮停在ForgeGradle远程证书握手，结束本次进程后使用文档列出的离线证书检查参数重试成功，未改项目校验。未启动或部署客户端；真实标签依赖与主副手运行验收仍待授权。共享工作区另有交互改动，完整构建验证的是当时整体状态。

- 2026-10-17 扳手承托依赖保护：`BuildingRules.hasDependents` 归一化目标并通过 `BuildingStructure` 获取全结构，每格检查六邻格、排除内部格；按当前有效列表的 `supports` 与目标朝外的 `allowedFaces` 判断，任一匹配即禁止拆除，不检查依赖方块区域或其它承托。`BuildingServerActions.remove` 在权限/距离后、内容确认和背包检查前执行，拒绝时清除强拆确认并通过 `PlayerFeedback` 显示 `icecore.build.block_depended_on`，双语资源同步。仅冒险模式扳手生效，Shift 不绕过；交互认领与客户端预览保持原入口。每次请求复杂度 O(S×6×L×E)，S 为结构格数、L 为列表数、E 为列表条目匹配成本；内部格排除 O(S)，当前结构有界为 1–2 格，命中立即退出，只有局部坐标及一次回调分配，无 tick 扫描、I/O、持久化关系或新增网络。验证入口：`BuildingDependencyTest` 的邻格方向/多格/短路行为测试和规则契约，`BuildingInteractionContractTest` 的失败顺序、确认清理、认领及双语检查；普通 JUnit 不初始化 Minecraft 方块注册表，真实规则与主副手交互实机待授权。

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 冒险模式放置、破坏与开发者规则 |
| 玩家行为 | 冒险模式可在有效区域、承托列表和方向限制均满足时放置受管方块；手持扳手可拆除属于有效列表的受管方块，列表配置区域时还需位于该区域内；客户端显示半透明成功/深红失败预览；非空容器普通右键禁止拆除，两次 Shift+右键确认后会清空内容并只返还方块；扳手对着拆除目标右键时，即使最终拆除条件不满足，也不会触发门开关等目标方块交互；建造系统产生的即时反馈统一显示在原版底部信息栏。 |
| 配置入口 | 世界 `data/icecore_building.json`；管理员命令 `/icecore build ...`，权限等级 2 |
| 代码入口 | `building.BuildingDataManager`、`BuildingRuntimeCache`、`BuildingCommands`、`BuildingEvents`、`BuildingServerActions`、`BuildingStructure`、`BuildingClientRenderState`、`building.render` 下的网格交接接口、`item.ModItems`、`building.BuildingClientEvents`、`mixin.MultiPlayerGameModeMixin`、`mixin.ClientBlockStateUseMixin`、`mixin.BlockStateUseMixin`、`mixin.RenderChunkRegionMixin`、`mixin.ChunkRebuildTaskMixin`、`mixin.EmbeddiumWorldSliceMixin`、`mixin.EmbeddiumChunkBuildOutputMixin`、`mixin.EmbeddiumRenderSectionMixin`、`mixin.EmbeddiumRenderRegionManagerMixin` |
| 实现链路 | 世界生命周期加载并缓存规则 → 命令复制、校验并原子保存 → 服务端在冒险模式 `RightClickBlock`/Forge 放置流程中重算区域、承托、方向、距离和多格放置，并逐一确认结构占用位置可替换且无遮挡 → 扳手拆除直接从权威规则数据匹配有效方块列表，检查区域（若配置）与距离，再模拟背包容量并整体处理结构位置；服务端 `RightClickBlock` 与扳手物品 `useOn` 共用该拆除入口 → 规则快照同步到客户端用于预览；客户端使用与服务端共享的状态属性结构解析，先归一化关键位置，再为所有去重后的占用位置渲染顶点着色半透明模型；成功预览保留原纹理，放置失败预览与成功预览共用原版 `RenderType.translucent()` 批次，仅通过顶点颜色改为深红；扳手拆除预览在 `AFTER_PARTICLES` 阶段由 `renderWrenchPreviewModel` 直接复用放置失败预览的 `RenderType.translucent()` 批次、顶点深红颜色和透明度，在真实表面深度与目标背后的真实地形混合，实际视觉与放置失败预览完全一致；原方块由 `RenderChunkRegionMixin`/Embeddium `WorldSlice` Mixin 从异步重建的区块网格移除，稳态下不进入新网格，不再需要额外的深度遮罩；不使用只写深度加负多边形偏移的遮罩，因为该遮罩会把目标背后的地形一并按深度剔除，导致半透明模型只能与被清空的天空色混合（表现为整块不透明的天空色）；代价是准星切到新目标后、异步区块重建完成前的 1~2 帧内原方块会短暂带红显示，随后由隐藏坐标从网格移除即恢复；异步重建时由 `RenderChunkRegionMixin` 和 Embeddium `WorldSlice` Mixin 读取隐藏坐标并从新网格移除原方块；隐藏坐标快照由 `onRender`（`AFTER_PARTICLES`）逐帧调用 `refreshHiddenRenderState` 用当帧命中目标更新，与预览同源同帧，避免客户端 tick（20Hz）与渲染（60Hz+）错拍导致切换目标时旧方块短暂变成缺口；`BuildingClientRenderState.update` 在坐标集合不变时提前返回，静止准星不标脏；放置受管方块时 `onRightClick` 先用共享的 `computePlacementPreview` 判定该次放置是否有效(`allowed`)：仅在有效时才启动放置抑制窗口(`suppressPlacementPreviewTicks`)并同步 `BuildingClientRenderState.clear()` 解除放置位置隐藏；抑制窗口只用于遮住服务端确认/背包同步在途时停留在刚放位置的幻影预览，放置失败时服务端不放置任何方块、手持栈不变、无幻影可遮，若仍启动抑制只会让红色失败预览黑屏约 150ms，故失败时不启动抑制，红色失败预览保持常显；放置成功才短暂抑制一次幻影；`onRender`、`refreshHiddenRenderState` 与 `onRightClick` 共用 `computePlacementPreview` 计算归一化关键位置、结构状态与放置有效性，三者对同一目标和有效性判定一致，且每帧不再重复计算多次；放置预览和扳手预览均在各自批次结束时提交且不绘制边框；扳手命中受管拆除目标时取消客户端 `RightClickBlock`，`MultiPlayerGameModeMixin` 和客户端 `BlockState.use` 入口均再次硬拦截本地方块交互，同时保留外层 `useItemOn` 创建的 `ServerboundUseItemOnPacket`；服务端 `BlockState.use` 入口也按受管目标硬拦截，因此背包已满、距离、区域或其它拆除校验失败时仍只显示拆除结果，不会触发门开关等目标方块交互；所有扳手拦截判定均为手部无关（判定"任一手持扳手且准星指向受管目标"而非当前交互手），因为 `Minecraft.startUseItem` 在主手扳手返回 `PASS` 后会用副手重试 `useItemOn`，只按当前手判定会让副手/空手的重试触发门等方块 `BlockState.use`；开发者调整模式保留客户端左右键数据包链，生存/冒险使用结果标记、创造模式取消本地左键但保留 Forge 数据包，以阻止本地交互并由服务端最高优先级记录坐标、取消实际交互，客户端绘制无深度遮挡的三维线框边界。 |
| 拆除预览事务 | 本项替代上方实现链路中“异步重建期间允许短暂带红/轻微过渡”的旧取舍：拆除目标进入、切换与离开改为按 section 的两阶段异步事务。`RenderChunkRegionMixin` 与 Embeddium `WorldSlice` Mixin 读取任务级不可变隐藏快照；Vanilla 重建任务在该任务自身的全部上传 future 成功完成、编译结果已写入且 section 已加入最近编译队列后，直接用任务快照提交对应 section，不经共享 RenderChunk 中转代次；Embeddium 实际网格上传完成回调逐 section 更新覆盖关系。新目标 section 隐藏后才增加对应预览格，旧目标 section 恢复后才撤下对应旧预览格；跨 section 多格结构也逐格保持“原网格或预览恰好一个”，不再存在允许闪烁的过渡窗口。供普通代码强转的 `PreviewMeshSnapshotHolder`、`EmbeddiumBuildOutputExtension` 与 `EmbeddiumRenderSectionExtension` 固定放在 `building.render`，不放入 `icecore.mixins.json` 声明的 Mixin 包，避免 Mixin 包所有权校验禁止直接加载。 |
| 资源位置 | `assets/icecore/models/item/wrench.json`、`assets/icecore/textures/item/wrench.png`（使用用户提供的 `bs.png` 16×16 贴图）、`assets/icecore/lang/zh_cn.json`、`assets/icecore/lang/en_us.json` |
| 详细说明 | [冒险模式建造系统](adventure-building-system.md) |
| 反馈显示 | 建造系统玩家反馈统一由 `feedback.PlayerFeedback` 路由到底部信息栏；原版通道保持 40 tick 显示并在随后 20 tick 淡出。 |
| 本次变更 | 2026-08-25，拆除预览切换改为代次化、按 section 提交的两阶段异步网格事务：进入目标时先等待目标 section 的隐藏网格完成再显示对应预览格；跨目标切换时先让新目标隐藏网格就绪并增加新预览，再恢复旧目标网格并按 section 移除旧预览；离开目标时保持旧预览直到恢复网格上传完成。Vanilla 在网格上传、可绘制元数据和方块实体集合完成后由该重建任务自身的成功完成路径提交，Embeddium 0.3.31 通过实际过滤后的 `RenderRegionManager.uploadMeshes` 完成回调提交；每个构建任务捕获不可变隐藏快照，快速扫动只串行处理最新排队目标，不同步重建区块。由此任意一帧、任意结构格都恰好由原网格或深红预览覆盖，不再出现缺口、双模型或颜色闪变。建造系统及其它 ICEcore 玩家即时提示继续由统一反馈入口发送到底部信息栏；控制台命令输出保持原行为。修复首次进入世界加载 `RenderChunkMixin` 时的 `IllegalClassLoadError`：普通桥接接口已迁出受 `icecore.mixins.json` 管理的 Mixin 包；随后依据目标 Forge 47.4.22 生产 JAR 将 `ChunkRebuildTaskMixin` 的两个 Shadow 字段改为生产 SRG 名，并保留开发名 aliases，同时移除共享 RenderChunk 代次中转，避免无 refmap 加载失败及并发重建覆盖任务代次。 |
| 验证方法 | Forge 47.4.10 执行 `gradlew.bat compileJava`、`processResources --rerun-tasks`、`clean build`；使用基线与目标实例 Forge 47.x 客户端 JAR 的 `javap` 核对 `MultiPlayerGameMode.m_233746_` 和 `BlockBehaviour.BlockStateBase.m_60664_` 的完整描述符；检查最终 JAR 的 Mixin 配置、Manifest 和目标类；检查 JAR 资源和世界数据原子保存；核对 `/icecore build list block <列表> add <注册名或#标签>` 的注册名解析、补全和执行；核对有效/无效放置限制、完整多格结构预览、放置同步延迟抑制、区域内/区域外扳手预览与拆除判定，并确认扳手覆盖旧区块网格时不出现原方块与预览的双重模型；客户端实机需分别验证拆除成功、背包已满、距离不足和区域外，四种场景右键均不触发门/容器交互。 |
| 最后修改 | 2026-08-22，修复放置方块后预览消失（失败时最明显）：`onRightClick` 原对任意受管方块放置无条件启动 3 tick 抑制窗口，失败放置时服务端不放置、手持栈不变，`isPlacementPreviewSuppressed` 整窗返回 true，把红色失败预览强行隐藏约 150ms；改为先用共享 `computePlacementPreview` 判定 `allowed`，仅有效放置才启动抑制并同步解除隐藏，失败放置不启动抑制、红色预览常显；同时抽出 `computePlacementPreview` 供 `onRender`/`refreshHiddenRenderState`/`onRightClick` 共用，消除每帧重复计算并统一目标与有效性判定。拆除目标切换的缺口闪烁属隐藏网格异步重建的固有延迟：已用 1.20.1 基线源码确认 `LevelRenderer.setBlocksDirty`→`ViewArea.setDirty` 的 `important` 参数即 `reRenderOnMainThread`（主线程同步重建，即此前因准星移动卡顿而移除的做法），保留透地形前提下仅同步重建可消除，属已知取舍未启用。同日早前缓解拆除目标切换与放置方块后预览的瞬间闪烁：根因是隐藏坐标快照原在客户端 tick（20Hz）更新而预览逐帧（60Hz+）渲染的错拍——切换拆除目标时某帧准星已到新目标但隐藏集仍指向旧目标，旧方块被从网格移除却无预览覆盖而短暂变成缺口最晃眼；放置方块时放置位置仍留在隐藏集，原版放置后的区块重建把新方块以隐藏状态重建（先不可见），下一 tick 清除隐藏集才触发第二次重建弹出。经用户确认采用方案1：将 `refreshHiddenRenderState` 从 `onClientTick` 移到 `onRender`（`AFTER_PARTICLES`、去重之后）逐帧执行，用当帧命中目标与预览同源同帧更新隐藏集，`update` 在坐标不变时提前返回不标脏；`onRightClick` 检测到受管方块放置时同步 `BuildingClientRenderState.clear()`，赶在原版放置后区块重建之前解除隐藏。残留仅异步区块重建本身的几帧轻微过渡（用户已接受，未采用会引入准星移动卡顿的同步重建方案2）。同日早前修复扳手拆除预览整体渲染成不透明天空色：原实现用 `AFTER_SKY` 深度遮罩（`polygonOffset(-1,-10)` 只写深度）在固体区块层前占用目标深度以遮蔽原方块，但该遮罩会把目标背后的真实地形（地面、相邻方块）一并按 `lequal` 深度测试剔除，使 `AFTER_PARTICLES` 的半透明模型只能与被清空的天空色混合，表现为整块不透明天空色；经用户确认，移除深度遮罩及其 `WRENCH_PREVIEW_TRANSPARENCY`/`WRENCH_PREVIEW_LAYERING`/`WRENCH_PREVIEW_DEPTH_MASK_RENDER_TYPE`/`WRENCH_PREVIEW_MODEL_RENDER_TYPE` 常量、`renderWrenchPreviewDepthMask`/`renderDepthMask` 方法与 `AFTER_SKY` 分支，`renderWrenchPreviewModel` 直接复用放置失败预览的 `RenderType.translucent()` 在真实深度与真实地形混合，视觉与放置失败预览一致；原方块仍由 `RenderChunkRegionMixin` 从异步区块网格移除，稳态不重叠，代价是准星切到新目标后异步重建的 1~2 帧内原方块短暂带红显示；同日早前将扳手右键的全部拦截判定改为手部无关：`Minecraft.startUseItem` 的双手循环在主手扳手返回 `PASS` 后会用副手重试 `useItemOn`，原判定只看当前交互手，导致副手/空手重试仍调用门等方块 `BlockState.use`；现客户端 `shouldBlockWrenchBlockUse`、Forge `RightClickBlock` 处理器、服务端 `shouldBlockVanillaBlockUse` 与服务端 `RightClickBlock` 处理器统一判定“任一手持扳手且准星指向受管目标”，`remove` 仍只由持扳手手触发；新增服务端 `holdsWrench`/`isManagedTarget` 供两端复用；同日早前修正预览批次生命周期：成功与失败预览统一写入独立单层 `RenderType.translucent()` 缓冲并各自只提交一次，避免失败预览在空位置也出现重复模型；移除准星移动路径上的同步区块编译，保留按坐标变化的异步区块标记和原方块/方块实体隐藏；扳手拆除预览改为“前置深度遮罩 + 标准失败预览”两阶段：`AFTER_SKY` 固体区块层之前的专用批次只写深度并隐藏原方块，`AFTER_PARTICLES` 直接使用放置失败预览的 `RenderType.translucent()`、深红顶点颜色和透明度，确保两者视觉实际效果一致；继续由异步重建隐藏状态清理新网格中的原方块；扳手命中受管目标时取消客户端 `RightClickBlock`，并通过 `MultiPlayerGameModeMixin`、客户端 `BlockState.use` Mixin 和服务端 `BlockState.use` Mixin 三层最终拦截，保留拆除请求包，避免拆除失败时门等方块仍被开关；修正生产 JAR 的 Mixin 目标，按 Forge 1.20.1 实际 SRG 方法名和描述符注入 `BlockEntityRenderDispatcher`、`RenderChunkRegion`、`MultiPlayerGameMode` 和两个 `BlockState.use` 入口，并在 JAR 检查中确认 Manifest、Mixin 配置、目标字符串和新增类一致。 |

### 冒险模式禁丢弃与玩家给予

| 字段 | 内容 |
| --- | --- |
| 玩家行为 | 冒险模式禁止 Q/Ctrl+Q、容器界外点击、槽位 THROW 和关闭界面 carried stack 掉落；准星命中玩家、主手非空且未打开界面时，通过可选 SmartKeyPrompts 显示实际丢弃键与“给予”（未安装时不显示提示），按下给予单个、Ctrl+Q 尽量给予整组。 |
| 代码入口 | `adventure.AdventureItemService`、`AdventureItemEvents`、`compat.smartkeyprompts.AdventureGivePrompt`、`AdventureItemServerEvents`、`network.ServerBoundGiveItemPacket`、`mixin.AdventureDropPacketMixin`、`mixin.AdventureCarriedStackMixin` |
| 实现链路 | 客户端在 `Minecraft.handleKeybinds` 的生产入口先接管 Q，给予请求才发送到服务端；容器界外点击、THROW 和界面内 Q 均在客户端与服务端双重阻断，避免原版丢弃动画；服务端复核冒险模式、同维度、存活、视线与距离后按目标背包容量转移；容器 carried stack 由世界 SavedData 暂存，关闭菜单后在 `ServerPlayer.doCloseContainer` 完成时恢复到 `inventoryMenu`；打开箱子等新容器时，Open 事件把 carried 从 `inventoryMenu` 原子转移到新 `containerMenu` 并同步客户端；登录时也恢复，不调用原版掉落分支。 |
| 性能与生命周期 | Q 长按每 4 客户端 tick 最多一个请求；目标与容量仅在请求时计算；待恢复物品按玩家 UUID 保存，不进行每 tick 全量扫描。 |
| 验证方法 | `gradlew.bat compileJava`、`gradlew.bat test`、生产 Mixin 选择器测试和基线 Forge 47.4.10 字节码核对。 |

- SmartKeyPrompts 兼容：以客户端 `smartkeyprompts-forge-1.20.1-1.1.3-hotfix-all.jar` 为事实来源，复制至忽略的 `libs/`，仅 compileOnly/testRuntimeOnly，不包入生产 JAR。客户端加载时缓存模组存在性，仅受保护分支访问桥接类；END 提交、外部 HUD 下一 START 消费并清空。分组 `icecore_adventure_give`，使用真实 `keyDrop.getKey().getName()`；固定 O(1) 条件检查，不产生网络请求。单个提示对象复用，改键时新建以避免修改仍处于外部集合中的哈希键；无玩家、界面打开、空手或目标失效时停止提交，下一 START 移除。
- KubeJS 自定义提示编写方法统一见 [SmartKeyPrompts 的 KubeJS 自定义提示](smartkeyprompts-kubejs.md)：目标插件公开绑定、启动 END 事件转发、实际按键读取、缓存及翻译；错误证据保留于 ERR-20260821-01。
- 2026-10-05 已按用户要求移除客户端 `client_scripts/smartkeyprompts_use_test.js`、`startup_scripts/smartkeyprompts_use_test_events.js` 及 `icecore.test_prompt.use` 中英文测试翻译。清理后重启客户端解除内存中的启动监听与回调，正式给予提示兼容保留。
- 验证：Forge 47.4.10 完整构建与 113 项测试通过，生产 JAR 不包含测试脚本/翻译；用户已确认正式 Q 提示生效。临时脚本删除与翻译 JSON 检查通过；历史真实 Rhino 解释执行验证与未完成的 HUD 实机边界见专题文档。
### JEI 配方隐藏与服务端合成禁止

| 字段 | 内容 |
| --- | --- |
| 功能领域 | 按配方/按物品隐藏 JEI 展示并在服务端真正禁止合成 |
| 玩家行为 | OP 用 `/icecore recipe hide all\|show all` 隐藏/显示全部配方；`hide item <物品>\|show item <物品>` 隐藏/解锁某物品的所有产出配方；`exempt on\|off\|toggle` 进入/退出豁免模式（仅执行者客户端：无视 JEI 隐藏、全部配方物品正常显示可查看可搜，服务端合成禁止仍生效）。隐藏后该配方在 JEI 搜不到看不到、收藏夹对应项失效，且工作台/熔炉族/切石机/锻造台等原版合成被真正禁止；按物品隐藏时该物品从 JEI 物品列表消失，解锁时恢复；无可解锁配方的物品仅切换其显示（不报“无配方”错误）。被隐藏物品所需的工作台/熔炉等催化剂物品也一并从 JEI 物品列表消失，但未隐藏物品的配方仍可正常点击/快捷键查看。一键隐藏（hide all）后仍可对单个物品 `show item` 单独解锁，其配方恢复可见与可合成。 |
| 启用条件 | 服务端合成禁止与名单持久化随 ICEcore 生效；JEI 侧隐藏在安装 JEI 15.49 时自动启用，缺少 JEI 时只保留服务端禁止与名单。命令默认权限等级 2（继承 `/icecore` 根门控）。 |
| 覆盖边界 | 覆盖所有注册到原版 `RecipeManager` 且配方对象为 `Recipe<?>`（有 `ResourceLocation` id）的配方，含绝大多数模组自定义 `RecipeType`。已知盲区（默认不处理）：① 不走 `RecipeManager` 的自有/运行时/硬编码合成，`byType` 拦不到；② JEI 中以非 `Recipe` 对象注册的类目按 id 匹配不到。遇具体模组再单独加兼容，不做通用臆测钩子。 |
| 代码入口 | `recipehide.RecipeHideRegistry`（双端静态过滤器）、`RecipeHideSavedData`（`FILE_ID=icecore_recipe_hide`，字段 `hideAll`/`forcedHiddenItems`/`forcedShownItems`）、`RecipeHideService`（服务端权威、物品→配方解析、有效集计算、同步）、`RecipeHideCommands`（`attach` 到 `icecore` 根）、`RecipeHideEvents`（ServerStarting/Stopping + `OnDatapackSyncEvent`）、`mixin.RecipeManagerMixin`；客户端 `recipehide.client.RecipeHideClientState`/`RecipeHideJeiApplier`/`RecipeHideJeiPlugin`（独立 UID `icecore:recipe_hide`）/`RecipeHideCatalystVisibility`（被隐藏催化剂物品集）、`mixin.JeiRecipeManagerInternalMixin`（client、`remap=false`）；网络 `network.ClientBoundRecipeHidePacket`（PLAY_TO_CLIENT，id 19）。 |
| 数据模型 | 服务端存 `hideAll`（基线）+ `forcedHiddenItems`/`forcedShownItems`（单物品覆盖，互斥）。统一语义：物品/配方是否隐藏 = 在 forcedHiddenItems→隐藏；在 forcedShownItems→显示；否则随 hideAll。**hide all = 对所有物品执行与单独隐藏物品相同的逻辑**（隐藏配方 + 从 JEI 物品列表移除），`show item X` 即把 X 从全隐中豁免。`RecipeHideRegistry` 用三态：NONE / ALL（id 集为豁免）/ PARTIAL（id 集为隐藏），避免全隐时枚举全部配方 id。 |
| 实现链路 | 命令 → `RecipeHideService` 更新 SavedData → `recomputeRegistry`（hideAll→Mode.ALL+豁免物品的产出配方 id；否则 Mode.PARTIAL+隐藏物品的产出配方 id）→ `broadcast` 全量包（`hideAll` + recipeIds（豁免或隐藏集）+ shownItemIds + hiddenItemIds）。`RecipeManagerMixin` 用核心 `@Inject(at=RETURN, cancellable)` 过滤 `byType`(SRG `m_44054_`)、`byKey`(`m_44043_`)，两端生效；**`getRecipes` 不过滤**。客户端收包 → `RecipeHideClientState.accept` 更新客户端 `RecipeHideRegistry` + `RecipeHideJeiApplier.reapply()`。**applier 每次对当前 runtime 全量重算到目标态、不跨 runtime 记账**（`onRuntimeAvailable` 换 runtime 即清物品记账；配方轴用当前 runtime 刚取的实例显式双向 `hideRecipes`/`unhideRecipes`，彻底解决 JEI 重载后实例失配导致"豁免配方点不开"；物品/流体逐 `getRegisteredIngredientTypes()` 类型处理；不吞异常改 `LOGGER.warn`，空集合先判空避免 `checkNotEmpty` 抛错破坏搜索索引）。豁免模式为纯客户端开关（`RecipeHideClientState.exemptMode`），开启走"全显"分支（全部 `unhideRecipes` + 物品加回、不清收藏），服务端合成禁止不受影响；指令经 `ClientBoundRecipeExemptPacket`(id 20) 发给执行者。JEI 配方隐藏按实例 `==`（IdentityHashMap）判定，实例取自当前 runtime 的 `createRecipeLookup().get()`，故必须用当前 runtime 实例、不能跨 runtime。**从不用 `hideRecipeCategory`**（JEI 字节码确认整类隐藏与逐条隐藏独立层，`unhideRecipes` 无法给整类挖洞）。登录经 `OnDatapackSyncEvent`（带玩家）单发；`/reload`（玩家 null）重算并广播。 |
| 催化剂可见性 | 催化剂物品（工作台/熔炉等）按普通隐藏物品一样 `removeIngredientsAtRuntime` 移除，从 JEI 物品列表消失。但 `removeIngredientsAtRuntime` 经 `IngredientBlacklistInternal`（注册为 ingredient listener）把 uid 拉黑，使物品在**所有 UidContext** 下 `isIngredientVisible` 返回 false。JEI 15.49.0.188 `RecipeManagerInternal.isCategoryHidden`（jar 字节码确认）第②道判定为「类目有催化剂（`getRecipeCatalystStream(type,true)` 非空）**且**可见催化剂为空（`getRecipeCatalystStream(type,false)` 空）→ 整类判 hidden」；类目被判 hidden 后 `getRecipeCategoriesUncached` 的 `.filter(!isCategoryHidden)` 把类目滤空，点产物时 `RecipeGuiLogic.setState` 因类目空静默 `return false`，配方 GUI 不开（键鼠皆不可查看）。该版本 `IIngredientVisibility` **只有** `isIngredientVisible`/`registerListener`，**无** per-context `hideIngredients`/`unhideIngredients`（属更新分支），故"列表隐藏但配方可见"无法靠 API 实现。解法：`JeiRecipeManagerInternalMixin` `@Redirect` `isCategoryHidden` 内 **ordinal=1** 的 `getRecipeCatalystStream(type,false)` 调用，把 `RecipeHideCatalystVisibility.isHidden(item)` 命中的催化剂从全催化剂流筛出 concat 回可见流，使第②道判定不再因它们把类目判 hidden，配方保持可查看；`RecipeHideJeiApplier.applyItems` 收集被移除的催化剂物品发布给 `RecipeHideCatalystVisibility`，集合空时 mixin 直接返回原流=原版行为。mixin 由 `ICECoreMixinPlugin`（`mixinClassName.contains("Jei")`→查 `mezz.jei.library.recipes.RecipeManagerInternal` 是否存在）在无 JEI 时跳过。 |
| 收藏夹清除 | JEI 的 hide/remove 都不触碰收藏夹，且无公开清除 API。`RecipeHideBookmarks` 反射 `BookmarkOverlay` 私有字段 `bookmarkList` 拿到内部 `mezz.jei.gui.bookmarks.BookmarkList`（public），物品收藏遍历 `getElements()` 按 `Item` 匹配、配方收藏用 `getMatchingBookmark(type, recipe)`，命中调 public `remove(IBookmark)`（内部 `saveBookmarks` 持久化）。为引用 JEI 内部类，`build.gradle` 给 forge jar 加 `compileOnly`。全程 try/catch，失败只是不清、不崩。 |
| 性能与生命周期 | `RecipeHideRegistry` 过滤在合成高频路径上：空名单引用相等返回原对象（零分配），非命中也返回原对象，仅真命中才拷贝；`hideAllFast` 走 `Map.of()`。物品→配方遍历只在命令/重载时执行一次；`getResultItem` 带 try/catch 与 null/empty 兜底。JEI 枚举仅在状态变更/runtime 可用时执行，非每帧。 |
| 已知限制 | JEI 无公开清除书签 API（`IBookmarkOverlay` 仅只读）；隐藏依赖移除物品/隐藏配方后 JEI 书签自动失效，属实机验证行为，非 API 保证。 |
| 验证方法 | Java 17 执行 `gradlew.bat clean build`（Forge 47.4.10）；`RecipeManagerMixinProductionSelectorTest`（断言只剩 `m_44054_`/`m_44043_`、无 `m_44051_`）与全局 `MixinProductionNamingPolicyTest` 校验 SRG 名+完整描述符；`javap` 确认 reobf 生产 JAR 注解常量池只含 byType/byKey 两选择器。`JeiRecipeManagerInternalMixin` 用 `remap=false`+完整方法描述符定位 `isCategoryHidden` 的 `getRecipeCatalystStream(...Z)` ordinal=1 调用，reobf 后 JEI 类不再混淆需 build 期验证 `@Redirect` 目标可解析。实机（装 JEI 的客户端）验证：hide all 后 `show item` 单独恢复配方（可见+可合成，反馈显示配方数不再“无配方”，且可点击/快捷键查看）、hide/show item 使物品从 JEI 消失/恢复且搜不到、被隐藏物品的催化剂（工作台/熔炉等）从 JEI 消失但未隐藏物品配方仍可查看、无配方物品仅切换显示、不再刷屏 PFM broken recipe、退出重进持久化、多人同步、`/reload` 覆盖新配方。 |



### 工作方块统一取回顺序（2026-10-01）

- `WorkstationItemOrder` 统一最高序号选槽、取出条件及序号恢复；`WorkstationContainerInteraction.canTake` 对接精确容器匹配。烤炉和混合碗先选顶端再判断，提示复用同一顶端，烤炉成品沿用生成序号。
- 保留既有 NBT；清槽清序号，补位获新序号。缺失、重复、非正或不可递增序号按槽号重建，空槽归零，重载保持有效顺序。
- 混合碗真实流体优先，其后顶端食材、流体失败拦截和默认投料不变；烤炉没有流体槽。
- 性能：交互扫描最多九格，加载最多九格两层校验，无新增 tick 扫描、网络包或配方字段。
- 验证：共享纯逻辑回归、两个交互入口源码契约及 Forge 47.4.10 独立 clean 后 test/build 全部通过；客户端流体不足、不兼容、输入/成品混存、音效与保存重载实测待执行。

### 建造右键归属修正（2026-10-15）

- BuildingServerActions.preparePlacement 返回原始放置上下文或 null，无世界修改；executePlacement 保留 Forge 事务及 mayBuild 的 finally 恢复。BuildingEvents 在执行前用弱引用事件集合记录归属，LOWEST 移除并重新取消，避免状态/栈变化使放置锁失效。
- MultiPlayerGameModeMixin 在生产 m_233746_ 内部预测入口返回 SUCCESS，停止双手重试，外层仍创建请求包；claimPlacement 复用预览合法性、距离和调整模式排除，仅有效时启动预览抑制。无新增协议、tick 扫描或日志。
- 扳手客户端与服务端统一按归一化主位置 canBreak 判定交互归属，非列表/区域外放行；背包和容器确认失败继续独占。
- 验证：BuildingInteractionContractTest 固定先声明再修改、末尾重申、内部预测选择器与 SUCCESS、区域权限边界及副手扳手推进；Forge 47.4.10 定向测试、生产命名测试及独立 clean 后完整 build 通过。实机熔炉/箱子/门、双手、多格与事务拒绝仍需用户授权。

### 咖啡馆电脑提示框恢复验收（2026-10-17）

- 保留Open按钮上方居中、6秒总时长和末1秒淡出；恢复原生Tooltip背景与渐变边框，两者透明度同步。Forge47.4.10和47.4.28完整build及verifyProductionJar通过，最终恢复基线47.4.10；177项测试零失败。最新产物SHA-256为ef0ddf124a46b9a4fa194bded720b57f651af64ce990032c52f7cbfee7b8716d，替代上述历史产物。未部署或启动客户端，实机画面待验。

- 历史“提示框恢复验收”的低层TooltipRenderUtil方案已被本轮完整原生入口替代；旧产物hash仅为历史记录，不代表当前兼容实现。

- 2026-10-17完整原生Tooltip最终验证：177项测试零失败；基线Forge47.4.10独立clean后完整build、47.4.28完整build及最终47.4.10恢复build均通过，verifyProductionJar通过。成品不打包Modern UI/CozyCafe类，SHA-256为60ff45035966cb6e21329e1a0d5db47876164a7d9f324e999a89cec7d07b4984；原生入口和资源清理已核对。截图场景仍待用户实机复测，未部署或启动客户端。

### 冒险模式普通交互限制（2026-10-17）

- 支持全局JSON黑名单/白名单：精确方块、方块标签、来源命名空间、来源与标签交集；坐标特例按世界保存并绑定ID。默认空列表，仅冒险普通右键受限，有效建造和扳手、调整保留。
- 入口、协议11/包23、加载/原子保存/缓存/两端拦截及性能边界见[冒险模式建造系统](adventure-building-system.md#冒险模式普通交互限制)。命令 `/icecore interaction` 权限2，成功操作立即保存同步，reload失败不替换旧规则；标签随原版/reload更新。
- 使用文档：[冒险模式交互限制](../wiki/冒险模式交互限制.md)。配置/选择器纯逻辑、坐标维度/ID、原子替换失败、双手/事务/同步契约由InteractionConfigTest、InteractionIntegrationTest及现有建造测试覆盖；实机未启动。

- 验证补充：最终全量198项测试零失败，新交互测试共10项，包含ASM直接读取当前Minecraft依赖的内部预测/Forge事件入口与BlockState.use完整描述符；客户端快照旧版本拒收及断线后低版本重新接收使用真实纯状态测试。Forge 47.4.10与47.4.28完整build及生产映射检查通过；客户端未启动、未部署JAR。

- 移除命令补全只读取当前黑/白名单对应规则类型；modtag先补全已有交集规则的来源，再按所选来源补全其标签，保留未加载ID/标签以便删除。添加仍按注册表补全；按请求O(R)过滤去重，无tick扫描或额外网络，InteractionConfigTest覆盖列表隔离、类型隔离、交集第二参数、未知项和删除后刷新。

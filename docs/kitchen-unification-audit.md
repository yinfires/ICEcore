# 厨具统一：雪克杯与料理台静态核对

日期：2026-09-09。用途：决定配方迁移与兼容开发边界。本文是设计审计，不是已实现功能或游戏验收报告。

## 依据与范围

- `libs/kaleidoscopetavern-1.2.0-forge+mc1.20.1.jar`：ShakerRecipeSerializer、ShakerBlockEntity、ShakerItem、BottleBlockItem。
- `libs/youkaisfeasts-0.5.0.jar`：CuisineBoardBlockEntity、TableItemManager、BaseTableItem、IngredientTableItem、VariantTableItemBase、VariantModelPart、FoodModelHelper、OrderedCuisineRecipe、TableBambooBowls。
- 咖啡馆实例 `mods/[水产乐事] aquaculturedelight-1.1.1-forge-1.20.1.jar` 的原始食品配方。
- 验证使用 ZIP 条目及 `javap -c -p`；未加载数据包、未运行匹配器、未启动客户端。字节码诊断文件在 `tmp/kitchen-audit/`，可随时删除；长期结论以本文为准。

## 雪克杯

- 配方类型 `kaleidoscope_tavern:shaker`，ingredients 为物品 Ingredient，result 使用 Forge CraftingHelper 读取 ItemStack。输入与结果没有命名空间限制；不是流体配方。
- 颜色标签与指定物品：默认 mojito 配方使用 white、white、green 三个颜色 Ingredient 标签。ShakerRecipe.matches 实际调用 RecipeMatcher.findMatches 匹配原料 Ingredient，不读取 ingredientColors；ShakerItem.handRecipe 直接用原始输入 ItemStack 查询 RecipeManager，不先转换为颜色。因此可以把 ingredients 改为明确的 item 或食材语义 tag，无需修改匹配器。ingredientColors 由 serializer 另外解析，不构成匹配条件。若保留宽泛颜色配方，新精准配方可能与其同时匹配；应移除/收窄冲突配方，不能假定引擎会优先选择具体物品配方。
- serializer 与方块存储均为三个原料位；serializer 遇到第四项就停止读取，不能写四项后假定全部消耗。
- BottleBlockItem.isValidForShaker 对非森罗 BottleBlockItem 返回 true；森罗瓶装酒额外要求 brewLevel >= 4。因此不能把该方法误读为只接受森罗酒。
- 原料容器返还存在 IHasContainer 及特定物品分支；不能假定任意外部瓶子、奶桶都自动返还，需要逐物品核对。
- ShakerItem 使用时长分支中，只有累计使用 89–98 tick 才调用 handRecipe；19–68 tick 为 mystery，69–88 为 signature，99 tick 起为 mystery；低于19 tick不结算。不能只改配方而忽略时间窗口。
- 原生倒出要求目标是 EMPTY_GLASSWARE。pourResult 仅在结果为 BlockItem 时用其默认方块状态替换杯子，随后 removeAll。普通非方块饮料没有交付分支，也会走清空；结果数量与一般 NBT 不会通过默认方块状态自动保留。
- 结论：外部原料可写配方；外部普通饮料成品需要领取/杯装兼容。建议保留雪克杯，补目标容器与产物映射、成功交付后扣除、外部原料容器返还；是否保留严格摇制窗口另作玩法决策。不可仅把任意饮料 ID 写进 result 当成端到端可用。

## 三种代表性食品

| 食品 | 原配方 | 静态结论 | 所需工作 |
| --- | --- | --- | --- |
| aquaculturedelight:raw_fish_fillet_roll | fish_fillet_raw ×2 + farmersdelight:cooked_rice ×1 → 成品×2 | 寿司 base 可复用，OrderedCuisineRecipe 支持重复输入与完整数量匹配；成品 ItemStack 可跨模组 | 新配方输入必须列两份鱼片；补外部鱼片的模型映射，并验证顶部可见层数/布局，不可用一份鱼片替代两份造成收益改变 |
| aquaculturedelight:fish_roll_medley | aquaculture:sushi ×3 + raw_fish_fillet_roll ×2 + fried_perch_roll ×2 + minecraft:bowl ×1 → 成品×1 | 已核对的原生流程没有该装盘底座和七份成品布局；recipe.base 是阶段标识，不能直接把 bowl 物品 ID 当成可用 base | 增加装盘起始状态、七份食品的可见摆放与数量匹配、完成/领取动作。碗仅为配方容器证据，真实器皿另确认 |
| youkaisfeasts:assorted_dango | mochi + matcha_mochi + sakura_mochi + minecraft:stick → 成品×1 | 原生仍是工作台无序配方；已核对流程没有相应签子起点与团子串阶段 | 新增签子底座、三次穿料显示与领取，消耗一根签子；这是熟团子组装，不另加烤制 |

CuisineBoardBlockEntity.addItem 先经当前 TableItem.find 查找下一状态，成功后才记录消耗并切换模型，故配方可匹配不等于原料可以从空台投入。TableItemManager 中已有寿司相关阶段；TableBambooBowls 另有代码注册的竹筒料理流程，说明该架构不只支持寿司，但新底座也不是随意添加 JSON 名称即可获得。

BaseTableItem.with、IngredientTableItem.addNext、VariantTableItemBase.create、VariantModelPart.addMapping 提供代码扩展点。配料显示通过独立模型映射选择，不能假定外部 Ingredient 会自动生成合适的三维食物外观。

## 决策与后续验收

推荐先扩展现有料理台：复用寿司流程，增加可复用的装盘、穿串流程。静态依据尚不足以证明替换整块方块更省成本；如果后续要求任意位置自由摆放、整批托盘加工或完全统一的工具动作，而现有状态结构难以承接，再评估自制。

实施后应逐例验证：从空台开始的投料顺序、错误原料拒绝、重复原料计数、未完成不能领取、完整产出数量、容器扣除/返还、退出重进与区块重载、所有中间状态显示。雪克杯另测三种输入数、四项配方拒绝校验、普通饮料与方块饮料领取、容器不匹配不清空、时长边界。

性能：当前未新增运行代码。现有料理台存在按交互进行的配方列表查找，模型映射也有遍历；扩展不应在 tick/render 中扫描全量配方。大规模迁移时按 base/原料建立随配方重载失效的索引；新增状态和可见材料数量设上限，状态变化时同步。性能结论仍须在实际实现后验证。

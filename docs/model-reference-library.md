# 模型参考源目录

用途：后续菜品与容器建模先查此处，复用已经找到的来源。更新日期：2026-09-08。当前为 JAR 资源核验，不代表所有候选模型都完成编辑器视觉验收。

## KC-1：Kaleidoscope Cookery 原模组

- 原始位置：`D:/Minecraft/PCL启动器/.minecraft/versions/咖啡馆/mods/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar`。
- 工作区副本：`libs/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar`。
- 元数据版本：1.4.1-forge+mc1.20.1；Minecraft 范围 [1.20.1,1.21)，Forge [47,)。SHA256：`CF99F12875FEE3FA915D06FA8AA62705C2FD55577FD1DDD459350FBA1ABBFF9F`。
- 目标原图：`assets/kaleidoscope_cookery/textures/item/scramble_egg_with_tomatoes.png`；物品模型位于同命名空间 `models/item/scramble_egg_with_tomatoes.json`。
- 容器参考：`assets/kaleidoscope_cookery/models/block/plate.json`，1 元素，纹理 `kaleidoscope_cookery:block/plate`。
- 食物层次参考：`assets/kaleidoscope_cookery/models/block/food/chorus_fried_egg/chorus_fried_egg_0.json`，6 元素，使用 block/plate 和 block/chorus_fried_egg。适合研究盘上食物厚度、少量几何和像素材质；不是番茄炒蛋的现成三维模型。
- 许可元数据：`BSD-3-Clause + CC BY-NC-SA 4.0`。当前仅作本地参考；若直接复制几何或贴图对外分发，须核对对应资产许可与署名要求。

## DD-1：Display Delight 菜品与碗盘

- 原始位置：`D:/Minecraft/PCL启动器/.minecraft/versions/咖啡馆/mods/[展示乐事] displaydelight-1.7.0.jar`。
- 工作区副本：`libs/[展示乐事] displaydelight-1.7.0.jar`。
- 元数据版本：1.7.0；SHA256：`56D3F0DAD434982895963F3EAA8B65F1C21C7F1B1EF8ED8AB1B42458A01464D6`。
- 以下路径相对 `assets/displaydelight/models/block/`：`food_plate.json`（1 元素、坐标 [1,0,1]..[15,2,15]）、`small_food_plate.json`（1 元素、[4,0,4]..[12,1,12]）、`fried_rice.json`（8 个显式元素，parent 为 `displaydelight:block/bowl`）。
- 炒饭纹理引用：`displaydelight:block/bacon_and_eggs`、`displaydelight:block/fried_rice`、`displaydelight:block/salad_bowl`。正式选用时继续解析 parent 和全部纹理依赖，不能仅提取子 JSON。
- 用途：碗盘比例、器皿内外结构、食物凸起和纹理结合、持握/GUI display 参数。小菜可优先查这一组；展示变换不能用于证明几何在一格内。
- 许可字段指向 `https://github.com/jkvin114/display-delight-neoforge/blob/main/LICENSE`，尚未核对其具体条款与本 JAR 版本对应关系；当前不把本地参考当成可以直接发布的授权。

## 后续维护

- 用户确认的干净碗资产：`C:/Users/yinfire/Desktop/BowlModels/bowl.bbmodel`，5元素，外/内两张32×16纹理；来源 DD-1 salad_bowl，内部 UV 已隔离，禁止回退旧单纹理污染版本。三款食物任务的两个饭碗实际复用该工程，牛肉实际复用 food_plate；仅食物缩放适配。来源副本在各 reference/approved-*，来源等效自动检查入口见 three-food-models.md。

新增来源必须记录来源实例或上游 URL、版本、工作区 JAR 路径、SHA256、内部模型与纹理路径、参考的具体特征、许可核对状态、已验证范围及使用过的任务。原图/模型摘取保留在各模型任务的 reference 下；文档只存于 docs。

当前使用记录：两组来源已用于 [番茄炒蛋流程规划](item-modeling-pilot.md) 的资源调查，尚未用于新模型成品。后续版本不能覆盖旧指纹而不说明变化。

## 番茄炒蛋实际复用（2026-09-08）

用户明确木盘参考 Display Delight；实际复用 DD-1 food_plate 几何及 tray_top/tray_bottom，保持原 UV 和像素最近邻等效，去除物品底面 cullface。食物几何/图集为本次制作。参考副本在任务 reference/dd-*。该本地试用包包含上游盘资产，未发布；许可状态仍按 DD-1 记录。不要将 parent_bowl 的名称或配方 bowl 当作碗形判断。


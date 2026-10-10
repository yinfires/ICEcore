# 三款 Cookery 食物独立模型

用途：2026-09-08 三个指定物品的资源依据、造型决策与交付验收。未要求接入模组。

## 来源与可见特征映射

- KC-1：Cookery 1.4.1-forge+mc1.20.1，工作区精确文件 `libs/[森罗物语：厨房] kaleidoscopecookery-1.4.1-forge+mc1.20.1.jar`，SHA256 与模型参考目录一致。三个模型均为 `minecraft:item/generated`，默认纹理均在 `assets/kaleidoscope_cookery/textures/item/<ID>.png`；不用 legacy_pack。
- 容器必须实际沿用用户此前指定来源：两碗直接复制 `C:/Users/yinfire/Desktop/BowlModels/bowl.bbmodel` 已修正 UV 的五元素干净碗（来源 DD salad_bowl），牛肉直接复制 DD-1 food_plate 原几何及 tray_top/tray_bottom。仅食物自主制作。资源包格式固定 Minecraft 1.20.1 的 15，不继承参考 JAR 内模型的 1.21.11 格式字段。包含上游容器资产，仅本地试用，许可边界同参考目录。
- 资产根目录 `D:/Minecraft/Mod Develop/模型/kaleidoscope_cookery-<ID>/`，原图、配方与模型证据已提取到各 reference。

| ID | 食材证据 | 原图解释与模型对应 |
| --- | --- | --- |
| egg_fried_rice | 默认 pot：两份 eggs + cooked_rice；flex_pot：各一份 | 上部奶白米饭、黄色碎蛋，高置信；橄榄绿小点仅作低置信装饰色，不新增大块配料。深棕容器具有明显侧壁和收底，采用深木碗，宽 12、高 4.5 单位，米饭堆高并保留碗沿。 |
| scramble_egg_with_tomatoes_rice_bowl | rice_bowl 类型以 scramble_egg_with_tomatoes 为 ingredient；后者默认配方为鸡蛋与番茄 | 红黄上层为番茄与蛋，底部白色块为可见饭层，置信高；深棕碗与炒饭一致。饭层上分布大红块与折叠蛋块，不做整颗煎蛋。 |
| braised_beef | pot 为三份 raw_beef + 三个 red_chili；flex_pot 为 raw_beef + chilipepper | 棕红主体与橙褐切面为肉块，红橙下缘与红椒互证；绿色只保留少量色点，不新增蔬菜。原图承载面较宽且低，采用宽 14、高 2 的浅木盘，食物覆盖 X/Z=2..14，仅留一单位外沿。 |

器皿分类依据原图，具体造型由用户已确认来源锁定；上表初拟宽12/高4.5碗规格已废弃，最终碗严格 X/Z=4..12，Y=0..4。食物横向缩到原碗口 X/Z=5..11；盘仍为14×2×14。默认配方数量不转成几何块数。

## 三视图与制作门禁

- gpt-image-2 当前网关 CLI 参考：三行分别蛋炒饭、番茄炒蛋盖饭、红烧牛肉，三列正面、右面、俯面；前两行深木碗，末行为浅木盘。米饭碎粒、折叠蛋、番茄切块、红棕厚肉块通过低数量几何和像素材质对应。生成图仅提供配色与层次，统一坐标表控制三视图一致性。
- 碗沿用已修正的两纹理 UV，盘沿用原 UV 的有意共享；不重新布局。食品使用第三张独立图集，各面 UV 岛有间隔；外底不消费食品像素。原32×16碗纹理在工程内最近邻转成128×128，UV逐轴等比换算，像素外观等效。
- 每张纹理先按 128×128 预算，项目 UV 与 PNG 相同，Java 导出归一化到 0–16；不足时先优化布局。单模型目标不超过 90 元素，静态，不增加 tick、网络或运行时生成。
- 最终检查 JSON、bbmodel 空工程重导入、旋转后边界、可见面 UV、纹理依赖、正/右/顶/底和 UV 编辑器；真实预览与生成参考分开保存。尚未执行的门禁不预先报告通过。

## 最终产物与验证

| 模型 | 元素 / 面 | 实际包围 |
| --- | --- | --- |
| 蛋炒饭 | 40 / 205 | [4,0,4]..[12,5.638,12] |
| 番茄炒蛋盖饭 | 50 / 255 | [4,0,4]..[12,6.03,12] |
| 红烧牛肉 | 31 / 156 | [1,0,1]..[15,5.4,15] |

- 每份 source 包含自包含 bbmodel、语义 UV 清单和 validation.json；export 包含真实 Blockbench Java codec 导出的 JSON、三张128×128 PNG 和单物品本地资源包。合并包位于蛋炒饭 export/Three-Cookery-Foods-1.20.1.zip。
- 制作与验收脚本在蛋炒饭 scripts：build_three.py、verify_three.py、capture_uv.py。生成与验证共用已确认的历史 bb_call.py MCP 封装。三个工程均在空项目重导入，JSON另建空项目并按随包同名 PNG 内容加载纹理；没有启动 Minecraft。
- 来源坐标/UV/最近邻像素等效、食物 UV 互斥、内嵌与外置纹理一致、一格边界、牛肉盘内部半单位采样全覆盖通过。三张图基础 RGBA 内存每模型192 KiB，无 tick/网络/运行时生成。原容器有意共享不误报为非预期重叠。
- preview 的 multiview 为真实编辑器正、右、顶、底、透视截图拼图；uv-0/1/2 为最终纹理 UV 边界图；editor-uv-* 为独立重导入后浮动 UV 面板截图。检查图与生成参考不混用。GUI/手持游戏光照仍未实测，用户审美尚未确认。
- 生成参考实际1254×1254 RGB；请求1024×1024 medium，通过当前网关 bundled CLI edit、模型名 gpt-image-2，原三图作为输入。提示词见本次三视图章节：三行食物×三列正/右/顶、两深碗一浅盘、无整颗蛋与餐具。生成碗造型没有采用，最终容器只以已确认资产为准。

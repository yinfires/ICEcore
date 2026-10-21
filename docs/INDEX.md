# ICEcore 文档索引

Last updated: 2026-10-10（工作区规则独立维护，补充本地开发与调试要求）

本文件是 `docs/` 的唯一阅读入口。开始任务时先确定任务类型，只读取对应文档；禁止为了“了解项目”而全量读取目录。

| 任务类型 | 必读文档 | 读取范围 |
| --- | --- | --- |
| 配方管理器同步与16类设备转换 | [配方管理器同步](recipe-manager-sync.md)、[工作方块容器兼容](workstation-container-compatibility.md) | 读取命令、旧脚本清理、设备数量/时长、葡萄杯桶容量和截图待测清单；生成成功不代表游戏可制作 |
| Spoiled 按天保质期、过期物品、冰箱与餐盘 | [内容实现索引](content-implementation.md)、[时间系统](time-system.md)、[容器兼容](workstation-container-compatibility.md) | 读取“按游戏天保质期与过期物品”；名单来源为根目录 AGENTS.md 记载的配方管理系统，使用配置见 Wiki《保质期与过期物品》 |
| 农夫乐事砧板输入、加工、成品留存与取回 | [内容实现索引](content-implementation.md)、[工作方块容器兼容](workstation-container-compatibility.md) | 读取“农夫乐事砧板单件输入与成品留存”，核对原生结果重定向、容器元数据和批量取回；操作说明见Wiki《农夫乐事砧板》 |
| 双格架子、72格储物、世界物品展示及木色提示背景 | [内容实现索引](content-implementation.md) | 读取“双格架子与木色提示背景”，核对模型、同步、客户端物品显示设置、指令权限、Jade及Modern UI背景入口；配置与全部调试指令见Wiki《架子》 |
| 一体式双层冰箱、原生冰箱复用、扳手拆放 | [内容实现索引](content-implementation.md)、[冒险模式建造系统](adventure-building-system.md) | 读取“一体式双层冰箱”，核对原生前置、上下配对、库存与强拆确认；长期使用说明见 Wiki《冰箱》 |
| 物品原始提示隐藏、效果过滤与自定义描述 | [内容实现索引](content-implementation.md)、[全局配置 Wiki](../wiki/全局配置.md) | 读取“物品提示与客户端描述覆盖”；配置、JSON 格式及重载方法见 Wiki |
| 工作区规则维护、实现/资源/构建/调试通用要求 | [项目工作区规则](../AGENTS.md)、[开发与调试规则](workspace-development-rules.md) | 规则维护读取“文档与规则维护”；实现、资源、构建与调试读取对应章节；必要规则直接维护在本工作区 |
| 种子袋作物灌溉、肥料、洒水器与 Bakery 水槽补水 | [内容实现索引](content-implementation.md) | 读取“Dew Drop 灌溉与 Bakery 水槽兼容”和“种子袋种植系统”；真实操作说明在独立 Wiki《种植与灌溉》 |
| 任意任务开工、规范错误复盘、经验沉淀 | [工作区规范错误与经验总结](workspace-errors-and-lessons.md) | 每次先读“生效中的防复发规则”；仅在任务命中相应类别或需要写入时读具体记录与模板 |
| CozyCafe电脑页面提示、操作失败与开店限制 | [项目规则](../AGENTS.md)、[内容实现索引](content-implementation.md)、[错误与经验](workspace-errors-and-lessons.md) | 读取页面反馈例外、营业时间条目和ERR-20261017-01；复用完整原生电脑提示和Modern UI接管，不使用通用底部反馈或底层自绘 |
| Yuushya入世声明、CozyCafe营业时间与夜间关店 | [内容实现索引](content-implementation.md) | 读取“Yuushya 入世声明与 CozyCafe 营业时间”；时间语义见 time-system.md，长期使用说明见Wiki菜单与顾客座位页 |
| 咖啡馆客流量设计、每日额度、开店快照与多人生成节奏 | [客流量计算方案](cafe-customer-flow-design.md)、[内容实现索引](content-implementation.md) | 先读设计的额度来源、快照边界、计时上限与接纳规则；设计尚未实现，当前行为仍以实现索引中的 CozyCafe 条目为准 |
| 厨具统一、雪克杯饮料扩展、料理台食品迁移 | [厨具统一静态核对](kitchen-unification-audit.md) | 读取雪克杯输入/倒出限制及寿司、拼盘、团子串三例；区分静态审计与未完成的运行验收 |
| 工作方块桶、瓶、碗兼容，容器返还、盛取、提示或声音 | [工作方块容器兼容](workstation-container-compatibility.md) | 读取共用服务、数据包格式、解析优先级、交互矩阵、槽位元数据和验证要求；禁止在新工作方块重复写死容器逻辑 |
| SmartKeyPrompts / KubeJS 自定义按键提示 | [KubeJS 提示编写方法](smartkeyprompts-kubejs.md) | 接口绑定、END 转发、缓存生命周期、类过滤与 Rhino 验证；版本变化重新核对 |
| 游戏内 TOML 配置保存、热重载或禁食开关未即时生效 | [内容实现索引](content-implementation.md) | 读取“标准配置保存与运行时刷新”和“全局禁止食物食用配置” |
| 任意代码或资源实现、修改、排错 | [内容实现索引](content-implementation.md) | 先读目录和本次涉及的功能条目；当前包含构建流程、模组加载、CozyCafe 菜单/电脑范围扩展和冒险模式建造系统条目 |
| 模型、贴图、UV 制作与交付 | [工作区模型规则](../AGENTS.md)、[贴图与模型制作流程](texture-and-model-workflow.md)、[内容实现索引](content-implementation.md) | 先读“模型与 UV 制作验收”和制作流程，再读相关资产条目；可先生成参考图；新 MC 贴图及模型资产统一保存到 `D:/Minecraft/Mod Develop/模型/`，按任务分子目录 |
| GitHub Wiki 内容、导航或独立仓库维护 | [根目录 Wiki](../wiki/Home.md)、[内容实现索引](content-implementation.md) | 仅在新增或改变长期使用说明时维护页面；纯视觉表现、渲染方式、内部实现、性能修复和开发期 bug 修正不写入 Wiki；读取 Wiki 当前页面、项目 Wiki 规范和独立仓库状态，只修改 `wiki/`，使用嵌套仓库命令提交与推送 |
| 版本更新、发布说明、玩家可见变化 | [更新日志编写要求](changelog-guidelines.md)、[根目录更新日志](../CHANGELOG.md) | 先读编写要求，再读取当前版本日志条目；旧版本仅按需读取 |
| 冒险模式建造系统实现、配置或排错 | [冒险模式建造系统](adventure-building-system.md)、[内容实现索引](content-implementation.md) | 先读系统行为、JSON、命令和入口，再回到实现索引核对当前状态 |
| 时间流速、日期 HUD、床投票或时间跳过 | [时间系统与时间跳过](time-system.md)、[内容实现索引](content-implementation.md) | 先读时间语义、JSON、命令、投票和过场状态机，再核对实现条目 |
| 固定 NPC、NPC 定义、生成移除命令或 ChatBox 对话 | [固定 NPC 系统](npc-system.md)、[内容实现索引](content-implementation.md) | 读取定义 JSON、实体行为、命令、ChatBox 资源链路和验证边界 |
| 任务与教程系统（J/教程界面、追踪 HUD、任务获取/解锁、奖励与联动） | [内容实现索引](content-implementation.md) | 已全部落地：读实现索引「任务与教程系统」条目（原交接文档已删除） |
| 岛屿传送/实质化系统（区域捕获/重扫、隐藏=真移除、入场镜头过场、屏幕变形特效、分批生成、千格放置） | [内容实现索引](content-implementation.md) | 已全部落地：读实现索引「岛屿传送/实质化系统」条目（原设计交接文档已删除） |
| 可复用镜头过场框架（相机接管、输入/HUD/手抑制、黑屏/闪白过渡与缓动，时间跳过与岛屿入场共用） | [内容实现索引](content-implementation.md) | 读“可复用镜头过场框架”条目：`client.cutscene` 的 `CameraPose`/`CutsceneCamera`/`CutsceneFade` 职责与两相位；改镜头/黑屏行为前先读 `TimeCutsceneSafetyTest` 安全不变式 |

| 冒险模式交互黑白名单、来源模组/标签和坐标特例 | [冒险模式建造系统](adventure-building-system.md)、[使用说明](../wiki/冒险模式交互限制.md) | 读取普通交互限制条目、JSON、命令和标签人工查看方法 |

## 维护规则

- 三款 Cookery 食物（蛋炒饭、番茄炒蛋盖饭、红烧牛肉）的独立模型：读 [三款食物模型记录](three-food-models.md)，核对来源、碗盘分类、可见特征与验收结果。

- 物品 ID 驱动的建模规划与执行：读 [物品建模能力尝试流程](item-modeling-pilot.md)，覆盖资源定位、色块解释、三视图、建模和阶段验收；选参考前读 [模型参考源目录](model-reference-library.md)，仅读取对应类别与版本。

- 新增、移动、重命名或删除 `docs/` 内文档时，同步维护本索引。
- 更新日志固定存放在项目根目录的 `CHANGELOG.md`，仅维护中文版本日志，不在 `docs/` 内重复维护；更新日志只保留版本元数据和关键玩家可见日志条目，具体编写要求统一写在 [更新日志编写要求](changelog-guidelines.md) 中；涉及更新日志的任务必须先读取该文档，版本号未修改时，涉及关键内容的更新写入当前版本号条目，不建立“未发布”区段；同一版本内不涉及关键内容的贴图、图标、模型资源、颜色/排版等非关键视觉调整，以及内部实现细节，只更新实现文档，不修改更新日志、标题日期或发布类型；版本号、标题日期和发布类型不构成公开发布证据，同一未公开版本新增功能的内部修正必须合并进最终“新增内容”，不得单列“调整内容”或“修复内容”；当前版本条目的发布日期和发布类型必须随关键内容同步更新。
- 每个新文档必须注明用途，并在此处给出“何时读取”和“读取范围”。
- 具体实现事实写入 `content-implementation.md` 或从其中链接的专题文档；不要把同一事实复制到多处。
- 规范错误、可复用经验及其防复发闭环统一写入 `workspace-errors-and-lessons.md`；不得散落到实现文档或仅保留在对话中。
- 根目录 `AGENTS.md` 与整个 `docs/` 目录允许 Git 跟踪、提交与推送；`tmp/` 与普通 `tool/` 内容不跟踪，用户要求的 `tool/recipe-sync/sync.mjs`、`sync.test.mjs` 精确放行；`wiki/` 是项目根目录下单独管理的 GitHub Wiki 仓库，不纳入主仓库跟踪。

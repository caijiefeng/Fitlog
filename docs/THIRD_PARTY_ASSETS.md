# 第三方动作素材记录（THIRD-PARTY ASSETS）

FitLog 内置动作示意图全部**离线打包**在 APK 中，运行时不从网络加载。
素材以 `SeedDataProvider.BUILT_IN_DEFS` 的稳定 `builtInKey` 为键映射，
逐项记录见 `app/src/main/assets/licenses/` 下的三个文件
（`free-exercise-db.txt` / `opentraining-exercises.txt` / `placeholder.txt`）。

## 来源一：yuhonas/free-exercise-db（主来源）

- 仓库：https://github.com/yuhonas/free-exercise-db
- 固定 commit：`b0eed061e1c832b3ed815fbaa4b45b3cdc14df49`
- 许可证：**Unlicense**（公有领域，见仓库 LICENSE.md）
- 作者/署名：yuhonas；数据与图片整理自 wger 项目（GPLv2+）衍生数据集
- 使用内容：动作 JSON（动作说明）、起始姿势图 `0.jpg`、结束姿势图 `1.jpg`
- 处理方式：等比缩放至 512px（start/end）与 160px（thumb），重编码为 WebP，
  添加统一浅灰背景留白；不拉伸人体比例

## 来源二：chaosbastler/opentraining-exercises（补充来源）

- 仓库：https://github.com/chaosbastler/opentraining-exercises
- 固定 commit：`03aa2b3370064b519a53c6cebe22b4c74ed960fc`
- 许可证：**CC-BY-SA-3.0**（见仓库 README；图片作者 Everkinetic）
- 使用内容：仅当主来源找不到准确对应动作时使用——
  `push_up`（俯卧撑）、`crunch`（卷腹）、`glute_bridge`（臀桥）
- 处理方式：同上（缩放、重编码、留白）

## 占位图（FitLog 原创）

- 未匹配动作使用按肌群分类的原创占位插图（标注「暂无动作示意图」），
  **绝不使用错误动作图片冒充**。
- 当前未匹配（5 个）：`reverse_pec_deck_fly`、`machine_lateral_raise`、
  `reverse_pec_deck`、`bulgarian_split_squat`、`nordic_curl`

## 覆盖情况

- 内置动作总数：54
- 准确图片匹配：49（覆盖率 **90.7%**，目标 ≥90%）
- 许可证分布：Unlicense ×46，CC-BY-SA-3.0 ×3，原创占位 ×5
- 素材总大小：约 1.95 MB（目标 ≤30 MB）

## 再生成本地素材

```bash
python3 -m pip install -r tools/exercise_assets/requirements.txt
# 按 sources.json 检出固定 commit 后：
python3 tools/exercise_assets/import_assets.py <fedb-checkout> <ot-checkout> --output .
```

脚本会校验本地 checkout 的 HEAD 与固定 commit 一致，避免意外使用上游新版本。
覆盖率报告输出至 `build/reports/exercise-assets/coverage.txt`。

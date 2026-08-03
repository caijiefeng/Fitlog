#!/usr/bin/env python3
"""
FitLog 动作素材导入脚本。

- 从固定 commit 的素材仓库（见 sources.json）读取动作姿势图，
  绝不每次构建时拉取上游最新版本。
- 以 SeedDataProvider.BUILT_IN_DEFS 的 builtInKey 为键，
  按 exercise_asset_map.json 精确映射（无模糊自动匹配）。
- 输出 app/src/main/assets/exercises/<builtInKey>/{thumb,start,end}.webp
  与 manifest.json，以及覆盖率报告 build/reports/exercise-assets/coverage.txt。
- 未匹配动作生成肌群占位图（标注「暂无动作示意图」），
  绝不使用错误动作图片冒充。

用法:
    python3 import_assets.py <fedb_checkout> <ot_checkout> [--output <repo_root>]
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

THUMB_SIZE = 160
FULL_SIZE = 512
WEBP_QUALITY = 80
BG_COLOR = (237, 241, 239)  # 统一浅灰背景留白

MUSCLE_PLACEHOLDERS = {
    "CHEST": "胸部动作",
    "BACK": "背部动作",
    "SHOULDERS": "肩部动作",
    "BICEPS": "手臂动作",
    "TRICEPS": "手臂动作",
    "FOREARMS": "手臂动作",
    "QUADRICEPS": "腿部动作",
    "HAMSTRINGS": "腿部动作",
    "GLUTES": "腿部动作",
    "CALVES": "腿部动作",
    "CORE": "核心动作",
    "CARDIO": "有氧动作",
    "FULL_BODY": "全身动作",
}

FONT_CANDIDATES = [
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
    "/usr/share/fonts/opentype/noto/NotoSansCJK-Bold.ttc",
    "/usr/share/fonts/opentype/noto/NotoSerifCJK-Regular.ttc",
    "/usr/share/fonts/opentype/noto/NotoSerifCJK-Bold.ttc",
]


def find_font() -> str:
    for f in FONT_CANDIDATES:
        if os.path.exists(f):
            return f
    return ""


def parse_seed_defs(seed_path: Path) -> dict:
    """从 SeedDataProvider.kt 提取 builtInKey -> (name, muscleGroup, equipment, tracking)。"""
    src = seed_path.read_text(encoding="utf-8")
    defs = {}
    for m in re.finditer(
        r'BuiltInExerciseDef\(\s*"([a-z0-9_]+)"\s*,\s*"([^"]*)"\s*,\s*"([A-Z_]+)"\s*,\s*\d+\s*,\s*"([A-Z_]+)"\s*,\s*"([A-Z_]+)"\s*\)',
        src,
    ):
        key, name, muscle, equip, tracking = m.groups()
        defs[key] = {
            "name": name,
            "primaryMuscleGroup": muscle,
            "equipmentType": equip,
            "trackingType": tracking,
        }
    return defs


def verify_checkout(repo_path: Path, expected_commit: str) -> None:
    """验证本地 checkout 是否与固定的 commit 一致。"""
    if not (repo_path / ".git").exists():
        sys.exit(f"FATAL: {repo_path} 不是 git 仓库。请先按 sources.json 检出固定 commit。")
    head = subprocess.check_output(
        ["git", "-C", str(repo_path), "rev-parse", "HEAD"], text=True
    ).strip()
    if head != expected_commit:
        sys.exit(
            f"FATAL: {repo_path} 当前 HEAD={head}，与固定 commit {expected_commit} 不一致。"
        )


def load_image(path: Path) -> Image.Image:
    im = Image.open(path)
    im.load()
    if im.mode != "RGB":
        im = im.convert("RGB")
    return im


def fit_onto_canvas(im: Image.Image, target: int) -> Image.Image:
    """等比缩放图片到最长边 target，放入统一浅灰画布（不拉伸人体比例）。"""
    w, h = im.size
    scale = target / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    im = im.resize((nw, nh), Image.LANCZOS)
    canvas = Image.new("RGB", (target, target), BG_COLOR)
    canvas.paste(im, ((target - nw) // 2, (target - nh) // 2))
    return canvas


def save_webp(im: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path, "WEBP", quality=WEBP_QUALITY, method=6)


def process_pair(start: Image.Image, end: Image.Image | None, out_dir: Path) -> None:
    """输出 thumb/start/end 三张图。end 缺失时用 start 占位并标记。"""
    save_webp(fit_onto_canvas(start, THUMB_SIZE), out_dir / "thumb.webp")
    save_webp(fit_onto_canvas(start, FULL_SIZE), out_dir / "start.webp")
    end_im = end if end is not None else start
    save_webp(fit_onto_canvas(end_im, FULL_SIZE), out_dir / "end.webp")


def placeholder_image(label: str, muscle_label: str, font_path: str) -> Image.Image:
    """肌群占位图：浅灰底 + 简单哑铃图形 + 「暂无动作示意图」。"""
    im = Image.new("RGB", (FULL_SIZE, FULL_SIZE), BG_COLOR)
    d = ImageDraw.Draw(im)
    # 柔和圆形底
    d.ellipse([116, 76, 396, 356], fill=(220, 229, 225))
    # 哑铃图形（两片杠铃片 + 杆）
    d.rounded_rectangle([196, 196, 316, 224], radius=14, fill=(140, 152, 147))
    for x0 in (156, 316):
        d.ellipse([x0, 168, x0 + 56, 252], fill=(120, 133, 128))
    # 文字
    if font_path:
        font_big = ImageFont.truetype(font_path, 34)
        font_small = ImageFont.truetype(font_path, 26)
    else:
        font_big = font_small = ImageFont.load_default()
    small = "暂无动作示意图"
    w1 = d.textlength(small, font=font_small)
    d.text(((FULL_SIZE - w1) / 2, 396), small, font=font_small, fill=(90, 102, 97))
    w2 = d.textlength(label, font=font_big)
    d.text(((FULL_SIZE - w2) / 2, 96), label, font=font_big, fill=(90, 102, 97))
    return im


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("fedb", help="yuhonas/free-exercise-db checkout 路径")
    parser.add_argument("ot", help="chaosbastler/opentraining-exercises checkout 路径")
    parser.add_argument("--output", default=".", help="仓库根目录")
    args = parser.parse_args()

    repo_root = Path(args.output).resolve()
    tools_dir = repo_root / "tools" / "exercise_assets"
    fedb = Path(args.fedb).resolve()
    ot = Path(args.ot).resolve()

    sources = json.loads((tools_dir / "sources.json").read_text(encoding="utf-8"))
    verify_checkout(fedb, sources["free_exercise_db"]["commit"])
    verify_checkout(ot, sources["opentraining"]["commit"])

    asset_map = json.loads((tools_dir / "exercise_asset_map.json").read_text(encoding="utf-8"))
    seed_defs = parse_seed_defs(
        repo_root / "app/src/main/java/com/example/fitlog/core/database/seed/SeedDataProvider.kt"
    )
    if not seed_defs:
        sys.exit("FATAL: 未能从 SeedDataProvider.kt 解析任何动作定义")

    out_root = repo_root / "app/src/main/assets/exercises"
    font_path = find_font()

    manifest = {}
    matched, missing = [], []
    total_bytes = 0

    for key, meta in sorted(seed_defs.items()):
        entry = asset_map.get(key)
        out_dir = out_root / key
        muscle = meta["primaryMuscleGroup"]
        placeholder_label = MUSCLE_PLACEHOLDERS.get(muscle, "其他动作")

        if entry is None or entry.get("source") == "placeholder":
            # 未匹配 → 肌群占位图
            ph = placeholder_image(placeholder_label, "暂无动作示意图", font_path)
            process_pair(ph, None, out_dir)
            missing.append(key)
            manifest[key] = {
                "thumbnail": f"exercises/{key}/thumb.webp",
                "startImage": f"exercises/{key}/start.webp",
                "endImage": f"exercises/{key}/end.webp",
                "instructionsZh": entry["instructionsZh"] if entry else ["暂无动作示意说明"],
                "sourceName": "placeholder",
                "sourceExerciseId": None,
                "license": "original",
                "isPlaceholder": True,
            }
        else:
            src, sid = entry["source"], entry["source_id"]
            if src == "free_exercise_db":
                base = fedb / "exercises" / sid
                start_p = base / "0.jpg"
                end_p = base / "1.jpg"
                start_im = load_image(start_p) if start_p.exists() else None
                end_im = load_image(end_p) if end_p.exists() else None
                source_name = "free-exercise-db"
                license_name = "Unlicense"
            else:
                # opentraining: <Name>-1.png / <Name>-2.png
                start_p = ot / f"{sid}-1.png"
                end_p = ot / f"{sid}-2.png"
                start_im = load_image(start_p) if start_p.exists() else None
                end_im = load_image(end_p) if end_p.exists() else None
                source_name = "opentraining-exercises"
                license_name = "CC-BY-SA-3.0"

            if start_im is None:
                sys.exit(f"FATAL: {key} 缺少源图 {start_p}")

            process_pair(start_im, end_im, out_dir)
            matched.append(key)
            manifest[key] = {
                "thumbnail": f"exercises/{key}/thumb.webp",
                "startImage": f"exercises/{key}/start.webp",
                "endImage": f"exercises/{key}/end.webp",
                "instructionsZh": entry["instructionsZh"],
                "sourceName": source_name,
                "sourceExerciseId": sid,
                "license": license_name,
                "isPlaceholder": False,
            }

    # 统计
    for k, e in manifest.items():
        for p in ("thumbnail", "startImage", "endImage"):
            f = repo_root / "app/src/main/assets" / e[p]
            if f.exists():
                total_bytes += f.stat().st_size

    (out_root / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    # 逐项许可证记录（app/src/main/assets/licenses/）
    licenses_dir = repo_root / "app/src/main/assets/licenses"
    licenses_dir.mkdir(parents=True, exist_ok=True)
    fedb_commit = sources["free_exercise_db"]["commit"]
    ot_commit = sources["opentraining"]["commit"]
    license_groups = {
        "free-exercise-db": (
            "free_exercise_db",
            f"源仓库: yuhonas/free-exercise-db @ {fedb_commit}\n"
            "许可证: Unlicense（公有领域）\n"
            "作者/署名: yuhonas（数据与图片源自 wger 项目，本仓库以 Unlicense 发布）\n"
            "修改说明: 已等比缩放至 512/160px 并重编码为 WebP，添加统一浅灰背景留白\n\n",
        ),
        "opentraining-exercises": (
            "opentraining",
            f"源仓库: chaosbastler/opentraining-exercises @ {ot_commit}\n"
            "许可证: CC-BY-SA-3.0\n"
            "作者/署名: Everkinetic\n"
            "修改说明: 已等比缩放至 512/160px 并重编码为 WebP，添加统一浅灰背景留白\n\n",
        ),
        "placeholder": (
            "placeholder",
            "来源: FitLog 自绘肌群占位图（原创）\n"
            "许可证: original\n"
            "作者/署名: FitLog 项目\n"
            "修改说明: 无（原创插图）\n\n",
        ),
    }
    grouped = {name: [] for name in license_groups}
    for key, e in sorted(manifest.items()):
        grouped[e["sourceName"]].append((key, e))
    for group_name, (source_key, header) in license_groups.items():
        lines = [header]
        for key, e in grouped[group_name]:
            lines.append(f"builtInKey: {key}")
            if e["sourceExerciseId"]:
                if source_key == "free_exercise_db":
                    src_file = f"exercises/{e['sourceExerciseId']}/0.jpg, 1.jpg"
                else:
                    src_file = f"{e['sourceExerciseId']}-1.png, -2.png"
                lines.append(f"  源文件: {src_file}")
            lines.append(f"  FitLog 中对应动作: {key}（{seed_defs[key]['name']}）")
            lines.append("")
        (licenses_dir / f"{group_name}.txt").write_text("\n".join(lines), encoding="utf-8")

    # 覆盖率报告
    report = repo_root / "build/reports/exercise-assets"
    report.mkdir(parents=True, exist_ok=True)
    total = len(seed_defs)
    license_dist = {}
    for e in manifest.values():
        license_dist[e["license"]] = license_dist.get(e["license"], 0) + 1
    lines = [
        f"总内置动作数: {total}",
        f"准确匹配数: {len(matched)}",
        f"缺失数: {len(missing)}",
        f"缺失 builtInKey: {', '.join(missing) if missing else '(无)'}",
        f"图片覆盖率: {len(matched)}/{total} = {100 * len(matched) / total:.1f}%",
        f"许可证分布: {json.dumps(license_dist, ensure_ascii=False)}",
        f"素材总大小: {total_bytes / 1024 / 1024:.2f} MB",
    ]
    (report / "coverage.txt").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print("\n".join(lines))
    print(f"输出目录: {out_root}")


if __name__ == "__main__":
    main()

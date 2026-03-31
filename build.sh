#!/bin/bash
# MC Helper 多版本构建脚本
# 用法：./build.sh <mc_target>
#   mc_target 可选值：mc1_20_1 | mc1_21_4 | mc1_21_11
#
# 示例：
#   ./build.sh mc1_20_1    # 构建 1.20.1 版本（需要 JDK 17）
#   ./build.sh mc1_21_4    # 构建 1.21.4 版本（需要 JDK 21）
#   ./build.sh mc1_21_11   # 构建 1.21.11 版本（需要 JDK 21）

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

MC_TARGET="${1:-mc1_20_1}"

case "$MC_TARGET" in
  mc1_20_1)
    BUILD_TEMPLATE="build.gradle.fg6"
    WRAPPER_FILE="gradle/wrapper/gradle-wrapper-fg6.properties"
    JDK_PATH="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
    ;;
  mc1_21_4|mc1_21_11)
    BUILD_TEMPLATE="build.gradle.fg7"
    WRAPPER_FILE="gradle/wrapper/gradle-wrapper-fg7.properties"
    JDK_PATH="/tmp/jdk21-arm64/Contents/Home"
    if [ ! -d "$JDK_PATH" ]; then
      echo "错误：JDK 21 不存在于 $JDK_PATH"
      echo "请先解压 JDK 21 arm64 到 /tmp/jdk21-arm64/"
      exit 1
    fi
    ;;
  *)
    echo "未知的 mc_target: $MC_TARGET"
    echo "可选值: mc1_20_1 | mc1_21_4 | mc1_21_11"
    exit 1
    ;;
esac

export JAVA_HOME="$JDK_PATH"

# 备份当前 build.gradle，替换为目标版本的模板
BUILD_BACKUP="build.gradle.backup.$$"
cp build.gradle "$BUILD_BACKUP"
cp "$BUILD_TEMPLATE" build.gradle

# 切换 Gradle wrapper
cp "$WRAPPER_FILE" gradle/wrapper/gradle-wrapper.properties

# 确保在任何情况下都恢复 build.gradle
cleanup() {
    cp "$BUILD_BACKUP" build.gradle
    rm -f "$BUILD_BACKUP"
}
trap cleanup EXIT

echo ">>> 使用 JDK: $JAVA_HOME"
echo ">>> 构建目标: $MC_TARGET"
echo ">>> 构建模板: $BUILD_TEMPLATE"
echo ""

./gradlew build \
  -Pmc_target="$MC_TARGET" \
  --no-daemon \
  "${@:2}"

echo ""
echo "构建完成！输出文件："
ls -la build/libs/*.jar 2>/dev/null || echo "（找不到 jar 文件）"

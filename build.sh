#!/bin/bash
# MC Helper 多版本构建脚本
# 用法：./build.sh <mc_target>
#   mc_target 可选值：mc1_20_1 | mc1_21_1 | mc1_21_4 | mc1_21_11 | mc26_1
#
# 示例：
#   ./build.sh mc1_20_1    # 构建 1.20.1 版本（需要 JDK 17）
#   ./build.sh mc1_21_1    # 构建 1.21.1 版本（需要 JDK 21）
#   ./build.sh mc1_21_4    # 构建 1.21.4 版本（需要 JDK 21）
#   ./build.sh mc1_21_11   # 构建 1.21.11 版本（需要 JDK 21）
#   ./build.sh mc26_1      # 构建 26.1 版本（Java 25 由 Gradle toolchain 自动下载）

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

MC_TARGET="${1:-mc1_20_1}"

# brew 安装的 JDK 路径（支持 Intel 和 Apple Silicon）
JDK17_BREW="/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
JDK21_BREW="/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
# Apple Silicon 路径
JDK17_BREW_ARM="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
JDK21_BREW_ARM="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"

find_jdk() {
  local version=$1
  local intel_path jdk_arm_path
  if [ "$version" = "17" ]; then
    intel_path="$JDK17_BREW"
    jdk_arm_path="$JDK17_BREW_ARM"
  else
    intel_path="$JDK21_BREW"
    jdk_arm_path="$JDK21_BREW_ARM"
  fi

  if [ -d "$intel_path" ]; then
    echo "$intel_path"
  elif [ -d "$jdk_arm_path" ]; then
    echo "$jdk_arm_path"
  else
    echo ""
  fi
}

case "$MC_TARGET" in
  mc1_20_1)
    BUILD_TEMPLATE="build.gradle.fg6"
    WRAPPER_FILE="gradle/wrapper/gradle-wrapper-fg6.properties"
    JDK_PATH="$(find_jdk 17)"
    if [ -z "$JDK_PATH" ]; then
      echo "错误：找不到 JDK 17，请通过 brew 安装：brew install openjdk@17"
      exit 1
    fi
    ;;
  mc1_21_1|mc1_21_4|mc1_21_11|mc26_1)
    BUILD_TEMPLATE="build.gradle.fg7"
    WRAPPER_FILE="gradle/wrapper/gradle-wrapper-fg7.properties"
    JDK_PATH="$(find_jdk 21)"
    if [ -z "$JDK_PATH" ]; then
      echo "错误：找不到 JDK 21，请通过 brew 安装：brew install openjdk@21"
      exit 1
    fi
    # mc26_1 编译需要 Java 25，由 Gradle toolchain 自动下载，JDK 21 仅用于启动 Gradle
    ;;
  *)
    echo "未知的 mc_target: $MC_TARGET"
    echo "可选值: mc1_20_1 | mc1_21_1 | mc1_21_4 | mc1_21_11 | mc26_1"
    exit 1
    ;;
esac

export JAVA_HOME="$JDK_PATH"

# 切换 Gradle wrapper
cp "$WRAPPER_FILE" gradle/wrapper/gradle-wrapper.properties

# build.gradle 是 .gitignore 中的临时文件，构建前从模板复制，构建后清除
cp "$BUILD_TEMPLATE" build.gradle
cleanup() {
    rm -f build.gradle
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

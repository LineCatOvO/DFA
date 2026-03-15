package com.dfa.core.vm.termux

/**
 * Termux相关常量定义
 *
 * 包含Termux环境的路径、包名、命令等常量配置
 */
object TermuxConstants {

    // ==================== 路径常量 ====================

    /**
     * Termux前缀路径
     * Termux安装的软件包的根目录
     */
    const val TERMUX_PREFIX = "/data/data/com.termux/files/usr"

    /**
     * Termux主目录
     * Termux用户的主目录
     */
    const val TERMUX_HOME = "/data/data/com.termux/files/home"

    /**
     * Termux应用文件路径
     */
    const val TERMUX_FILES_PATH = "/data/data/com.termux/files"

    /**
     * Termux临时目录
     */
    const val TERMUX_TMP = "/data/data/com.termux/files/usr/tmp"

    /**
     * Termux bin目录
     */
    const val TERMUX_BIN = "/data/data/com.termux/files/usr/bin"

    /**
     * Termux lib目录
     */
    const val TERMUX_LIB = "/data/data/com.termux/files/usr/lib"

    /**
     * Termux etc目录
     */
    const val TERMUX_ETC = "/data/data/com.termux/files/usr/etc"

    /**
     * Termux var目录
     */
    const val TERMUX_VAR = "/data/data/com.termux/files/usr/var"

    // ==================== 包名常量 ====================

    /**
     * Termux主应用包名
     */
    const val TERMUX_PACKAGE_NAME = "com.termux"

    /**
     * Termux:API包名
     */
    const val TERMUX_API_PACKAGE_NAME = "com.termux.api"

    /**
     * Termux:Boot包名
     */
    const val TERMUX_BOOT_PACKAGE_NAME = "com.termux.boot"

    /**
     * Termux:Float包名
     */
    const val TERMUX_FLOAT_PACKAGE_NAME = "com.termux.window"

    /**
     * Termux:Styling包名
     */
    const val TERMUX_STYLING_PACKAGE_NAME = "com.termux.styling"

    /**
     * Termux:Tasker包名
     */
    const val TERMUX_TASKER_PACKAGE_NAME = "com.termux.tasker"

    /**
     * Termux:Widget包名
     */
    const val TERMUX_WIDGET_PACKAGE_NAME = "com.termux.widget"

    // ==================== 命令常量 ====================

    /**
     * pkg包管理器命令
     */
    const val PKG_COMMAND = "pkg"

    /**
     * apt包管理器命令
     */
    const val APT_COMMAND = "apt"

    /**
     * dpkg命令
     */
    const val DPKG_COMMAND = "dpkg"

    /**
     * Termux shell路径
     */
    const val TERMUX_SHELL = "/data/data/com.termux/files/usr/bin/bash"

    /**
     * Termux zsh路径
     */
    const val TERMUX_ZSH = "/data/data/com.termux/files/usr/bin/zsh"

    // ==================== 环境变量常量 ====================

    /**
     * PREFIX环境变量名
     */
    const val ENV_PREFIX = "PREFIX"

    /**
     * HOME环境变量名
     */
    const val ENV_HOME = "HOME"

    /**
     * PATH环境变量名
     */
    const val ENV_PATH = "PATH"

    /**
     * LD_LIBRARY_PATH环境变量名
     */
    const val ENV_LD_LIBRARY_PATH = "LD_LIBRARY_PATH"

    /**
     * TMPDIR环境变量名
     */
    const val ENV_TMPDIR = "TMPDIR"

    /**
     * TERM环境变量名
     */
    const val ENV_TERM = "TERM"

    /**
     * LANG环境变量名
     */
    const val ENV_LANG = "LANG"

    // ==================== 超时常量 ====================

    /**
     * 默认命令执行超时时间（毫秒）
     */
    const val DEFAULT_COMMAND_TIMEOUT_MS = 30_000L

    /**
     * 长时间命令执行超时时间（毫秒）
     */
    const val LONG_COMMAND_TIMEOUT_MS = 300_000L

    /**
     * 包安装超时时间（毫秒）
     */
    const val PACKAGE_INSTALL_TIMEOUT_MS = 120_000L

    /**
     * 文件操作超时时间（毫秒）
     */
    const val FILE_OPERATION_TIMEOUT_MS = 60_000L

    // ==================== 其他常量 ====================

    /**
     * Termux运行文件目录
     */
    const val TERMUX_RUN_DIR = "/data/data/com.termux/files/usr/var/run"

    /**
     * Termux日志目录
     */
    const val TERMUX_LOG_DIR = "/data/data/com.termux/files/usr/var/log"

    /**
     * Termux应用共享目录
     */
    const val TERMUX_SHARED_DIR = "/data/data/com.termux/files/shared"

    /**
     * Termux应用外部存储目录
     */
    const val TERMUX_EXTERNAL_STORAGE = "/storage/emulated/0"

    /**
     * Termux应用内部存储前缀
     */
    const val TERMUX_INTERNAL_STORAGE_PREFIX = "/sdcard"

    /**
     * Termux支持的架构列表
     */
    val SUPPORTED_ARCHITECTURES = setOf(
        "aarch64",
        "arm",
        "x86_64",
        "i686"
    )

    /**
     * Termux必需的附加包列表
     */
    val ESSENTIAL_PACKAGES = setOf(
        "bash",
        "coreutils",
        "grep",
        "sed",
        "curl",
        "wget"
    )

    /**
     * Termux API包列表
     */
    val API_PACKAGES = setOf(
        "termux-api",
        "termux-api-doc"
    )

    /**
     * 开发工具包列表
     */
    val DEVELOPMENT_PACKAGES = setOf(
        "clang",
        "make",
        "git",
        "python",
        "nodejs"
    )
}
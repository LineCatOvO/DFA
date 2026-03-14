package com.dfa.core.vm.image

/**
 * 镜像管理常量配置
 */
object ImageConstants {

    /**
     * 默认镜像下载URL
     * Debian 12 (Bookworm) NoCloud ARM64 QCOW2镜像
     */
    const val DEFAULT_IMAGE_URL = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-nocloud-arm64.qcow2"

    /**
     * 默认镜像名称
     */
    const val DEFAULT_IMAGE_NAME = "debian-12-nocloud-arm64.qcow2"

    /**
     * 默认镜像ID
     */
    const val DEFAULT_IMAGE_ID = "debian-12-nocloud-arm64"

    /**
     * 镜像存储子目录名
     */
    const val IMAGE_SUBDIRECTORY = "vm-images"

    /**
     * 默认校验和类型
     */
    const val DEFAULT_CHECKSUM_TYPE = "sha256"

    /**
     * 下载缓冲区大小（8KB）
     */
    const val DOWNLOAD_BUFFER_SIZE = 8192

    /**
     * 下载超时时间（毫秒）
     */
    const val DOWNLOAD_TIMEOUT_MS = 30_000L

    /**
     * 连接超时时间（毫秒）
     */
    const val CONNECT_TIMEOUT_MS = 10_000L

    /**
     * 读取超时时间（毫秒）
     */
    const val READ_TIMEOUT_MS = 60_000L

    /**
     * 最大重试次数
     */
    const val MAX_RETRY_COUNT = 3

    /**
     * 重试间隔（毫秒）
     */
    const val RETRY_DELAY_MS = 1_000L

    /**
     * 进度更新间隔（毫秒）
     */
    const val PROGRESS_UPDATE_INTERVAL_MS = 500L

    /**
     * 支持的镜像格式
     */
    val SUPPORTED_FORMATS = setOf("qcow2", "img", "raw")

    /**
     * Debian镜像信息
     */
    object Debian {
        const val VERSION = "12"
        const val CODENAME = "bookworm"
        const val BASE_URL = "https://cloud.debian.org/images/cloud/bookworm/latest/"

        /**
         * 可用的镜像变体
         */
        object Variants {
            const val NOCLOUD_ARM64 = "debian-12-nocloud-arm64.qcow2"
            const val NOCLOUD_AMD64 = "debian-12-nocloud-amd64.qcow2"
            const val GENERIC_ARM64 = "debian-12-generic-arm64.qcow2"
            const val GENERIC_AMD64 = "debian-12-generic-amd64.qcow2"
        }
    }

    /**
     * Ubuntu镜像信息
     */
    object Ubuntu {
        const val VERSION = "22.04"
        const val CODENAME = "jammy"
        const val BASE_URL = "https://cloud-images.ubuntu.com/releases/22.04/release/"

        /**
         * 可用的镜像变体
         */
        object Variants {
            const val SERVER_ARM64 = "ubuntu-22.04-server-cloudimg-arm64.img"
            const val SERVER_AMD64 = "ubuntu-22.04-server-cloudimg-amd64.img"
        }
    }

    /**
     * 预定义镜像列表
     */
    val PREDEFINED_IMAGES = listOf(
        ImageInfo(
            id = DEFAULT_IMAGE_ID,
            name = "Debian 12 NoCloud ARM64",
            url = DEFAULT_IMAGE_URL,
            checksumType = DEFAULT_CHECKSUM_TYPE
        ),
        ImageInfo(
            id = "debian-12-nocloud-amd64",
            name = "Debian 12 NoCloud AMD64",
            url = "${Debian.BASE_URL}${Debian.Variants.NOCLOUD_AMD64}",
            checksumType = DEFAULT_CHECKSUM_TYPE
        ),
        ImageInfo(
            id = "ubuntu-22.04-server-arm64",
            name = "Ubuntu 22.04 Server ARM64",
            url = "${Ubuntu.BASE_URL}${Ubuntu.Variants.SERVER_ARM64}",
            checksumType = DEFAULT_CHECKSUM_TYPE
        )
    )
}
package com.dfa.core.vm.image

/**
 * 预定义镜像注册表
 *
 * 包含所有官方QCOW2镜像源的注册表，提供镜像元数据供用户选择下载
 */
object PredefinedImageRegistry {

    /**
     * 所有预定义镜像列表
     */
    val ALL_IMAGES: List<PredefinedImageSource> = listOf(
        // ==================== Debian 12 ====================
        PredefinedImageSource(
            id = "debian-12-nocloud-arm64",
            name = "Debian 12 (Bookworm) NoCloud ARM64",
            description = "Debian 12 Bookworm NoCloud版本，适用于ARM64架构，无需云初始化配置",
            url = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-nocloud-arm64.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 300L * 1024 * 1024, // ~300MB
            loginAccount = "root",
            isMinimal = false,
            tags = setOf("cloud", "nocloud", "stable"),
            officialUrl = "https://cloud.debian.org/images/cloud/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "debian-12-nocloud-amd64",
            name = "Debian 12 (Bookworm) NoCloud AMD64",
            description = "Debian 12 Bookworm NoCloud版本，适用于AMD64架构，无需云初始化配置",
            url = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-nocloud-amd64.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 350L * 1024 * 1024, // ~350MB
            loginAccount = "root",
            isMinimal = false,
            tags = setOf("cloud", "nocloud", "stable"),
            officialUrl = "https://cloud.debian.org/images/cloud/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "debian-12-generic-arm64",
            name = "Debian 12 (Bookworm) Generic ARM64",
            description = "Debian 12 Bookworm Generic版本，适用于ARM64架构，支持云初始化",
            url = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-generic-arm64.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 320L * 1024 * 1024, // ~320MB
            loginAccount = "root",
            isMinimal = false,
            tags = setOf("cloud", "generic", "stable", "docker"),
            officialUrl = "https://cloud.debian.org/images/cloud/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "debian-12-generic-amd64",
            name = "Debian 12 (Bookworm) Generic AMD64",
            description = "Debian 12 Bookworm Generic版本，适用于AMD64架构，支持云初始化",
            url = "https://cloud.debian.org/images/cloud/bookworm/latest/debian-12-generic-amd64.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.DEBIAN,
            osVersion = "12",
            estimatedSizeBytes = 370L * 1024 * 1024, // ~370MB
            loginAccount = "root",
            isMinimal = false,
            tags = setOf("cloud", "generic", "stable", "docker"),
            officialUrl = "https://cloud.debian.org/images/cloud/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Ubuntu 22.04 ====================
        PredefinedImageSource(
            id = "ubuntu-22.04-server-arm64",
            name = "Ubuntu 22.04 LTS (Jammy) Server ARM64",
            description = "Ubuntu 22.04 LTS Jammy Jellyfish服务器版本，适用于ARM64架构",
            url = "https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-arm64.img",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.UBUNTU,
            osVersion = "22.04",
            estimatedSizeBytes = 600L * 1024 * 1024, // ~600MB
            loginAccount = "ubuntu",
            isMinimal = false,
            tags = setOf("cloud", "lts", "server", "docker"),
            officialUrl = "https://cloud-images.ubuntu.com/releases/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "ubuntu-22.04-server-amd64",
            name = "Ubuntu 22.04 LTS (Jammy) Server AMD64",
            description = "Ubuntu 22.04 LTS Jammy Jellyfish服务器版本，适用于AMD64架构",
            url = "https://cloud-images.ubuntu.com/releases/22.04/release/ubuntu-22.04-server-cloudimg-amd64.img",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.UBUNTU,
            osVersion = "22.04",
            estimatedSizeBytes = 650L * 1024 * 1024, // ~650MB
            loginAccount = "ubuntu",
            isMinimal = false,
            tags = setOf("cloud", "lts", "server", "docker"),
            officialUrl = "https://cloud-images.ubuntu.com/releases/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Ubuntu 24.04 ====================
        PredefinedImageSource(
            id = "ubuntu-24.04-server-arm64",
            name = "Ubuntu 24.04 LTS (Noble) Server ARM64",
            description = "Ubuntu 24.04 LTS Noble Numbat服务器版本，适用于ARM64架构",
            url = "https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-arm64.img",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.UBUNTU,
            osVersion = "24.04",
            estimatedSizeBytes = 700L * 1024 * 1024, // ~700MB
            loginAccount = "ubuntu",
            isMinimal = false,
            tags = setOf("cloud", "lts", "server", "docker"),
            officialUrl = "https://cloud-images.ubuntu.com/releases/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "ubuntu-24.04-server-amd64",
            name = "Ubuntu 24.04 LTS (Noble) Server AMD64",
            description = "Ubuntu 24.04 LTS Noble Numbat服务器版本，适用于AMD64架构",
            url = "https://cloud-images.ubuntu.com/releases/24.04/release/ubuntu-24.04-server-cloudimg-amd64.img",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.UBUNTU,
            osVersion = "24.04",
            estimatedSizeBytes = 750L * 1024 * 1024, // ~750MB
            loginAccount = "ubuntu",
            isMinimal = false,
            tags = setOf("cloud", "lts", "server", "docker"),
            officialUrl = "https://cloud-images.ubuntu.com/releases/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Alpine Linux ====================
        PredefinedImageSource(
            id = "alpine-3.19-virt-arm64",
            name = "Alpine Linux 3.19 Virtual ARM64",
            description = "Alpine Linux 3.19虚拟化版本，适用于ARM64架构，轻量级镜像",
            url = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/aarch64/alpine-virt-3.19.0-aarch64.iso",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.ALPINE,
            osVersion = "3.19",
            estimatedSizeBytes = 50L * 1024 * 1024, // ~50MB
            loginAccount = "root",
            loginPassword = "",
            isMinimal = true,
            tags = setOf("minimal", "virtual", "lightweight", "docker"),
            officialUrl = "https://alpinelinux.org/downloads/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "alpine-3.19-virt-amd64",
            name = "Alpine Linux 3.19 Virtual AMD64",
            description = "Alpine Linux 3.19虚拟化版本，适用于AMD64架构，轻量级镜像",
            url = "https://dl-cdn.alpinelinux.org/alpine/v3.19/releases/x86_64/alpine-virt-3.19.0-x86_64.iso",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.ALPINE,
            osVersion = "3.19",
            estimatedSizeBytes = 55L * 1024 * 1024, // ~55MB
            loginAccount = "root",
            loginPassword = "",
            isMinimal = true,
            tags = setOf("minimal", "virtual", "lightweight", "docker"),
            officialUrl = "https://alpinelinux.org/downloads/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Fedora ====================
        PredefinedImageSource(
            id = "fedora-42-cloud-arm64",
            name = "Fedora 42 Cloud ARM64",
            description = "Fedora 42云版本，适用于ARM64架构",
            url = "https://download.fedoraproject.org/pub/fedora/linux/releases/42/Cloud/aarch64/images/Fedora-Cloud-Base-Generic-42-1.1.aarch64.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.FEDORA,
            osVersion = "42",
            estimatedSizeBytes = 400L * 1024 * 1024, // ~400MB
            loginAccount = "fedora",
            isMinimal = false,
            tags = setOf("cloud", "latest", "docker"),
            officialUrl = "https://fedoraproject.org/cloud/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "fedora-42-cloud-amd64",
            name = "Fedora 42 Cloud AMD64",
            description = "Fedora 42云版本，适用于AMD64架构",
            url = "https://download.fedoraproject.org/pub/fedora/linux/releases/42/Cloud/x86_64/images/Fedora-Cloud-Base-Generic-42-1.1.x86_64.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.FEDORA,
            osVersion = "42",
            estimatedSizeBytes = 450L * 1024 * 1024, // ~450MB
            loginAccount = "fedora",
            isMinimal = false,
            tags = setOf("cloud", "latest", "docker"),
            officialUrl = "https://fedoraproject.org/cloud/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Cirros (测试用) ====================
        PredefinedImageSource(
            id = "cirros-0.6.3-arm64",
            name = "Cirros 0.6.3 ARM64",
            description = "Cirros 0.6.3最小化测试镜像，适用于ARM64架构，仅用于测试目的",
            url = "https://download.cirros-cloud.net/0.6.3/cirros-0.6.3-aarch64-disk.img",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.CIRROS,
            osVersion = "0.6.3",
            estimatedSizeBytes = 20L * 1024 * 1024, // ~20MB
            loginAccount = "cirros",
            loginPassword = "gocubsgo",
            isMinimal = true,
            tags = setOf("minimal", "test", "debug"),
            officialUrl = "https://cirros-project.org/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "cirros-0.6.3-amd64",
            name = "Cirros 0.6.3 AMD64",
            description = "Cirros 0.6.3最小化测试镜像，适用于AMD64架构，仅用于测试目的",
            url = "https://download.cirros-cloud.net/0.6.3/cirros-0.6.3-x86_64-disk.img",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.CIRROS,
            osVersion = "0.6.3",
            estimatedSizeBytes = 18L * 1024 * 1024, // ~18MB
            loginAccount = "cirros",
            loginPassword = "gocubsgo",
            isMinimal = true,
            tags = setOf("minimal", "test", "debug"),
            officialUrl = "https://cirros-project.org/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== Rocky Linux ====================
        PredefinedImageSource(
            id = "rocky-9-cloud-arm64",
            name = "Rocky Linux 9 Cloud ARM64",
            description = "Rocky Linux 9云版本，适用于ARM64架构，RHEL兼容发行版",
            url = "https://download.rockylinux.org/pub/rocky/9/images/aarch64/Rocky-9-GenericCloud-Base.latest.aarch64.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.ROCKY,
            osVersion = "9",
            estimatedSizeBytes = 500L * 1024 * 1024, // ~500MB
            loginAccount = "rocky",
            isMinimal = false,
            tags = setOf("cloud", "rhel-compatible", "enterprise", "docker"),
            officialUrl = "https://rockylinux.org/download/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "rocky-9-cloud-amd64",
            name = "Rocky Linux 9 Cloud AMD64",
            description = "Rocky Linux 9云版本，适用于AMD64架构，RHEL兼容发行版",
            url = "https://download.rockylinux.org/pub/rocky/9/images/x86_64/Rocky-9-GenericCloud-Base.latest.x86_64.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.ROCKY,
            osVersion = "9",
            estimatedSizeBytes = 550L * 1024 * 1024, // ~550MB
            loginAccount = "rocky",
            isMinimal = false,
            tags = setOf("cloud", "rhel-compatible", "enterprise", "docker"),
            officialUrl = "https://rockylinux.org/download/",
            lastVerified = System.currentTimeMillis()
        ),

        // ==================== AlmaLinux ====================
        PredefinedImageSource(
            id = "alma-9-cloud-arm64",
            name = "AlmaLinux 9 Cloud ARM64",
            description = "AlmaLinux 9云版本，适用于ARM64架构，RHEL兼容发行版",
            url = "https://repo.almalinux.org/almalinux/9/cloud/aarch64/images/AlmaLinux-9-GenericCloud-latest.aarch64.qcow2",
            architecture = ImageArchitecture.ARM64,
            osType = OsType.ALMALINUX,
            osVersion = "9",
            estimatedSizeBytes = 480L * 1024 * 1024, // ~480MB
            loginAccount = "almalinux",
            isMinimal = false,
            tags = setOf("cloud", "rhel-compatible", "enterprise", "docker"),
            officialUrl = "https://almalinux.org/cloud/",
            lastVerified = System.currentTimeMillis()
        ),
        PredefinedImageSource(
            id = "alma-9-cloud-amd64",
            name = "AlmaLinux 9 Cloud AMD64",
            description = "AlmaLinux 9云版本，适用于AMD64架构，RHEL兼容发行版",
            url = "https://repo.almalinux.org/almalinux/9/cloud/x86_64/images/AlmaLinux-9-GenericCloud-latest.x86_64.qcow2",
            architecture = ImageArchitecture.AMD64,
            osType = OsType.ALMALINUX,
            osVersion = "9",
            estimatedSizeBytes = 530L * 1024 * 1024, // ~530MB
            loginAccount = "almalinux",
            isMinimal = false,
            tags = setOf("cloud", "rhel-compatible", "enterprise", "docker"),
            officialUrl = "https://almalinux.org/cloud/",
            lastVerified = System.currentTimeMillis()
        )
    )

    /**
     * 按架构分组的镜像
     */
    val BY_ARCHITECTURE: Map<ImageArchitecture, List<PredefinedImageSource>> by lazy {
        ALL_IMAGES.groupBy { it.architecture }
    }

    /**
     * 按操作系统类型分组的镜像
     */
    val BY_OS_TYPE: Map<OsType, List<PredefinedImageSource>> by lazy {
        ALL_IMAGES.groupBy { it.osType }
    }

    /**
     * 推荐镜像列表（用于Docker等场景）
     */
    val RECOMMENDED_IMAGES: List<PredefinedImageSource> by lazy {
        ALL_IMAGES.filter { it.isRecommendedForDocker }
    }

    /**
     * 最小镜像列表
     */
    val MINIMAL_IMAGES: List<PredefinedImageSource> by lazy {
        ALL_IMAGES.filter { it.isMinimal }
    }

    /**
     * 根据ID获取镜像
     *
     * @param id 镜像ID
     * @return 镜像源，如果不存在则返回null
     */
    fun getById(id: String): PredefinedImageSource? {
        return ALL_IMAGES.find { it.id == id }
    }

    /**
     * 搜索镜像
     *
     * @param query 搜索关键词
     * @return 匹配的镜像列表
     */
    fun search(query: String): List<PredefinedImageSource> {
        val lowerQuery = query.lowercase()
        return ALL_IMAGES.filter { image ->
            image.name.lowercase().contains(lowerQuery) ||
            image.description.lowercase().contains(lowerQuery) ||
            image.osType.name.lowercase().contains(lowerQuery) ||
            image.tags.any { it.lowercase().contains(lowerQuery) }
        }
    }

    /**
     * 获取默认镜像（Debian 12 NoCloud ARM64）
     */
    val DEFAULT_IMAGE: PredefinedImageSource
        get() = getById("debian-12-nocloud-arm64") ?: ALL_IMAGES.first()
}
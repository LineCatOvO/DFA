package com.dfa.core.vm.termux

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * TermuxPackageManager单元测试
 *
 * 测试Termux包管理器接口、状态管理和操作结果模型
 */
class TermuxPackageManagerTest {

    // ==================== PackageManagerState测试 ====================

    @Test
    fun `PackageManagerState Idle should be data object`() {
        val state = PackageManagerState.Idle

        assertThat(state).isInstanceOf(PackageManagerState.Idle::class.java)
    }

    @Test
    fun `PackageManagerState LoadingPackages should be data object`() {
        val state = PackageManagerState.LoadingPackages

        assertThat(state).isInstanceOf(PackageManagerState.LoadingPackages::class.java)
    }

    @Test
    fun `PackageManagerState Executing should contain operation name`() {
        val state = PackageManagerState.Executing("install")

        assertThat(state.operation).isEqualTo("install")
    }

    @Test
    fun `PackageManagerState Completed should contain result`() {
        val result = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.INSTALL,
            packageName = "test-package"
        )
        val state = PackageManagerState.Completed(result)

        assertThat(state.result).isEqualTo(result)
    }

    @Test
    fun `PackageManagerState Error should contain message and optional exception`() {
        val exception = RuntimeException("Test error")
        val state = PackageManagerState.Error("Error message", exception)

        assertThat(state.message).isEqualTo("Error message")
        assertThat(state.exception).isEqualTo(exception)
    }

    // ==================== PackageOperationProgress测试 ====================

    @Test
    fun `PackageOperationProgress should have correct default values`() {
        val progress = PackageOperationProgress(
            operation = "install"
        )

        assertThat(progress.operation).isEqualTo("install")
        assertThat(progress.currentPackage).isNull()
        assertThat(progress.totalPackages).isEqualTo(0)
        assertThat(progress.processedPackages).isEqualTo(0)
        assertThat(progress.percentage).isEqualTo(0)
        assertThat(progress.phase).isEqualTo(PackageOperationProgress.OperationPhase.PREPARING)
    }

    @Test
    fun `PackageOperationProgress should calculate progress correctly`() {
        val progress = PackageOperationProgress(
            operation = "install",
            totalPackages = 10,
            processedPackages = 5,
            downloadProgress = 1024 * 1024,
            downloadTotal = 10 * 1024 * 1024,
            phase = PackageOperationProgress.OperationPhase.DOWNLOADING
        )

        assertThat(progress.totalPackages).isEqualTo(10)
        assertThat(progress.processedPackages).isEqualTo(5)
        assertThat(progress.downloadProgress).isEqualTo(1024 * 1024)
        assertThat(progress.phase).isEqualTo(PackageOperationProgress.OperationPhase.DOWNLOADING)
    }

    @Test
    fun `PackageOperationProgress OperationPhase should have all expected values`() {
        assertThat(PackageOperationProgress.OperationPhase.values()).asList().containsExactly(
            PackageOperationProgress.OperationPhase.PREPARING,
            PackageOperationProgress.OperationPhase.DOWNLOADING,
            PackageOperationProgress.OperationPhase.INSTALLING,
            PackageOperationProgress.OperationPhase.CONFIGURING,
            PackageOperationProgress.OperationPhase.COMPLETED,
            PackageOperationProgress.OperationPhase.FAILED
        )
    }

    // ==================== PackageOperationResult测试 ====================

    @Test
    fun `PackageOperationResult success should create successful result`() {
        val result = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.INSTALL,
            packageName = "test-package",
            message = "Installed successfully",
            executionTimeMs = 1000,
            output = "output"
        )

        assertThat(result.isSuccess).isTrue()
        assertThat(result.operationType).isEqualTo(PackageOperationResult.OperationType.INSTALL)
        assertThat(result.packageName).isEqualTo("test-package")
        assertThat(result.message).isEqualTo("Installed successfully")
        assertThat(result.error).isNull()
        assertThat(result.executionTimeMs).isEqualTo(1000)
    }

    @Test
    fun `PackageOperationResult failure should create failed result`() {
        val result = PackageOperationResult.failure(
            operationType = PackageOperationResult.OperationType.INSTALL,
            packageName = "test-package",
            error = "Installation failed",
            executionTimeMs = 500,
            output = "error output"
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.error).isEqualTo("Installation failed")
    }

    @Test
    fun `PackageOperationResult OperationType should have all expected values`() {
        assertThat(PackageOperationResult.OperationType.values()).asList().containsExactly(
            PackageOperationResult.OperationType.INSTALL,
            PackageOperationResult.OperationType.UNINSTALL,
            PackageOperationResult.OperationType.UPDATE,
            PackageOperationResult.OperationType.UPGRADE_ALL,
            PackageOperationResult.OperationType.UPDATE_INDEX,
            PackageOperationResult.OperationType.REINSTALL,
            PackageOperationResult.OperationType.CLEAN_CACHE,
            PackageOperationResult.OperationType.AUTOREMOVE,
            PackageOperationResult.OperationType.FIX_DEPENDENCIES
        )
    }

    // ==================== BatchOperationResult测试 ====================

    @Test
    fun `BatchOperationResult should track succeeded and failed packages`() {
        val result = BatchOperationResult(
            isSuccess = false,
            operationType = PackageOperationResult.OperationType.INSTALL,
            succeeded = listOf("package1", "package2"),
            failed = listOf(
                BatchOperationResult.FailedPackage("package3", "Network error")
            ),
            executionTimeMs = 5000
        )

        assertThat(result.isSuccess).isFalse()
        assertThat(result.succeeded).containsExactly("package1", "package2")
        assertThat(result.failed).hasSize(1)
        assertThat(result.failed[0].name).isEqualTo("package3")
        assertThat(result.failed[0].error).isEqualTo("Network error")
    }

    @Test
    fun `BatchOperationResult successRate should calculate correctly`() {
        val result = BatchOperationResult(
            isSuccess = false,
            operationType = PackageOperationResult.OperationType.INSTALL,
            succeeded = listOf("p1", "p2", "p3"),
            failed = listOf(
                BatchOperationResult.FailedPackage("p4", "error")
            ),
            executionTimeMs = 1000
        )

        assertThat(result.successRate).isWithin(0.01f).of(0.75f)
    }

    @Test
    fun `BatchOperationResult successRate should be 1 when all succeed`() {
        val result = BatchOperationResult(
            isSuccess = true,
            operationType = PackageOperationResult.OperationType.INSTALL,
            succeeded = listOf("p1", "p2"),
            failed = emptyList(),
            executionTimeMs = 1000
        )

        assertThat(result.successRate).isEqualTo(1.0f)
    }

    @Test
    fun `BatchOperationResult successRate should be 0 when all fail`() {
        val result = BatchOperationResult(
            isSuccess = false,
            operationType = PackageOperationResult.OperationType.INSTALL,
            succeeded = emptyList(),
            failed = listOf(
                BatchOperationResult.FailedPackage("p1", "error")
            ),
            executionTimeMs = 1000
        )

        assertThat(result.successRate).isEqualTo(0.0f)
    }

    // ==================== PackageDetailedInfo测试 ====================

    @Test
    fun `PackageDetailedInfo should have all properties`() {
        val info = PackageDetailedInfo(
            name = "test-package",
            version = "1.0.0",
            description = "Test package",
            isInstalled = true,
            architecture = "aarch64",
            dependencies = listOf("dep1"),
            recommends = listOf("rec1"),
            suggests = listOf("sug1"),
            reverseDependencies = listOf("rdep1"),
            installedSize = 1024 * 1024,
            downloadSize = 512 * 1024,
            maintainer = "Developer <dev@example.com>",
            homepage = "https://example.com",
            license = "MIT",
            repository = "main",
            installStatus = PackageDetailedInfo.InstallStatus.INSTALLED,
            installPath = "/data/data/com.termux/files/usr"
        )

        assertThat(info.name).isEqualTo("test-package")
        assertThat(info.version).isEqualTo("1.0.0")
        assertThat(info.maintainer).isEqualTo("Developer <dev@example.com>")
        assertThat(info.installStatus).isEqualTo(PackageDetailedInfo.InstallStatus.INSTALLED)
    }

    @Test
    fun `PackageDetailedInfo formatInstalledSize should format correctly`() {
        val info = PackageDetailedInfo(
            name = "test",
            version = "1.0",
            installedSize = 1024 * 1024 * 100 // 100 MB
        )

        assertThat(info.formatInstalledSize()).isEqualTo("100 MB")
    }

    @Test
    fun `PackageDetailedInfo formatDownloadSize should format correctly`() {
        val info = PackageDetailedInfo(
            name = "test",
            version = "1.0",
            downloadSize = 512 * 1024 // 512 KB
        )

        assertThat(info.formatDownloadSize()).isEqualTo("512 KB")
    }

    @Test
    fun `PackageDetailedInfo InstallStatus should have all expected values`() {
        assertThat(PackageDetailedInfo.InstallStatus.values()).asList().containsExactly(
            PackageDetailedInfo.InstallStatus.NOT_INSTALLED,
            PackageDetailedInfo.InstallStatus.INSTALLED,
            PackageDetailedInfo.InstallStatus.UPGRADABLE,
            PackageDetailedInfo.InstallStatus.BROKEN,
            PackageDetailedInfo.InstallStatus.HALF_INSTALLED
        )
    }

    // ==================== RepositoryInfo测试 ====================

    @Test
    fun `RepositoryInfo should have correct properties`() {
        val repo = RepositoryInfo(
            name = "termux-main",
            url = "https://packages.termux.org/termux-main",
            isEnabled = true,
            type = RepositoryInfo.RepositoryType.MAIN,
            architecture = "aarch64",
            components = listOf("main"),
            packageCount = 1000
        )

        assertThat(repo.name).isEqualTo("termux-main")
        assertThat(repo.url).isEqualTo("https://packages.termux.org/termux-main")
        assertThat(repo.isEnabled).isTrue()
        assertThat(repo.type).isEqualTo(RepositoryInfo.RepositoryType.MAIN)
        assertThat(repo.packageCount).isEqualTo(1000)
    }

    @Test
    fun `RepositoryInfo RepositoryType should have all expected values`() {
        assertThat(RepositoryInfo.RepositoryType.values()).asList().containsExactly(
            RepositoryInfo.RepositoryType.MAIN,
            RepositoryInfo.RepositoryType.X11,
            RepositoryInfo.RepositoryType.ROOT,
            RepositoryInfo.RepositoryType.UNSTABLE,
            RepositoryInfo.RepositoryType.CUSTOM
        )
    }

    // ==================== PackageVerificationResult测试 ====================

    @Test
    fun `PackageVerificationResult should track issues`() {
        val result = PackageVerificationResult(
            name = "test-package",
            isValid = false,
            issues = listOf("Missing file", "Checksum mismatch"),
            missingFiles = listOf("/usr/bin/test"),
            modifiedFiles = listOf("/etc/config")
        )

        assertThat(result.isValid).isFalse()
        assertThat(result.issues).containsExactly("Missing file", "Checksum mismatch")
        assertThat(result.missingFiles).containsExactly("/usr/bin/test")
        assertThat(result.modifiedFiles).containsExactly("/etc/config")
    }

    @Test
    fun `PackageVerificationResult valid should have no issues`() {
        val result = PackageVerificationResult(
            name = "valid-package",
            isValid = true
        )

        assertThat(result.isValid).isTrue()
        assertThat(result.issues).isEmpty()
        assertThat(result.missingFiles).isEmpty()
        assertThat(result.modifiedFiles).isEmpty()
    }

    // ==================== TermuxPackageManager接口测试（Mock） ====================

    @Test
    fun `TermuxPackageManager state should return StateFlow`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val stateFlow = MutableStateFlow<PackageManagerState>(PackageManagerState.Idle)
        every { mockManager.state } returns stateFlow

        assertThat(mockManager.state.value).isEqualTo(PackageManagerState.Idle)
    }

    @Test
    fun `TermuxPackageManager progress should return StateFlow`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val progressFlow = MutableStateFlow<PackageOperationProgress?>(null)
        every { mockManager.progress } returns progressFlow

        assertThat(mockManager.progress.value).isNull()
    }

    @Test
    fun `TermuxPackageManager listPackages should return Result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val packages = listOf(
            PackageInfo("package1", "1.0.0"),
            PackageInfo("package2", "2.0.0")
        )
        coEvery { mockManager.listPackages() } returns Result.success(packages)

        val result = mockManager.listPackages()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(2)
    }

    @Test
    fun `TermuxPackageManager searchPackages should return matching packages`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val packages = listOf(PackageInfo("python", "3.11.0"))
        coEvery { mockManager.searchPackages("python") } returns Result.success(packages)

        val result = mockManager.searchPackages("python")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.first()?.name).isEqualTo("python")
    }

    @Test
    fun `TermuxPackageManager listInstalledPackages should return installed packages`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val packages = listOf(
            PackageInfo("bash", "5.0", isInstalled = true),
            PackageInfo("coreutils", "9.0", isInstalled = true)
        )
        coEvery { mockManager.listInstalledPackages() } returns Result.success(packages)

        val result = mockManager.listInstalledPackages()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.all { it.isInstalled }).isTrue()
    }

    @Test
    fun `TermuxPackageManager isPackageInstalled should return boolean`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        coEvery { mockManager.isPackageInstalled("bash") } returns true
        coEvery { mockManager.isPackageInstalled("nonexistent") } returns false

        assertThat(mockManager.isPackageInstalled("bash")).isTrue()
        assertThat(mockManager.isPackageInstalled("nonexistent")).isFalse()
    }

    @Test
    fun `TermuxPackageManager install should return result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val successResult = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.INSTALL,
            packageName = "test-package"
        )
        coEvery { mockManager.install("test-package") } returns Result.success(successResult)

        val result = mockManager.install("test-package")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.isSuccess).isTrue()
    }

    @Test
    fun `TermuxPackageManager uninstall should return result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val successResult = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.UNINSTALL,
            packageName = "test-package"
        )
        coEvery { mockManager.uninstall("test-package", false) } returns Result.success(successResult)

        val result = mockManager.uninstall("test-package")

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `TermuxPackageManager upgradeAll should return result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val successResult = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.UPGRADE_ALL,
            packageName = "all"
        )
        coEvery { mockManager.upgradeAll() } returns Result.success(successResult)

        val result = mockManager.upgradeAll()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `TermuxPackageManager updatePackageIndex should return result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val successResult = PackageOperationResult.success(
            operationType = PackageOperationResult.OperationType.UPDATE_INDEX,
            packageName = ""
        )
        coEvery { mockManager.updatePackageIndex() } returns Result.success(successResult)

        val result = mockManager.updatePackageIndex()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `TermuxPackageManager listRepositories should return repositories`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        val repos = listOf(
            RepositoryInfo("main", "https://packages.termux.org/main", true)
        )
        coEvery { mockManager.listRepositories() } returns Result.success(repos)

        val result = mockManager.listRepositories()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(1)
    }

    @Test
    fun `TermuxPackageManager cleanCache should return success`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        coEvery { mockManager.cleanCache() } returns Result.success(Unit)

        val result = mockManager.cleanCache()

        assertThat(result.isSuccess).isTrue()
    }

    @Test
    fun `TermuxPackageManager getCacheSize should return size`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        coEvery { mockManager.getCacheSize() } returns Result.success(1024 * 1024 * 100L)

        val result = mockManager.getCacheSize()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1024 * 1024 * 100L)
    }

    @Test
    fun `TermuxPackageManager fixBrokenDependencies should return result`() = runTest {
        val mockManager = mockk<TermuxPackageManager>()
        coEvery { mockManager.fixBrokenDependencies() } returns Result.success(Unit)

        val result = mockManager.fixBrokenDependencies()

        assertThat(result.isSuccess).isTrue()
    }
}
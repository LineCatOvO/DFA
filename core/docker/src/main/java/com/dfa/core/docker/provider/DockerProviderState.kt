package com.dfa.core.docker.provider

/**
 * Docker Provider状态枚举
 *
 * 定义Docker Provider的生命周期状态。
 *
 * @since 1.0.0
 */
enum class DockerProviderState {
    /**
     * 初始状态
     *
     * Provider已创建但尚未初始化。
     */
    CREATED,

    /**
     * 初始化中
     *
     * Provider正在执行初始化操作。
     */
    INITIALIZING,

    /**
     * 已初始化
     *
     * Provider初始化完成，可以启动。
     */
    INITIALIZED,

    /**
     * 启动中
     *
     * Provider正在启动。
     */
    STARTING,

    /**
     * 运行中
     *
     * Provider正在运行，可以提供服务。
     */
    RUNNING,

    /**
     * 停止中
     *
     * Provider正在停止。
     */
    STOPPING,

    /**
     * 已停止
     *
     * Provider已停止，可以重新启动或销毁。
     */
    STOPPED,

    /**
     * 销毁中
     *
     * Provider正在销毁资源。
     */
    DESTROYING,

    /**
     * 已销毁
     *
     * Provider资源已释放，无法再使用。
     */
    DESTROYED,

    /**
     * 错误状态
     *
     * Provider遇到错误，需要处理。
     */
    ERROR;

    /**
     * 检查是否为活跃状态
     *
     * @return 如果Provider正在运行或可以提供服务返回true
     */
    fun isActive(): Boolean = this == RUNNING || this == INITIALIZED

    /**
     * 检查是否为过渡状态
     *
     * @return 如果Provider正在执行状态转换返回true
     */
    fun isTransitioning(): Boolean = this in listOf(
        INITIALIZING, STARTING, STOPPING, DESTROYING
    )

    /**
     * 检查是否为终态
     *
     * @return 如果Provider已达到终态返回true
     */
    fun isTerminal(): Boolean = this == DESTROYED

    /**
     * 检查是否可以启动
     *
     * @return 如果可以从当前状态启动返回true
     */
    fun canStart(): Boolean = this == INITIALIZED || this == STOPPED

    /**
     * 检查是否可以停止
     *
     * @return 如果可以从当前状态停止返回true
     */
    fun canStop(): Boolean = this == RUNNING

    /**
     * 检查是否可以销毁
     *
     * @return 如果可以从当前状态销毁返回true
     */
    fun canDestroy(): Boolean = this in listOf(CREATED, INITIALIZED, STOPPED, ERROR)
}
#!/bin/bash
# DFA Docker Build Script
# Build and push Docker images for Android CI/CD

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
IMAGE_NAME="dfa-android-builder"
REGISTRY="${DOCKER_REGISTRY:-ghcr.io}"
IMAGE_TAG="${1:-latest}"
FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

echo "========================================"
echo "DFA Docker Build Script"
echo "========================================"
echo "Image: ${FULL_IMAGE_NAME}"
echo "Registry: ${REGISTRY}"
echo "========================================"

build_image() {
    echo "[INFO] Building Docker image..."
    docker build \
        -t "${FULL_IMAGE_NAME}" \
        -f "${SCRIPT_DIR}/Dockerfile" \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        "${SCRIPT_DIR}"
    
    if [ $? -eq 0 ]; then
        echo "[SUCCESS] Image built successfully: ${FULL_IMAGE_NAME}"
    else
        echo "[ERROR] Failed to build image"
        exit 1
    fi
}

push_image() {
    echo "[INFO] Pushing Docker image to registry..."
    
    if [ -z "${DOCKER_USERNAME}" ] || [ -z "${DOCKER_PASSWORD}" ]; then
        echo "[WARN] DOCKER_USERNAME or DOCKER_PASSWORD not set"
        echo "[INFO] Skipping push. To push, set these environment variables."
        return 0
    fi
    
    echo "${DOCKER_PASSWORD}" | docker login "${REGISTRY}" -u "${DOCKER_USERNAME}" --password-stdin
    
    docker push "${FULL_IMAGE_NAME}"
    
    if [ $? -eq 0 ]; then
        echo "[SUCCESS] Image pushed successfully: ${FULL_IMAGE_NAME}"
    else
        echo "[ERROR] Failed to push image"
        exit 1
    fi
}

test_image() {
    echo "[INFO] Testing Docker image..."
    
    docker run --rm \
        -v "${PROJECT_ROOT}:/workspace:ro" \
        "${FULL_IMAGE_NAME}" \
        -c "java -version && gradle --version && sdkmanager --list | head -20"
    
    if [ $? -eq 0 ]; then
        echo "[SUCCESS] Image test passed"
    else
        echo "[ERROR] Image test failed"
        exit 1
    fi
}

show_usage() {
    echo "Usage: $0 [TAG]"
    echo ""
    echo "Arguments:"
    echo "  TAG    Docker image tag (default: latest)"
    echo ""
    echo "Environment Variables:"
    echo "  DOCKER_REGISTRY   Docker registry URL (default: ghcr.io)"
    echo "  DOCKER_USERNAME   Docker registry username"
    echo "  DOCKER_PASSWORD   Docker registry password"
    echo ""
    echo "Examples:"
    echo "  $0 latest              # Build with tag 'latest'"
    echo "  $0 v1.0.0              # Build with tag 'v1.0.0'"
    echo "  $0 \$(git rev-parse --short HEAD)  # Build with git commit hash"
}

main() {
    if [ "$1" = "-h" ] || [ "$1" = "--help" ]; then
        show_usage
        exit 0
    fi
    
    build_image
    test_image
    
    if [ "${SKIP_PUSH:-false}" != "true" ]; then
        push_image
    fi
    
    echo "========================================"
    echo "[DONE] Docker build process completed"
    echo "========================================"
}

main "$@"

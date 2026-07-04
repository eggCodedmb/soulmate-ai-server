#!/bin/bash
# =============================================
# SoulMate AI - Server 2 一键部署脚本
# 用途: 从 GitHub 拉取代码 → 构建 Docker 镜像 → 启动服务
# 使用: bash deploy-server2.sh
# =============================================

set -e

# ==================== 配置 ====================
REPO_URL="https://github.com/eggCodedmb/soulmate-ai-server.git"
BRANCH="main"
DEPLOY_DIR="/opt/soulmate-ai"
COMPOSE_FILE="docker-compose.server2.yml"
ENV_FILE=".env.server2"

# ==================== 颜色输出 ====================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# ==================== 前置检查 ====================
check_deps() {
    log_info "检查依赖..."

    if ! command -v git &> /dev/null; then
        log_error "git 未安装，正在安装..."
        apt-get update && apt-get install -y git
    fi

    if ! command -v docker &> /dev/null; then
        log_error "docker 未安装，请先安装 Docker"
        exit 1
    fi

    if ! docker compose version &> /dev/null; then
        log_error "docker compose 未安装，请先安装 Docker Compose V2"
        exit 1
    fi

    log_info "依赖检查通过"
}

# ==================== 克隆或更新代码 ====================
sync_code() {
    if [ -d "$DEPLOY_DIR/.git" ]; then
        log_info "代码已存在，拉取最新代码..."
        cd "$DEPLOY_DIR"
        git fetch origin "$BRANCH"
        LOCAL=$(git rev-parse HEAD)
        REMOTE=$(git rev-parse "origin/$BRANCH")

        if [ "$LOCAL" = "$REMOTE" ]; then
            log_info "代码已是最新，跳过 pull"
        else
            git reset --hard "origin/$BRANCH"
            log_info "代码已更新到最新"
        fi
    else
        log_info "首次部署，克隆仓库..."
        rm -rf "$DEPLOY_DIR"
        git clone -b "$BRANCH" "$REPO_URL" "$DEPLOY_DIR"
        cd "$DEPLOY_DIR"
        log_info "仓库克隆完成"
    fi
}

# ==================== 复制环境变量 ====================
setup_env() {
    cd "$DEPLOY_DIR"

    if [ -f ".env" ]; then
        log_info ".env 已存在，跳过（如需更新请手动编辑 .env）"
        return
    fi

    if [ -f "$ENV_FILE" ]; then
        cp "$ENV_FILE" .env
        log_info "已从 $ENV_FILE 创建 .env"
    else
        log_error "$ENV_FILE 不存在！"
        log_warn "请先将 .env.server2 文件 SCP 到 $DEPLOY_DIR/"
        log_warn "scp .env.server2 root@39.108.137.45:$DEPLOY_DIR/"
        exit 1
    fi
}

# ==================== 下载模型 ====================
setup_model() {
    MODEL_DIR="/opt/soulmate-ai/models/e5-small-v2"

    if [ -d "$MODEL_DIR" ] && [ -f "$MODEL_DIR/model.onnx" ]; then
        log_info "嵌入模型已存在，跳过下载"
        return
    fi

    log_info "下载嵌入模型 e5-small-v2..."
    mkdir -p "$MODEL_DIR"

    # 从 Hugging Face 下载
    MODEL_BASE="https://huggingface.co/intfloat/multilingual-e5-small/resolve/main"

    curl -L -o "$MODEL_DIR/tokenizer.json" "$MODEL_BASE/tokenizer.json" && \
    curl -L -o "$MODEL_DIR/tokenizer_config.json" "$MODEL_BASE/tokenizer_config.json" && \
    curl -L -o "$MODEL_DIR/config.json" "$MODEL_BASE/config.json" && \
    curl -L -o "$MODEL_DIR/model.onnx" "$MODEL_BASE/model.onnx"

    if [ $? -eq 0 ]; then
        log_info "模型下载完成"
    else
        log_warn "模型下载失败，请手动下载到 $MODEL_DIR"
    fi
}

# ==================== 部署服务 ====================
deploy() {
    cd "$DEPLOY_DIR"
    log_info "构建 Docker 镜像..."

    docker compose -f "$COMPOSE_FILE" --env-file .env build --no-cache

    log_info "停止旧容器..."
    docker compose -f "$COMPOSE_FILE" --env-file .env down

    log_info "启动新容器..."
    docker compose -f "$COMPOSE_FILE" --env-file .env up -d

    log_info "等待服务启动..."
    sleep 10

    # 健康检查
    RETRIES=12
    while [ $RETRIES -gt 0 ]; do
        if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_info "服务启动成功！"
            return 0
        fi
        RETRIES=$((RETRIES - 1))
        log_warn "等待服务就绪... (剩余 $RETRIES 次重试)"
        sleep 5
    done

    log_error "服务启动超时，请检查日志: docker compose -f $COMPOSE_FILE logs soulmate-app"
    return 1
}

# ==================== 打印状态 ====================
show_status() {
    cd "$DEPLOY_DIR"
    echo ""
    echo "=========================================="
    docker compose -f "$COMPOSE_FILE" ps
    echo "=========================================="
    echo ""
    log_info "部署目录: $DEPLOY_DIR"
    log_info "应用地址: http://39.108.137.45"
    log_info "健康检查: curl http://localhost:8080/actuator/health"
    log_info "查看日志: docker compose -f $COMPOSE_FILE logs -f soulmate-app"
}

# ==================== 清理旧镜像 ====================
cleanup() {
    log_info "清理悬空镜像..."
    docker image prune -f 2>/dev/null || true
}

# ==================== 主流程 ====================
main() {
    echo "=========================================="
    echo "  SoulMate AI - Server 2 部署"
    echo "=========================================="
    echo ""

    check_deps
    sync_code
    setup_env
    setup_model
    deploy
    show_status
    cleanup

    echo ""
    log_info "部署完成!"
}

main "$@"

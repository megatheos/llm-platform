# LLM 语言学习平台

基于大语言模型的智能语言学习平台，提供单词查询、情景对话、语言测验等功能。

## 功能特性

- **单词查询** - AI 生成单词释义、例句和翻译，支持多语言
- **情景对话** - 与 AI 进行角色扮演对话练习，提供多种预设场景
- **语言测验** - AI 生成选择题，支持不同难度级别
- **学习记录** - 追踪学习进度和历史记录
- **多语言界面** - 支持中文、英文、日文、韩文

## 技术栈

### 后端
- Java 17 + Spring Boot 3.2
- MyBatis-Plus
- MySQL 8.0
- Redis 7
- JWT 认证

### 前端
- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia
- Vue Router
- Vue I18n

### AI 服务
- DeepSeek API（默认）
- OpenAI API（可选）
- Ollama 本地模型（可选）

### 部署
- Docker + Docker Compose
- Nginx（生产环境）

## 快速开始

### 环境要求

- Docker 20.10+
- Docker Compose 2.0+

### 配置

1. 复制环境变量文件：
```bash
cp .env.example .env
```

2. 编辑 `.env` 文件，配置 AI 服务：
```env
# DeepSeek（推荐）
DEEPSEEK_ENABLED=true
DEEPSEEK_API_KEY=your_api_key

# 或使用 Ollama 本地模型
OLLAMA_ENABLED=true
OLLAMA_BASE_URL=http://host.docker.internal:11434
OLLAMA_MODEL=qwen2.5:7b
```

### 开发环境

```bash
docker compose -f docker-compose.dev.yml up -d
```

访问：
- 前端：http://localhost
- 后端 API：http://localhost:8080

### 生产环境

```bash
docker compose up -d --build
```

## 项目结构

```
├── backend/                 # 后端 Spring Boot 项目
│   ├── src/main/java/      # Java 源码
│   ├── src/main/resources/ # 配置文件和 SQL
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               # 前端 Vue 项目
│   ├── src/
│   │   ├── api/           # API 接口
│   │   ├── components/    # 组件
│   │   ├── views/         # 页面
│   │   ├── stores/        # Pinia 状态管理
│   │   ├── i18n/          # 国际化
│   │   └── router/        # 路由
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml      # 生产环境配置
├── docker-compose.dev.yml  # 开发环境配置
└── .env.example           # 环境变量示例
```

## API 接口

| 模块 | 接口 | 说明 |
|------|------|------|
| 认证 | POST /api/auth/register | 用户注册 |
| 认证 | POST /api/auth/login | 用户登录 |
| 认证 | GET /api/auth/me | 获取当前用户 |
| 单词 | POST /api/words/query | 查询单词 |
| 单词 | GET /api/words/history | 查询历史 |
| 对话 | GET /api/dialogue/scenarios | 获取场景列表 |
| 对话 | POST /api/dialogue/sessions | 创建对话会话 |
| 对话 | POST /api/dialogue/sessions/{id}/messages | 发送消息 |
| 测验 | POST /api/quiz/generate | 生成测验 |
| 测验 | POST /api/quiz/{id}/submit | 提交答案 |
| 记录 | GET /api/records | 学习记录 |
| 记录 | GET /api/records/statistics | 学习统计 |

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| MYSQL_DATABASE | 数据库名 | llm_learning |
| MYSQL_USERNAME | 数据库用户 | llm_user |
| MYSQL_PASSWORD | 数据库密码 | llm_password |
| JWT_SECRET | JWT 密钥 | - |
| DEEPSEEK_ENABLED | 启用 DeepSeek | true |
| DEEPSEEK_API_KEY | DeepSeek API Key | - |
| OLLAMA_ENABLED | 启用 Ollama | false |
| OLLAMA_BASE_URL | Ollama 地址 | http://ollama:11434 |
| OLLAMA_MODEL | Ollama 模型 | qwen2.5:7b |

## 常用命令

```bash
# 查看容器状态
docker compose ps

# 查看日志
docker compose logs -f backend
docker compose logs -f frontend

# 重启服务
docker compose restart backend

# 停止所有服务
docker compose down

# 清除数据重建
docker compose down -v
docker compose up -d --build
```

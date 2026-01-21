# LLM 语言学习平台

基于大语言模型的智能语言学习平台，提供个性化学习、间隔重复记忆、智能学习计划、AI内容生成和激励系统等功能。

## 功能特性

### 核心学习功能
- **单词查询** - AI 生成单词释义、例句和翻译，支持多语言
- **情景对话** - 与 AI 进行角色扮演对话练习，提供多种预设场景
- **语言测验** - AI 生成选择题，支持不同难度级别
- **学习记录** - 追踪学习进度和历史记录

### 个性化学习系统
- **间隔重复记忆** - 基于艾宾浩斯遗忘曲线的科学记忆算法，智能安排复习时间
- **学习档案分析** - 分析学习行为、识别薄弱环节、生成学习洞察报告
- **智能学习计划** - 根据目标和表现动态生成个性化学习路径和每日任务
- **AI内容生成** - 根据掌握度水平生成定制化例句、练习题和对话场景
- **激励系统** - 成就徽章、学习连续记录、进度可视化，保持学习动力
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
│   │   └── com/llmplatform/
│   │       ├── ai/         # AI 网关和提供商
│   │       ├── personalized/ # 个性化学习系统
│   │       │   ├── engine/      # 核心引擎
│   │       │   │   ├── SpacedRepetitionEngine.java      # 间隔重复引擎
│   │       │   │   ├── LearningAnalyticsEngine.java     # 学习分析引擎
│   │       │   │   └── PlanOptimizerEngine.java        # 计划优化引擎
│   │       │   ├── service/     # 服务层
│   │       │   │   ├── MemoryService.java              # 记忆服务
│   │       │   │   ├── ProfileAnalysisService.java     # 档案分析服务
│   │       │   │   ├── PlanGenerationService.java      # 计划生成服务
│   │       │   │   ├── AIContentService.java          # AI 内容服务
│   │       │   │   └── MotivationService.java         # 激励服务
│   │       │   ├── entity/      # 实体类
│   │       │   ├── repository/  # 数据访问层
│   │       │   ├── controller/  # 控制器
│   │       │   └── dto/         # 数据传输对象
│   │       ├── service/      # 业务逻辑服务
│   │       ├── controller/   # REST 控制器
│   │       ├── entity/       # 数据库实体
│   │       ├── mapper/       # MyBatis 映射器
│   │       ├── dto/          # 数据传输对象
│   │       └── vo/           # 视图对象
│   ├── src/main/resources/ # 配置文件和 SQL
│   │   └── db/schema.sql    # 数据库结构
│   ├── Dockerfile
│   └── pom.xml
├── frontend/               # 前端 Vue 项目
│   ├── src/
│   │   ├── api/           # API 接口
│   │   │   ├── memory.ts      # 记忆系统 API
│   │   │   ├── profile.ts      # 学习档案 API
│   │   │   ├── plans.ts        # 学习计划 API
│   │   │   ├── motivation.ts  # 激励系统 API
│   │   │   └── ...           # 其他 API
│   │   ├── components/    # 组件
│   │   │   └── personalized/ # 个性化学习组件
│   │   │       ├── MemoryCard.vue      # 记忆卡片
│   │   │       ├── ProgressChart.vue   # 进度图表
│   │   │       ├── DailyTasks.vue      # 每日任务
│   │   │       ├── AchievementWall.vue # 成就墙
│   │   │       └── StreakCalendar.vue  # 连续学习日历
│   │   ├── views/         # 页面
│   │   │   └── LearningPage.vue # 学习页面
│   │   ├── stores/        # Pinia 状态管理
│   │   ├── i18n/          # 国际化
│   │   └── router/        # 路由
│   ├── Dockerfile
│   └── nginx.conf
├── docker-compose.yml      # 生产环境配置
├── docker-compose.dev.yml  # 开发环境配置
├── .env.example           # 环境变量示例
└── .kiro/specs/          # 系统设计文档
    └── personalized-learning-system/
        ├── requirements.md # 需求文档
        ├── design.md      # 设计文档
        └── tasks.md       # 实施计划
```

## API 接口

### 基础功能
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

### 个性化学习系统
| 模块 | 接口 | 说明 |
|------|------|------|
| 记忆 | POST /api/memory/records | 创建记忆记录 |
| 记忆 | PUT /api/memory/records/{id}/review | 更新复习结果 |
| 记忆 | GET /api/memory/due-reviews | 获取待复习列表 |
| 记忆 | GET /api/memory/statistics | 获取记忆统计 |
| 档案 | GET /api/profile | 获取学习档案 |
| 档案 | POST /api/profile/analyze | 触发档案分析 |
| 档案 | GET /api/profile/insights | 获取学习洞察报告 |
| 档案 | GET /api/profile/progress-curve | 获取进度曲线 |
| 计划 | POST /api/plans | 创建学习计划 |
| 计划 | GET /api/plans/current | 获取当前计划 |
| 计划 | GET /api/plans/daily-tasks | 获取每日任务 |
| 计划 | PUT /api/plans/adjust | 触发计划调整 |
| 计划 | POST /api/plans/tasks/{id}/complete | 完成任务 |
| 内容 | POST /api/content/example-sentences | 生成例句 |
| 内容 | POST /api/content/exercises | 生成练习题 |
| 内容 | POST /api/content/dialogue-scenario | 生成对话场景 |
| 激励 | GET /api/motivation/streak | 获取学习连续记录 |
| 激励 | GET /api/motivation/achievements | 获取用户成就 |
| 激励 | GET /api/motivation/visualization | 获取进度可视化 |
| 激励 | POST /api/motivation/check-achievements | 检查新成就 |

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

## 个性化学习系统

### 系统概述

个性化学习系统是平台的核心功能模块，通过科学的记忆算法、智能的学习分析和AI驱动的内容生成，为每个用户提供定制化的学习体验。系统包含5个核心子系统：

1. **间隔重复记忆系统** - 基于艾宾浩斯遗忘曲线，智能安排复习时间
2. **学习档案分析** - 分析学习行为，识别薄弱环节
3. **智能学习计划** - 动态生成个性化学习路径和每日任务
4. **AI内容生成** - 根据掌握度生成定制化学习材料
5. **激励系统** - 通过成就和奖励保持学习动力

### 核心算法

#### 间隔重复算法

基于艾宾浩斯遗忘曲线的改进算法，根据掌握度和复习次数计算下次复习时间：

```
复习间隔（小时）= 基础间隔 × (1 + 掌握度/100)^复习次数

掌握度更新规则：
- 答对：masteryLevel = min(100, masteryLevel + 10)
- 答错：masteryLevel = max(0, masteryLevel - 15)
- 连续3次答错：重置复习间隔为1小时
```

#### 学习分析算法

- **时间偏好分析**：统计不同时间段的学习频率和效果
- **薄弱领域识别**：错误率>40%的领域标记为薄弱项
- **学习速度计算**：基于最近30天学习记录的平均值
- **进度曲线生成**：时间序列数据展示学习趋势

#### 计划优化算法

- **每日任务量计算**：根据用户水平动态调整（10-50词汇/天）
- **任务难度调整**：
  - 完成率<60%：降低20%
  - 完成率>90%：提升10%
- **薄弱领域倾斜**：每日任务中薄弱内容占比≥30%

### 数据库设计

系统使用以下数据表存储个性化学习数据：

- **memory_records** - 记忆记录表，追踪每个词汇的掌握度
- **learning_profiles** - 学习档案表，存储分析结果
- **study_plans** - 学习计划表，管理学习目标
- **daily_tasks** - 每日任务表，分配具体任务
- **achievements** - 成就定义表，预定义成就类型
- **user_achievements** - 用户成就表，记录获得的成就
- **learning_streaks** - 学习连续记录表，追踪连续学习天数

### Redis缓存

系统使用Redis缓存提升性能：

```
learning:profile:{userId}           - 学习档案缓存，TTL 24小时
learning:plan:{userId}              - 当前学习计划缓存，TTL 24小时
learning:daily_tasks:{userId}:{date} - 每日任务缓存，TTL 24小时
learning:due_reviews:{userId}       - 待复习列表缓存，TTL 1小时
learning:streak:{userId}            - 学习连续记录缓存，TTL 24小时
learning:achievements:{userId}      - 用户成就缓存，TTL 24小时
```

### 测试策略

系统采用**双重测试方法**：

1. **单元测试** - 验证特定示例和边界情况
2. **基于属性的测试（PBT）** - 验证通用正确性属性

使用jqwik库进行属性测试，每个属性测试运行100次迭代。系统定义了38个正确性属性，覆盖所有核心功能。

### 性能指标

- 复习间隔计算：< 100ms/用户
- 计划生成：< 500ms/用户
- 缓存命中率：> 80%
- 并发处理：100用户 < 2秒



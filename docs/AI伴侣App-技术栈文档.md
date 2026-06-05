# AI伴侣App — 技术栈文档（Flutter 双端版）

**文档版本：** v3.0
**编写日期：** 2026年6月5日
**文档状态：** 修订稿

---

## 1. 整体架构

```
┌──────────────────────────────────────────────────────┐
│                    客户端层                            │
│     Flutter 3.x + Dart 3.x (iOS / Android)           │
│     后续可扩展至 Web / Desktop                         │
└──────────────────────┬───────────────────────────────┘
                       │  HTTPS / WebSocket / SSE
┌──────────────────────▼───────────────────────────────┐
│                  API 网关层                            │
│     Spring Cloud Gateway / 认证 / 限流 / 路由          │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│              业务服务层 (Spring Boot)                    │
│  - 用户服务 / 关系服务 / 消息服务                         │
│  - 商城服务 / 通知服务 / 支付服务                         │
│  - AI服务 / 记忆服务 / 内容安全                          │
└──────────────────────┬───────────────────────────────┘
                       │
┌──────────────────────▼───────────────────────────────┐
│                    数据层                              │
│  PostgreSQL / Redis / Milvus(向量) / Elasticsearch     │
│  RocketMQ(消息队列)                                    │
└──────────────────────────────────────────────────────┘
```

---

## 2. 核心技术选型

| 模块 | 技术方案 | 说明 |
|------|---------|------|
| 客户端 | Flutter 3.x + Dart 3.x | 一套代码覆盖 iOS + Android，后续可扩展至 Web / Desktop |
| 服务端 | Spring Boot 3.x + Java 21 | 企业级框架，虚拟线程高并发 |
| LLM 接入 | OpenAI / Claude / 国产大模型API / LM Studio / Ollama | 支持云端API和本地离线模型，按需切换 |
| 记忆系统 | Milvus 向量数据库 + RAG | 长期记忆的向量存储与检索 |
| 情感分析 | LLM Prompt + 微调 BERT | 实时情绪识别 |
| 语音合成 | Azure TTS / CosyVoice | 多音色支持 |
| 语音识别 | 阿里云 ASR / Whisper | 高精度语音转文字 |
| 图像生成 | Stable Diffusion API | 伴侣形象和场景生成 |
| 即时通讯 | WebSocket + SSE | 实时双向通信 + AI 流式输出 |

---

## 3. 客户端技术栈（Flutter）

> 📄 详细内容已拆分至独立文档：**[AI伴侣App-前端技术栈文档.md](AI伴侣App-前端技术栈文档.md)**
>
> 涵盖：开发环境、架构模式（Clean Architecture + Riverpod）、第三方库清单（30+ 库）、网络层设计（Dio + Retrofit + WebSocket + SSE）、本地持久化（Drift + Hive）、推送与通知等。

---

## 4. 服务端技术栈（Spring Boot）

| 技术 | 用途 |
|------|------|
| Java 21 + Spring Boot 3.x | 后端主框架，虚拟线程高并发 |
| Spring Security + JWT | 认证鉴权 |
| MyBatis-Plus | ORM |
| Spring WebSocket + SSE | 实时通信 + AI 流式输出 |
| Nacos | 服务注册发现 + 配置中心 |
| Redis | 缓存 + 会话管理 + 消息队列（Stream） |
| XXL-JOB | 定时任务（主动关心、晚安问候等） |
| Spring AI | 统一 LLM 调用与 RAG 编排，兼容云端和本地模型 |

---

## 5. 数据存储

| 技术 | 用途 |
|------|------|
| PostgreSQL | 用户、订单、订阅等业务数据 |
| Redis | 会话状态、热点缓存、消息队列 |
| Milvus | 向量数据库，存储长期记忆的 embedding |
| Elasticsearch | 聊天记录全文检索 |

---

## 6. AI 能力

| 技术 | 用途 |
|------|------|
| LLM API（OpenAI / Claude / 国产大模型） | 云端对话能力 |
| LM Studio / Ollama（OpenAI 兼容 API） | 本地离线模型部署，支持模型配置切换 |
| Spring AI | 统一 LLM 调用与 RAG 编排，兼容云端和本地模型 |
| Azure TTS / 阿里云 ASR | 语音合成与识别 |
| Stable Diffusion API | 伴侣形象与场景生成 |

> **离线模型说明：** LM Studio 和 Ollama 均提供 OpenAI 兼容的 API 接口，Spring AI 可通过修改 `base-url` 配置无缝切换云端/本地模型，无需改动业务代码。客户端通过设置页的"本地模型地址"字段配置连接地址。

---

## 7. 第三方服务

| 服务 | 用途 |
|------|------|
| Firebase (FCM + Crashlytics + Analytics) | 推送、崩溃收集、数据统计 |
| App Store Connect | iOS 应用分发、TestFlight 测试 |
| Google Play Console | Android 应用分发、内部测试 |
| 支付宝 SDK | 支付功能 |

---

## 8. 技术栈总览

```
┌─────────────────── 客户端 ───────────────────┐
│         Flutter 3.x + Dart 3.x               │
│   详见：前端技术栈文档.md                       │
└───────────────────┬─────────────────────────┘
                    │ HTTPS / WebSocket / SSE
┌───────────────────▼─────────────────────────┐
│       Spring Boot 3.x + Spring Cloud        │
│  Security │ MyBatis-Plus │ WebSocket         │
│  Nacos │ XXL-JOB │ Spring AI                │
└───────────────────┬─────────────────────────┘
                    │
┌───────────────────▼─────────────────────────┐
│                数据层                         │
│  PostgreSQL │ Redis │ Milvus │ ES            │
└─────────────────────────────────────────────┘
```

---

## 9. 关键技术决策记录

> 📄 前端技术选型决策（Riverpod vs Bloc、Drift vs sqflite/Hive、go_router vs auto_route）已合并至前端文档第 8 节：**[AI伴侣App-前端技术栈文档.md](AI伴侣App-前端技术栈文档.md#8-关键技术决策记录)**

---


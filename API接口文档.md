# SoulMate AI 接口文档

Base URL: `http://39.108.137.45/api`

## 认证 /api/auth

- POST /api/auth/send-code — 发送验证码
- POST /api/auth/login — 邮箱登录
- POST /api/auth/guest — 游客登录

## 用户 /api/user

- GET /api/user/info — 用户信息
- PUT /api/user/info — 更新信息
- PUT /api/user/avatar — 更新头像
- GET /api/user/profile — 用户档案
- PUT /api/user/profile — 更新档案
- GET /api/user/settings — 设置
- PUT /api/user/settings — 更新设置

## 伴侣 /api/companion

- GET /api/companion/list — 伴侣列表
- GET /api/companion/{id} — 伴侣详情
- PUT /api/companion/{id} — 更新伴侣
- DELETE /api/companion/{id} — 删除伴侣
- PUT /api/companion/{id}/avatar — 更新伴侣头像

## 对话与聊天 /api

- POST /api/conversation — 创建对话
- GET /api/conversation/list — 对话列表
- GET /api/conversation/{id}/messages — 历史消息
- POST /api/chat/stream — 流式聊天 (SSE)
- POST /api/chat/send — 同步发消息
- DELETE /api/message/{id} — 删除消息

## 记忆 /api/memory

- GET /api/memory/list — 记忆列表
- GET /api/memory/stats — 记忆统计
- PUT /api/memory/{id} — 更新记忆
- DELETE /api/memory/{id} — 删除记忆
- POST /api/memory/admin/rebuild-vectors — 重建向量索引

## 订阅 /api/subscription

- GET /api/subscription/plans — 套餐列表
- GET /api/subscription/current — 当前订阅
- GET /api/subscription/status — 订阅状态

## 提醒 /api/reminders

- GET /api/reminders/list — 提醒列表
- GET /api/reminders/{id} — 提醒详情
- PUT /api/reminders/{id} — 更新提醒
- DELETE /api/reminders/{id} — 删除提醒

## 语音识别 /api/asr

- POST /api/asr/transcribe — 语音转文字

## 文件 /api/file

- POST /api/file/upload — 上传文件
- POST /api/file/upload/batch — 批量上传
- DELETE /api/file/delete — 删除文件

## 支付 /api/alipay

- POST /api/alipay/create — 创建支付订单
- POST /api/alipay/notify — 支付宝异步回调
- GET /api/alipay/status — 查询支付状态

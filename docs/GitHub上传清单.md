# GitHub 上传清单

这份清单用于保证项目上传到 GitHub 后结构清晰、没有敏感信息，并且方便面试官快速理解项目价值。

## 上传前检查

1. 检查敏感信息：

```powershell
Select-String -Path README.md,docs\*.md,src\main\resources\application.yaml,src\main\java\com\hmdp\**\*.java -Pattern "password|123321|abc123456|Kuanfan|183\.169|E:\\hmdping"
```

如果只在本清单里命中示例关键字，可以忽略；如果在配置文件或 Java 代码里命中真实密码、服务器地址、本机路径，需要先替换为环境变量或示例值。

2. 检查 Git 状态：

```powershell
git status
```

工作区应该只包含你准备提交的文件。

3. 检查文档编码：

在 GitHub 页面打开 `README.md` 和 `docs` 目录，确认中文不是乱码。

## 推荐提交顺序

```powershell
git add src pom.xml
git commit -m "feat: add backend core services"

git add agent-service
git commit -m "feat: add fastapi agent service"

git add docs README.md
git commit -m "docs: add architecture and interview guide"

git add .gitignore .env.example docker-compose.yml
git commit -m "chore: add local development setup"
```

如果已经提交过，可以继续追加一次文档修复提交：

```powershell
git add README.md docs
git commit -m "docs: fix chinese documentation encoding"
```

## 推送到 GitHub

```powershell
git remote -v
git push -u origin main
```

如果提示远程仓库已有提交，先拉取并解决冲突：

```powershell
git pull origin main --allow-unrelated-histories
```

解决冲突后：

```powershell
git add README.md docs
git commit -m "merge remote metadata"
git push -u origin main
```

## GitHub 页面建议

仓库描述可以写：

```text
Local life service platform with Redis cache governance, Kafka async seckill, Elasticsearch search and FastAPI Agent orchestration.
```

推荐 Topics：

```text
spring-boot redis kafka elasticsearch redisson lua fastapi agent java
```

## 面试官快速阅读路径

1. 先看 `README.md`，了解项目定位和亮点。
2. 再看 `docs/架构设计.md`，理解整体架构。
3. 看 `docs/代码详解.md`，定位核心实现。
4. 看 `docs/面试讲解.md`，快速抓住项目表达重点。

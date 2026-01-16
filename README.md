# 钉钉文档解析服务 - Spring Boot 3 实现

🚀 基于 Spring Boot 3 的钉钉文档解析服务，支持将钉钉文档转换为 Markdown 格式。

## ✨ 特性

- 🎯 **智能 Cookie 管理** - 支持手动提供、环境变量、本地文件
- 📄 **完整文档解析** - 自动获取钉钉文档内容
- 📝 **Markdown 生成** - 支持段落、表格、代码块、图片、列表等
- 💾 **自动文件保存** - 自动创建目录并保存 Markdown 文件
- 🌐 **REST API** - 提供简洁的 REST API 接口
- 🎨 **Web 界面** - 美观的前端页面，支持在线预览 Markdown
- 📚 **Swagger 文档** - 自动生成 API 文档
- ✅ **完善的测试** - 单元测试 + 属性测试 + 集成测试

## 📋 技术栈

- **框架**: Spring Boot 3.2.1
- **语言**: Java 17
- **构建**: Maven
- **HTTP**: RestTemplate
- **JSON**: Jackson
- **HTML 解析**: Jsoup
- **测试**: JUnit 5 + Mockito + jqwik + WireMock
- **文档**: Springdoc OpenAPI (Swagger)

## 📦 安装

### 前置要求

- JDK 17 或更高版本
- Maven 3.6+

### 构建项目

```bash
# 克隆项目
git clone <repository-url>
cd dingtalk-doc-parser

# 构建
mvn clean package

# 运行
java -jar target/dingtalk-doc-parser-1.0.0.jar
```

## ⚡ 快速开始

### 1. 配置 Cookie

有三种方式配置钉钉 Cookie：

**方式 1: 环境变量（推荐）**
```bash
export DINGTALK_COOKIE="your_cookie_here"
```

**方式 2: 配置文件**
在 `application.properties` 中添加：
```properties
dingtalk.cookie=your_cookie_here
```

**方式 3: API 请求时提供**
在请求体中包含 `cookie` 字段

### 2. 启动服务

```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动

### 3. 使用方式

#### 方式 1: Web 界面（推荐）

1. 打开浏览器访问：**http://localhost:8080**
2. 在页面上输入 Cookie（可选）和文档地址
3. 点击"开始解析"按钮
4. 查看解析结果和文件路径
5. 点击"预览 Markdown"查看渲染效果

**Web 界面功能：**
- ✅ 手动输入 Cookie
- ✅ 输入文档地址
- ✅ 实时解析进度
- ✅ 显示文件路径
- ✅ Markdown 在线预览（GitHub 风格）

详细使用说明请查看：[前端页面使用指南](FRONTEND_USAGE.md)

#### 方式 2: REST API

**解析文档**
```bash
curl -X POST http://localhost:8080/api/document/parse \
  -H "Content-Type: application/json" \
  -d '{
    "documentUrl": "https://alidocs.dingtalk.com/i/nodes/xxx",
    "cookie": "your_cookie_here"
  }'
```

**响应示例**
```json
{
  "success": true,
  "message": "文档解析成功",
  "filePath": "/Users/username/Documents/dingtalk-docs/文档标题/文档标题.md",
  "error": null
}
```

**读取 Markdown 文件**
```bash
curl "http://localhost:8080/api/document/markdown?filePath=/path/to/file.md"
```

**健康检查**
```bash
curl http://localhost:8080/api/health
```

## 📖 API 文档

启动服务后，访问 Swagger UI：
```
http://localhost:8080/swagger-ui.html
```

## 🔧 配置说明

### application.properties

```properties
# 钉钉 API 配置
dingtalk.api.base-url=https://alidocs.dingtalk.com
dingtalk.api.timeout=30000

# 文件存储配置
file.output.base-dir=${user.home}/Documents/dingtalk-docs

# Cookie 配置
cookie.file.path=dingtalk_cookies.json

# 日志配置
logging.level.com.dingtalk=DEBUG
```

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行单元测试
mvn test -Dtest=*Test

# 运行属性测试
mvn test -Dtest=*PropertyTest

# 运行集成测试
mvn test -Dtest=*IntegrationTest

# 生成测试覆盖率报告
mvn jacoco:report
```

测试覆盖率报告位于：`target/site/jacoco/index.html`

## 📂 项目结构

```
dingtalk-doc-parser/
├── src/
│   ├── main/
│   │   ├── java/com/dingtalk/doc/
│   │   │   ├── controller/      # REST 控制器
│   │   │   ├── service/         # 业务服务
│   │   │   ├── repository/      # 数据持久化
│   │   │   ├── model/           # 数据模型
│   │   │   ├── config/          # 配置类
│   │   │   ├── exception/       # 自定义异常
│   │   │   └── util/            # 工具类
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/dingtalk/doc/
├── pom.xml
└── README.md
```

## 🎯 支持的文档元素

- ✅ 段落和富文本（粗体、斜体）
- ✅ 表格
- ✅ 代码块（支持多种语言）
- ✅ 图片
- ✅ 标题（H1-H6）
- ✅ 列表（有序、无序）

## ⚠️ 注意事项

- Cookie 会过期，需要定期更新（通常 7-30 天）
- OSS 加密的文档内容暂不支持完整解密
- 确保有足够的磁盘空间用于保存文档

## 🔧 故障排查

### 问题 1: Cookie 失效

**症状**: 返回 401 或 403 错误

**解决方案**:
1. 浏览器访问 https://alidocs.dingtalk.com
2. 登录钉钉账号
3. 按 F12 → Network → 复制 Cookie
4. 更新环境变量或配置文件

### 问题 2: 无法提取文档内容

**症状**: 返回 "无法提取文档内容（可能是OSS加密）"

**解决方案**: 该文档使用了 OSS 加密，当前版本暂不支持

### 问题 3: 文件保存失败

**症状**: 返回文件操作异常

**解决方案**: 检查输出目录权限和磁盘空间

## 📄 许可证

MIT License

## 👨‍💻 作者

基于 TypeScript 版本转换为 Java 实现

---

**快速开始**: 配置 Cookie → 启动服务 → 调用 API → 获取 Markdown 文件！🚀

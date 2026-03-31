export interface DocsSection {
    title: string;
    bullets: string[];
}

export interface DocsPage {
    slug: string;
    title: string;
    summary: string;
    sections: DocsSection[];
}

export const docsPages: DocsPage[] = [
    {
        slug: "quick-start",
        title: "快速开始",
        summary: "从启动前端到执行第一条 SQL 的最短路径。",
        sections: [
            {
                title: "启动与访问",
                bullets: [
                    "在 web 目录执行 npm install 安装依赖。",
                    "执行 npm run dev 启动本地开发服务，默认端口 5173。",
                    "默认后端地址为 http://localhost:8080，可通过 VITE_API_BASE_URL 覆盖。"
                ]
            },
            {
                title: "第一条 SQL",
                bullets: [
                    "进入控制台后在 SQL 输入区输入语句。",
                    "点击“执行 SQL”或使用 Ctrl/Cmd + Enter 运行。",
                    "执行后可在结果区和执行链路区查看反馈。"
                ]
            },
            {
                title: "最小验证清单",
                bullets: [
                    "执行 show databases; 检查连接是否可用。",
                    "执行 show tables; 检查当前数据库内容。",
                    "确认结果区、历史区、链路区都出现对应数据。"
                ]
            }
        ]
    },
    {
        slug: "authentication",
        title: "身份与登录",
        summary: "登录、注册、邮箱验证、改密和游客模式的操作方式。",
        sections: [
            {
                title: "登录与游客",
                bullets: [
                    "登录页支持账号密码登录。",
                    "未登录时可用游客模式进入控制台。",
                    "登录后导航栏可直接退出当前账号。"
                ]
            },
            {
                title: "账号流程",
                bullets: [
                    "注册后通过邮件链接完成验证。",
                    "支持忘记密码、重置密码和改密邮件确认。",
                    "验证码开启时登录页会展示 Turnstile 校验。"
                ]
            },
            {
                title: "登录态说明",
                bullets: [
                    "前端将 token 持久化在 localStorage。",
                    "应用启动时会自动调用 /api/auth/me 回填当前用户。",
                    "登出时会清理 token 与工作台本地状态。"
                ]
            }
        ]
    },
    {
        slug: "workspace",
        title: "SQL 工作台",
        summary: "会话管理、SQL 草稿、结果面板和执行链路的使用说明。",
        sections: [
            {
                title: "会话管理",
                bullets: [
                    "支持新建、切换、删除工作台会话。",
                    "每个会话有独立最近查询历史。",
                    "本地会保留会话级 SQL 草稿。"
                ]
            },
            {
                title: "结果与链路",
                bullets: [
                    "执行结果区显示表格和原始输出。",
                    "链路区可按需展开 trace 执行步骤。",
                    "失败执行会在结果区显示错误信息。"
                ]
            },
            {
                title: "编辑效率",
                bullets: [
                    "提供常用 SQL 预设按钮（查看库、查看表、示例查询）。",
                    "当前会话草稿会自动保存，切换会话后可恢复。",
                    "支持将历史 SQL 一键回填到编辑器。"
                ]
            }
        ]
    },
    {
        slug: "assets",
        title: "内容资产",
        summary: "SQL 片段、场景脚本、导出任务的管理方式。",
        sections: [
            {
                title: "片段与场景",
                bullets: [
                    "可从当前 SQL 保存为片段或场景。",
                    "资产列表支持关键词筛选。",
                    "场景支持一键运行并回填结果。"
                ]
            },
            {
                title: "导出任务",
                bullets: [
                    "支持 CSV / JSON 导出格式。",
                    "任务可立即执行或稍后手动执行。",
                    "任务状态包含执行中、完成、失败。"
                ]
            },
            {
                title: "筛选与检索",
                bullets: [
                    "内容库支持关键词检索标题、说明、SQL 与错误信息。",
                    "支持查看近 7 天更新内容。",
                    "可单独过滤失败导出任务进行排障。"
                ]
            }
        ]
    },
    {
        slug: "admin",
        title: "管理功能",
        summary: "管理员身份可进行用户数据库分配与回收。",
        sections: [
            {
                title: "管理员面板",
                bullets: [
                    "仅管理员在控制台侧栏可见管理面板。",
                    "可为用户分配数据库。",
                    "可回收非管理员且非默认库的数据库。"
                ]
            },
            {
                title: "操作建议",
                bullets: [
                    "分配数据库后建议通知用户重新登录以刷新上下文。",
                    "回收前先确认该用户是否还有待导出任务。",
                    "优先在低峰时段进行批量分配与回收。"
                ]
            }
        ]
    },
    {
        slug: "architecture",
        title: "前端架构",
        summary: "页面结构、状态管理、服务调用和组件分层。",
        sections: [
            {
                title: "目录约定",
                bullets: [
                    "views 存放路由页面：Home、Login、Console、Docs。",
                    "components 放业务组件和 ui 基础组件。",
                    "stores 放 Pinia 状态，services 放 API 调用。"
                ]
            },
            {
                title: "状态分层",
                bullets: [
                    "auth store 负责登录态、用户信息和认证流程。",
                    "platform store 负责工作台会话、SQL、结果和资产。",
                    "页面组件只做交互编排，业务动作下沉到 store。"
                ]
            },
            {
                title: "接口模式",
                bullets: [
                    "services/api.ts 统一封装 request 与错误处理。",
                    "所有工作台接口通过 Bearer token 透传身份。",
                    "HTTP 204 响应统一转成 undefined，避免页面层分支噪音。"
                ]
            }
        ]
    },
    {
        slug: "api-contract",
        title: "接口清单",
        summary: "当前前端依赖的后端 API 路径与用途。",
        sections: [
            {
                title: "认证接口",
                bullets: [
                    "GET /api/auth/config：获取验证码配置。",
                    "POST /api/auth/login / register：登录与注册。",
                    "GET /api/auth/me / DELETE /api/auth/logout：当前用户与登出。"
                ]
            },
            {
                title: "工作台接口",
                bullets: [
                    "GET/POST /api/workspace/sessions：会话列表与创建会话。",
                    "POST /api/workspace/sessions/{id}/execute：执行 SQL。",
                    "GET /api/workspace/dashboard：统计概览。"
                ]
            },
            {
                title: "资产接口",
                bullets: [
                    "snippets：GET/POST/DELETE 管理 SQL 片段。",
                    "scenarios：GET/POST/DELETE 与 /run 执行场景。",
                    "export-tasks：GET/POST/DELETE 与 /run 触发导出。"
                ]
            }
        ]
    },
    {
        slug: "deployment",
        title: "部署与环境",
        summary: "本地开发、生产构建和环境变量说明。",
        sections: [
            {
                title: "开发环境",
                bullets: [
                    "Node.js 版本建议与项目锁文件保持一致。",
                    "web/.env.development 中配置 VITE_API_BASE_URL。",
                    "若跨域受限，优先在后端侧放开本地 Origin。"
                ]
            },
            {
                title: "生产构建",
                bullets: [
                    "执行 npm run build 产物输出到 web/dist。",
                    "静态资源可由 Nginx、CDN 或后端静态目录托管。",
                    "发布前建议先执行 npm run preview 做本地验收。"
                ]
            },
            {
                title: "常用环境变量",
                bullets: [
                    "VITE_API_BASE_URL：后端基础地址。",
                    "可按环境拆分 .env.development 与 .env.production。",
                    "前端只应存放公开配置，不放服务端密钥。"
                ]
            }
        ]
    },
    {
        slug: "troubleshooting",
        title: "排错指南",
        summary: "开发阶段最常见的问题与快速处理方式。",
        sections: [
            {
                title: "页面样式异常",
                bullets: [
                    "先检查 postcss 与 tailwind 配置是否存在。",
                    "执行 npm run build 看是否有 @apply 编译错误。",
                    "浏览器强刷 Ctrl+F5 清理旧缓存样式。"
                ]
            },
            {
                title: "接口报错",
                bullets: [
                    "确认 VITE_API_BASE_URL 指向可达后端地址。",
                    "检查后端日志中的鉴权失败或参数校验失败。",
                    "优先用浏览器 Network 面板确认实际请求路径。"
                ]
            },
            {
                title: "登录异常",
                bullets: [
                    "清理 localStorage 的 token 后重试登录。",
                    "检查邮箱验证链接中的 token 是否过期。",
                    "验证码开启时确认站点 key 与域名配置一致。"
                ]
            }
        ]
    },
    {
        slug: "faq",
        title: "常见问题",
        summary: "面向使用与开发协作的高频问答。",
        sections: [
            {
                title: "为什么游客也能进入控制台",
                bullets: [
                    "用于体验主要流程，默认落到共享数据库。",
                    "游客模式不覆盖登录用户的个人数据库资产。",
                    "进入正式演示或评审建议使用登录态。"
                ]
            },
            {
                title: "为什么要保留文档模块",
                bullets: [
                    "减少首页信息负担，保持门户纯入口定位。",
                    "将操作规范、排错信息集中到可维护页面。",
                    "便于后续持续追加接口与版本变更说明。"
                ]
            },
            {
                title: "文档如何继续扩展",
                bullets: [
                    "直接在 docs.ts 增加新的 page 或 section。",
                    "slug 作为路由锚点，保持稳定便于分享链接。",
                    "新增内容优先写操作步骤和验证结果，少写叙述。"
                ]
            }
        ]
    }
];

export const defaultDocsSlug = docsPages[0]?.slug ?? "quick-start";

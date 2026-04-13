#!/bin/zsh

echo "🚀 启动 FCG 全栈项目..."

# 清理残留进程
echo "🧹 清理残留进程..."
lsof -ti:8080 | xargs kill -9 2>/dev/null
lsof -ti:5173 | xargs kill -9 2>/dev/null
sleep 1

PROJECT_DIR=$(cd "$(dirname "$0")" && pwd)
cd "$PROJECT_DIR"

echo "📦 启动后端 (端口 8080)..."
cd "$PROJECT_DIR/fcg-server" && mvn spring-boot:run &
BACKEND_PID=$!

sleep 8

echo "🎨 启动前端 (端口 5173)..."
cd "$PROJECT_DIR/fcg-client" && npm run dev &
FRONTEND_PID=$!

echo ""
echo "✅ 已启动:"
echo "   后端: http://localhost:8080"
echo "   前端: http://localhost:5173"
echo "   API文档: http://localhost:8080/doc.html"
echo ""
echo "按 Ctrl+C 停止所有服务"

cleanup() {
  echo ""
  echo "🛑 停止服务..."
  kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
  exit 0
}

trap cleanup SIGINT SIGTERM
wait
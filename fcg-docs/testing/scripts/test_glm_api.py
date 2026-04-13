#!/usr/bin/env python3
import requests
import json
import base64

API_KEY = "90c54afb5e59441c9b9a83d0943b7dd4.PPDjNt6JQt2sq72k"
BASE_URL = "https://open.bigmodel.cn/api/paas/v4"


def test_text_chat():
    """测试纯文本对话"""
    url = f"{BASE_URL}/chat/completions"
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {API_KEY}"}

    # 测试不同的模型名称
    models = ["glm-4v-flash", "glm-4v", "glm-4-flash", "glm-4"]

    for model in models:
        data = {"model": model, "messages": [{"role": "user", "content": "你好"}]}

        print(f"\n测试模型: {model}")
        print(f"请求: {json.dumps(data, ensure_ascii=False)}")

        try:
            response = requests.post(url, headers=headers, json=data, timeout=30)
            print(f"状态码: {response.status_code}")
            print(f"响应: {response.text[:200]}")

            if response.status_code == 200:
                print(f"✅ 模型 {model} 可用!")
                return model
        except Exception as e:
            print(f"❌ 错误: {e}")

    return None


def test_vision():
    """测试视觉识别（使用纯色图片）"""
    url = f"{BASE_URL}/chat/completions"
    headers = {"Content-Type": "application/json", "Authorization": f"Bearer {API_KEY}"}

    # 创建一个1x1像素的红色图片（最小的测试图片）
    # Red pixel in PNG format
    tiny_png = b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x02\x00\x00\x00\x90wS\xde\x00\x00\x00\x0cIDATx\x9cc\xf8\xcf\xc0\x00\x00\x00\x03\x00\x01\x00\x18\xdd\x8d\xb4\x00\x00\x00\x00IEND\xaeB`\x82"
    image_base64 = base64.b64encode(tiny_png).decode("utf-8")

    data = {
        "model": "glm-4v-flash",
        "messages": [
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": "这是什么颜色?"},
                    {
                        "type": "image_url",
                        "image_url": {"url": f"data:image/png;base64,{image_base64}"},
                    },
                ],
            }
        ],
    }

    print(f"\n\n测试视觉识别")
    print(f"图片大小: {len(tiny_png)} bytes")

    try:
        response = requests.post(url, headers=headers, json=data, timeout=30)
        print(f"状态码: {response.status_code}")
        result = response.json()
        print(f"响应: {json.dumps(result, ensure_ascii=False, indent=2)[:500]}")

        if response.status_code == 200:
            print(f"✅ 视觉识别功能正常!")
            return True
    except Exception as e:
        print(f"❌ 错误: {e}")

    return False


if __name__ == "__main__":
    print("=" * 60)
    print("智谱AI API 测试")
    print("=" * 60)

    # 测试文本对话
    working_model = test_text_chat()

    # 测试视觉识别
    if working_model:
        test_vision()

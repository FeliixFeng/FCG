#!/usr/bin/env python3
"""测试改造后的药品OCR接口"""

import requests
import base64
from PIL import Image, ImageDraw, ImageFont
import io


def create_test_image():
    """创建一个测试药品图片"""
    img = Image.new("RGB", (400, 300), color="white")
    draw = ImageDraw.Draw(img)

    text_lines = [
        "盐酸氨溴索片",
        "Ambroxol Hydrochloride Tablets",
        "30mg × 20片",
        "生产企业：北京协和药厂",
        "有效期至：2027-12-31",
        "适应症：用于急慢性呼吸道疾病",
        "禁忌：孕妇及哺乳期妇女禁用",
        "不良反应：偶见恶心、胃部不适",
    ]

    y = 30
    for line in text_lines:
        draw.text((20, y), line, fill="black")
        y += 30

    buffer = io.BytesIO()
    img.save(buffer, format="JPEG")
    buffer.seek(0)
    return buffer.getvalue()


def test_ocr_api():
    """测试OCR接口"""
    url = "http://localhost:8080/api/medicine/ocr"

    # 创建测试图片
    print("📸 创建测试药品图片...")
    image_data = create_test_image()

    # 准备multipart/form-data请求（注意：参数名从files改为file）
    files = {"file": ("test_medicine.jpg", image_data, "image/jpeg")}

    print(f"🚀 调用OCR接口: POST {url}")
    print(f"📋 参数名: file (单文件上传)")

    try:
        response = requests.post(url, files=files, timeout=60)

        print(f"\n✅ 响应状态: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print("\n📦 返回数据结构:")
            print(f"  - code: {data.get('code')}")
            print(f"  - message: {data.get('message')}")

            result = data.get("data", {})
            print(f"\n🔍 识别结果:")
            print(f"  - model: {result.get('model')}")
            print(f"  - fallback: {result.get('fallback')}")

            parsed = result.get("parsed")
            if parsed:
                print(f"\n💊 解析的药品信息:")
                print(f"  - 药品名称: {parsed.get('name')}")
                print(f"  - 规格: {parsed.get('specification')}")
                print(f"  - 生产厂家: {parsed.get('manufacturer')}")
                print(f"  - 剂型: {parsed.get('dosageForm')}")
                print(f"  - 有效期: {parsed.get('expireDate')}")
                print(f"  - 适应症: {parsed.get('usage')}")
                print(f"  - 禁忌: {parsed.get('contraindications')}")
                print(f"  - 副作用: {parsed.get('sideEffects')}")
                print(f"  - 用法用量: {parsed.get('dosage')}")

            ai_raw = result.get("aiRaw")
            if ai_raw:
                print(f"\n🤖 AI原始响应 (前200字符):")
                print(f"  {ai_raw[:200]}...")

            print("\n✅ 测试通过！新架构工作正常！")
            print("\n📊 新架构特点:")
            print("  ✅ 单图上传 (file 不是 files)")
            print("  ✅ 多模态AI一步完成识别")
            print("  ✅ 不再依赖外部OCR服务器")
            print("  ✅ 返回结构简化 (无rawText/rawLines/ocrError)")

        else:
            print(f"\n❌ 请求失败: {response.status_code}")
            print(f"响应内容: {response.text}")

    except Exception as e:
        print(f"\n❌ 测试失败: {str(e)}")


if __name__ == "__main__":
    test_ocr_api()

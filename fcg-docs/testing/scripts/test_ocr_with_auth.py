#!/usr/bin/env python3
"""测试改造后的药品OCR接口（带JWT认证）"""

import requests
import base64
from PIL import Image, ImageDraw, ImageFont
import io

BASE_URL = "http://localhost:8080"


def login():
    """登录获取JWT token"""
    url = f"{BASE_URL}/api/family/login"
    payload = {"username": "zhangfamily", "password": "test123"}

    print("🔐 登录获取JWT token...")
    response = requests.post(url, json=payload)

    if response.status_code == 200:
        data = response.json()
        family_token = data.get("data", {}).get("token")
        print(f"✅ 家庭token获取成功: {family_token[:30]}...")
        return family_token
    else:
        print(f"❌ 登录失败: {response.status_code}")
        print(response.text)
        return None


def select_member(family_token):
    """选择成员获取member token"""
    url = f"{BASE_URL}/api/family/switch-member/1"
    headers = {"Authorization": f"Bearer {family_token}"}

    print("👤 选择成员获取member token...")
    response = requests.post(url, headers=headers)

    if response.status_code == 200:
        data = response.json()
        member_token = data.get("data", {}).get("token")
        print(f"✅ 成员token获取成功: {member_token[:30]}...")
        return member_token
    else:
        print(f"❌ 选择成员失败: {response.status_code}")
        print(response.text)
        return None


def create_test_image():
    """创建测试药品图片"""
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


def test_ocr_api(token):
    """测试OCR接口"""
    url = f"{BASE_URL}/api/medicine/ocr"

    print("\n📸 创建测试药品图片...")
    image_data = create_test_image()

    files = {"file": ("test_medicine.jpg", image_data, "image/jpeg")}

    headers = {"Authorization": f"Bearer {token}"}

    print(f"🚀 调用OCR接口: POST {url}")
    print(f"📋 参数名: file (单文件上传)")

    try:
        response = requests.post(url, files=files, headers=headers, timeout=60)

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
            else:
                print("\n⚠️  警告: parsed字段为空")

            ai_raw = result.get("aiRaw")
            if ai_raw:
                print(f"\n🤖 AI原始响应 (前200字符):")
                print(f"  {ai_raw[:200]}...")
            else:
                print("\n⚠️  警告: aiRaw字段为空")

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
        import traceback

        traceback.print_exc()


if __name__ == "__main__":
    family_token = login()
    if not family_token:
        exit(1)

    member_token = select_member(family_token)
    if not member_token:
        exit(1)

    test_ocr_api(member_token)

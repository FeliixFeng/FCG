#!/usr/bin/env python3
"""测试真实的药品图片OCR"""

import requests

BASE_URL = "http://localhost:8080"


def login():
    url = f"{BASE_URL}/api/family/login"
    payload = {"username": "zhangfamily", "password": "test123"}
    response = requests.post(url, json=payload)
    return response.json().get("data", {}).get("token")


def select_member(token):
    url = f"{BASE_URL}/api/family/switch-member/1"
    response = requests.post(url, headers={"Authorization": f"Bearer {token}"})
    return response.json().get("data", {}).get("token")


def test_ocr(token):
    url = f"{BASE_URL}/api/medicine/ocr"
    with open(
        "/Users/haifeng/code/project/fcg/fcg-docs/testing/images/test.jpg", "rb"
    ) as f:
        files = {"file": ("test.jpg", f, "image/jpeg")}
        headers = {"Authorization": f"Bearer {token}"}
        response = requests.post(url, files=files, headers=headers, timeout=60)

    data = response.json()
    result = data.get("data", {})

    print("=== OCR识别结果 ===")
    print(f"Model: {result.get('model')}")
    print(f"Fallback: {result.get('fallback')}")

    parsed = result.get("parsed", {})
    if parsed:
        print("\n=== 药品信息 ===")
        print(f"名称: {parsed.get('name')}")
        print(f"规格: {parsed.get('specification')}")
        print(f"生产厂家: {parsed.get('manufacturer')}")
        print(f"剂型: {parsed.get('dosageForm')}")
        print(f"有效期: {parsed.get('expireDate')}")
        print(f"适应症: {parsed.get('usage')}")
        print(f"禁忌: {parsed.get('contraindications')}")
        print(f"副作用: {parsed.get('sideEffects')}")
        print(f"用法用量: {parsed.get('dosage')}")
    else:
        print("⚠️ parsed为空")
        print(
            f"aiRaw: {result.get('aiRaw')[:500] if result.get('aiRaw') else 'None'}..."
        )


if __name__ == "__main__":
    token = login()
    member_token = select_member(token)
    test_ocr(member_token)

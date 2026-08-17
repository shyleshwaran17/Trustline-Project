from backend.deepfake_detector import detect_voice_deepfake


print("\n===== CLEAN CALL TEST =====")

clean_result = detect_voice_deepfake(
    r"D:\Trustline VS\TrustLineAI\audio\clean_call.mp3"
)

print("Clean call result:")
print(clean_result)


print("\n===== SCAM CALL TEST =====")

scam_result = detect_voice_deepfake(
    r"D:\Trustline VS\TrustLineAI\audio\scam_call.mp3"
)

print("Scam call result:")
print(scam_result)
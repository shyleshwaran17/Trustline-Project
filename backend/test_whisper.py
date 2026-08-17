from ai_classifier import classify
from rules import analyze
import whisper
from pathlib import Path

print("Loading Whisper model...")

model = whisper.load_model("base")

print("Model loaded!")

audio_path = Path(__file__).parent.parent / "audio" / "clean_call.mp3"

result = model.transcribe(str(audio_path))

print("\n===== TRANSCRIPT =====\n")
print(result["text"])
score, reasons = analyze(result["text"])
print("\n========================")
print("TRUST SCORE")
print("========================")
print(score)
print("\nDetected:")
for r in reasons:
    print("-", r)
    print("\n========================")
print("AI ANALYSIS")
print("========================")

ai_result = classify(result["text"])

print("Prediction :", ai_result["label"])
print("Confidence :", round(ai_result["confidence"] * 100, 2), "%")
from transformers import pipeline


MODEL_ID = "Vansh180/deepfake-audio-wav2vec2"

print("Loading voice deepfake detector...")
print("First run may download the model (~378 MB).")

deepfake_classifier = pipeline(
    "audio-classification",
    model=MODEL_ID
)

print("Voice deepfake detector loaded!")


def detect_voice_deepfake(audio_path):

    results = deepfake_classifier(audio_path)

    print("Deepfake model result:")
    print(results)

    top_result = results[0]

    label = top_result["label"].lower()
    confidence = top_result["score"] * 100

    if label in {"fake", "spoof", "deepfake"}:

        prediction = "possible AI-generated voice"

    else:

        prediction = "likely human voice"

    return {
        "prediction": prediction,
        "label": label,
        "confidence": round(confidence, 2)
    }
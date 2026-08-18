from flask import Flask, request, jsonify
import whisper
from pathlib import Path

from rules import analyze
from ai_classifier import classify
from deepfake_detector import detect_voice_deepfake

app = Flask(__name__)

print("Loading Whisper model...")
model = whisper.load_model("base")
print("Whisper model loaded!")

@app.route("/")
def home():
    return "TrustLine AI Backend is Running!"
def calculate_final_score(
    rule_score,
    prediction,
    confidence,
    voice_label="real",
    voice_confidence=100
):

    prediction = prediction.lower()

    scam_labels = {
        "scam phone call",
        "banking fraud",
        "technical support scam",
        "identity theft"
    }

    # -----------------------------
    # AI trust score
    # -----------------------------

    if prediction == "normal conversation":

        ai_score = confidence

    elif prediction in scam_labels:

        ai_score = 100 - confidence

    else:

        ai_score = 50

    # -----------------------------
    # Voice authenticity score
    # -----------------------------

    voice_label = voice_label.lower()

    if voice_label == "fake":

        voice_score = 100 - voice_confidence

    elif voice_label == "real":

        voice_score = 100

    else:

        voice_score = 50

    # -----------------------------
    # Combine three signals
    # -----------------------------

    final_score = int(
        (rule_score * 0.40) +
        (ai_score * 0.40) +
        (voice_score * 0.20)
    )

    # -----------------------------
    # Strong scam safeguard
    # -----------------------------

    if prediction in scam_labels and confidence >= 90:

        final_score = min(final_score, 40)

    # -----------------------------
    # Strong voice-clone safeguard
    # -----------------------------

    if voice_label == "fake" and voice_confidence >= 90:

        final_score = min(final_score, 40)

    return max(0, min(100, final_score))


def get_risk_prediction(score):

    if score >= 80:
        return "normal conversation"

    elif score >= 60:
        return "suspicious conversation"

    elif score >= 40:
        return "high-risk conversation"

    else:
        return "scam phone call"
@app.route("/analyze", methods=["POST"])
def analyze_audio():

    if "audio" not in request.files:
        return jsonify({"error": "No audio file uploaded"}), 400

    audio = request.files["audio"]

    upload_path = Path("temp_audio.mp3")
    audio.save(upload_path)

    # -----------------------------
    # Check very small audio files
    # -----------------------------

    if upload_path.stat().st_size < 1000:
        return jsonify({
            "transcript": "",
            "trust_score": 100,
            "rule_score": 100,
            "detected": [],
            "prediction": "audio too short",
            "ai_prediction": "audio too short",
            "confidence": 0,
            "risk_override": False,
            "voice_prediction": "unknown",
            "voice_label": "unknown",
            "voice_confidence": 0
        })

    # -----------------------------
    # Whisper transcription
    # -----------------------------

    transcript = model.transcribe(str(upload_path))

    text = transcript["text"].strip()

    # -----------------------------
    # Ignore empty audio
    # -----------------------------

    if not text:
        return jsonify({
            "transcript": "",
            "trust_score": 100,
            "rule_score": 100,
            "detected": [],
            "prediction": "no speech detected",
            "ai_prediction": "no speech detected",
            "confidence": 0,
            "risk_override": False,
            "voice_prediction": "unknown",
            "voice_label": "unknown",
            "voice_confidence": 0
        })

    # -----------------------------
    # Deepfake / voice-clone detection
    # -----------------------------

    voice_result = detect_voice_deepfake(str(upload_path))

    voice_prediction = voice_result["prediction"]
    voice_label = voice_result["label"]
    voice_confidence = voice_result["confidence"]

    # -----------------------------
    # Rule-based analysis
    # -----------------------------

    score, reasons = analyze(text)

    # -----------------------------
    # AI classification
    # -----------------------------

    ai_result = classify(text)

    ai_prediction = ai_result["label"]
    confidence = ai_result["confidence"] * 100

    # -----------------------------
    # Calculate current Trust Score
    # -----------------------------

       # -----------------------------
    # Calculate combined Trust Score
    # -----------------------------

    final_score = calculate_final_score(
        score,
        ai_prediction,
        confidence,
        voice_label,
        voice_confidence
    )

    # -----------------------------
    # Final TrustLine prediction
    # -----------------------------

    final_prediction = get_risk_prediction(final_score)

    # -----------------------------
    # Debug information
    # -----------------------------

    print("Transcript:", text)
    print("Rule score:", score)
    print("Detected:", reasons)

    print("AI prediction:", ai_prediction)
    print("AI confidence:", confidence)

    print("Voice prediction:", voice_prediction)
    print("Voice label:", voice_label)
    print("Voice confidence:", voice_confidence)

    print("Final prediction:", final_prediction)
    print("Final trust score:", final_score)

    # -----------------------------
    # Return result
    # -----------------------------

    return jsonify({
        "transcript": text,

        "trust_score": final_score,
        "rule_score": score,

        "detected": reasons,

        "prediction": final_prediction,
        "ai_prediction": ai_prediction,
        "confidence": round(confidence, 2),

        "risk_override": False,

        "voice_prediction": voice_prediction,
        "voice_label": voice_label,
        "voice_confidence": round(voice_confidence, 2)
    })
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
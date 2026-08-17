from flask import Flask, request, jsonify
import whisper
from pathlib import Path

from rules import analyze
from ai_classifier import classify

app = Flask(__name__)

print("Loading Whisper model...")
model = whisper.load_model("base")
print("Whisper model loaded!")

@app.route("/")
def home():
    return "TrustLine AI Backend is Running!"
def calculate_final_score(rule_score, prediction, confidence):

    prediction = prediction.lower()

    scam_labels = {
        "scam phone call",
        "banking fraud",
        "technical support scam",
        "identity theft"
    }

    if prediction == "normal conversation":

        ai_score = confidence

    elif prediction in scam_labels:

        ai_score = 100 - confidence

    else:

        ai_score = 50

    final_score = int(
        (rule_score * 0.5) +
        (ai_score * 0.5)
    )

    if prediction in scam_labels and confidence >= 90:
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

    # Check very small audio files
    if upload_path.stat().st_size < 1000:
        return jsonify({
            "transcript": "",
            "trust_score": 100,
            "rule_score": 100,
            "detected": [],
            "prediction": "audio too short",
            "ai_prediction": "audio too short",
            "confidence": 0,
            "risk_override": False
        })

    # Transcribe audio
    transcript = model.transcribe(str(upload_path))

    text = transcript["text"].strip()

    # Ignore empty transcripts
    if not text:
        return jsonify({
            "transcript": "",
            "trust_score": 100,
            "rule_score": 100,
            "detected": [],
            "prediction": "no speech detected",
            "ai_prediction": "no speech detected",
            "confidence": 0,
            "risk_override": False
        })

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
    # Calculate combined Trust Score
    # -----------------------------

    final_score = calculate_final_score(
        score,
        ai_prediction,
        confidence
    )

    # -----------------------------
    # Final TrustLine risk prediction
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
    print("Final prediction:", final_prediction)
    print("Final trust score:", final_score)

    # -----------------------------
    # Return result to Android
    # -----------------------------

    return jsonify({
        "transcript": text,
        "trust_score": final_score,
        "rule_score": score,
        "detected": reasons,
        "prediction": final_prediction,
        "ai_prediction": ai_prediction,
        "confidence": round(confidence, 2),
        "risk_override": False
    })
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
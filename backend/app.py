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
    # Risk override reasons
    # -----------------------------

    override_reasons = []

    # -----------------------------
    # Base weighted trust score
    # -----------------------------

    final_score = int(
        (rule_score * 0.45) +
        (ai_score * 0.45) +
        (voice_score * 0.10)
    )

    # -----------------------------
    # SECURITY RISK OVERRIDES
    # -----------------------------

    if prediction in scam_labels and confidence >= 80:
        final_score = min(final_score, 30)

        override_reasons.append(
            "High-confidence AI scam detection triggered a security score limit."
        )

    if rule_score <= 30:
        final_score = min(final_score, 40)

        override_reasons.append(
            "Multiple suspicious conversation patterns triggered a risk score limit."
        )

    if voice_label == "fake":
        final_score = min(final_score, 50)

        override_reasons.append(
            "Possible AI-generated or cloned voice triggered a security score limit."
        )

    if (
        prediction in scam_labels
        and confidence >= 70
        and rule_score <= 50
    ):
        final_score = min(final_score, 20)

        override_reasons.append(
            "Critical risk override: AI scam detection combined with suspicious financial language."
        )

    if prediction in scam_labels and confidence >= 90:
        final_score = min(final_score, 15)

        override_reasons.append(
            "Critical risk override: Very high AI confidence indicates a likely scam."
        )

    if voice_label == "fake" and voice_confidence >= 90:
        final_score = min(final_score, 39)

        override_reasons.append(
            "Critical risk override: High-confidence cloned or AI-generated voice detected."
        )

    # Ensure score stays between 0 and 100
    final_score = max(0, min(100, final_score))

    return final_score, ai_score, voice_score, override_reasons
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
            "trust_score": 0,
            "rule_score": 100,
            "detected": [],
            "prediction": "no speech detected",
            "ai_prediction": "no speech detected",
            "confidence": 0,
            "ai_score": 0,

            "risk_override": False,

            "voice_prediction": "unknown",
            "voice_label": "unknown",
            "voice_confidence": 0,
            "voice_score": 0,

            "risk_reasons": [
                "No speech detected in this audio chunk"
            ],
                "voice_analysis": "Voice authenticity could not be determined."
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

    final_score, ai_score, voice_score, override_reasons = calculate_final_score(
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
    # Explainable risk reasons
    # -----------------------------

    risk_reasons = []

    if reasons:
        for reason in reasons:
            risk_reasons.append(
                f"Suspicious keyword detected: {reason}"
            )

    if ai_prediction in {
        "scam phone call",
        "banking fraud",
        "technical support scam",
        "identity theft"
    }:
        risk_reasons.append(
            f"AI classified the conversation as {ai_prediction}"
        )

    if confidence >= 80:
        risk_reasons.append(
            f"AI confidence is {confidence:.2f}%"
        )

    if voice_label == "fake":
        risk_reasons.append(
            f"Possible AI-generated or cloned voice detected "
            f"with {voice_confidence:.2f}% confidence"
        )

    elif voice_label == "real":
        risk_reasons.append(
            f"Voice appears human ({voice_confidence:.2f}% confidence)"
        )
     # -----------------------------
    # Voice explanation
    # -----------------------------

    if voice_label == "fake":
        voice_analysis = (
            "Possible voice clone detected. "
            "Verify the caller's identity independently."
        )

    elif voice_label == "real":
        voice_analysis = (
            "Voice appears human. "
            "This does not guarantee that the conversation is safe."
        )

    else:
        voice_analysis = (
            "Voice authenticity could not be determined."
        )
        # -----------------------------
# Add security override reasons
# -----------------------------

    risk_reasons.extend(override_reasons)
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
        "ai_score": round(ai_score, 2),
        "voice_score": round(voice_score, 2),

        "detected": reasons,
        "risk_reasons": risk_reasons,

        "prediction": final_prediction,
        "ai_prediction": ai_prediction,
        "confidence": round(confidence, 2),

       "risk_override": len(override_reasons) > 0,

        "voice_prediction": voice_prediction,
        "voice_label": voice_label,
        "voice_confidence": round(voice_confidence, 2),
        "voice_analysis": voice_analysis
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
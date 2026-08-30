from transformers import pipeline

print("Loading BERT Scam Call Classifier... (first run may take a few minutes)")

classifier = pipeline(
    "text-classification",
    model="hatim00101/bert-scam-classifier"
)


def classify(text):

    # Run BERT prediction
    result = classifier(text)[0]

    print("BERT result:", result)

    raw_label = result["label"]
    confidence = float(result["score"])

    # Convert model output into TrustLine labels
    #
    # Model:
    # 0 = non_scam
    # 1 = scam
    #
    # Some model versions may expose LABEL_0 / LABEL_1 instead.

    raw_label_lower = raw_label.lower()

    if raw_label_lower in ["1", "label_1", "scam"]:
        label = "scam phone call"

    else:
        label = "normal conversation"

    return {
        "label": label,
        "confidence": confidence
    }
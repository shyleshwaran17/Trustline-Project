from transformers import pipeline

print("Loading AI model... (first run may take a minute)")

classifier = pipeline(
    "zero-shot-classification",
    model="facebook/bart-large-mnli"
)

LABELS = [
    "scam phone call",
    "normal conversation",
    "banking fraud",
    "technical support scam",
    "identity theft"
]

def classify(text):
    result = classifier(text, LABELS)
    print(result)
    return {
        "label": result["labels"][0],
        "confidence": result["scores"][0]
    } 
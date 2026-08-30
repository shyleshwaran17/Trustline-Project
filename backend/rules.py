from scoring import TrustScore

RULES = {

    # Sensitive credentials
    "otp": 30,
    "one time password": 30,
    "password": 30,
    "passcode": 30,
    "pin": 30,
    "pin number": 30,
    "cvv": 35,

    # Banking information
    "bank account": 25,
    "account number": 25,
    "bank details": 25,
    "card number": 30,

    # Money requests
    "send money": 35,
    "transfer": 30,
    "payment": 25,
    "send payment": 35,
    "pay immediately": 35,
    "gift card": 40,
    "crypto": 35,
    "bitcoin": 35,

    # Pressure and urgency
    "urgent": 20,
    "urgently": 20,
    "immediately": 20,
    "emergency": 25,
    "act now": 25,

    # Secrecy / isolation
    "don't tell anyone": 25,
    "do not tell anyone": 25,
    "don't share": 25,
    "do not share": 25,
    "do not share this": 25,
    "keep this confidential": 30,
    "keep it confidential": 30,
    "confidential": 20,
    "secret": 20,
    "keep this secret": 30,

    # Verification / phishing
    "verify account": 20,
    "verify your account": 20,
    "verify your identity": 20,
    "click this link": 20,
    "click the link": 20
}

def analyze(text):

    text = text.lower()

    trust = TrustScore()

    # -----------------------------------------
    # Normal suspicious keyword detection
    # -----------------------------------------

    for keyword, penalty in RULES.items():

        if keyword in text:

            trust.deduct(penalty, keyword)

    # -----------------------------------------
    # Common speech-to-text variations
    # -----------------------------------------

    transcription_variations = {

        "ott": "otp",
        "otb": "otp",
        "o t p": "otp",
        "one time passcode": "otp",

        "account pass": "account number",
        "account password": "password",

        "do not share this": "do not share",
        "don't share this": "don't share",

        "keep it secret": "keep this secret"
    }

    for variation, actual_keyword in transcription_variations.items():

        if variation in text:

            penalty = RULES.get(actual_keyword)

            if penalty:

                trust.deduct(
                    penalty,
                    actual_keyword
                )

    return trust.result()
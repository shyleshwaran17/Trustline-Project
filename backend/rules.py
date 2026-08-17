from scoring import TrustScore

RULES = {

    "otp": 30,
    "password": 30,
    "send money": 35,
    "transfer": 30,
    "gift card": 40,
    "bank account": 25,
    "account number": 25,
    "pin": 30,
    "pin number": 30,
    "verify account": 20,
    "urgent": 20,
    "immediately": 20,
    "click this link": 20,
    "don't tell anyone": 25,
    "confidential information": 20,
    "crypto": 35,
    "bitcoin": 35

}


def analyze(text):

    text=text.lower()

    trust=TrustScore()

    for keyword, penalty in RULES.items():

        if keyword in text:

            trust.deduct(penalty, keyword)

    return trust.result()
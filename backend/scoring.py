class TrustScore:

    def __init__(self):
        self.score = 100
        self.reasons = []

    def deduct(self, points, reason):
        self.score -= points
        self.reasons.append(reason)

    def result(self):
        self.score = max(self.score, 0)
        return self.score, self.reasons
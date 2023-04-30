import nltk
from nltk.corpus import stopwords
import re

nltk.download('stopwords')

def normalize_text(text):
    text = text.lower()
    return " ".join(text.split())
    words = text.lower().split(" ")

    # Remove stopwords
    words = [word for word in words if word not in stopwords.words('english')]

    return_val = []
    for word in words:
        if word.startswith("@"):
            return_val.append("USER_MENTION")
            word = word[1:]
        elif word.startswith("#"):
            return_val.append("#HASHTAG")
            word = word[1:]
        return_val.append(word)
    return " ".join(return_val)

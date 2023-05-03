import nltk
from nltk.corpus import stopwords
from nltk.stem import WordNetLemmatizer
import re

nltk.download('stopwords')
nltk.download('wordnet')

lemmatizer = WordNetLemmatizer()

def normalize_text(text, remove_stopwords=False, lemmatize=False):
    """
        Normalizes text by removing stopwords and lemmatizing words based on the
        arguments passed in
    """
    text = text.lower()

    words = text.split()
    if lemmatize:
        words = [lemmatizer.lemmatize(word) for word in words]
    if remove_stopwords:
        words = [word for word in words if word not in stopwords.words("english")]
    return " ".join(words)


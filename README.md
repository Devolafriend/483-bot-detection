# 483-bot-detection


## How to run the Python code


First install the required packages:

```bash
pip install -r python/requirements.txt
```

Run this code to format the data:
```bash
python3 python/process_data.py
```
Optional Arguments:
```bash
--remove-stopwords : Remove stopwords from the tweets
--lemmatize : Lemmatize the tweets
```

Then run the code:
```bash
python3 python/main.py
```
Optional arguments:
```bash
--remove-stopwords : Remove stopwords from the tweets
```


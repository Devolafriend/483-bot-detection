import os
import random
from utils import normalize_text
from os import path
import sys

def remove_dir_files(path):
    for file in os.listdir(path):
        os.remove(os.path.join(path, file))

def update_dir(path, data, msg=""):
    remove_dir_files(path)
    for i in range(len(data)):
        file = open(os.path.join(path, str(i) + ".txt"), "w")
        file.write(data[i])
        file.close()
        if (i == 100):
            print(f"{msg} : First 100 files done")
        if (i % 1000 == 0):
            print(f"{msg} {i}/{len(data)}")

def open_file(file_path):
    file = open(file_path, 'r', encoding="utf8")
    lines = file.readlines()
    file.close()
    return lines

def main_process_data(args):
    remove = True if "--remove-stopwords" in args else False
    lemmatize = True if "--lemmatize" in args else False
    if remove:
        print("Removing stopwords")
    if lemmatize:
        print("Lemmatizing")
    process(remove, lemmatize)

    

def process(remove_stopwords=False, lemmatize=False):
    cwd_path = path.dirname(path.realpath(__file__))
    lines = open_file(path.join(cwd_path, "data.csv"))
    bot = []
    human = []
    print("Total lines: ", len(lines))
    max = len(lines)
    i = 0
    for raw_line in lines:
        line = raw_line.strip().split(",")
        line = line
        if (len(line) <= 1):
            continue
        if line[-1] == "1":
            bot.append(normalize_text(" ".join(line[:-1]), remove_stopwords, lemmatize))
        elif line[-1] == "0":
            human.append(normalize_text(" ".join(line[:-1]), remove_stopwords, lemmatize))
        else:
            pass
        i += 1
        if (i % 1000 == 0):
            print(f"{i}/{max}")
     
    # Print first 10 line of each
    print("Bot: ", bot[:10])
    print("Human: ", human[:10])    

    print("Bot: ", len(bot))
    print("Human: ", len(human))
    print("Total: ", len(bot) + len(human))
    print("Unlabeled: ", len(lines) - len(bot) - len(human))

    TRAINING_PERCENT = 0.7
    VALIDATION_PERCENT = 0.2
    TESTING_PERCENT = 0.1

    # Set random seed
    random.seed(42)

    # Shuffle data
    random.shuffle(bot)
    random.shuffle(human)

    # Split data
    bot_train = bot[:int(len(bot) * TRAINING_PERCENT)]
    bot_test = bot[int(len(bot) * (TRAINING_PERCENT + VALIDATION_PERCENT)):]
    human_train = human[:int(len(human) * TRAINING_PERCENT)]
    human_test = human[int(len(human) * (TRAINING_PERCENT + VALIDATION_PERCENT)):]
    print("Bot train: ", len(bot_train))
    print("Bot test: ", len(bot_test))
    print("Human train: ", len(human_train))
    print("Human test: ", len(human_test))

    # Clear bot and human directories

    data_path = path.join(cwd_path, "data")
    test_dir = os.path.join(data_path, "test")
    train_dir = os.path.join(data_path, "train")

    bot_train_path = os.path.join(train_dir, "bot")
    bot_test_path = os.path.join(test_dir, "bot")
    human_train_path = os.path.join(train_dir, "human")
    human_test_path = os.path.join(test_dir, "human")

    update_dir(bot_train_path, bot_train, "bot train")
    update_dir(bot_test_path, bot_test, "bot test")
    update_dir(human_train_path, human_train, "human train")
    update_dir(human_test_path, human_test, "human test")
        
if __name__ == "__main__":
    main_process_data(sys.argv[1:])
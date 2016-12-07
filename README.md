# Clinical Abbreviation and Phrase Normalizer (CAP'N)

## About this project

This is work that I did in 2016 as a postdoc in the NLP/IE group in the Institute for Health Informatics at the University of Minnesota.
The process is designed for clinical (EHR/EMR) text, but it would work for other types of text just fine.

(Code cleaning and documentation are still in progress. If you were to express an interest in using this repository, that would be excellent motivation for me to do that more quickly.)

CAP'N generates a thesaurus for looking up equivalent or similar words, phrases, and abbreviations.
It does this in a totally unsupervised manner by:
- automatically detecting likely phrases in text (using n-gram counts and modified PMI-based criteria)
- measuring semantic similarity between all words/phrases (using word embeddings)
- measuring orthographic similarity (using a modified edit distance measure and a specialized abbreviation measure)
- calculating and thresholding on overall similarity between phrases

CAP'N outputs a human- and machine-readable plaintext thesaurus and includes an API (the Thesaurus class) for reading the thesaurus and looking up words.
Another possibility is a clustered thesaurus, in which similar terms are all grouped together rather than ranked and scored by lookup term (API in the ClusteredThesaurus class).

## Building a thesaurus from text

Build the Java project with Maven. (This version has no dependencies, so you could get by without Maven.)

    mvn install

See full_process.sh. You'll need SRILM and word2vec (both free) installed; modify the script to point to their directories.

    ./full_process.sh <large-text-corpus-in-a-single-file>

(Running word2vec and especially SRILM will generate large files on disk and use a lot of memory, so be sure you have space on hand.)

Modifications to parameters should be done in full_process.sh and in default.properties.
If you want to run CAP'N classes using other properties files, pass -DpropertiesFile=<...> to the JVM.

## Using the API

See the Thesaurus and ClusteredThesaurus classes for working with built thesauri.

You may also get some mileage out of overriding the TextProcessor interface; see the included sources in the textprocessing package for ideas.

## Included experiment code

Basic experiments (really more data overview analyses) are included as well. They require a UMLS installation; see the sources.

## Other stuff

Code includes some little utilities that could be useful—mostly painless multithreading method, a word2vec bin file reader, a Phrase class (with more flexibility than String), an algorithm for building graphs of words in phrases, etc.
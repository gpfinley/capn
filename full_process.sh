#!/bin/bash

# set these to the folders containing SRILM and word2vec executables
SRILM_HOME="../srilm/bin/i686-m64/"
WORD2VEC_HOME="../word2vec/"

PATH=$WORD2VEC_HOME:$SRILM_HOME:$PATH
CLASSPATH=target/classes/

# decrease this if srilm runs out of memory
splitsize=10000000

# minimum number of counts to keep any n-gram
minwordcount=100

# if making a clustered thesaurus, adjust these until clusters are reasonable
prethesaurustrim=250000
nclusters=100000

# intermediate filenames
unprocessedname=$1
preprocessedname=allnotes_processed.txt
collocatedname=allnotes_collocated.txt
collocations=collocations.txt
prefix=allnotes_split
w2vbin=vectors.bin
w2vvocab=vocab.txt
thesaurus=thesaurus.txt

java -cp $CLASSPATH run.PreprocessText $unprocessedname $preprocessedname 

# split before n-gram counts 
split -d -l $splitsize $preprocessedname $prefix

# TODO: figure out how to make the seq go up to less than 10
for i in `seq 0 10`
do
	if [ '$i<3' ]; then i=0$i; fi
	fname=$prefix$i
	uniname=unigrams_part$i
	biname=bigrams_part$i
	triname=trigrams_part$i
	quadname=quadgrams_part$i
	quintname=quintgrams_part$i
	ngram-count -text $fname -write1 $uniname -write2 $biname -write3 $triname -write4 $quadname -write5 $quintname -no-eos -no-sos -sort -order 5 && rm $fname
done

uniname=1grams.txt
biname=2grams.txt
triname=3grams.txt
quadname=4grams.txt
quintname=5grams.txt
ngram-merge -write $uniname-unpruned unigrams* && rm unigrams_part*
ngram-merge -write $biname-unpruned bigrams* && rm bigrams_part*
ngram-merge -write $triname-unpruned trigrams* && rm trigrams_part*
ngram-merge -write $quadname-unpruned quadgrams* && rm quadgrams_part*
ngram-merge -write $quintname-unpruned quintgrams* && rm quintgrams_part*

java -cp $CLASSPATH run.PruneNgramCounts $uniname-unpruned $uniname && rm $uniname-unpruned
java -cp $CLASSPATH run.PruneNgramCounts $biname-unpruned $biname && rm $biname-unpruned
java -cp $CLASSPATH run.PruneNgramCounts $triname-unpruned $triname && rm $triname-unpruned
java -cp $CLASSPATH run.PruneNgramCounts $quadname-unpruned $quadname && rm $quadname-unpruned
java -cp $CLASSPATH run.PruneNgramCounts $quintname-unpruned $quintname && rm $quintname-unpruned

java -cp $CLASSPATH run.CollocationsBuilder $collocations
java -cp $CLASSPATH run.ApplyCollocations $collocations $preprocessedname $collocatedname

word2vec -train $collocatedname -output $w2vbin -cbow 1 -size 300 -window 10 -negative 25 -hs 0 -sample 1e-5 -min-count $minwordcount -threads 20 -binary 1 -iter 15 -save-vocab $w2vvocab

java -cp $CLASSPATH run.MakeThesaurus $w2vbin $thesaurus

java -cp $CLASSPATH run.ClusterThesaurus $w2vvocab $prethesaurustrim $thesaurus $nclusters $clusteredthesaurus

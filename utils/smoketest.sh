#!/bin/zsh

set -e
script_dir=${0:a:h}
# A simple script to generate models for testing

# Environment parameters
ud_version=2.12
ud_home="${UD_HOME:-$HOME/Downloads/ud-treebanks-v${ud_version}}"
opennlp_version="${OPENNLP_VER:-1.9.4}"
opennlp_home="${OPENNLP_HOME:-$HOME/Software/apache-opennlp-${opennlp_version}}"

# Script parameters
language="${LANGUAGE:-English}"
lg="${LG:-en}"
ud_set="${UD_SET:-EWT}"
ud_set_lower="${ud_set:l}"
ud_variant="${UD_VARIANT:-train}"
output_dir="${script_dir}/../src/yamlRestTest/resources/config/opennlp_analysis"
opennlp_opts=-Dlog4j.configurationFile=$opennlp_home/conf/log4j2.xml
opennlp_classpath="$opennlp_home/lib/*"
data_file="$ud_home/UD_$language-$ud_set/${lg}_${ud_set_lower}-ud-$ud_variant.conllu"
sentence_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-sentence-${ud_version}-${opennlp_version}.bin"
tokenizer_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-tokens-${ud_version}-${opennlp_version}.bin"
pos_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-pos-${ud_version}-${opennlp_version}.bin"
lemmatizer_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-lemmatizer-${ud_version}-${opennlp_version}.bin"

echo "Варкалось. Хливкие шорьки пырялись по наве, и хрюкотали зелюки, как мюмзики в мове. Глокая куздра штеко будланула бокра и курдячит бокрёнка." |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  SentenceDetector "$sentence_model" 2> /dev/null |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  TokenizerME "$tokenizer_model" 2> /dev/null |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  POSTagger "$pos_model" 2> /dev/null  |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  LemmatizerME "$lemmatizer_model" 2> /dev/null

echo "Несмотря на это, общий смысл фразы понятен: некоторая определённым образом характеризуемая сущность женского рода что-то сделала определённым образом с другим существом мужского пола, а затем начала (и продолжает до настоящего момента) делать что-то другое с его детёнышем (или более мелким представителем того же вида). Фраза создана для иллюстрации того, что многие семантические признаки слова можно понять из его морфологии." |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  SentenceDetector "$sentence_model" 2> /dev/null |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  TokenizerME "$tokenizer_model" 2> /dev/null |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  POSTagger "$pos_model" 2> /dev/null  |
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  LemmatizerME "$lemmatizer_model" 2> /dev/null
#!/bin/zsh

# A simple script to generate models for testing

set -e
# Configurable settings
opennlp_version="${OPENNLP_VERSION:-1.9.4}"
language="${LANGUAGE:-English}"
lg="${LG:-en}"
ud_set="${UD_SET:-EWT}"

script_dir=${0:a:h}
data_dir="${script_dir}/data"
# Create data dir if it doesn't exist
[[ -d "$data_dir" ]] || mkdir -p "$data_dir"


# Download opennlp distro if needed
opennlp_name="apache-opennlp-${opennlp_version}"
opennlp_home="${script_dir}/data/${opennlp_name}"
opennlp_url="https://archive.apache.org/dist/opennlp/opennlp-${opennlp_version}/${opennlp_name}-bin.tar.gz"
if [[ ! -d "$opennlp_home" ]]; then
  curl -L -o "${opennlp_home}-bin.tar.gz" "$opennlp_url"
  tar -xzf "${opennlp_home}-bin.tar.gz" --directory "$data_dir"
  rm "${opennlp_home}-bin.tar.gz"
fi

# Download Universal Dependencies if needed
ud_version="2.12"
ud_home="${data_dir}/ud-treebanks-v${ud_version}"
ud_url="https://lindat.mff.cuni.cz/repository/xmlui/bitstream/handle/11234/1-5150/ud-treebanks-v2.12.tgz"
if [[ ! -d "$ud_home" ]]; then
  curl -L -o "${ud_home}.tgz" "$ud_url"
  tar -xzf "${ud_home}.tgz" --directory "$data_dir"
  rm "${ud_home}.tgz"
fi

ud_set_lower="${ud_set:l}"
output_dir="${script_dir}/../src/yamlRestTest/resources/config/opennlp_analysis/${lg}-ud-${ud_set_lower}-${ud_version}"
opennlp_opts=-Dlog4j.configurationFile=$opennlp_home/conf/log4j2.xml
opennlp_classpath="$opennlp_home/lib/*"
licence_file="$ud_home/UD_$language-$ud_set/LICENSE.txt"
readme_file="$ud_home/UD_$language-$ud_set/README.md"
data_file="$ud_home/UD_$language-$ud_set/${lg}_${ud_set_lower}-ud-train.conllu"
test_file="$ud_home/UD_$language-$ud_set/${lg}_${ud_set_lower}-ud-test.conllu"
sentence_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-sentence-${ud_version}-${opennlp_version}.bin"
tokenizer_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-tokens-${ud_version}-${opennlp_version}.bin"
pos_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-pos-${ud_version}-${opennlp_version}.bin"
lemmatizer_model="$output_dir/opennlp-${lg}-ud-${ud_set_lower}-lemmatizer-${ud_version}-${opennlp_version}.bin"

# Create an output directory and copy appropriate licenses
mkdir -p "$output_dir"
cp "$licence_file" "$readme_file" "$output_dir"

# Sentence Detector Training
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  SentenceDetectorTrainer.conllu -lang "$lg" -params "$script_dir/sentence_params.txt" -model "$sentence_model" -sentencesPerSample 1 -data "$data_file" -encoding UTF-8

# Tokenizer Training
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  TokenizerTrainer.conllu -lang "$lg" -params "$script_dir/tokens_params.txt" -model "$tokenizer_model" -data "$data_file" -encoding UTF-8

# POS Tagger Training
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  POSTaggerTrainer.conllu -lang "$lg" -params "$script_dir/pos_params.txt" -model "$pos_model" -data "$data_file" -encoding UTF-8

# Lemmatizer Training
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  LemmatizerTrainerME.conllu -lang "$lg" -params "$script_dir/lemmatizer_params.txt" -model "$lemmatizer_model" -data "$data_file" -encoding UTF-8

# Lemmatizer Evaluation
java $opennlp_opts -cp "$opennlp_classpath" opennlp.tools.cmdline.CLI \
  LemmatizerEvaluator.conllu -model "$lemmatizer_model" -data "$test_file" -encoding UTF-8

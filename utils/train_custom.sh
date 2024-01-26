#!/bin/zsh

# A simple script to generate models for testing

set -e
# Configurable settings
opennlp_version="${OPENNLP_VERSION:-1.9.4}"
lg="${LG:-en}"
dataset_name="${DATASET_NAME:-treebank-9-rc1-unique}"
dataset_path="${DATASET_PATH:-/Users/igor/Data/IAHLT/custom/treebank-9-rc1}"

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

output_dir="${script_dir}/../src/test/resources/${dataset_name}"

opennlp_opts=-Dlog4j.configurationFile=$opennlp_home/conf/log4j2.xml
opennlp_classpath="$opennlp_home/lib/*"
data_file="$dataset_path/${dataset_name}-train.conllum"
test_file="$dataset_path/${dataset_name}-test.conllu"
sentence_model="$output_dir/${dataset_name}-sentence-${opennlp_version}.bin"
tokenizer_model="$output_dir/${dataset_name}-tokens-${opennlp_version}.bin"
pos_model="$output_dir/${dataset_name}-pos-${opennlp_version}.bin"
lemmatizer_model="$output_dir/${dataset_name}-lemmatizer-${opennlp_version}.bin"

# Create an output directory and copy appropriate licenses
mkdir -p "$output_dir"

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

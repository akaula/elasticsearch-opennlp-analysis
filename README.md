OpenNLP Plugin for elasticsearch
=============================

## Installation

The plugin can be installed using the plugin manager utility:

```shell
sudo bin/elasticsearch-plugin install https://plugin_url/
```
Please, make sure that the plugin URL corresponds to the version of elasticsearch that you are installing it on.

For example in order to install plugin v8.15.0+0 for elasticsearch v8.15.0 run

```shell
sudo bin/elasticsearch-plugin install https://github.com/akaula/elasticsearch-opennlp-analysis/releases/download/v8.15.0%2B0/opennlp-analysis-8.15.0+0.zip
```

The installation command needs to be executed on all nodes in the cluster, after which all nodes have to be restarted before you can start
using this plug functionality.

## Offline installation

To perform installation on the system that are not connected to internet, download the corresponding plugin file from the table above and
install it using `file:` url:

```shell
sudo bin/elasticsearch-plugin install file:///path_to_plugin_zip_file
```

## Removal

Before uninstalling the plugin make sure that you don't have any indices that are using plugin any tokenizers and filters provided by this
plugin. Once all indices are deleted remove the plugin from all node using the following command:


```shell
sudo bin/elasticsearch-plugin remove opennlp-analysis
```

## Plugin functionality

The `analysis-openlp` plugin provides `opennlp` tokenizer and two token filters: `opennlp_pos` and `opennlp_lemmatizer`, that expose that
functionality provided by [Apache OpenNLP](https://opennlp.apache.org) library version 1.9.1.

## Models

The plugin requires models trained using OpenNLP 1.9.4 or bellow. The model files have to be placed into `config` directory of each
elasticesearch node. For future compatibility we recommend placing the model into `config/opennlp-analysys` directory. Please note that
paths to the models specified in the settings are resolved relative to the `config` directory.

### OpenNLP Tokenizer

The `opennlp` tokenizer wraps OpenNLP Sentence Detector and Tokenizer. The tokenizer requires the following settings to be specified:

- `sentence_model_path` - should point to the OpenNLP Sentence Detector model file
- `tokenizer_model_path`  - should point to the OpenNLP Tokenizer model file

Assuming that both models reside in the `config/opennlp-analysys` directory the following elasticsearch command will test the tokenizer
on the supplied string:

```
POST _analyze
{
  "tokenizer": {
    "type": "opennlp",
    "sentence_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-sentence-2.12-1.9.4.bin",
    "tokenizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-tokens-2.12-1.9.4.bin"
  },
  "text": ["Don't Panic!"]
}
```

### OpenNLP POS Token Filter

The `opennlp_pos` token filter provides OpenNLP Part-of-Speech Tagger functionality. This token filter requires the following setting to be
specified:

- `pos_model_path` - should point to the OpenNLP Part-of-Speech Tagger model.

Assuming that the pos models reside in the `config/opennlp-analysys` directory the following elasticsearch command will test the token
filter on the supplied string:

```
POST _analyze
{
  "tokenizer": {
    "type": "opennlp",
    "sentence_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-sentence-2.12-1.9.4.bin",
    "tokenizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-tokens-2.12-1.9.4.bin"
  },
  "filter": {
    "type": "opennlp_pos",
      "pos_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-pos-2.12-1.9.4.bin"
  },
  "text": ["This is a test."]
}
```

The OpenNLP POS Token Filter is expected to be used in an analyzer together with `opennlp` tokenizer, but it can work with other tokenizers
as well.

### OpenNLP Lemmatizer Token Filter

The `opennlp_lemmatizer` token filter provides OpenNLP DictionaryLemmatizer and MELemmatizer functionalities. This filter requires at least
one of the following setting to be specified:

- `dictionary_path` - should point to the dictionary file in UTF-8 format in which each line has a for of `word[tab]lemma[tab]part-of-speech`.
- `lemmatizer_model_path` - should point to the OpenNLP Part-of-Speech Tagger model.

Either `dictionary_path` or `lemmatizer_model_path` have to be specified. If both of them are present, the filter tries to resolve each
token against the dictionary first and if no matches are found, the token is resolved using the supplied model.

The `opennlp_lemmatizer` token filter expects all tokens to have part-of-speech tag assigned by the OpenNLP POS Token Filter, which should
appear earlier in the analysis chain.

Assuming that the pos models reside in the `config/opennlp-analysys/en-ud-ewt-2.12` directory the following elasticsearch command will test
the token filter on the supplied string. Please, not that for the optimal performance it is using `opennlp` tokenizer and `opennlp_pos`
token filter prior to invoking the lemmatizer.

```
POST _analyze
{
  "tokenizer": {
    "type": "opennlp",
    "sentence_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-sentence-2.12-1.9.4.bin",
    "tokenizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-tokens-2.12-1.9.4.bin"
  },
  "filter": [
    {
      "type": "opennlp_pos",
      "pos_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-pos-2.12-1.9.4.bin"
    },
    {
      "type": "opennlp_lemmatizer",
      "lemmatizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-lemmatizer-2.12-1.9.4.bin"
    }
  ],
  "text": [
    "This is a test."
  ]
}
```

### General recommendation on using OpenNLP components in index settings:

Depending on the preprocessing done to the text using to train OpenNLP model, it might be useful to do some normalization to punctuation
symbols. For example, symbols such as `‘` or `’` can be normalized to `'`. These operations can be done by adding `mapping` character
filter to the analyzer.

Some lemmatizer models produce multiple output tokens for a single input token. In such cases the output tokens will be separated by `+`.
For example, an English contraction `isn't` might be converted into `be+not`. An `pattern_capture` token filter can be added after the
lemmatizer in order split such tokens.

Combining all these methods together we will end up with the analyzer like this:



```
PUT test
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "char_filter": {
        "opennlp_quotes": {
          "type": "mapping",
          "mappings": [
            "`=>'",
            "´=>'",
            "‘=>'",
            "’=>'",
            "“=>\"",
            "”=>\""
          ]
        }
      },
      "tokenizer": {
        "en_opennlp": {
          "type": "opennlp",
          "sentence_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-sentence-2.12-1.9.4.bin",
          "tokenizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-tokens-2.12-1.9.4.bin"
        }
      },
      "filter": {
        "en_opennlp_pos": {
          "type": "opennlp_pos",
          "pos_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-pos-2.12-1.9.4.bin"
        },
        "en_opennlp_lemmatizer": {
          "type": "opennlp_lemmatizer",
          "lemmatizer_model_path": "opennlp_analysis/en-ud-ewt-2.12/opennlp-en-ud-ewt-lemmatizer-2.12-1.9.4.bin"
        },
        "open_nlp_splitter": {
          "type": "word_delimiter_graph",
          "preserve_original": "false",
          "type_table": [ "- => ALPHA" ],
          "split_on_case_change": false,
          "split_on_numerics": false,
          "stem_english_possessive": false
        }
      },
      "analyzer": {
        "en_opennlp": {
          "char_filter": "opennlp_quotes",
          "tokenizer": "en_opennlp",
          "filter": [
            "en_opennlp_pos",
            "en_opennlp_lemmatizer",
            "open_nlp_splitter"
          ]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "text": {
        "type": "text",
        "analyzer": "en_opennlp"
      }
    }
  }
}
```



## Building the plugin from the sources:

To build the plugin from the sources checkout the appropriate branch, update the `build.gradle` file to select required version and run the
following command:

```shell
./gradlew build`
```

If build is successful, the assembled plugin file can be found in the `build/distributions/` directory. 



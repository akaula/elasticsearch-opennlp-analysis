#!/bin/zsh
set -e

LANGUAGE=English LG=en UD_SET=EWT source ./train.sh
LANGUAGE=Hebrew LG=he UD_SET=IAHLTwiki source ./train.sh
LANGUAGE=Russian LG=ru UD_SET=Taiga source ./train.sh

#!/bin/bash

rm -f ~/a.rar
rar a -mde -m5 -s ~/a.rar bin classes lib output res tmpclasses tmplib src
#cd _backup/old
#rar a -mde -m5 -s ~/a.rar src/My*.java

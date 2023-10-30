#!/usr/bin/env bash

sudo dd if=/dev/zero of=/pmem0/pMemHeap bs=1024 count=1024; sync

java -jar target/benchmarks.jar PetStoreBenchmark -f 1 -wi 1 -i 1

cmake_minimum_required(VERSION 3.22.1)
project(boostfilter)

add_library(
    boostfilter
    SHARED
    native-lib.cpp
)

find_library(log-lib log)

target_compile_features(boostfilter PRIVATE cxx_std_17)
target_compile_options(boostfilter PRIVATE -O2 -Wall -Wextra)
target_link_libraries(boostfilter ${log-lib})

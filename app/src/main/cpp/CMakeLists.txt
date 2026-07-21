cmake_minimum_required(VERSION 3.4.1)

add_library(
        opensles_sink
        SHARED
        opensles_sink.cpp)

find_library(
        log-lib
        log)

# OpenSLES 라이브러리 링크! 이게 핵심입니다.
target_link_libraries(
        opensles_sink
        ${log-lib}
        OpenSLES)

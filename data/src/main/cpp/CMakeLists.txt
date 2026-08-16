cmake_minimum_required(VERSION 3.22.1)
project(arcx_native C CXX)

set(ENABLE_CPAX OFF CACHE BOOL "" FORCE)
set(ENABLE_TAR OFF CACHE BOOL "" FORCE)
set(ENABLE_OPENSSL OFF CACHE BOOL "" FORCE)
set(ENABLE_CAT OFF CACHE BOOL "" FORCE)
set(ENABLE_TEST OFF CACHE BOOL "" FORCE)
set(ENABLE_WERROR OFF CACHE BOOL "" FORCE)
set(ENABLE_LIBXML2 OFF CACHE BOOL "" FORCE)
set(ENABLE_EXPAT OFF CACHE BOOL "" FORCE)
set(ENABLE_PCREPOSIX OFF CACHE BOOL "" FORCE)
set(ENABLE_LIBB2 OFF CACHE BOOL "" FORCE)
set(ENABLE_LZ4 OFF CACHE BOOL "" FORCE)
set(ENABLE_LZO OFF CACHE BOOL "" FORCE)
set(ENABLE_LZMA ON CACHE BOOL "" FORCE)
set(ENABLE_ZSTD OFF CACHE BOOL "" FORCE)
set(ENABLE_MBEDTLS OFF CACHE BOOL "" FORCE)
set(ENABLE_NETTLE OFF CACHE BOOL "" FORCE)
set(ENABLE_ICONV OFF CACHE BOOL "" FORCE)
set(ENABLE_XATTR OFF CACHE BOOL "" FORCE)
set(ENABLE_ACL OFF CACHE BOOL "" FORCE)
set(ENABLE_ZLIB ON CACHE BOOL "" FORCE)

include(FetchContent)

FetchContent_Declare(
    libarchive
    URL https://github.com/libarchive/libarchive/releases/download/v3.7.7/libarchive-3.7.7.tar.gz
)
FetchContent_MakeAvailable(libarchive)

add_library(
    arcx_native
    SHARED
    native_archive.cpp
)

find_library(log-lib log)

target_include_directories(arcx_native PRIVATE ${libarchive_SOURCE_DIR}/libarchive)

target_link_libraries(
    arcx_native
    archive
    z
    ${log-lib}
)

#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <archive.h>
#include <archive_entry.h>
#include <sys/stat.h>
#include <unistd.h>
#include <limits.h>

#define LOG_TAG "ArcX_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static void throwIOException(JNIEnv *env, const char *msg) {
    jclass exClass = env->FindClass("java/io/IOException");
    if (exClass != nullptr) {
        env->ThrowNew(exClass, msg);
    }
}

static int copy_data(struct archive *ar, struct archive *aw) {
    int r;
    const void *buff;
    size_t size;
    int64_t offset;

    while (true) {
        r = archive_read_data_block(ar, &buff, &size, &offset);
        if (r == ARCHIVE_EOF)
            return (ARCHIVE_OK);
        if (r < ARCHIVE_OK)
            return (r);
        r = archive_write_data_block(aw, buff, size, offset);
        if (r < ARCHIVE_OK) {
            return (r);
        }
    }
}

static bool is_safe_path(const std::string& base_dir, const std::string& target_path) {
    if (target_path.find("..") != std::string::npos) {
        char resolved_base[PATH_MAX];
        char resolved_target[PATH_MAX];

        if (realpath(base_dir.c_str(), resolved_base) != nullptr) {
            if (realpath(target_path.c_str(), resolved_target) != nullptr) {
                return std::string(resolved_target).find(resolved_base) == 0;
            }
        }
        return false;
    }
    return true;
}

static int count_archive_entries(const char *archive_path, const char *password) {
    struct archive *a = archive_read_new();
    archive_read_support_filter_all(a);
    archive_read_support_format_all(a);

    if (password != nullptr && strlen(password) > 0) {
        archive_read_add_passphrase(a, password);
    }

    if (archive_read_open_filename(a, archive_path, 10240) != ARCHIVE_OK) {
        archive_read_free(a);
        return 0;
    }

    int count = 0;
    struct archive_entry *entry;
    while (archive_read_next_header(a, &entry) == ARCHIVE_OK) {
        count++;
        archive_read_data_skip(a);
    }

    archive_read_close(a);
    archive_read_free(a);
    return count;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_com_m5dev_arcx_data_ndk_ArchiveNative_listArchiveContents(
        JNIEnv *env,
        jobject thiz,
        jstring archive_path_jstr) {

    if (archive_path_jstr == nullptr) {
        throwIOException(env, "Archive path is null");
        return nullptr;
    }

    const char *archive_path = env->GetStringUTFChars(archive_path_jstr, nullptr);
    if (archive_path == nullptr) {
        throwIOException(env, "Failed to get archive path string");
        return nullptr;
    }

    struct archive *a = archive_read_new();
    archive_read_support_filter_all(a);
    archive_read_support_format_all(a);

    int r = archive_read_open_filename(a, archive_path, 10240);
    if (r != ARCHIVE_OK) {
        std::string err = "Failed to open archive: ";
        err += archive_error_string(a) ? archive_error_string(a) : "Unknown error";
        env->ReleaseStringUTFChars(archive_path_jstr, archive_path);
        archive_read_free(a);
        throwIOException(env, err.c_str());
        return nullptr;
    }

    std::vector<std::string> file_list;
    struct archive_entry *entry;
    while ((r = archive_read_next_header(a, &entry)) == ARCHIVE_OK) {
        const char *pathname = archive_entry_pathname(entry);
        if (pathname != nullptr) {
            file_list.push_back(std::string(pathname));
        }
        archive_read_data_skip(a);
    }

    if (r != ARCHIVE_EOF && r != ARCHIVE_OK) {
        LOGE("Error reading archive header: %s", archive_error_string(a));
    }

    archive_read_close(a);
    archive_read_free(a);
    env->ReleaseStringUTFChars(archive_path_jstr, archive_path);

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray resultArray = env->NewObjectArray((jsize)file_list.size(), stringClass, nullptr);

    for (size_t i = 0; i < file_list.size(); ++i) {
        jstring jstr = env->NewStringUTF(file_list[i].c_str());
        env->SetObjectArrayElement(resultArray, (jsize)i, jstr);
        env->DeleteLocalRef(jstr);
    }

    return resultArray;
}

static jboolean perform_extraction(
        JNIEnv *env,
        const char *archive_path,
        const char *dest_path,
        const char *password,
        jobject listener) {

    int total_entries = 0;
    jmethodID onProgressMethod = nullptr;
    if (listener != nullptr) {
        total_entries = count_archive_entries(archive_path, password);
        if (total_entries <= 0) total_entries = 1;

        jclass listenerClass = env->GetObjectClass(listener);
        if (listenerClass != nullptr) {
            onProgressMethod = env->GetMethodID(listenerClass, "onProgress", "(IILjava/lang/String;)Z");
        }
    }

    struct archive *a = archive_read_new();
    struct archive *ext = archive_write_disk_new();

    archive_read_support_filter_all(a);
    archive_read_support_format_all(a);

    if (password != nullptr && strlen(password) > 0) {
        archive_read_add_passphrase(a, password);
    }

    int flags = ARCHIVE_EXTRACT_TIME | ARCHIVE_EXTRACT_PERM | ARCHIVE_EXTRACT_ACL | ARCHIVE_EXTRACT_FFLAGS;
    archive_write_disk_set_options(ext, flags);
    archive_write_disk_set_standard_lookup(ext);

    int r = archive_read_open_filename(a, archive_path, 10240);
    if (r != ARCHIVE_OK) {
        std::string err = "Failed to open archive: ";
        err += archive_error_string(a) ? archive_error_string(a) : "Unknown error";
        archive_read_free(a);
        archive_write_free(ext);
        throwIOException(env, err.c_str());
        return JNI_FALSE;
    }

    struct archive_entry *entry;
    std::string base_dest(dest_path);
    if (!base_dest.empty() && base_dest.back() != '/') {
        base_dest += '/';
    }

    bool success = true;
    std::string error_msg = "";
    int current_index = 0;

    while (true) {
        r = archive_read_next_header(a, &entry);
        if (r == ARCHIVE_EOF) break;
        if (r < ARCHIVE_OK) {
            error_msg = archive_error_string(a) ? archive_error_string(a) : "Header read error";
            if (r < ARCHIVE_WARN) {
                success = false;
                break;
            }
        }

        const char *current_entry = archive_entry_pathname(entry);
        if (current_entry == nullptr) continue;

        current_index++;
        if (listener != nullptr && onProgressMethod != nullptr) {
            jstring nameJStr = env->NewStringUTF(current_entry);
            jboolean shouldContinue = env->CallBooleanMethod(listener, onProgressMethod, (jint)current_index, (jint)total_entries, nameJStr);
            env->DeleteLocalRef(nameJStr);

            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                shouldContinue = JNI_FALSE;
            }

            if (!shouldContinue) {
                error_msg = "Extraction canceled";
                success = false;
                break;
            }
        }

        std::string full_dest_path = base_dest + current_entry;

        if (!is_safe_path(base_dest, full_dest_path)) {
            error_msg = "Security exception: Invalid entry path (Zip Slip detected)";
            success = false;
            break;
        }

        archive_entry_set_pathname(entry, full_dest_path.c_str());

        r = archive_write_header(ext, entry);
        if (r < ARCHIVE_OK) {
            error_msg = archive_error_string(ext) ? archive_error_string(ext) : "Write header error";
            if (r < ARCHIVE_WARN) {
                success = false;
                break;
            }
        } else if (archive_entry_size(entry) > 0) {
            r = copy_data(a, ext);
            if (r < ARCHIVE_OK) {
                error_msg = archive_error_string(a) ? archive_error_string(a) : "Copy data error";
                if (r < ARCHIVE_WARN) {
                    success = false;
                    break;
                }
            }
        }

        r = archive_write_finish_entry(ext);
        if (r < ARCHIVE_OK) {
            error_msg = archive_error_string(ext) ? archive_error_string(ext) : "Finish entry error";
            if (r < ARCHIVE_WARN) {
                success = false;
                break;
            }
        }
    }

    archive_read_close(a);
    archive_read_free(a);
    archive_write_close(ext);
    archive_write_free(ext);

    if (!success) {
        throwIOException(env, error_msg.empty() ? "Archive extraction failed" : error_msg.c_str());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_m5dev_arcx_data_ndk_ArchiveNative_extractArchive(
        JNIEnv *env,
        jobject thiz,
        jstring archive_path_jstr,
        jstring dest_path_jstr) {

    if (archive_path_jstr == nullptr || dest_path_jstr == nullptr) {
        throwIOException(env, "Archive path or destination path is null");
        return JNI_FALSE;
    }

    const char *archive_path = env->GetStringUTFChars(archive_path_jstr, nullptr);
    const char *dest_path = env->GetStringUTFChars(dest_path_jstr, nullptr);

    jboolean result = perform_extraction(env, archive_path, dest_path, nullptr, nullptr);

    env->ReleaseStringUTFChars(archive_path_jstr, archive_path);
    env->ReleaseStringUTFChars(dest_path_jstr, dest_path);

    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_m5dev_arcx_data_ndk_ArchiveNative_extractArchiveWithPassword(
        JNIEnv *env,
        jobject thiz,
        jstring archive_path_jstr,
        jstring dest_path_jstr,
        jstring password_jstr) {

    if (archive_path_jstr == nullptr || dest_path_jstr == nullptr) {
        throwIOException(env, "Archive path or destination path is null");
        return JNI_FALSE;
    }

    const char *archive_path = env->GetStringUTFChars(archive_path_jstr, nullptr);
    const char *dest_path = env->GetStringUTFChars(dest_path_jstr, nullptr);
    const char *password = password_jstr ? env->GetStringUTFChars(password_jstr, nullptr) : nullptr;

    jboolean result = perform_extraction(env, archive_path, dest_path, password, nullptr);

    env->ReleaseStringUTFChars(archive_path_jstr, archive_path);
    env->ReleaseStringUTFChars(dest_path_jstr, dest_path);
    if (password_jstr && password) {
        env->ReleaseStringUTFChars(password_jstr, password);
    }

    return result;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_m5dev_arcx_data_ndk_ArchiveNative_extractArchiveWithProgress(
        JNIEnv *env,
        jobject thiz,
        jstring archive_path_jstr,
        jstring dest_path_jstr,
        jstring password_jstr,
        jobject listener) {

    if (archive_path_jstr == nullptr || dest_path_jstr == nullptr) {
        throwIOException(env, "Archive path or destination path is null");
        return JNI_FALSE;
    }

    const char *archive_path = env->GetStringUTFChars(archive_path_jstr, nullptr);
    const char *dest_path = env->GetStringUTFChars(dest_path_jstr, nullptr);
    const char *password = password_jstr ? env->GetStringUTFChars(password_jstr, nullptr) : nullptr;

    jboolean result = perform_extraction(env, archive_path, dest_path, password, listener);

    env->ReleaseStringUTFChars(archive_path_jstr, archive_path);
    env->ReleaseStringUTFChars(dest_path_jstr, dest_path);
    if (password_jstr && password) {
        env->ReleaseStringUTFChars(password_jstr, password);
    }

    return result;
}

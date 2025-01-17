//
// Created by Devilsen on 2021/5/19.
//

#ifndef CZXING_SCANRESULT_H
#define CZXING_SCANRESULT_H

#include "config.h"
#include <string>
#include <jni.h>
#include "DataStruct.h"

CZXING_BEGIN_NAMESPACE()

USING_CZXING_NAMESPACE()

class ScanResult {

public:
    ScanResult(std::string text, CodeRect rect) : m_text(text), m_codeRect(rect){};

    ScanResult(CodeFormat& format, std::string text, CodeRect rect) :
    m_codeFormat(format), m_text(text), m_codeRect(rect){};

    static void init(JNIEnv *env);
    static void unInit(JNIEnv *env);
    static jobjectArray obtainResultArray(JNIEnv* env, int size);

    CodeFormat format() const {
        return m_codeFormat;
    }

    const std::string& text() const {
        return m_text;
    }

    const CodeRect& rect() const {
        return m_codeRect;
    }

    jobject getJCodeResult(JNIEnv* env);

private:
    CodeFormat m_codeFormat { CodeFormat::QRCode };
    std::string m_text;
    CodeRect m_codeRect;

};

CZXING_END_NAMESPACE()

#endif //CZXING_SCANRESULT_H

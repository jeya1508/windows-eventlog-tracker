#include "pch.h"
#include <jni.h>
#include <Windows.h>
#include <iostream>
#include <map>
#include <string>
#include <vector>
#include <memory>
#include "org_logging_config_EventLogCollector.h"
using namespace std;

string GetEventTypeName(WORD eventType) {
    switch (eventType) {
    case EVENTLOG_ERROR_TYPE:
        return "Error";
    case EVENTLOG_WARNING_TYPE:
        return "Warning";
    case EVENTLOG_INFORMATION_TYPE:
        return "Information";
    case EVENTLOG_AUDIT_SUCCESS:
        return "Audit Success";
    case EVENTLOG_AUDIT_FAILURE:
        return "Audit Failure";
    default:
        return "Unknown";
    }
}
string GetSourceName(const EVENTLOGRECORD* record) {
    const char* dataPtr = reinterpret_cast<const char*>(record) + sizeof(EVENTLOGRECORD);

    const wchar_t* sourcePtr = reinterpret_cast<const wchar_t*>(dataPtr);

    int sourceLength = wcslen(sourcePtr);

    int sizeNeeded = WideCharToMultiByte(CP_UTF8, 0, sourcePtr, sourceLength, NULL, 0, NULL, NULL);
    string source(sizeNeeded, 0);
    WideCharToMultiByte(CP_UTF8, 0, sourcePtr, sourceLength, &source[0], sizeNeeded, NULL, NULL);

    return source;
}

map<string, string> CreateLogDataMap(const EVENTLOGRECORD* record) {
    map<string, string> logData;

    logData["event_id"] = to_string(record->EventID);
    logData["event_type"] = GetEventTypeName(record->EventType);  
    logData["record_number"] = to_string(record->RecordNumber);
    logData["event_category"] = to_string(record->EventCategory);
    logData["time_generated"] = to_string(record->TimeGenerated);
    logData["time_written"] = to_string(record->TimeWritten);
    logData["source"] = GetSourceName(record);
    return logData;
}

extern "C" JNIEXPORT jobjectArray JNICALL Java_org_logging_config_EventLogCollector_collectWindowsLogs(JNIEnv* env, jobject obj, jlong lastRecordNumber) {
    HANDLE hEventLog = OpenEventLog(NULL, L"Security");
    if (hEventLog == NULL) {
        cerr << "Could not open event log: " << GetLastError() << endl;
        return nullptr;
    }

    DWORD oldestRecord = 0;
    DWORD numberOfRecords = 0;
    if (!GetOldestEventLogRecord(hEventLog, &oldestRecord)) {
        cerr << "Failed to get the oldest record: " << GetLastError() << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    if (!GetNumberOfEventLogRecords(hEventLog, &numberOfRecords)) {
        cerr << "Failed to get the number of records: " << GetLastError() << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    DWORD maxRecordNumber = oldestRecord + numberOfRecords - 1;
    DWORD recordNumber = (lastRecordNumber == -1) ? oldestRecord : static_cast<DWORD>(lastRecordNumber);

    cout << "Oldest record: " << oldestRecord << ", Max record: " << maxRecordNumber << ", Starting record: " << recordNumber << endl;

    if (recordNumber == maxRecordNumber)
    {
        cout << "No more new logs" << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    if (recordNumber > maxRecordNumber) {
        cerr << "Requested recordNumber " << recordNumber << " exceeds available records." << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    DWORD bytesRead = 0;
    DWORD minBytesNeeded = sizeof(EVENTLOGRECORD);
    vector<map<string, string>> logs;
    unique_ptr<EVENTLOGRECORD, decltype(&free)> pRecord(static_cast<EVENTLOGRECORD*>(malloc(minBytesNeeded)), &free);

    while (true) {
        if (!ReadEventLog(hEventLog, EVENTLOG_SEEK_READ | EVENTLOG_FORWARDS_READ, recordNumber, pRecord.get(), minBytesNeeded, &bytesRead, &minBytesNeeded)) {
            DWORD error = GetLastError();
            if (error == ERROR_INSUFFICIENT_BUFFER) {
                pRecord.reset(static_cast<EVENTLOGRECORD*>(malloc(minBytesNeeded)));
                if (!pRecord) {
                    cerr << "Memory allocation failed during buffer resize." << endl;
                    break;
                }
                continue;  
            }
            else if (error == ERROR_NO_MORE_ITEMS || error == ERROR_HANDLE_EOF) {
                cout << "Reached the end of the event log. No more records to read." << endl;
                break;
            }
            else {
                cerr << "Error reading event log: " << error << endl;
                break;
            }
        }
        logs.push_back(CreateLogDataMap(pRecord.get()));

        recordNumber = pRecord->RecordNumber + 1;

        if (recordNumber > maxRecordNumber) {
            cout << "Reached the last record. Stopping at record number: " << recordNumber - 1 << endl;
            break;  
        }
    }

    jclass mapClass = env->FindClass("java/util/HashMap");
    if (mapClass == nullptr) {
        cerr << "Error: Unable to find HashMap class" << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    jobjectArray result = env->NewObjectArray(logs.size(), mapClass, nullptr);
    if (result == nullptr) {
        cerr << "Error: Unable to create new jobjectArray" << endl;
        CloseEventLog(hEventLog);
        return nullptr;
    }

    jmethodID putMethod = env->GetMethodID(mapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jmethodID initMethod = env->GetMethodID(mapClass, "<init>", "()V");

    for (size_t i = 0; i < logs.size(); ++i) {
        jobject mapObject = env->NewObject(mapClass, initMethod);
        if (mapObject == nullptr) {
            cerr << "Error: Unable to create new HashMap object" << endl;
            CloseEventLog(hEventLog);
            return nullptr;
        }
        for (const auto& entry : logs[i]) {
            jstring jKey = env->NewStringUTF(entry.first.c_str());
            jstring jValue = env->NewStringUTF(entry.second.c_str());

            env->CallObjectMethod(mapObject, putMethod, jKey, jValue);

            env->DeleteLocalRef(jKey);
            env->DeleteLocalRef(jValue);
        }

        env->SetObjectArrayElement(result, i, mapObject);
        env->DeleteLocalRef(mapObject);
    }

    CloseEventLog(hEventLog);
    return result;
}
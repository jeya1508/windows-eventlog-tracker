//// dllmain.cpp : Defines the entry point for the DLL application.
#include "pch.h"
//#include <windows.h>
//#include <evntcons.h>
//#include <iostream>
//
//extern "C" __declspec(dllexport) void getEventLogs() {
//    HANDLE hEventLog = OpenEventLog(NULL, L"Security");
//    if (hEventLog == NULL) {
//        std::cerr << "Could not open the event log." << std::endl;
//        return;
//    }
//
//    EVENTLOGRECORD* pEventLogRecord = (EVENTLOGRECORD*)malloc(1024);
//    DWORD dwBytesRead = 0;
//    DWORD dwMinNumberOfBytesNeeded = 0;
//
//    while (ReadEventLog(hEventLog, EVENTLOG_SEQUENTIAL_READ | EVENTLOG_FORWARDS_READ, 0, pEventLogRecord, 1024, &dwBytesRead, &dwMinNumberOfBytesNeeded)) {
//        std::cout << "Event ID: " << pEventLogRecord->EventID << std::endl;
//        std::cout << "Event Source: " << (char*)((BYTE*)pEventLogRecord + sizeof(EVENTLOGRECORD)) << std::endl;
//        // You can extract more information from the EVENTLOGRECORD structure here.
//    }
//
//    CloseEventLog(hEventLog);
//    free(pEventLogRecord);
//}
//

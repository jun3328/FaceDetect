cmake_minimum_required(VERSION 3.4.1)

set(pathPROJECT C:/Users/Lee/AndroidStudioProjects/OpcvApplication) # 수정필요
set(pathOPENCV ${pathPROJECT}/opencv)
set(pathLIBOPENCV_JAVA ${pathOPENCV}/native/libs/${ANDROID_ABI}/libopencv_java4.so)
set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

include_directories(${pathOPENCV}/native/jni/include)
# 라이브러리 추가
add_library( # 라이브러리 이름
             native-lib
             # 공유 라이브러리로 설정
             SHARED
             # 파일 위치 상대경로
             native-lib.cpp )
add_library( lib_opencv SHARED IMPORTED )
set_target_properties(lib_opencv PROPERTIES IMPORTED_LOCATION ${pathLIBOPENCV_JAVA})
# NDK 라이브러리를 찾은 후 해당 경로를 변수로 저장
# Android 관련 로그 지원 라이브러리를 찾은 후 해당 경로를 log-lib에 저장
find_library( # 라이브러리 변수명
              log-lib
              # 라이브러리
              log )
find_library( android-lib android)
# 네이티브 라이브러리가 log 라이브러리의 함수를 호출하기 위해
# target_link_libraries() 명령을 사용하여 라이브러리를 링크
target_link_libraries( # 대상 라이브러리
                       native-lib
                       lib_opencv
                       # find_library 명령으로 지정한 변수명
                       ${android-lib}
                       ${log-lib} )
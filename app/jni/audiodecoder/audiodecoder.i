%module audiodecoder
#pragma SWIG nowarn=503,516,401

#%include "typemaps.i"
%include "arrays_java.i"
#%include "std_map.i"
#%include "std_vector.i"
%include "stdint.i"
%include "various.i"

%typemap(in) (char* pchInput, int inputSize) {
  $1 = (char*)jenv->GetDirectBufferAddress($input);
  $2 = (int)(jenv->GetDirectBufferCapacity($input));
}
%typemap(in) (char* pchOutput, int* outputSize) {
  $1 = (char*)jenv->GetDirectBufferAddress($input);
  $2 = &((int)(jenv->GetDirectBufferCapacity($input)));
}

/* These 3 typemaps tell SWIG what JNI and Java types to use */
%typemap(jni)       (char* pchInput, int inputSize), (char* pchOutput, int* outputSize) "jobject"
%typemap(jtype)     (char* pchInput, int inputSize), (char* pchOutput, int* outputSize) "java.nio.ByteBuffer"
%typemap(jstype)    (char* pchInput, int inputSize), (char* pchOutput, int* outputSize) "java.nio.ByteBuffer"
%typemap(javain)    (char* pchInput, int inputSize), (char* pchOutput, int* outputSize) "$javainput"
%typemap(javaout)   (char* pchInput, int inputSize), (char* pchOutput, int* outputSize) {
    return $jnicall;
}

%{
#include "ac3decoder.h"
#include "mpadecoder.h"
%}


//
// AC3Decoder
//

%include "ac3decoder.h"

//
// MpegAudioDecoder
//

%include "mpadecoder.h"

%pragma(java) jniclasscode=%{

static {
	try {
		System.loadLibrary("a52");
		System.loadLibrary("mad");
		System.loadLibrary("audiodecoder");
	}
	catch (UnsatisfiedLinkError e) {
	}
}
%}

%module audiodecoder
#pragma SWIG nowarn=503,516,401

#%include "typemaps.i"
%include "arrays_java.i"
#%include "std_map.i"
#%include "std_vector.i"
%include "stdint.i"
%include "various.i"

%rename("%(lowercamelcase)s") "";
%rename (AC3DecoderNative) AC3Decoder;

%{
#include "ac3decoder.h"
%}


//
// AC3Decoder
//

%include "ac3decoder.h"

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

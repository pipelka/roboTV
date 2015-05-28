%module ac3decoder
#pragma SWIG nowarn=503,516,401

#%include "typemaps.i"
%include "arrays_java.i"
#%include "std_map.i"
#%include "std_vector.i"
%include "stdint.i"
%include "various.i"

%rename("%(lowercamelcase)s") "";
%rename (Decoder) AC3Decoder;
%rename (SyncInfo) AC3SyncInfo;

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
		System.loadLibrary("ac3decoder");
	}
	catch (UnsatisfiedLinkError e) {
	}
}
%}

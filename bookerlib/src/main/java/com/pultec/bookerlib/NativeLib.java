package com.pultec.bookerlib;

public class NativeLib {

    // Used to load the 'bookerlib' library on application startup.
    static {
        System.loadLibrary("bookerlib");
    }

    /**
     * A native method that is implemented by the 'bookerlib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}